package dev.amblelabs.celestium.mixins.client;

import dev.amblelabs.celestium.init.CelestiumDimensions;
import dev.amblelabs.celestium.util.SkyFadeUtil;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class SkyDarkenLevelMixin {

    @Inject(method = "addEnvironmentAttributeLayers", at = @At("RETURN"))
    private void celestium$spaceFade(EnvironmentAttributeSystem.Builder builder, CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> cir) {
        // Couldn't find a way to do it without mixins, is there better way probably?
        ClientLevel self = (ClientLevel) (Object) this;
        if (self.dimension() == CelestiumDimensions.SPACE_KEY) return;

        builder.addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR,
                (color, tick) -> SkyFadeUtil.darken(color, SkyFadeUtil.current()));
        builder.addTimeBasedLayer(EnvironmentAttributes.FOG_COLOR,
                (color, tick) -> SkyFadeUtil.darken(color, SkyFadeUtil.current()));
    }
}