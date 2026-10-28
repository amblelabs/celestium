package dev.amblelabs.celestium.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.amblelabs.celestium.client.starlight.Starlight;
import dev.amblelabs.celestium.space.AstralBody;
import dev.amblelabs.celestium.space.Ring;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

public final class RingDebris {

    private static final double RANGE = 96.0;
    private static final double CELL = 16.0;
    private static final double THICKNESS = 6.0;
    private static final int MAX_PER_CELL = 5;
    private static final double ORBIT_SPEED = 2.0;

    private static final float[][] NORMALS = {
            {0, 0, 1}, {0, 0, -1}, {-1, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, -1, 0}
    };
    private static final int[][] FACES = {
            {4, 5, 7, 6}, {1, 0, 2, 3}, {0, 4, 6, 2}, {5, 1, 3, 7}, {2, 6, 7, 3}, {0, 1, 5, 4}
    };
    private static final float[][] UVS = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};

    private RingDebris() {}

    public static void render(VertexConsumer buffer, PoseStack.Pose root, List<Starlight.Scene> scenes, Vector3d camera, double ticks) {
        double seconds = ticks / 20.0;
        for (Starlight.Scene scene : scenes) {
            for (Map.Entry<Identifier, Starlight.Body> entry : scene.bodies().entrySet()) {
                List<Ring> rings = entry.getValue().body().rings();
                for (int r = 0; r < rings.size(); r++) {
                    renderRing(buffer, root, scene, entry.getValue(), rings.get(r), entry.getKey().hashCode() * 31L + r, camera, seconds);
                }
            }
        }
    }

    private static void renderRing(VertexConsumer buffer, PoseStack.Pose root, Starlight.Scene scene, Starlight.Body state,
                                   Ring ring, long seed, Vector3d camera, double seconds) {
        AstralBody body = state.body();
        double radius = Math.max(body.sizeX(), Math.max(body.sizeY(), body.sizeZ())) / 2.0;
        double inner = ring.innerRadius() * radius, outer = ring.outerRadius() * radius;
        int perCell = Math.round(ARGB.alpha(ring.color()) / 255F * MAX_PER_CELL);
        if (perCell <= 0) return;

        Vector3fc rot = ring.rotation();
        Matrix4d frame = new Matrix4d(state.transform())
                .rotateXYZ(Math.toRadians(rot.x()), Math.toRadians(rot.y()), Math.toRadians(rot.z()));
        Vector3d local = frame.invert(new Matrix4d()).transformPosition(new Vector3d(camera));
        double camRadius = Math.max(Math.abs(local.x), Math.abs(local.z));
        if (Math.abs(local.y) > RANGE + THICKNESS || camRadius < inner - RANGE || camRadius > outer + RANGE) return;

        Quaternionf frameRotation = new Quaternionf(frame.getNormalizedRotation(new Quaterniond()));
        double camAlong = perimeterFraction(local.x, local.z);
        float[] point = new float[2];
        float[] rgb = new float[3];
        Vector3d toward = new Vector3d();
        Vector3d world = new Vector3d();

        int firstBand = (int) Math.floor(Math.max(inner, camRadius - RANGE) / CELL);
        int lastBand = (int) Math.floor(Math.min(outer, camRadius + RANGE) / CELL);
        for (int band = firstBand; band <= lastBand; band++) {
            double bandRadius = (band + 0.5) * CELL;
            double perimeter = 8.0 * bandRadius;
            int cells = Math.max(1, (int) (perimeter / CELL));
            double cellLength = perimeter / cells;
            double drift = ORBIT_SPEED * Math.sqrt(inner / bandRadius) * seconds;
            double origin = camAlong * perimeter - drift;
            int firstCell = (int) Math.floor((origin - RANGE) / cellLength);
            int lastCell = (int) Math.floor((origin + RANGE) / cellLength);

            for (int cell = firstCell; cell <= lastCell; cell++) {
                int wrapped = Math.floorMod(cell, cells);
                SplittableRandom random = new SplittableRandom(seed * 1_000_003L + band * 92_821L + wrapped);
                for (int n = 0; n < perCell; n++) {
                    double r = band * CELL + random.nextDouble() * CELL;
                    double along = (wrapped + random.nextDouble()) * cellLength + drift;
                    double y = (random.nextDouble() * 2.0 - 1.0) * THICKNESS;
                    double roll = random.nextDouble();
                    float size = (float) (0.3 + 2.7 * roll * roll * roll + (random.nextDouble() < 0.03 ? 4.0 : 0.0));
                    Vector3f axis = new Vector3f((float) random.nextDouble() - 0.5F, (float) random.nextDouble() - 0.5F, (float) random.nextDouble() - 0.5F).normalize();
                    float spin = (float) ((random.nextDouble() * 2.0 - 1.0) * 0.6 * seconds + random.nextDouble() * Math.PI * 2.0);
                    float shade = 0.8F + (float) random.nextDouble() * 0.35F;
                    if (r < inner || r > outer) continue;

                    double fraction = along / perimeter;
                    fraction -= Math.floor(fraction);
                    int side = (int) (fraction * 4.0);
                    ringPoint(side, (float) (fraction * 4.0 - side), (float) r, point);
                    frame.transformPosition(world.set(point[0], y, point[1]));

                    double distance = world.distance(camera);
                    if (distance > RANGE) continue;
                    float half = size * 0.5F * (float) Mth.clamp((RANGE - distance) / (RANGE * 0.25), 0.0, 1.0);
                    if (half <= 0.01F) continue;

                    Starlight.illumination(scene, world, rgb, toward);
                    Quaternionf rotation = new Quaternionf(frameRotation).rotateAxis(spin, axis);
                    PoseStack.Pose pose = root.copy();
                    pose.translate((float) (world.x - camera.x), (float) (world.y - camera.y), (float) (world.z - camera.z));
                    pose.rotate(rotation);
                    cube(buffer, pose, half, rotation, ring.color(), shade, rgb, toward);
                }
            }
        }
    }

    private static double perimeterFraction(double x, double z) {
        double ax = Math.abs(x), az = Math.abs(z);
        if (ax < 1.0E-6 && az < 1.0E-6) return 0.0;
        int side;
        double t;
        if (ax >= az) {
            side = x > 0 ? 0 : 2;
            t = ((x > 0 ? z : -z) / ax + 1.0) / 2.0;
        } else {
            side = z > 0 ? 1 : 3;
            t = ((z > 0 ? -x : x) / az + 1.0) / 2.0;
        }
        return (side + t) / 4.0;
    }

    private static void ringPoint(int side, float t, float extent, float[] out) {
        float along = Mth.lerp(t, -extent, extent);
        switch (side) {
            case 0 -> { out[0] = extent; out[1] = along; }
            case 1 -> { out[0] = -along; out[1] = extent; }
            case 2 -> { out[0] = -extent; out[1] = -along; }
            default -> { out[0] = along; out[1] = -extent; }
        }
    }

    private static void cube(VertexConsumer buffer, PoseStack.Pose pose, float half, Quaternionf rotation, int color,
                             float shade, float[] rgb, Vector3d toward) {
        Vector3f normal = new Vector3f();
        for (int f = 0; f < FACES.length; f++) {
            float[] n = NORMALS[f];
            rotation.transform(normal.set(n[0], n[1], n[2]));
            float lit = Math.max(0.0F, (float) (normal.x * toward.x + normal.y * toward.y + normal.z * toward.z));
            int faceColor = ARGB.color(
                    channel(ARGB.red(color) / 255F * shade * (0.03F + rgb[0] * lit)),
                    channel(ARGB.green(color) / 255F * shade * (0.03F + rgb[1] * lit)),
                    channel(ARGB.blue(color) / 255F * shade * (0.03F + rgb[2] * lit)));
            int[] face = FACES[f];
            for (int v = 0; v < 4; v++) {
                int i = face[v];
                buffer.addVertex(pose, (i & 1) != 0 ? half : -half, (i & 2) != 0 ? half : -half, (i & 4) != 0 ? half : -half)
                        .setColor(faceColor)
                        .setUv(UVS[v][0], UVS[v][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(0xF000F0)
                        .setNormal(pose, n[0], n[1], n[2]);
            }
        }
    }

    private static int channel(float v) {
        return Math.round(Math.clamp(v, 0.0F, 1.0F) * 255.0F);
    }
}
