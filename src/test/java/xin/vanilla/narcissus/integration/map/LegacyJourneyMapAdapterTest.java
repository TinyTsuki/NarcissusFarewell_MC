package xin.vanilla.narcissus.integration.map;

import net.minecraft.core.BlockPos;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class LegacyJourneyMapAdapterTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    private URLClassLoader fixture() throws Exception {
        Path root = temporary.newFolder().toPath();
        Path nativePoint = root.resolve("waypoint/Waypoint.java");
        Path store = root.resolve("waypoint/WaypointStore.java");
        Path apiPoint = root.resolve("api/Waypoint.java");
        Files.createDirectories(nativePoint.getParent());
        Files.createDirectories(apiPoint.getParent());
        try (var input = getClass().getResourceAsStream("/journeymap5/Waypoint.java")) {
            Files.copy(input, nativePoint);
        }
        try (var input = getClass().getResourceAsStream("/journeymap5/WaypointStore.java")) {
            Files.copy(input, store);
        }
        try (var input = getClass().getResourceAsStream("/journeymap5/ApiWaypoint.java")) {
            Files.copy(input, apiPoint);
        }
        String minecraft = Path.of(BlockPos.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-encoding", "UTF-8", "-classpath", minecraft,
                "-d", root.toString(), nativePoint.toString(), store.toString(), apiPoint.toString()));
        return new URLClassLoader(new java.net.URL[]{root.toUri().toURL()}, getClass().getClassLoader());
    }

    @Test
    public void ownedDisplayIdsDoNotReplaceManualOrOtherKindWaypoints() throws Exception {
        try (URLClassLoader loader = fixture()) {
            JourneyMap.Adapter adapter = LegacyJourneyMapAdapter.bind(loader);
            Class<?> point = loader.loadClass("journeymap.client.waypoint.Waypoint");
            Class<?> type = loader.loadClass("journeymap.client.waypoint.Waypoint$Type");
            Class<?> storeType = loader.loadClass("journeymap.client.waypoint.WaypointStore");
            Object instance = storeType.getField("INSTANCE").get(null);
            Object manual = point.getConstructor(String.class, int.class, int.class, int.class, boolean.class,
                            int.class, int.class, int.class, type, String.class, String.class, Collection.class, boolean.class)
                    .newInstance("home", -1, 64, -2, true, 1, 2, 3, type.getField("Normal").get(null), "journeymap",
                            "minecraft:overworld", List.of("minecraft:overworld"), true);
            storeType.getMethod("save", point, boolean.class).invoke(instance, manual, true);
            JourneyMap.Point home = new JourneyMap.Point("HOME", "home", "minecraft:overworld", -1, 64, -2);
            JourneyMap.Point stage = new JourneyMap.Point("STAGE", "home", "minecraft:overworld", -1, 64, -2);
            adapter.add(home);
            adapter.add(stage);
            adapter.add(home);
            Collection<?> values = (Collection<?>) storeType.getMethod("getAll").invoke(instance);
            assertEquals("manual, HOME and STAGE must have independent IDs", 3, values.size());
            assertTrue(values.contains(manual));
            java.util.Map<?, ?> files = (java.util.Map<?, ?>) storeType.getField("files").get(instance);
            assertEquals("native filenames must not overwrite manual or other kind", 3, files.size());
            storeType.getMethod("reload").invoke(instance);
            assertEquals(3, values.size());
            adapter.remove(home);
            assertEquals(2, values.size());
            assertEquals(2, files.size());
            assertTrue(values.contains(manual));
            adapter.remove(stage);
            storeType.getMethod("reload").invoke(instance);
            assertEquals(List.of(manual), List.copyOf(values));
        }
    }

    @Test
    public void incompleteHistoricalDisplayIdIsNeverAdoptedOrDeleted() throws Exception {
        try (URLClassLoader loader = fixture()) {
            JourneyMap.Adapter adapter = LegacyJourneyMapAdapter.bind(loader);
            Class<?> api = loader.loadClass("journeymap.client.api.display.Waypoint");
            Object value = api.getConstructor(String.class, String.class, String.class, String.class, BlockPos.class)
                    .newInstance("narcissus_farewell", "HOME_old", "home", "minecraft:overworld", new BlockPos(-1, 64, -2));
            Class<?> point = loader.loadClass("journeymap.client.waypoint.Waypoint");
            Object historical = point.getConstructor(api).newInstance(value);
            Class<?> store = loader.loadClass("journeymap.client.waypoint.WaypointStore");
            Object instance = store.getField("INSTANCE").get(null);
            store.getMethod("save", point, boolean.class).invoke(instance, historical, true);
            JourneyMap.Point home = new JourneyMap.Point("HOME", "home", "minecraft:overworld", -1, 64, -2);
            adapter.add(home);
            Collection<?> points = (Collection<?>) store.getMethod("getAll").invoke(instance);
            assertEquals("An unproven old identity must not suppress the new owned point", 2, points.size());
            adapter.remove(home);
            assertEquals(1, points.size());
            assertSame(historical, points.iterator().next());
        }
    }

    @Test
    public void netherCoordinatesMatchInTheirOwnDimensionWhileViewingOverworld() throws Exception {
        try (URLClassLoader loader = fixture()) {
            JourneyMap.Adapter adapter = LegacyJourneyMapAdapter.bind(loader);
            Class<?> store = loader.loadClass("journeymap.client.waypoint.WaypointStore");
            Object instance = store.getField("INSTANCE").get(null);
            JourneyMap.Point first = new JourneyMap.Point("HOME", "nether", "minecraft:the_nether", -1, 64, -2);
            JourneyMap.Point second = new JourneyMap.Point("HOME", "nether", "minecraft:the_nether", -8, 64, -16);
            adapter.add(first);
            adapter.add(second);
            Collection<?> points = (Collection<?>) store.getMethod("getAll").invoke(instance);
            assertEquals("different dimension coordinates must not deduplicate", 2, points.size());
            loader.loadClass("journeymap.client.waypoint.Waypoint").getField("viewingNether").set(null, true);
            adapter.remove(second);
            assertEquals(1, points.size());
            Object remaining = points.iterator().next();
            assertEquals(-7.5, (double) remaining.getClass().getMethod("getRawCenterX").invoke(remaining), 0);
            adapter.remove(first);
            assertTrue(points.isEmpty());
        }
    }

    @Test
    public void reflectedErrorsIsolateLinkageButPreserveVirtualMachineFailures() throws Exception {
        try (URLClassLoader loader = fixture()) {
            JourneyMap.Adapter adapter = LegacyJourneyMapAdapter.bind(loader);
            Class<?> store = loader.loadClass("journeymap.client.waypoint.WaypointStore");
            store.getField("failure").set(null, new NoSuchMethodError("fixture changed contract"));
            JourneyMap.install(adapter);
            JourneyMap.startSession();
            try {
                JourneyMap.addWaypoint("linked", "minecraft:overworld", 0, 64, 1);
                store.getField("failure").set(null, new OutOfMemoryError("fixture VM failure"));
                JourneyMap.endSession();
                JourneyMap.startSession();
                assertThrows(OutOfMemoryError.class, () -> JourneyMap.addWaypoint("fatal", "minecraft:overworld", 0, 64, 1));
            } finally {
                JourneyMap.endSession();
                JourneyMap.install(null);
            }
        }
    }

    @Test
    public void absentLegacyClassesDoNotRequireMinecraftOrMapLinkage() throws Exception {
        try (URLClassLoader isolated = new URLClassLoader(new java.net.URL[0], ClassLoader.getPlatformClassLoader())) {
            assertNull(LegacyJourneyMapAdapter.bind(isolated));
        }
    }
}
