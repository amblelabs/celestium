package dev.amblelabs.celestium.mixins.grid;

import com.llamalad7.mixinextras.sugar.Local;
import dev.amblelabs.celestium.api.GridRiderExtKt;
import dev.amblelabs.celestium.world.grid.SubGrid;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LivingEntity.class)
public abstract class LivingEntityGridAnimationMixin {

    @ModifyArg(
        method = "calculateEntityAnimation",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;updateWalkAnimation(F)V"
        )
    )
    private float celestium$useRelativeMovementForAnimation(
            float vanillaDistance,
            @Local(argsOnly = true) boolean useY
    ) {
        Entity entity = (Entity) (Object) this;
        SubGrid grid = GridRiderExtKt.getAttachedGrid(entity);

        if (grid == null) return vanillaDistance;

        Vec3 gridDelta = grid.getLastMovement();

        // might be needed??
//        if (gridDelta.lengthSqr() < 1.0E-12) return vanillaDistance;

        double dx = entity.getX() - entity.xOld - gridDelta.x;
        double dz = entity.getZ() - entity.zOld - gridDelta.z;

        double dy = useY
                ? entity.getY() - entity.yOld - gridDelta.y
                : 0.0;

        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}