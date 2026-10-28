package dev.amblelabs.celestium.mixins.client.starlight;

import com.mojang.blaze3d.platform.Lighting;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Lighting.class)
public interface LightingAccessor {

    @Invoker("updateBuffer")
    void celestium$updateBuffer(Lighting.Entry entry, Vector3fc light0, Vector3fc light1);
}
