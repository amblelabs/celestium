package dev.amblelabs.celestium.world.item

import dev.amblelabs.celestium.world.level.entity.PocketStarEntity
import net.minecraft.server.level.ServerLevel
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.level.Level

class PocketStarItem(properties: Properties) : Item(properties) {

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        val itemStack = player.getItemInHand(hand)

        if (level is ServerLevel) {
            val look = player.lookAngle
            val x = player.x + look.x
            val y = player.eyeY - 0.1
            val z = player.z + look.z

            val star = PocketStarEntity(level)
            star.setPos(x, y, z)
            level.addFreshEntity(star)

            player.awardStat(Stats.ITEM_USED.get(this))
            itemStack.consume(1, player)
        }

        return InteractionResult.SUCCESS
    }
}
