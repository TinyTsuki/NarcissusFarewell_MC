package xin.vanilla.narcissus.integration.map;

import com.google.gson.JsonParser;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JourneyMapFabricBoundaryTest {
    private static String source(String path) throws Exception {
        return Files.readString(Path.of(path), StandardCharsets.UTF_8);
    }

    @Test
    public void journeyMapDiscoversOptionalClientPlugin() throws Exception {
        var metadata = JsonParser.parseString(source("src/main/resources/fabric.mod.json")).getAsJsonObject();
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
        assertTrue(build.contains("modCompileOnly files(journeyMapApi)"));
        assertFalse(build.contains("include files(journeyMapApi)"));
        assertFalse(build.contains("journeyMapBeta"));
    }
}
