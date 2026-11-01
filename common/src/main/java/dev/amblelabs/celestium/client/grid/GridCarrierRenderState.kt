package dev.amblelabs.celestium.client.grid;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

import java.util.List;

public class GridCarrierRenderState extends EntityRenderState {
   public List<GridBlockRenderState> blocks = List.of();
}
