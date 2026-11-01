package dev.amblelabs.celestium.client.grid;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.amblelabs.celestium.world.grid.GridCarrier;
import dev.amblelabs.celestium.world.grid.SubGridBlocks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.NonNull;

public class GridCarrierRenderer extends EntityRenderer<GridCarrier, GridCarrierRenderState> {
   private static final double CULL_BUFFER_SIZE = 3F;

   public GridCarrierRenderer(EntityRendererProvider.Context context) {
      super(context);
   }

   @Override
   public @NonNull GridCarrierRenderState createRenderState() {
      return new GridCarrierRenderState();
   }

   @Override
   protected @NonNull AABB getBoundingBoxForCulling(GridCarrier carrier, float partialTicks) {
      double x = Mth.lerp(partialTicks, carrier.xOld, carrier.getX());
      double y = Mth.lerp(partialTicks, carrier.yOld, carrier.getY());
      double z = Mth.lerp(partialTicks, carrier.zOld, carrier.getZ());

      SubGridBlocks blocks = carrier.grid().getBlocks();
      return new AABB(x, y, z, x + blocks.sizeX() + 1, y + blocks.sizeY() + 1, z + blocks.sizeZ() + 1)
              .inflate(CULL_BUFFER_SIZE);
   }

   @Override
   public void extractRenderState(@NonNull GridCarrier carrier, @NonNull GridCarrierRenderState state, float partialTicks) {
      super.extractRenderState(carrier, state, partialTicks);

      if (carrier.level() instanceof ClientLevel level) {
         GridMesh mesh = GridMesh.of(carrier.grid(), level);
         mesh.moveTo(BlockPos.containing(state.x + 0.5, state.y + 0.5, state.z + 0.5));
         state.blocks = mesh.renderStates();
      }
   }

   @Override
   public void submit(GridCarrierRenderState state, @NonNull PoseStack poseStack, @NonNull SubmitNodeCollector submitNodeCollector, @NonNull CameraRenderState camera) {
      for (GridBlockRenderState block : state.blocks) {
         poseStack.pushPose();
         poseStack.translate(block.blockPos.getX(), block.blockPos.getY(), block.blockPos.getZ());
         submitNodeCollector.submitMovingBlock(poseStack, block, 0);
         poseStack.popPose();
      }

      super.submit(state, poseStack, submitNodeCollector, camera);
   }
}
