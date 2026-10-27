package dev.amblelabs.celestium.init;

import dev.amblelabs.celestium.world.item.PocketStarItem;
import dev.amblelabs.celestium.platform.Services;
import dev.amblelabs.celestium.platform.services.PlatformHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.function.Function;

public class CelestiumItems {

    private static final PlatformHelper.Registrar<Item> REGISTRAR = Services.PLATFORM.createRegistrar(Registries.ITEM);

    public static final Holder<Item> POCKET_STAR = item("pocket_star", PocketStarItem::new);

    public static final Holder<Item> FLOATATER = blockItem("floatater", CelestiumBlocks.FLOATATER);

    private static Holder<Item> item(String name, Function<Item.Properties, Item> factory) {
        return REGISTRAR.registerHolder(name, identifier -> factory.apply(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, identifier))));
    }

    private static Holder<Item> blockItem(String name, Holder<Block> block) {
        return item(name, properties -> new BlockItem(block.value(), properties.useBlockDescriptionPrefix()));
    }

    public static void register() {
        REGISTRAR.registerAll();
    }
}
