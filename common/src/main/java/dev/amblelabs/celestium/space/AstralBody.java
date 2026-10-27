package dev.amblelabs.celestium.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record AstralBody(Identifier id, boolean isStar, int defaultColor, float axis, Orbit orbit, Atmosphere atmosphere, boolean isSatellite, List<AstralBody> satellites, int sizeX, int sizeY, int sizeZ) {
    public static final Codec<AstralBody> CODEC = Codec.recursive("AstralBody", selfCodec -> RecordCodecBuilder.create(instance -> instance.group(
        Identifier.CODEC.fieldOf("id").forGetter(AstralBody::id),
        Codec.BOOL.fieldOf("is_star").forGetter(AstralBody::isStar),
        Codec.INT.fieldOf("default_color").forGetter(AstralBody::defaultColor),
        Codec.FLOAT.fieldOf("axis").forGetter(AstralBody::axis),
        Orbit.CODEC.fieldOf("orbit").forGetter(AstralBody::orbit),
        Atmosphere.CODEC.fieldOf("atmosphere").forGetter(AstralBody::atmosphere),
        Codec.BOOL.fieldOf("is_satellite").forGetter(AstralBody::isSatellite),
            selfCodec.listOf().fieldOf("satellites").forGetter(AstralBody::satellites),
        Codec.INT.fieldOf("size_x").forGetter(AstralBody::sizeX),
        Codec.INT.fieldOf("size_y").forGetter(AstralBody::sizeY),
        Codec.INT.fieldOf("size_z").forGetter(AstralBody::sizeZ)
    ).apply(instance, AstralBody::new)));

    /*public static final StreamCodec<FriendlyByteBuf, AstralBody> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf output, AstralBody value) {
            output.writeIdentifier(value.id());
            output.writeInt(value.defaultColor());
            output.writeFloat(value.axis());
            Orbit.STREAM_CODEC.encode(output, value.orbit());
            output.writeInt(value.sizeX());
            output.writeInt(value.sizeY());
            output.writeInt(value.sizeZ());
        }

        @Override
        public AstralBody decode(FriendlyByteBuf input) {
            Identifier id = input.readIdentifier();
            int defaultColor = input.readInt();
            float axis = input.readFloat();
            Orbit orbit = Orbit.STREAM_CODEC.decode(input);
            List<AstralBody> moons =
            int sizeX = input.readInt();
            int sizeY = input.readInt();
            int sizeZ = input.readInt();
            return new AstralBody(id, defaultColor, axis, orbit, moons, sizeX, sizeY, sizeZ);
        }
    };*/
}
