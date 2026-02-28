package xin.vanilla.narcissus.integration;

import net.minecraftforge.fml.ModList;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.integration.map.FtbChunks;
import xin.vanilla.narcissus.integration.map.JourneyMap;
import xin.vanilla.narcissus.integration.map.XaeroMinimap;
import xin.vanilla.narcissus.network.packet.WaypointSyncToClient;

public final class MapHelper {
    private MapHelper() {
    }

    public static void handle(final WaypointSyncToClient packet) {
        switch (packet.type()) {
            case HOME:
                if (!ClientConfig.SYNC_HOME_MAP_WAYPOINT.get()) return;
                break;
            case STAGE:
                if (!ClientConfig.SYNC_STAGE_MAP_WAYPOINT.get()) return;
                break;
            default:
                return;
        }
        switch (packet.action()) {
            case ADD:
                addWaypoint(packet.name(), packet.dimension(), packet.x(), packet.y(), packet.z());
                break;
            case REMOVE:
                removeWaypoint(packet.name(), packet.dimension(), packet.x(), packet.y(), packet.z());
                break;
        }
    }

    public static void addWaypoint(final String name, final String dimension, final double x, final double y, final double z) {
        if (ModList.get().isLoaded("xaerominimap")) {
            XaeroMinimap.addWaypoint(name, dimension, x, y, z);
        }
        if (ModList.get().isLoaded("journeymap")) {
            JourneyMap.addWaypoint(name, dimension, x, y, z);
        }
        if (ModList.get().isLoaded("ftbchunks")) {
            FtbChunks.addWaypoint(name, dimension, x, y, z);
        }
    }

    public static void removeWaypoint(final String name, final String dimension, final double x, final double y, final double z) {
        if (ModList.get().isLoaded("xaerominimap")) {
            XaeroMinimap.removeWaypoint(name, dimension, x, y, z);
        }
        if (ModList.get().isLoaded("journeymap")) {
            JourneyMap.removeWaypoint(name, dimension, x, y, z);
        }
        if (ModList.get().isLoaded("ftbchunks")) {
            FtbChunks.removeWaypoint(name, dimension, x, y, z);
        }
    }

}
