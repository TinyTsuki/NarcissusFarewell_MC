package xin.vanilla.narcissus.internal.server.dev;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarcissusNetworkSmokeWorkloadTest {

    @Test
    public void viewEndFixtureUsesARealLongDistanceProbe() throws Exception {
        String runner = read("src/main/java/xin/vanilla/narcissus/internal/server/dev/"
                + "NarcissusNetworkSmokeServerRunner.java");

        assertTrue(runner.contains("VIEW_RANGE = 64"));
        assertTrue(runner.contains("VIEW_COLLISION_DISTANCE = VIEW_RANGE - 8"));
        assertTrue(runner.contains("prepareViewCollisionWall(level, player)"));
        assertTrue(runner.contains("findViewEndCandidate(player, false, VIEW_RANGE)"));
    }
    @Test
    public void completesAfterTheFixedSearchWindow() {
        NarcissusNetworkSmokeWorkload workload = new NarcissusNetworkSmokeWorkload(80);

        assertFalse(workload.completeAt(399));
        assertTrue(workload.completeAt(400));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
