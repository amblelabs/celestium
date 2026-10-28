package dev.amblelabs.celestium.client.starlight;

import dev.amblelabs.celestium.space.AstralBody;
import dev.amblelabs.celestium.space.Atmosphere;
import dev.amblelabs.celestium.space.StarSystem;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ARGB;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class Starlight {

    private static final float[] ECLIPSE_TINT = {1.0F, 0.35F, 0.12F};

    public record Light(Vector3dc position, double radius, float r, float g, float b) {}

    public record Body(AstralBody body, Matrix4d transform, Vector3dc center, double radius, float[] scatter) {
        public boolean hasAtmosphere() {
            return scatter != null;
        }
    }

    public record Scene(List<Light> lights, List<Body> occluders, Map<Identifier, Body> bodies, Map<Body, List<Body>> casters) {
        public Body body(Identifier id) {
            return bodies.get(id);
        }

        public Body body(Holder<AstralBody> holder) {
            return bodies.get(holder.unwrapKey().orElseThrow().identifier());
        }

        private List<Body> castersOf(Body self) {
            return self == null ? occluders : casters.computeIfAbsent(self, this::findCasters);
        }

        private List<Body> findCasters(Body self) {
            List<Body> found = new ArrayList<>();
            Vector3d axis = new Vector3d();
            Vector3d offset = new Vector3d();
            for (Body occluder : occluders) {
                if (occluder == self) continue;
                for (Light light : lights) {
                    self.center().sub(light.position(), axis);
                    double length = axis.length();
                    occluder.center().sub(light.position(), offset);
                    double along = offset.dot(axis) / length;
                    if (along <= 0.0 || along >= length) continue;
                    double across = Math.sqrt(Math.max(0.0, offset.lengthSquared() - along * along));
                    double penumbra = light.radius() * 0.033 * (1.0 + (length - along) / along);
                    if (across <= occluder.radius() + self.radius() * 2.2 + penumbra) {
                        found.add(occluder);
                        break;
                    }
                }
            }
            return found;
        }
    }

    private static final Map<StarSystem, Scene> scenes = new IdentityHashMap<>();
    private static double scenesTicks = Double.NaN;

    private Starlight() {}

    public static Scene scene(Registry<AstralBody> registry, StarSystem system, double ticks) {
        if (ticks != scenesTicks) {
            scenes.clear();
            scenesTicks = ticks;
        }
        return scenes.computeIfAbsent(system, s -> build(registry, s, ticks));
    }

    private static Scene build(Registry<AstralBody> registry, StarSystem system, double ticks) {
        Vector3fc rot = system.rot();
        Matrix4d origin = new Matrix4d().translation(system.posX(), system.posY(), system.posZ())
                .rotateXYZ(Math.toRadians(rot.x()), Math.toRadians(rot.y()), Math.toRadians(rot.z()));

        Scene scene = new Scene(new ArrayList<>(), new ArrayList<>(), new HashMap<>(), new IdentityHashMap<>());
        for (Holder<AstralBody> holder : system.astralBodies()) {
            AstralBody body = holder.value();
            if (!body.isSatellite()) {
                collect(registry, scene, origin, holder.unwrapKey().orElseThrow().identifier(), body, ticks);
            }
        }
        return scene;
    }

    private static void collect(Registry<AstralBody> registry, Scene scene, Matrix4d parent, Identifier id, AstralBody body, double ticks) {
        Matrix4d orbit = new Matrix4d(parent).rotateY(Math.toRadians(body.orbit().angleDegrees(ticks)))
                .translate(body.orbit().distanceFromCenter(), 0, 0);

        for (ResourceKey<AstralBody> key : body.satellites()) {
            AstralBody satellite = registry.getValue(key);
            if (satellite != null) {
                collect(registry, scene, orbit, key.identifier(), satellite, ticks);
            }
        }

        Matrix4d self = orbit.rotateX(Math.toRadians(body.axis())).rotateY(Math.toRadians(body.spinDegrees(ticks)));
        Vector3d center = self.getTranslation(new Vector3d());
        double radius = Math.max(body.sizeX(), Math.max(body.sizeY(), body.sizeZ())) / 2.0;

        if (body.isStar()) {
            int c = mix(body.defaultColor(), 0xFFFFFF, 0.6F);
            scene.lights().add(new Light(center, radius, ARGB.red(c) / 255F * 1.9F, ARGB.green(c) / 255F * 1.9F, ARGB.blue(c) / 255F * 1.9F));
            scene.bodies().put(id, new Body(body, self, center, radius, null));
            return;
        }

        Atmosphere atmosphere = body.atmosphere();
        float[] scatter = atmosphere == null ? null : scatterCoefficients(mix(atmosphere.color(), 0x7FB8FF, 0.5F));
        Body state = new Body(body, self, center, radius * 1.15, scatter);
        scene.occluders().add(state);
        scene.bodies().put(id, state);
    }

    public static int surface(Scene scene, Body self, Vector3dc pos, Vector3dc faceNormal, Vector3dc sphereNormal, int albedo) {
        Vector3d n = new Vector3d(faceNormal).lerp(sphereNormal, 0.7).normalize();
        Vector3d toLight = new Vector3d();
        float[] vis = new float[3];
        float[] transmit = {1.0F, 1.0F, 1.0F};
        float r = 0.006F, g = 0.006F, b = 0.006F;

        for (Light light : scene.lights()) {
            light.position().sub(pos, toLight).normalize();
            double nl = n.dot(toLight);
            float diffuse = (float) Math.max(0.0, self.hasAtmosphere() ? (nl + 0.08) / 1.08 : nl);
            if (diffuse <= 0.0F) continue;

            shadow(scene, self, pos, light, vis);
            if (self.hasAtmosphere()) {
                transmittance(self.scatter(), (float) (0.12 / Math.max(0.08, sphereNormal.dot(toLight))), transmit);
            }
            r += light.r() * diffuse * vis[0] * transmit[0];
            g += light.g() * diffuse * vis[1] * transmit[1];
            b += light.b() * diffuse * vis[2] * transmit[2];
        }

        return ARGB.color(
                channel(ARGB.red(albedo) / 255F * r),
                channel(ARGB.green(albedo) / 255F * g),
                channel(ARGB.blue(albedo) / 255F * b));
    }

    public static int atmosphere(Scene scene, Body self, Vector3dc pos, Vector3dc camera, double atmosphereRadius, float alpha) {
        float[] beta = self.scatter();
        Vector3d up = new Vector3d(pos).sub(self.center()).normalize();
        Vector3d view = new Vector3d(pos).sub(camera).normalize();
        Vector3d toLight = new Vector3d();
        float[] vis = new float[3];
        float[] transmit = new float[3];
        float r = 0.004F * beta[0], g = 0.004F * beta[1], b = 0.004F * beta[2];

        for (Light light : scene.lights()) {
            light.position().sub(pos, toLight).normalize();
            float daylight = (float) smoothstep(-0.3, 0.2, up.dot(toLight));
            if (daylight <= 0.0F) continue;

            shadow(scene, self, pos, light, vis);
            transmittance(beta, (float) (exitDistance(pos, toLight, self.center(), atmosphereRadius) / atmosphereRadius), transmit);

            float cos = (float) view.dot(toLight);
            float rayleigh = 0.6F * (1.0F + cos * cos);
            float mie = 0.28F * henyeyGreenstein(cos, 0.76F);
            r += light.r() * vis[0] * transmit[0] * (beta[0] * rayleigh + mie) * daylight;
            g += light.g() * vis[1] * transmit[1] * (beta[1] * rayleigh + mie) * daylight;
            b += light.b() * vis[2] * transmit[2] * (beta[2] * rayleigh + mie) * daylight;
        }

        float peak = Math.max(1.0F, Math.max(r, Math.max(g, b)));
        return ARGB.color(channel(alpha * peak), channel(r / peak), channel(g / peak), channel(b / peak));
    }

    public static void illumination(Scene scene, Vector3dc pos, float[] rgb, Vector3d direction) {
        Vector3d toLight = new Vector3d();
        Vector3d up = new Vector3d();
        float[] vis = new float[3];
        rgb[0] = rgb[1] = rgb[2] = 0.0F;
        direction.zero();

        for (Light light : scene.lights()) {
            light.position().sub(pos, toLight).normalize();
            float daylight = 1.0F;
            for (Body body : scene.occluders()) {
                if (body.center().distanceSquared(pos) <= body.radius() * body.radius()) {
                    pos.sub(body.center(), up).normalize();
                    daylight *= (float) smoothstep(-0.1, 0.1, up.dot(toLight));
                }
            }
            if (daylight <= 0.0F) continue;

            shadow(scene, null, pos, light, vis);
            float r = light.r() * vis[0] * daylight, g = light.g() * vis[1] * daylight, b = light.b() * vis[2] * daylight;
            rgb[0] += r;
            rgb[1] += g;
            rgb[2] += b;
            direction.fma((r + g + b) / 3.0, toLight);
        }
    }

    private static void shadow(Scene scene, Body self, Vector3dc pos, Light light, float[] out) {
        out[0] = out[1] = out[2] = 1.0F;
        double dx = light.position().x() - pos.x(), dy = light.position().y() - pos.y(), dz = light.position().z() - pos.z();
        double lightDist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double starAngle = Math.asin(Math.min(1.0, light.radius() * 0.033 / lightDist));

        for (Body occluder : scene.castersOf(self)) {
            double ox = occluder.center().x() - pos.x(), oy = occluder.center().y() - pos.y(), oz = occluder.center().z() - pos.z();
            double occluderDist = Math.sqrt(ox * ox + oy * oy + oz * oz);
            if (occluderDist >= lightDist || occluderDist <= occluder.radius()) continue;

            double cos = (dx * ox + dy * oy + dz * oz) / (lightDist * occluderDist);
            if (cos <= 0.0) continue;
            double occluderAngle = Math.asin(occluder.radius() / occluderDist);
            double reach = starAngle + occluderAngle;
            if (reach < Math.PI && cos <= Math.cos(reach)) continue;

            float covered = (float) discOverlap(starAngle, occluderAngle, Math.acos(Math.min(1.0, cos)));
            if (covered <= 0.0F) continue;
            float leak = occluder.hasAtmosphere() ? covered * 0.08F : 0.0F;
            for (int c = 0; c < 3; c++) {
                out[c] *= 1.0F - covered + leak * ECLIPSE_TINT[c];
            }
        }
    }

    private static double discOverlap(double r1, double r2, double d) {
        if (d >= r1 + r2) return 0.0;
        if (d <= Math.abs(r1 - r2)) {
            return r2 >= r1 ? 1.0 : (r2 * r2) / (r1 * r1);
        }
        double a1 = r1 * r1 * Math.acos(Math.clamp((d * d + r1 * r1 - r2 * r2) / (2 * d * r1), -1.0, 1.0));
        double a2 = r2 * r2 * Math.acos(Math.clamp((d * d + r2 * r2 - r1 * r1) / (2 * d * r2), -1.0, 1.0));
        double lens = 0.5 * Math.sqrt(Math.max(0.0, (-d + r1 + r2) * (d + r1 - r2) * (d - r1 + r2) * (d + r1 + r2)));
        return Math.min(1.0, (a1 + a2 - lens) / (Math.PI * r1 * r1));
    }

    private static double exitDistance(Vector3dc pos, Vector3dc dir, Vector3dc center, double radius) {
        double px = pos.x() - center.x(), py = pos.y() - center.y(), pz = pos.z() - center.z();
        double b = px * dir.x() + py * dir.y() + pz * dir.z();
        double disc = b * b - (px * px + py * py + pz * pz - radius * radius);
        if (disc <= 0.0) return 0.0;
        double root = Math.sqrt(disc);
        return Math.max(0.0, -b + root - Math.max(0.0, -b - root));
    }

    private static void transmittance(float[] beta, float depth, float[] out) {
        for (int c = 0; c < 3; c++) {
            out[c] = (float) Math.exp(-1.3 * depth * beta[c]);
        }
    }

    private static float henyeyGreenstein(float cos, float g) {
        return (float) ((1.0 - g * g) / Math.pow(1.0 + g * g - 2.0 * g * cos, 1.5));
    }

    private static float[] scatterCoefficients(int color) {
        float r = ARGB.red(color), g = ARGB.green(color), b = ARGB.blue(color);
        float max = Math.max(1.0F, Math.max(r, Math.max(g, b)));
        return new float[]{r / max, g / max, b / max};
    }

    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private static int channel(float v) {
        return Math.round(Math.clamp(v, 0.0F, 1.0F) * 255.0F);
    }

    private static int mix(int from, int to, float t) {
        return ARGB.color(
                Math.round(ARGB.red(from) + (ARGB.red(to) - ARGB.red(from)) * t),
                Math.round(ARGB.green(from) + (ARGB.green(to) - ARGB.green(from)) * t),
                Math.round(ARGB.blue(from) + (ARGB.blue(to) - ARGB.blue(from)) * t));
    }
}
