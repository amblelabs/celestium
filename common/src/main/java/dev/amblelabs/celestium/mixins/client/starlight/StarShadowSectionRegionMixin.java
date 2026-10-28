package dev.amblelabs.celestium.mixins.client.starlight;

import dev.amblelabs.celestium.client.starlight.CameraStarlight;
import dev.amblelabs.celestium.client.starlight.StarShadows;
import dev.amblelabs.celestium.init.CelestiumDimensions;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(RenderSectionRegion.class)
public abstract class StarShadowSectionRegionMixin {

    @Shadow @Final private int minSectionX;
    @Shadow @Final private int minSectionY;
    @Shadow @Final private int minSectionZ;
    @Shadow @Final private ClientLevel level;

    @Unique
    private final Long2IntOpenHashMap celestium$shadowedSky = new Long2IntOpenHashMap();

    public int getBrightness(LightLayer layer, BlockPos pos) {
        RenderSectionRegion self = (RenderSectionRegion) (Object) this;
        int value = self.getLightEngine().getLayerListener(layer).getLightValue(pos);
        if (layer != LightLayer.SKY || level.dimension() != CelestiumDimensions.SPACE_KEY) return value;
        Vector3fc toward = CameraStarlight.shadowDirection();
        if (toward == null) return value;

        long key = pos.asLong();
        if (celestium$shadowedSky.containsKey(key)) return celestium$shadowedSky.get(key);
        int minX = minSectionX << 4, minY = minSectionY << 4, minZ = minSectionZ << 4;
        int shaded = StarShadows.skyLight(self, pos, value, toward,
                minX, minY, minZ, minX + 47, minY + 47, minZ + 47);
        celestium$shadowedSky.put(key, shaded);
        return shaded;
    }
}
