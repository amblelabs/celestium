package dev.amblelabs.celestium.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amblelabs.celestium.init.CelestiumRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public record AstralBody(boolean isStar, int defaultColor, float axis, float spinSpeed, Orbit orbit, @Nullable Atmosphere atmosphere, boolean isSatellite, List<ResourceKey<AstralBody>> satellites, int sizeX, int sizeY, int sizeZ) {
    public static final Codec<AstralBody> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.BOOL.optionalFieldOf("is_star", false).forGetter(AstralBody::isStar),
            Codec.INT.fieldOf("default_color").forGetter(AstralBody::defaultColor),
            Codec.FLOAT.optionalFieldOf("axis", 0.0F).forGetter(AstralBody::axis),
            Codec.FLOAT.optionalFieldOf("spin_speed", 0.0F).forGetter(AstralBody::spinSpeed),
            Orbit.CODEC.fieldOf("orbit").forGetter(AstralBody::orbit),
            Atmosphere.CODEC.optionalFieldOf("atmosphere").forGetter(b -> Optional.ofNullable(b.atmosphere())),
            Codec.BOOL.optionalFieldOf("is_satellite", false).forGetter(AstralBody::isSatellite),
            ResourceKey.codec(CelestiumRegistries.ASTRAL_BODY).listOf().optionalFieldOf("satellites", List.of()).forGetter(AstralBody::satellites),
            Codec.INT.fieldOf("size_x").forGetter(AstralBody::sizeX),
            Codec.INT.fieldOf("size_y").forGetter(AstralBody::sizeY),
            Codec.INT.fieldOf("size_z").forGetter(AstralBody::sizeZ)
    ).apply(i, (star, color, axis, spin, orbit, atmo, sat, sats, x, y, z) ->
            new AstralBody(star, color, axis, spin, orbit, atmo.orElse(null), sat, sats, x, y, z)));

    public float spinDegrees(double ticks) {
        return (float) ((ticks / 20.0 * spinSpeed * Orbit.SPEED_SCALE) % 360.0);
    }
}