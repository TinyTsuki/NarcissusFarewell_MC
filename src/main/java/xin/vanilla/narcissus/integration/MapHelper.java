package xin.vanilla.narcissus.integration;

import net.minecraftforge.fml.ModList;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.integration.map.FtbChunks;
import xin.vanilla.narcissus.integration.map.JourneyMap;
import xin.vanilla.narcissus.integration.map.XaeroMinimap;
import xin.vanilla.narcissus.network.packet.WaypointSyncToClient;

public final class MapHelper {
    private static final java.util.Set<String> DISABLED = new java.util.HashSet<>();

    private static void run(String backend, Runnable action) {
        if (DISABLED.contains(backend)) return;
        try {
            action.run();
        } catch (RuntimeException | LinkageError unavailable) {
            DISABLED.add(backend);
            org.apache.logging.log4j.LogManager.getLogger().warn("Waypoint integration disabled: " + backend, unavailable);
        }
    }

    private MapHelper() {
    }

    public static void handle(final WaypointSyncToClient packet) {
        switch (packet.type()) {
            case HOME:
                if (!ClientConfig.get().client().syncHomeMapWaypoint()) return;
                break;
            case STAGE:
                if (!ClientConfig.get().client().syncStageMapWaypoint()) return;
                break;
            default:
                return;
        }
        switch (packet.action()) {
            case ADD:
                addWaypoint(packet.type().name(), packet.name(), packet.dimension(), packet.x(), packet.y(), packet.z());
                break;
            case REMOVE:
                removeWaypoint(packet.type().name(), packet.name(), packet.dimension(), packet.x(), packet.y(), packet.z());
                break;
        }
    }

    public static void addWaypoint(final String name, final String dimension, final double x, final double y, final double z) {
        addWaypoint("WAYPOINT", name, dimension, x, y, z);
    }

    private static void addWaypoint(final String kind, final String name, final String dimension, final double x, final double y, final double z) {
        if (ModList.get().isLoaded("xaerominimap")) {
            run("XaeroMinimap", () -> XaeroMinimap.addWaypoint(name, dimension, x, y, z));
        }
        if (ModList.get().isLoaded("journeymap")) {
            run("JourneyMap", () -> JourneyMap.addWaypoint(kind, name, dimension, x, y, z));
        }
        if (ModList.get().isLoaded("ftbchunks")) {
            run("FtbChunks", () -> FtbChunks.addWaypoint(name, dimension, x, y, z));
        }
    }

    public static void removeWaypoint(final String name, final String dimension, final double x, final double y, final double z) {
        removeWaypoint("WAYPOINT", name, dimension, x, y, z);
    }

    private static void removeWaypoint(final String kind, final String name, final String dimension, final double x, final double y, final double z) {
        if (ModList.get().isLoaded("xaerominimap")) {
            run("XaeroMinimap", () -> XaeroMinimap.removeWaypoint(name, dimension, x, y, z));
        }
        if (ModList.get().isLoaded("journeymap")) {
            run("JourneyMap", () -> JourneyMap.removeWaypoint(kind, name, dimension, x, y, z));
        }
        if (ModList.get().isLoaded("ftbchunks")) {
            run("FtbChunks", () -> FtbChunks.removeWaypoint(name, dimension, x, y, z));
        }
    }

}
