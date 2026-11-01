package dev.amblelabs.celestium.client.particle

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SingleQuadParticle
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import kotlin.math.max

class ThrusterParticle private constructor(
    level: ClientLevel, x: Double, y: Double, z: Double,
    xa: Double, ya: Double, za: Double, sprites: SpriteSet
) : SingleQuadParticle(level, x, y, z, 0.0, 0.0, 0.0, sprites.first()) {
    private val sprites: SpriteSet
    private val shade: Float

    init {
        this.friction = 0.96f
        this.sprites = sprites
        this.xd *= 0.1
        this.yd *= 0.1
        this.zd *= 0.1
        this.xd += xa
        this.yd += ya
        this.zd += za
        val col = 1.0f - this.random.nextFloat() * 0.3f
        this.shade = 1.0f - this.random.nextFloat() * 0.3f
        this.rCol = col
        this.gCol = col
        this.bCol = col
        this.quadSize *= 5f
        val baseLifetime = (8.0f / (this.random.nextFloat() * 0.8 + 0.3)).toInt()
        this.lifetime = max((baseLifetime * 5.0f).toInt(), 1)
        this.hasPhysics = false
        this.setSpriteFromAge(sprites)
        this.updateGradient()
    }

    private fun updateGradient() {
        val t = Mth.clamp(this.age.toFloat() / this.lifetime, 0.0f, 1.0f)
        this.rCol = Mth.lerp(t, 0.20f, 0.60f) * shade
        this.gCol = Mth.lerp(t, 0.50f, 0.20f) * shade
        this.bCol = Mth.lerp(t, 1.00f, 0.90f) * shade
    }

    public override fun getLayer(): Layer {
        return Layer.TRANSLUCENT
    }

    override fun getQuadSize(a: Float): Float {
        return this.quadSize * Mth.clamp((this.age.toFloat() + a) / this.lifetime.toFloat() * 32.0f, 0.0f, 1.0f)
    }

    override fun tick() {
        super.tick()
        this.updateGradient()

        if (!this.removed) {
            this.setSpriteFromAge(this.sprites)
            val player = this.level.getNearestPlayer(this.x, this.y, this.z, 2.0, false)
            if (player != null) {
                val playerY = player.y
                if (this.y > playerY) {
                    this.y += (playerY - this.y) * 0.2
                    this.yd += (player.deltaMovement.y - this.yd) * 0.2
                    this.setPos(this.x, this.y, this.z)
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    class Provider(private val sprites: SpriteSet) : ParticleProvider<SimpleParticleType> {
        override fun createParticle(
            options: SimpleParticleType, level: ClientLevel,
            x: Double, y: Double, z: Double,
            xAux: Double, yAux: Double, zAux: Double,
            random: RandomSource
        ): Particle? {
            return ThrusterParticle(level, x, y, z, xAux, yAux, zAux, this.sprites)
        }
    }

    @Environment(EnvType.CLIENT)
    class SneezeProvider(private val sprites: SpriteSet) : ParticleProvider<SimpleParticleType> {
        override fun createParticle(
            options: SimpleParticleType, level: ClientLevel,
            x: Double, y: Double, z: Double,
            xAux: Double, yAux: Double, zAux: Double,
            random: RandomSource
        ): Particle {
            val particle = ThrusterParticle(level, x, y, z, xAux, yAux, zAux, this.sprites)
            particle.setColor(0.22f, 1.0f, 0.53f)
            particle.setAlpha(0.4f)
            return particle
        }
    }
}