package xin.vanilla.narcissus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.enums.EnumSafeMode;

import javax.annotation.Nullable;


public class SafeCoordinateFinder {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Level world;
    private final SafeBlockChecker checker;
    private final BlockPos.MutableBlockPos mutablePos;

    public SafeCoordinateFinder(Level world, Entity entity) {
        this.world = world;
        this.checker = new SafeBlockChecker(world, entity);
        this.mutablePos = new BlockPos.MutableBlockPos();
    }

    public int getWorldMinY() {
        return 0;
    }

    public int getWorldMaxY() {
        return world.dimensionType().minY() + world.dimensionType().height();
    }

    /**
     * 从当前位置向上查找（从高到低）
     */
    @Nullable
    public Coordinate findTopCandidate(Coordinate start) {
        if (start.y() >= getWorldMaxY()) return null;
        int x = start.getXInt();
        int z = start.getZInt();
        for (int y = getWorldMaxY(); y > start.getYInt(); y--) {
            mutablePos.set(x, y, z);
            if (checker.isSafeBlock(mutablePos.immutable(), false)) {
                return start.clone().y(y);
            }
        }
        return null;
    }

    /**
     * 从底部到当前位置查找（从低到高）
     */
    @Nullable
    public Coordinate findBottomCandidate(Coordinate start) {
        if (start.y() <= getWorldMinY()) return null;
        int x = start.getXInt();
        int z = start.getZInt();
        for (int y = getWorldMinY(); y < start.getYInt(); y++) {
            mutablePos.set(x, y, z);
            if (checker.isSafeBlock(mutablePos.immutable(), false)) {
                return start.clone().y(y);
            }
        }
        return null;
    }

    /**
     * 从当前位置向上查找（从低到高）
     */
    @Nullable
    public Coordinate findUpCandidate(Coordinate start) {
        if (start.y() >= getWorldMaxY()) return null;
        int x = start.getXInt();
        int z = start.getZInt();
        for (int y = start.getYInt() + 1; y <= getWorldMaxY(); y++) {
            mutablePos.set(x, y, z);
            if (checker.isSafeBlock(mutablePos.immutable(), false)) {
                return start.clone().y(y);
            }
        }
        return null;
    }

    /**
     * 从当前位置向下查找（从高到低）
     */
    @Nullable
    public Coordinate findDownCandidate(Coordinate start) {
        if (start.y() <= getWorldMinY()) return null;
        int x = start.getXInt();
        int z = start.getZInt();
        for (int y = start.getYInt() - 1; y >= getWorldMinY(); y--) {
            mutablePos.set(x, y, z);
            if (checker.isSafeBlock(mutablePos.immutable(), false)) {
                return start.clone().y(y);
            }
        }
        return null;
    }

    /**
     * 沿玩家视线方向射线检测，返回碰撞点或射线终点；
     * 若 safe 则反向查找安全站立位置
     */
    @Nullable
    public Coordinate findViewEndCandidate(ServerPlayer player, boolean safe, int range) {
        LOGGER.debug("TimeMillis before findViewEndCandidate: {}", System.currentTimeMillis());
        final double stepScale = 0.75;
        final Coordinate start = new Coordinate(player);
        Coordinate result;

        final Vec3 startPosition = player.getEyePosition(1.0F);
        final Vec3 stepVector = player.getViewVector(1.0F).normalize().scale(stepScale);
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
                    Math.floor(startX + stepX * stepCount),
                    Math.floor(startY + stepY * stepCount),
                    Math.floor(startZ + stepZ * stepCount)
            );
            if (world.getBlockState(mutablePos).blocksMotion()) {
                collisionStep = stepCount;
                break;
            }
        }

        // 确定碰撞点或射线终点
        if (collisionStep > 0) {
            result = start.clone().x(startX + stepX * (collisionStep - 1))
                    .y(startY + stepY * (collisionStep - 1))
                    .z(startZ + stepZ * (collisionStep - 1));
        } else if (collisionStep == 0) {
            result = start.clone().fromVec3(startPosition);
        } else {
            result = start.clone().x(startX + stepX * range)
                    .y(startY + stepY * range)
                    .z(startZ + stepZ * range);
        }

        // 若需寻找安全坐标，则从碰撞点反向查找安全位置
        if (safe && result != null) {
            final double dist = Math.sqrt(
                    Math.pow(result.x() - startX, 2) + Math.pow(result.y() - startY, 2) + Math.pow(result.z() - startZ, 2)
            );
            final int maxStep = (int) Math.ceil(dist / stepScale);
            final int[] yOffsets = {0, -1, 1, -2, 2, -3};
            boolean found = false;
            for (int stepCount = maxStep; stepCount >= 0 && !found; stepCount--) {
                final int blockX = (int) Math.floor(startX + stepX * stepCount);
                final int blockY = (int) Math.floor(startY + stepY * stepCount);
                final int blockZ = (int) Math.floor(startZ + stepZ * stepCount);
                for (int yOffset : yOffsets) {
                    mutablePos.set(blockX, blockY + yOffset, blockZ);
                    if (checker.isSafeBlock(mutablePos.immutable(), false)) {
                        result = start.clone().fromBlockPos(mutablePos).addX(0.5).addY(0.15).addZ(0.5);
                        found = true;
                        break;
                    }
                }
            }
        }
        if (result != null && start.equalsOfRange(result, 1)) {
            result = null;
        }
        LOGGER.debug("TimeMillis after findViewEndCandidate: {}", System.currentTimeMillis());
        return result;
    }

    /**
     * 在区块范围内按安全模式搜索，使用螺旋迭代避免全量排序
     */
    @Nullable
    public Coordinate searchInChunk(Coordinate coordinate, int chunkX, int chunkZ, boolean belowAllowAir) {
        int offset = (ServerConfig.SAFE_CHUNK_RANGE.get() - 1) * 16;
        int chunkMinX = (chunkX << 4) - offset;
        int chunkMinZ = (chunkZ << 4) - offset;
        int chunkMaxX = chunkMinX + 15 + offset;
        int chunkMaxZ = chunkMinZ + 15 + offset;
        int minY = getWorldMinY();
        int maxY = getWorldMaxY();
        int cx = coordinate.getXInt();
        int cy = coordinate.getYInt();
        int cz = coordinate.getZInt();

        EnumSafeMode mode = coordinate.safeMode();

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

    private Coordinate toResult(BlockPos pos) {
        return new Coordinate().fromBlockPos(pos).dimension(world.dimension()).addX(0.5).addY(0.15).addZ(0.5);
    }

    /**
     * 在 BlockPos 列表中查找第一个安全坐标
     */
    @Nullable
    public Coordinate findFirstSafe(Iterable<BlockPos> positions, boolean belowAllowAir) {
        for (BlockPos pos : positions) {
            if (checker.isSafeBlock(pos, belowAllowAir)) {
                return new Coordinate().fromBlockPos(pos).dimension(world.dimension());
            }
        }
        return null;
    }
}
