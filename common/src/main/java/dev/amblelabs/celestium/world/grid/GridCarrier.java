package dev.amblelabs.celestium.world.grid;

import dev.amblelabs.celestium.api.GridLevelExtKt;
import dev.amblelabs.celestium.network.protocol.game.SubGridPayload;
import dev.amblelabs.celestium.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class GridCarrier extends Entity {

    private static final EntityDataAccessor<Direction> MOVEMENT_DIRECTION = SynchedEntityData.defineId(GridCarrier.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<Float> MOVEMENT_SPEED = SynchedEntityData.defineId(GridCarrier.class, EntityDataSerializers.FLOAT);

    private final SubGrid grid;

    private @Nullable SubGridMovementCollider movementCollider;
    private int placeInTicks;

    public GridCarrier(EntityType<?> type, Level level) {
        super(type, level);
        this.grid = GridLevelExtKt.createSubGrid(level, this);
    }

    public void setMovement(Direction direction, float f) {
        this.getEntityData().set(MOVEMENT_DIRECTION, direction);
        this.getEntityData().set(MOVEMENT_SPEED, f);
        this.movementCollider = SubGridMovementCollider.generate(this.grid.getBlocks(), direction);
    }

    public void clearMovement() {
        this.getEntityData().set(MOVEMENT_SPEED, 0.0F);
        this.movementCollider = null;
    }

    public SubGrid grid() {
        return this.grid;
    }

    @Override
    public void setPos(double d, double e, double f) {
        super.setPos(d, e, f);
        if (this.grid != null) {
            this.grid.updatePosition(d, e, f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.grid.getBlocks().tick(this.level(), this.position(), this.getMovementDirection());

        if (!this.level().isClientSide()) {
            this.tickServer();
        }
    }

    private void tickServer() {
        float speed = this.getMovementSpeed();

        if (this.exceedsBuildHeight()) {
            this.placeInTicks = 0;
            if (this.movementCollider != null) {
                this.tickMovement(this.movementCollider, this.getMovementDirection(), speed);
            }
            return;
        }

        if (this.placeInTicks == 0 && speed == 0)
            this.placeInTicks = 2;

        if (this.placeInTicks > 0) {
            this.placeInTicks--;
            if (this.placeInTicks == 1) {
                this.grid.getBlocks().place(this.blockPosition(), this.level());
            } else if (this.placeInTicks == 0) {
                this.discard();
            }
        } else if (this.movementCollider != null) {
            this.tickMovement(this.movementCollider, this.getMovementDirection(), speed);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty()) {
            if (this.exceedsBuildHeight()) return InteractionResult.PASS;
            this.clearMovement();
            this.placeInTicks = 5;
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand, location);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    private void tickMovement(SubGridMovementCollider collider, Direction direction, float speed) {
        Vec3 from = this.position();
        Vec3 to = from.add(direction.getStepX() * speed, direction.getStepY() * speed, direction.getStepZ() * speed);

        BlockPos start = this.getCollidingPos(from, direction);
        BlockPos end = this.getCollidingPos(to, direction);
        MutableBlockPos pos = start.mutable();

        while (!pos.equals(end)) {
            pos.move(direction);
            if (collider.checkCollision(this.level(), pos)) {
                this.setPos(Vec3.atLowerCornerOf(pos.relative(direction, -1)));
                this.clearMovement();
                this.placeInTicks = 5; // FIXME: magic
                return;
            }
        }

        this.setPos(to);
    }

    private boolean exceedsBuildHeight() {
        int top = this.blockPosition().getY() + this.grid.getBlocks().sizeY() - 1;
        return top > this.level().getMaxY();
    }

    private BlockPos getCollidingPos(Vec3 position, Direction direction) {
        BlockPos pos = BlockPos.containing(position);
        return direction.getAxisDirection() == AxisDirection.POSITIVE ? pos.relative(direction) : pos;
    }

    @Override
    protected void defineSynchedData(Builder builder) {
        builder.define(MOVEMENT_DIRECTION, Direction.NORTH);
        builder.define(MOVEMENT_SPEED, 0f);
    }

    @Override
    protected AABB makeBoundingBox(Vec3 pos) {
        if (this.grid == null)
            return super.makeBoundingBox(pos);

        SubGridBlocks blocks = this.grid.getBlocks();
        return new AABB(pos.x, pos.y, pos.z, pos.x + blocks.sizeX(), pos.y + blocks.sizeY(), pos.z + blocks.sizeZ());
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        if (this.grid == null)
            return super.getDimensions(pose);

        SubGridBlocks blocks = this.grid.getBlocks();
        return EntityDimensions.scalable(Math.max(blocks.sizeX(), blocks.sizeZ()), blocks.sizeY());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.read("blocks", SubGridBlocks.CODEC).ifPresent(this.grid::setBlocks);

        input.read("biome", ResourceKey.codec(Registries.BIOME)).flatMap(key -> this.registryAccess()
                .lookupOrThrow(Registries.BIOME).get(key)).ifPresent(this.grid::setBiome);

        input.read("movement_direction", Direction.CODEC).ifPresentOrElse(dir -> {
            this.setMovement(dir, input.getFloatOr("movement_speed", 0f));
        }, this::clearMovement);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("blocks", SubGridBlocks.CODEC, this.grid.getBlocks());
        this.grid.getBiome().unwrapKey().ifPresent(key -> output.store("biome", ResourceKey.codec(Registries.BIOME), key));
        output.store("movement_direction", Direction.CODEC, this.getMovementDirection());
        output.putFloat("movement_speed", this.getMovementSpeed());
    }

    private float getMovementSpeed() {
        return this.getEntityData().get(MOVEMENT_SPEED);
    }

    private Direction getMovementDirection() {
        return this.getEntityData().get(MOVEMENT_DIRECTION);
    }

    @Override
    public void startSeenByPlayer(@NonNull ServerPlayer player) {
        super.startSeenByPlayer(player);

        Services.PLATFORM.sendToPlayer(player, new SubGridPayload(this.getId(), this.grid.getBlocks().copy(), this.grid.getBiome()));
    }

    @Override
    protected @NonNull InterpolationHandler createInterpolationHandler() {
        return LinearInterpolationHandler.create(this);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public boolean hurtServer(@NonNull ServerLevel level, @NonNull DamageSource source, float damage) {
        return false;
    }

    @Override
    public @NonNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }
}
