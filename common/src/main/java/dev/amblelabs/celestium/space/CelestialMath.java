package dev.amblelabs.celestium.space;

import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector3fc;

import java.util.List;

public final class CelestialMath {

    private CelestialMath() {}

    public static Matrix4d bodyTransform(Registry<AstralBody> registry, StarSystem system, Identifier id, double ticks) {
        Vector3fc rot = system.rot();
        Matrix4d origin = new Matrix4d().translation(system.posX(), system.posY(), system.posZ())
                .rotateXYZ(Math.toRadians(rot.x()), Math.toRadians(rot.y()), Math.toRadians(rot.z()));

        List<ResourceKey<AstralBody>> roots = system.astralBodies().stream().filter(h -> !h.value().isSatellite())
                .map(h -> h.unwrapKey().orElseThrow()).toList();

        return find(registry, roots, origin, id, ticks);
    }

    public static Vector3d aboveBody(Registry<AstralBody> registry, StarSystem system, Identifier id, double ticks, double clearance) {
        AstralBody body = registry.getValue(id);
        if (body == null) return null;

        Matrix4d transform = bodyTransform(registry, system, id, ticks);
        if (transform == null) return null;

        Vector3d center = transform.getTranslation(new Vector3d());
        double topExtent = Math.abs(transform.m01()) * body.sizeX() / 2.0
                + Math.abs(transform.m11()) * body.sizeY() / 2.0
                + Math.abs(transform.m21()) * body.sizeZ() / 2.0;
        return center.add(0, topExtent + clearance, 0);
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