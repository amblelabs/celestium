package dev.amblelabs.celestium.client.grid;

import dev.amblelabs.celestium.api.GridLevelExtKt;
import dev.amblelabs.celestium.client.starlight.CameraStarlight;
import dev.amblelabs.celestium.client.starlight.StarShadows;
import dev.amblelabs.celestium.world.grid.SubGrid;
import dev.amblelabs.celestium.world.grid.SubGridBlocks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;

public final class GridMesh {
   private static final Direction[] DIRECTIONS = Direction.values();
   private static final int LIGHT_MARGIN = 15;

   private final SubGridBlocks blocks;
   private final int version;
   private final List<GridBlockRenderState> renderStates = new ArrayList<>();
   private final BlockPos.MutableBlockPos worldPos = new BlockPos.MutableBlockPos();

   private ClientLevel level;
   private BlockPos origin;
   private final Long2BooleanOpenHashMap selfShadowed = new Long2BooleanOpenHashMap();
   private Vector3fc shadowToward;
   private byte[] blockLight;
   private boolean blockLightBuilt;

   private GridMesh(SubGrid grid, ClientLevel level) {
      this.origin = BlockPos.ZERO;
      this.blocks = grid.getBlocks();
      this.version = grid.getVersion();
      this.level = level;
      BlockPos.MutableBlockPos neighbor = new BlockPos.MutableBlockPos();

      for (int x = 0; x < this.blocks.sizeX(); ++x) {
         for (int y = 0; y < this.blocks.sizeY(); ++y) {
            for (int z = 0; z < this.blocks.sizeZ(); ++z) {
               BlockState state = this.blocks.getBlockState(x, y, z);

               if (state.getRenderShape() == RenderShape.MODEL && !this.isHidden(x, y, z, neighbor)) {
                  GridBlockRenderState renderState = new GridBlockRenderState(this, new BlockPos(x, y, z), state);
                  renderState.biome = grid.getBiome();
                  renderState.cardinalLighting = level.cardinalLighting();
                  renderState.lightEngine = level.getLightEngine();
                  this.renderStates.add(renderState);
               }
            }
         }
      }
   }

   public static GridMesh of(SubGrid grid, ClientLevel level) {
      Object var3 = grid.getRenderCache();
      if (var3 instanceof GridMesh mesh) {
         if (mesh.version == grid.getVersion()) {
            mesh.level = level;
            return mesh;
         }
      }

      GridMesh mesh = new GridMesh(grid, level);
      grid.setRenderCache(mesh);
      return mesh;
   }

   private boolean isHidden(int x, int y, int z, BlockPos.MutableBlockPos neighbor) {
      for(Direction direction : DIRECTIONS) {
         neighbor.set(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
         if (!this.blocks.getBlockState(neighbor).isSolidRender()) {
            return false;
         }
      }

      return true;
   }

   public void moveTo(BlockPos origin) {
      this.origin = origin;
   }

   public SubGridBlocks blocks() {
      return this.blocks;
   }

   public List<GridBlockRenderState> renderStates() {
      return this.renderStates;
   }

   int worldBrightness(BlockGetter local, LightLayer layer, BlockPos localPos) {
      this.worldPos.setWithOffset(this.origin, localPos);
      int value = this.level.getBrightness(layer, this.worldPos);
      Vector3fc toward = CameraStarlight.shadowDirection();
      if (layer != LightLayer.SKY || toward == null) return value;

      if (toward != this.shadowToward) {
         this.shadowToward = toward;
         this.selfShadowed.clear();
      }
      long key = localPos.asLong();
      if (!this.selfShadowed.containsKey(key)) {
         this.selfShadowed.put(key, StarShadows.blocked(local, localPos, toward,
                 0, 0, 0, this.blocks.sizeX() - 1, this.blocks.sizeY() - 1, this.blocks.sizeZ() - 1));
      }
      return this.selfShadowed.get(key) ? StarShadows.shadowed(value) : value;
   }

   public static int withGridBlockLight(ClientLevel level, BlockPos pos, int value) {
      for (SubGrid grid : GridLevelExtKt.grids(level).values()) {
         if (value >= 15) break;
         if (grid.getRenderCache() instanceof GridMesh mesh && mesh.version == grid.getVersion()) {
            value = Math.max(value, mesh.blockLightAt(
                    pos.getX() - mesh.origin.getX(), pos.getY() - mesh.origin.getY(), pos.getZ() - mesh.origin.getZ()));
         }
      }
      return value;
   }

   private int blockLightAt(int x, int y, int z) {
      if (!this.blockLightBuilt) {
         this.blockLight = this.floodBlockLight();
         this.blockLightBuilt = true;
      }
      if (this.blockLight == null) return 0;
      int index = this.lightIndex(x + LIGHT_MARGIN, y + LIGHT_MARGIN, z + LIGHT_MARGIN);
      return index < 0 ? 0 : this.blockLight[index];
   }

   private byte[] floodBlockLight() {
      int sx = this.blocks.sizeX() + LIGHT_MARGIN * 2;
      int sy = this.blocks.sizeY() + LIGHT_MARGIN * 2;
      int sz = this.blocks.sizeZ() + LIGHT_MARGIN * 2;
      byte[] light = null;
      IntArrayFIFOQueue queue = new IntArrayFIFOQueue();

      for (int x = 0; x < this.blocks.sizeX(); ++x) {
         for (int y = 0; y < this.blocks.sizeY(); ++y) {
            for (int z = 0; z < this.blocks.sizeZ(); ++z) {
               int emission = this.blocks.getBlockState(x, y, z).getLightEmission();
               if (emission <= 0) continue;
               if (light == null) light = new byte[sx * sy * sz];
               int index = this.lightIndex(x + LIGHT_MARGIN, y + LIGHT_MARGIN, z + LIGHT_MARGIN);
               if (emission > light[index]) {
                  light[index] = (byte) emission;
                  queue.enqueue(index);
               }
            }
         }
      }
      if (light == null) return null;

      while (!queue.isEmpty()) {
         int index = queue.dequeueInt();
         int level = light[index];
         int x = index % sx, y = (index / sx) % sy, z = index / (sx * sy);
         for (Direction direction : DIRECTIONS) {
            int nx = x + direction.getStepX(), ny = y + direction.getStepY(), nz = z + direction.getStepZ();
            int neighbor = this.lightIndex(nx, ny, nz);
            if (neighbor < 0) continue;
            int next = level - Math.max(1, this.gridState(nx - LIGHT_MARGIN, ny - LIGHT_MARGIN, nz - LIGHT_MARGIN).getLightDampening());
            if (next > light[neighbor]) {
               light[neighbor] = (byte) next;
               queue.enqueue(neighbor);
            }
         }
      }
      return light;
   }

   private BlockState gridState(int x, int y, int z) {
      if (x < 0 || y < 0 || z < 0 || x >= this.blocks.sizeX() || y >= this.blocks.sizeY() || z >= this.blocks.sizeZ()) {
         return Blocks.AIR.defaultBlockState();
      }
      return this.blocks.getBlockState(x, y, z);
   }

   private int lightIndex(int x, int y, int z) {
      int sx = this.blocks.sizeX() + LIGHT_MARGIN * 2;
      int sy = this.blocks.sizeY() + LIGHT_MARGIN * 2;
      int sz = this.blocks.sizeZ() + LIGHT_MARGIN * 2;
      if (x < 0 || y < 0 || z < 0 || x >= sx || y >= sy || z >= sz) return -1;
      return (z * sy + y) * sx + x;
   }
}
