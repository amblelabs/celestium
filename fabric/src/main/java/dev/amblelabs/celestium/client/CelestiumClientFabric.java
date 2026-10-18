package dev.amblelabs.celestium.client;

import dev.amblelabs.celestium.client.render.SpaceSkyRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public class CelestiumClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LevelRenderEvents.COLLECT_SUBMITS.register(SpaceSkyRenderer::render);
    }
}
