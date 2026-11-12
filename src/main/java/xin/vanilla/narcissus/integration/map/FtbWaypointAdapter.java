package xin.vanilla.narcissus.integration.map;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import xin.vanilla.banira.common.util.DimensionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

final class FtbWaypointAdapter implements MapWaypointSync.Adapter {
    private static final int MAX_ADDS_PER_PASS = 64;
    private final Map<Location, Owned> owned = new LinkedHashMap<>();
    private Method api, mapManager, dimension, dimensionManager, all, insert, remove, pos, transientPoint, name;
    private Method setTransient, setName, color, hidden;
    private Constructor<?> waypoint;
    private Object defaultType;

    private record Location(String dimension, BlockPos pos) {
    }

    private record Owned(Object manager, Object waypoint, String label, int color, boolean hidden) {
    }

    private void bind() throws ReflectiveOperationException {
        api = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI").getMethod("clientApi");
        Class<?> managers = Class.forName("dev.ftb.mods.ftbchunks.api.client.waypoint.WaypointManager");
        Class<?> points = Class.forName("dev.ftb.mods.ftbchunks.api.client.waypoint.Waypoint");
        Class<?> mapManagers = Class.forName("dev.ftb.mods.ftbchunks.client.map.MapManager");
        Class<?> dimensions = Class.forName("dev.ftb.mods.ftbchunks.client.map.MapDimension");
        Class<?> pointImpl = Class.forName("dev.ftb.mods.ftbchunks.client.map.WaypointImpl");
        Class<?> types = Class.forName("dev.ftb.mods.ftbchunks.client.map.WaypointType");
        mapManager = mapManagers.getMethod("getInstance");
        dimension = mapManagers.getMethod("getDimension", ResourceKey.class);
        dimensionManager = dimensions.getMethod("getWaypointManager");
        waypoint = pointImpl.getConstructor(types, dimensions, BlockPos.class);
        defaultType = types.getField("DEFAULT").get(null);
        insert = dimensionManager.getReturnType().getMethod("add", pointImpl);
        all = managers.getMethod("getAllWaypoints");
        remove = managers.getMethod("removeWaypoint", points);
        pos = points.getMethod("getPos");
        transientPoint = points.getMethod("isTransient");
        name = points.getMethod("getName");
        setTransient = points.getMethod("setTransient", boolean.class);
        setName = points.getMethod("setName", String.class);
        color = points.getMethod("getColor");
        hidden = points.getMethod("isHidden");
    }

