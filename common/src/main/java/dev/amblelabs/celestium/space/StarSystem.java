package dev.amblelabs.celestium.space;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.amblelabs.celestium.init.CelestiumRegistries;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.codec.RegistryFixedCodec;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

public record StarSystem(List<Holder<AstralBody>> astralBodies, float timeCode, Holder<AstralBody> centralStar, Vector3fc rot, int posX, int posY, int posZ) {
    private static final Codec<Holder<AstralBody>> BODY = RegistryFixedCodec.create(CelestiumRegistries.ASTRAL_BODY);
    public static final Codec<StarSystem> CODEC = RecordCodecBuilder.create(i -> i.group(
            BODY.listOf().fieldOf("astral_bodies").forGetter(StarSystem::astralBodies),
            Codec.FLOAT.fieldOf("time_code").forGetter(StarSystem::timeCode),
            BODY.fieldOf("central_star").forGetter(StarSystem::centralStar),
            ExtraCodecs.VECTOR3F.fieldOf("rot").forGetter(StarSystem::rot),
            Codec.INT.fieldOf("pos_x").forGetter(StarSystem::posX),
            Codec.INT.fieldOf("pos_y").forGetter(StarSystem::posY),
            Codec.INT.fieldOf("pos_z").forGetter(StarSystem::posZ)
    ).apply(i, StarSystem::new));
}
