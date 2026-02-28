package xin.vanilla.narcissus.integration.map;

// import dev.ftb.mods.ftbchunks.client.map.MapDimension;
// import dev.ftb.mods.ftbchunks.client.map.MapManager;
// import dev.ftb.mods.ftbchunks.client.map.WaypointImpl;
// import dev.ftb.mods.ftbchunks.client.map.WaypointType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public final class FtbChunks {
    private static final Logger LOGGER = LogManager.getLogger();

    private FtbChunks() {
    }

    public static void addWaypoint(String name, String dimension, double x, double y, double z) {
        // try {
        //     ResourceKey<Level> dim = DimensionUtils.parse(dimension);
        //     MapDimension map = MapManager.getInstance().get().getDimension(dim);
        //     WaypointImpl waypoint = new WaypointImpl(WaypointType.DEFAULT, map, new BlockPos((int) x, (int) y, (int) z));
        //     waypoint.setName(name);
        //     waypoint.setColor(EnumMCColor.GREEN.getColor());
        //     map.getWaypointManager().add(waypoint);
        // } catch (Exception e) {
        //     LOGGER.warn("Failed to add FtbChunks waypoint: {}", e.getMessage());
        // }
    }

    public static void removeWaypoint(String name, String dimension, double x, double y, double z) {
        // try {
        //     ResourceKey<Level> dim = DimensionUtils.parse(dimension);
        //     MapDimension map = MapManager.getInstance().get().getDimension(dim);
        //     for (WaypointImpl waypoint : map.getWaypointManager()) {
        //         if (waypoint.getName().equals(name)
        //                 && waypoint.getDimension().equals(dim)
        //                 && waypoint.getPos().getX() == (int) x && waypoint.getPos().getY() == (int) y && waypoint.getPos().getZ() == (int) z
        //         ) {
        //             map.getWaypointManager().remove(waypoint);
        //             break;
        //         }
        //     }
        // } catch (Exception e) {
        //     LOGGER.warn("Failed to remove FtbChunks waypoint: {}", e.getMessage());
        // }
    }
}
