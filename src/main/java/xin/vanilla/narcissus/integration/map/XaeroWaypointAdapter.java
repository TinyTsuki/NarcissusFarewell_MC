package xin.vanilla.narcissus.integration.map;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

final class XaeroWaypointAdapter implements MapWaypointSync.Adapter {
    private static final int MAX_ADDS_PER_PASS = 64;
    private final Map<String, Owned> owned = new LinkedHashMap<>();
    private Object collection;
    private Method currentSession, processor, session, worlds, currentWorld, dimension, container;
    private Method thirdParty, origin, get, add, clear, projection;
    private Constructor<?> waypoint;

    private record Owned(MapWaypointSync.Point point, Object waypoint) {
    }

    private void bind() throws ReflectiveOperationException {
        currentSession = Class.forName("xaero.common.XaeroMinimapSession").getMethod("getCurrentSession");
        processor = currentSession.getReturnType().getMethod("getMinimapProcessor");
        session = processor.getReturnType().getMethod("getSession");
        worlds = session.getReturnType().getMethod("getWorldManager");
        currentWorld = worlds.getReturnType().getMethod("getCurrentWorld");
        dimension = currentWorld.getReturnType().getMethod("getDimId");
        container = currentWorld.getReturnType().getMethod("getContainer");
        thirdParty = container.getReturnType().getMethod("getThirdPartyWaypointManager");
        origin = thirdParty.getReturnType().getMethod("get", ResourceLocation.class);
        Class<?> pointClass = Class.forName("xaero.common.minimap.waypoints.Waypoint");
        waypoint = pointClass.getConstructor(int.class, int.class, int.class, String.class, String.class, int.class);
        get = origin.getReturnType().getMethod("get", String.class);
        add = origin.getReturnType().getMethod("add", String.class, pointClass);
        clear = origin.getReturnType().getMethod("clear");
        projection = origin.getReturnType().getMethod("getWaypoints");
    }

    @Override
    public void reconcile(Map<MapWaypointSync.Key, MapWaypointSync.Point> desired) throws ReflectiveOperationException {
        if (desired.isEmpty()) {
            close();
            return;
        }
        if (currentSession == null) bind();
        Object hud = currentSession.invoke(null);
        Object minimap = hud == null ? null : processor.invoke(hud);
        if (minimap == null) return;
        Object minimapSession = session.invoke(minimap);
        if (minimapSession == null) return;
        Object worldManager = worlds.invoke(minimapSession);
        if (worldManager == null) return;
        Object world = currentWorld.invoke(worldManager);
        if (world == null) return;
        ResourceKey<?> dimensionKey = (ResourceKey<?>) dimension.invoke(world);
        if (dimensionKey == null) return;
        String dim = dimensionKey.location().toString();
        Object worldContainer = container.invoke(world);
        if (worldContainer == null) return;
        Object thirdPartyManager = thirdParty.invoke(worldContainer);
        if (thirdPartyManager == null) return;
        Object target = origin.invoke(thirdPartyManager,
                ResourceLocation.fromNamespaceAndPath("narcissus_farewell", "waypoints"));
        if (target == null) return;
        if (target != collection) {
            close();
            collection = target;
        }
        requireExclusiveProjection();
        boolean rebuild = false;
        for (Owned handle : owned.values()) {
            MapWaypointSync.Point expected = desired.get(handle.point().key());
            if (!handle.point().equals(expected) || !dim.equals(handle.point().key().dimension())) {
                rebuild = true;
                break;
            }
        }
        if (rebuild) clearProjection();
        int additions = 0;
        for (MapWaypointSync.Point point : desired.values()) {
            if (!dim.equals(point.key().dimension())) continue;
            String id = point.key().id();
            Object existing = get.invoke(collection, id);
            Owned handle = owned.get(id);
            if (existing != null) {
                // Even within our namespace, never adopt or replace an object we did not insert.
                if (handle == null || existing != handle.waypoint()) owned.remove(id);
                continue;
            }
            if (additions++ >= MAX_ADDS_PER_PASS) break;
            Object created = waypoint.newInstance(point.x(), point.y(), point.z(), point.label(), "N", 10);
            owned.put(id, new Owned(point, created));
            add.invoke(collection, id, created);
            if (get.invoke(collection, id) != created) owned.remove(id);
        }
    }

    private void requireExclusiveProjection() throws ReflectiveOperationException {
        for (var entry : ((Map<?, ?>) projection.invoke(collection)).entrySet()) {
            Owned handle = owned.get(entry.getKey());
            if (handle == null || entry.getValue() != handle.waypoint())
                throw new IllegalStateException("Foreign Xaero object in owned origin; leaving collection untouched");
        }
    }

    private void clearProjection() throws ReflectiveOperationException {
        requireExclusiveProjection();
        // Xaero 26.6 clear() retains user render overrides; remove(id) deletes and saves them.
        clear.invoke(collection);
        if (!((Map<?, ?>) projection.invoke(collection)).isEmpty())
            throw new IllegalStateException("Xaero did not clear owned projection");
        owned.clear();
    }

    @Override
    public void close() throws ReflectiveOperationException {
        if (collection != null && !owned.isEmpty()) clearProjection();
        owned.clear();
        collection = null;
    }
}
