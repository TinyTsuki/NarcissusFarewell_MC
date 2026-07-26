package xin.vanilla.narcissus.integration;

import xin.vanilla.narcissus.network.packet.WaypointSyncToClient;

/**
 * 地图同步的稳定入口；具体地图模组由后续 Fabric adapter 按需接入。
 */
public final class MapHelper {
    private MapHelper() {
    }

    public static void handle(WaypointSyncToClient packet) {
        // 核心传送点同步不依赖第三方地图模组。
    }

    public static void addWaypoint(String name, String dimension, double x, double y, double z) {
    }

    public static void removeWaypoint(String name, String dimension, double x, double y, double z) {
    }
}
