package dev.amblelabs.celestium.init;

import dev.amblelabs.celestium.Celestium;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class CelestiumSounds {

    public static final SoundEvent SPACE_AMBIENT = register("ambient.space");


    public static final SoundEvent ALONE = register("music.alone");

    public static final SoundEvent ENTITY_PLAYER_BREATHING = register("entity.player.breathing");
    public static final SoundEvent ENTITY_PLAYER_HEARTBEAT = register("entity.player.heartbeat");


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
