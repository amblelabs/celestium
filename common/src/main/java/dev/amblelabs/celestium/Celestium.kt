package dev.amblelabs.celestium

import dev.amblelabs.celestium.init.CelestiumBlocks
import dev.amblelabs.celestium.init.CelestiumEntityTypes.initialize
import dev.amblelabs.celestium.init.CelestiumItems
import dev.amblelabs.celestium.init.CelestiumSounds
import net.minecraft.core.Registry
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.gamerules.GameRule
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Celestium {
    const val MOD_ID: String = "celestium"

    @JvmField
    val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

    @JvmStatic
    fun id(s: String): Identifier {
        return Identifier.fromNamespaceAndPath(MOD_ID, s)
    }

    @JvmField
    val THRUSTER: SimpleParticleType = object : SimpleParticleType(false) {}

    lateinit var MAX_FLOATATER_SIZE: GameRule<Int>

    @JvmStatic
    fun init() {
        CelestiumSounds.register()
        CelestiumBlocks.register()
        CelestiumItems.register()

        initialize()

        Registry.register(
            BuiltInRegistries.PARTICLE_TYPE,
            Identifier.fromNamespaceAndPath(MOD_ID, "thruster"),
            THRUSTER
        )
    }
}
