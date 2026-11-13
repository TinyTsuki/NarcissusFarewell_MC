package xin.vanilla.narcissus.integration.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FtbWaypointList {
    private static boolean warned;

    private FtbWaypointList() {
    }

    public static boolean isOwnedRow(Object row) {
        try {
            Field field = row.getClass().getDeclaredField("wp");
            field.setAccessible(true);
            return FtbEventWaypointAdapter.isOwnedWaypoint(field.get(row));
        } catch (ReflectiveOperationException failure) {
            return false;
        }
    }

    private static void requireReadOnlyRows() throws ClassNotFoundException {
        if (!ReadOnlyFtbRow.class.isAssignableFrom(Class.forName("dev.ftb.mods.ftbchunks.client.WaypointEditorScreen$RowPanel")))
            throw new IllegalStateException("FTB waypoint list read-only hooks unavailable");
    }

    private static void warn(Throwable failure) {
        if (!warned) {
            warned = true;
            LogManager.getLogger().warn("FTB waypoint list projection unavailable; original entries retained", failure);
        }
    }

    public static List<?> append(List<?> original) {
        try {
            requireReadOnlyRows();
            Class<?> group = Class.forName("dev.ftb.mods.ftbchunks.client.WaypointEditorScreen$DimWayPoints");
            Constructor<?> constructor = group.getDeclaredConstructor(ResourceLocation.class, List.class);
            Method levelKey = group.getDeclaredMethod("levelKey"), waypoints = group.getDeclaredMethod("waypoints");
            constructor.setAccessible(true);
            levelKey.setAccessible(true);
            waypoints.setAccessible(true);
            Map<ResourceLocation, List<Object>> groups = new LinkedHashMap<>();
            for (Object row : original)
                groups.computeIfAbsent((ResourceLocation) levelKey.invoke(row), ignored -> new ArrayList<>())
                        .addAll((List<?>) waypoints.invoke(row));
            for (Map.Entry<ResourceKey<?>, List<Object>> entry : FtbEventWaypointAdapter.editorWaypoints().entrySet()) {
                List<Object> rows = groups.computeIfAbsent(entry.getKey().location(), ignored -> new ArrayList<>());
                for (Object waypoint : entry.getValue())
                    if (rows.stream().noneMatch(existing -> existing == waypoint)) rows.add(waypoint);
            }
            List<Object> result = new ArrayList<>();
            for (Map.Entry<ResourceLocation, List<Object>> entry : groups.entrySet())
                result.add(constructor.newInstance(entry.getKey(), entry.getValue()));
            return result;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            warn(failure);
            return original;
        }
    }
}
