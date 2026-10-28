package dev.amblelabs.celestium.mixins.client.starlight;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.amblelabs.celestium.client.starlight.CameraStarlight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.model.geom.ModelPart$Cube")
public class StarlightModelPartMixin {

    @WrapOperation(method = "compile", at = @At(value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;addVertex(FFFIFFIIFFF)V"))
    private void celestium$perVertexStarlight(VertexConsumer builder, float x, float y, float z, int color, float u, float v,
                                              int overlay, int light, float nx, float ny, float nz, Operation<Void> original) {
        original.call(builder, x, y, z, color, u, v, overlay, CameraStarlight.modelVertexLight(x, y, z, light), nx, ny, nz);
    }
}
