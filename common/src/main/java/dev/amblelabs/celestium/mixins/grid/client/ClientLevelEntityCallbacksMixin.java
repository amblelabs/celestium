package dev.amblelabs.celestium.mixins.grid.client;

import dev.amblelabs.celestium.api.GridLevelExtKt;
import dev.amblelabs.celestium.world.grid.GridCarrier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientLevel$EntityCallbacks")
public abstract class ClientLevelEntityCallbacksMixin {

   @Inject(method = "onTickingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
   private void celestium$tickGridsSeparately(Entity entity, CallbackInfo ci) {
      if (entity instanceof GridCarrier) ci.cancel();
   }

   @Inject(method = "onTrackingStart(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
   private void celestium$trackGrid(Entity entity, CallbackInfo ci) {
      if (entity instanceof GridCarrier carrier)
         GridLevelExtKt.trackGrid(carrier.level(), carrier);
   }

   @Inject(method = "onTrackingEnd(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"))
   private void celestium$untrackGrid(Entity entity, CallbackInfo ci) {
      if (entity instanceof GridCarrier carrier) {
         GridLevelExtKt.untrackGrid(carrier.level(), carrier);
         carrier.grid().setRenderCache(null);
      }
   }
}
