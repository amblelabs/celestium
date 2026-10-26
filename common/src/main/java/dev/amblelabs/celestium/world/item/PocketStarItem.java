package dev.amblelabs.celestium.world.item;


import dev.amblelabs.celestium.world.level.entitiy.PocketStarEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class PocketStarItem extends Item{

    public PocketStarItem(final Properties properties) {
        super(properties);
    }

    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);

        if (level instanceof ServerLevel serverLevel) {
            Vec3 look = player.getLookAngle();
            double x = player.getX() + look.x;
            double y = player.getEyeY() - 0.1;
            double z = player.getZ() + look.z;

            PocketStarEntity star = new PocketStarEntity(level);
            star.setPos(x, y, z);
            serverLevel.addFreshEntity(star);

            player.awardStat(Stats.ITEM_USED.get(this));
            itemStack.consume(1, player);
        }

        return InteractionResult.SUCCESS;
    }
}
