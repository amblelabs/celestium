package dev.amblelabs.celestium.client.grid

import net.minecraft.client.renderer.block.MovingBlockRenderState
import net.minecraft.core.BlockPos
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.FluidState

class GridBlockRenderState(private val mesh: GridMesh, localPos: BlockPos, state: BlockState) : MovingBlockRenderState() {
    init {
        this.blockPos = localPos
        this.randomSeedPos = localPos
        this.blockState = state
    }

    override fun getBlockState(pos: BlockPos): BlockState {
        return this.mesh.blocks.getBlockState(pos)
    }

    override fun getFluidState(pos: BlockPos): FluidState {
        return this.getBlockState(pos).fluidState
    }

    override fun getBrightness(layer: LightLayer, pos: BlockPos): Int {
        return this.mesh.worldBrightness(this, layer, pos)
    }

    override fun getHeight(): Int {
        return this.mesh.blocks.sizeY
    }

    override fun getMinY(): Int {
        return 0
    }
}
