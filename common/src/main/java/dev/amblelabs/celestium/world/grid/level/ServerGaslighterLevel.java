package dev.amblelabs.celestium.world.grid.level;

import dev.amblelabs.celestium.world.grid.SubGrid;
import dev.amblelabs.celestium.world.grid.SubGridBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ServerGaslighterLevel extends ServerLevel {

    private final Holder<Biome> biomeHolder;
    private final SubGrid subGrid;
    private final Level originalLevel;
    private final SubGridBlocks subGridBlocks;

    public ServerGaslighterLevel(SubGrid subGrid) {
        super(Objects.requireNonNull(subGrid.level().getServer()), Util.backgroundExecutor(), null, (ServerLevelData) subGrid.level().getLevelData(),
                subGrid.level().dimension(), null, false, 0L, List.of(), false);
        this.subGrid = subGrid;
        this.originalLevel = subGrid.level();
        this.biomeHolder = subGrid.getBiome();
        this.subGridBlocks = subGrid.getBlocks();
    }

    @Override
    public @NonNull BlockState getBlockState(@NonNull BlockPos blockPos) {
        return this.subGridBlocks.getBlockState(blockPos);
    }

    @Override
    public @NonNull FluidState getFluidState(@NonNull BlockPos blockPos) {
        return this.getBlockState(blockPos).getFluidState();
    }

    @Nullable
    @Override
    public BlockEntity getBlockEntity(@NonNull BlockPos blockPos) {
        return this.subGridBlocks.getBlockState(blockPos);
    }

    @Override
    public int getHeight() {
        return this.subGridBlocks.sizeY();
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public @NonNull WorldBorder getWorldBorder() {
        return this.originalLevel.getWorldBorder();
    }

    @Nullable
    @Override
    public BlockGetter getChunkForCollisions(int i, int j) {
        return this;
    }

    @Override
    public @NonNull List<VoxelShape> getEntityCollisions(@Nullable Entity entity, @NonNull AABB aABB) {
        return List.of();
    }

    @Override
    public @NonNull Holder<Biome> getBiome(BlockPos pos) {
        return biomeHolder;
    }

    @Override
    public @NonNull Holder<Biome> getNoiseBiome(int quartX, int quartY, int quartZ) {
        return biomeHolder;
    }

    @Override
    public @NonNull Holder<Biome> getUncachedNoiseBiome(int quartX, int quartY, int quartZ) {
        return biomeHolder;
    }

    @Override
    public @NonNull Set<Holder<Biome>> getBiomesWithin(int x, int y, int z, int radius) {
        return Set.of(biomeHolder);
    }
}
