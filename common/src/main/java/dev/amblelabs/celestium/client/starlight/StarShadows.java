package dev.amblelabs.celestium.client.starlight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.joml.Vector3fc;

public final class StarShadows {

    private StarShadows() {}

    public static int skyLight(BlockGetter level, BlockPos pos, int sky, Vector3fc toward,
                               int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (sky <= 2) return sky;
        return blocked(level, pos, toward, minX, minY, minZ, maxX, maxY, maxZ) ? shadowed(sky) : sky;
    }

    public static int shadowed(int sky) {
        return Math.min(sky, 2);
    }

    public static boolean blocked(BlockGetter level, BlockPos origin, Vector3fc dir,
                                  int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int x = origin.getX(), y = origin.getY(), z = origin.getZ();
        int stepX = dir.x() > 0 ? 1 : -1, stepY = dir.y() > 0 ? 1 : -1, stepZ = dir.z() > 0 ? 1 : -1;
        double deltaX = dir.x() == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.x());
        double deltaY = dir.y() == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.y());
        double deltaZ = dir.z() == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.z());
        double nextX = deltaX * 0.5, nextY = deltaY * 0.5, nextZ = deltaZ * 0.5;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        while (true) {
            double t;
            if (nextX <= nextY && nextX <= nextZ) {
                t = nextX;
                x += stepX;
                nextX += deltaX;
            } else if (nextY <= nextZ) {
                t = nextY;
                y += stepY;
                nextY += deltaY;
            } else {
                t = nextZ;
                z += stepZ;
                nextZ += deltaZ;
            }
            if (t > 32) return false;
            if (x < minX || y < minY || z < minZ || x > maxX || y > maxY || z > maxZ) return false;
            if (level.getBlockState(cursor.set(x, y, z)).canOcclude()) return true;
        }
    }
}
