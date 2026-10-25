package dev.amblelabs.celestium.core;

import dev.amblelabs.celestium.Celestium;
import dev.amblelabs.celestium.core.blocks.TestSpongeBlock;
import dev.amblelabs.celestium.platform.Services;
import dev.amblelabs.celestium.platform.services.PlatformHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

public class CelestiumBlocks {

    private static final PlatformHelper.Registrar<Block> REGISTRAR = Services.PLATFORM.createRegistrar(Registries.BLOCK);

    public static final Holder<Block> TEST_SPONGE = block("test_sponge",
                    properties -> new TestSpongeBlock(properties
                            .strength(0.5F)
                            .sound(SoundType.STONE)
                    ));

    private static <T extends Block> Holder<T> block(String name, Function<BlockBehaviour.Properties, T> function) {
        return REGISTRAR.registerHolder(name, id -> {
            ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);

            BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().setId(blockKey);
            return function.apply(properties);
        });
    }

    private static <T extends Block> void registerBlockItem(String name, T block) {
        Identifier id = Identifier.fromNamespaceAndPath(Celestium.MOD_ID, name);
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);

        Item.Properties properties = new Item.Properties()
                .useBlockDescriptionPrefix()
                .setId(itemKey);

        Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, properties));
    }

    public static void register() {
        REGISTRAR.registerAll();
    }
}
