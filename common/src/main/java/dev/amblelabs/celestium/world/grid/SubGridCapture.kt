package dev.amblelabs.celestium.world.grid;

import dev.amblelabs.celestium.Celestium;
import dev.amblelabs.celestium.init.CelestiumBlocks;
import dev.amblelabs.celestium.world.level.block.FloataterBlock;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;

import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public record SubGridCapture(SubGridBlocks blocks, LongSet mask, BlockPos minPos, Vec3i size, int engines) {
    private static final Direction[] DIRECTIONS = Direction.values();

    @Nullable
    public static SubGridCapture scan(Level level, BlockPos blockPos, Direction movement) {
        Long2ObjectMap<BlockState> captured = new Long2ObjectOpenHashMap<>();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();

        captured.put(blockPos.asLong(), level.getBlockState(blockPos));
        queue.enqueue(blockPos.asLong());

        int minX = blockPos.getX();
        int minY = blockPos.getY();
        int minZ = blockPos.getZ();
        int maxX = blockPos.getX();
        int maxY = blockPos.getY();
        int maxZ = blockPos.getZ();

        MutableBlockPos pos = new MutableBlockPos();
        MutableBlockPos neighbor = new MutableBlockPos();
        int sizeLimit = level.getServer().getGameRules().get(Celestium.MAX_FLOATATER_SIZE);

        while (!queue.isEmpty()) {
            long p = queue.dequeueLastLong();
            pos.set(p);

            BlockState state = captured.get(p);
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());

            if (maxX - minX + 1 > sizeLimit || maxY - minY + 1 > sizeLimit || maxZ - minZ + 1 > sizeLimit)
                return null;

            VoxelShape shape = state.getShape(level, pos);

            for (Direction direction : DIRECTIONS) {
                neighbor.setWithOffset(pos, direction);
                long neighborPacked = neighbor.asLong();

                if (!captured.containsKey(neighborPacked)) {
                    BlockState neighborState = level.getBlockState(neighbor);
                    VoxelShape neighborShape = neighborState.getShape(level, neighbor);

                    boolean pushedInFront = direction == movement && !neighborState.canBeReplaced();
                    if (pushedInFront || isConnected(direction, shape, neighborShape, state, neighborState)) {
                        queue.enqueue(neighborPacked);
                        captured.put(neighborPacked, neighborState);
                    }
                }
            }
        }

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        SubGridBlocks blocks = new SubGridBlocks(sizeX, sizeY, sizeZ);
        int engines = 0;

        for (Entry<BlockState> entry : Long2ObjectMaps.fastIterable(captured)) {
            pos.set(entry.getLongKey());
            BlockState state = entry.getValue().trySetValue(BlockStateProperties.WATERLOGGED, false);

            if (state.is(CelestiumBlocks.FLOATATER)
                    && state.getValue(FloataterBlock.FACING) == movement
                    && state.getValue(FloataterBlock.TRIGGERED)) {
                engines++;
            }

            if (state.getBlock() instanceof FlyingTickable)
                blocks.markTickable(new BlockPos(pos.getX() - minX, pos.getY() - minY, pos.getZ() - minZ));

            if (!state.hasBlockEntity())
                blocks.setBlockState(pos.getX() - minX, pos.getY() - minY, pos.getZ() - minZ, state);
        }

        return new SubGridCapture(blocks, new LongOpenHashSet(captured.keySet()), new BlockPos(minX, minY, minZ), new Vec3i(sizeX, sizeY, sizeZ), engines);
    }

    private static boolean isConnected(Direction direction, VoxelShape shape, VoxelShape neighborShape, BlockState state, BlockState neighborState) {
        if (!isStickyInDirection(state, direction) && !isStickyInDirection(neighborState, direction.getOpposite())) {
            return areShapesConnected(direction, shape, neighborShape) && !isNonStickyInDirection(state, direction) && !isNonStickyInDirection(neighborState, direction.getOpposite());
        } else {
            return true;
        }
    }

    private static boolean areShapesConnected(Direction direction, VoxelShape shape, VoxelShape neighborShape) {
        if (shape != Shapes.empty() && neighborShape != Shapes.empty()) {
            VoxelShape face = shape.getFaceShape(direction);
            VoxelShape neighborFace = neighborShape.getFaceShape(direction.getOpposite());
            return face == Shapes.block() && neighborFace == Shapes.block() || Shapes.joinIsNotEmpty(face, neighborFace, BooleanOp.AND);
        } else {
            return false;
        }
    }

    private static boolean isNonStickyInDirection(BlockState state, Direction direction) {
        return state.is(CelestiumBlocks.FLOATATER) && state.getValue(FloataterBlock.FACING) != direction;
    }

    private static boolean isStickyInDirection(BlockState state, Direction direction) {
        if (!state.is(Blocks.SLIME_BLOCK) && !state.is(Blocks.HONEY_BLOCK)) {
            return state.is(Blocks.STICKY_PISTON) && state.getValue(BlockStateProperties.FACING) == direction
                    || state.is(CelestiumBlocks.FLOATATER) && state.getValue(FloataterBlock.FACING) == direction;
        } else {
            return true;
        }
    }

    public void remove(Level level) {
        this.forEachPos(pos -> {
            BlockState state = level.getBlockState(pos);
            if (state.hasBlockEntity()) {
                level.destroyBlock(pos, true);
            } else {
                level.setBlock(pos, state.getFluidState().createLegacyBlock(), Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS);
            }
        });

        this.forEachPos(pos -> level.updateNeighborsAt(pos, level.getBlockState(pos).getBlock()));
    }

    private void forEachPos(Consumer<BlockPos> consumer) {
        MutableBlockPos pos = new MutableBlockPos();
        LongIterator iterator = this.mask.longIterator();

        while (iterator.hasNext()) {
            pos.set(iterator.nextLong());
            consumer.accept(pos);
        }
    }
}
