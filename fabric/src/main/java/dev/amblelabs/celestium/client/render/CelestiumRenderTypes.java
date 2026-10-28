package dev.amblelabs.celestium.client.render;

import com.mojang.renderpearl.api.pipeline.*;
import dev.amblelabs.celestium.Celestium;
import net.minecraft.client.renderer.BindGroupLayouts;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.oit.OitPipelineSet;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.util.List;
import java.util.function.Function;

public class CelestiumRenderTypes {

    private static final OitPipelineSet OIT_SKY_GLOW = RenderPipelines.register(
            OitPipelineSet.builder("celestium_sky_glow", RenderPipeline.builder(RenderPipelines.OIT_DEBUG_FILLED_SNIPPET).withShaderDefine("OIT_ADDITIVE")).build()
    );

    private static final RenderPipeline SKY_SOLID_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Celestium.id("pipeline/sky_solid"))
                    .withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, true))
                    .build()
    );

    public static final RenderType SKY_SOLID = RenderType.create(
            "brightestday_sky_solid",
            RenderSetup.builder(SKY_SOLID_PIPELINE).setOitPipelines(RenderPipelines.OIT_DEBUG_QUADS).sortOnUpload().createRenderSetup()
    );

    private static final RenderPipeline SKY_GLOW_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
                    .withLocation(Celestium.id("pipeline/sky_glow"))
                    .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                    .build()
    );

    public static final RenderType SKY_GLOW = RenderType.create(
            "brightestday_sky_glow",
            RenderSetup.builder(SKY_GLOW_PIPELINE).setOitPipelines(OIT_SKY_GLOW).sortOnUpload().createRenderSetup()
    );

    private static final RenderPipeline PLANET_SURFACE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
                    .withLocation(Celestium.id("pipeline/planet_surface"))
                    .withShaderDefine("NO_CARDINAL_LIGHTING")
                    .withBindGroupLayout(BindGroupLayouts.SAMPLER1)
                    .withColorTargetState(ColorTargetState.DEFAULT)
                    .build()
    );

    private static final Function<Identifier, RenderType> PLANET_SURFACE = Util.memoize(texture -> RenderType.create(
            "celestium_planet_surface",
            RenderSetup.builder(PLANET_SURFACE_PIPELINE)
                    .withTexture("Sampler0", texture)
                    .useLightmap()
                    .useOverlay()
                    .createRenderSetup()
    ));

    public static RenderType planetSurface(Identifier texture) {
        return PLANET_SURFACE.apply(texture);
    }
}
