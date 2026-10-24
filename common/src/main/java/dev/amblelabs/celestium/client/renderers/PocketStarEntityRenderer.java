package dev.amblelabs.celestium.client.renderers;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.amblelabs.celestium.Celestium;
import dev.amblelabs.celestium.client.model.PocketStarEntityModel;
import dev.amblelabs.celestium.core.entities.PocketStarEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class PocketStarEntityRenderer extends MobRenderer<PocketStarEntity, LivingEntityRenderState, PocketStarEntityModel> {

    private static final Identifier TEXTURE = Celestium.id("textures/entity/pocket_star.png");
    private static final RenderType EMISSIVE = RenderTypes.eyes(Celestium.id("textures/entity/pocket_star.png"));
    private static final RenderType RAYS_RENDER_TYPE = RenderTypes.dragonRays();

    private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0F) / 2.0F);
    private static final float BEAM_POS_X = 0.0F;
    private static final float BEAM_POS_Y = 1.45F;
    private static final float BEAM_POS_Z = 0.0F;
    private static final float BEAM_LENGTH = 5.0F;

    public PocketStarEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new PocketStarEntityModel(context.bakeLayer(PocketStarEntityModel.LAYER_LOCATION)), 0.3f);
        this.addLayer(new EyesLayer<>(this) {
            @Override public RenderType renderType() {
                return EMISSIVE;
            }
        });
    }

    @Override
    public void extractRenderState(PocketStarEntity entity, LivingEntityRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.deathTime = 0.0f;
        state.hasRedOverlay = false;
    }

    @Override
    public LivingEntityRenderState createRenderState() {
        return new LivingEntityRenderState();
    }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) {
        return TEXTURE;
    }

    @Override
    public void submit(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {

        float starY = getY(state.ageInTicks);
        poseStack.pushPose();
        poseStack.translate(BEAM_POS_X, BEAM_POS_Y + starY, BEAM_POS_Z);
        submitRays(poseStack, state.ageInTicks, collector, RAYS_RENDER_TYPE);

        poseStack.pushPose();
        poseStack.translate(0.0, -0.6, 0.0);
        super.submit(state, poseStack, collector, camera);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void submitRays(final PoseStack poseStack, final float timeInTicks, final SubmitNodeCollector submitNodeCollector, final RenderType renderType) {
        submitNodeCollector.submitCustomGeometry(poseStack, renderType, (pose, buffer) -> {
            float pulse = Mth.sin(timeInTicks * 0.2F) * 0.5F + 0.5F;
            int innerColor = ARGB.colorFromFloat(1.0F, 1.0F, 1.0F, 1.0F);
            int outerColor = 24721935;

            RandomSource random = RandomSource.createThreadLocalInstance(432L);

            Vector3f origin = new Vector3f();
            Vector3f outerLeft = new Vector3f();
            Vector3f outerRight = new Vector3f();
            Vector3f outerBottom = new Vector3f();
            Quaternionf rayRotation = new Quaternionf();

            int rayCount = 15;

            for(int i = 0; i < rayCount; ++i) {
                rayRotation.rotationXYZ(random.nextFloat() * ((float)Math.PI * 2F), random.nextFloat() * ((float)Math.PI * 2F), random.nextFloat() * ((float)Math.PI * 2F))
                        .rotateXYZ(random.nextFloat() * ((float)Math.PI * 2F), random.nextFloat() * ((float)Math.PI * 2F), random.nextFloat() * ((float)Math.PI * 2F) + timeInTicks * ((float)Math.PI / 180F));

                pose.rotate(rayRotation);
                float length = BEAM_LENGTH * (1.0F + pulse * 0.2F);
                float width = (random.nextFloat() + 0.5F);

                outerLeft.set(-HALF_SQRT_3 * width, length, -0.5F * width);
                outerRight.set(HALF_SQRT_3 * width, length, -0.5F * width);
                outerBottom.set(0.0F, length, width);

                buffer.addVertex(pose, origin).setColor(innerColor);
                buffer.addVertex(pose, outerLeft).setColor(outerColor);
                buffer.addVertex(pose, outerRight).setColor(outerColor);

                buffer.addVertex(pose, origin).setColor(innerColor);
                buffer.addVertex(pose, outerRight).setColor(outerColor);
                buffer.addVertex(pose, outerBottom).setColor(outerColor);

                buffer.addVertex(pose, origin).setColor(innerColor);
                buffer.addVertex(pose, outerBottom).setColor(outerColor);
                buffer.addVertex(pose, outerLeft).setColor(outerColor);
            }
        });
    }

    public static float getY(final float timeInTicks) {
        float hh = Mth.sin((double)(timeInTicks * 0.05F)) / 2.0F + 0.5F;
        hh = (hh * hh + hh) * 0.4F;
        return hh - 1.4F;
    }

}