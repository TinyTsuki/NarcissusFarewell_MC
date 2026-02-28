package xin.vanilla.narcissus.integration.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xaero.common.XaeroMinimapSession;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.common.minimap.waypoints.WaypointSet;
import xaero.common.minimap.waypoints.WaypointWorld;
import xaero.common.minimap.waypoints.WaypointsManager;
import xaero.common.settings.ModOptions;
import xin.vanilla.narcissus.enums.EnumMCColor;
import xin.vanilla.narcissus.util.DimensionUtils;
import xin.vanilla.narcissus.util.StringUtils;


public final class XaeroMinimap {
    private static final Logger LOGGER = LogManager.getLogger();

    private XaeroMinimap() {
    }

    public static void addWaypoint(String name, String dimension, double x, double y, double z) {
        try {
            ResourceKey<Level> dimKey = DimensionUtils.parse(dimension);
            Waypoint waypoint = new Waypoint((int) x, (int) y, (int) z, name, StringUtils.firstChar(name), EnumMCColor.GREEN.getColor());

            XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
            WaypointsManager waypointsManager = session.getWaypointsManager();
            String mainContainerId = waypointsManager.getAutoRootContainerID();
            String containerId = mainContainerId + "/" + waypointsManager.getDimensionDirectoryName(dimKey);
            String worldId = waypointsManager.getNewAutoWorldID(dimKey, ModOptions.modMain != null && ModOptions.modMain.getSupportMods().worldmap());

            WaypointWorld world = session.getWaypointsManager().getWorld(containerId, worldId);

            WaypointSet waypointSet = world.getCurrentSet();
            waypointSet.getList().add(waypoint);
        } catch (Exception e) {
            LOGGER.warn("Failed to add Xaero waypoint: {}", e.getMessage());
        }
    }

    public static void removeWaypoint(String name, String dimension, double x, double y, double z) {
        try {
            ResourceKey<Level> dimKey = DimensionUtils.parse(dimension);
            Waypoint waypoint = new Waypoint((int) x, (int) y, (int) z, name, StringUtils.firstChar(name), EnumMCColor.GREEN.getColor());

            XaeroMinimapSession session = XaeroMinimapSession.getCurrentSession();
            WaypointsManager waypointsManager = session.getWaypointsManager();
            String mainContainerId = waypointsManager.getAutoRootContainerID();
            String containerId = mainContainerId + "/" + waypointsManager.getDimensionDirectoryName(dimKey);
            String worldId = waypointsManager.getNewAutoWorldID(dimKey, ModOptions.modMain != null && ModOptions.modMain.getSupportMods().worldmap());

            WaypointWorld world = session.getWaypointsManager().getWorld(containerId, worldId);

            WaypointSet waypointSet = world.getCurrentSet();
            for (Waypoint w : waypointSet.getList()) {
                if (w.compareTo(waypoint) == 0) {
                    waypointSet.getList().remove(w);
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to remove Xaero waypoint: {}", e.getMessage());
        }

    }
}
