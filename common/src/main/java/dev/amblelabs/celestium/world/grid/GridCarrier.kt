package dev.amblelabs.celestium.world.grid

import dev.amblelabs.celestium.api.createSubGrid
import dev.amblelabs.celestium.network.protocol.game.SubGridPayload
import dev.amblelabs.celestium.platform.Services
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.function.Consumer
import java.util.function.Function
import kotlin.math.max

class GridCarrier(type: EntityType<*>, level: Level) : Entity(type, level) {
    private val grid: SubGrid = level.createSubGrid(this)

    private var movementCollider: SubGridMovementCollider? = null
    private var placeInTicks = 0

    fun setMovement(direction: Direction, f: Float) {
        this.getEntityData().set(MOVEMENT_DIRECTION, direction)
        this.getEntityData().set(MOVEMENT_SPEED, f)
        this.movementCollider = SubGridMovementCollider.generate(this.grid.blocks, direction)
    }

    fun clearMovement() {
        this.getEntityData().set(MOVEMENT_SPEED, 0.0f)
        this.movementCollider = null
    }

    fun grid(): SubGrid {
        return this.grid
    }

    override fun setPos(d: Double, e: Double, f: Double) {
        super.setPos(d, e, f)
        this.grid.updatePosition(d, e, f)
    }

    override fun tick() {
        super.tick()
        this.grid.blocks.tick(this.level(), this.position(), this.movementDirection)

        if (!this.level().isClientSide) {
            this.tickServer()
        }
    }

    private fun tickServer() {
        val speed = this.movementSpeed
        val weight = this.grid.blocks.countSolidBlocks().toFloat()
        val moveSpeed = speed / (1f + weight * 0.001f)

        if (this.exceedsBuildHeight()) {
            this.placeInTicks = 0
            if (this.movementCollider != null) {
                this.tickMovement(this.movementCollider!!, this.movementDirection, moveSpeed)
            }
            return
        }

        if (this.placeInTicks == 0 && speed == 0f) this.placeInTicks = 2

        if (this.placeInTicks > 0) {
            this.placeInTicks--
            if (this.placeInTicks == 1) {
                this.grid.blocks.place(this.blockPosition(), this.level())
            } else if (this.placeInTicks == 0) {
                this.discard()
            }
        } else if (this.movementCollider != null) {
            this.tickMovement(this.movementCollider!!, this.movementDirection, moveSpeed)
        }
    }

    override fun interact(player: Player, hand: InteractionHand, location: Vec3): InteractionResult {
        if (hand == InteractionHand.MAIN_HAND && player.mainHandItem.isEmpty) {
            if (this.exceedsBuildHeight()) return InteractionResult.PASS
            this.clearMovement()
            this.placeInTicks = 5
            return InteractionResult.SUCCESS
        }
        return super.interact(player, hand, location)
    }

    override fun isPickable(): Boolean {
        super.isPickable
        return !this.isRemoved
    }

    private fun tickMovement(collider: SubGridMovementCollider, direction: Direction, speed: Float) {
        val from = this.position()
        val to = from.add(
            (direction.stepX * speed).toDouble(),
            (direction.stepY * speed).toDouble(),
            (direction.stepZ * speed).toDouble()
        )

        val start = this.getCollidingPos(from, direction)
        val end = this.getCollidingPos(to, direction)
        val pos = start.mutable()

        while (pos != end) {
            pos.move(direction)
            if (collider.checkCollision(this.level(), pos)) {
                this.setPos(Vec3.atLowerCornerOf(pos.relative(direction, -1)))
                this.clearMovement()
                this.placeInTicks = 5 // FIXME: magic
                return
            }
        }

        this.setPos(to)
    }

    private fun exceedsBuildHeight(): Boolean {
        val top = this.blockPosition().y + this.grid.blocks.sizeY - 1
        return top > this.level().maxY
    }

    private fun getCollidingPos(position: Vec3, direction: Direction): BlockPos {
        val pos = BlockPos.containing(position)
        return if (direction.axisDirection == Direction.AxisDirection.POSITIVE) pos.relative(direction) else pos
    }

    override fun defineSynchedData(builder: SynchedEntityData.Builder) {
        builder.define(MOVEMENT_DIRECTION, Direction.NORTH)
        builder.define(MOVEMENT_SPEED, 0f)
    }

    override fun makeBoundingBox(pos: Vec3): AABB {
        val blocks = this.grid.blocks
        return AABB(pos.x, pos.y, pos.z, pos.x + blocks.sizeX, pos.y + blocks.sizeY, pos.z + blocks.sizeZ)
    }

    override fun getDimensions(pose: Pose): EntityDimensions {
        val blocks = this.grid.blocks
        return EntityDimensions.scalable(max(blocks.sizeX, blocks.sizeZ).toFloat(), blocks.sizeY.toFloat())
    }

    override fun readAdditionalSaveData(input: ValueInput) {
        input.read("blocks", SubGridBlocks.CODEC).ifPresent { this.grid.blocks = it }

        input.read("biome", ResourceKey.codec(Registries.BIOME))
            .flatMap(
                Function { key: ResourceKey<Biome> ->
                    this.registryAccess()
                        .lookupOrThrow(Registries.BIOME).get(key)
                }).ifPresent { this.grid.biome = it }

        input.read("movement_direction", Direction.CODEC).ifPresentOrElse(Consumer { dir: Direction ->
            this.setMovement(
                dir,
                input.getFloatOr("movement_speed", 0f)
            )
        }, this::clearMovement)
    }

    override fun addAdditionalSaveData(output: ValueOutput) {
        output.store("blocks", SubGridBlocks.CODEC, this.grid.blocks)

        this.grid.biome.unwrapKey().ifPresent(Consumer { key: ResourceKey<Biome> ->
            output.store(
                "biome", ResourceKey.codec(
                    Registries.BIOME
                ), key
            )
        })

        output.store(
            "movement_direction", Direction.CODEC,
            this.movementDirection
        )

        output.putFloat("movement_speed", this.movementSpeed)
    }

    private val movementSpeed: Float
        get() = this.getEntityData()
            .get(MOVEMENT_SPEED)

    private val movementDirection: Direction
        get() = this.getEntityData()
            .get<Direction>(MOVEMENT_DIRECTION)

    override fun startSeenByPlayer(player: ServerPlayer) {
        super.startSeenByPlayer(player)

        Services.PLATFORM.sendToPlayer(
            player,
            SubGridPayload(this.id, this.grid.blocks.copy(), this.grid.biome)
        )
    }

    override fun createInterpolationHandler(): InterpolationHandler {
        return LinearInterpolationHandler.create(this)
    }

    override fun shouldRenderAtSqrDistance(distance: Double): Boolean {
        return true
    }

    override fun hurtServer(level: ServerLevel, source: DamageSource, damage: Float): Boolean {
        return false
    }

    override fun getPistonPushReaction(): PushReaction {
        return PushReaction.IGNORE_ENTITY
    }

    override fun isIgnoringBlockTriggers(): Boolean {
        return true
    }

    companion object {
        private val MOVEMENT_DIRECTION =
            SynchedEntityData.defineId(GridCarrier::class.java, EntityDataSerializers.DIRECTION)

        private val MOVEMENT_SPEED =
            SynchedEntityData.defineId(GridCarrier::class.java, EntityDataSerializers.FLOAT)
    }
}
