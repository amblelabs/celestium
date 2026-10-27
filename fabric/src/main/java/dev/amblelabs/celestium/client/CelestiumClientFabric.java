package dev.amblelabs.celestium.client;

import dev.amblelabs.celestium.Celestium;
import dev.amblelabs.celestium.client.grid.GridCarrierRenderer;
import dev.amblelabs.celestium.client.model.PocketStarEntityModel;
import dev.amblelabs.celestium.client.particle.ThrusterParticle;
import dev.amblelabs.celestium.client.render.SpaceSkyRenderer;
import dev.amblelabs.celestium.client.renderers.PocketStarEntityRenderer;
import dev.amblelabs.celestium.init.CelestiumEntityTypes;
import dev.amblelabs.celestium.network.protocol.game.GridWaitPayload;
import dev.amblelabs.celestium.network.protocol.game.SubGridPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
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

        EntityRenderers.register(
                CelestiumEntityTypes.GRID_CARRIER,
                GridCarrierRenderer::new
        );

        ParticleProviderRegistry.getInstance().register(
                Celestium.THRUSTER,
                ThrusterParticle.Provider::new
        );

        ClientPlayNetworking.registerGlobalReceiver(SubGridPayload.TYPE, (payload, context) -> payload.apply(context.player().level()));
        ClientPlayNetworking.registerGlobalReceiver(GridWaitPayload.TYPE, (payload, context) -> payload.apply(context.player()));
    }
}
