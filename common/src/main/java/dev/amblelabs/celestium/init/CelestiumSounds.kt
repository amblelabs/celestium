package dev.amblelabs.celestium.init

import dev.amblelabs.celestium.Celestium
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

object CelestiumSounds {
    val SPACE_AMBIENT: SoundEvent = register("ambient.space")
    val ALONE: SoundEvent = register("music.alone")

    val ENTITY_PLAYER_BREATHING: SoundEvent = register("entity.player.breathing")
    val ENTITY_PLAYER_HEARTBEAT: SoundEvent = register("entity.player.heartbeat")

    private fun register(name: String): SoundEvent {
        val id = Identifier.fromNamespaceAndPath(Celestium.MOD_ID, name)
        return Registry.register(
            BuiltInRegistries.SOUND_EVENT,
            id,
            SoundEvent.createVariableRangeEvent(id)
        )
    }

    @JvmStatic
    fun register() {
    }
}
