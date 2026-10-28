package dev.amblelabs.celestium.mixins.client.starlight;

import dev.amblelabs.celestium.client.starlight.CameraStarlight;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapRenderStateExtractor.class)
public class StarlightLightmapMixin {

    @Inject(method = "extract", at = @At("TAIL"))
    private void celestium$starlightSkyColor(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
        if (!renderState.needsUpdate || !CameraStarlight.active()) return;
        renderState.skyFactor = 1.0F;
        renderState.skyLightColor = new Vector3f(CameraStarlight.color());
        renderState.brightness *= 0.25F;
    }
}
