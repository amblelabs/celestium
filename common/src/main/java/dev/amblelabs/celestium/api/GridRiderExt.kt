package dev.amblelabs.celestium.api

import dev.amblelabs.celestium.world.grid.SubGrid
import net.minecraft.world.entity.Entity
import java.util.UUID

fun Entity.waitForGrid(gridId: UUID, timeout: Int) {
    (this as GridRider).`celestium$waitForGrid`(gridId, timeout)
}

val Entity.pendingGrid: UUID?
    get() = (this as GridRider).`celestium$pendingGrid`()

val Entity.attachedGrid: SubGrid?
    get() = (this as GridRider).`celestium$attachedGrid`()