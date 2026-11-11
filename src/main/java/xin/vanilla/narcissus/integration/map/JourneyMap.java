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
    private static boolean connected;
    private static boolean disabled;

    private JourneyMap() {
    }

    interface Adapter {
        boolean ready() throws ReflectiveOperationException;

        void add(Point point) throws ReflectiveOperationException;

        void remove(Point point) throws ReflectiveOperationException;
    }

    static final class Point {
        private final String kind, name, dimension;
        private final int x, y, z;

        Point(String kind, String name, String dimension, int x, int y, int z) {
            this.kind = kind;
            this.name = name;
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        String kind() {
            return kind;
        }

        String name() {
            return name;
        }

        String dimension() {
            return dimension;
        }

        int x() {
            return x;
        }

        int y() {
            return y;
        }

        int z() {
            return z;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Point)) return false;
            Point point = (Point) other;
            return x == point.x && y == point.y && z == point.z
                    && java.util.Objects.equals(kind, point.kind)
                    && java.util.Objects.equals(name, point.name)
                    && java.util.Objects.equals(dimension, point.dimension);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(kind, name, dimension, x, y, z);
        }
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
    }

    public static void startSession() {
        PENDING.clear();
        connected = true;
        disabled = false;
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
        if (!connected || disabled || adapter == null || PENDING.isEmpty()) return;
        try {
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
