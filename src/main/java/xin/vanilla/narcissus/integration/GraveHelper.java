package xin.vanilla.narcissus.integration;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.NarcissusUtils;
import xin.vanilla.narcissus.util.SafeBlockChecker;

import javax.annotation.Nullable;

/** Fabric 基线的死亡记录查询；外部墓碑模组需要各自的 Fabric adapter。 */
public final class GraveHelper {
    private GraveHelper() {
    }

    @Nullable
    public static SafeWorldCoordinate parseObituaryFromHeldItem(ServerPlayer player) {
        return null;
    }

    @Nullable
    public static TeleportRecord findLastDeathRecord(ServerPlayer player, @Nullable ResourceKey<Level> dimension) {
        return NarcissusUtils.getBackTeleportRecord(player, EnumTeleportType.DEATH, dimension);
    }

    @Nullable
    public static SafeWorldCoordinate findCorpseGravestoneNear(
            SafeWorldCoordinate center, int radius, java.util.UUID playerUuid) {
        return null;
    }

    @Nullable
    public static SafeWorldCoordinate findCorpseGravestoneNearPlayer(ServerPlayer player, int range) {
        int limit = CommonConfig.get().general().graveSearchRangeLimit();
        int radius = Math.min(Math.max(range, 1), limit);
        return findCorpseGravestoneNear(new SafeWorldCoordinate(player), radius, player.getUUID());
    }

    @Nullable
    public static SafeWorldCoordinate findCorpseGravestoneNearDeath(ServerPlayer player, TeleportRecord deathRecord) {
        return findCorpseGravestoneNear(deathRecord.getBefore(), 64, player.getUUID());
    }

    public static boolean isCoordinateSafe(SafeWorldCoordinate coordinate) {
        ServerLevel world = DimensionUtils.getLevel(coordinate.dimension());
        if (world == null) {
            return false;
        }
        BlockPos position = coordinate.toBlockPos();
        return new SafeBlockChecker(world).isSafeBlock(position, false);
    }
}
