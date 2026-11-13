package xin.vanilla.narcissus.integration.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/**
 * Read-only event projection; never joins FTB's persistent waypoint manager.
 */
public final class FtbEventWaypointAdapter implements MapWaypointSync.Adapter {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<Object, Boolean> ICONS = Collections.synchronizedMap(new WeakHashMap<>());
    private static FtbEventWaypointAdapter active;
    private static boolean minimapRegistered, largeMapRegistered;
    private final Map<MapWaypointSync.Key, Owned> owned = new LinkedHashMap<>();
    private Map<MapWaypointSync.Key, MapWaypointSync.Point> desired = Collections.emptyMap();
    private Constructor<?> constructor;
    private Field name, color;
    private Method dimension, add, update, icon, manager, mapDimension;
    private Object defaultType;
    private boolean failed;

    private static final class Owned {
        final MapWaypointSync.Point point;
        final Object dimension, icon;

        Owned(MapWaypointSync.Point point, Object dimension, Object icon) {
            this.point = point;
            this.dimension = dimension;
            this.icon = icon;
        }
    }

    static boolean owns(Object icon) {
        return ICONS.containsKey(icon);
    }

    static boolean visible(Object icon) {
        return Boolean.TRUE.equals(ICONS.get(icon));
    }

    private void bind() throws ReflectiveOperationException {
        Class<?> event = Class.forName("dev.ftb.mods.ftbchunks.api.client.event.MapIconEvent");
        Class<?> point = Class.forName("dev.ftb.mods.ftbchunks.client.map.WaypointImpl");
        Class<?> dimensions = Class.forName("dev.ftb.mods.ftbchunks.client.map.MapDimension");
        Class<?> managers = Class.forName("dev.ftb.mods.ftbchunks.client.map.MapManager");
        Class<?> types = Class.forName("dev.ftb.mods.ftbchunks.client.map.WaypointType");
        manager = managers.getMethod("getInstance");
        mapDimension = managers.getMethod("getDimension", ResourceKey.class);
        defaultType = types.getField("DEFAULT").get(null);
        dimension = event.getMethod("getDimension");
        add = event.getMethod("add", Class.forName("dev.ftb.mods.ftbchunks.api.client.icon.MapIcon"));
        constructor = point.getConstructor(types, dimensions, BlockPos.class);
        // Public setters dirty the dimension; these fields belong only to our unpublished object.
        name = point.getDeclaredField("name");
        color = point.getDeclaredField("color");
        name.setAccessible(true);
        color.setAccessible(true);
        icon = point.getMethod("getMapIcon");
        update = point.getMethod("refreshIcon");
        if (!minimapRegistered) {
            register(event.getField("MINIMAP"));
            minimapRegistered = true;
        }
        if (!largeMapRegistered) {
            register(event.getField("LARGE_MAP"));
            largeMapRegistered = true;
        }
    }

    private static void register(Field event) throws ReflectiveOperationException {
        Consumer<Object> listener = value -> {
            FtbEventWaypointAdapter current = active;
            if (current != null) current.render(value);
        };
        event.getType().getMethod("register", Object.class).invoke(event.get(null), listener);
    }

    @Override
    public void reconcile(Map<MapWaypointSync.Key, MapWaypointSync.Point> points) throws ReflectiveOperationException {
        if (failed) return;
        if (points.isEmpty()) {
            close();
            return;
        }
        if (constructor == null || !minimapRegistered || !largeMapRegistered) bind();
        if (active != null && active != this) active.close();
        desired = new LinkedHashMap<>(points);
        owned.entrySet().removeIf(entry -> {
            if (entry.getValue().point.equals(desired.get(entry.getKey()))) return false;
            ICONS.put(entry.getValue().icon, false);
            return true;
        });
        active = this;
    }

    private void render(Object event) {
        try {
            ResourceKey<?> key = (ResourceKey<?>) dimension.invoke(event);
            Object map = ((java.util.Optional<?>) manager.invoke(null)).orElse(null);
            if (map == null || key == null) return;
            Object target = mapDimension.invoke(map, key);
            if (target == null || key == null) return;
            String id = key.location().toString();
            int additions = 0;
            for (MapWaypointSync.Point point : desired.values()) {
                if (!point.key().dimension().equals(id)) continue;
                Owned current = owned.get(point.key());
                if (current == null || current.dimension != target) {
                    if (additions++ >= 64) continue;
                    if (current != null) ICONS.put(current.icon, false);
                    Object waypoint = constructor.newInstance(defaultType, target, new BlockPos(point.x(), point.y(), point.z()));
                    name.set(waypoint, point.label());
                    color.setInt(waypoint, 0xF3A8C2);
                    update.invoke(waypoint);
                    Object projection = icon.invoke(waypoint);
                    if (!(projection instanceof ReadOnlyFtbIcon))
                        throw new IllegalStateException("FTB read-only hooks unavailable");
                    current = new Owned(point, target, projection);
                    ICONS.put(projection, true);
                    owned.put(point.key(), current);
                }
                add.invoke(event, current.icon);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            failed = true;
            close();
            LOGGER.warn("FTB waypoint event projection disabled for this connection", error);
        }
    }

    @Override
    public void close() {
        for (Owned point : owned.values()) ICONS.put(point.icon, false);
        owned.clear();
        desired = Collections.emptyMap();
        if (active == this) active = null;
    }

    public static boolean isOwnedIcon(Object icon) {
        return owns(icon);
    }

    public static boolean isActiveIcon(Object icon) {
        return visible(icon);
    }
}

