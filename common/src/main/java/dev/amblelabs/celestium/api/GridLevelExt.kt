package dev.amblelabs.celestium.api

import dev.amblelabs.celestium.world.grid.GridCarrier
import dev.amblelabs.celestium.world.grid.SubGrid
import net.minecraft.world.level.Level
import java.util.UUID

fun Level.createSubGrid(carrier: GridCarrier): SubGrid {
    return SubGrid(this, carrier)
}

fun Level.grids(): MutableMap<UUID, SubGrid> {
    return (this as GridLevel).`celestium$grids`()
}

fun Level.snapshot(): List<SubGrid> {
    val grids = this.grids()
    return if (grids.isEmpty()) listOf() else grids.values.toList()
}

fun Level.trackGrid(carrier: GridCarrier) {
    this.grids()[carrier.uuid] = carrier.grid()
}

fun Level.untrackGrid(carrier: GridCarrier) {
    this.grids().remove(carrier.uuid, carrier.grid())
}