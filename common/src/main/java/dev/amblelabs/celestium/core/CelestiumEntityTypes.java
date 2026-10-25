package dev.amblelabs.celestium.core;

import dev.amblelabs.celestium.core.entities.PocketStarEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

public class CelestiumEntityTypes {

    public static final EntityType<PocketStarEntity> POCKET_STAR =
            EntityType.Builder.of((EntityType<PocketStarEntity> type, Level level) -> new PocketStarEntity(type, level), MobCategory.MISC)
                    .sized(1f, 1f)
                    .fireImmune()
                    .build(CelestiumEntityTypeIds.POCKET_STAR);


    public static void initialize() {

    }
}