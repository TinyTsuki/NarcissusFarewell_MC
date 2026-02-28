package xin.vanilla.narcissus.integration.map;

import journeymap.api.common.waypoint.WaypointFactoryImpl;
import journeymap.client.waypoint.ClientWaypointImpl;
import journeymap.common.waypoint.WaypointStore;
import net.minecraft.core.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.enums.EnumMCColor;


public final class JourneyMap {
    private static final Logger LOGGER = LogManager.getLogger();

    private JourneyMap() {
    }

    public static void addWaypoint(String name, String dimension, double x, double y, double z) {
        try {
            ClientWaypointImpl waypoint = WaypointFactoryImpl.at(new BlockPos((int) x, (int) y, (int) z), false, dimension);
            waypoint.setName(name);
            waypoint.setPersistent(true);
            waypoint.setEnabled(true);
            waypoint.setShowDeviation(true);
            waypoint.setColor(EnumMCColor.GREEN.getColor());
            WaypointStore.getInstance().save(waypoint, true);
        } catch (Exception e) {
            LOGGER.warn("Failed to add JourneyMap waypoint: {}", e.getMessage());
        }
    }

    public static void removeWaypoint(String name, String dimension, double x, double y, double z) {
        try {
            for (ClientWaypointImpl waypoint : WaypointStore.getInstance().getAll()) {
                if (waypoint.getName().equals(name)
                        && waypoint.getDimensions().stream().allMatch(d -> d.equals(dimension)
                        && waypoint.getX() == (int) x && waypoint.getY() == (int) y && waypoint.getZ() == (int) z)
                ) {
                    WaypointStore.getInstance().remove(waypoint, true);
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to remove JourneyMap waypoint: {}", e.getMessage());
        }

    }
}
