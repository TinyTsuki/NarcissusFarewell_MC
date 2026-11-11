package xin.vanilla.narcissus.integration.map;

import net.minecraft.core.BlockPos;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

final class LegacyJourneyMapAdapter implements JourneyMap.Adapter {
    private final Field instance;
    private final Constructor<?> constructor;
    private final Constructor<?> apiConstructor;
    private final Method ready, all, save, remove, origin, displayId, name, rename, dimensions, x, y, z, color, deviation;

    static JourneyMap.Adapter bind(ClassLoader loader) throws ReflectiveOperationException {
        try {
            return new LegacyJourneyMapAdapter(loader);
        } catch (ClassNotFoundException absent) {
            return null;
        }
    }

    private LegacyJourneyMapAdapter(ClassLoader loader) throws ReflectiveOperationException {
        Class<?> waypoint = Class.forName("journeymap.client.waypoint.Waypoint", false, loader);
        Class<?> store = Class.forName("journeymap.client.waypoint.WaypointStore", false, loader);
        Class<?> apiWaypoint = Class.forName("journeymap.client.api.display.Waypoint", false, loader);
        instance = store.getField("INSTANCE");
        if (!Modifier.isStatic(instance.getModifiers()) || instance.getType() != store)
            throw new NoSuchFieldException("WaypointStore.INSTANCE");
        apiConstructor = apiWaypoint.getConstructor(String.class, String.class, String.class, String.class, BlockPos.class);
        constructor = waypoint.getConstructor(apiWaypoint);
        ready = method(store, "hasLoaded", boolean.class);
        all = method(store, "getAll", Collection.class);
        save = method(store, "save", void.class, waypoint, boolean.class);
        remove = method(store, "remove", void.class, waypoint, boolean.class);
        origin = method(waypoint, "getOrigin", String.class);
        displayId = method(waypoint, "getDisplayId", String.class);
        name = method(waypoint, "getName", String.class);
        rename = method(waypoint, "setName", waypoint, String.class);
        dimensions = method(waypoint, "getDimensions", Collection.class);
        x = method(waypoint, "getRawCenterX", double.class);
        y = method(waypoint, "getY", int.class);
        z = method(waypoint, "getRawCenterZ", double.class);
        color = method(waypoint, "setColor", waypoint, Integer.class);
        deviation = method(waypoint, "setShowDeviation", waypoint, Boolean.class);
    }

    static Method method(Class<?> type, String name, Class<?> result, Class<?>... parameters) throws NoSuchMethodException {
        Method method = type.getMethod(name, parameters);
        if (Modifier.isStatic(method.getModifiers()) || method.getReturnType() != result)
            throw new NoSuchMethodException(type.getName() + "." + name);
        return method;
    }

    private static Object call(Method method, Object target, Object... args) throws ReflectiveOperationException {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException error) {
            // Reflection must not hide VM failures behind an optional integration error.
            if (error.getCause() instanceof Error fatal && !(fatal instanceof LinkageError)) throw fatal;
            throw error;
        }
    }

    @Override
    public boolean ready() throws ReflectiveOperationException {
        return (boolean) call(ready, instance.get(null));
    }

    @Override
    public void add(JourneyMap.Point point) throws ReflectiveOperationException {
        for (Object waypoint : waypoints()) if (matches(waypoint, point)) return;
        Object waypoint;
        try {
            // JM5 derives its disk ID during construction; set the visible name only after both IDs are unique.
            String id = point.kind() + "_" + UUID.randomUUID();
            Object apiPoint = apiConstructor.newInstance(JourneyMap.MOD_ID, id,
                    id, point.dimension(), new BlockPos(point.x(), point.y(), point.z()));
            waypoint = constructor.newInstance(apiPoint);
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof Error fatal && !(fatal instanceof LinkageError)) throw fatal;
            throw error;
        }
        call(rename, waypoint, point.name());
        call(color, waypoint, Integer.valueOf(0x00FF00));
        call(deviation, waypoint, Boolean.TRUE);
        call(save, instance.get(null), waypoint, true);
    }

    @Override
    public void remove(JourneyMap.Point point) throws ReflectiveOperationException {
        for (Object waypoint : waypoints()) {
            if (matches(waypoint, point)) call(remove, instance.get(null), waypoint, true);
        }
    }

    private Collection<?> waypoints() throws ReflectiveOperationException {
        return new ArrayList<>((Collection<?>) call(all, instance.get(null)));
    }

    private boolean matches(Object waypoint, JourneyMap.Point point) throws ReflectiveOperationException {
        Object id = call(displayId, waypoint);
        int scale = "minecraft:the_nether".equals(point.dimension()) ? 8 : 1;
        return id instanceof String && ((String) id).startsWith(point.kind() + "_")
                && LegacyJourneyMapFiles.fileName(JourneyMap.MOD_ID, (String) id) != null
                && JourneyMap.matches((String) call(origin, waypoint), (String) call(name, waypoint), strings(call(dimensions, waypoint)),
                JourneyMap.block((double) call(x, waypoint)), (int) call(y, waypoint), JourneyMap.block((double) call(z, waypoint)),
                point.name(), point.dimension(), Math.multiplyExact(point.x(), scale), point.y(), Math.multiplyExact(point.z(), scale));
    }

    private static Collection<String> strings(Object value) {
        if (value == null) return Collections.emptyList();
        Collection<String> result = new ArrayList<>();
        for (Object dimension : (Collection<?>) value) result.add((String) dimension);
        return result;
    }
}
