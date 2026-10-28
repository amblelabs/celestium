package dev.amblelabs.celestium.world.grid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amblelabs.celestium.Celestium;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;

public class SubGridBlocks {
    private static final BlockState EMPTY_BLOCK_STATE = Blocks.AIR.defaultBlockState();
    private static final int NO_INDEX = -1;

    private static final StreamCodec<ByteBuf, List<BlockState>> PALETTE_STREAM_CODEC = ByteBufCodecs.idMapper(Block.BLOCK_STATE_REGISTRY).apply(ByteBufCodecs.list());
    private static final StreamCodec<ByteBuf, List<BlockPos>> TICKABLES_STREAM_CODEC = BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list());

    public static final StreamCodec<RegistryFriendlyByteBuf, SubGridBlocks> STREAM_CODEC = new StreamCodec<>() {
        public @NonNull SubGridBlocks decode(RegistryFriendlyByteBuf buffer) {
            int sizeX = buffer.readVarInt();
            int sizeY = buffer.readVarInt();
            int sizeZ = buffer.readVarInt();
            List<BlockState> palette = SubGridBlocks.PALETTE_STREAM_CODEC.decode(buffer);
            BlockState[] blockStates = new BlockState[sizeX * sizeY * sizeZ];
            SimpleBitStorage storage = new SimpleBitStorage(SubGridBlocks.bitsFor(palette.size()), blockStates.length, buffer.readLongArray());

            for (int i = 0; i < blockStates.length; ++i) {
                blockStates[i] = palette.get(storage.get(i));
            }

            List<BlockPos> tickables = new ArrayList<>(SubGridBlocks.TICKABLES_STREAM_CODEC.decode(buffer));
            Object2ObjectMap<BlockPos, BlockEntity> blockEntities = new Object2ObjectOpenHashMap<>();

            SubGridBlocks subGridBlocks = new SubGridBlocks(blockStates, blockEntities, tickables, sizeX, sizeY, sizeZ);

            int size = buffer.readVarInt();
            for (int i=0;i<size;i++) {
                BlockPos blockPos = buffer.readBlockPos();
                CompoundTag tag = buffer.readNbt();
                BlockState state = subGridBlocks.getBlockState(blockPos);
                BlockEntity blockEntity;
                if ("DUMMY".equals(tag.getStringOr("id", ""))) {
                    if (state.hasBlockEntity()) {
                        blockEntity = ((EntityBlock)state.getBlock()).newBlockEntity(blockPos, state);
                    } else {
                        blockEntity = null;
                        Celestium.LOGGER.warn("Tried to load a DUMMY block entity @ {} but found not block entity block {} at location", pos, state);
                    }
                } else {
                    blockEntity = BlockEntity.loadStatic(blockPos, state, tag, buffer.registryAccess());
                }

                if (blockEntity != null) {
                    // TODO COME BACK TO
                    //blockEntity.setLevel(this.level);
                    this.addAndRegisterBlockEntity(blockEntity);
                } else {
                    LOGGER.warn("Tried to load a block entity for block {} but failed at location {}", state, pos);
                }
            }

            return subGridBlocks;
        }

        public void encode(RegistryFriendlyByteBuf buffer, SubGridBlocks blocks) {
            buffer.writeVarInt(blocks.sizeX);
            buffer.writeVarInt(blocks.sizeY);
            buffer.writeVarInt(blocks.sizeZ);
            Reference2IntMap<BlockState> indices = new Reference2IntOpenHashMap<>();
            indices.defaultReturnValue(NO_INDEX);
            List<BlockState> palette = new ArrayList<>();

            for (BlockState state : blocks.blockStates) {
                if (indices.putIfAbsent(state, palette.size()) == NO_INDEX) {
                    palette.add(state);
                }
            }

            SubGridBlocks.PALETTE_STREAM_CODEC.encode(buffer, palette);
            SimpleBitStorage storage = new SimpleBitStorage(SubGridBlocks.bitsFor(palette.size()), blocks.blockStates.length);

            for (int i = 0; i < blocks.blockStates.length; ++i) {
                storage.set(i, indices.getInt(blocks.blockStates[i]));
            }

            buffer.writeLongArray(storage.getRaw());
            SubGridBlocks.TICKABLES_STREAM_CODEC.encode(buffer, blocks.tickables);
        }
    };

    public void addAndRegisterBlockEntity(Level level, BlockEntity blockEntity) {
        this.setBlockEntity(level, blockEntity);
        if (level instanceof ServerLevel serverLevel) {
            this.addGameEventListener(blockEntity, serverLevel);
        }

        this.updateBlockEntityTicker(blockEntity);
    }

    public void setBlockEntity(Level level, BlockEntity blockEntity) {
        BlockPos blockPos = blockEntity.getBlockPos();
        BlockState blockState = this.getBlockState(blockPos);
        if (!blockState.hasBlockEntity()) {
            Celestium.LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", blockEntity, blockPos, blockState);
        } else {
            BlockState blockState2 = blockEntity.getBlockState();
            if (blockState != blockState2) {
                if (!blockEntity.getType().isValid(blockState)) {
                    Celestium.LOGGER.warn("Trying to set block entity {} at position {}, but state {} does not allow it", blockEntity, blockPos, blockState);
                    return;
                }

                if (blockState.getBlock() != blockState2.getBlock()) {
                    Celestium.LOGGER.warn("Block state mismatch on block entity {} in position {}, {} != {}, updating", blockEntity, blockPos, blockState, blockState2);
                }

                blockEntity.setBlockState(blockState);
            }

            blockEntity.setLevel(level);
            blockEntity.clearRemoved();
            BlockEntity blockEntity2 = this.blockEntities.put(blockPos.immutable(), blockEntity);
            if (blockEntity2 != null && blockEntity2 != blockEntity) {
                blockEntity2.setRemoved();
            }
        }
    }

    public static final Codec<SubGridBlocks> CODEC = SubGridBlocks.Stored.CODEC.xmap(SubGridBlocks::fromStored, SubGridBlocks::toStored);

    private static int bitsFor(int paletteSize) {
        return Math.max(1, Mth.ceillog2(paletteSize));
    }

    final BlockState[] blockStates;
    final Object2ObjectMap<BlockPos, BlockEntity> blockEntities;
    final List<BlockPos> tickables;
    final int sizeX;
    final int sizeY;
    final int sizeZ;

    SubGridBlocks(BlockState[] blockStates, Object2ObjectMap<BlockPos, BlockEntity> blockEntities, List<BlockPos> list, int i, int j, int k) {
        this.blockStates = blockStates;
        this.blockEntities = blockEntities;
        this.tickables = list;
        this.sizeX = i;
        this.sizeY = j;
        this.sizeZ = k;
    }

    public SubGridBlocks(Object2ObjectMap<BlockPos, BlockEntity> blockEntities, int i, int j, int k) {
        this.blockEntities = blockEntities;
        this.blockStates = new BlockState[i * j * k];
        Arrays.fill(this.blockStates, EMPTY_BLOCK_STATE);
        this.tickables = new ArrayList<>();
        this.sizeX = i;
        this.sizeY = j;
        this.sizeZ = k;
    }

    public void setBlockState(int i, int j, int k, BlockState blockState) {
        int l = this.index(i, j, k);
        if (l == -1) {
            throw new IllegalStateException("Block was out of bounds");
        }

        this.blockStates[l] = blockState;
    }

    public void markTickable(BlockPos blockPos) {
        this.tickables.add(blockPos);
    }

    public void tick(Level level, Vec3 vec3, Direction direction) {
        this.tickables.forEach(blockPos -> {
            BlockState blockState = this.getBlockState(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if (blockState.getBlock() instanceof FlyingTickable flyingTickable) {
                flyingTickable.flyingTick(level, this, blockState, blockPos, vec3.add(blockPos.getX(), blockPos.getY(), blockPos.getZ()), direction);
            }
        });
    }

    public BlockState getBlockState(int i, int j, int k) {
        int l = this.index(i, j, k);
        return l == NO_INDEX ? EMPTY_BLOCK_STATE : this.blockStates[l];
    }

    public BlockState getBlockState(BlockPos blockPos) {
        return this.getBlockState(blockPos.getX(), blockPos.getY(), blockPos.getZ());
    }

    private int index(int i, int j, int k) {
        return i >= 0 && j >= 0 && k >= 0 && i < this.sizeX && j < this.sizeY && k < this.sizeZ ? (i + k * this.sizeX) * this.sizeY + j : -1;
    }

    public int sizeX() {
        return this.sizeX;
    }

    public int sizeY() {
        return this.sizeY;
    }

    public int sizeZ() {
        return this.sizeZ;
    }

    public SubGridBlocks copy() {
        return new SubGridBlocks(
                Arrays.copyOf(this.blockStates, this.blockStates.length), new ArrayList<>(this.tickables), this.sizeX, this.sizeY, this.sizeZ
        );
    }

    public void place(BlockPos blockPos, Level level) {
        MutableBlockPos pos = new MutableBlockPos();

        for (int z = 0; z < this.sizeZ; z++) {
            for (int x = 0; x < this.sizeX; x++) {
                for (int y = 0; y < this.sizeY; y++) {
                    pos.setWithOffset(blockPos, x, y, z);
                    BlockState state = this.getBlockState(x, y, z);

                    if (!state.isAir()) {
                        FluidState fluidState = level.getFluidState(pos);

                        if (fluidState.is(Fluids.WATER))
                            state = state.trySetValue(BlockStateProperties.WATERLOGGED, true);

                        level.setBlock(pos, state, Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
                    }
                }
            }
        }

        for (int z = 0; z < this.sizeZ; z++) {
            for (int x = 0; x < this.sizeX; x++) {
                for (int y = 0; y < this.sizeY; y++) {
                    pos.setWithOffset(blockPos, x, y, z);
                    level.updateNeighborsAt(pos, this.getBlockState(x, y, z).getBlock());
                }
            }
        }
    }

    private static SubGridBlocks fromStored(Stored stored) {
        BlockState[] blockStates = new BlockState[stored.sizeX() * stored.sizeY() * stored.sizeZ()];
        int[] indices = stored.blocks().toArray();
        if (indices.length != blockStates.length) {
            return new SubGridBlocks(stored.sizeX(), stored.sizeY(), stored.sizeZ());
        } else {
            for (int i = 0; i < indices.length; ++i) {
                int index = indices[i];
                blockStates[i] = index >= 0 && index < stored.palette().size() ? stored.palette().get(index) : EMPTY_BLOCK_STATE;
            }

            return new SubGridBlocks(blockStates,
                    stored.tickables().mapToObj(BlockPos::of).collect(Collectors.toList()), // TODO: does this need to be an array list?
                    stored.sizeX(), stored.sizeY(), stored.sizeZ());
        }
    }

    private Stored toStored() {
        Reference2IntMap<BlockState> indices = new Reference2IntOpenHashMap<>();
        indices.defaultReturnValue(NO_INDEX);
        List<BlockState> palette = new ArrayList<>();
        int[] blocks = new int[this.blockStates.length];

        for (int i = 0; i < this.blockStates.length; ++i) {
            int index = indices.putIfAbsent(this.blockStates[i], palette.size());
            if (index == NO_INDEX) {
                blocks[i] = palette.size();
                palette.add(this.blockStates[i]);
            } else {
                blocks[i] = index;
            }
        }

        return new Stored(this.sizeX, this.sizeY, this.sizeZ, palette, IntStream.of(blocks), this.tickables.stream().mapToLong(BlockPos::asLong));
    }

    private record Stored(int sizeX, int sizeY, int sizeZ, List<BlockState> palette, IntStream blocks, LongStream tickables) {
        static final Codec<Stored> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("size_x").forGetter(Stored::sizeX),
                Codec.INT.fieldOf("size_y").forGetter(Stored::sizeY),
                Codec.INT.fieldOf("size_z").forGetter(Stored::sizeZ),
                BlockState.CODEC.listOf().fieldOf("palette").forGetter(Stored::palette),
                Codec.INT_STREAM.fieldOf("blocks").forGetter(Stored::blocks),
                Codec.LONG_STREAM.optionalFieldOf("tickables", LongStream.empty()).forGetter(Stored::tickables)
        ).apply(instance, Stored::new));
    }
}

