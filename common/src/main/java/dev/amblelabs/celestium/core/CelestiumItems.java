package dev.amblelabs.celestium.core;

import dev.amblelabs.celestium.Celestium;
import dev.amblelabs.celestium.core.items.PocketStarItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class CelestiumItems {

    public static final Item POCKET_STAR =
            registerItem("pocket_star", id -> new PocketStarItem(
                    new Item.Properties()
                            .setId(ResourceKey.create(Registries.ITEM, id))
            ));

    private static Item registerItem(String name, java.util.function.Function<Identifier, Item> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(Celestium.MOD_ID, name);
        Item item = factory.apply(id);
        Registry.register(BuiltInRegistries.ITEM, id, item);
        return item;
    }

    public static void register() {
    }
}
