package dev.amblelabs.celestium.init

import dev.amblelabs.celestium.Celestium
import dev.amblelabs.celestium.space.AstralBody
import dev.amblelabs.celestium.space.StarSystem
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey

object CelestiumRegistries {
    @JvmField
    val ASTRAL_BODY: ResourceKey<Registry<AstralBody>> =
        ResourceKey.createRegistryKey(Celestium.id("astral_body"))

    @JvmField
    val STAR_SYSTEM: ResourceKey<Registry<StarSystem>> =
        ResourceKey.createRegistryKey(Celestium.id("star_system"))
}
