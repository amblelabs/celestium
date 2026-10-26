package dev.amblelabs.celestium.mixins.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.amblelabs.celestium.init.CelestiumDimensions;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Camera.class)
public class CameraMixin {
    @ModifyExpressionValue(
            method = "update",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F")
    )
    private float celestium$modifyFarPlane(float original) {
        if (Minecraft.getInstance().level != null && Minecraft.getInstance().level.dimension() == CelestiumDimensions.SPACE_KEY)
            return original * 64.0F;
        return original;
    }
}
