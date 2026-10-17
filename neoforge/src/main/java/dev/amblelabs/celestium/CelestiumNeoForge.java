package dev.amblelabs.celestium;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Celestium.MOD_ID)
public class CelestiumNeoForge {

    public CelestiumNeoForge(IEventBus eventBus) {
        // This method is invoked by the NeoForge mod loader when it is ready
        // to load your mod. You can access NeoForge and Common code in this
        // project.

        // Use NeoForge to bootstrap the Common mod.
        Celestium.LOGGER.info("Hello NeoForge world!");
        Celestium.init();
    }
}