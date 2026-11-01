package dev.amblelabs.celestium.client.grid

import dev.amblelabs.celestium.api.grids
import dev.amblelabs.celestium.client.starlight.CameraStarlight
import dev.amblelabs.celestium.client.starlight.StarShadows
import dev.amblelabs.celestium.world.grid.SubGrid
import dev.amblelabs.celestium.world.grid.SubGridBlocks
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.profiling.Profiler
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3fc
import kotlin.math.max

class GridMesh private constructor(grid: SubGrid, private var level: ClientLevel) {
    private val version: Int = grid.version
    private val worldPos = BlockPos.MutableBlockPos()

    private val selfShadowed = Long2BooleanOpenHashMap()
    private var shadowToward: Vector3fc? = null
    private var blockLight: ByteArray? = null
    private var blockLightBuilt = false

    val renderStates: MutableList<GridBlockRenderState> = ArrayList()
    var origin: BlockPos = BlockPos.ZERO
    val blocks: SubGridBlocks = grid.blocks

    init {
        val neighbor = BlockPos.MutableBlockPos()

        for (x in 0..<this.blocks.sizeX) {
            for (y in 0..<this.blocks.sizeY) {
                for (z in 0..<this.blocks.sizeZ) {
                    val state = this.blocks.getBlockState(x, y, z)

                    if (state.renderShape == RenderShape.MODEL && !this.isHidden(x, y, z, neighbor)) {
                        val renderState = GridBlockRenderState(this, BlockPos(x, y, z), state)
                        renderState.biome = grid.biome
                        renderState.cardinalLighting = level.cardinalLighting()
                        renderState.lightEngine = level.lightEngine
                        this.renderStates.add(renderState)
                    }
                }
            }
        }
    }

    private fun isHidden(x: Int, y: Int, z: Int, neighbor: BlockPos.MutableBlockPos): Boolean {
        for (direction in DIRECTIONS) {
            neighbor.set(x + direction.stepX, y + direction.stepY, z + direction.stepZ)
            if (!this.blocks.getBlockState(neighbor).isSolidRender) {
                return false
            }
        }

        return true
    }

    fun worldBrightness(local: BlockGetter?, layer: LightLayer, localPos: BlockPos): Int {
        this.worldPos.setWithOffset(this.origin, localPos)
        val value = this.level.getBrightness(layer, this.worldPos)
        val toward = CameraStarlight.shadowDirection()
        if (layer != LightLayer.SKY || toward == null) return value

        if (toward !== this.shadowToward) {
            this.shadowToward = toward
            this.selfShadowed.clear()
        }
        val key = localPos.asLong()
        if (!this.selfShadowed.containsKey(key)) {
            this.selfShadowed.put(
                key, StarShadows.blocked(
                    local, localPos, toward,
                    0, 0, 0, this.blocks.sizeX - 1, this.blocks.sizeY - 1, this.blocks.sizeZ - 1
                )
            )
        }
        return if (this.selfShadowed.get(key)) StarShadows.shadowed(value) else value
    }

    private fun blockLightAt(x: Int, y: Int, z: Int): Int {
        if (!this.blockLightBuilt) {
            Profiler.get().push("celestium_grid_light")
            this.blockLight = this.floodBlockLight()
            Profiler.get().pop()
            this.blockLightBuilt = true
        }
        if (this.blockLight == null) return 0
        val index = this.lightIndex(x + LIGHT_MARGIN, y + LIGHT_MARGIN, z + LIGHT_MARGIN)
        return (if (index < 0) 0 else this.blockLight!![index]).toInt()
    }

    private fun floodBlockLight(): ByteArray? {
        val sx: Int = this.blocks.sizeX + LIGHT_MARGIN * 2
        val sy: Int = this.blocks.sizeY + LIGHT_MARGIN * 2
        val sz: Int = this.blocks.sizeZ + LIGHT_MARGIN * 2
        var light: ByteArray? = null
        val queue = IntArrayFIFOQueue()

        for (x in 0..<this.blocks.sizeX) {
            for (y in 0..<this.blocks.sizeY) {
                for (z in 0..<this.blocks.sizeZ) {
                    val emission = this.blocks.getBlockState(x, y, z).lightEmission
                    if (emission <= 0) continue
                    if (light == null) light = ByteArray(sx * sy * sz)
                    val index = this.lightIndex(x + LIGHT_MARGIN, y + LIGHT_MARGIN, z + LIGHT_MARGIN)
                    if (emission > light[index]) {
                        light[index] = emission.toByte()
                        queue.enqueue(index)
                    }
                }
            }
        }
        if (light == null) return null

        while (!queue.isEmpty) {
            val index = queue.dequeueInt()
            val level = light[index].toInt()
            val x = index % sx
            val y = (index / sx) % sy
            val z = index / (sx * sy)
            for (direction in DIRECTIONS) {
                val nx = x + direction.stepX
                val ny = y + direction.stepY
                val nz = z + direction.stepZ
                val neighbor = this.lightIndex(nx, ny, nz)
                if (neighbor < 0) continue
                val next = level - max(
                    1, this.gridState(
                        nx - LIGHT_MARGIN,
                        ny - LIGHT_MARGIN,
                        nz - LIGHT_MARGIN
                    ).lightDampening
                )
                if (next > light[neighbor]) {
                    light[neighbor] = next.toByte()
                    queue.enqueue(neighbor)
                }
            }
        }
        return light
    }

    private fun gridState(x: Int, y: Int, z: Int): BlockState {
        if (x < 0 || y < 0 || z < 0 || x >= this.blocks.sizeX || y >= this.blocks.sizeY || z >= this.blocks.sizeZ) {
            return Blocks.AIR.defaultBlockState()
        }
        return this.blocks.getBlockState(x, y, z)
    }

    private fun lightIndex(x: Int, y: Int, z: Int): Int {
        val sx: Int = this.blocks.sizeX + LIGHT_MARGIN * 2
        val sy: Int = this.blocks.sizeY + LIGHT_MARGIN * 2
        val sz: Int = this.blocks.sizeZ + LIGHT_MARGIN * 2
        if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) return -1
        return (z * sy + y) * sx + x
    }

    companion object {
        private val DIRECTIONS = Direction.entries.toTypedArray()
        private const val LIGHT_MARGIN = 15

        fun of(grid: SubGrid, level: ClientLevel): GridMesh {
            val var3 = grid.renderCache
            if (var3 is GridMesh) {
                if (var3.version == grid.version) {
                    var3.level = level
                    return var3
                }
            }

            Profiler.get().push("celestium_grid_mesh")
            val mesh = GridMesh(grid, level)
            grid.renderCache = mesh
            Profiler.get().pop()
            return mesh
        }

        @JvmStatic
        fun withGridBlockLight(level: ClientLevel, pos: BlockPos, value: Int): Int {
            var value = value
            for (grid in level.grids().values) {
                if (value >= 15) break

                val renderCache = grid.renderCache
                if (renderCache is GridMesh && renderCache.version == grid.version) {
                    value = max(
                        value, renderCache.blockLightAt(
                            pos.x - renderCache.origin.x,
                            pos.y - renderCache.origin.y,
                            pos.z - renderCache.origin.z
                        )
                    )
                }
            }
            return value
        }
    }
}
