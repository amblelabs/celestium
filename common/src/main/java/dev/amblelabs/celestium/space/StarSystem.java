package dev.amblelabs.celestium.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

public record StarSystem(Identifier id, List<AstralBody> astralBodies, float timeCode, AstralBody centralStar, Vector3fc rot, int posX, int posY, int posZ) {
    public static final Codec<StarSystem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(StarSystem::id),
            AstralBody.CODEC.listOf().fieldOf("astral_bodies").forGetter(StarSystem::astralBodies),
            Codec.FLOAT.fieldOf("time_code").forGetter(StarSystem::timeCode),
            AstralBody.CODEC.fieldOf("central_star").forGetter(StarSystem::centralStar),
            ExtraCodecs.VECTOR3F.fieldOf("rot").forGetter(StarSystem::rot),
            Codec.INT.fieldOf("pos_x").forGetter(StarSystem::posX),
            Codec.INT.fieldOf("pos_y").forGetter(StarSystem::posY),
            Codec.INT.fieldOf("pos_z").forGetter(StarSystem::posZ)
    ).apply(instance, StarSystem::new));
}
