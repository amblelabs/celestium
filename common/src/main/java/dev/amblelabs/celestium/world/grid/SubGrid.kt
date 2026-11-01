package dev.amblelabs.celestium.world.grid

import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.CollisionGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biomes
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.border.WorldBorder
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.*

open class SubGrid(val level: Level, val carrier: GridCarrier) : BlockGetter, CollisionGetter {

    var blocks = SubGridBlocks(0, 0, 0)
        set(value) {
            field = value

            this.version++

            this.updatePosition(this.carrier.x, this.carrier.y, this.carrier.z)
            this.carrier.refreshDimensions()
        }

    var biome: Holder<Biome> = level.registryAccess()
        .lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)

        set(value) {
            field = value
            version++
        }

    var nextBoundingBox: AABB = AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        private set

    val knownBoundingBox: AABB
        get() {
            val lastMovement = this.lastMovement
            return this.nextBoundingBox.move(-lastMovement.x, -lastMovement.y, -lastMovement.z)
        }

    val lastMovement: Vec3
        get() = Vec3(
            this.carrier.x - this.carrier.xOld,
            this.carrier.y - this.carrier.yOld,
            this.carrier.z - this.carrier.zOld
        )

    var version: Int = 0
        private set

    @JvmField
    var renderCache: Any? = null

    init {
        this.updatePosition(carrier.x, carrier.y, carrier.z)
    }

    fun updatePosition(x: Double, y: Double, z: Double) {
        this.nextBoundingBox =
            AABB(x, y, z, x + this.blocks.sizeX + 1, y + this.blocks.sizeY + 1, z + this.blocks.sizeZ + 1)
    }

    fun id(): UUID {
        return this.carrier.getUUID()
    }

    override fun getBlockState(blockPos: BlockPos): BlockState {
        return this.blocks.getBlockState(blockPos)
    }

    override fun getFluidState(blockPos: BlockPos): FluidState {
        return this.getBlockState(blockPos).fluidState
    }

    override fun getBlockEntity(blockPos: BlockPos): BlockEntity? {
        return null
    }

    override fun getHeight(): Int {
        return this.blocks.sizeY
    }

    override fun getMinY(): Int {
        return 0
    }

    override fun getWorldBorder(): WorldBorder {
        return this.level.worldBorder
    }

    override fun getChunkForCollisions(i: Int, j: Int): BlockGetter? {
        return this
    }

    override fun getEntityCollisions(entity: Entity?, aABB: AABB): List<VoxelShape> {
        return emptyList()
    }
}