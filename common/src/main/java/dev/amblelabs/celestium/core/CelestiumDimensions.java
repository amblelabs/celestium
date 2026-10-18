package dev.amblelabs.celestium.core;

import dev.amblelabs.celestium.Celestium;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

public class CelestiumDimensions {

    public static final ResourceKey<Level> SPACE_KEY = ResourceKey.create(
            Registries.DIMENSION,
            Celestium.id("space")
    );

    public static final ResourceKey<LevelStem> SPACE_OPTIONS_KEY = ResourceKey.create(
            Registries.LEVEL_STEM,
            Celestium.id("space")
    );

    public static final ResourceKey<DimensionType> SPACE_TYPE_KEY = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            Celestium.id("space_type")
    );
}
