package dev.amblelabs.celestium.mixins.grid.client;

import dev.amblelabs.celestium.api.GridLevelExtKt;
import dev.amblelabs.celestium.world.grid.GridCarrier;
import dev.amblelabs.celestium.world.grid.SubGrid;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

   @Inject(method = "tickEntities", at = @At("HEAD"))
   private void celestium$tickGridsFirst(CallbackInfo ci) {
      ClientLevel level = (ClientLevel) (Object) this;

      for (SubGrid grid : GridLevelExtKt.snapshot(level)) {
         GridCarrier carrier = grid.carrier();
         if (!carrier.isRemoved() && !level.tickRateManager().isEntityFrozen(carrier)) {
            level.guardEntityTick(level::tickNonPassenger, carrier);
         }
      }
   }
}
