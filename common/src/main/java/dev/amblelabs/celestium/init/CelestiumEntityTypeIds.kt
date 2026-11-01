package dev.amblelabs.celestium.init

import dev.amblelabs.celestium.Celestium
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EntityType

object CelestiumEntityTypeIds {
    @JvmField
    val POCKET_STAR: ResourceKey<EntityType<*>> = create("pocket_star")
    @JvmField
    val GRID_CARRIER: ResourceKey<EntityType<*>> = create("grid_carrier")

    private fun create(name: String): ResourceKey<EntityType<*>> {
        return ResourceKey.create(Registries.ENTITY_TYPE, Celestium.id(name))
    }
}