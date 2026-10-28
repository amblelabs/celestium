package dev.amblelabs.celestium.space;

import net.minecraft.resources.Identifier;
import org.joml.Matrix4d;
import org.joml.Vector3fc;

import java.util.List;

public final class CelestialMath {

    private CelestialMath() {}

    public static Matrix4d bodyTransform(StarSystem system, Identifier id, double ticks) {
        Vector3fc rot = system.rot();
        Matrix4d origin = new Matrix4d().translation(system.posX(), system.posY(), system.posZ())
                .rotateXYZ(Math.toRadians(rot.x()), Math.toRadians(rot.y()), Math.toRadians(rot.z()));

        return find(system.astralBodies().stream().filter(b ->
                !b.isSatellite()).toList(), origin, id, ticks);
    }

    public static Vector3d aboveBody(StarSystem system, AstralBody body, double ticks, double clearance) {
        Matrix4d transform = bodyTransform(system, body.id(), ticks);
        if (transform == null) return null;

        Vector3d center = transform.getTranslation(new Vector3d());
        double topExtent = Math.abs(transform.m01()) * body.sizeX() / 2.0
                + Math.abs(transform.m11()) * body.sizeY() / 2.0
                + Math.abs(transform.m21()) * body.sizeZ() / 2.0;
        return center.add(0, topExtent + clearance, 0);
    }

    private static Matrix4d find(List<AstralBody> bodies, Matrix4d parent, Identifier id, double ticks) {
        for (AstralBody body : bodies) {
            Matrix4d orbit = new Matrix4d(parent).rotateY(Math.toRadians(body.orbit().angleDegrees(ticks)))
                    .translate(body.orbit().distanceFromCenter(), 0, 0);

            if (body.id().equals(id)) {
                return orbit.rotateX(Math.toRadians(body.axis())).rotateY(Math.toRadians(body.spinDegrees(ticks)));
            }

            Matrix4d found = find(body.satellites(), orbit, id, ticks);
            if (found != null) return found;
        }
        return null;
    }
}