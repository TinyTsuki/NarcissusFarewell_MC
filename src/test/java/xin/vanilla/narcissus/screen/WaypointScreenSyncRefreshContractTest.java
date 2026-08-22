package xin.vanilla.narcissus.screen;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class WaypointScreenSyncRefreshContractTest {
    @Test
    public void openWaypointScreenReloadsAfterPlayerDataSync() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/narcissus/screen/WaypointScreen.java")),
                StandardCharsets.UTF_8);

        assertTrue(source.contains("NarcissusClientSyncState.playerDataGeneration()"));
        assertTrue(source.contains("observedPlayerDataGeneration"));
        assertTrue(source.contains("public void tick()"));
        assertTrue(source.contains("refreshFromSynchronizedPlayerData()"));
        assertTrue(source.contains("sameWaypoint(previousSelection"));
    }
}
