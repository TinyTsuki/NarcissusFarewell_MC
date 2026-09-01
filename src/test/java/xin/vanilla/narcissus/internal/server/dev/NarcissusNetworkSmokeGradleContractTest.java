package xin.vanilla.narcissus.internal.server.dev;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class NarcissusNetworkSmokeGradleContractTest {

    @Test
    public void phaseOneRequiresAllTeleportWorkloadMarkers() throws Exception {
        String script = new String(Files.readAllBytes(Paths.get("gradle", "network-smoke.gradle")), StandardCharsets.UTF_8);

        assertTrue(script.contains("PASS safe-random-teleport"));
        assertTrue(script.contains("PASS view-end-teleport"));
        assertTrue(script.contains("PASS cross-dimension-follower-teleport"));
        assertTrue(script.contains("PASS spark-profiler-active"));
        assertTrue(script.contains("PASS spark-report-written"));
    }

    @Test
    public void commonConfigSmokeUsesTheRegisteredDescriptorPath() throws Exception {
        String runner = new String(Files.readAllBytes(Paths.get("src", "main", "java", "xin", "vanilla", "narcissus",
                "internal", "server", "dev", "NarcissusNetworkSmokeServerRunner.java")), StandardCharsets.UTF_8);

        assertTrue(runner.contains("narcissus config common base.teleportLimit.teleportRecordLimit"));
    }
}
