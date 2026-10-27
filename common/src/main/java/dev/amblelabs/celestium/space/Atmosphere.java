package dev.amblelabs.celestium.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Atmosphere(int color, int layers) {
    public static final Codec<Atmosphere> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("color").forGetter(Atmosphere::color),
            Codec.INT.fieldOf("layers").forGetter(Atmosphere::layers)
    ).apply(instance, Atmosphere::new));
}
