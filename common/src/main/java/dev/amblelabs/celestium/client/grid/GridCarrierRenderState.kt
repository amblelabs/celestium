package dev.amblelabs.celestium.client.grid

import net.minecraft.client.renderer.entity.state.EntityRenderState

class GridCarrierRenderState : EntityRenderState() {
    var blocks: MutableList<GridBlockRenderState> = mutableListOf()
}
