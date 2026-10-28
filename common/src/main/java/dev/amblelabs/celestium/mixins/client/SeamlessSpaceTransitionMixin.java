package dev.amblelabs.celestium.mixins.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.amblelabs.celestium.init.CelestiumDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class SeamlessSpaceTransitionMixin {

    @Unique
    private boolean celestium$seamless;

    @Inject(method = "handleRespawn", at = @At("HEAD"))
    private void celestium$checkSpaceTransition(ClientboundRespawnPacket packet, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        ResourceKey<Level> from = mc.player.level().dimension();
        ResourceKey<Level> to = packet.commonPlayerSpawnInfo().dimension();
        celestium$seamless = from != to && (from == CelestiumDimensions.SPACE_KEY || to == CelestiumDimensions.SPACE_KEY);
    }

    @Inject(method = "handleRespawn", at = @At("RETURN"))
    private void celestium$resetSpaceTransition(ClientboundRespawnPacket packet, CallbackInfo ci) {
        celestium$seamless = false;
    }

    @WrapOperation(method = "startWaitingForNewLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/Minecraft;setScreenAndShow(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void celestium$skipLoadingScreen(Minecraft minecraft, Screen screen, Operation<Void> original) {
        if (!celestium$seamless) original.call(minecraft, screen);
    }
}
