package dev.amblelabs.celestium.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public record Ring(float innerRadius, float outerRadius, int color, Vector3fc rotation) {
    public static final Codec<Ring> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("inner_radius").forGetter(Ring::innerRadius),
            Codec.FLOAT.fieldOf("outer_radius").forGetter(Ring::outerRadius),
            Codec.INT.fieldOf("color").forGetter(Ring::color),
            ExtraCodecs.VECTOR3F.optionalFieldOf("rotation", new Vector3f()).forGetter(Ring::rotation)
    ).apply(instance, Ring::new));
}
