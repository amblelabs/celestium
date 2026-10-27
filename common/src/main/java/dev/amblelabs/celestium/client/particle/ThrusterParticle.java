package dev.amblelabs.celestium.client.particle;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;

public class ThrusterParticle extends SingleQuadParticle {
    private final SpriteSet sprites;
    private final float shade;

    private ThrusterParticle(final ClientLevel level, final double x, final double y, final double z,
                             final double xa, final double ya, final double za, final SpriteSet sprites) {
        super(level, x, y, z, 0.0D, 0.0D, 0.0D, sprites.first());
        this.friction = 0.96F;
        this.sprites = sprites;
        this.xd *= 0.1D;
        this.yd *= 0.1D;
        this.zd *= 0.1D;
        this.xd += xa;
        this.yd += ya;
        this.zd += za;
        float col = 1.0F - this.random.nextFloat() * 0.3F;
        this.shade = 1.0F - this.random.nextFloat() * 0.3F;
        this.rCol = col;
        this.gCol = col;
        this.bCol = col;
        this.quadSize *= 5F;
        int baseLifetime = (int) (8.0F / (this.random.nextFloat() * 0.8 + 0.3));
        this.lifetime = Math.max((int) (baseLifetime * 5.0F), 1);
        this.hasPhysics = false;
        this.setSpriteFromAge(sprites);
        this.updateGradient();
    }

    private void updateGradient() {
        float t = Mth.clamp((float) this.age / this.lifetime, 0.0F, 1.0F);
        this.rCol = Mth.lerp(t, 0.20F, 0.60F) * shade;
        this.gCol = Mth.lerp(t, 0.50F, 0.20F) * shade;
        this.bCol = Mth.lerp(t, 1.00F, 0.90F) * shade;
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    @Override
    public float getQuadSize(final float a) {
        return this.quadSize * Mth.clamp(((float) this.age + a) / (float) this.lifetime * 32.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        this.updateGradient();

        if (!this.removed) {
            this.setSpriteFromAge(this.sprites);
            Player player = this.level.getNearestPlayer(this.x, this.y, this.z, 2.0D, false);
            if (player != null) {
                double playerY = player.getY();
                if (this.y > playerY) {
                    this.y += (playerY - this.y) * 0.2;
                    this.yd += (player.getDeltaMovement().y - this.yd) * 0.2;
                    this.setPos(this.x, this.y, this.z);
                }
            }
        }
    }

    @Environment(EnvType.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(final SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(final SimpleParticleType options, final ClientLevel level,
                                       final double x, final double y, final double z,
                                       final double xAux, final double yAux, final double zAux,
                                       final RandomSource random) {
            return new ThrusterParticle(level, x, y, z, xAux, yAux, zAux, this.sprites);
        }
    }

    @Environment(EnvType.CLIENT)
    public static class SneezeProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SneezeProvider(final SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(final SimpleParticleType options, final ClientLevel level,
                                       final double x, final double y, final double z,
                                       final double xAux, final double yAux, final double zAux,
                                       final RandomSource random) {
            ThrusterParticle particle = new ThrusterParticle(level, x, y, z, xAux, yAux, zAux, this.sprites);
            particle.setColor(0.22F, 1.0F, 0.53F);
            particle.setAlpha(0.4F);
            return particle;
        }
    }
}