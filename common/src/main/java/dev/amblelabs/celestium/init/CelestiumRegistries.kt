package dev.amblelabs.celestium.init;

import dev.amblelabs.celestium.Celestium;
import dev.amblelabs.celestium.space.AstralBody;
import dev.amblelabs.celestium.space.StarSystem;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public class CelestiumRegistries {
    public static final ResourceKey<Registry<AstralBody>> ASTRAL_BODY = ResourceKey.createRegistryKey(Celestium.id("astral_body"));
    public static final ResourceKey<Registry<StarSystem>> STAR_SYSTEM = ResourceKey.createRegistryKey(Celestium.id("star_system"));
}
