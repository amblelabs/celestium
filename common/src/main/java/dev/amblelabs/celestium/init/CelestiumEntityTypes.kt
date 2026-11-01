package dev.amblelabs.celestium.init

import dev.amblelabs.celestium.world.grid.GridCarrier
import dev.amblelabs.celestium.world.level.entity.PocketStarEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.MobCategory

object CelestiumEntityTypes {
    @JvmField
    val POCKET_STAR: EntityType<PocketStarEntity> =
        EntityType.Builder.of(::PocketStarEntity, MobCategory.MISC)
            .sized(1f, 1f)
            .fireImmune()
            .build(CelestiumEntityTypeIds.POCKET_STAR)

    @JvmField
    val GRID_CARRIER: EntityType<GridCarrier> =
        EntityType.Builder.of(::GridCarrier, MobCategory.MISC)
            .sized(1f, 1f)
            .fireImmune()
            .build(CelestiumEntityTypeIds.GRID_CARRIER)

    @JvmStatic
    fun initialize() {
    }
}