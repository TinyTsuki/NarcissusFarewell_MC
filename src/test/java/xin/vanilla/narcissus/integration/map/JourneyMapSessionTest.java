package xin.vanilla.narcissus.integration.map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class JourneyMapSessionTest {
    private final RecordingAdapter adapter = new RecordingAdapter();

    @Before
    public void connect() {
        JourneyMap.install(adapter);
        JourneyMap.startSession();
    }

    @After
    public void disconnect() {
        JourneyMap.endSession();
        JourneyMap.install(null);
    }

    @Test
    public void readyAdapterReceivesFlooredCoordinatesAndType() {
        adapter.ready = true;
        JourneyMap.addWaypoint("HOME", "home", "minecraft:overworld", -0.2, 63.9, -1.2);
        assertEquals(java.util.Arrays.asList("ADD:HOME:home:-1:63:-2"), adapter.operations);
    }

    @Test
    public void pendingUpdatesAreCoalescedButDifferentTypesRemainIndependent() {
        JourneyMap.addWaypoint("HOME", "home", "minecraft:overworld", 0, 64, 1);
        JourneyMap.addWaypoint("HOME", "home", "minecraft:overworld", 0, 64, 1);
        JourneyMap.addWaypoint("STAGE", "home", "minecraft:overworld", 0, 64, 1);
        JourneyMap.removeWaypoint("HOME", "home", "minecraft:overworld", 0, 64, 1);
        assertTrue(adapter.operations.isEmpty());
        adapter.ready = true;
        JourneyMap.tick();
        assertEquals(java.util.Arrays.asList("REMOVE:HOME:home:0:64:1", "ADD:STAGE:home:0:64:1"), adapter.operations);
        JourneyMap.tick();
        assertEquals(2, adapter.operations.size());
    }

    @Test
    public void reconnectAndSwitchDoNotReplayOldPendingOperations() {
        JourneyMap.addWaypoint("old-server", "minecraft:overworld", 0, 64, 1);
        JourneyMap.endSession();
        adapter.ready = true;
        JourneyMap.tick();
        JourneyMap.addWaypoint("disconnected", "minecraft:overworld", 0, 64, 1);
        JourneyMap.startSession();
        JourneyMap.tick();
        assertTrue(adapter.operations.isEmpty());
        JourneyMap.addWaypoint("new-server", "minecraft:overworld", 0, 64, 1);
        assertEquals(java.util.Arrays.asList("ADD:WAYPOINT:new-server:0:64:1"), adapter.operations);
    }

    @Test
    public void linkageFailureIsIsolatedAndOnlyRetriedOnNewConnection() {
        adapter.ready = true;
        adapter.failure = new NoSuchMethodError("changed contract");
        JourneyMap.addWaypoint("bad", "minecraft:overworld", 0, 64, 1);
        JourneyMap.addWaypoint("suppressed", "minecraft:overworld", 0, 64, 1);
        assertEquals(1, adapter.attempts);
        adapter.failure = null;
        JourneyMap.endSession();
        JourneyMap.startSession();
        JourneyMap.addWaypoint("new", "minecraft:overworld", 0, 64, 1);
        assertEquals(java.util.Arrays.asList("ADD:WAYPOINT:new:0:64:1"), adapter.operations);
    }

    @Test
    public void queueIsBoundedAndOverflowDoesNotReplayInNextWorld() {
        for (int i = 0; i < 300; i++) JourneyMap.addWaypoint("home-" + i, "minecraft:overworld", 0, 64, 1);
        adapter.ready = true;
        JourneyMap.tick();
        assertTrue(adapter.operations.isEmpty());
        JourneyMap.endSession();
        JourneyMap.startSession();
        JourneyMap.tick();
        assertTrue(adapter.operations.isEmpty());
    }

    @Test
    public void virtualMachineFailuresAreNotSwallowed() {
        adapter.ready = true;
        adapter.failure = new OutOfMemoryError("fixture");
        assertThrows(OutOfMemoryError.class, () -> JourneyMap.addWaypoint("fatal", "minecraft:overworld", 0, 64, 1));
    }

    @Test
    public void contractCheckRejectsStaticAndWrongReturnMethods() throws Exception {
        assertEquals(boolean.class, LegacyJourneyMapAdapter.method(Contract.class, "ready", boolean.class).getReturnType());
        assertThrows(NoSuchMethodException.class, () -> LegacyJourneyMapAdapter.method(Contract.class, "wrong", boolean.class));
        assertThrows(NoSuchMethodException.class, () -> LegacyJourneyMapAdapter.method(Contract.class, "statik", boolean.class));
    }

    public static class Contract {
        public boolean ready() { return true; }
        public int wrong() { return 1; }
        public static boolean statik() { return true; }
    }

    private static final class RecordingAdapter implements JourneyMap.Adapter {
        boolean ready;
        Error failure;
        int attempts;
        final List<String> operations = new ArrayList<>();
        public boolean ready() { return ready; }
        public void add(JourneyMap.Point point) {
            attempts++;
            if (failure != null) throw failure;
            operations.add("ADD:" + key(point));
        }
        public void remove(JourneyMap.Point point) { operations.add("REMOVE:" + key(point)); }
        private String key(JourneyMap.Point point) {
            return point.kind() + ":" + point.name() + ":" + point.x() + ":" + point.y() + ":" + point.z();
        }
    }
}
