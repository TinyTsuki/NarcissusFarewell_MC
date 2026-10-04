package xin.vanilla.narcissus.search;

import net.minecraft.util.math.BlockPos;
import xin.vanilla.narcissus.enums.EnumSafeMode;

import java.util.ArrayList;
import java.util.List;

final class LegacyCandidateOrder {
    private LegacyCandidateOrder() {
    }

    static List<BlockPos> enumerate(EnumSafeMode mode, int cx, int cy, int cz, SearchBox box) {
        List<BlockPos> points = new ArrayList<>();
        if (mode == EnumSafeMode.Y_C_TO_T) {
            for (int y = cy; y <= box.maxY; y++) points.add(new BlockPos(cx, y, cz));
            return points;
        }
        if (mode == EnumSafeMode.Y_B_TO_C) {
            for (int y = box.minY; y <= cy; y++) points.add(new BlockPos(cx, y, cz));
            return points;
        }
        if (mode == EnumSafeMode.Y_C_TO_B) {
            for (int y = cy; y >= box.minY; y--) points.add(new BlockPos(cx, y, cz));
            return points;
        }
        if (mode == EnumSafeMode.Y_T_TO_C) {
            for (int y = box.maxY; y >= cy; y--) points.add(new BlockPos(cx, y, cz));
            return points;
        }
        if (mode == EnumSafeMode.Y_C_OFFSET_3) {
            for (int offset : new int[]{0, -1, 1, -2, 2, -3}) {
                int y = cy + offset;
                if (y >= box.minY && y <= box.maxY) points.add(new BlockPos(cx, y, cz));
            }
            return points;
        }
        int rangeX = Math.max(Math.abs(box.maxX - cx), Math.abs(box.minX - cx));
        int rangeZ = Math.max(Math.abs(box.maxZ - cz), Math.abs(box.minZ - cz));
        int rangeY = Math.max(Math.abs(box.maxY - cy), Math.abs(box.minY - cy));
        int maxManhattan = rangeX + rangeZ + rangeY;
        for (int m = 0; m <= maxManhattan; m++) {
            for (int dx = -m; dx <= m; dx++) {
                int rest = m - Math.abs(dx);
                for (int dy = -rest; dy <= rest; dy++) {
                    int dzAbs = rest - Math.abs(dy);
                    if (dzAbs == 0) {
                        addIfInside(points, cx + dx, cy + dy, cz, box);
                    } else {
                        for (int sign = 0; sign < 2; sign++) {
                            int dz = sign == 0 ? dzAbs : -dzAbs;
                            addIfInside(points, cx + dx, cy + dy, cz + dz, box);
                        }
                    }
                }
            }
        }
        return points;
    }

    private static void addIfInside(List<BlockPos> points, int x, int y, int z, SearchBox box) {
        if (x >= box.minX && x <= box.maxX && y >= box.minY && y <= box.maxY
                && z >= box.minZ && z <= box.maxZ) points.add(new BlockPos(x, y, z));
    }
}