    @Override
    public void reconcile(Map<MapWaypointSync.Key, MapWaypointSync.Point> desired) throws ReflectiveOperationException {
        if (desired.isEmpty()) {
            close();
            return;
        }
        if (api == null) bind();
        try {
            if (api.invoke(null) == null) return;
        } catch (InvocationTargetException error) {
            // Verified clientApi() only requires its initialized singleton; loading is retryable.
            if (error.getCause() instanceof NullPointerException) return;
            throw error;
        }
        Object activeMap = ((Optional<?>) mapManager.invoke(null)).orElse(null);
        if (activeMap == null) return;
        Map<Location, Map<String, String>> grouped = new LinkedHashMap<>();
        for (MapWaypointSync.Point point : desired.values()) {
            Location location = new Location(point.key().dimension(), new BlockPos(point.x(), point.y(), point.z()));
            grouped.computeIfAbsent(location, ignored -> new TreeMap<>()).put(point.key().id(), point.label());
        }
        Map<String, Object> managers = new LinkedHashMap<>();
        Map<String, Object> dimensions = new LinkedHashMap<>();
        for (Location location : grouped.keySet()) resolve(activeMap, managers, dimensions, location.dimension());
        for (Location location : owned.keySet()) resolve(activeMap, managers, dimensions, location.dimension());
        Map<String, Map<BlockPos, Object>> indexed = new LinkedHashMap<>();
        for (var entry : managers.entrySet()) {
            Map<BlockPos, Object> points = new LinkedHashMap<>();
            if (entry.getValue() != null) {
                for (Object point : (Collection<?>) all.invoke(entry.getValue()))
                    points.put((BlockPos) pos.invoke(point), point);
            }
            indexed.put(entry.getKey(), points);
        }
        var iterator = owned.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            Location location = entry.getKey();
            Owned handle = entry.getValue();
            Object active = managers.get(location.dimension());
            if (!grouped.containsKey(location) || !label(grouped.get(location)).equals(handle.label())
                    || (active != null && active != handle.manager())) {
                // Release the captured manager's object before forgetting it, even across a manager switch.
                removeOwned(location, handle);
                iterator.remove();
                if (active == handle.manager()) indexed.get(location.dimension()).remove(location.pos());
            } else if (active != null && (indexed.get(location.dimension()).get(location.pos()) != handle.waypoint()
                    || !unchanged(handle))) {
                iterator.remove();
            }
        }
        int additions = 0;
        for (var entry : grouped.entrySet()) {
            Location location = entry.getKey();
            Object active = managers.get(location.dimension());
            if (active == null || owned.containsKey(location)
                    || indexed.get(location.dimension()).containsKey(location.pos())) continue;
            if (additions++ >= MAX_ADDS_PER_PASS) break;
            if (at(active, location.pos()) != null) continue;
            Object mapDimension = dimensions.get(location.dimension());
            if (((Optional<?>) mapManager.invoke(null)).orElse(null) != activeMap
                    || dimensionManager.invoke(mapDimension) != active) return;
            String label = label(entry.getValue());
            Object created = waypoint.newInstance(defaultType, mapDimension, location.pos());
            setTransient.invoke(created, true);
            setName.invoke(created, label);
            // Hold the exact object before add(): FTB can throw after inserting it into the set.
            owned.put(location, new Owned(active, created, label, (int) color.invoke(created), (boolean) hidden.invoke(created)));
            insert.invoke(active, created);
            if (at(active, location.pos()) != created) owned.remove(location);
        }
    }

    private void resolve(Object activeMap, Map<String, Object> managers, Map<String, Object> dimensions,
                         String id) throws ReflectiveOperationException {
        if (!managers.containsKey(id)) {
            Object mapDimension = dimension.invoke(activeMap, DimensionUtils.parse(id));
            dimensions.put(id, mapDimension);
            managers.put(id, mapDimension == null ? null : dimensionManager.invoke(mapDimension));
        }
    }

    private static String label(Map<String, String> points) {
        String first = points.values().iterator().next();
        return points.size() == 1 ? first : first + " (+" + (points.size() - 1) + ")";
    }

    private Object at(Object manager, BlockPos position) throws ReflectiveOperationException {
        for (Object point : (Collection<?>) all.invoke(manager)) {
            if (position.equals(pos.invoke(point))) return point;
        }
        return null;
    }

    private boolean isOwned(Location location, Owned handle) throws ReflectiveOperationException {
        return at(handle.manager(), location.pos()) == handle.waypoint()
                && unchanged(handle);
    }

    private boolean unchanged(Owned handle) throws ReflectiveOperationException {
        return (boolean) transientPoint.invoke(handle.waypoint())
                && handle.label().equals(name.invoke(handle.waypoint()))
                && handle.color() == (int) color.invoke(handle.waypoint())
                && handle.hidden() == (boolean) hidden.invoke(handle.waypoint());
    }

    private void removeOwned(Location location, Owned handle) throws ReflectiveOperationException {
        if (isOwned(location, handle)) {
            remove.invoke(handle.manager(), handle.waypoint());
            if (at(handle.manager(), location.pos()) == handle.waypoint())
                throw new IllegalStateException("FTB did not remove owned waypoint");
        }
    }

    @Override
    public void close() throws ReflectiveOperationException {
        ReflectiveOperationException failure = null;
        var iterator = owned.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            try {
                // Only the captured manager/object is eligible; never resolve the current server here.
                removeOwned(entry.getKey(), entry.getValue());
                iterator.remove();
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                if (failure == null) failure = new ReflectiveOperationException("Could not clear owned FTB waypoints");
                failure.addSuppressed(error);
            }
        }
        if (failure != null) throw failure;
    }
}
