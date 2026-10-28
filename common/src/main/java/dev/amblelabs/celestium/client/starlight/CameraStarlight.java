package dev.amblelabs.celestium.client.starlight;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.QuadInstance;
import dev.amblelabs.celestium.init.CelestiumDimensions;
import dev.amblelabs.celestium.mixins.client.starlight.LightingAccessor;
import dev.amblelabs.celestium.space.StarSystem;
import dev.amblelabs.celestium.space.StarSystemEntries;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class CameraStarlight {

    private static final CardinalLighting SPACE_FACES = new CardinalLighting(0.75F, 1.0F, 0.9F, 0.9F, 0.85F, 0.85F);

    private static final Vector3f color = new Vector3f(1.0F);
    private static final Vector3f direction = new Vector3f(0.0F, 1.0F, 0.0F);

    private static ClientLevel level;
    private static volatile boolean active;
    private static volatile Vector3fc meshedToward;
    private static Vector3fc uploadedToward;
    private static long lastRemesh;
    private static Thread renderThread;
    private static int frame;
    private static boolean levelPass;
    private static Vec3 camera = Vec3.ZERO;

    private CameraStarlight() {}

    public static boolean active() {
        return active;
    }

    public static Vector3fc color() {
        return color;
    }

    public static CardinalLighting cardinalLighting() {
        return SPACE_FACES;
    }

    public static Vector3fc shadowDirection() {
        return active ? meshedToward : null;
    }

    public static boolean onRenderThread() {
        return Thread.currentThread() == renderThread;
    }

    public static int frame() {
        return frame;
    }

    public static void applyStarFacing(BakedQuad quad, QuadInstance instance) {
        Vector3fc toward = shadowDirection();
        if (toward == null) return;
        Direction face = quad.materialInfo().shadeDirectionOverride() != null ? quad.materialInfo().shadeDirectionOverride() : quad.direction();
        float factor = 0.1F + 0.9F * Math.max(0.0F, face.getStepX() * toward.x() + face.getStepY() * toward.y() + face.getStepZ() * toward.z());
        if (factor >= 1.0F) return;

        for (int vertex = 0; vertex < 4; vertex++) {
            int coords = instance.getLightCoords(vertex);
            float sky = LightCoordsUtil.smoothSky(coords) / 240.0F;
            float brightness = sky / (4.0F - 3.0F * sky) * factor;
            int scaled = Math.clamp(Math.round(4.0F * brightness / (1.0F + 3.0F * brightness) * 240.0F), 0, 240);
            instance.setLightCoords(vertex, LightCoordsUtil.smoothPack(LightCoordsUtil.smoothBlock(coords), scaled));
        }
    }

    public static void beginLevelPass(Vec3 pos) {
        levelPass = active && onRenderThread();
        camera = pos;
    }

    public static void endLevelPass() {
        levelPass = false;
    }

    public static int modelVertexLight(float x, float y, float z, int light) {
        if (!levelPass || light == LightCoordsUtil.FULL_BRIGHT || level == null) return light;
        double px = camera.x + x - 0.5, py = camera.y + y - 0.5, pz = camera.z + z - 0.5;
        int bx = (int) Math.floor(px), by = (int) Math.floor(py), bz = (int) Math.floor(pz);
        double fx = px - bx, fy = py - by, fz = pz - bz;
        BlockPos.MutableBlockPos cell = new BlockPos.MutableBlockPos();

        double total = 0.0, block = 0.0, sky = 0.0;
        for (int corner = 0; corner < 8; corner++) {
            int dx = corner & 1, dy = (corner >> 1) & 1, dz = (corner >> 2) & 1;
            cell.set(bx + dx, by + dy, bz + dz);
            if (level.getBlockState(cell).canOcclude()) continue;
            double weight = (dx == 1 ? fx : 1.0 - fx) * (dy == 1 ? fy : 1.0 - fy) * (dz == 1 ? fz : 1.0 - fz);
            if (weight <= 0.0) continue;
            total += weight;
            block += weight * level.getBrightness(LightLayer.BLOCK, cell);
            sky += weight * level.getBrightness(LightLayer.SKY, cell);
        }
        if (total <= 1.0E-4) return light;

        return LightCoordsUtil.smoothPack(
                Math.clamp(Math.max((int) Math.round(block / total * 16.0), LightCoordsUtil.smoothBlock(light)), 0, 240),
                Math.clamp((int) Math.round(sky / total * 16.0), 0, 240));
    }

    public static void update(Minecraft mc) {
        renderThread = Thread.currentThread();
        frame++;
        ClientLevel current = mc.level;
        if (current != level) {
            level = current;
            meshedToward = null;
            uploadedToward = null;
        }
        active = current != null && current.dimension() == CelestiumDimensions.SPACE_KEY;
        if (!active) return;

        double ticks = current.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 cam = mc.gameRenderer.mainCamera().position();
        Vector3d pos = new Vector3d(cam.x, cam.y, cam.z);

        Starlight.Scene scene = nearestScene(pos, ticks);
        if (scene == null) {
            color.set(1.0F);
        } else {
            float[] rgb = new float[3];
            Vector3d toward = new Vector3d();
            Starlight.illumination(scene, pos, rgb, toward);
            color.set(Math.clamp(rgb[0], 0.0F, 1.0F), Math.clamp(rgb[1], 0.0F, 1.0F), Math.clamp(rgb[2], 0.0F, 1.0F));
            if (toward.lengthSquared() > 1.0E-8) direction.set(toward.normalize());
        }

        if (uploadedToward == null || !uploadedToward.equals(direction, 1.0E-3F)) {
            Vector3f light = direction.mul(0.75F, new Vector3f());
            ((LightingAccessor) mc.gameRenderer.lighting()).celestium$updateBuffer(Lighting.Entry.LEVEL, light, light);
            uploadedToward = new Vector3f(direction);
        }

        if (meshedToward == null) {
            meshedToward = new Vector3f(direction);
        } else if (meshedToward.dot(direction) < 0.9976F && System.currentTimeMillis() - lastRemesh > 2000L) {
            meshedToward = new Vector3f(direction);
            lastRemesh = System.currentTimeMillis();
            int radius = mc.options.getEffectiveRenderDistance();
            int cx = SectionPos.blockToSectionCoord(cam.x);
            int cz = SectionPos.blockToSectionCoord(cam.z);
            mc.levelExtractor.setSectionRangeDirty(
                    cx - radius, current.getMinSectionY(), cz - radius,
                    cx + radius, current.getMaxSectionY(), cz + radius);
        }
    }

    private static Starlight.Scene nearestScene(Vector3d pos, double ticks) {
        Starlight.Scene nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (StarSystem system : StarSystemEntries.ENTRIES.values()) {
            Starlight.Scene scene = Starlight.scene(system, ticks);
            for (Starlight.Light light : scene.lights()) {
                double distance = light.position().distanceSquared(pos);
                if (distance < best) {
                    best = distance;
                    nearest = scene;
                }
            }
        }
        return nearest;
    }
}
