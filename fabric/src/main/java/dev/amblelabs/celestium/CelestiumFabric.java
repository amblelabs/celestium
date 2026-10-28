package dev.amblelabs.celestium;

import dev.amblelabs.celestium.command.OrbitCommand;
import dev.amblelabs.celestium.init.CelestiumEntityTypeIds;
import dev.amblelabs.celestium.init.CelestiumEntityTypes;
import dev.amblelabs.celestium.init.CelestiumRegistries;
import dev.amblelabs.celestium.network.protocol.game.GridWaitPayload;
import dev.amblelabs.celestium.network.protocol.game.SubGridPayload;
import dev.amblelabs.celestium.space.AstralBody;
import dev.amblelabs.celestium.space.StarSystem;
import dev.amblelabs.celestium.world.level.entitiy.PocketStarEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

public class CelestiumFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Celestium.init();

        Celestium.MAX_FLOATATER_SIZE = GameRuleBuilder.forInteger(256).minValue(1).buildAndRegister(Celestium.id("max_floatater_size"));

        register(CelestiumEntityTypeIds.POCKET_STAR, CelestiumEntityTypes.POCKET_STAR, PocketStarEntity.createPocketStarAttributes());
        register(CelestiumEntityTypeIds.GRID_CARRIER, CelestiumEntityTypes.GRID_CARRIER);

        PayloadTypeRegistry.clientboundPlay().register(SubGridPayload.TYPE, SubGridPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(GridWaitPayload.TYPE, GridWaitPayload.CODEC);

        DynamicRegistries.registerSynced(CelestiumRegistries.ASTRAL_BODY, AstralBody.CODEC);
        DynamicRegistries.registerSynced(CelestiumRegistries.STAR_SYSTEM, StarSystem.CODEC);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> OrbitCommand.register(dispatcher));
    }

    private static <T extends Entity> EntityType<T> register(ResourceKey<EntityType<?>> key, EntityType<T> entity) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, entity);
    }

    private static <T extends LivingEntity> EntityType<T> register(ResourceKey<EntityType<?>> key, EntityType<T> entity, AttributeSupplier.Builder attributes) {
        EntityType<T> type = register(key, entity);
        FabricDefaultAttributeRegistry.register(type, attributes);
        return type;
    }
}