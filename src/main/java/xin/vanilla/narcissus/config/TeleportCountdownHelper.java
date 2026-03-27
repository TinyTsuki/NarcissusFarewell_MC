package xin.vanilla.narcissus.config;

import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumTeleportType;


public final class TeleportCountdownHelper {

    /**
     * 与 common 配置中倒计时相关 BoundedDiscrete 上限一致
     */
    public static final int ABSOLUTE_MAX_SEC = 86400;

    private TeleportCountdownHelper() {
    }

    public static int playerRangeLo() {
        int a = CommonConfig.get().teleportCountdown().playerCountdownRangeMin();
        int b = CommonConfig.get().teleportCountdown().playerCountdownRangeMax();
        return Math.min(Math.min(a, b), ABSOLUTE_MAX_SEC);
    }

    public static int playerRangeHi() {
        int a = CommonConfig.get().teleportCountdown().playerCountdownRangeMin();
        int b = CommonConfig.get().teleportCountdown().playerCountdownRangeMax();
        return Math.min(Math.max(a, b), ABSOLUTE_MAX_SEC);
    }

    /**
     * 将玩家偏好秒数夹到当前配置的允许区间 [playerRangeLo, playerRangeHi]。
     */
    public static int clampToPlayerAllowedRange(int seconds) {
        int lo = playerRangeLo();
        int hi = playerRangeHi();
        return Math.min(hi, Math.max(lo, Math.min(ABSOLUTE_MAX_SEC, Math.max(0, seconds))));
    }

    public static int serverCountdownForType(EnumTeleportType type) {
        if (type == null) {
            return 0;
        }
        CommonConfig.ServerPerTypeTeleportCountdownView s = CommonConfig.get().teleportCountdown().server();
        switch (type) {
            case TP_COORDINATE:
                return s.serverCountdownTpCoordinate();
            case TP_STRUCTURE:
                return s.serverCountdownTpStructure();
            case TP_ASK:
                return s.serverCountdownTpAsk();
            case TP_HERE:
                return s.serverCountdownTpHere();
            case TP_RANDOM:
                return s.serverCountdownTpRandom();
            case TP_SPAWN:
                return s.serverCountdownTpSpawn();
            case TP_WORLD_SPAWN:
                return s.serverCountdownTpWorldSpawn();
            case TP_TOP:
                return s.serverCountdownTpTop();
            case TP_BOTTOM:
                return s.serverCountdownTpBottom();
            case TP_UP:
                return s.serverCountdownTpUp();
            case TP_DOWN:
                return s.serverCountdownTpDown();
            case TP_VIEW:
                return s.serverCountdownTpView();
            case TP_HOME:
                return s.serverCountdownTpHome();
            case TP_STAGE:
                return s.serverCountdownTpStage();
            case TP_BACK:
                return s.serverCountdownTpBack();
            case TP_GRAVE:
                return s.serverCountdownTpGrave();
            default:
                return 0;
        }
    }

    /**
     * 传送前实际等待秒数
     */
    public static int getEffectiveCountdownSeconds(ServerPlayerEntity player, EnumTeleportType type) {
        int server = Math.min(ABSOLUTE_MAX_SEC, Math.max(0, serverCountdownForType(type)));
        if (CommonConfig.get().teleportCountdown().forceServerCountdown()) {
            return server;
        }
        int stored = PlayerTeleportData.getData(player).getTeleportCountdownSeconds(type);
        return Math.max(stored, server);
    }
}
