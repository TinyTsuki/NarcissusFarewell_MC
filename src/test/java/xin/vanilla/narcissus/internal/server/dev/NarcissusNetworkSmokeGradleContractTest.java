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
    public void smokeChildrenExcludeClientOnlyPerformanceMods() throws Exception {
        String script = new String(Files.readAllBytes(Paths.get("gradle", "network-smoke.gradle")), StandardCharsets.UTF_8);

        assertTrue(script.contains("module: 'embeddium'"));
        assertTrue(script.contains("module: 'rubidium-extra-654373'"));
    }

    @Test
    public void commonConfigSmokeUsesTheRegisteredDescriptorPath() throws Exception {
        String runner = new String(Files.readAllBytes(Paths.get("src", "main", "java", "xin", "vanilla", "narcissus",
                "internal", "server", "dev", "NarcissusNetworkSmokeServerRunner.java")), StandardCharsets.UTF_8);

        assertTrue(runner.contains("narcissus config common base.teleportLimit.teleportRecordLimit"));
    }

    @Test
    public void configEchoWaitsForTheSentinelValueInsteadOfTheFirstPlayerDataPacket() throws Exception {
        String runner = new String(Files.readAllBytes(Paths.get("src", "main", "java", "xin", "vanilla", "narcissus",
                "internal", "client", "dev", "NarcissusNetworkSmokeClientRunner.java")), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(runner.contains("if (data.getTeleportCountdownSeconds(EnumTeleportType.TP_HOME) != NarcissusNetworkSmokeFixture.COUNTDOWN) {\n            return;\n        }"));
    }

    @Test
    public void teleportFixtureDoesNotWritePersistentConfigOrPlayerData() throws Exception {
        String runner = new String(Files.readAllBytes(Paths.get("src", "main", "java", "xin", "vanilla", "narcissus",
                "internal", "server", "dev", "NarcissusNetworkSmokeServerRunner.java")), StandardCharsets.UTF_8);

        assertTrue(runner.contains("EnumTeleportType.OTHER"));
        assertTrue(!runner.contains("forceServerCountdown(true)"));
        assertTrue(!runner.contains("data.setTeleportCountdownSeconds(EnumTeleportType.TP_HOME, 0)"));
        assertTrue(!runner.contains("teleportRandomDistanceLimit(SAFE_RANDOM_RANGE)"));
    }
}
