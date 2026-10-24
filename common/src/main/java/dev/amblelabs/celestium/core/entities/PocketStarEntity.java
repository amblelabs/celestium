package dev.amblelabs.celestium.core.entities;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class PocketStarEntity extends PathfinderMob {

    public PocketStarEntity(EntityType<? extends PocketStarEntity> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createPocketStarAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.0D);
    }

    @Override
    public boolean isNoAi() {
        return true;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }


}