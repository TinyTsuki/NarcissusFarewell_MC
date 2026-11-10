package xin.vanilla.narcissus.integration.map;

import org.junit.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;

public class JourneyMapSafetyContractTest {
    private static Method required(String name, Class<?>... types) {
        try {
            Method method = JourneyMap.class.getDeclaredMethod(name, types);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("Missing waypoint safety contract: " + name, missing);
        }
    }

    @Test
    public void negativeFractionalCoordinatesUseFloor() throws Exception {
        Method block = required("block", double.class);
        for (double value : new double[]{-10.00001, -1.2, -0.2, 0, 0.8, 10.00001}) {
            assertEquals((int) Math.floor(value), block.invoke(null, value));
        }
    }

    @Test
    public void unknownHistoricalOwnerAndEmptyDimensionsNeverMatch() throws Exception {
        Method matches = required("matches", String.class, String.class, Collection.class,
                int.class, int.class, int.class, String.class, String.class, int.class, int.class, int.class);
        Object[] point = {"narcissus_farewell", "home", Collections.singletonList("minecraft:overworld"),
                -1, 64, -2, "home", "minecraft:overworld", -1, 64, -2};
        assertEquals(true, matches.invoke(null, point));
        point[0] = "journeymap";
        assertEquals(false, matches.invoke(null, point));
        point[0] = "narcissus_farewell";
        point[2] = Collections.emptyList();
        assertEquals(false, matches.invoke(null, point));
    }

    @Test
    public void nativeReaderCannotRenameUnprovenSourceFiles() throws Exception {
        Method guard;
        try {
            guard = Class.forName("xin.vanilla.narcissus.integration.map.LegacyJourneyMapFiles")
                    .getMethod("requireNativeFileIdentity", java.nio.file.Path.class, String.class);
        } catch (ReflectiveOperationException missing) {
            throw new AssertionError("Missing native reader file-identity guard", missing);
        }
        guard.invoke(null, Paths.get("home.json"), "home.json");
        try {
            guard.invoke(null, Paths.get("old-home.json"), "home.json");
            fail("A native filename change must stop before old file deletion");
        } catch (java.lang.reflect.InvocationTargetException blocked) {
            assertTrue(blocked.getCause() instanceof java.io.IOException);
        }
    }


    @Test
    public void clientMixinsProtectFileIdentityAndRunBeforeTheNativeReader() throws Exception {
        var config = com.google.gson.JsonParser.parseString(new String(Files.readAllBytes(
                Paths.get("src/main/resources/narcissus_farewell.mixins.json")), StandardCharsets.UTF_8)).getAsJsonObject();
        var client = new java.util.HashSet<String>();
        config.getAsJsonArray("client").forEach(value -> client.add(value.getAsString()));
        assertTrue(client.contains("JourneyMapWaypointFileMixin"));
        assertTrue(client.contains("JourneyMapWaypointReaderMixin"));
    }
}
