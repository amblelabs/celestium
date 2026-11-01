package dev.amblelabs.celestium.init

import dev.amblelabs.celestium.Celestium
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.dimension.LevelStem

object CelestiumDimensions {
    @JvmField
    val SPACE_KEY: ResourceKey<Level> = ResourceKey.create(
        Registries.DIMENSION,
        Celestium.id("space")
    )

    val SPACE_OPTIONS_KEY: ResourceKey<LevelStem> = ResourceKey.create(
        Registries.LEVEL_STEM,
        Celestium.id("space")
    )

    val SPACE_TYPE_KEY: ResourceKey<DimensionType> = ResourceKey.create(
        Registries.DIMENSION_TYPE,
        Celestium.id("space_type")
    )
}
