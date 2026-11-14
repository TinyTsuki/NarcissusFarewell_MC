package xin.vanilla.narcissus.integration.map;

import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.Waypoint;
import dev.ftb.mods.ftbchunks.integration.RefreshMinimapIconsEvent;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.DimensionUtils;

import java.awt.*;


public final class FtbChunks {
    private static final Logger LOGGER = LogManager.getLogger();

    private FtbChunks() {
    }

    public static void addWaypoint(String name, String dimension, double x, double y, double z) {
        try {
            RegistryKey<World> dim = DimensionUtils.parse(dimension);
            MapDimension map = MapManager.inst.getDimension(dim);
            Waypoint waypoint = new Waypoint(map);
            waypoint.name = name;
            waypoint.color = Color.GREEN.getRGB();
            waypoint.x = (int) x;
            waypoint.y = (int) y;
            waypoint.z = (int) z;
            for (Waypoint existing : map.getWaypoints()) {
                if (existing.name.equals(name)
                        && existing.dimension.dimension.equals(dim)
                        && existing.x == (int) x && existing.y == (int) y && existing.z == (int) z) return;
            }
            map.getWaypoints().add(waypoint);
            WaypointOwnership.remember(waypoint);
            waypoint.update();
            map.saveData = true;
            RefreshMinimapIconsEvent.trigger();
        } catch (Exception e) {
            LOGGER.warn("Failed to add FtbChunks waypoint: {}", e.getMessage());
        }
    }

    public static void removeWaypoint(String name, String dimension, double x, double y, double z) {
        try {
            RegistryKey<World> dim = DimensionUtils.parse(dimension);
            MapDimension map = MapManager.inst.getDimension(dim);
            for (Waypoint waypoint : map.getWaypoints()) {
                if (WaypointOwnership.owns(waypoint) && waypoint.name.equals(name)
                        && waypoint.dimension.dimension.equals(dim)
                        && waypoint.x == (int) x && waypoint.y == (int) y && waypoint.z == (int) z
                ) {
                    map.getWaypoints().remove(waypoint);
                    WaypointOwnership.forget(waypoint);
                    break;
                }
            }
            map.saveData = true;
            RefreshMinimapIconsEvent.trigger();
        } catch (Exception e) {
            LOGGER.warn("Failed to remove FtbChunks waypoint: {}", e.getMessage());
        }

    }
}
