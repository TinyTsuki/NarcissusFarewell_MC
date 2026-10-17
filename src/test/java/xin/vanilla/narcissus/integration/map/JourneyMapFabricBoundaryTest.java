package xin.vanilla.narcissus.integration.map;

import com.google.gson.JsonParser;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class JourneyMapFabricBoundaryTest {
    private static String source(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    @Test
    public void journeyMapDiscoversOptionalClientPlugin() throws Exception {
        com.google.gson.JsonObject metadata = new JsonParser().parse(source("src/main/resources/fabric.mod.json")).getAsJsonObject();
        assertTrue(metadata.getAsJsonObject("entrypoints").has("journeymap"));
        assertFalse(metadata.getAsJsonObject("depends").has("journeymap"));
    }

    @Test
    public void packetBridgeHonorsBothExistingPreferences() throws Exception {
        String bridge = source("src/main/java/xin/vanilla/narcissus/integration/MapHelper.java");
        assertTrue(bridge.contains("syncHomeMapWaypoint()"));
        assertTrue(bridge.contains("syncStageMapWaypoint()"));
        assertTrue(bridge.contains("JourneyMap.addWaypoint"));
        assertTrue(bridge.contains("JourneyMap.removeWaypoint"));
        assertFalse(bridge.contains("journeymap.api."));
    }

    @Test
    public void connectionLifecycleDrainsAndClearsPendingMapWork() throws Exception {
        String events = source("src/main/java/xin/vanilla/narcissus/event/ClientModEventHandler.java");
        assertTrue(events.contains("JourneyMap.startSession()"));
        assertTrue(events.contains("JourneyMap.endSession()"));
        assertTrue(events.contains("JourneyMap.tick()"));
    }

    @Test
    public void apiInputIsRemappedAndNeverBundled() throws Exception {
        String build = source("build.gradle");
        assertTrue(build.contains("modCompileOnly 'info.journeymap:journeymap-api-fabric:1.16.5-2.0.0'"));
        assertFalse(build.contains("include('info.journeymap:"));
        assertFalse(build.contains("journeyMapBeta"));
    }
}
