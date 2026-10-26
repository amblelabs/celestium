package dev.amblelabs.celestium.mixins.space;

import dev.amblelabs.celestium.init.CelestiumDimensions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(value = Entity.class, priority = 1500)
public class TeleportUpMixin {

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
            entity.teleportTo(spaceLevel, 0, 128, 0, Set.of(), 0, 0, false);
        } else if(entity.level() == spaceLevel && entity.getY() <= 0) {
            entity.teleportTo(overworldLevel, 0, 256, 0, Set.of(), 0, 0, false);
        }
    }
}
