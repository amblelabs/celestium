package dev.amblelabs.celestium.world.level.block;

import dev.amblelabs.celestium.Celestium;
import dev.amblelabs.celestium.init.CelestiumEntityTypes;
import dev.amblelabs.celestium.world.grid.FlyingTickable;
import dev.amblelabs.celestium.world.grid.GridCarrier;
import dev.amblelabs.celestium.world.grid.SubGridBlocks;
import dev.amblelabs.celestium.world.grid.SubGridCapture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class FloataterBlock extends Block implements FlyingTickable {

    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    public FloataterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return this.defaultBlockState().setValue(FACING, blockPlaceContext.getPlayer().isCrouching() ? blockPlaceContext.getNearestLookingDirection().getOpposite() : blockPlaceContext.getNearestLookingDirection());
    }

    @Override
    protected @NonNull BlockState rotate(BlockState blockState, Rotation rotation) {
        return blockState.setValue(FACING, rotation.rotate(blockState.getValue(FACING)));
    }

    @Override
    protected @NonNull BlockState mirror(BlockState blockState, Mirror mirror) {
        return blockState.rotate(mirror.getRotation(blockState.getValue(FACING)));
    }

    @Override
    protected void neighborChanged(@NonNull BlockState state, @NonNull Level level, @NonNull BlockPos pos, @NonNull Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);

        if (powered != state.getValue(TRIGGERED)) {
            if (powered) level.scheduleTick(pos, this, 1);
            level.setBlock(pos, state.setValue(TRIGGERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void tick(BlockState blockState, @NonNull ServerLevel level, @NonNull BlockPos blockPos, @NonNull RandomSource randomSource) {
        Direction direction = blockState.getValue(FACING);
        SubGridCapture capture = SubGridCapture.scan(level, blockPos, direction);

        if (capture != null) {
            GridCarrier carrier = new GridCarrier(CelestiumEntityTypes.GRID_CARRIER, level);
            BlockPos minPos = capture.minPos();

            carrier.snapTo(minPos.getX(), minPos.getY(), minPos.getZ());
            carrier.grid().setBlocks(capture.blocks());
            carrier.setYRot(direction.toYRot());
            carrier.grid().setBiome(level.getBiome(blockPos));
            carrier.setMovement(direction, capture.engines() * 0.1F);
            capture.remove(level);

            level.addFreshEntity(carrier);
        }
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING, TRIGGERED);
    }

    @Override
    public void flyingTick(Level level, SubGridBlocks blocks, BlockState state, BlockPos pos, Vec3 worldPos, Direction movement) {
        if (level.isClientSide()) {
            Direction facing = state.getValue(FACING);
            if (movement == facing && state.getValue(TRIGGERED) && level.getRandom().nextBoolean()) {
                Direction exhaust = facing.getOpposite();
                if (blocks.getBlockState(pos.relative(exhaust)).isAir()) {
                    double d = 0.5;
                    worldPos = worldPos.add(d)
                            .add(exhaust.getStepX() * d, exhaust.getStepY() * d, exhaust.getStepZ() * d);

                    level.addParticle(Celestium.THRUSTER, worldPos.x, worldPos.y, worldPos.z, 0, -0.2, 0);
                }
            }
        }
    }
}