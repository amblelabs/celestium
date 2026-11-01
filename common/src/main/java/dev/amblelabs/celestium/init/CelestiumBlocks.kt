package dev.amblelabs.celestium.init

import dev.amblelabs.celestium.Celestium
import dev.amblelabs.celestium.platform.Services
import dev.amblelabs.celestium.platform.services.PlatformHelper
import dev.amblelabs.celestium.world.level.block.FloataterBlock
import dev.amblelabs.celestium.world.level.block.TestSpongeBlock
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import java.util.function.Function

object CelestiumBlocks {
    private val REGISTRAR: PlatformHelper.Registrar<Block> =
        Services.PLATFORM.createRegistrar<Block>(Registries.BLOCK)

    val TEST_SPONGE: Holder<Block> = block(
        "test_sponge"
    ) { properties: BlockBehaviour.Properties? ->
        TestSpongeBlock(
            properties!!
                .strength(0.5f)
                .sound(SoundType.STONE)
        )
    }

    @JvmField
    val FLOATATER: Holder<Block> = block(
        "floatater"
    ) { properties: BlockBehaviour.Properties? ->
        FloataterBlock(
            properties!!
                .strength(0.5f)
                .sound(SoundType.IRON)
                .noOcclusion()
        )
    }

    private fun <T : Block> block(name: String, function: Function<BlockBehaviour.Properties, T>): Holder<T> {
        return REGISTRAR.registerHolder(name) { id: Identifier ->
            val blockKey: ResourceKey<Block> = ResourceKey.create(Registries.BLOCK, id)
            val properties = BlockBehaviour.Properties.of().setId(blockKey)
            function.apply(properties)
        }
    }

    private fun <T : Block> registerBlockItem(name: String, block: T) {
        val id = Identifier.fromNamespaceAndPath(Celestium.MOD_ID, name)
        val itemKey: ResourceKey<Item> = ResourceKey.create(Registries.ITEM, id)

        val properties = Item.Properties()
            .useBlockDescriptionPrefix()
            .setId(itemKey)

        Registry.register(BuiltInRegistries.ITEM, itemKey, BlockItem(block, properties))
    }

    @JvmStatic
    fun register() {
        REGISTRAR.registerAll()
    }
}
