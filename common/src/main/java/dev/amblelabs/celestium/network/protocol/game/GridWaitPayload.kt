package dev.amblelabs.celestium.network.protocol.game;

import dev.amblelabs.celestium.Celestium;
import dev.amblelabs.celestium.api.GridRider;
import dev.amblelabs.celestium.api.GridRiderExtKt;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public record GridWaitPayload(UUID gridId) implements CustomPacketPayload {

   public static final Type<GridWaitPayload> TYPE = new Type<>(Celestium.id("sub_grid/wait"));

   public static final StreamCodec<ByteBuf, GridWaitPayload> CODEC = UUIDUtil.STREAM_CODEC.cast().map(GridWaitPayload::new, GridWaitPayload::gridId);

   public @NonNull Type<GridWaitPayload> type() {
      return TYPE;
   }

   public void apply(Player player) {
      GridRiderExtKt.waitForGrid(player, this.gridId, GridRider.CLIENT_RELOAD_TIMEOUT);
   }
}
