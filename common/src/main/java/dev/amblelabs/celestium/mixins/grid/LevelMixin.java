package dev.amblelabs.celestium.mixins.grid;

import dev.amblelabs.celestium.api.GridLevel;
import dev.amblelabs.celestium.world.grid.SubGrid;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Mixin(Level.class)
public abstract class LevelMixin implements GridLevel {
   @Unique private final Map<UUID, SubGrid> celestium$grids = new LinkedHashMap<>();

   @Override
   public Map<UUID, SubGrid> celestium$grids() {
      return this.celestium$grids;
   }
}
