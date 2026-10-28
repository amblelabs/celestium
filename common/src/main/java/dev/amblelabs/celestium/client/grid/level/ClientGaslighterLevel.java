package dev.amblelabs.celestium.client.grid.level;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ClientGaslighterLevel extends ClientLevel {
    public ClientGaslighterLevel(ClientPacketListener connection, ClientLevelData levelData, ResourceKey<Level> dimension, Holder<DimensionType> dimensionType, int serverChunkRadius, int serverSimulationDistance, LevelExtractor levelExtractor, boolean isDebug, long biomeZoomSeed, int seaLevel) {
        super(connection, levelData, dimension, dimensionType, serverChunkRadius, serverSimulationDistance, levelExtractor, isDebug, biomeZoomSeed, seaLevel);
    }
}
