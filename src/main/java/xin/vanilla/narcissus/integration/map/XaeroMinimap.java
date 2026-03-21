package xin.vanilla.narcissus.integration.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xaero.common.HudMod;
import xaero.common.minimap.waypoints.Waypoint;
import xaero.hud.minimap.BuiltInHudModules;
import xaero.hud.minimap.module.MinimapSession;
import xaero.hud.minimap.waypoint.WaypointColor;
import xaero.hud.minimap.waypoint.set.WaypointSet;
import xaero.hud.minimap.world.MinimapWorld;
import xaero.hud.path.XaeroPath;
import xin.vanilla.banira.common.util.DimensionUtils;


public final class XaeroMinimap {
    private static final Logger LOGGER = LogManager.getLogger();

    private XaeroMinimap() {
    }

    private static String firstChar(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        int end = s.offsetByCodePoints(0, 1);
        return s.substring(0, end);
    }

    public static void addWaypoint(String name, String dimension, double x, double y, double z) {
        try {
            ResourceKey<Level> dimKey = DimensionUtils.parse(dimension);
            Waypoint waypoint = new Waypoint((int) x, (int) y, (int) z, name, firstChar(name), WaypointColor.GREEN);

            MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();

            XaeroPath root = session.getWorldState().getAutoWorldPath().getRoot();
            XaeroPath resolved = root.resolve(session.getDimensionHelper().getDimensionDirectoryName(dimKey))
                    .resolve(session.getWorldStateUpdater().getPotentialWorldNode(dimKey, HudMod.INSTANCE != null && HudMod.INSTANCE.getSupportMods().worldmap()));
            MinimapWorld world = session.getWorldManager().getWorld(resolved);

            xaero.hud.minimap.waypoint.set.WaypointSet waypointSet = world.getCurrentWaypointSet();
            waypointSet.add(waypoint);
            session.getWorldManagerIO().saveWorld(world);
        } catch (Exception e) {
            LOGGER.warn("Failed to add Xaero waypoint: {}", e.getMessage());
        }
    }

    public static void removeWaypoint(String name, String dimension, double x, double y, double z) {
        try {
            ResourceKey<Level> dimKey = DimensionUtils.parse(dimension);
            Waypoint waypoint = new Waypoint((int) x, (int) y, (int) z, name, firstChar(name), WaypointColor.GREEN);

            MinimapSession session = BuiltInHudModules.MINIMAP.getCurrentSession();

            XaeroPath root = session.getWorldState().getAutoWorldPath().getRoot();
            XaeroPath resolved = root.resolve(session.getDimensionHelper().getDimensionDirectoryName(dimKey))
                    .resolve(session.getWorldStateUpdater().getPotentialWorldNode(dimKey, HudMod.INSTANCE != null && HudMod.INSTANCE.getSupportMods().worldmap()));
            MinimapWorld world = session.getWorldManager().getWorld(resolved);

            WaypointSet waypointSet = world.getCurrentWaypointSet();
            for (Waypoint w : waypointSet.getWaypoints()) {
                if (w.compareTo(waypoint) == 0) {
                    waypointSet.remove(w);
                    break;
                }
            }
            session.getWorldManagerIO().saveWorld(world);
        } catch (Exception e) {
            LOGGER.warn("Failed to remove Xaero waypoint: {}", e.getMessage());
        }

    }
}
