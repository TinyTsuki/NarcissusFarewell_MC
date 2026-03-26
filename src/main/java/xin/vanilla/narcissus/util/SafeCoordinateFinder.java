package xin.vanilla.narcissus.util;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumSafeMode;

import javax.annotation.Nullable;


public class SafeCoordinateFinder {

    private static final Logger LOGGER = LogManager.getLogger();

    private final World world;
    private final SafeBlockChecker checker;
    private final BlockPos.Mutable mutablePos;

    public SafeCoordinateFinder(World world) {
        this.world = world;
        this.checker = new SafeBlockChecker(world);
        this.mutablePos = new BlockPos.Mutable();
    }

    public int getWorldMinY() {
        return DimensionUtils.getWorldMinY(world);
    }

    public int getWorldMaxY() {
        return DimensionUtils.getWorldMaxY(world);
    }

    /**
     * 从当前位置向上查找（从高到低）
     */
    @Nullable
    public SafeWorldCoordinate findTopCandidate(SafeWorldCoordinate start) {
        if (start.y() >= getWorldMaxY()) return null;
        int x = start.xInt();
        int z = start.zInt();
        for (int y = getWorldMaxY(); y > start.yInt(); y--) {
            mutablePos.set(x, y, z);
            if (checker.isSafeBlock(mutablePos.immutable(), false)) {
                SafeWorldCoordinate clone = start.clone();
                clone.y(y);
                return clone;
            }
        }
        return null;
    }

    /**
     * 从底部到当前位置查找（从低到高）
     */
    @Nullable
    public SafeWorldCoordinate findBottomCandidate(SafeWorldCoordinate start) {
        if (start.y() <= getWorldMinY()) return null;
        int x = start.xInt();
        int z = start.zInt();
        for (int y = getWorldMinY(); y < start.yInt(); y++) {
            mutablePos.set(x, y, z);
            if (checker.isSafeBlock(mutablePos.immutable(), false)) {
                SafeWorldCoordinate clone = start.clone();
                clone.y(y);
                return clone;
            }
        }
        return null;
    }

    /**
     * 从当前位置向上查找（从低到高）
     */
    @Nullable
    public SafeWorldCoordinate findUpCandidate(SafeWorldCoordinate start) {
        if (start.y() >= getWorldMaxY()) return null;
        int x = start.xInt();
        int z = start.zInt();
        for (int y = start.yInt() + 1; y <= getWorldMaxY(); y++) {
            mutablePos.set(x, y, z);
            if (checker.isSafeBlock(mutablePos.immutable(), false)) {
                SafeWorldCoordinate clone = start.clone();
                clone.y(y);
                return clone;
            }
        }
        return null;
    }

    /**
     * 从当前位置向下查找（从高到低）
     */
    @Nullable
    public SafeWorldCoordinate findDownCandidate(SafeWorldCoordinate start) {
        if (start.y() <= getWorldMinY()) return null;
        int x = start.xInt();
        int z = start.zInt();
        for (int y = start.yInt() - 1; y >= getWorldMinY(); y--) {
            mutablePos.set(x, y, z);
            if (checker.isSafeBlock(mutablePos.immutable(), false)) {
                SafeWorldCoordinate clone = start.clone();
                clone.y(y);
                return clone;
            }
        }
        return null;
    }

