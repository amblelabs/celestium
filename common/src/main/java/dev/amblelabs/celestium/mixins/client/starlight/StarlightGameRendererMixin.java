package dev.amblelabs.celestium.mixins.client.starlight;

import dev.amblelabs.celestium.client.starlight.CameraStarlight;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class StarlightGameRendererMixin {

    @Shadow @Final private GameRenderState gameRenderState;

    @Inject(method = "extract", at = @At("HEAD"))
    private void celestium$updateStarlight(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        CameraStarlight.update(Minecraft.getInstance());
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;render"))
    private void celestium$beginLevelPass(CallbackInfo ci) {
        CameraStarlight.beginLevelPass(this.gameRenderState.levelRenderState.cameraRenderState.pos);
    }

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;render", shift = At.Shift.AFTER))
    private void celestium$endLevelPass(CallbackInfo ci) {
        CameraStarlight.endLevelPass();
    }
}
