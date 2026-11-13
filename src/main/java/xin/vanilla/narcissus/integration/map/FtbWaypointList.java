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
        if (!ReadOnlyFtbRow.class.isAssignableFrom(Class.forName("dev.ftb.mods.ftbchunks.client.gui.WaypointEditorScreen$RowPanel")))
            throw new IllegalStateException("FTB waypoint list read-only hooks unavailable");
    }

    private static void warn(Throwable failure) {
        if (!warned) {
            warned = true;
            LogManager.getLogger().warn("FTB waypoint list projection unavailable; original entries retained", failure);
        }
    }

    public static Map<?, ?> append(Map<?, ?> original) {
        try {
            requireReadOnlyRows();
            Map<Object, List<Object>> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : original.entrySet())
                result.put(entry.getKey(), new ArrayList<>((List<?>) entry.getValue()));
            for (Map.Entry<ResourceKey<?>, List<Object>> entry : FtbEventWaypointAdapter.editorWaypoints().entrySet()) {
                List<Object> rows = result.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>());
                for (Object waypoint : entry.getValue())
                    if (rows.stream().noneMatch(existing -> existing == waypoint)) rows.add(waypoint);
            }
            return result;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            warn(failure);
            return original;
        }
    }
}