    /**
     * 沿玩家视线方向射线检测，返回碰撞点或射线终点；
     * 若 safe 则反向查找安全站立位置
     */
    @Nullable
    public SafeWorldCoordinate findViewEndCandidate(ServerPlayerEntity player, boolean safe, int range) {
        LOGGER.debug("TimeMillis before findViewEndCandidate: {}", System.currentTimeMillis());
        final double stepScale = 0.75;
        final SafeWorldCoordinate start = new SafeWorldCoordinate(player);
        SafeWorldCoordinate result;

        final Vector3d startPosition = player.getEyePosition(1.0F);
        final Vector3d stepVector = player.getViewVector(1.0F).normalize().scale(stepScale);
        final double stepX = stepVector.x;
        final double stepY = stepVector.y;
        final double stepZ = stepVector.z;
        final double startX = startPosition.x;
        final double startY = startPosition.y;
        final double startZ = startPosition.z;

        // 从近到远寻找碰撞点
        int collisionStep = -1;
        for (int stepCount = 0; stepCount <= range; stepCount++) {
            mutablePos.set(
                    MathHelper.floor(startX + stepX * stepCount),
                    MathHelper.floor(startY + stepY * stepCount),
                    MathHelper.floor(startZ + stepZ * stepCount)
            );
            if (world.getBlockState(mutablePos).getMaterial().blocksMotion()) {
                collisionStep = stepCount;
                break;
            }
        }

        // 确定碰撞点或射线终点
        SafeWorldCoordinate clone = start.clone();
        if (collisionStep > 0) {
            clone.x(startX + stepX * (collisionStep - 1))
                    .y(startY + stepY * (collisionStep - 1))
                    .z(startZ + stepZ * (collisionStep - 1));
        } else if (collisionStep == 0) {
            clone.fromVector3d(startPosition);
        } else {
            clone.x(startX + stepX * range)
                    .y(startY + stepY * range)
                    .z(startZ + stepZ * range);
        }
        result = clone;

        // 若需寻找安全坐标，则从碰撞点反向查找安全位置
        if (safe) {
            final double dist = Math.sqrt(
                    Math.pow(result.x() - startX, 2) + Math.pow(result.y() - startY, 2) + Math.pow(result.z() - startZ, 2)
            );
            final int maxStep = (int) Math.ceil(dist / stepScale);
            final int[] yOffsets = {0, -1, 1, -2, 2, -3};
            boolean found = false;
            for (int stepCount = maxStep; stepCount >= 0 && !found; stepCount--) {
                final int blockX = MathHelper.floor(startX + stepX * stepCount);
                final int blockY = MathHelper.floor(startY + stepY * stepCount);
                final int blockZ = MathHelper.floor(startZ + stepZ * stepCount);
                for (int yOffset : yOffsets) {
                    mutablePos.set(blockX, blockY + yOffset, blockZ);
                    if (checker.isSafeBlock(mutablePos.immutable(), false)) {
                        clone.fromBlockPos(mutablePos).addX(0.5).addY(0.15).addZ(0.5);
                        found = true;
                        break;
                    }
                }
            }
        }
        if (result != null && start.equalsInRange(result, 1)) {
            result = null;
        }
        LOGGER.debug("TimeMillis after findViewEndCandidate: {}", System.currentTimeMillis());
        return result;
    }

