package dev.amblelabs.celestium;

import dev.amblelabs.celestium.core.CelestiumEntityTypeIds;
import dev.amblelabs.celestium.core.CelestiumEntityTypes;
import dev.amblelabs.celestium.core.entities.PocketStarEntity;
import net.fabricmc.api.ModInitializer;
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
        // This method is invoked by the Fabric mod loader when it is ready
        // to load your mod. You can access Fabric and Common code in this
        // project.

        // Use Fabric to bootstrap the Common mod.
        Celestium.LOGGER.info("Hello Fabric world!");
        Celestium.init();

        register(CelestiumEntityTypeIds.POCKET_STAR, CelestiumEntityTypes.POCKET_STAR, PocketStarEntity.createPocketStarAttributes());

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