package dev.amblelabs.celestium.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;

public class TestSpongeBlock extends Block {
    private static final Direction[] ALL_DIRECTIONS = Direction.values();

    ArrayList<BlockPos> positions = new ArrayList<>();

    public void Poses(BlockPos pos){
        positions.add(pos);
    }

    public TestSpongeBlock(final BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(final BlockState state, final @NonNull Level level, final @NonNull BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
        if (!oldState.is(state.getBlock())) {
            this.tryAbsorbWater(level, pos);
        }
    }

    @Override
    protected void neighborChanged(final @NonNull BlockState state, final @NonNull Level level, final @NonNull BlockPos pos, final @NonNull Block block, final @Nullable Orientation orientation, final boolean movedByPiston) {
        this.tryAbsorbWater(level, pos);
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }

    protected void tryAbsorbWater(final Level level, final BlockPos pos) {
        positions.clear();
        if (this.removeWaterBreadthFirstSearch(level, pos)) {
            level.playSound(null, pos, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 1.0F, 1.0F);

            System.out.println("Distributed gas: " + positions.size() + " blocks");
        }
    }

    private boolean removeWaterBreadthFirstSearch(final Level level, final BlockPos startPos) {
        return BlockPos.breadthFirstTraversal(startPos, 6, 65, (pos, consumer) -> {
            for(Direction direction : ALL_DIRECTIONS) {
                consumer.accept(pos.relative(direction));
            }

        }, (pos) -> {
            if (pos.equals(startPos)) {
                return BlockPos.TraversalNodeStatus.ACCEPT;
            } else {
                BlockState state = level.getBlockState(pos);
                FluidState fluidState = level.getFluidState(pos);
                if (!fluidState.is(FluidTags.WATER)) {
                    return BlockPos.TraversalNodeStatus.SKIP;
                } else {
                    Block patt0$temp = state.getBlock();
                    if (patt0$temp instanceof BucketPickup bucketPickup) {
                        if (!bucketPickup.pickupBlock(null, level, pos, state).isEmpty()) {
                            this.Poses(pos);
                            return BlockPos.TraversalNodeStatus.ACCEPT;
                        }
                    }

                    if (state.getBlock() instanceof LiquidBlock) {
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        this.Poses(pos);
                    } else {
                        if (!state.is(Blocks.KELP) && !state.is(Blocks.KELP_PLANT) && !state.is(Blocks.SEAGRASS) && !state.is(Blocks.TALL_SEAGRASS)) {
                            return BlockPos.TraversalNodeStatus.SKIP;
                        }

                        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
                        dropResources(state, level, pos, blockEntity);
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                        this.Poses(pos);
                    }

                    return BlockPos.TraversalNodeStatus.ACCEPT;
                }
            }
        }) > 1;
    }
}
