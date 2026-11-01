package dev.amblelabs.celestium.world.grid

import dev.amblelabs.celestium.api.grids
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.util.*
import java.util.function.Consumer

object SubGridCollisions {

    @JvmStatic
    fun movesWithGrid(entity: Entity): Boolean {
        return entity !is Player || !entity.abilities.flying
    }

    @JvmStatic
    fun noGridCollision(entity: Entity?, box: AABB, level: Level): Boolean {
        return entity is GridCarrier || findGridIn(entity, box, level) == null
    }

    @JvmStatic
    fun findGridBelow(entity: Entity): SubGrid? {
        val below = entity.boundingBox.inflate(0.0625).expandTowards(0.0, -0.55, 0.0)
        return findGridIn(entity, below, entity.level())
    }

    private fun findGridIn(entity: Entity?, box: AABB, level: Level): SubGrid? {
        val grids: MutableMap<UUID, SubGrid> = level.grids()

        if (grids.isNotEmpty()) {
            for (grid in grids.values) {
                val bounds = grid.nextBoundingBox
                if (bounds.intersects(box) && !grid.noBlockCollision(
                        entity,
                        box.move(-bounds.minX, -bounds.minY, -bounds.minZ)
                    )
                ) {
                    return grid
                }
            }
        }

        return null
    }

    @JvmStatic
    fun stateUnderFeet(entity: Entity, grid: SubGrid): BlockState {
        val local = entity.position().subtract(grid.carrier.position()).add(0.0, -0.2, 0.0)
        return grid.getBlockState(BlockPos.containing(local))
    }

    @JvmStatic
    fun collide(entity: Entity, movement: Vec3, box: AABB, level: Level): Result {
        var movement = movement
        val grids: MutableMap<UUID, SubGrid> = level.grids()
        var touched: SubGrid? = null

        if (grids.isNotEmpty() && entity !is GridCarrier) {
            val shapes: MutableList<VoxelShape> = ArrayList()

            for (grid in grids.values) {
                val gridMovement = grid.lastMovement
                val known = grid.knownBoundingBox
                val relative = movement.subtract(gridMovement)
                val swept = box.expandTowards(relative)
                if (known.intersects(swept)) {
                    val localBox = box.move(-known.minX, -known.minY, -known.minZ)
                    val localSwept = swept.move(-known.minX, -known.minY, -known.minZ)
                    shapes.clear()

                    grid.getBlockCollisions(entity, localSwept).forEach(Consumer { e: VoxelShape -> shapes.add(e) })
                    val collided = collideWithShapes(relative, localBox, shapes)

                    if (collided != relative) {
                        movement = collided.add(gridMovement)
                        touched = grid
                    }
                }
            }
        }

        return Result(movement, touched)
    }

    private fun collideWithShapes(movement: Vec3, box: AABB, shapes: MutableList<VoxelShape>): Vec3 {
        if (shapes.isEmpty()) {
            return movement
        } else {
            var resolved = Vec3.ZERO

            for (axis in Direction.axisStepOrder(movement)) {
                val axisMovement = movement.get(axis)
                if (axisMovement != 0.0) {
                    val collision = Shapes.collide(axis, box.move(resolved), shapes, axisMovement)
                    resolved = resolved.with(axis, collision)
                }
            }

            return resolved
        }
    }

    @JvmRecord
    data class Result(@JvmField val movement: Vec3?, @JvmField val grid: SubGrid?)
}
