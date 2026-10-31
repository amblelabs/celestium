package dev.amblelabs.celestium.world.level.block

import dev.amblelabs.celestium.Celestium
import dev.amblelabs.celestium.init.CelestiumEntityTypes
import dev.amblelabs.celestium.world.grid.FlyingTickable
import dev.amblelabs.celestium.world.grid.GridCarrier
import dev.amblelabs.celestium.world.grid.SubGridBlocks
import dev.amblelabs.celestium.world.grid.SubGridCapture
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.redstone.Orientation
import net.minecraft.world.phys.Vec3

class FloataterBlock(properties: Properties) : Block(properties), FlyingTickable {
    init {
        this.registerDefaultState(
            this.stateDefinition.any().setValue(FACING, Direction.NORTH)
                .setValue(
                    TRIGGERED, false
                ).setValue(POWERED, false)
        )
    }

    override fun getStateForPlacement(blockPlaceContext: BlockPlaceContext): BlockState {
        return this.defaultBlockState().setValue(
            FACING,
            if (blockPlaceContext.player!!.isCrouching) blockPlaceContext.nearestLookingDirection
                .opposite else blockPlaceContext.nearestLookingDirection
        )
    }

    override fun rotate(blockState: BlockState, rotation: Rotation): BlockState {
        return blockState.setValue(
            FACING, rotation.rotate(
                blockState.getValue(
                    FACING
                )
            )
        )
    }

    override fun mirror(blockState: BlockState, mirror: Mirror): BlockState {
        return blockState.rotate(mirror.getRotation(blockState.getValue(FACING)))
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        orientation: Orientation?,
        movedByPiston: Boolean
    ) {
        var state = state
        val powered = level.hasNeighborSignal(pos)

        if (powered != state.getValue(TRIGGERED)) {
            if (powered) {
                state = state.cycle(POWERED)
                if (state.getValue(POWERED)) level.scheduleTick(pos, this, 1)
            }
            level.setBlock(pos, state.setValue(TRIGGERED, powered), UPDATE_CLIENTS)
        }
    }

    override fun tick(blockState: BlockState, level: ServerLevel, blockPos: BlockPos, randomSource: RandomSource) {
        if (!blockState.getValue(POWERED)) return
        val direction = blockState.getValue(FACING)
        val capture = SubGridCapture.scan(level, blockPos, direction)

        if (capture != null) {
            val carrier = GridCarrier(CelestiumEntityTypes.GRID_CARRIER, level)
            val minPos = capture.minPos

            carrier.snapTo(minPos.x.toDouble(), minPos.y.toDouble(), minPos.getZ().toDouble())
            carrier.grid().blocks = capture.blocks
            carrier.yRot = direction.toYRot()
            carrier.grid().biome = level.getBiome(blockPos)
            carrier.setMovement(direction, capture.engines * 0.1f)
            capture.remove(level)

            level.addFreshEntity(carrier)
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING, TRIGGERED, POWERED)
    }

    override fun flyingTick(
        level: Level,
        blocks: SubGridBlocks,
        state: BlockState,
        pos: BlockPos,
        worldPos: Vec3,
        movement: Direction
    ) {
        var worldPos = worldPos
        if (level.isClientSide) {
            val facing = state.getValue(FACING)
            if (movement == facing && state.getValue(TRIGGERED) && level.getRandom().nextBoolean()) {
                val exhaust = facing.opposite
                if (blocks.getBlockState(pos.relative(exhaust)).isAir) {
                    val d = 0.5
                    worldPos = worldPos.add(d)
                        .add(exhaust.stepX * d, exhaust.stepY * d, exhaust.stepZ * d)

                    level.addParticle(Celestium.THRUSTER, worldPos.x, worldPos.y, worldPos.z, 0.0, 0.0, 0.0)
                }
            }
        }
    }

    companion object {
        @JvmField
        val FACING: EnumProperty<Direction> = DirectionalBlock.FACING

        @JvmField
        val TRIGGERED: BooleanProperty = BlockStateProperties.TRIGGERED
        val POWERED: BooleanProperty = BlockStateProperties.POWERED
    }
}