@file:Suppress("FunctionName")

package dev.amblelabs.celestium.api

import dev.amblelabs.celestium.world.grid.SubGrid
import net.minecraft.world.entity.Entity
import java.util.*

interface GridRider {
    fun `celestium$pendingGrid`(): UUID?

    fun `celestium$attachedGrid`(): SubGrid?

    fun `celestium$waitForGrid`(gridId: UUID?, timeout: Int)

    companion object {
        const val SERVER_RELOAD_TIMEOUT: Int = 40
        const val CLIENT_RELOAD_TIMEOUT: Int = 60
    }
}

fun Entity.waitForGrid(gridId: UUID, timeout: Int) {
    (this as GridRider).`celestium$waitForGrid`(gridId, timeout)
}

val Entity.pendingGrid: UUID?
    get() = (this as GridRider).`celestium$pendingGrid`()

val Entity.attachedGrid: SubGrid?
    get() = (this as GridRider).`celestium$attachedGrid`()