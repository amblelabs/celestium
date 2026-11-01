package dev.amblelabs.celestium.mixins.grid;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.amblelabs.celestium.api.GridRiderKt;
import dev.amblelabs.celestium.world.grid.SubGridCollisions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

   @Shadow
   public ServerPlayer player;

   @ModifyReturnValue(method = "noBlocksAround", at = @At("RETURN"))
   private boolean celestium$gridCountsAsGround(boolean noBlocksAround, Entity entity) {
      return noBlocksAround && SubGridCollisions.findGridBelow(entity) == null;
   }

   @ModifyConstant(method = "handlePlayerPositionChange", constant = @Constant(doubleValue = 0.0625D))
   private double allowCheating(double original) {
      return GridRiderKt.getAttachedGrid(this.player) != null ? original * 7 : original;
   }

   @Inject(method = "isEntityCollidingWithAnythingNew", at = @At("HEAD"), cancellable = true)
   private void allowMoreCheating(LevelReader level, Entity entity, AABB oldAABB, double newX, double newY, double newZ, CallbackInfoReturnable<Boolean> cir) {
      if (GridRiderKt.getAttachedGrid(this.player) != null) cir.setReturnValue(false);
   }
}
