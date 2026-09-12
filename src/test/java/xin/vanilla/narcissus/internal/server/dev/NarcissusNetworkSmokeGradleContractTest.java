package xin.vanilla.narcissus.internal.server.dev;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

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
    public void bothScopesRequireReadOnlyFullConfigCheckpointsBeforeFinishing() throws Exception {
        String runner = new String(Files.readAllBytes(Paths.get("src", "main", "java", "xin", "vanilla", "narcissus",
                "internal", "server", "dev", "NarcissusNetworkSmokeServerRunner.java")), StandardCharsets.UTF_8);
        String client = source("client", "dev", "NarcissusNetworkSmokeClientRunner.java");
        String configs = source("dev", "NarcissusNetworkSmokeConfigs.java");
        String script = read("gradle", "network-smoke.gradle");

        assertTrue(runner.contains("NarcissusNetworkSmokeConfigs.completeServer(phase)"));
        assertTrue(client.contains("NarcissusNetworkSmokeConfigs.completeClient(NarcissusNetworkSmokeStatus.phase())"));
        assertTrue(script.contains("PASS complete-config-snapshot"));
        assertTrue(script.contains("PASS complete-config-restart"));
        assertTrue(script.contains("configMarker + ' scope=CLIENT'"));
        assertTrue(script.contains("configMarker + ' scope=COMMON'"));
        assertFalse(configs.contains(".save("));
    }

    @Test
    public void configEchoWaitsForTheSentinelValueInsteadOfTheFirstPlayerDataPacket() throws Exception {
        String runner = new String(Files.readAllBytes(Paths.get("src", "main", "java", "xin", "vanilla", "narcissus",
                "internal", "client", "dev", "NarcissusNetworkSmokeClientRunner.java")), StandardCharsets.UTF_8)
                .replace("\r\n", "\n");

        assertTrue(runner.contains("if (data.getTeleportCountdownSeconds(EnumTeleportType.TP_HOME) != NarcissusNetworkSmokeFixture.COUNTDOWN) {\n            return;\n        }"));
    }

    @Test
    public void teleportFixturePreservesTheRealCountdownAndChecksEveryActorRecord() throws Exception {
        String runner = source("server", "dev", "NarcissusMeasuredTeleports.java");

        assertTrue(runner.contains("EnumTeleportType.OTHER"));
        assertFalse(runner.contains("forceServerCountdown(true)"));
        assertFalse(runner.contains("setTeleportCountdownSeconds(EnumTeleportType.TP_HOME, 0)"));
        assertFalse(runner.contains("teleportRandomDistanceLimit("));
        assertTrue(runner.contains("expected.equals(records)"));
        assertTrue(runner.contains("random == 20 && view == 20 && cross == 20"));
        assertTrue(runner.contains("cycles=20 teleports=240 homes=48 records-per-actor=60"));
    }

    @Test
    public void carpetUsesTheForge18ArtifactOnlyInTheSmokeParentAndDedicatedChild() throws Exception {
        String script = read("gradle", "network-smoke.gradle");
        String actor = source("server", "dev", "NarcissusMeasuredTeleports.java");
        assertTrue(script.contains("def devForgeCarpet = 'curse.maven:forge-carpet-547361:3930877'"));
        assertTrue(script.contains("smokeTasks.contains('networkSmoke') || (smokeChild && smokeTasks.contains('runServer'))"));
        assertTrue(script.contains("dependencies.add('runtimeOnly', fg.deobf(devForgeCarpet))"));
        assertTrue(script.contains("carpetCoordinate: devForgeCarpet"));
        assertFalse(script.contains("3527542"));
        assertTrue(actor.contains("net.cjsah.mod.carpet.patches.EntityPlayerMPFake"));
        assertTrue(actor.contains("ResourceKey.class, GameType.class, boolean.class"));
        assertTrue(actor.contains("PLAYER_COUNT = 4"));
    }

    @Test
    public void nativeUiUsesTheForge18ConnectionRenderAndChatMappings() throws Exception {
        String client = source("client", "dev", "NarcissusNetworkSmokeClientRunner.java");
        String screens = source("client", "dev", "NarcissusNetworkSmokeScreens.java");
        String chat = source("client", "dev", "NarcissusNetworkSmokeNotificationsCheck.java");
        assertTrue(client.contains("ConnectScreen.startConnecting("));
        assertTrue(screens.contains("ScreenEvent.DrawScreenEvent.Post"));
        assertTrue(screens.contains("event.getScreen()"));
        assertTrue(screens.contains("InputStateManager.instance().handleDrawScreenPre"));
        assertTrue(chat.contains("ChatComponent.class, client.gui.getChat(), \"f_93760_\""));
        assertTrue(chat.contains("net.minecraftforge.fml.util.ObfuscationReflectionHelper"));
        assertFalse(chat.contains("field_146252_h"));
    }

    @Test
    public void sparkUsesTheExistingForge18ApiWithoutDoubleStartingOrEarlyStopping() throws Exception {
        for (String side : new String[]{"client", "server"}) {
            String runner = source(side, "dev", "NarcissusNetworkSmoke"
                    + (side.equals("client") ? "Client" : "Server") + "Runner.java");
            assertTrue(runner.contains("Sampler$ExportProps"));
            assertTrue(runner.contains("\"toProto\", 2"));
            assertTrue(runner.contains("\"getAutoEndTime\""));
            assertTrue(runner.contains("future.get()"));
            assertFalse(runner.contains("\"getEndTime\""));
            assertFalse(runner.contains("\"toProto\", 6"));
            assertFalse(runner.contains("\"start\", 0"));
            assertFalse(runner.contains("\"stop\", 1"));
        }
        assertTrue(source("client", "dev", "NarcissusNetworkSmokeClientRunner.java")
                .contains("specific.getConstructor(Thread.class).newInstance(renderThread)"));
    }

    private static String source(String... parts) throws Exception {
        return new String(Files.readAllBytes(Paths.get("src", "main", "java", "xin", "vanilla", "narcissus",
                "internal").resolve(Paths.get("", parts))), StandardCharsets.UTF_8);
    }

    private static String read(String... parts) throws Exception {
        return new String(Files.readAllBytes(Paths.get("", parts)), StandardCharsets.UTF_8);
    }
}
