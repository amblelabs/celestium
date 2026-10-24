package dev.amblelabs.celestium.client;

import dev.amblelabs.celestium.client.model.PocketStarEntityModel;
import dev.amblelabs.celestium.client.render.SpaceSkyRenderer;
import dev.amblelabs.celestium.client.renderers.PocketStarEntityRenderer;
import dev.amblelabs.celestium.core.CelestiumEntityTypes;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.renderer.entity.EntityRenderers;

public class CelestiumClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        LevelRenderEvents.COLLECT_SUBMITS.register(SpaceSkyRenderer::render);

        ModelLayerRegistry.registerModelLayer(
                PocketStarEntityModel.LAYER_LOCATION,
                PocketStarEntityModel::createBodyLayer
        );

        EntityRenderers.register(
                CelestiumEntityTypes.POCKET_STAR,
                PocketStarEntityRenderer::new
        );
    }
}
