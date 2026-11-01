package dev.amblelabs.celestium.world.level.entity

import dev.amblelabs.celestium.init.CelestiumEntityTypes
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.PathfinderMob
import net.minecraft.world.entity.ai.attributes.AttributeSupplier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.level.Level

class PocketStarEntity(entityType: EntityType<out PocketStarEntity>, level: Level) : PathfinderMob(entityType, level) {
    constructor(level: Level) : this(CelestiumEntityTypes.POCKET_STAR, level)

    override fun isNoAi(): Boolean {
        return true
    }

    override fun isNoGravity(): Boolean {
        return true
    }

    companion object {
        @JvmStatic
        fun createPocketStarAttributes(): AttributeSupplier.Builder {
            return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
        }
    }
}