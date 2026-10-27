package dev.amblelabs.celestium.space;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.joml.Matrix4d;
import org.joml.Vector3fc;

import java.util.List;

public final class CelestialMath {

    private CelestialMath() {}

    public static Matrix4d bodyTransform(Registry<AstralBody> registry, StarSystem system, Identifier id, double ticks) {
        Vector3fc rot = system.rot();
        Matrix4d origin = new Matrix4d().translation(system.posX(), system.posY(), system.posZ()).rotateXYZ(rot.x(), rot.y(), rot.z());

        List<ResourceKey<AstralBody>> roots = system.astralBodies().stream().filter(h -> !h.value().isSatellite())
                .map(h -> h.unwrapKey().orElseThrow()).toList();

        return find(registry, roots, origin, id, ticks);
    }

    private static Matrix4d find(Registry<AstralBody> registry, List<ResourceKey<AstralBody>> keys, Matrix4d parent, Identifier id, double ticks) {
        for (ResourceKey<AstralBody> key : keys) {
            AstralBody body = registry.getValue(key);
            if (body == null) continue;

            Matrix4d orbit = new Matrix4d(parent).rotateY(Math.toRadians(body.orbit().angleDegrees(ticks))).translate(body.orbit().distanceFromCenter(), 0, 0);

            if (key.identifier().equals(id)) {
                return orbit.rotateX(Math.toRadians(body.axis())).rotateY(Math.toRadians(body.spinDegrees(ticks)));
            }

            Matrix4d found = find(registry, body.satellites(), orbit, id, ticks);
            if (found != null) return found;
        }
        return null;
    }
}