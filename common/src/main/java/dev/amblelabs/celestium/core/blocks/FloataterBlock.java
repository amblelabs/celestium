package dev.amblelabs.celestium.core.blocks;

import com.mojang.serialization.MapCodec;
import dev.amblelabs.celestium.world.grid.FlyingTickable;
import dev.amblelabs.celestium.world.grid.GridCarrier;
import dev.amblelabs.celestium.world.grid.SubGridBlocks;
import dev.amblelabs.celestium.world.grid.SubGridCapture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;

public class FloataterBlock extends Block implements FlyingTickable {

    public static final MapCodec<FloataterBlock> CODEC = simpleCodec(FloataterBlock::new);
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    @Override
    protected MapCodec<FloataterBlock> codec() {
        return CODEC;
    }

    protected FloataterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState((BlockState)((BlockState)((BlockState)this.stateDefinition.any()).setValue(FACING, Direction.NORTH)).setValue(TRIGGERED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return (BlockState)this.defaultBlockState().setValue(FACING, blockPlaceContext.getNearestLookingDirection());
    }

    @Override
    protected BlockState rotate(BlockState blockState, Rotation rotation) {
        return (BlockState)blockState.setValue(FACING, rotation.rotate((Direction)blockState.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState blockState, Mirror mirror) {
        return blockState.rotate(mirror.getRotation((Direction)blockState.getValue(FACING)));
    }

    @Override
    protected void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, BlockPos blockPos2, boolean bl) {
        boolean bl2 = shouldTriggerNoQC(DoorBlock.shouldTrigger(level, blockPos), level, blockPos);
        boolean bl3 = (Boolean)blockState.getValue(TRIGGERED);
        if (bl2 != bl3) {
            if (bl2) {
                level.scheduleTick(blockPos, this, 1);
                level.setBlock(blockPos, (BlockState)blockState.setValue(TRIGGERED, true), 2);
            } else {
                level.setBlock(blockPos, (BlockState)blockState.setValue(TRIGGERED, false), 2);
            }
        }
    }

    private static boolean shouldTriggerNoQC(boolean bl, Level level, BlockPos blockPos) {
        return bl && level.hasNeighborSignal(blockPos);
    }

    @Override
    protected void tick(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos, RandomSource randomSource) {
        Direction direction = (Direction)blockState.getValue(FACING);
        SubGridCapture subGridCapture = SubGridCapture.scan(serverLevel, blockPos, direction);
        if (subGridCapture != null) {
            GridCarrier gridCarrier = new GridCarrier(EntityType.GRID_CARRIER, serverLevel);
            BlockPos blockPos2 = subGridCapture.minPos();
            gridCarrier.moveTo(blockPos2.getX(), blockPos2.getY(), blockPos2.getZ());
            gridCarrier.grid().setBlocks(subGridCapture.blocks());
            gridCarrier.grid().setBiome(serverLevel.getBiome(blockPos));
            gridCarrier.setMovement(direction, subGridCapture.engines() * 0.1F);
            subGridCapture.remove(serverLevel);
            serverLevel.addFreshEntity(gridCarrier);
        }
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(FACING, TRIGGERED);
    }

    @Override
    public void flyingTick(Level level, SubGridBlocks subGridBlocks, BlockState blockState, BlockPos blockPos, Vec3 vec3, Direction direction) {
        if (level.isClientSide()) {
            Direction direction2 = (Direction)blockState.getValue(FACING);
            if (direction == direction2 && (Boolean)blockState.getValue(TRIGGERED) && level.getRandom().nextBoolean()) {
                Direction direction3 = direction2.getOpposite();
                if (subGridBlocks.getBlockState(blockPos.relative(direction3)).isAir()) {
                    double d = 0.5;
                    vec3 = vec3.add(0.5, 0.5, 0.5).add(direction3.getStepX() * 0.5, direction3.getStepY() * 0.5, direction3.getStepZ() * 0.5);
                    level.addParticle(ParticleTypes.CLOUD, vec3.x, vec3.y, vec3.z, 0.0, 0.0, 0.0);
                }
            }
        }
    }
}