    /**
     * 在区块范围内按安全模式搜索，使用螺旋迭代避免全量排序
     */
    @Nullable
    public SafeWorldCoordinate searchInChunk(SafeWorldCoordinate safeWorldCoordinate, int chunkX, int chunkZ, boolean belowAllowAir) {
        int offset = (CommonConfig.get().general().safeTeleport().safeChunkRange() - 1) * 16;
        int chunkMinX = (chunkX << 4) - offset;
        int chunkMinZ = (chunkZ << 4) - offset;
        int chunkMaxX = chunkMinX + 15 + offset;
        int chunkMaxZ = chunkMinZ + 15 + offset;
        int minY = getWorldMinY();
        int maxY = getWorldMaxY();
        int cx = safeWorldCoordinate.xInt();
        int cy = safeWorldCoordinate.yInt();
        int cz = safeWorldCoordinate.zInt();

        EnumSafeMode mode = safeWorldCoordinate.safeMode();

        // Y 轴单列模式：直接迭代，无需列表与排序
        if (mode == EnumSafeMode.Y_C_TO_T) {
            for (int y = cy; y <= maxY; y++) {
                mutablePos.set(cx, y, cz);
                if (checker.isSafeBlock(mutablePos.immutable(), belowAllowAir)) {
                    return toResult(mutablePos);
                }
            }
            return null;
        }
        if (mode == EnumSafeMode.Y_B_TO_C) {
            for (int y = minY; y <= cy; y++) {
                mutablePos.set(cx, y, cz);
                if (checker.isSafeBlock(mutablePos.immutable(), belowAllowAir)) {
                    return toResult(mutablePos);
                }
            }
            return null;
        }
        if (mode == EnumSafeMode.Y_C_TO_B) {
            for (int y = cy; y >= minY; y--) {
                mutablePos.set(cx, y, cz);
                if (checker.isSafeBlock(mutablePos.immutable(), belowAllowAir)) {
                    return toResult(mutablePos);
                }
            }
            return null;
        }
        if (mode == EnumSafeMode.Y_T_TO_C) {
            for (int y = maxY; y >= cy; y--) {
                mutablePos.set(cx, y, cz);
                if (checker.isSafeBlock(mutablePos.immutable(), belowAllowAir)) {
                    return toResult(mutablePos);
                }
            }
            return null;
        }
        if (mode == EnumSafeMode.Y_C_OFFSET_3) {
            int[] yOffsets = {0, -1, 1, -2, 2, -3};
            for (int dy : yOffsets) {
                int y = cy + dy;
                if (y >= minY && y <= maxY) {
                    mutablePos.set(cx, y, cz);
                    if (checker.isSafeBlock(mutablePos.immutable(), belowAllowAir)) {
                        return toResult(mutablePos);
                    }
                }
            }
            return null;
        }

        // 按 3D曼哈顿距离 放射状迭代
        LOGGER.debug("TimeMillis before radial search: {}", System.currentTimeMillis());
        int rangeX = Math.max(Math.abs(chunkMaxX - cx), Math.abs(chunkMinX - cx));
        int rangeZ = Math.max(Math.abs(chunkMaxZ - cz), Math.abs(chunkMinZ - cz));
        int rangeY = Math.max(Math.abs(maxY - cy), Math.abs(minY - cy));
        int maxManhattan = rangeX + rangeZ + rangeY;
        for (int m = 0; m <= maxManhattan; m++) {
            for (int dx = -m; dx <= m; dx++) {
                int restDyDz = m - Math.abs(dx);
                if (restDyDz < 0) continue;
                for (int dy = -restDyDz; dy <= restDyDz; dy++) {
                    int dzAbs = restDyDz - Math.abs(dy);
                    if (dzAbs == 0) {
                        int x = cx + dx, y = cy + dy, z = cz;
                        if (x >= chunkMinX && x <= chunkMaxX && z >= chunkMinZ && z <= chunkMaxZ && y >= minY && y <= maxY) {
                            mutablePos.set(x, y, z);
                            if (checker.isSafeBlock(mutablePos.immutable(), belowAllowAir)) {
                                LOGGER.debug("TimeMillis after radial search: {}", System.currentTimeMillis());
                                return toResult(mutablePos);
                            }
                        }
                    } else {
                        for (int sign = 0; sign < 2; sign++) {
                            int dz = sign == 0 ? dzAbs : -dzAbs;
                            int x = cx + dx, y = cy + dy, z = cz + dz;
                            if (x >= chunkMinX && x <= chunkMaxX && z >= chunkMinZ && z <= chunkMaxZ && y >= minY && y <= maxY) {
                                mutablePos.set(x, y, z);
                                if (checker.isSafeBlock(mutablePos.immutable(), belowAllowAir)) {
                                    LOGGER.debug("TimeMillis after radial search: {}", System.currentTimeMillis());
                                    return toResult(mutablePos);
                                }
                            }
                        }
                    }
                }
            }
        }
        LOGGER.debug("TimeMillis after radial search: {}", System.currentTimeMillis());
        return null;
    }

    private SafeWorldCoordinate toResult(BlockPos pos) {
        SafeWorldCoordinate result = new SafeWorldCoordinate();
        result.fromBlockPos(pos).dimension(world.dimension()).addX(0.5).addY(0.15).addZ(0.5);
        return result;
    }

    /**
     * 在 BlockPos 列表中查找第一个安全坐标
     */
    @Nullable
    public SafeWorldCoordinate findFirstSafe(Iterable<BlockPos> positions, boolean belowAllowAir) {
        for (BlockPos pos : positions) {
            if (checker.isSafeBlock(pos, belowAllowAir)) {
                SafeWorldCoordinate result = new SafeWorldCoordinate();
                result.fromBlockPos(pos).dimension(world.dimension());
                return result;
            }
        }
        return null;
    }
}
