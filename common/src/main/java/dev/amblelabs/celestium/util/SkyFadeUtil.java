package dev.amblelabs.celestium.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class SkyFadeUtil {
    private SkyFadeUtil() {}

    public static float space(double y) {
        float t = Mth.clamp((float) ((y - 300) / (750 - 300)), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    public static float current() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null) return 0.0F;
        return space(mc.gameRenderer.mainCamera().position().y);
    }

    public static Vector3fc darken(Vector3fc color, float fade) {
        return new Vector3f(color).mul(1.0F - fade);
    }
}