package dev.amblelabs.celestium.mixins.space;

import dev.amblelabs.celestium.space.AstralBodyCollisions;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public class ServerLevelMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void celestium$astralBodyCollisions(BooleanSupplier haveTime, CallbackInfo ci) {
        AstralBodyCollisions.tick((ServerLevel) (Object) this);
    }
}
