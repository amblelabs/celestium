package dev.amblelabs.celestium.world.grid;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
            return new SubGridBlocks(blockStates, tickables, sizeX, sizeY, sizeZ);
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

    public static final Codec<SubGridBlocks> CODEC = SubGridBlocks.Stored.CODEC.xmap(SubGridBlocks::fromStored, SubGridBlocks::toStored);

    private static int bitsFor(int paletteSize) {
        return Math.max(1, Mth.ceillog2(paletteSize));
    }

    final BlockState[] blockStates;
    final List<BlockPos> tickables;
    final int sizeX;
    final int sizeY;
    final int sizeZ;

    SubGridBlocks(BlockState[] blockStates, List<BlockPos> list, int i, int j, int k) {
        this.blockStates = blockStates;
        this.tickables = list;
        this.sizeX = i;
        this.sizeY = j;
        this.sizeZ = k;
    }

    public SubGridBlocks(int i, int j, int k) {
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

