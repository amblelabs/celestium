package dev.amblelabs.celestium.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record Orbit(float speed, float distanceFromCenter, float phase) {

    public static final float SPEED_SCALE = 0.001F;

    public static final Codec<Orbit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("speed").forGetter(Orbit::speed),
            Codec.FLOAT.fieldOf("distance_from_center").forGetter(Orbit::distanceFromCenter),
            Codec.FLOAT.optionalFieldOf("phase", 0.0F).forGetter(Orbit::phase)
    ).apply(instance, Orbit::new));

    public static final StreamCodec<ByteBuf, Orbit> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, Orbit::speed,
            ByteBufCodecs.FLOAT, Orbit::distanceFromCenter,
            ByteBufCodecs.FLOAT, Orbit::phase,
            Orbit::new);

    public float angleDegrees(double ticks) {
        return (float) ((phase + ticks / 20.0 * speed * SPEED_SCALE) % 360.0);
    }
}