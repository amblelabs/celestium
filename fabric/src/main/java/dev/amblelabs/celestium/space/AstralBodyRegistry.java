package dev.amblelabs.celestium.space;

import dev.amblelabs.celestium.Celestium;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class AstralBodyRegistry {

    public static final ResourceKey<Registry<AstralBody>> ASTRAL_BODY_REGISTRY_KEY = ResourceKey.createRegistryKey(Celestium.id("astral_body_registry"));

    public static final Registry<AstralBody> ASTRAL_BODY_REGISTRY = FabricRegistryBuilder.create(ASTRAL_BODY_REGISTRY_KEY).buildAndRegister();

    public static void initialize() {
        AstralBodyEntries.ENTRIES.forEach((id, astralBody) -> Registry.register(ASTRAL_BODY_REGISTRY, id, astralBody));
    }
}
