package dev.amblelabs.celestium.core;

import dev.amblelabs.celestium.Celestium;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class CelestiumSounds {

    public static final SoundEvent SPACE_AMBIENT =
            register("ambient.space");


    private static SoundEvent register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(Celestium.MOD_ID, name);
        return Registry.register(
                BuiltInRegistries.SOUND_EVENT,
                id,
                SoundEvent.createVariableRangeEvent(id)
        );
    }

    public static void register() {
    }
}
