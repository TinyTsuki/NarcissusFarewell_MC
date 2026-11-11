package xin.vanilla.narcissus.integration.map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;


public final class JourneyMap {
    static final String MOD_ID = "narcissus_farewell";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_PENDING = 256;
    private static final Map<Point, Boolean> PENDING = new LinkedHashMap<>();
    private static Adapter adapter;
    private static boolean checked;
    private static boolean disabled;
    private static boolean connected;

    private JourneyMap() {
    }

    interface Adapter {
        boolean ready() throws ReflectiveOperationException;

        void add(Point point) throws ReflectiveOperationException;

        void remove(Point point) throws ReflectiveOperationException;
    }

    record Point(String kind, String name, String dimension, int x, int y, int z) {
    }

    static int block(double value) {
        return (int) Math.floor(value);
    }

    static boolean matches(String owner, String name, Collection<String> dimensions, int x, int y, int z,
                           String expectedName, String dimension, int expectedX, int expectedY, int expectedZ) {
        return MOD_ID.equals(owner) && expectedName.equals(name) && dimensions != null
                && dimensions.contains(dimension) && x == expectedX && y == expectedY && z == expectedZ;
    }

    static void install(Adapter value) {
        adapter = value;
        checked = true;
        disabled = false;
    }

    public static void startSession() {
        PENDING.clear();
        connected = true;
        disabled = false;
        if (adapter == null) checked = false;
    }

    public static void endSession() {
        PENDING.clear();
        connected = false;
        disabled = false;
    }

    public static void addWaypoint(String name, String dimension, double x, double y, double z) {
        addWaypoint("WAYPOINT", name, dimension, x, y, z);
    }

    public static void removeWaypoint(String name, String dimension, double x, double y, double z) {
        removeWaypoint("WAYPOINT", name, dimension, x, y, z);
    }

    public static void addWaypoint(String kind, String name, String dimension, double x, double y, double z) {
        enqueue(new Point(kind, name, dimension, block(x), block(y), block(z)), true);
    }

    public static void removeWaypoint(String kind, String name, String dimension, double x, double y, double z) {
        enqueue(new Point(kind, name, dimension, block(x), block(y), block(z)), false);
    }

    private static void enqueue(Point point, boolean add) {
        if (!connected || disabled) return;
        if (!PENDING.containsKey(point) && PENDING.size() >= MAX_PENDING) {
            fail(new IllegalStateException("JourneyMap waypoint queue full"));
            return;
        }
        PENDING.put(point, add);
        tick();
    }

    public static void tick() {
        if (!connected || disabled || PENDING.isEmpty()) return;
        try {
            if (!checked) {
                checked = true;
                adapter = LegacyJourneyMapAdapter.bind(JourneyMap.class.getClassLoader());
            }
            if (adapter == null) {
                fail(new IllegalStateException("No supported JourneyMap waypoint contract"));
                return;
            }
            if (!adapter.ready()) return;
            for (Map.Entry<Point, Boolean> entry : PENDING.entrySet()) {
                if (entry.getValue()) adapter.add(entry.getKey());
                else adapter.remove(entry.getKey());
            }
            PENDING.clear();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
            fail(error);
        }
    }

    static void fail(Throwable error) {
        PENDING.clear();
        if (!disabled) LOGGER.warn("JourneyMap waypoint integration unavailable for this connection", error);
        disabled = true;
    }
}
