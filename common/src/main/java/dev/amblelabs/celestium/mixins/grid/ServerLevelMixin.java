package dev.amblelabs.celestium.mixins.grid;

import dev.amblelabs.celestium.api.GridLevelExtKt;
import dev.amblelabs.celestium.world.grid.GridCarrier;
import dev.amblelabs.celestium.world.grid.SubGrid;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

   @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/EntityTickList;forEach(Ljava/util/function/Consumer;)V"))
   private void celestium$tickGridsFirst(BooleanSupplier haveTime, CallbackInfo ci) {
      ServerLevel level = (ServerLevel) (Object) this;

      for (SubGrid grid : GridLevelExtKt.snapshot(level)) {
         GridCarrier carrier = grid.carrier();
         if (!carrier.isRemoved() && !level.tickRateManager().isEntityFrozen(carrier))
             level.guardEntityTick(level::tickNonPassenger, carrier);
      }
   }
}
