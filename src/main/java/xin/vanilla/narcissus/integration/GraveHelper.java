package xin.vanilla.narcissus.integration;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.ModList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.integration.grave.CorailTombstoneIntegration;
import xin.vanilla.narcissus.integration.grave.CorpseGraveIntegration;
import xin.vanilla.narcissus.integration.grave.GravestoneModGraveIntegration;
import xin.vanilla.narcissus.integration.grave.SimpleTombGraveIntegration;
import xin.vanilla.narcissus.util.NarcissusUtils;
import xin.vanilla.narcissus.util.SafeBlockChecker;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 查找遗体/墓碑坐标
 */
public final class GraveHelper {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int DEFAULT_SEARCH_RADIUS = 64;

    private GraveHelper() {
    }

    /**
     * 解析玩家手持的讣告
     *
     * @return 讣告中的坐标
     */
    @Nullable
    public static SafeWorldCoordinate parseObituaryFromHeldItem(ServerPlayerEntity player) {
        SafeWorldCoordinate result = null;
        if (ModList.get().isLoaded("corpse")) {
            result = CorpseGraveIntegration.parseObituary(player);
        }
        if (ModList.get().isLoaded("gravestone")) {
            result = GravestoneModGraveIntegration.parseObituary(player);
        }
        if (ModList.get().isLoaded("simpletomb")) {
            result = SimpleTombGraveIntegration.parseObituary(player);
        }
        if (ModList.get().isLoaded("tombstone")) {
            result = CorailTombstoneIntegration.parseObituary(player);
        }
        return result;
    }

    /**
     * 获取玩家最近一次死亡记录
     */
    @Nullable
    public static TeleportRecord findLastDeathRecord(ServerPlayerEntity player, @Nullable RegistryKey<World> dimension) {
        return NarcissusUtils.getBackTeleportRecord(player, EnumTeleportType.DEATH, dimension);
    }

    /**
     * 在指定坐标附近搜索遗体/墓碑
     */
    @Nullable
    public static SafeWorldCoordinate findCorpseGravestoneNear(SafeWorldCoordinate center, int radius, UUID playerUuid) {
        List<SafeWorldCoordinate> candidates = new ArrayList<>();
        double cx = center.x(), cy = center.y(), cz = center.z();

        if (ModList.get().isLoaded("corpse")) {
            CorpseGraveIntegration.findNear(center, radius, playerUuid, candidates);
        }
        if (ModList.get().isLoaded("gravestone")) {
            GravestoneModGraveIntegration.findNear(center, radius, playerUuid, candidates);
        }
        if (ModList.get().isLoaded("simpletomb")) {
            SimpleTombGraveIntegration.findNear(center, radius, playerUuid, candidates);
        }
        if (ModList.get().isLoaded("tombstone")) {
            CorailTombstoneIntegration.findNear(center, radius, playerUuid, candidates);
        }

        if (candidates.isEmpty()) return null;
        return candidates.stream()
                .min(Comparator.comparingDouble(c -> c.distanceFrom(cx, cy, cz)))
                .orElse(null);
    }

    /**
     * 在玩家附近搜索遗体/墓碑
     */
    @Nullable
    public static SafeWorldCoordinate findCorpseGravestoneNearPlayer(ServerPlayerEntity player, int range) {
        int limit = CommonConfig.get().server().general().graveSearchRangeLimit();
        int r = Math.min(Math.max(range, 1), limit);
        SafeWorldCoordinate center = new SafeWorldCoordinate(player);
        return findCorpseGravestoneNear(center, r, player.getUUID());
    }

    /**
     * 在死亡记录附近搜索遗体/墓碑
     */
    @Nullable
    public static SafeWorldCoordinate findCorpseGravestoneNearDeath(ServerPlayerEntity player, TeleportRecord deathRecord) {
        SafeWorldCoordinate before = deathRecord.getBefore();
        return findCorpseGravestoneNear(before, DEFAULT_SEARCH_RADIUS, player.getUUID());
    }

    /**
     * 检查坐标是否安全可站立
     */
    public static boolean isCoordinateSafe(SafeWorldCoordinate coord) {
        ServerWorld world = DimensionUtils.getLevel(coord.dimension());
        if (world == null) return false;
        SafeBlockChecker checker = new SafeBlockChecker(world);
        BlockPos pos = coord.toBlockPos();
        return checker.isSafeBlock(pos, false);
    }
}
