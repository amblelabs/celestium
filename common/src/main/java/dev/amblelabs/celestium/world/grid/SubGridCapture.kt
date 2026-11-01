package dev.amblelabs.celestium.world.grid

import dev.amblelabs.celestium.Celestium
import dev.amblelabs.celestium.init.CelestiumBlocks
import dev.amblelabs.celestium.world.level.block.FloataterBlock
import it.unimi.dsi.fastutil.longs.*
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

@JvmRecord
data class SubGridCapture(
    val blocks: SubGridBlocks,
    val mask: LongSet,
    val minPos: BlockPos,
    val size: Vec3i,
    val engines: Int
) {
    fun remove(level: Level) {
        this.forEachPos { pos: BlockPos ->
            val state = level.getBlockState(pos)
            if (state.hasBlockEntity()) {
                level.destroyBlock(pos, true)
            } else {
                level.setBlock(
                    pos,
                    state.fluidState.createLegacyBlock(),
                    Block.UPDATE_KNOWN_SHAPE or Block.UPDATE_CLIENTS
                )
            }
        }

        this.forEachPos { pos: BlockPos? ->
            level.updateNeighborsAt(
                pos!!,
                level.getBlockState(pos).block
            )
        }
    }

    private fun forEachPos(consumer: Consumer<BlockPos>) {
        val pos = BlockPos.MutableBlockPos()
        val iterator = this.mask.longIterator()

        while (iterator.hasNext()) {
            pos.set(iterator.nextLong())
            consumer.accept(pos)
        }
    }

    companion object {
        private val DIRECTIONS = Direction.entries.toTypedArray()

        fun scan(level: Level, blockPos: BlockPos, movement: Direction): SubGridCapture? {
            val captured: Long2ObjectMap<BlockState> = Long2ObjectOpenHashMap()
            val queue = LongArrayFIFOQueue()

            captured.put(blockPos.asLong(), level.getBlockState(blockPos))
            queue.enqueue(blockPos.asLong())

            var minX = blockPos.x
            var minY = blockPos.y
            var minZ = blockPos.z
            var maxX = blockPos.x
            var maxY = blockPos.y
            var maxZ = blockPos.z

            val pos = BlockPos.MutableBlockPos()
            val neighbor = BlockPos.MutableBlockPos()
            val sizeLimit: Int = level.server!!.gameRules.get(Celestium.MAX_FLOATATER_SIZE)

            while (!queue.isEmpty) {
                val p = queue.dequeueLastLong()
                pos.set(p)

                val state = captured.get(p)
                minX = min(minX, pos.x)
                minY = min(minY, pos.y)
                minZ = min(minZ, pos.z)
                maxX = max(maxX, pos.x)
                maxY = max(maxY, pos.y)
                maxZ = max(maxZ, pos.z)

                if (maxX - minX + 1 > sizeLimit || maxY - minY + 1 > sizeLimit || maxZ - minZ + 1 > sizeLimit) return null

                val shape = state.getShape(level, pos)

                for (direction in DIRECTIONS) {
                    neighbor.setWithOffset(pos, direction)
                    val neighborPacked = neighbor.asLong()

                    if (!captured.containsKey(neighborPacked)) {
                        val neighborState = level.getBlockState(neighbor)
                        val neighborShape = neighborState.getShape(level, neighbor)

                        val pushedInFront = direction == movement && !neighborState.canBeReplaced()
                        if (pushedInFront || isConnected(direction, shape, neighborShape, state, neighborState)) {
                            queue.enqueue(neighborPacked)
                            captured.put(neighborPacked, neighborState)
                        }
                    }
                }
            }

            val sizeX = maxX - minX + 1
            val sizeY = maxY - minY + 1
            val sizeZ = maxZ - minZ + 1

            val blocks = SubGridBlocks(sizeX, sizeY, sizeZ)
            var engines = 0

            for (entry in Long2ObjectMaps.fastIterable<BlockState?>(captured)) {
                pos.set(entry.longKey)
                val state = entry.value!!.trySetValue(BlockStateProperties.WATERLOGGED, false)

                if (state.`is`(CelestiumBlocks.FLOATATER)
                    && state.getValue(FloataterBlock.FACING) == movement && state.getValue(
                        FloataterBlock.TRIGGERED
                    )
                ) {
                    engines++
                }

                if (state.block is FlyingTickable) blocks.markTickable(
                    BlockPos(
                        pos.x - minX,
                        pos.y - minY,
                        pos.z - minZ
                    )
                )

                if (!state.hasBlockEntity()) blocks.setBlockState(
                    pos.x - minX,
                    pos.y - minY,
                    pos.z - minZ,
                    state
                )
            }

            return SubGridCapture(
                blocks,
                LongOpenHashSet(captured.keys),
                BlockPos(minX, minY, minZ),
                Vec3i(sizeX, sizeY, sizeZ),
                engines
            )
        }

        private fun isConnected(
            direction: Direction,
            shape: VoxelShape,
            neighborShape: VoxelShape,
            state: BlockState,
            neighborState: BlockState
        ): Boolean {
            return !(!isStickyInDirection(state, direction) && !isStickyInDirection(
                neighborState,
                direction.opposite
            )) || areShapesConnected(direction, shape, neighborShape) && !isNonStickyInDirection(
                state,
                direction
            ) && !isNonStickyInDirection(neighborState, direction.opposite)
        }

        private fun areShapesConnected(direction: Direction, shape: VoxelShape, neighborShape: VoxelShape): Boolean {
            if (shape !== Shapes.empty() && neighborShape !== Shapes.empty()) {
                val face = shape.getFaceShape(direction)
                val neighborFace = neighborShape.getFaceShape(direction.opposite)
                return face === Shapes.block() && neighborFace === Shapes.block() || Shapes.joinIsNotEmpty(
                    face,
                    neighborFace,
                    BooleanOp.AND
                )
            } else {
                return false
            }
        }

        private fun isNonStickyInDirection(state: BlockState, direction: Direction?): Boolean {
            return state.`is`(CelestiumBlocks.FLOATATER) && state.getValue(FloataterBlock.FACING) != direction
        }

        private fun isStickyInDirection(state: BlockState, direction: Direction?): Boolean {
            return !(!state.`is`(Blocks.SLIME_BLOCK) && !state.`is`(Blocks.HONEY_BLOCK))
                    || (state.`is`(Blocks.STICKY_PISTON) && state.getValue(BlockStateProperties.FACING) == direction
                    || state.`is`(CelestiumBlocks.FLOATATER) && state.getValue(FloataterBlock.FACING) == direction)
        }
    }
}
