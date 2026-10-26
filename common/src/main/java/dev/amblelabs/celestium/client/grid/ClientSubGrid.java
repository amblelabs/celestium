package dev.amblelabs.celestium.client.grid;

import dev.amblelabs.celestium.world.grid.GridCarrier;
import dev.amblelabs.celestium.world.grid.SubGrid;
import net.minecraft.client.multiplayer.ClientLevel;

public class ClientSubGrid extends SubGrid implements AutoCloseable {
    private final SubGridRenderer renderer = new SubGridRenderer(this);

    public ClientSubGrid(ClientLevel clientLevel, GridCarrier gridCarrier) {
        super(clientLevel, gridCarrier);
    }

    public SubGridRenderer getRenderer() {
        return this.renderer;
    }

    public void close() {
        this.renderer.close();
    }

    @Override
    public int getMinY() {
        return 0;
    }
}

