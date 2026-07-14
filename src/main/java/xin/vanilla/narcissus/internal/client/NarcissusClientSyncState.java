package xin.vanilla.narcissus.internal.client;

/**
 * 记录客户端完整玩家数据同步次数，供界面刷新和开发烟测判断数据是否就绪。
 */
public final class NarcissusClientSyncState {
    private static volatile long playerDataGeneration;

    private NarcissusClientSyncState() {
    }

    public static void markPlayerDataReceived() {
        playerDataGeneration++;
    }

    public static long playerDataGeneration() {
        return playerDataGeneration;
    }
}
