package dev.amblelabs.celestium.util

import net.minecraft.client.Minecraft
import net.minecraft.util.Mth
import org.joml.Vector3f
import org.joml.Vector3fc

object SkyFadeUtil {
    @JvmStatic
    fun space(y: Double): Float {
        val t = Mth.clamp(((y - 300) / (750 - 300)).toFloat(), 0f, 1f)
        return t * t * (3f - 2f * t)
    }

    @JvmStatic
    fun current(): Float {
        val mc = Minecraft.getInstance()
        return space(mc.gameRenderer.mainCamera().position().y)
    }

    @JvmStatic
    fun darken(color: Vector3fc, fade: Float): Vector3fc {
        return Vector3f(color).mul(1.0f - fade)
    }
}