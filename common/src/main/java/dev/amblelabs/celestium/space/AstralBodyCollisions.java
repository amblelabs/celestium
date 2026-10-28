package dev.amblelabs.celestium.space;

import dev.amblelabs.celestium.init.CelestiumDimensions;
import dev.amblelabs.celestium.init.CelestiumRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// TODO: theo please bash the shit out of this
public final class AstralBodyCollisions {

    private AstralBodyCollisions() {}

    public static void tick(ServerLevel level) {
        if (!level.dimension().equals(CelestiumDimensions.SPACE_KEY)) return;

        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        Registry<StarSystem> starSystems = level.registryAccess().lookupOrThrow(CelestiumRegistries.STAR_SYSTEM);
        Registry<AstralBody> astralBodies = level.registryAccess().lookupOrThrow(CelestiumRegistries.ASTRAL_BODY);

        double ticks = level.getGameTime();
        List<AstralBody> bodies = new ArrayList<>();
        List<Matrix4d> inverses = new ArrayList<>();

        for (StarSystem system : starSystems) {
            for (Holder<AstralBody> holder : system.astralBodies()) {
                AstralBody body = holder.value();
                if (body.isStar()) continue;

                Identifier id = holder.unwrapKey().orElseThrow().identifier();
                Matrix4d transform = CelestialMath.bodyTransform(astralBodies, system, id, ticks);
                if (transform == null) continue;

                bodies.add(body);
                inverses.add(transform.invert());
            }
        }

        if (bodies.isEmpty()) return;

        List<Entity> toTeleport = new ArrayList<>();
        Vector3d local = new Vector3d();

        for (Entity entity : level.getAllEntities()) {
            if (entity.isPassenger()) continue;

            double halfWidth = entity.getBbWidth() / 2.0;
            double halfHeight = entity.getBbHeight() / 2.0;

            for (int i = 0; i < bodies.size(); i++) {
                AstralBody body = bodies.get(i);
                inverses.get(i).transformPosition(entity.getX(), entity.getY() + halfHeight, entity.getZ(), local);

                if (Math.abs(local.x) <= body.sizeX() / 2.0 + halfWidth
                        && Math.abs(local.y) <= body.sizeY() / 2.0 + halfHeight
                        && Math.abs(local.z) <= body.sizeZ() / 2.0 + halfWidth) {
                    toTeleport.add(entity);
                    break;
                }
            }
        }

        for (Entity entity : toTeleport) {
            entity.teleportTo(overworld, 0, 500, 0, Set.of(), entity.getYRot(), entity.getXRot(), false);
        }
    }
}