package dev.amblelabs.celestium;

import dev.amblelabs.celestium.init.CelestiumRegistries;
import dev.amblelabs.celestium.space.AstralBody;
import dev.amblelabs.celestium.space.StarSystem;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

@Mod(Celestium.MOD_ID)
public class CelestiumNeoForge {

    public static IEventBus EVENT_BUS;

    public CelestiumNeoForge(IEventBus eventBus) {
        EVENT_BUS = eventBus;

        // This method is invoked by the NeoForge mod loader when it is ready
        // to load your mod. You can access NeoForge and Common code in this
        // project.

        // Use NeoForge to bootstrap the Common mod.
        Celestium.LOGGER.info("Hello NeoForge world!");
        Celestium.init();

        Celestium.MAX_FLOATATER_SIZE = GameRules.registerInteger("max_floatator_size", GameRuleCategory.MISC, 256, 1);
    }

    @SubscribeEvent
    public static void onDatapackRegistries(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(CelestiumRegistries.ASTRAL_BODY, AstralBody.CODEC, AstralBody.CODEC);
        event.dataPackRegistry(CelestiumRegistries.STAR_SYSTEM, StarSystem.CODEC, StarSystem.CODEC);
    }
}