package dev.amblelabs.celestium.client.grid;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.NonNull;

public class GridBlockRenderState extends MovingBlockRenderState {
   private final GridMesh mesh;

   public GridBlockRenderState(GridMesh mesh, BlockPos localPos, BlockState state) {
      this.mesh = mesh;
      this.blockPos = localPos;
      this.randomSeedPos = localPos;
      this.blockState = state;
   }

   @Override
   public @NonNull BlockState getBlockState(@NonNull BlockPos pos) {
      return this.mesh.blocks().getBlockState(pos);
   }

   @Override
   public @NonNull FluidState getFluidState(@NonNull BlockPos pos) {
      return this.getBlockState(pos).getFluidState();
   }

   @Override
   public int getBrightness(@NonNull LightLayer layer, @NonNull BlockPos pos) {
      return this.mesh.worldBrightness(this, layer, pos);
   }

   @Override
   public int getHeight() {
      return this.mesh.blocks().sizeY();
   }

   @Override
   public int getMinY() {
      return 0;
   }
}
