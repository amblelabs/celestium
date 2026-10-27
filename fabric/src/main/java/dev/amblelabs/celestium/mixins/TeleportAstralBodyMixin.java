package dev.amblelabs.celestium.mixins;

import dev.amblelabs.celestium.init.CelestiumDimensions;
import dev.amblelabs.celestium.space.AstralBodyRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(Entity.class)
public class TeleportAstralBodyMixin {

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

        // TODO set up timecode stuff for retrieving the position of an astral body - also redo this ass code
        /*AstralBodyRegistry.ASTRAL_BODY_REGISTRY.stream().forEach(astralBody -> {
            if (astralBody.isStar()) return;
            int x = astralBody.sizeX() / 2;
            int y = astralBody.sizeY() / 2;
            int z = astralBody.sizeZ() / 2;
            AABB aabb = new AABB(-x, -y, -z, x, y, z);

            if (entity.getBoundingBox().intersects(aabb)) {
                entity.teleportTo(overworldLevel, 0, 500, 0, Set.of(), 0, 0, false);
            }
        });*/
    }

}
