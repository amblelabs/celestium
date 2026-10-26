package dev.amblelabs.celestium.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.amblelabs.celestium.init.CelestiumDimensions;
import dev.amblelabs.celestium.space.AstralBody;
import dev.amblelabs.celestium.space.AstralBodyRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpaceSkyRenderer {

    private SpaceSkyRenderer() {}

    public static void render(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;

        float blocks = Math.max(48, mc.options.getEffectiveRenderDistance() * 16);
        double ticks = level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float seconds = (float) ((ticks % 720000.0) / 20.0);
        PoseStack poses = context.poseStack();
        SubmitNodeCollector out = context.submitNodeCollector();

        if (level.dimension() == CelestiumDimensions.SPACE_KEY) {
            renderStars(poses, out, blocks * 3.2f, 1.0f, seconds);
            renderStarSystemEntries(poses, out, seconds);
        }
    }

    private static final Catalog CATALOG = Catalog.generate();

    private static class Catalog {
        public static final int STARS = 400;
        private static final int[] STAR_COLORS = {0x9DB4FF, 0xCAD8FF, 0xF8F7FF, 0xFFF4EA, 0xFFE8A0, 0xFFB46B, 0xFF7A4A};
        public List<Star> stars;

        public Catalog(List<Star> stars) {
            this.stars = stars;
        }

        public List<Star> stars() {
            return this.stars;
        }

        public static Catalog generate() {
            Random random = new Random(0x2814_0000_0666_0001L + 10 * 0x9E3779B97F4A7C15L);
            ArrayList<Star> stars = new ArrayList<>();
            for (int i = 0; i < STARS; i++) {
                float yaw = random.nextFloat() * 360.0F;
                float pitch = (float) Math.toDegrees(Math.asin(random.nextFloat() * 1.9F - 0.95F));
                float size = 0.12F + random.nextFloat() * random.nextFloat() * 0.45F;
                stars.add(new Star(yaw, pitch, size, STAR_COLORS[random.nextInt(STAR_COLORS.length)]));
            }
            return new Catalog(List.copyOf(stars));
        }
    }

    public static class Star {
        public float yaw;
        public float pitch;
        public float size;
        public int color;

        public Star(float yaw, float pitch, float size, int color) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.size = size;
            this.color = color;
        }

        public float yaw() {
            return this.yaw;
        }

        public float pitch() {
            return this.pitch;
        }

        public float size() {
            return this.size;
        }

        public int color() {
            return this.color;
        }
    }

    private static void renderStars(PoseStack poses, SubmitNodeCollector out, float distance, float alpha, float seconds) {
        if (alpha <= 0.01F) return;
        out.submitCustomGeometry(poses, CelestiumRenderTypes.SKY_GLOW, (pose, buffer) -> {
            int index = 0;
            for (Star star : CATALOG.stars()) {
                index++;
                Vector3f dir = new Quaternionf().transform(direction(star.yaw(), star.pitch())).mul(distance);
                float twinkle = 0.75F + 0.25F * Mth.sin(seconds * (1.5F + (index % 7) * 0.4F) + index);
                float shown = alpha * twinkle;
                if (shown <= 0.01F) continue;
                float half = distance * (float) Math.tan(Math.toRadians(star.size() * 0.35f));
                Vector3f facing = new Vector3f(dir).normalize();
                Vector3f right = new Vector3f(facing).cross(0.0F, 1.0F, 0.0F);
                if (right.lengthSquared() < 1.0E-4F) right.set(1.0F, 0.0F, 0.0F);
                right.normalize().mul(half);
                Vector3f up = new Vector3f(right).cross(facing).normalize().mul(half);
                point(buffer, pose, dir, right, up, ARGB.color(Math.round(255 * shown), star.color()));
                point(buffer, pose, dir, right.mul(2.5f), up.mul(2.5f), ARGB.color(Math.round(70 * shown), star.color()));
            }
        });
    }

    private static void renderStarSystemEntries(PoseStack poses, SubmitNodeCollector out, float seconds) {
        out.submitCustomGeometry(poses, CelestiumRenderTypes.SKY_GLOW,
                (pose, buffer) -> renderSystem(buffer, pose, seconds, true));
        out.submitCustomGeometry(poses, CelestiumRenderTypes.SKY_SOLID,
                (pose, buffer) -> renderSystem(buffer, pose, seconds, false));
    }

    private static void renderSystem(VertexConsumer buffer, PoseStack.Pose pose, float seconds, boolean starPass) {
        Vec3 cam = Minecraft.getInstance().gameRenderer.mainCamera().position();

        PoseStack.Pose origin = pose.copy();
        origin.translate((float) -cam.x, (float) -cam.y + 300, (float) -cam.z);

        AstralBodyRegistry.ASTRAL_BODY_REGISTRY.stream()
                .filter(body -> !body.isSatellite())
                .forEach(body -> renderBody(buffer, origin, body, seconds, starPass));
    }

    private static void renderBody(VertexConsumer buffer, PoseStack.Pose parent, AstralBody body, float seconds, boolean starPass) {
        PoseStack.Pose orbit = parent.copy();
        orbit.rotate(Axis.YP, Mth.wrapDegrees(seconds * body.orbit().speed() * 2f));
        orbit.translate(body.orbit().distanceFromCenter(), 0, 0);

        for (AstralBody satellite : body.satellites()) {
            renderBody(buffer, orbit, satellite, seconds, starPass);
        }

        if (body.isStar() != starPass) return;

        PoseStack.Pose self = orbit.copy();
        self.rotate(Axis.XP, body.axis());

        point(buffer, self, body.sizeX(), body.sizeY(), body.sizeZ(), body.defaultColor());
        if (starPass) {
            point(buffer, self, body.sizeX() * 1.1f, body.sizeY() * 1.1f, body.sizeZ() * 1.1f, body.defaultColor());
        }
    }

    private static final int[][] FACES = {
            {4, 5, 7, 6}, {1, 0, 2, 3},
            {0, 4, 6, 2}, {5, 1, 3, 7},
            {2, 6, 7, 3}, {0, 1, 5, 4}
    };

    private static void point(VertexConsumer buffer, PoseStack.Pose pose,
                              float sizeX, float sizeY, float sizeZ, int color) {
        float hx = sizeX / 2f, hy = sizeY / 2f, hz = sizeZ / 2f;
        for (int[] face : FACES) {
            for (int i : face) {
                buffer.addVertex(pose,((i & 1) != 0 ? hx : -hx),((i & 2) != 0 ? hy : -hy),((i & 4) != 0 ? hz : -hz)).setColor(color);
            }
        }
    }

    private static void point(VertexConsumer buffer, PoseStack.Pose pose, Vector3f center, Vector3f right, Vector3f up, int color) {
        buffer.addVertex(pose, center.x - right.x - up.x, center.y - right.y - up.y, center.z - right.z - up.z).setColor(color);
        buffer.addVertex(pose, center.x + right.x - up.x, center.y + right.y - up.y, center.z + right.z - up.z).setColor(color);
        buffer.addVertex(pose, center.x + right.x + up.x, center.y + right.y + up.y, center.z + right.z + up.z).setColor(color);
        buffer.addVertex(pose, center.x - right.x + up.x, center.y - right.y + up.y, center.z - right.z + up.z).setColor(color);
    }

    private static Vector3f direction(float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        return new Vector3f((float) (Math.cos(pitch) * Math.sin(yaw)), (float) Math.sin(pitch), (float) (-Math.cos(pitch) * Math.cos(yaw)));
    }
}
