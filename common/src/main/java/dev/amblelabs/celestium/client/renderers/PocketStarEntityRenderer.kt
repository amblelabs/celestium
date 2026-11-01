package dev.amblelabs.celestium.client.renderers

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import dev.amblelabs.celestium.Celestium
import dev.amblelabs.celestium.client.model.PocketStarEntityModel
import dev.amblelabs.celestium.world.level.entity.PocketStarEntity
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.MobRenderer
import net.minecraft.client.renderer.entity.layers.EyesLayer
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.resources.Identifier
import net.minecraft.util.ARGB
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import org.joml.Quaternionf
import org.joml.Vector3f

class PocketStarEntityRenderer(context: EntityRendererProvider.Context) : MobRenderer<PocketStarEntity, LivingEntityRenderState, PocketStarEntityModel>(
        context, PocketStarEntityModel(
            context.bakeLayer(
                PocketStarEntityModel.LAYER_LOCATION
            )
        ), 0.3f
    ) {

    init {
        this.addLayer(object : EyesLayer<LivingEntityRenderState, PocketStarEntityModel>(this) {
            override fun renderType(): RenderType {
                return EMISSIVE
            }
        })
    }

    override fun extractRenderState(entity: PocketStarEntity, state: LivingEntityRenderState, partialTicks: Float) {
        super.extractRenderState(entity, state, partialTicks)
        state.deathTime = 0.0f
        state.hasRedOverlay = false
    }

    override fun createRenderState(): LivingEntityRenderState {
        return LivingEntityRenderState()
    }

    override fun getTextureLocation(state: LivingEntityRenderState): Identifier {
        return TEXTURE
    }

    override fun submit(
        state: LivingEntityRenderState,
        poseStack: PoseStack,
        collector: SubmitNodeCollector,
        camera: CameraRenderState
    ) {
        val starY: Float = getY(state.ageInTicks)
        poseStack.pushPose()
        poseStack.translate(BEAM_POS_X, BEAM_POS_Y + starY, BEAM_POS_Z)
        submitRays(poseStack, state.ageInTicks, collector, RAYS_RENDER_TYPE)

        poseStack.pushPose()
        poseStack.translate(0.0, -0.6, 0.0)
        super.submit(state, poseStack, collector, camera)
        poseStack.popPose()

        poseStack.popPose()
    }

    companion object {
        private val TEXTURE: Identifier = Celestium.id("textures/entity/pocket_star.png")
        private val EMISSIVE = RenderTypes.eyes(TEXTURE)
        private val RAYS_RENDER_TYPE = RenderTypes.dragonRays()

        private val HALF_SQRT_3 = Mth.sqrt(3.0f) / 2.0f
        private const val BEAM_POS_X = 0.0f
        private const val BEAM_POS_Y = 1.45f
        private const val BEAM_POS_Z = 0.0f
        private const val BEAM_LENGTH = 5.0f

        private val RAY_INNER_COLOR = ARGB.white(255)
        private const val RAY_OUTER_COLOR = 24721935
        private const val RAY_COUNT = 15

        private fun submitRays(
            poseStack: PoseStack,
            timeInTicks: Float,
            submitNodeCollector: SubmitNodeCollector,
            renderType: RenderType
        ) {
            submitNodeCollector.submitCustomGeometry(
                poseStack,
                renderType
            ) { pose: PoseStack.Pose?, buffer: VertexConsumer? ->
                val pulse = Mth.sin((timeInTicks * 0.2f).toDouble()) * 0.5f + 0.5f
                val random = RandomSource.createThreadLocalInstance(432L)

                val origin = Vector3f()
                val outerLeft = Vector3f()
                val outerRight = Vector3f()
                val outerBottom = Vector3f()
                val rayRotation = Quaternionf()
                repeat(RAY_COUNT) {
                    rayRotation.rotationXYZ(
                        random.nextFloat() * (Mth.PI * 2f),
                        random.nextFloat() * (Mth.PI * 2f),
                        random.nextFloat() * (Mth.PI * 2f)
                    )
                        .rotateXYZ(
                            random.nextFloat() * (Mth.PI * 2f),
                            random.nextFloat() * (Mth.PI * 2f),
                            random.nextFloat() * (Mth.PI * 2f) + timeInTicks * (Mth.PI / 180f)
                        )

                    pose!!.rotate(rayRotation)
                    val length: Float = BEAM_LENGTH * (1.0f + pulse * 0.2f)
                    val width = (random.nextFloat() + 0.5f)

                    outerLeft.set(-HALF_SQRT_3 * width, length, -0.5f * width)
                    outerRight.set(HALF_SQRT_3 * width, length, -0.5f * width)
                    outerBottom.set(0.0f, length, width)

                    buffer!!.addVertex(pose, origin).setColor(RAY_INNER_COLOR)
                    buffer.addVertex(pose, outerLeft).setColor(RAY_OUTER_COLOR)
                    buffer.addVertex(pose, outerRight).setColor(RAY_OUTER_COLOR)

                    buffer.addVertex(pose, origin).setColor(RAY_INNER_COLOR)
                    buffer.addVertex(pose, outerRight).setColor(RAY_OUTER_COLOR)
                    buffer.addVertex(pose, outerBottom).setColor(RAY_OUTER_COLOR)

                    buffer.addVertex(pose, origin).setColor(RAY_INNER_COLOR)
                    buffer.addVertex(pose, outerBottom).setColor(RAY_OUTER_COLOR)
                    buffer.addVertex(pose, outerLeft).setColor(RAY_OUTER_COLOR)
                }
            }
        }

        fun getY(timeInTicks: Float): Float {
            var hh = Mth.sin((timeInTicks * 0.05f).toDouble()) / 2.0f + 0.5f
            hh = (hh * hh + hh) * 0.4f
            return hh - 1.4f
        }
    }
}