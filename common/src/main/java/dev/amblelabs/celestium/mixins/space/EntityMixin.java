package dev.amblelabs.celestium.mixins.space;

import dev.amblelabs.celestium.Celestium;
import dev.amblelabs.celestium.init.CelestiumDimensions;
import dev.amblelabs.celestium.init.CelestiumRegistries;
import dev.amblelabs.celestium.space.AstralBody;
import dev.amblelabs.celestium.space.CelestialMath;
import dev.amblelabs.celestium.space.StarSystem;
import dev.amblelabs.celestium.world.grid.SubGridCollisions;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(value = Entity.class, priority = 1500)
public class EntityMixin {

    @Unique
    private static final double SURFACE_GRAVITY_RANGE = 3.0;

    @Inject(method="tick",at=@At("HEAD"))
    private void celestium$teleportTo(CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;

        if (entity == null) return;

        if (entity.level() == null) return;

        MinecraftServer server = entity.level().getServer();

        if (server == null) return;

        ServerLevel overworldLevel = server.getLevel(Level.OVERWORLD);

        if (overworldLevel == null) return;

        ServerLevel spaceLevel = server.getLevel(CelestiumDimensions.SPACE_KEY);

        if (spaceLevel == null) return;

        if (entity.level() == overworldLevel && entity.getY() >= 700) {
            if (entity.isPassenger()) return;

            RegistryAccess access = server.registryAccess();
            Registry<AstralBody> bodies = access.lookupOrThrow(CelestiumRegistries.ASTRAL_BODY);
            Registry<StarSystem> systems = access.lookupOrThrow(CelestiumRegistries.STAR_SYSTEM);

            StarSystem solar = systems.getValue(Celestium.id("solar_system"));
            if (solar == null) return;

            Vector3d exit = CelestialMath.aboveBody(bodies, solar, Celestium.id("solar_system/terra"), spaceLevel.getGameTime(), entity.getBbHeight() + 2.0);

            if (exit == null) return;

            entity.teleport(new TeleportTransition(spaceLevel, new Vec3(exit.x, exit.y, exit.z), entity.getDeltaMovement(),
                    entity.getYRot(), entity.getXRot(), Set.of(), TeleportTransition.DO_NOTHING));
        } else if(entity.level() == spaceLevel && entity.getY() <= 0) {
            entity.teleportTo(overworldLevel, 0, 690, 0, Set.of(), 0, 0, false);
        }
    }

    @Inject(method = "isNoGravity", at = @At("HEAD"), cancellable = true)
    private void celestium$zeroGravity(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().dimension().equals(CelestiumDimensions.SPACE_KEY) && !celestium$nearSurface(self)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static boolean celestium$nearSurface(Entity entity) {
        AABB box = entity.getBoundingBox();
        AABB below = new AABB(box.minX, box.minY - SURFACE_GRAVITY_RANGE, box.minZ, box.maxX, box.minY, box.maxZ);
        return !entity.level().noCollision(entity, below) || !SubGridCollisions.noGridCollision(entity, below, entity.level());
    }
}
