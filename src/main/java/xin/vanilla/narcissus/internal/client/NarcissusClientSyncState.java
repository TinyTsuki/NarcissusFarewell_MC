package xin.vanilla.narcissus.internal.client;

/**
 * 记录客户端完整玩家数据同步次数，供界面刷新和开发烟测判断数据是否就绪。
 */
public final class NarcissusClientSyncState {
    private static volatile long playerDataGeneration;
    private static volatile long waypointDataGeneration;

    private NarcissusClientSyncState() {
    }

    public static void markPlayerDataReceived() {
        playerDataGeneration++;
        waypointDataGeneration++;
    }

    /** 公共驿站同步只刷新地标视图，不伪装成完整玩家数据同步。 */
    public static void markStageDataReceived() {
        waypointDataGeneration++;
    }

    public static long playerDataGeneration() {
        return playerDataGeneration;
    }

    public static long waypointDataGeneration() {
        return waypointDataGeneration;
    }
}
