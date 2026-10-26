package dev.amblelabs.celestium.client.grid;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.Iterator;

import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public class SubGridMeshBuilder {
    private static final int INITIAL_BUFFER_SIZE = 4096;
    private final BlockRenderDispatcher blockRenderer;
    private final SubGridMeshBuilder.BlockView blockView;

    public SubGridMeshBuilder(BlockRenderDispatcher blockRenderDispatcher, SubGridMeshBuilder.BlockView blockView) {
        this.blockRenderer = blockRenderDispatcher;
        this.blockView = blockView;
    }

    public SubGridMeshBuilder.Results build() {
        Reference2ObjectMap<RenderType, BufferBuilder> reference2ObjectMap = new Reference2ObjectArrayMap();
        PoseStack poseStack = new PoseStack();
        RandomSource randomSource = RandomSource.create();

        for (BlockPos blockPos : this.blockView) {
            BlockState blockState = this.blockView.getBlockState(blockPos);
            FluidState fluidState = blockState.getFluidState();
            if (!fluidState.isEmpty()) {
                BufferBuilder bufferBuilder = startBuilding(reference2ObjectMap, ItemBlockRenderTypes.getRenderLayer(fluidState));
                this.blockRenderer.renderLiquid(blockPos, this.blockView, bufferBuilder, blockState, fluidState, blockPos.getX(), blockPos.getY(), blockPos.getZ());
            }

            if (blockState.getRenderShape() != RenderShape.INVISIBLE) {
                BufferBuilder bufferBuilder = startBuilding(reference2ObjectMap, ItemBlockRenderTypes.getChunkRenderType(blockState));
                poseStack.pushPose();
                poseStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
                this.blockRenderer.renderBatched(blockState, blockPos, this.blockView, poseStack, bufferBuilder, true, randomSource);
                poseStack.popPose();
            }
        }

        return new SubGridMeshBuilder.Results(reference2ObjectMap);
    }

    private static BufferBuilder startBuilding(Reference2ObjectMap<RenderType, BufferBuilder> reference2ObjectMap, RenderType renderType) {
        return (BufferBuilder)reference2ObjectMap.computeIfAbsent(renderType, renderTypex -> {
            BufferBuilder bufferBuilder = new BufferBuilder(4096);
            bufferBuilder.begin(Mode.QUADS, DefaultVertexFormat.BLOCK);
            return bufferBuilder;
        });
    }

    public record BlockView(Level level, SubGridBlocks blocks, Holder<Biome> biome) implements BlockAndTintGetter, Iterable<BlockPos> {
        public static SubGridMeshBuilder.BlockView copyOf(ClientSubGrid clientSubGrid) {
            return new SubGridMeshBuilder.BlockView(clientSubGrid.level(), clientSubGrid.getBlocks().copy(), clientSubGrid.getBiome());
        }

        @Override
        public float getShade(Direction direction, boolean bl) {
            return this.level.getShade(direction, bl);
        }

        @Override
        public LevelLightEngine getLightEngine() {
            return SubGridLightEngine.INSTANCE;
        }

        @Override
        public int getBlockTint(BlockPos blockPos, ColorResolver colorResolver) {
            return colorResolver.getColor(this.biome.value(), blockPos.getX(), blockPos.getZ());
        }

        @Nullable
        public BlockEntity getBlockEntity(BlockPos blockPos) {
            return null;
        }

        public BlockState getBlockState(BlockPos blockPos) {
            return this.blocks.getBlockState(blockPos);
        }

        public FluidState getFluidState(BlockPos blockPos) {
            return this.getBlockState(blockPos).getFluidState();
        }

        public boolean isPotato() {
            return false;
        }

        public int getHeight() {
            return this.blocks.sizeY();
        }

        public int getMinBuildHeight() {
            return 0;
        }

        public Iterator<BlockPos> iterator() {
            return BlockPos.betweenClosed(0, 0, 0, this.blocks.sizeX() - 1, this.blocks.sizeY() - 1, this.blocks.sizeZ() - 1).iterator();
        }
    }

    public static class Results implements AutoCloseable {
        private final Reference2ObjectMap<RenderType, BufferBuilder> builders;

        public Results(Reference2ObjectMap<RenderType, BufferBuilder> reference2ObjectMap) {
            this.builders = reference2ObjectMap;
        }

        public void uploadTo(Reference2ObjectMap<RenderType, VertexBuffer> reference2ObjectMap) {
            for (RenderType renderType : RenderType.chunkBufferLayers()) {
                RenderedBuffer renderedBuffer = this.takeLayer(renderType);
                if (renderedBuffer == null) {
                    VertexBuffer vertexBuffer = (VertexBuffer)reference2ObjectMap.remove(renderType);
                    if (vertexBuffer != null) {
                        vertexBuffer.close();
                    }
                } else {
                    VertexBuffer vertexBuffer = (VertexBuffer)reference2ObjectMap.get(renderType);
                    if (vertexBuffer == null) {
                        vertexBuffer = new VertexBuffer(Usage.STATIC);
                        reference2ObjectMap.put(renderType, vertexBuffer);
                    }

                    vertexBuffer.bind();
                    vertexBuffer.upload(renderedBuffer);
                }
            }
        }

        @Nullable
        public RenderedBuffer takeLayer(RenderType renderType) {
            BufferBuilder bufferBuilder = (BufferBuilder)this.builders.get(renderType);
            return bufferBuilder != null ? bufferBuilder.endOrDiscardIfEmpty() : null;
        }

        public void close() {
            this.builders.values().forEach(BufferBuilder::release);
        }
    }
}
