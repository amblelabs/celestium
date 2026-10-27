package dev.amblelabs.celestium.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record AstralBody(Identifier id, boolean isStar, int defaultColor, float axis, float spinSpeed, Orbit orbit, Atmosphere atmosphere, boolean isSatellite, List<AstralBody> satellites, int sizeX, int sizeY, int sizeZ) {
    public static final Codec<AstralBody> CODEC = Codec.recursive("AstralBody", selfCodec -> RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(AstralBody::id),
            Codec.BOOL.fieldOf("is_star").forGetter(AstralBody::isStar),
            Codec.INT.fieldOf("default_color").forGetter(AstralBody::defaultColor),
            Codec.FLOAT.fieldOf("axis").forGetter(AstralBody::axis),
            Codec.FLOAT.optionalFieldOf("spin_speed", 0.0F).forGetter(AstralBody::spinSpeed),
            Orbit.CODEC.fieldOf("orbit").forGetter(AstralBody::orbit),
            Atmosphere.CODEC.fieldOf("atmosphere").forGetter(AstralBody::atmosphere),
            Codec.BOOL.fieldOf("is_satellite").forGetter(AstralBody::isSatellite),
            selfCodec.listOf().fieldOf("satellites").forGetter(AstralBody::satellites),
            Codec.INT.fieldOf("size_x").forGetter(AstralBody::sizeX),
            Codec.INT.fieldOf("size_y").forGetter(AstralBody::sizeY),
            Codec.INT.fieldOf("size_z").forGetter(AstralBody::sizeZ)
    ).apply(instance, AstralBody::new)));

    public float spinDegrees(double ticks) {
        return (float) ((ticks / 20.0 * spinSpeed * Orbit.SPEED_SCALE) % 360.0);
    }
}