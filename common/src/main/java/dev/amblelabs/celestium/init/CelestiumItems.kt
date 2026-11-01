package dev.amblelabs.celestium.init

import dev.amblelabs.celestium.platform.Services
import dev.amblelabs.celestium.platform.services.PlatformHelper
import dev.amblelabs.celestium.world.item.PocketStarItem
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import java.util.function.Function

object CelestiumItems {
    private val REGISTRAR: PlatformHelper.Registrar<Item> = Services.PLATFORM.createRegistrar(Registries.ITEM)

    val POCKET_STAR: Holder<Item> = item("pocket_star", ::PocketStarItem)

    val FLOATATER: Holder<Item> = blockItem("floatater", CelestiumBlocks.FLOATATER)

    private fun item(name: String, factory: Function<Item.Properties, Item>): Holder<Item> {
        return REGISTRAR.registerHolder(name) { identifier: Identifier ->
            factory.apply(
                Item.Properties()
                    .setId(ResourceKey.create(Registries.ITEM, identifier))
            )
        }
    }

    private fun blockItem(name: String, block: Holder<Block>): Holder<Item> {
        return item(
            name
        ) { properties: Item.Properties ->
            BlockItem(
                block.value(),
                properties.useBlockDescriptionPrefix()
            )
        }
    }

    @JvmStatic
    fun register() {
        REGISTRAR.registerAll()
    }
}
