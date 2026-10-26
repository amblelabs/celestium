package dev.amblelabs.celestium.api

import net.minecraft.world.entity.Entity
import java.util.UUID

fun Entity.waitForGrid(gridId: UUID, timeout: Int) {
    (this as GridRider).`celestium$waitForGrid`(gridId, timeout)
}