package dev.amblelabs.celestium.world.grid;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class SubGridMovementCollider {
    private final LongList edgeBlocks;
    private final BlockPos size;

    private SubGridMovementCollider(LongList edgeBlocks, BlockPos size) {
        this.edgeBlocks = edgeBlocks;
        this.size = size;
    }

    public static SubGridMovementCollider generate(SubGridBlocks blocks, Direction movement) {
        LongList edgeBlocks = new LongArrayList();
        MutableBlockPos pos = new MutableBlockPos();
        Direction backwards = movement.getOpposite();
        int depth = movement.getAxis().choose(blocks.sizeX(), blocks.sizeY(), blocks.sizeZ());

        for (BlockPos front : getFrontSide(blocks, movement)) {
            pos.set(front);
            boolean prevCollidable = false;

            for (int j = 0; j < depth; j++) {
                if (isCollidable(blocks.getBlockState(pos))) {
                    if (!prevCollidable) {
                        edgeBlocks.add(pos.asLong());
                    }

                    prevCollidable = true;
                } else {
                    prevCollidable = false;
                }

                pos.move(backwards);
            }
        }

        return new SubGridMovementCollider(edgeBlocks, new BlockPos(blocks.sizeX(), blocks.sizeY(), blocks.sizeZ()));
    }

    private static Iterable<BlockPos> getFrontSide(SubGridBlocks blocks, Direction movement) {
        BlockPos start = new BlockPos(
                Math.max(movement.getStepX(), 0) * (blocks.sizeX() - 1),
                Math.max(movement.getStepY(), 0) * (blocks.sizeY() - 1),
                Math.max(movement.getStepZ(), 0) * (blocks.sizeZ() - 1)
        );
        BlockPos end = start.offset(
                movement.getAxis() == Axis.X ? 0 : blocks.sizeX() - 1,
                movement.getAxis() == Axis.Y ? 0 : blocks.sizeY() - 1,
                movement.getAxis() == Axis.Z ? 0 : blocks.sizeZ() - 1
        );
        return BlockPos.betweenClosed(start, end);
    }

    public boolean checkCollision(Level level, BlockPos origin) {
        int bottom = origin.getY();
        int top = bottom + this.size.getY() - 1;

        if (bottom >= level.getMinY()) { // readd maxY reactive to an interface like #hasTeleportToSpace or smt
            MutableBlockPos pos = new MutableBlockPos();
            LongIterator iterator = this.edgeBlocks.longIterator();

            while (iterator.hasNext()) {
                pos.set(iterator.nextLong());
                pos.move(origin);

                if (isCollidable(level.getBlockState(pos)))
                    return true;
            }

            return false;
        } else {
            return true;
        }
    }

    private static boolean isCollidable(BlockState blockState) {
        return !blockState.canBeReplaced();
    }
}

