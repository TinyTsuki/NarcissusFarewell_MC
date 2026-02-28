package xin.vanilla.narcissus.integration.map;

import journeymap.client.waypoint.Waypoint;
import journeymap.client.waypoint.WaypointStore;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.Arrays;


public final class JourneyMap {
    private static final Logger LOGGER = LogManager.getLogger();

    private JourneyMap() {
    }

    public static void addWaypoint(String name, String dimension, double x, double y, double z) {
        try {
            Waypoint waypoint = new Waypoint(name, (int) x, (int) y, (int) z, true
                    , Color.GREEN.getRed(), Color.GREEN.getGreen(), Color.GREEN.getBlue()
                    , Waypoint.Type.Normal, "journeymap"
                    , dimension, Arrays.asList(dimension), true
            );
            WaypointStore.INSTANCE.save(waypoint, true);
        } catch (Exception e) {
            LOGGER.warn("Failed to add JourneyMap waypoint: {}", e.getMessage());
        }
    }

    public static void removeWaypoint(String name, String dimension, double x, double y, double z) {
        try {
            for (Waypoint waypoint : WaypointStore.INSTANCE.getAll()) {
                if (waypoint.getName().equals(name)
                        && waypoint.getDimensions().stream().allMatch(d -> d.equals(dimension)
                        && waypoint.getX() == (int) x && waypoint.getY() == (int) y && waypoint.getZ() == (int) z)
                ) {
                    WaypointStore.INSTANCE.remove(waypoint, true);
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to remove JourneyMap waypoint: {}", e.getMessage());
        }

    }
}
