package xin.vanilla.narcissus.internal.server.dev;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarcissusNetworkSmokeGradleContractTest {

    @Test
    public void phaseOneRequiresTeleportWorkloadAndSparkEvidence() throws Exception {
        String script = read("gradle", "network-smoke.gradle");

        assertTrue(script.contains("PASS random-safe-teleport"));
        assertTrue(script.contains("PASS view-end-teleport"));
        assertTrue(script.contains("PASS carpet-fake-player-teleports"));
        assertTrue(script.contains("PASS spark-profiler-active"));
        assertTrue(script.contains("PASS spark-report-written"));
    }

    @Test
    public void smokeChildExcludesClientOnlyRuntimeMods() throws Exception {
        String script = read("gradle", "network-smoke.gradle");

        assertTrue(script.contains("module: 'sodium-extra'"));
        assertTrue(script.contains("module: 'AANobbMI'"));
    }

    @Test
    public void fabricSixteenServerRunsRemappedProductionArtifacts() throws Exception {
        String script = read("gradle", "network-smoke.gradle");

        assertTrue(script.contains("prepareProductionFabricServer"));
        assertTrue(script.contains("fabric-server-launch.jar"));
        assertTrue(script.contains("remapJar"));
        assertTrue(script.contains("Narcissus production smoke server"));
    }

    @Test
    public void legacySparkRuntimeUsesItsOwnSamplerExportPath() throws Exception {
        String runner = read("src", "main", "java", "xin", "vanilla", "narcissus", "internal", "server", "dev",
                "NarcissusNetworkSmokeServerRunner.java");

        assertTrue(runner.contains("startLegacySparkProfile"));
        assertTrue(runner.contains("me.lucko.spark.fabric.FabricPlatformInfo"));
        assertTrue(runner.contains("Sampler$ExportProps"));
        assertTrue(runner.contains("legacySpark"));
    }

    @Test
    public void phaseOneClientStaysConnectedUntilTheFakePlayerWorkloadCompletes() throws Exception {
        String script = read("gradle", "network-smoke.gradle");
        String client = read("src", "main", "java", "xin", "vanilla", "narcissus", "internal", "client", "dev",
                "NarcissusNetworkSmokeClientRunner.java");

        assertTrue(script.contains("narcissus.networkSmoke.serverStatus"));
        assertTrue(client.contains("PASS carpet-fake-player-cleanup"));
        assertTrue(client.contains("serverStatusContains"));
    }

    @Test
    public void configEchoWaitsForTheSentinelInsteadOfTheFirstPlayerPacket() throws Exception {
        String runner = read("src", "main", "java", "xin", "vanilla", "narcissus", "internal", "client", "dev",
                "NarcissusNetworkSmokeClientRunner.java").replace("\r\n", "\n");

        assertTrue(runner.contains("if (data.getTeleportCountdownSeconds(EnumTeleportType.TP_HOME) != NarcissusNetworkSmokeFixture.COUNTDOWN) {\n"
                + "            // The login packet and the C2S echo can arrive in either order. Keep waiting\n"
                + "            // until the exact sentinel returns instead of treating the first packet as the echo.\n"
                + "            syncGeneration = NarcissusClientSyncState.playerDataGeneration();\n"
                + "            return;\n"
                + "        }"));
    }

    @Test
    public void teleportFixtureDoesNotMutatePersistentPreferences() throws Exception {
        String runner = read("src", "main", "java", "xin", "vanilla", "narcissus", "internal", "server", "dev",
                "NarcissusNetworkSmokeServerRunner.java");

        assertTrue(runner.contains("EnumTeleportType.OTHER"));
        assertFalse(runner.contains("forceServerCountdown(true)"));
        assertFalse(runner.contains("setTeleportCountdownSeconds("));
        assertFalse(runner.contains("teleportRandomDistanceLimit(SAFE_RANDOM_RANGE)"));
    }

    private static String read(String... path) throws Exception {
        return new String(Files.readAllBytes(Paths.get("", path)), StandardCharsets.UTF_8);
    }
}
