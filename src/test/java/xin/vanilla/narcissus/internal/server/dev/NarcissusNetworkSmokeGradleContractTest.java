package xin.vanilla.narcissus.internal.server.dev;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

public class NarcissusNetworkSmokeGradleContractTest {

    @Test
    public void phaseStatusPathsAndSparkPropertyKeepTheInheritedSmokeContract() {
        java.nio.file.Path client = Paths.get("evidence", "client-phase-one.status");
        assertTrue(xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus.serverStatusPath(client)
                .equals(Paths.get("evidence", "server-phase-one.status")));
        String key = xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus.SPARK_REPORT_PROPERTY;
        String previous = System.getProperty(key);
        try {
            System.setProperty(key, " report.sparkprofile ");
            assertTrue("report.sparkprofile".equals(
                    xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus.sparkReport()));
        } finally {
            if (previous == null) System.clearProperty(key);
            else System.setProperty(key, previous);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void statusPathRejectsNonClientEvidence() {
        xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus.serverStatusPath(
                Paths.get("evidence", "server-phase-one.status"));
    }

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

        assertTrue(script.contains("module: '5ZwdcRci'"));
        assertFalse(script.contains("module: 'embeddium'"));
        assertFalse(script.contains("module: 'rubidium-extra-654373'"));
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
        assertTrue(configs.contains("import xin.vanilla.banira.api.BaniraConfigs;"));
        assertTrue(configs.contains("BaniraConfigs.holder(configClass)"));
        assertFalse(configs.contains("common.config.BaniraConfig"));
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
    public void fakePlayersUseThePublishedForge21ArtifactOnlyInTheSmokeParentAndBothChildren() throws Exception {
        String script = read("gradle", "network-smoke.gradle");
        String actor = source("server", "dev", "NarcissusMeasuredTeleports.java");
        assertTrue(script.contains("def devFakePlayer = 'curse.maven:advanced-fake-player-1566022:8869054'"));
        assertTrue(script.contains("-Pnarcissus.networkSmoke.fakePlayerSha256=${fakePlayerRuntimeSha256}"));
        assertTrue(script.contains("smokeTasks.contains('networkSmoke') || (smokeChild &&"));
        assertTrue(script.contains("(smokeTasks.contains('runServer') || smokeTasks.contains('runClient'))"));
        assertTrue(script.contains("dependencies.add('runtimeOnly', fg.deobf(devFakePlayer))"));
        assertTrue(script.contains("fakePlayerCoordinate: devFakePlayer"));
        assertTrue(script.contains("fakePlayerRuntimeSha256 = sha256(fakePlayer)"));
        assertTrue(script.contains("it.name.startsWith('advanced-fake-player-1566022-8869054')"));
        assertTrue(script.contains("fakePlayerRuntimeScope: 'both-smoke-children'"));
        assertFalse(script.contains("maven.modrinth:curtain"));
        assertFalse(script.contains("3527542"));
        assertFalse(script.contains("3930877"));
        assertFalse(script.contains("forgeCarpet"));
        assertTrue(actor.contains("com.advancedfakeplayers.entity.FakeServerPlayer"));
        assertTrue(actor.contains("com.advancedfakeplayers.manager.FakePlayerManager"));
        assertTrue(actor.contains("getMethod(\"spawnFakePlayer\", MinecraftServer.class"));
        assertTrue(actor.contains("ServerLevel.class, com.mojang.authlib.GameProfile.class"));
        assertTrue(actor.contains("UUID.nameUUIDFromBytes((\"OfflinePlayer:\" + name).getBytes(StandardCharsets.UTF_8))"));
        assertFalse(actor.contains("allowSpawningOfflinePlayers"));
        assertTrue(actor.contains("\"fakeplayer remove \""));
        assertTrue(actor.contains("PLAYER_COUNT = 4"));
        assertTrue(actor.contains("new SafeBlockChecker(actor.serverLevel(), actor)"));
        assertFalse(actor.contains("actor.getLevel()"));
        assertFalse(actor.contains("actor.level."));
        assertTrue(actor.contains("performPrefixedCommand("));
        assertFalse(actor.contains(".performCommand("));
    }

    @Test
    public void fakePlayerRuntimeIsBoundToItsModContainerAndParentHashOnBothSidesAndPhases() throws Exception {
        String script = read("gradle", "network-smoke.gradle");
        String actor = source("server", "dev", "NarcissusMeasuredTeleports.java");
        String provider = source("dev", "NarcissusNetworkSmokeNotifications.java");
        String server = source("server", "dev", "NarcissusNetworkSmokeServerRunner.java");
        assertTrue(actor.contains("NarcissusNetworkSmokeNotifications.verifyFakePlayerRuntime()"));
        assertTrue(provider.contains("ServerPlayer.class.isAssignableFrom(type)"));
        assertTrue(provider.contains("getModContainerById(\"advancedfakeplayers\")"));
        assertTrue(provider.contains("\"1.0.0\".equals(container.getModInfo().getVersion().toString())"));
        assertTrue(provider.contains("type.getClassLoader() == mod.getClass().getClassLoader()"));
        assertTrue(provider.contains("source.equals(mod.getClass().getProtectionDomain().getCodeSource().getLocation().toExternalForm())"));
        assertTrue(provider.contains("expectedHash.equals(hash.toString())"));
        assertTrue(provider.contains("FAKE_PLAYER_COORDINATE.equals(System.getProperty"));
        assertTrue(provider.indexOf("verifyFakePlayerRuntime();") < provider.indexOf("if (runtimeRecorded) return"));
        assertTrue(server.indexOf("NarcissusMeasuredTeleports.verifyProvider()") < server.indexOf("if (\"phase-two\""));
        assertTrue(script.contains("'RUNTIME com.advancedfakeplayers.entity.FakeServerPlayer'"));
        assertTrue(script.contains("'PASS advanced-fake-players-ready'"));
        assertTrue(script.contains("'PASS advanced-fake-player-cleanup'"));
        assertFalse(script.contains("PASS carpet-fake"));
    }

    @Test
    public void nativeUiUsesTheForge21ConnectionRenderRegistryAndChatMappings() throws Exception {
        String client = source("client", "dev", "NarcissusNetworkSmokeClientRunner.java");
        String screens = source("client", "dev", "NarcissusNetworkSmokeScreens.java");
        String chat = source("client", "dev", "NarcissusNetworkSmokeNotificationsCheck.java");
        assertTrue(client.contains("ConnectScreen.startConnecting("));
        assertTrue(client.contains("ServerData.Type.OTHER"));
        assertTrue(client.contains("ServerAddress.parseString(server.ip), server, false, null)"));
        assertTrue(screens.contains("import net.minecraft.client.gui.GuiGraphics;"));
        assertTrue(screens.contains("public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)"));
        assertFalse(screens.contains("PoseStack"));
        assertTrue(screens.contains("ScreenEvent.Render.Post"));
        assertTrue(screens.contains("event.getScreen()"));
        assertTrue(screens.contains("screen.inputState()"));
        assertTrue(screens.contains("getMethod(\"handleDrawScreenPre\", double.class, double.class)"));
        assertTrue(screens.contains("updateRenderInput((BaniraScreen) screen, mouseX, mouseY)"));
        assertFalse(screens.contains("import xin.vanilla.banira.internal."));
        assertFalse(screens.contains("InputStateManager"));
        assertTrue(chat.contains("ChatComponent.class, client.gui.getChat(), \"allMessages\""));
        assertTrue(chat.contains("Serializer.toJson(expected, registries)"));
        assertTrue(chat.contains("client.level.registryAccess()"));
        assertTrue(chat.contains("net.minecraftforge.fml.util.ObfuscationReflectionHelper"));
        assertFalse(chat.contains("field_146252_h"));
        assertTrue(chat.contains("List<GuiMessage>"));
        assertTrue(chat.contains("line.content()"));
        assertTrue(client.contains("Component.literal(\"Narcissus network smoke complete\")"));
        assertFalse(client.contains("TextComponent"));
        assertFalse(source("dev", "NarcissusNetworkSmokeNotifications.java").contains("TextComponent"));
    }

    @Test
    public void sparkUsesTheExistingForge21ApiWithoutDoubleStartingOrEarlyStopping() throws Exception {
        for (String side : new String[]{"client", "server"}) {
            String runner = source(side, "dev", "NarcissusNetworkSmoke"
                    + (side.equals("client") ? "Client" : "Server") + "Runner.java");
            assertTrue(runner.contains("Sampler$ExportProps"));
            assertTrue(runner.contains("\"toProto\", 2"));
            assertTrue(runner.contains("\"getAutoEndTime\""));
            assertTrue(runner.contains("future.get()"));
            assertTrue(runner.contains("getMethod(\"threadGrouper\", Supplier.class)"));
            assertTrue(runner.contains("getMethod(\"mergeStrategy\", strategyType)"));
            assertFalse(runner.contains("getMethod(\"mergeMode\""));
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
