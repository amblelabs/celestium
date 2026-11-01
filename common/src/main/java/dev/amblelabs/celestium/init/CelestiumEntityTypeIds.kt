package dev.amblelabs.celestium.init;

import dev.amblelabs.celestium.Celestium;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

public class CelestiumEntityTypeIds {

    public static final ResourceKey<EntityType<?>> POCKET_STAR = create("pocket_star");
    public static final ResourceKey<EntityType<?>> GRID_CARRIER = create("grid_carrier");

    private static ResourceKey<EntityType<?>> create(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Celestium.id(name));
    }
}