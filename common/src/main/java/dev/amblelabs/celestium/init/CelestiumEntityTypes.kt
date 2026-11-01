package dev.amblelabs.celestium.init;

import dev.amblelabs.celestium.world.level.entity.PocketStarEntity;
import dev.amblelabs.celestium.world.grid.GridCarrier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class CelestiumEntityTypes {

    public static final EntityType<PocketStarEntity> POCKET_STAR =
            EntityType.Builder.<PocketStarEntity>of(PocketStarEntity::new, MobCategory.MISC)
                    .sized(1f, 1f)
                    .fireImmune()
                    .build(CelestiumEntityTypeIds.POCKET_STAR);

    public static final EntityType<GridCarrier> GRID_CARRIER =
            EntityType.Builder.of(GridCarrier::new, MobCategory.MISC)
                    .sized(1f, 1f)
                    .fireImmune()
                    .build(CelestiumEntityTypeIds.GRID_CARRIER);

    public static void initialize() {

    }
}