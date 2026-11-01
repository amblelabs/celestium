package dev.amblelabs.celestium.world.grid;

import dev.amblelabs.celestium.api.GridLevelExtKt;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class SubGridCollisions {
   public static final int ATTACHED_GRID_TIMEOUT = 30;

   private SubGridCollisions() {
   }

   public static boolean movesWithGrid(Entity entity) {
       return !(entity instanceof Player player) || !player.getAbilities().flying;
   }

   public static boolean noGridCollision(@Nullable Entity entity, AABB box, Level level) {
      return entity instanceof GridCarrier || findGridIn(entity, box, level) == null;
   }

   public static @Nullable SubGrid findGridBelow(Entity entity) {
      AABB below = entity.getBoundingBox().inflate(0.0625).expandTowards(0, -0.55, 0);
      return findGridIn(entity, below, entity.level());
   }

   private static @Nullable SubGrid findGridIn(@Nullable Entity entity, AABB box, Level level) {
      Map<UUID, SubGrid> grids = GridLevelExtKt.grids(level);

       if (!grids.isEmpty()) {
           for (SubGrid grid : grids.values()) {
               AABB bounds = grid.getNextBoundingBox();
               if (bounds.intersects(box) && !grid.noBlockCollision(entity, box.move(-bounds.minX, -bounds.minY, -bounds.minZ))) {
                   return grid;
               }
           }
       }

       return null;
   }

   public static BlockState stateUnderFeet(Entity entity, SubGrid grid) {
      Vec3 local = entity.position().subtract(grid.carrier().position()).add(0, -0.2, 0);
      return grid.getBlockState(BlockPos.containing(local));
   }

   public static Result collide(Entity entity, Vec3 movement, AABB box, Level level) {
      Map<UUID, SubGrid> grids = GridLevelExtKt.grids(level);
      SubGrid touched = null;

      if (!grids.isEmpty() && !(entity instanceof GridCarrier)) {
         List<VoxelShape> shapes = new ArrayList<>();

         for(SubGrid grid : grids.values()) {
            Vec3 gridMovement = grid.getLastMovement();
            AABB known = grid.getKnownBoundingBox();
            Vec3 relative = movement.subtract(gridMovement);
            AABB swept = box.expandTowards(relative);
            if (known.intersects(swept)) {
               AABB localBox = box.move(-known.minX, -known.minY, -known.minZ);
               AABB localSwept = swept.move(-known.minX, -known.minY, -known.minZ);
               shapes.clear();

               grid.getBlockCollisions(entity, localSwept).forEach(shapes::add);
               Vec3 collided = collideWithShapes(relative, localBox, shapes);

               if (!collided.equals(relative)) {
                  movement = collided.add(gridMovement);
                  touched = grid;
               }
            }
         }
      }

      return new Result(movement, touched);
   }

   private static Vec3 collideWithShapes(Vec3 movement, AABB box, List<VoxelShape> shapes) {
      if (shapes.isEmpty()) {
         return movement;
      } else {
         Vec3 resolved = Vec3.ZERO;

         for (Direction.Axis axis : Direction.axisStepOrder(movement)) {
            double axisMovement = movement.get(axis);
            if (axisMovement != (double) 0.0F) {
               double collision = Shapes.collide(axis, box.move(resolved), shapes, axisMovement);
               resolved = resolved.with(axis, collision);
            }
         }

         return resolved;
      }
   }

   public record Result(Vec3 movement, @Nullable SubGrid grid) { }
}
