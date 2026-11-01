package dev.amblelabs.celestium.network.protocol.game

import dev.amblelabs.celestium.Celestium
import dev.amblelabs.celestium.api.GridRider
import dev.amblelabs.celestium.api.waitForGrid
import io.netty.buffer.ByteBuf
import net.minecraft.core.UUIDUtil
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.world.entity.player.Player
import java.util.*
import java.util.function.Function

@JvmRecord
data class GridWaitPayload(val gridId: UUID) : CustomPacketPayload {
    override fun type(): CustomPacketPayload.Type<GridWaitPayload> {
        return TYPE
    }

    fun apply(player: Player) {
        player.waitForGrid(this.gridId, GridRider.CLIENT_RELOAD_TIMEOUT)
    }

    companion object {
        @JvmField
        val TYPE: CustomPacketPayload.Type<GridWaitPayload> =
            CustomPacketPayload.Type(Celestium.id("sub_grid/wait"))

        @JvmField
        val CODEC: StreamCodec<ByteBuf, GridWaitPayload> =
            UUIDUtil.STREAM_CODEC.map(
                Function { gridId: UUID -> GridWaitPayload(gridId) }, GridWaitPayload::gridId
            )
    }
}
