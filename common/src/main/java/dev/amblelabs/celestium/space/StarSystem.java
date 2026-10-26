package dev.amblelabs.celestium.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.List;

public record StarSystem(List<AstralBody> astralBodies, float timeCode, AstralBody centralStar, int posX, int posY, int posZ) {
    public static final Codec<StarSystem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            AstralBody.CODEC.listOf().fieldOf("astral_bodies").forGetter(StarSystem::astralBodies),
            Codec.FLOAT.fieldOf("time_code").forGetter(StarSystem::timeCode),
            AstralBody.CODEC.fieldOf("central_star").forGetter(StarSystem::centralStar),
            Codec.INT.fieldOf("pos_x").forGetter(StarSystem::posX),
            Codec.INT.fieldOf("pos_y").forGetter(StarSystem::posY),
            Codec.INT.fieldOf("pos_z").forGetter(StarSystem::posZ)
    ).apply(instance, StarSystem::new));
}
