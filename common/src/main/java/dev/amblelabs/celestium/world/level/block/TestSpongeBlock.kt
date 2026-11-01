package dev.amblelabs.celestium.world.level.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.tags.FluidTags
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.BucketPickup
import net.minecraft.world.level.block.LiquidBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.redstone.Orientation
import java.util.function.Consumer

class TestSpongeBlock(properties: Properties) : Block(properties) {

    var positions: ArrayList<BlockPos> = ArrayList()

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, movedByPiston: Boolean) {
        if (!oldState.`is`(state.block)) {
            this.tryAbsorbWater(level, pos)
        }
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        orientation: Orientation?,
        movedByPiston: Boolean
    ) {
        this.tryAbsorbWater(level, pos)
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston)
    }

    private fun tryAbsorbWater(level: Level, pos: BlockPos) {
        positions.clear()
        if (this.removeWaterBreadthFirstSearch(level, pos)) {
            level.playSound(null, pos, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 1.0f, 1.0f)

            println("Distributed gas: " + positions.size + " blocks")
        }
    }

    private fun removeWaterBreadthFirstSearch(level: Level, startPos: BlockPos): Boolean {
        return BlockPos.breadthFirstTraversal(
            startPos,
            6,
            65,
            { pos: BlockPos, consumer: Consumer<BlockPos> ->
                for (direction in ALL_DIRECTIONS) {
                    consumer.accept(pos.relative(direction))
                }
            },
            { pos: BlockPos? ->
                if (pos == startPos) {
                    return@breadthFirstTraversal BlockPos.TraversalNodeStatus.ACCEPT
                } else {
                    val state = level.getBlockState(pos!!)
                    val fluidState = level.getFluidState(pos)
                    if (!fluidState.`is`(FluidTags.WATER)) {
                        return@breadthFirstTraversal BlockPos.TraversalNodeStatus.SKIP
                    } else {
                        val block = state.block
                        if (block is BucketPickup) {
                            if (!block.pickupBlock(null, level, pos, state).isEmpty) {
                                this.positions.add(pos)
                                return@breadthFirstTraversal BlockPos.TraversalNodeStatus.ACCEPT
                            }
                        }

                        if (state.block is LiquidBlock) {
                            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())
                            this.positions.add(pos)
                        } else {
                            if (!state.`is`(Blocks.KELP) && !state.`is`(Blocks.KELP_PLANT)
                                && !state.`is`(Blocks.SEAGRASS) && !state.`is`(
                                    Blocks.TALL_SEAGRASS
                                )
                            ) {
                                return@breadthFirstTraversal BlockPos.TraversalNodeStatus.SKIP
                            }

                            val blockEntity = if (state.hasBlockEntity()) level.getBlockEntity(pos) else null
                            dropResources(state, level, pos, blockEntity)
                            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())
                            this.positions.add(pos)
                        }

                        return@breadthFirstTraversal BlockPos.TraversalNodeStatus.ACCEPT
                    }
                }
            }) > 1
    }

    companion object {
        private val ALL_DIRECTIONS = Direction.entries.toTypedArray()
    }
}
