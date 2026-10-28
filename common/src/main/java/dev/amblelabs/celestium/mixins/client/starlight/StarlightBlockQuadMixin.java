package dev.amblelabs.celestium.mixins.client.starlight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.QuadInstance;
import dev.amblelabs.celestium.client.starlight.CameraStarlight;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelBlockRenderer.class)
public class StarlightBlockQuadMixin {

    @WrapOperation(method = "putQuadWithTint", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
    private void celestium$starFacing(BlockQuadOutput output, float x, float y, float z, BakedQuad quad, QuadInstance instance,
                                      Operation<Void> original, @Local(argsOnly = true) BlockAndTintGetter level) {
        if (level.cardinalLighting() == CameraStarlight.cardinalLighting()) {
            CameraStarlight.applyStarFacing(quad, instance);
        }
        original.call(output, x, y, z, quad, instance);
    }
}
