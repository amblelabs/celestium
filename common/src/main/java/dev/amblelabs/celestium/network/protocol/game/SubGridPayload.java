package dev.amblelabs.celestium.network.protocol.game;

import dev.amblelabs.celestium.Celestium;
import dev.amblelabs.celestium.world.grid.GridCarrier;
import dev.amblelabs.celestium.world.grid.SubGridBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.NonNull;

public record SubGridPayload(int entityId, SubGridBlocks blocks, Holder<Biome> biome) implements CustomPacketPayload {

   public static final Type<SubGridPayload> TYPE = new Type<>(Celestium.id("sub_grid/create"));

   public static final StreamCodec<RegistryFriendlyByteBuf, SubGridPayload> CODEC = StreamCodec.composite(
           ByteBufCodecs.VAR_INT, SubGridPayload::entityId,
           SubGridBlocks.STREAM_CODEC, SubGridPayload::blocks,
           ByteBufCodecs.holderRegistry(Registries.BIOME), SubGridPayload::biome,
           SubGridPayload::new
   );

   public @NonNull Type<SubGridPayload> type() {
      return TYPE;
   }

   public void apply(Level level) {
      Entity entity = level.getEntity(this.entityId);

      if (entity instanceof GridCarrier carrier) {
         carrier.grid().setBlocks(blocks);
         carrier.grid().setBiome(biome);
      }
   }
}
