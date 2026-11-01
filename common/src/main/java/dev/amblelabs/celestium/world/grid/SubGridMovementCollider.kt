package dev.amblelabs.celestium.world.grid

import it.unimi.dsi.fastutil.longs.LongArrayList
import it.unimi.dsi.fastutil.longs.LongList
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import kotlin.math.max

class SubGridMovementCollider private constructor(private val edgeBlocks: LongList, private val size: BlockPos) {
    fun checkCollision(level: Level, origin: BlockPos): Boolean {
        val bottom = origin.y
        val top = bottom + this.size.y - 1

        if (bottom >= level.minY) { // readd maxY reactive to an interface like #hasTeleportToSpace or smt
            val pos = BlockPos.MutableBlockPos()
            val iterator = this.edgeBlocks.longIterator()

            while (iterator.hasNext()) {
                pos.set(iterator.nextLong())
                pos.move(origin)

                if (isCollidable(level.getBlockState(pos))) return true
            }

            return false
        } else {
            return true
        }
    }

    companion object {
        fun generate(blocks: SubGridBlocks, movement: Direction): SubGridMovementCollider {
            val edgeBlocks: LongList = LongArrayList()
            val pos = BlockPos.MutableBlockPos()
            val backwards = movement.opposite
            val depth = movement.axis.choose(blocks.sizeX, blocks.sizeY, blocks.sizeZ)

            for (front in getFrontSide(blocks, movement)) {
                pos.set(front)
                var prevCollidable = false

                repeat(depth) {
                    if (isCollidable(blocks.getBlockState(pos))) {
                        if (!prevCollidable) {
                            edgeBlocks.add(pos.asLong())
                        }

                        prevCollidable = true
                    } else {
                        prevCollidable = false
                    }

                    pos.move(backwards)
                }
            }

            return SubGridMovementCollider(edgeBlocks, BlockPos(blocks.sizeX, blocks.sizeY, blocks.sizeZ))
        }

        private fun getFrontSide(blocks: SubGridBlocks, movement: Direction): Iterable<BlockPos> {
            val start = BlockPos(
                max(movement.stepX, 0) * (blocks.sizeX - 1),
                max(movement.stepY, 0) * (blocks.sizeY - 1),
                max(movement.stepZ, 0) * (blocks.sizeZ - 1)
            )

            val end = start.offset(
                if (movement.axis == Direction.Axis.X) 0 else blocks.sizeX - 1,
                if (movement.axis == Direction.Axis.Y) 0 else blocks.sizeY - 1,
                if (movement.axis == Direction.Axis.Z) 0 else blocks.sizeZ - 1
            )

            return BlockPos.betweenClosed(start, end)
        }

        private fun isCollidable(blockState: BlockState): Boolean {
            return !blockState.canBeReplaced()
        }
    }
}

