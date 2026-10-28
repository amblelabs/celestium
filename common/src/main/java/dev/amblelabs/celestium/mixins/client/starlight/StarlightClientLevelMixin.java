package dev.amblelabs.celestium.mixins.client.starlight;

import dev.amblelabs.celestium.client.grid.GridMesh;
import dev.amblelabs.celestium.client.starlight.CameraStarlight;
import dev.amblelabs.celestium.client.starlight.StarShadows;
import dev.amblelabs.celestium.init.CelestiumDimensions;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.LightLayer;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public class StarlightClientLevelMixin {

    @Unique
    private final Long2IntOpenHashMap celestium$shadowedSky = new Long2IntOpenHashMap();
    @Unique
    private int celestium$shadowFrame;

    @Inject(method = "cardinalLighting", at = @At("HEAD"), cancellable = true)
    private void celestium$starlightFaceShading(CallbackInfoReturnable<CardinalLighting> cir) {
        if (((ClientLevel) (Object) this).dimension() == CelestiumDimensions.SPACE_KEY) {
            cir.setReturnValue(CameraStarlight.cardinalLighting());
        }
    }

    public int getBrightness(LightLayer layer, BlockPos pos) {
        ClientLevel self = (ClientLevel) (Object) this;
        int value = self.getLightEngine().getLayerListener(layer).getLightValue(pos);
        if (layer == LightLayer.BLOCK) {
            return CameraStarlight.onRenderThread() ? GridMesh.withGridBlockLight(self, pos, value) : value;
        }
        if (self.dimension() != CelestiumDimensions.SPACE_KEY) return value;
        Vector3fc toward = CameraStarlight.shadowDirection();
        if (toward == null) return value;

        boolean cached = CameraStarlight.onRenderThread();
        long key = pos.asLong();
        if (cached) {
            if (celestium$shadowFrame != CameraStarlight.frame()) {
                celestium$shadowFrame = CameraStarlight.frame();
                celestium$shadowedSky.clear();
            }
            if (celestium$shadowedSky.containsKey(key)) return celestium$shadowedSky.get(key);
        }
        int shaded = StarShadows.skyLight(self, pos, value, toward,
                Integer.MIN_VALUE, self.getMinY(), Integer.MIN_VALUE, Integer.MAX_VALUE, self.getMaxY(), Integer.MAX_VALUE);
        if (cached) celestium$shadowedSky.put(key, shaded);
        return shaded;
    }
}
