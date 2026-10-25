package dev.amblelabs.celestium.core;

import dev.amblelabs.celestium.core.items.PocketStarItem;
import dev.amblelabs.celestium.platform.Services;
import dev.amblelabs.celestium.platform.services.PlatformHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

import java.util.function.Function;

public class CelestiumItems {

    private static final PlatformHelper.Registrar<Item> REGISTRAR = Services.PLATFORM.createRegistrar(Registries.ITEM);

    public static final Holder<Item> POCKET_STAR = item("pocket_star", PocketStarItem::new);

    private static Holder<Item> item(String name, Function<Item.Properties, Item> factory) {
        return REGISTRAR.registerHolder(name, identifier -> factory.apply(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, identifier))));
    }

    public static void register() {
        REGISTRAR.registerAll();
    }
}
