package dev.amblelabs.celestium.client.grid

import com.mojang.blaze3d.vertex.PoseStack
import dev.amblelabs.celestium.world.grid.GridCarrier
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB

class GridCarrierRenderer(context: EntityRendererProvider.Context) : EntityRenderer<GridCarrier, GridCarrierRenderState>(context) {
    override fun createRenderState(): GridCarrierRenderState {
        return GridCarrierRenderState()
    }

    override fun getBoundingBoxForCulling(carrier: GridCarrier, partialTicks: Float): AABB {
        val x = Mth.lerp(partialTicks.toDouble(), carrier.xOld, carrier.x)
        val y = Mth.lerp(partialTicks.toDouble(), carrier.yOld, carrier.y)
        val z = Mth.lerp(partialTicks.toDouble(), carrier.zOld, carrier.z)

        val blocks = carrier.grid().blocks
        return AABB(x, y, z, x + blocks.sizeX + 1, y + blocks.sizeY + 1, z + blocks.sizeZ + 1)
            .inflate(CULL_BUFFER_SIZE)
    }

    override fun extractRenderState(carrier: GridCarrier, state: GridCarrierRenderState, partialTicks: Float) {
        super.extractRenderState(carrier, state, partialTicks)

        val level = carrier.level()
        if (level is ClientLevel) {
            val mesh = GridMesh.of(carrier.grid(), level)
            mesh.origin = BlockPos.containing(state.x + 0.5, state.y + 0.5, state.z + 0.5)
            state.blocks = mesh.renderStates
        }
    }

    override fun submit(
        state: GridCarrierRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        for (block in state.blocks) {
            poseStack.pushPose()
            poseStack.translate(
                block.blockPos.x.toFloat(),
                block.blockPos.y.toFloat(),
                block.blockPos.z.toFloat()
            )
            submitNodeCollector.submitMovingBlock(poseStack, block, 0)
            poseStack.popPose()
        }

        super.submit(state, poseStack, submitNodeCollector, camera)
    }

    companion object {
        private const val CULL_BUFFER_SIZE = 3.0
    }
}
