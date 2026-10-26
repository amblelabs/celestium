package dev.amblelabs.celestium.mixins.grid;

import dev.amblelabs.celestium.api.GridRider;
import dev.amblelabs.celestium.network.protocol.game.GridWaitPayload;
import dev.amblelabs.celestium.platform.Services;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

   @Inject(method = "placeNewPlayer", at = @At("TAIL"))
   private void celestium$sendPendingGrid(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
      UUID gridId = ((GridRider) player).celestium$pendingGrid();

      if (gridId != null)
         Services.PLATFORM.sendToPlayer(player, new GridWaitPayload(gridId));
   }
}
