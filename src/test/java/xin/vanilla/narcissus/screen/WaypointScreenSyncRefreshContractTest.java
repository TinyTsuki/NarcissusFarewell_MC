package xin.vanilla.narcissus.screen;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class WaypointScreenSyncRefreshContractTest {
    @Test
    public void openWaypointScreenReloadsAfterPlayerDataSync() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/narcissus/screen/WaypointScreen.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("NarcissusClientSyncState.waypointDataGeneration()"));
        assertTrue(source.contains("observedWaypointDataGeneration"));
        assertTrue(source.contains("public void tick()"));
        assertTrue(source.contains("refreshFromSynchronizedPlayerData()"));
    }

    @Test
    public void synchronizedRefreshRetainsOnlyTheSameLogicalWaypoint() {
        WaypointSelectionState.Key selected = stage("spawn", "minecraft:overworld");
        WaypointSelectionState.Key sameWaypoint = stage("spawn", "minecraft:overworld");
        WaypointSelectionState.Key otherWaypoint = stage("market", "minecraft:overworld");

        assertSame(sameWaypoint, WaypointSelectionState.findMatching(
                selected, Arrays.asList(otherWaypoint, sameWaypoint)));
        assertNull(WaypointSelectionState.findMatching(selected, Collections.singletonList(otherWaypoint)));
    }

    @Test
    public void deletingTheSelectedWaypointDoesNotSelectAnotherEntry() {
        Object selected = new Object();
        Object otherWaypoint = new Object();

        assertNull(WaypointSelectionState.afterDelete(selected, selected));
        assertSame(selected, WaypointSelectionState.afterDelete(selected, otherWaypoint));
    }

    private static WaypointSelectionState.Key stage(String name, String dimension) {
        return new WaypointSelectionState.Key("STAGE", name, dimension, null);
    }
}
