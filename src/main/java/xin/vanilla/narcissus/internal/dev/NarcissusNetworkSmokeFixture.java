package xin.vanilla.narcissus.internal.dev;

import xin.vanilla.narcissus.data.PlayerAccess;

/** 两个独立游戏进程共享的固定测试值，不依赖客户端类。 */
public final class NarcissusNetworkSmokeFixture {
    public static final int COUNTDOWN = 37;
    public static final int TELEPORT_RECORD_LIMIT = 113;
    public static final String ACCESS_UUID = "00000000-0000-0000-0000-000000000002";

    private NarcissusNetworkSmokeFixture() {
    }

    public static void verifyAccess(PlayerAccess access) {
        if (!access.getWhiteList().contains(ACCESS_UUID)
                || !access.getAutoTpaList().contains(ACCESS_UUID)) {
            throw new IllegalStateException("Access-list sentinel is missing");
        }
    }
}
