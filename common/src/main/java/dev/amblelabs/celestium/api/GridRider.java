package dev.amblelabs.celestium.api;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface GridRider {
    int SERVER_RELOAD_TIMEOUT = 40;
    int CLIENT_RELOAD_TIMEOUT = 60;

    @Nullable UUID celestium$pendingGrid();

    void celestium$waitForGrid(UUID gridId, int timeout);
}
