package dev.amblelabs.celestium.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jspecify.annotations.NonNull;

public record Orbit(float speed, float distanceFromCenter) {
    public static final Codec<Orbit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("speed").forGetter(Orbit::speed),
            Codec.FLOAT.fieldOf("distance_from_center").forGetter(Orbit::distanceFromCenter)
    ).apply(instance, Orbit::new));

    public static final StreamCodec<FriendlyByteBuf, Orbit> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public void encode(FriendlyByteBuf output, Orbit value) {
            output.writeFloat(value.speed());
            output.writeFloat(value.distanceFromCenter());
        }

        @Override
        public @NonNull Orbit decode(FriendlyByteBuf input) {
            float speed = input.getFloat(0);
            float distanceFromCenter = input.getFloat(1);
            return new Orbit(speed, distanceFromCenter);
        }
    };
}
