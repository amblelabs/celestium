package dev.amblelabs.celestium.client.model

import dev.amblelabs.celestium.Celestium
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.geom.ModelLayerLocation
import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState

class PocketStarEntityModel(root: ModelPart) : EntityModel<LivingEntityRenderState>(root) {
    companion object {
        @JvmField
        val LAYER_LOCATION: ModelLayerLocation = ModelLayerLocation(
            Celestium.id("pocket_star_model"), "main"
        )

        @JvmStatic
        fun createBodyLayer(): LayerDefinition {
            val def = MeshDefinition()

            def.root.addOrReplaceChild(
                "main",
                CubeListBuilder.create().texOffs(0, 0)
                    .addBox(-4.0f, -12.0f, -4.0f, 8.0f, 8.0f, 8.0f, CubeDeformation(0.0f)),
                PartPose.offset(0.0f, 24.0f, 0.0f)
            )

            return LayerDefinition.create(def, 32, 32)
        }
    }
}