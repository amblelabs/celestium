package dev.amblelabs.celestium.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.renderpearl.api.commands.RenderPass;
import dev.amblelabs.celestium.init.CelestiumDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
public class SpaceSkyRendererMixin {

    @Inject(method="renderSun",at=@At("HEAD"),cancellable=true)
    private void celestium$cancelSun(RenderPass renderPass, float rainBrightness, PoseStack poseStack, CallbackInfo ci) {
        if (celestium$inSpace()) ci.cancel();
    }

    @Inject(method="renderMoon",at=@At("HEAD"),cancellable=true)
    private void celestium$cancelMoon(RenderPass renderPass, MoonPhase moonPhase, float rainBrightness, PoseStack poseStack, CallbackInfo ci) {
        if (celestium$inSpace()) ci.cancel();
    }

    @Unique
    private static boolean celestium$inSpace() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && level.dimension() == CelestiumDimensions.SPACE_KEY;
    }
}
