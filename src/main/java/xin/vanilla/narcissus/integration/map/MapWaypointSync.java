package xin.vanilla.narcissus.integration.map;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.network.packet.WaypointSyncToClient;

import java.util.ArrayList;
import java.util.Collections;

import lombok.Value;
import lombok.experimental.Accessors;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client-thread projection of server-owned data; map objects never become source data.
 */
public final class MapWaypointSync {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_POINTS = 4096;
    private static final Map<Key, Point> EXPECTED = new LinkedHashMap<>();
    private static final List<Backend> BACKENDS = new ArrayList<>();
    // At most one failed backend per adapter type; never retarget or retry it in another session.
    private static final List<Backend> QUARANTINED = new ArrayList<>();
    private static Object connection;
    private static UUID playerId;
    private static int ticks;
    private static boolean rejected;

    private MapWaypointSync() {
    }

    @Value
    @Accessors(fluent = true)
    static class Key {
        UUID player;
        WaypointSyncToClient.Type kind;
        String dimension;
        String name;

        String id() {
            return player + ":" + kind.name() + ":" + dimension.length() + ":" + dimension + name.length() + ":" + name;
        }
    }

    @Value
    @Accessors(fluent = true)
    static class Point {
        Key key;
        int x;
        int y;
        int z;

        String label() {
            return "[" + key.kind().name() + "] " + key.name();
        }
    }

    interface Adapter {
        void reconcile(Map<Key, Point> desired) throws ReflectiveOperationException;

        void close() throws ReflectiveOperationException;
    }

    private static final class Backend {
        private final Adapter adapter;
        private boolean failed;
        private boolean cleaned;
        private boolean quarantined;

        private Backend(Adapter adapter) {
            this.adapter = adapter;
        }

        private void run(Map<Key, Point> desired, boolean closing) {
            if (closing) {
                cleanup();
                return;
            }
            if (failed) return;
            try {
                adapter.reconcile(desired);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                LOGGER.warn("{} waypoint integration unavailable for this connection",
                        adapter.getClass().getSimpleName(), error);
                failed = true;
                cleanup();
            }
        }

        private void cleanup() {
            if (cleaned || quarantined) return;
            Throwable lastFailure = null;
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    adapter.close();
                    cleaned = true;
                    return;
                } catch (ReflectiveOperationException | RuntimeException | LinkageError error) {
                    lastFailure = error;
                }
            }
            failed = true;
            quarantined = true;
            QUARANTINED.add(this);
            LOGGER.error("{} cleanup exhausted; retained old handles and disabled this bridge until restart",
                    adapter.getClass().getSimpleName(), lastFailure);
        }
    }

    private static void register(Adapter adapter) {
        if (QUARANTINED.stream().noneMatch(backend -> backend.adapter.getClass() == adapter.getClass()))
            BACKENDS.add(new Backend(adapter));
    }

    public static void startSession() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null || client.player == null) return;
        Object current = client.getConnection().getConnection();
        UUID currentPlayer = client.player.getUUID();
        if (connection == current && currentPlayer.equals(playerId)) return;
        endSession();
        connection = current;
        playerId = currentPlayer;
        if (FabricLoader.getInstance().isModLoaded("xaerominimap"))
            register(new XaeroWaypointAdapter());
    }

    public static boolean accepts(Object transport) {
        Minecraft client = Minecraft.getInstance();
        if (transport == null || client.getConnection() == null || client.player == null
                || transport != client.getConnection().getConnection()) return false;
        startSession();
        return transport == connection;
    }

    public static void endSession() {
        for (Backend backend : BACKENDS) backend.run(Collections.emptyMap(), true);
        BACKENDS.clear();
        EXPECTED.clear();
        connection = null;
        playerId = null;
        ticks = 0;
        rejected = false;
    }

    public static void snapshot(WaypointSyncToClient.Type kind,
                                Map<KeyValue<String, String>, SafeWorldCoordinate> snapshot) {
        if (connection == null) return;
        // Validate the entire replacement before removing anything, including for an empty snapshot.
        Map<Key, Point> replacement = new LinkedHashMap<>();
        try {
            if (snapshot.size() > MAX_POINTS) throw new IllegalArgumentException("Waypoint snapshot too large");
            for (Map.Entry<KeyValue<String, String>, SafeWorldCoordinate> entry : snapshot.entrySet()) {
                SafeWorldCoordinate value = entry.getValue();
                if (!entry.getKey().key().equals(value.dimensionId()))
                    throw new IllegalArgumentException("Waypoint dimension mismatch");
                Point point = point(kind, entry.getKey().value(), value.dimensionId(), value.x(), value.y(), value.z());
                replacement.put(point.key(), point);
            }
            long otherCount = EXPECTED.keySet().stream().filter(key -> key.kind() != kind).count();
            if (otherCount + replacement.size() > MAX_POINTS)
                throw new IllegalArgumentException("Too many map waypoints");
        } catch (RuntimeException error) {
            reject(error);
            return;
        }
        EXPECTED.keySet().removeIf(key -> key.kind() == kind);
        EXPECTED.putAll(replacement);
        ticks = 0;
    }

    public static void increment(WaypointSyncToClient packet) {
        if (connection == null) return;
        try {
            Point point = point(packet.type(), packet.name(), packet.dimension(), packet.x(), packet.y(), packet.z());
            if (packet.action() == WaypointSyncToClient.Action.REMOVE) {
                EXPECTED.remove(point.key(), point);
            } else {
                if (!EXPECTED.containsKey(point.key()) && EXPECTED.size() >= MAX_POINTS)
                    throw new IllegalArgumentException("Too many map waypoints");
                EXPECTED.put(point.key(), point);
            }
            ticks = 0;
        } catch (RuntimeException error) {
            reject(error);
        }
    }

    private static Point point(WaypointSyncToClient.Type kind, String name, String dimension,
                               double x, double y, double z) {
        if (kind == null || name == null || name.length() > 64 || dimension == null
                || dimension.length() > 256 || ResourceLocation.tryParse(dimension) == null)
            throw new IllegalArgumentException("Invalid map waypoint");
        return new Point(new Key(playerId, kind, dimension, name), block(x), block(y), block(z));
    }

    private static int block(double value) {
        double floor = Math.floor(value);
        if (!Double.isFinite(floor) || floor < Integer.MIN_VALUE || floor > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid waypoint coordinate");
        return (int) floor;
    }

    private static void reject(RuntimeException error) {
        if (!rejected) LOGGER.warn("Rejected map waypoint update; retaining previous projection", error);
        rejected = true;
    }

    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null || client.player == null) {
            if (connection != null) endSession();
            return;
        }
        startSession();
        if (ticks++ % 10 != 0) return;
        Map<Key, Point> desired = new LinkedHashMap<>();
        boolean homes = ClientConfig.get().client().syncHomeMapWaypoint();
        boolean stages = ClientConfig.get().client().syncStageMapWaypoint();
        EXPECTED.forEach((key, point) -> {
            if (key.kind() == WaypointSyncToClient.Type.HOME ? homes : stages) desired.put(key, point);
        });
        for (Backend backend : BACKENDS) backend.run(desired, false);
    }
}
