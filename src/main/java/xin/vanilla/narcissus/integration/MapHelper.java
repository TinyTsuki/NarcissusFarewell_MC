package xin.vanilla.narcissus.integration;

import net.fabricmc.loader.api.FabricLoader;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.integration.map.JourneyMap;
import xin.vanilla.narcissus.integration.map.MapWaypointSync;
import xin.vanilla.narcissus.network.packet.WaypointSyncToClient;

/**
 * 可选客户端地图同步入口。
 */
public final class MapHelper {
    private MapHelper() {
    }

    public static void handle(WaypointSyncToClient packet) {
        MapWaypointSync.increment(packet);
        boolean enabled = packet.type() == WaypointSyncToClient.Type.HOME
                ? ClientConfig.get().client().syncHomeMapWaypoint()
                : ClientConfig.get().client().syncStageMapWaypoint();
        if (!enabled || !FabricLoader.getInstance().isModLoaded("journeymap")) return;
        if (packet.action() == WaypointSyncToClient.Action.ADD)
            JourneyMap.addWaypoint(packet.type().name(), packet.name(), packet.dimension(), packet.x(), packet.y(), packet.z());
        else
            JourneyMap.removeWaypoint(packet.type().name(), packet.name(), packet.dimension(), packet.x(), packet.y(), packet.z());
    }

    public static void addWaypoint(String name, String dimension, double x, double y, double z) {
        if (FabricLoader.getInstance().isModLoaded("journeymap"))
            JourneyMap.addWaypoint("WAYPOINT", name, dimension, x, y, z);
    }

    public static void removeWaypoint(String name, String dimension, double x, double y, double z) {
        if (FabricLoader.getInstance().isModLoaded("journeymap"))
            JourneyMap.removeWaypoint("WAYPOINT", name, dimension, x, y, z);
    }

    public static void startSession() {
        JourneyMap.startSession();
        MapWaypointSync.startSession();
    }

    public static void endSession() {
        MapWaypointSync.endSession();
        JourneyMap.endSession();
    }

    public static void tick() {
        MapWaypointSync.tick();
        JourneyMap.tick();
    }
}
