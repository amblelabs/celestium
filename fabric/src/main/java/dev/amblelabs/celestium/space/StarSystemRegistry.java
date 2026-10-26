package dev.amblelabs.celestium.space;

import dev.amblelabs.celestium.Celestium;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class StarSystemRegistry {

    public static final ResourceKey<Registry<StarSystem>> STAR_SYSTEM_REGISTRY_KEY = ResourceKey.createRegistryKey(Celestium.id("star_system_registry"));

    public static final Registry<StarSystem> STAR_SYSTEM_REGISTRY = FabricRegistryBuilder.create(STAR_SYSTEM_REGISTRY_KEY).buildAndRegister();

    public static void initialize() {
        Registry.register(STAR_SYSTEM_REGISTRY, Celestium.id("solar_system"), StarSystemEntries.SOLAR_SYSTEM);
    }
}
