package dev.amblelabs.celestium.network.protocol.game

import dev.amblelabs.celestium.Celestium
import dev.amblelabs.celestium.world.grid.GridCarrier
import dev.amblelabs.celestium.world.grid.SubGridBlocks
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome

@JvmRecord
data class SubGridPayload(val entityId: Int, val blocks: SubGridBlocks, val biome: Holder<Biome>) : CustomPacketPayload {

    override fun type(): CustomPacketPayload.Type<SubGridPayload> {
       return TYPE
    }

    fun apply(level: Level) {
        val entity = level.getEntity(this.entityId)

        if (entity is GridCarrier) {
            entity.grid().blocks = blocks
            entity.grid().biome = biome
        }
    }

    companion object {
        @JvmField
        val TYPE: CustomPacketPayload.Type<SubGridPayload> =
            CustomPacketPayload.Type(Celestium.id("sub_grid/create"))

        @JvmField
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, SubGridPayload> =
            StreamCodec.composite(
                ByteBufCodecs.VAR_INT, SubGridPayload::entityId,
                SubGridBlocks.STREAM_CODEC, SubGridPayload::blocks,
                ByteBufCodecs.holderRegistry(Registries.BIOME), SubGridPayload::biome, ::SubGridPayload
            )
    }
}
