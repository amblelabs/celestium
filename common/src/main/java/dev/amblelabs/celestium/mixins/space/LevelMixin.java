package dev.amblelabs.celestium.mixins.space;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.amblelabs.celestium.init.CelestiumDimensions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.Consumer;

@Mixin(Level.class)
public abstract class LevelMixin {

    @WrapMethod(method = "guardEntityTick")
    private <T extends Entity> void celestium$zeroDrag(Consumer<T> tick, T entity, Operation<Void> original) {
        if (entity instanceof LivingEntity || !entity.level().dimension().equals(CelestiumDimensions.SPACE_KEY)) {
            original.call(tick, entity);
            return;
        }

        Vec3 before = entity.getDeltaMovement();
        original.call(tick, entity);
        if (entity.onGround()) return;

        Vec3 after = entity.getDeltaMovement();
        entity.setDeltaMovement(
                after.x * before.x > 0 && Math.abs(after.x) < Math.abs(before.x) ? before.x : after.x,
                after.y * before.y > 0 && Math.abs(after.y) < Math.abs(before.y) ? before.y : after.y,
                after.z * before.z > 0 && Math.abs(after.z) < Math.abs(before.z) ? before.z : after.z);
    }
}
