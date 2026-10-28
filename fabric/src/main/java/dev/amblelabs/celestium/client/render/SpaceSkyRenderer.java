package dev.amblelabs.celestium.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.amblelabs.celestium.client.starlight.Starlight;
import dev.amblelabs.celestium.init.CelestiumDimensions;
import dev.amblelabs.celestium.space.AstralBody;
import dev.amblelabs.celestium.space.StarSystem;
import dev.amblelabs.celestium.space.StarSystemRegistry;
import dev.amblelabs.celestium.util.SkyFadeUtil;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class SpaceSkyRenderer {

    private static final Identifier PLANET_TEXTURE = Identifier.withDefaultNamespace("textures/block/diorite.png");
    private static final RenderType PLANET_SURFACE = CelestiumRenderTypes.planetSurface(PLANET_TEXTURE);

    private static final int STAR_COUNT = 400;
    private static final int[] STAR_COLORS = {0x9DB4FF, 0xCAD8FF, 0xF8F7FF, 0xFFF4EA, 0xFFE8A0, 0xFFB46B, 0xFF7A4A};
    private static final List<Star> STARS = generateStars();

    private static final float MAX_RENDER_FRACTION = 0.8F;

    // TODO: make config option
    private static final int LIGHTING_REFRESH_TICKS = 20;

    private static final Map<Identifier, Lit> LIT = new HashMap<>();

    private static final float ATMOSPHERE_INNER_SCALE = 1.06F;
    private static final float ATMOSPHERE_OUTER_SCALE = 1.4F;
    private static final float ATMOSPHERE_INNER_ALPHA = 0.3F;
    private static final float ATMOSPHERE_OUTER_ALPHA = 0.06F;

    private static final int WHITE = 0xFFFFFF;
    private static final float STAR_CORE_WHITENESS = 0.35F;
    private static final int STAR_GLOW_LAYERS = 2;
    private static final float STAR_GLOW_INNER_SCALE = 1.2F;
    private static final float STAR_GLOW_OUTER_SCALE = 1.5F;
    private static final float STAR_GLOW_INNER_ALPHA = 0.35F;
    private static final float STAR_GLOW_OUTER_ALPHA = 0.1F;
    private static final float STAR_PULSE_SPEED = 0.4F;
    private static final float STAR_PULSE_AMOUNT = 0.03F;

    private static final float HALF_CUBE_DIAGONAL = 0.8660254F;
    private static final double TWO_PI = Math.PI * 2.0;

    private static final int[][] FACES = {
            {4, 5, 7, 6}, {1, 0, 2, 3},
            {0, 4, 6, 2}, {5, 1, 3, 7},
            {2, 6, 7, 3}, {0, 1, 5, 4}
    };
    private static final float[][] FACE_NORMALS = {
            {0, 0, 1}, {0, 0, -1},
            {-1, 0, 0}, {1, 0, 0},
            {0, 1, 0}, {0, -1, 0}
    };

    private SpaceSkyRenderer() {}

    public static void render(LevelRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        boolean isSpace = level.dimension().equals(CelestiumDimensions.SPACE_KEY);

        float starAlpha = isSpace ? 1.0F : SkyFadeUtil.space(mc.gameRenderer.mainCamera().position().y);
        if (starAlpha <= 0.01F) return;

        float blocks = Math.max(48, mc.options.getEffectiveRenderDistance() * 16);
        double ticks = level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float starSeconds = (float) ((ticks % 720000.0) / 20.0);
        PoseStack poses = context.poseStack();
        SubmitNodeCollector out = context.submitNodeCollector();

        renderStars(poses, out, blocks * 3.2F, starAlpha, starSeconds);
        if (isSpace) {
            renderSystem(poses, out, ticks, blocks * MAX_RENDER_FRACTION);
        }
    }

    public record Star(float yaw, float pitch, float size, int color) {}

    private enum Pass { GLOW, SOLID }

    private record Frame(VertexConsumer buffer, double ticks, Pass pass, float maxRenderDistance,
                         Map<Identifier, Starlight.Scene> scenes, Vector3d camera) {}

    private static List<Star> generateStars() {
        Random random = new Random(0x2814_0000_0666_0001L + 10 * 0x9E3779B97F4A7C15L);
        List<Star> stars = new ArrayList<>(STAR_COUNT);
        for (int i = 0; i < STAR_COUNT; i++) {
            float yaw = random.nextFloat() * 360.0F;
            float pitch = (float) Math.toDegrees(Math.asin(random.nextFloat() * 1.9F - 0.95F));
            float size = 0.12F + random.nextFloat() * random.nextFloat() * 0.45F;
            stars.add(new Star(yaw, pitch, size, STAR_COLORS[random.nextInt(STAR_COLORS.length)]));
        }
        return List.copyOf(stars);
    }

    private static void renderStars(PoseStack poses, SubmitNodeCollector out, float distance, float alpha, float seconds) {
        if (alpha <= 0.01F) return;
        out.submitCustomGeometry(poses, CelestiumRenderTypes.SKY_GLOW, (pose, buffer) -> {
            for (int i = 0; i < STARS.size(); i++) {
                Star star = STARS.get(i);
                int index = i + 1;
                float twinkle = 0.75F + 0.25F * Mth.sin(seconds * (1.5F + (index % 7) * 0.4F) + index);
                float shown = alpha * twinkle;
                if (shown <= 0.01F) continue;

                Vector3f center = direction(star.yaw(), star.pitch()).mul(distance);
                float half = distance * (float) Math.tan(Math.toRadians(star.size() * 0.35F));
                Vector3f right = new Vector3f();
                Vector3f up = new Vector3f();
                billboardBasis(new Vector3f(center).normalize(), right, up);
                right.mul(half);
                up.mul(half);

                quad(buffer, pose, center, right, up, ARGB.color(Math.round(255 * shown), star.color()));
                quad(buffer, pose, center, right.mul(2.5F), up.mul(2.5F), ARGB.color(Math.round(70 * shown), star.color()));
            }
        });
    }

    private static void renderSystem(PoseStack poses, SubmitNodeCollector out, double ticks, float maxRenderDistance) {
        ProfilerFiller profiler = Profiler.get();
        profiler.push("celestium_starlight_scenes");
        Map<Identifier, Starlight.Scene> scenes = new HashMap<>();
        for (StarSystem starSystem : StarSystemRegistry.STAR_SYSTEM_REGISTRY) {
            scenes.put(starSystem.id(), Starlight.scene(starSystem, ticks));
        }
        profiler.pop();
        Vec3 cam = Minecraft.getInstance().gameRenderer.mainCamera().position();
        Vector3d camera = new Vector3d(cam.x, cam.y, cam.z);

        out.submitCustomGeometry(poses, PLANET_SURFACE, (pose, buffer) -> {
            Profiler.get().push("celestium_planet_surfaces");
            renderBodies(new Frame(buffer, ticks, Pass.SOLID, maxRenderDistance, scenes, camera), pose);
            Profiler.get().pop();
        });
        out.submitCustomGeometry(poses, CelestiumRenderTypes.SKY_GLOW, (pose, buffer) -> {
            Profiler.get().push("celestium_planet_glow");
            renderBodies(new Frame(buffer, ticks, Pass.GLOW, maxRenderDistance, scenes, camera), pose);
            Profiler.get().pop();
        });
    }

    private static void renderBodies(Frame frame, PoseStack.Pose root) {
        for (StarSystem starSystem : StarSystemRegistry.STAR_SYSTEM_REGISTRY) {
            Starlight.Scene scene = frame.scenes().get(starSystem.id());
            for (Starlight.Body state : scene.bodies().values()) {
                renderBody(frame, scene, root, state);
            }
        }
    }

    private static void renderBody(Frame frame, Starlight.Scene scene, PoseStack.Pose root, Starlight.Body state) {
        AstralBody body = state.body();
        Matrix4d transform = state.transform();
        Vector3d cam = frame.camera();
        float size = Math.max(body.sizeX(), Math.max(body.sizeY(), body.sizeZ()));
        double angular = size / Math.max(1.0, cam.distance(transform.m30(), transform.m31(), transform.m32()));
        if (!body.isStar() && angular < 0.0005) return;
        int detail = angular > 0.2 ? 12 : angular > 0.05 ? 8 : angular > 0.01 ? 4 : 1;

        PoseStack.Pose self = root.copy();
        self.translate((float) (transform.m30() - cam.x), (float) (transform.m31() - cam.y), (float) (transform.m32() - cam.z));
        self.rotate(new Quaternionf(transform.getNormalizedRotation(new Quaterniond())));

        float shell = body.isStar()
                ? STAR_GLOW_OUTER_SCALE * (1.0F + STAR_PULSE_AMOUNT)
                : body.atmosphere() != null ? ATMOSPHERE_OUTER_SCALE : 1.0F;
        float farthest = self.pose().getTranslation(new Vector3f()).length() + size * HALF_CUBE_DIAGONAL * shell;
        if (farthest > frame.maxRenderDistance()) {
            self.pose().scaleLocal(frame.maxRenderDistance() / farthest);
        }

        switch (frame.pass()) {
            case GLOW -> {
                if (body.isStar()) {
                    renderStarBody(frame, self, body);
                } else if (body.atmosphere() != null) {
                    renderAtmosphere(frame, scene, self, body, detail);
                }
            }
            case SOLID -> {
                if (!body.isStar()) {
                    renderSurface(frame, scene, self, body, detail);
                }
            }
        }
    }

    private static void renderStarBody(Frame frame, PoseStack.Pose self, AstralBody body) {
        VertexConsumer buffer = frame.buffer();
        int color = body.defaultColor();
        int hot = mix(color, WHITE, STAR_CORE_WHITENESS);
        float pulsePhase = (float) ((frame.ticks() / 20.0 * STAR_PULSE_SPEED) % TWO_PI);
        float pulse = 1.0F + STAR_PULSE_AMOUNT * Mth.sin(pulsePhase);

        cube(buffer, self, body.sizeX(), body.sizeY(), body.sizeZ(), ARGB.opaque(hot));

        for (int i = 0; i < STAR_GLOW_LAYERS; i++) {
            float t = STAR_GLOW_LAYERS == 1 ? 0.0F : (float) i / (STAR_GLOW_LAYERS - 1);
            float scale = Mth.lerp(t, STAR_GLOW_INNER_SCALE, STAR_GLOW_OUTER_SCALE) * pulse;
            float alpha = Mth.lerp(t, STAR_GLOW_INNER_ALPHA, STAR_GLOW_OUTER_ALPHA);
            cube(buffer, self,
                    body.sizeX() * scale,
                    body.sizeY() * scale,
                    body.sizeZ() * scale,
                    ARGB.color(Math.round(255 * alpha), mix(hot, color, t)));
        }
    }

    private static void renderSurface(Frame frame, Starlight.Scene scene, PoseStack.Pose self, AstralBody body, int detail) {
        Starlight.Body state = scene.body(body);
        Lit lit = LIT.computeIfAbsent(body.id(), id -> new Lit());
        long bucket = refreshBucket(body, frame.ticks());
        if (lit.surface == null || lit.surfaceDetail != detail || lit.surfaceBucket != bucket) {
            Matrix4d transform = state.transform();
            float hx = body.sizeX() / 2F, hy = body.sizeY() / 2F, hz = body.sizeZ() / 2F;
            Vector3d[] faceNormals = new Vector3d[FACES.length];
            for (int f = 0; f < FACES.length; f++) {
                float[] n = FACE_NORMALS[f];
                faceNormals[f] = transform.transformDirection(new Vector3d(n[0], n[1], n[2])).normalize();
            }
            Vector3d world = new Vector3d();
            Vector3d sphereNormal = new Vector3d();
            lit.surface = shadeCube(body.sizeX(), body.sizeY(), body.sizeZ(), detail,
                    (face, x, y, z) -> Starlight.surface(scene, state,
                            transform.transformPosition(world.set(x, y, z)),
                            faceNormals[face],
                            transform.transformDirection(sphereNormal.set(x / hx, y / hy, z / hz)).normalize(),
                            body.defaultColor()));
            lit.surfaceDetail = detail;
            lit.surfaceBucket = bucket;
        }
        emitCube(frame.buffer(), self, body.sizeX(), body.sizeY(), body.sizeZ(), detail, true, lit.surface);
    }

    private static void renderAtmosphere(Frame frame, Starlight.Scene scene, PoseStack.Pose self, AstralBody body, int detail) {
        Starlight.Body state = scene.body(body);
        int layers = body.atmosphere().layers();
        Lit lit = LIT.computeIfAbsent(body.id(), id -> new Lit());
        long bucket = refreshBucket(body, frame.ticks());
        Vector3d view = new Vector3d(frame.camera()).sub(state.center()).normalize();

        if (lit.atmosphere == null || lit.atmosphereDetail != detail || lit.atmosphereBucket != bucket || lit.view.dot(view) < 0.9998) {
            double radius = Math.max(body.sizeX(), Math.max(body.sizeY(), body.sizeZ())) / 2.0 * ATMOSPHERE_OUTER_SCALE;
            Vector3d world = new Vector3d();
            lit.atmosphere = new int[layers][];
            for (int i = 0; i < layers; i++) {
                float t = layers == 1 ? 0.0F : (float) i / (layers - 1);
                float scale = Mth.lerp(t, ATMOSPHERE_INNER_SCALE, ATMOSPHERE_OUTER_SCALE);
                float alpha = Mth.lerp(t, ATMOSPHERE_INNER_ALPHA, ATMOSPHERE_OUTER_ALPHA);
                lit.atmosphere[i] = shadeCube(body.sizeX() * scale, body.sizeY() * scale, body.sizeZ() * scale, detail,
                        (face, x, y, z) -> Starlight.atmosphere(scene, state,
                                state.transform().transformPosition(world.set(x, y, z)), frame.camera(), radius, alpha));
            }
            lit.atmosphereDetail = detail;
            lit.atmosphereBucket = bucket;
            lit.view.set(view);
        }

        for (int i = 0; i < layers; i++) {
            float scale = Mth.lerp(layers == 1 ? 0.0F : (float) i / (layers - 1), ATMOSPHERE_INNER_SCALE, ATMOSPHERE_OUTER_SCALE);
            emitCube(frame.buffer(), self, body.sizeX() * scale, body.sizeY() * scale, body.sizeZ() * scale, detail, false, lit.atmosphere[i]);
        }
    }

    private static long refreshBucket(AstralBody body, double ticks) {
        return (long) Math.floor((ticks + Math.floorMod(body.id().hashCode(), LIGHTING_REFRESH_TICKS)) / LIGHTING_REFRESH_TICKS);
    }

    private static int mix(int from, int to, float t) {
        return ARGB.color(
                Math.round(Mth.lerp(t, ARGB.red(from), ARGB.red(to))),
                Math.round(Mth.lerp(t, ARGB.green(from), ARGB.green(to))),
                Math.round(Mth.lerp(t, ARGB.blue(from), ARGB.blue(to))));
    }

    private static void billboardBasis(Vector3f facing, Vector3f right, Vector3f up) {
        right.set(facing).cross(0.0F, 1.0F, 0.0F);
        if (right.lengthSquared() < 1.0E-4F) right.set(1.0F, 0.0F, 0.0F);
        right.normalize();
        up.set(right).cross(facing).normalize();
    }

    private static void cube(VertexConsumer buffer, PoseStack.Pose pose, float sizeX, float sizeY, float sizeZ, int color) {
        float hx = sizeX / 2F, hy = sizeY / 2F, hz = sizeZ / 2F;
        for (int[] face : FACES) {
            for (int i : face) {
                buffer.addVertex(pose,
                        (i & 1) != 0 ? hx : -hx,
                        (i & 2) != 0 ? hy : -hy,
                        (i & 4) != 0 ? hz : -hz).setColor(color);
            }
        }
    }

    @FunctionalInterface
    private interface GridShader {
        int shade(int face, float x, float y, float z);
    }

    private static final class Lit {
        int[] surface;
        int surfaceDetail;
        long surfaceBucket;
        int[][] atmosphere;
        int atmosphereDetail;
        long atmosphereBucket;
        final Vector3d view = new Vector3d();
    }

    private static int[] shadeCube(float sizeX, float sizeY, float sizeZ, int n, GridShader shader) {
        float hx = sizeX / 2F, hy = sizeY / 2F, hz = sizeZ / 2F;
        int row = n + 1;
        int[] colors = new int[FACES.length * row * row];
        for (int f = 0; f < FACES.length; f++) {
            int[] face = FACES[f];
            for (int j = 0; j <= n; j++) {
                for (int i = 0; i <= n; i++) {
                    float s = (float) i / n, t = (float) j / n;
                    colors[(f * row + j) * row + i] = shader.shade(f,
                            facePoint(face, 1, hx, s, t), facePoint(face, 2, hy, s, t), facePoint(face, 4, hz, s, t));
                }
            }
        }
        return colors;
    }

    private static void emitCube(VertexConsumer buffer, PoseStack.Pose pose, float sizeX, float sizeY, float sizeZ,
                                 int n, boolean textured, int[] colors) {
        float hx = sizeX / 2F, hy = sizeY / 2F, hz = sizeZ / 2F;
        for (int f = 0; f < FACES.length; f++) {
            for (int j = 0; j < n; j++) {
                for (int i = 0; i < n; i++) {
                    gridVertex(buffer, pose, f, hx, hy, hz, colors, i, j, n, textured);
                    gridVertex(buffer, pose, f, hx, hy, hz, colors, i + 1, j, n, textured);
                    gridVertex(buffer, pose, f, hx, hy, hz, colors, i + 1, j + 1, n, textured);
                    gridVertex(buffer, pose, f, hx, hy, hz, colors, i, j + 1, n, textured);
                }
            }
        }
    }

    private static float facePoint(int[] face, int bit, float half, float s, float t) {
        float c0 = (face[0] & bit) != 0 ? half : -half;
        float c1 = (face[1] & bit) != 0 ? half : -half;
        float c2 = (face[2] & bit) != 0 ? half : -half;
        float c3 = (face[3] & bit) != 0 ? half : -half;
        return Mth.lerp(t, Mth.lerp(s, c0, c1), Mth.lerp(s, c3, c2));
    }

    private static void gridVertex(VertexConsumer buffer, PoseStack.Pose pose, int f, float hx, float hy, float hz,
                                   int[] colors, int i, int j, int n, boolean textured) {
        int[] face = FACES[f];
        float s = (float) i / n, t = (float) j / n;
        VertexConsumer vertex = buffer.addVertex(pose, facePoint(face, 1, hx, s, t), facePoint(face, 2, hy, s, t), facePoint(face, 4, hz, s, t))
                .setColor(colors[(f * (n + 1) + j) * (n + 1) + i]);
        if (textured) {
            float[] normal = FACE_NORMALS[f];
            vertex.setUv(s, t)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(0xF000F0)
                    .setNormal(pose, normal[0], normal[1], normal[2]);
        }
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, Vector3f center, Vector3f right, Vector3f up, int color) {
        buffer.addVertex(pose, center.x - right.x - up.x, center.y - right.y - up.y, center.z - right.z - up.z).setColor(color);
        buffer.addVertex(pose, center.x + right.x - up.x, center.y + right.y - up.y, center.z + right.z - up.z).setColor(color);
        buffer.addVertex(pose, center.x + right.x + up.x, center.y + right.y + up.y, center.z + right.z + up.z).setColor(color);
        buffer.addVertex(pose, center.x - right.x + up.x, center.y - right.y + up.y, center.z - right.z + up.z).setColor(color);
    }

    private static Vector3f direction(float yawDeg, float pitchDeg) {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        return new Vector3f(
                (float) (Math.cos(pitch) * Math.sin(yaw)),
                (float) Math.sin(pitch),
                (float) (-Math.cos(pitch) * Math.cos(yaw)));
    }
}