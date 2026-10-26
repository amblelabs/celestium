package dev.amblelabs.celestium.mixins.grid;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.amblelabs.celestium.api.GridLevelExtKt;
import dev.amblelabs.celestium.api.GridRider;
import dev.amblelabs.celestium.world.grid.SubGrid;
import dev.amblelabs.celestium.world.grid.SubGridCollisions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityMixin implements GridRider {
   @Unique private static final String CELESTIUM$ATTACHED_GRID_KEY = "celestium:attached_grid";
   @Unique private static final String CELESTIUM$ATTACHED_GRID_OFFSET_KEY = "celestium:attached_grid_offset";

   @Shadow public boolean verticalCollision;
   @Shadow public boolean verticalCollisionBelow;
   @Unique private @Nullable SubGrid attachedGrid;
   @Unique private @Nullable Vec3 pendingOffset;
   @Unique private int attachedGridTimeout;
   @Unique private boolean ignoreGridCollision;
   @Unique private @Nullable UUID pendingGrid;
   @Unique private int pendingGridTimeout;

   @Shadow public abstract AABB getBoundingBox();
   @Shadow public abstract Vec3 getDeltaMovement();

   @Shadow public abstract void setDeltaMovement(double xd, double yd, double zd);
   @Shadow public abstract void snapTo(double x, double y, double z);

   @Shadow
   public abstract Level level();

   @Shadow
   public abstract Vec3 position();

   @Override
   public @Nullable UUID celestium$pendingGrid() {
      return this.pendingGrid;
   }

   @Override
   public @Nullable SubGrid celestium$attachedGrid() {
      return attachedGrid;
   }

   @Override
   public void celestium$waitForGrid(UUID gridId, int timeout) {
      this.pendingGrid = gridId;
      this.pendingGridTimeout = timeout;
   }

   @Inject(method = "baseTick", at = @At("HEAD"))
   private void tickAttachedGrid(CallbackInfo ci) {
      if (this.attachedGridTimeout > 0) {
         if (--this.attachedGridTimeout == 0) {
            this.attachedGrid = null;
         } else if (this.attachedGrid != null && this.attachedGrid.carrier().isRemoved()) {
            this.attachedGrid = null;
         }
      }

      if (this.pendingGrid != null) {
         SubGrid grid = GridLevelExtKt.grids(this.level()).get(this.pendingGrid);
         if (grid != null) {
            this.reseatOn(grid);
            this.attachedGrid = grid;
            this.attachedGridTimeout = 30;
            this.pendingGrid = null;
            this.pendingOffset = null;
         } else if (--this.pendingGridTimeout > 0 && this.attachedGrid == null) {
            Vec3 motion = this.getDeltaMovement();
            if (motion.y < 0) this.setDeltaMovement(motion.x, 0, motion.z);
         } else {
            this.pendingGrid = null;
            this.pendingOffset = null;
         }
      }
   }

   @Unique
   private void reseatOn(SubGrid grid) {
      Vec3 offset = this.pendingOffset;
      if (offset != null && !this.level().isClientSide()) {
         Vec3 target = grid.carrier().position().add(offset);
         if (this.asEntity() instanceof ServerPlayer player) {
            player.connection.teleport(target.x, target.y, target.z, player.getYRot(), player.getXRot());
         } else {
            this.snapTo(target.x, target.y, target.z);
         }
      }
   }

   @Inject(method = "getGravity", at = @At("HEAD"), cancellable = true)
   private void hoverWhileWaitingForGrid(CallbackInfoReturnable<Double> cir) {
      if (this.pendingGrid != null) cir.setReturnValue(0d);
   }

   @Inject(method = "saveWithoutId", at = @At("TAIL"))
   private void saveAttachedGrid(ValueOutput output, CallbackInfo ci) {
      SubGrid grid = this.attachedGrid != null ? this.attachedGrid : SubGridCollisions.findGridBelow(this.asEntity());
      UUID gridId = grid != null ? grid.id() : this.pendingGrid;
      Vec3 offset = grid != null ? this.position().subtract(grid.carrier().position()) : this.pendingOffset;

      if (gridId != null) {
         output.store(CELESTIUM$ATTACHED_GRID_KEY, UUIDUtil.CODEC, gridId);
         if (offset != null) output.store(CELESTIUM$ATTACHED_GRID_OFFSET_KEY, Vec3.CODEC, offset);
      }
   }

   @Inject(method = "load", at = @At("TAIL"))
   private void loadAttachedGrid(ValueInput input, CallbackInfo ci) {
      input.read(CELESTIUM$ATTACHED_GRID_KEY, UUIDUtil.CODEC).ifPresent((gridId) -> {
         this.celestium$waitForGrid(gridId, GridRider.SERVER_RELOAD_TIMEOUT);
         this.pendingOffset = input.read(CELESTIUM$ATTACHED_GRID_OFFSET_KEY, Vec3.CODEC).orElse(null);
      });
   }

   @WrapOperation(method = {"move", "applyMovementEmissionAndPlaySound", "spawnSprintParticle"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
   private BlockState effectStateFromGrid(Level level, BlockPos pos, Operation<BlockState> original) {
      BlockState state = original.call(level, pos);
      if (this.attachedGrid != null && state.isAir()) {
         BlockState gridState = SubGridCollisions.stateUnderFeet(this.asEntity(), this.attachedGrid);
         if (!gridState.isAir()) return gridState;
      }

      return state;
   }

   @WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;maybeBackOffFromEdge(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/MoverType;)Lnet/minecraft/world/phys/Vec3;"))
   public Vec3 actuallyMoveWithAttachedGrid(Entity instance, Vec3 delta, MoverType moverType, Operation<Vec3> original) {
      Vec3 res = instance instanceof Player player && player.isShiftKeyDown() ? delta : original.call(instance, delta, moverType);

      boolean previouslyIgnoring = this.ignoreGridCollision;
      this.ignoreGridCollision = moverType == MoverType.PLAYER;

      try {
         if (!this.ignoreGridCollision && this.attachedGrid != null && SubGridCollisions.movesWithGrid(this.asEntity())) {
            res = res.add(this.attachedGrid.getLastMovement());
         }
      } finally {
         this.ignoreGridCollision = previouslyIgnoring;
      }

      return res;
   }

   @ModifyReturnValue(method = "collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"))
   private Vec3 collideWithGrids(Vec3 movement) {
      if (this.ignoreGridCollision) {
         return movement;
      } else {
         SubGridCollisions.Result result = SubGridCollisions.collide(this.asEntity(), movement, this.getBoundingBox(), this.level());
         if (result.grid() != null) {
            this.attachedGrid = result.grid();
            this.attachedGridTimeout = 30;
         }

         return result.movement();
      }
   }

   @WrapOperation(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setOnGroundWithMovement(ZZLnet/minecraft/world/phys/Vec3;)V"))
   private void groundedOnGrid(Entity entity, boolean onGround, boolean horizontalCollision, Vec3 movement, Operation<Void> original, @Local(argsOnly = true) Vec3 delta) {
      if (this.attachedGrid != null) {
         onGround = this.verticalCollision && delta.y < this.attachedGrid.getLastMovement().y;
         this.verticalCollisionBelow = onGround;
      }

      original.call(entity, onGround, horizontalCollision, movement);
   }

   @Unique
   private Entity asEntity() {
      return (Entity) (Object) this;
   }
}
