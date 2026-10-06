package xin.vanilla.narcissus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.search.SafeCandidateCursor;
import xin.vanilla.narcissus.search.SearchBox;
import xin.vanilla.narcissus.search.ViewSearchCursor;

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
    public SafeWorldCoordinate findViewEndCandidate(ServerPlayer player, boolean safe, int range) {
        final double stepScale = 0.75;
        final SafeWorldCoordinate start = new SafeWorldCoordinate(player);
        final Vec3 startPosition = player.getEyePosition(1.0F);
        final Vec3 stepVector = player.getViewVector(1.0F).normalize().scale(stepScale);
        ViewSearchCursor cursor = new ViewSearchCursor(startPosition.x, startPosition.y, startPosition.z,
                stepVector.x, stepVector.y, stepVector.z, range, safe);
        for (int advances = 0; ; advances++) {
            if ((advances & 63) == 0) {
                cursor.beginSlice();
                checker.beginSlice();
            }
            ViewSearchCursor.Step step = cursor.advance();
            if (step == ViewSearchCursor.Step.DONE) break;
            if (step == ViewSearchCursor.Step.MOTION || step == ViewSearchCursor.Step.SAFETY) {
                mutablePos.set(cursor.x(), cursor.y(), cursor.z());
                cursor.accept(step == ViewSearchCursor.Step.MOTION
                        ? world.getBlockState(mutablePos).blocksMotion()
                        : checker.isSafeBlock(mutablePos, false));
            }
        }
        SafeWorldCoordinate result = start.clone();
        result.x(cursor.resultX()).y(cursor.resultY()).z(cursor.resultZ());
        return start.equalsInRange(result, 1) ? null : result;
    }

    /**
     * 在区块范围内按安全模式搜索
     */
    @Nullable
    public SafeWorldCoordinate searchInChunk(SafeWorldCoordinate safeWorldCoordinate, int chunkX, int chunkZ, boolean belowAllowAir) {
        int offset = (CommonConfig.get().base().safeTeleport().safeChunkRange() - 1) * 16;
        int chunkMinX = (chunkX << 4) - offset;
        int chunkMinZ = (chunkZ << 4) - offset;
        int chunkMaxX = chunkMinX + 15 + offset;
        int chunkMaxZ = chunkMinZ + 15 + offset;
        int minY = getWorldMinY();
        int maxY = getWorldMaxY();
        int cx = safeWorldCoordinate.xInt();
        int cy = safeWorldCoordinate.yInt();
        int cz = safeWorldCoordinate.zInt();

        SafeCandidateCursor cursor = new SafeCandidateCursor(safeWorldCoordinate.safeMode(), cx, cy, cz,
                new SearchBox(chunkMinX, chunkMaxX, minY, maxY, chunkMinZ, chunkMaxZ));
        SafeCandidateCursor.Step step;
        int advances = 0;
        while ((step = cursor.advance()) != SafeCandidateCursor.Step.DONE) {
            if ((advances++ & 63) == 0) checker.beginSlice();
            if (step == SafeCandidateCursor.Step.CANDIDATE) {
                mutablePos.set(cursor.x(), cursor.y(), cursor.z());
                if (checker.isSafeBlock(mutablePos.immutable(), belowAllowAir)) return toResult(mutablePos);
            }
        }
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
