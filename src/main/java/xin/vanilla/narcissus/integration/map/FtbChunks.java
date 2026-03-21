package xin.vanilla.narcissus.integration.map;

import dev.ftb.mods.ftbchunks.client.map.MapDimension;
import dev.ftb.mods.ftbchunks.client.map.MapManager;
import dev.ftb.mods.ftbchunks.client.map.Waypoint;
import dev.ftb.mods.ftbchunks.integration.RefreshMinimapIconsEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.enums.EnumMCColor;
import xin.vanilla.banira.common.util.DimensionUtils;


public final class FtbChunks {
    private static final Logger LOGGER = LogManager.getLogger();

    private FtbChunks() {
    }

    public static void addWaypoint(String name, String dimension, double x, double y, double z) {
        try {
            ResourceKey<Level> dim = DimensionUtils.parse(dimension);
            MapDimension map = MapManager.inst.getDimension(dim);
            Waypoint waypoint = new Waypoint(map, (int) x, (int) y, (int) z);
            waypoint.name = name;
            waypoint.color = EnumMCColor.GREEN.getColor();
            map.getWaypointManager().add(waypoint);
        } catch (Exception e) {
            LOGGER.warn("Failed to add FtbChunks waypoint: {}", e.getMessage());
        }
    }

    public static void removeWaypoint(String name, String dimension, double x, double y, double z) {
        try {
            ResourceKey<Level> dim = DimensionUtils.parse(dimension);
            MapDimension map = MapManager.inst.getDimension(dim);
            for (Waypoint waypoint : map.getWaypointManager()) {
                if (waypoint.name.equals(name)
                        && waypoint.dimension.dimension.equals(dim)
                        && waypoint.x == (int) x && waypoint.y == (int) y && waypoint.z == (int) z
                ) {
                    map.getWaypointManager().remove(waypoint);
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
