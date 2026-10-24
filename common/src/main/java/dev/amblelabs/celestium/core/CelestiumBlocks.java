package dev.amblelabs.celestium.core;

import dev.amblelabs.celestium.Celestium;
import dev.amblelabs.celestium.core.blocks.TestSpongeBlock;
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

    public static final Block TEST_SPONGE =
            registerBlock("test_sponge",
                    properties -> new TestSpongeBlock(properties
                            .strength(0.5F)
                            .sound(SoundType.STONE)
                    ));

    private static <T extends Block> T registerBlock(String name, Function<BlockBehaviour.Properties, T> function) {
        Identifier id = Identifier.fromNamespaceAndPath(Celestium.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);

        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of().setId(blockKey);
        T block = function.apply(properties);

        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        registerBlockItem(name, block);
        return block;
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
    }
}
