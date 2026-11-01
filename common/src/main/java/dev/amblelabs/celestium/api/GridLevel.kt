package dev.amblelabs.celestium.api;

import dev.amblelabs.celestium.world.grid.SubGrid;

import java.util.Map;
import java.util.UUID;

public interface GridLevel {
    Map<UUID, SubGrid> celestium$grids();
}
