package xin.vanilla.narcissus.internal.client.dev;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.client.gui.NotificationTypeConfigScreen;
import xin.vanilla.banira.common.util.EnvironmentUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumPanelMode;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.event.ClientModEventHandler;
import xin.vanilla.narcissus.integration.ScreenHelper;
import xin.vanilla.narcissus.internal.client.NarcissusClientSyncState;
import xin.vanilla.narcissus.network.packet.PlayerConfigSyncToServer;
import xin.vanilla.narcissus.network.packet.WaypointAddHomeToServer;
import xin.vanilla.narcissus.network.packet.WaypointDelToServer;
import xin.vanilla.narcissus.network.packet.WaypointTeleportToServer;
import xin.vanilla.narcissus.screen.AccessListScreen;
import xin.vanilla.narcissus.screen.PlayerConfigScreen;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * Narcissus 开发环境功能烟测：验证配置、同步、核心界面和可选传送闭环。
 */
public final class NarcissusUiSmokeRunner {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int START_DELAY_TICKS = 80;
    private static final int STEP_TICKS = 40;
    private static final int CAPTURE_TICK = 30;
    private static final int WORLD_TIMEOUT_TICKS = 1200;
    private static final int SYNC_TIMEOUT_TICKS = 300;
    private static final int TELEPORT_TIMEOUT_TICKS = 1200;
    private static final String HOME_NAME = "__narcissus_smoke_home__";
    private static final String ACCESS_BLACK_FIXTURE = "00000000-0000-0000-0000-000000000001";
    private static final String ACCESS_WHITE_FIXTURE = "00000000-0000-0000-0000-000000000002";

    private static NarcissusUiSmokeRunner instance;

    private final Path outputDir;
    private final NarcissusSmokeOptions options;
    private final List<ScreenStep> preWorldSteps;
    private final List<ScreenStep> worldSteps;
    private Phase phase = Phase.WAITING;
    private int tick;
    private int phaseTick;
    private int stepTick;
    private int stepIndex;
    private long syncGeneration;
    private Vec3 homePosition;
    private String homeDimension;
    private PlayerAccess fixtureAccess;
    private Set<String> originalBlackList;
    private Set<String> originalWhiteList;
    private Set<String> originalAutoTpaList;
    private Set<String> originalAutoTphList;
    private EnumPanelMode originalAccessPanelMode;

    private NarcissusUiSmokeRunner(Path outputDir, NarcissusSmokeOptions options) {
        this.outputDir = outputDir;
        this.options = options;
        this.preWorldSteps = Arrays.asList(
                new ScreenStep("player-preferences", true, client -> client.setScreen(
                        new PlayerConfigScreen(new PlayerConfigScreen.Args()))),
                new ScreenStep("client-config", false, client -> client.setScreen(
                        new ConfigEditorScreen(ClientConfig.get().holder(), new ConfigEditorScreen.Args()))),
                new ScreenStep("common-config", true, client -> client.setScreen(
                        new ConfigEditorScreen(CommonConfig.get().holder(), new ConfigEditorScreen.Args()))),
                new ScreenStep("notification-type-config", true, client -> client.setScreen(
                        new NotificationTypeConfigScreen(new NotificationTypeConfigScreen.Args())),
                        NotificationTypeConfigScreen.class, NarcissusUiSmokeRunner::verifyCleanEscapeCloses)
        );
        this.worldSteps = Arrays.asList(
                new ScreenStep("waypoints", true, client -> ScreenHelper.openScreen()),
                new ScreenStep("player-preferences", true, client -> ScreenHelper.openPlayerTeleportPrefsScreen()),
                new ScreenStep("access-list", true, client -> {
                    installAccessListFixtures(client);
                    ScreenHelper.openAccessListScreen();
                })
        );
    }

    public static void register() {
        NarcissusSmokeOptions options = NarcissusSmokeOptions.from(System::getProperty);
        if (!options.enabled()) {
            return;
        }
        if (!EnvironmentUtils.isDevelopment()) {
            LOGGER.warn("Narcissus UI smoke requested outside development environment; ignored");
            return;
        }
        Minecraft client = Minecraft.getInstance();
        Path outputDir = client.gameDirectory.toPath().resolve("screenshots")
                .resolve("narcissus-ui-smoke")
                .resolve(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()));
        instance = new NarcissusUiSmokeRunner(outputDir, options);
        LOGGER.info("Narcissus UI smoke registered; output directory: {}", outputDir);
    }

    public static void tick(@Nonnull Minecraft client) {
        if (instance != null) {
            instance.runTick(client);
        }
    }

    private void runTick(Minecraft client) {
        if (phase == Phase.FINISHED) {
            return;
        }
        tick++;
        try {
            switch (phase) {
                case WAITING:
                    if (tick >= START_DELAY_TICKS) start(client);
                    break;
                case PRE_WORLD_UI:
                case WORLD_UI:
                    runUiTick(client);
                    break;
                case WORLD_LOADING:
                    runWorldLoadingTick(client);
                    break;
                case CONFIG_SYNC:
                    runConfigSyncTick(client);
                    break;
                case HOME_ADD:
                    runHomeAddTick(client);
                    break;
                case MOVE_AWAY:
                    runMoveAwayTick(client);
                    break;
                case HOME_TELEPORT:
                    runHomeTeleportTick(client);
                    break;
                case HOME_DELETE:
                    runHomeDeleteTick(client);
                    break;
                default:
                    break;
            }
        } catch (Throwable error) {
            fail(client, phase.name().toLowerCase(Locale.ROOT), error);
        }
    }

    private void start(Minecraft client) throws IOException {
        Files.createDirectories(outputDir);
        appendStatus("STARTED " + LocalDateTime.now());
        validateIntegration();
        appendStatus("PASS integration");
        phase = Phase.PRE_WORLD_UI;
        stepIndex = 0;
        enterStep(client, preWorldSteps);
    }

    /**
     * 先访问稳定 API，尽早发现配置或键位注册时序回归。
     */
    private static void validateIntegration() {
        if (ClientConfig.get().holder() == null || CommonConfig.get().holder() == null) {
            throw new IllegalStateException("Narcissus config holder is not registered");
        }
        ClientModEventHandler.TP_HOME_KEY.currentKey();
        ClientModEventHandler.TP_BACK_KEY.currentKey();
        ClientModEventHandler.TP_REQ_YES.currentKey();
        ClientModEventHandler.TP_REQ_NO.currentKey();
        ClientModEventHandler.TP_GRAVE_KEY.currentKey();
        ClientModEventHandler.OPEN_SCREEN_KEY.currentKey();
        ClientModEventHandler.OPEN_ACCESS_LIST_KEY.currentKey();
    }

    private void runUiTick(Minecraft client) {
        stepTick++;
        List<ScreenStep> steps = phase == Phase.PRE_WORLD_UI ? preWorldSteps : worldSteps;
        ScreenStep step = steps.get(stepIndex);
        if (stepTick == CAPTURE_TICK && step.capture) {
            if ("access-list".equals(step.name)) {
                validateAccessListFixtures(client);
            }
            if (step.expectedScreen != null && !step.expectedScreen.isInstance(client.screen)) {
                throw new IllegalStateException("Expected screen " + step.expectedScreen.getName()
                        + " but found " + (client.screen == null ? "null" : client.screen.getClass().getName()));
            }
            int number = phase == Phase.PRE_WORLD_UI ? stepIndex + 1 : preWorldSteps.size() + stepIndex + 1;
            capture(client, String.format(Locale.ROOT, "%02d-%s", number, step.name));
        }
        if (stepTick == CAPTURE_TICK + 2 && step.verifier != null) {
            step.verifier.accept(client);
            appendStatus("PASS " + step.name + "-behavior");
        }
        if (stepTick < STEP_TICKS) {
            return;
        }
        stepIndex++;
        if (stepIndex < steps.size()) {
            enterStep(client, steps);
        } else if (phase == Phase.PRE_WORLD_UI) {
            beginWorldSmoke(client);
        } else {
            restoreAccessListFixtures();
            beginConfigSync(client);
        }
    }

    /**
     * 临时填充两列数据，让截图真正覆盖列表项绘制，结束后会恢复原值。
     */
    private void installAccessListFixtures(Minecraft client) {
        fixtureAccess = PlayerTeleportData.getData(client.player).getAccess();
        originalBlackList = new HashSet<>(fixtureAccess.getBlackList());
        originalWhiteList = new HashSet<>(fixtureAccess.getWhiteList());
        originalAutoTpaList = new HashSet<>(fixtureAccess.getAutoTpaList());
        originalAutoTphList = new HashSet<>(fixtureAccess.getAutoTphList());
        originalAccessPanelMode = ClientConfig.get().client().accessListScreenPanelMode();
        ClientConfig.get().client().accessListScreenPanelMode(EnumPanelMode.COLUMNS);
        fixtureAccess.getBlackList().add(ACCESS_BLACK_FIXTURE);
        fixtureAccess.getWhiteList().add(ACCESS_WHITE_FIXTURE);
        fixtureAccess.getAutoTpaList().add(ACCESS_WHITE_FIXTURE);
        appendStatus("SEED access-list black=1 white=1 mode=COLUMNS");
    }

    private void validateAccessListFixtures(Minecraft client) {
        if (!(client.screen instanceof AccessListScreen)) {
            throw new IllegalStateException("Access-list screen did not open");
        }
        PlayerAccess access = PlayerTeleportData.getData(client.player).getAccess();
        if (!access.getBlackList().contains(ACCESS_BLACK_FIXTURE)
                || !access.getWhiteList().contains(ACCESS_WHITE_FIXTURE)) {
            throw new IllegalStateException("Access-list fixtures disappeared before capture");
        }
        if (ClientConfig.get().client().accessListScreenPanelMode() != EnumPanelMode.COLUMNS) {
            throw new IllegalStateException("Access-list smoke did not keep the two-column layout");
        }
        appendStatus("PASS access-list-fixture-data");
    }

    private void restoreAccessListFixtures() {
        if (fixtureAccess == null) {
            return;
        }
        restoreSet(fixtureAccess.getBlackList(), originalBlackList);
        restoreSet(fixtureAccess.getWhiteList(), originalWhiteList);
        restoreSet(fixtureAccess.getAutoTpaList(), originalAutoTpaList);
        restoreSet(fixtureAccess.getAutoTphList(), originalAutoTphList);
        if (originalAccessPanelMode != null) {
            ClientConfig.get().client().accessListScreenPanelMode(originalAccessPanelMode);
        }
        fixtureAccess = null;
        originalAccessPanelMode = null;
    }

    private static void restoreSet(Set<String> target, Set<String> original) {
        target.clear();
        if (original != null) {
            target.addAll(original);
        }
    }

    private void enterStep(Minecraft client, List<ScreenStep> steps) {
        stepTick = 0;
        ScreenStep step = steps.get(stepIndex);
        step.opener.accept(client);
        appendStatus("OPEN " + step.name);
    }

    private void beginWorldSmoke(Minecraft client) {
        if (options.worldName().isEmpty()) {
            finish(client);
            return;
        }
        client.setScreen(null);
        syncGeneration = NarcissusClientSyncState.playerDataGeneration();
        phase = Phase.WORLD_LOADING;
        phaseTick = 0;
        appendStatus("LOAD world " + options.worldName());
        client.createWorldOpenFlows().loadLevel(client.screen, options.worldName());
    }

    private void runWorldLoadingTick(Minecraft client) {
        phaseTick++;
        boolean loaded = client.player != null && client.level != null
                && client.getSingleplayerServer() != null && client.screen == null;
        if (loaded && NarcissusClientSyncState.playerDataGeneration() > syncGeneration) {
            appendStatus("PASS world-load");
            appendStatus("PASS login-sync");
            phase = Phase.WORLD_UI;
            stepIndex = 0;
            enterStep(client, worldSteps);
            return;
        }
        if (phaseTick >= WORLD_TIMEOUT_TICKS) {
            throw new IllegalStateException("Timed out loading or synchronizing world " + options.worldName());
        }
    }

    private void beginConfigSync(Minecraft client) {
        client.setScreen(null);
        syncGeneration = NarcissusClientSyncState.playerDataGeneration();
        PacketUtils.sendPacketToServer(new PlayerConfigSyncToServer(
                PlayerTeleportData.getData(client.player).writeTeleportCountdownToNbt()));
        appendStatus("SEND player-config-roundtrip");
        phase = Phase.CONFIG_SYNC;
        phaseTick = 0;
    }

    private void runConfigSyncTick(Minecraft client) {
        phaseTick++;
        if (NarcissusClientSyncState.playerDataGeneration() > syncGeneration) {
            appendStatus("PASS player-config-roundtrip");
            if (options.teleportEnabled()) {
                beginHomeAdd(client);
            } else {
                appendStatus("SKIP teleport-flow (disabled)");
                finish(client);
            }
        } else if (phaseTick >= SYNC_TIMEOUT_TICKS) {
            throw new IllegalStateException("Player config was not synchronized back");
        }
    }

    private void beginHomeAdd(Minecraft client) {
        homePosition = client.player.position();
        homeDimension = client.player.level.dimension().location().toString();
        if (hasSmokeHome(client)) {
            PacketUtils.sendPacketToServer(new WaypointDelToServer(0, HOME_NAME, homeDimension));
        }
        syncGeneration = NarcissusClientSyncState.playerDataGeneration();
        PacketUtils.sendPacketToServer(new WaypointAddHomeToServer(HOME_NAME));
        appendStatus("SEND temporary-home-add");
        phase = Phase.HOME_ADD;
        phaseTick = 0;
    }

    private void runHomeAddTick(Minecraft client) {
        phaseTick++;
        if (hasSmokeHome(client) && NarcissusClientSyncState.playerDataGeneration() > syncGeneration) {
            appendStatus("PASS temporary-home-add");
            moveServerPlayerAway(client);
            phase = Phase.MOVE_AWAY;
            phaseTick = 0;
        } else if (phaseTick >= SYNC_TIMEOUT_TICKS) {
            throw new IllegalStateException("Temporary home was not added");
        }
    }

    private void moveServerPlayerAway(Minecraft client) {
        client.getSingleplayerServer().execute(() -> {
            ServerPlayer player = client.getSingleplayerServer().getPlayerList().getPlayer(client.player.getUUID());
            if (player != null) {
                player.teleportTo(homePosition.x + 4.0D, homePosition.y, homePosition.z);
            }
        });
    }

    private void runMoveAwayTick(Minecraft client) {
        phaseTick++;
        if (client.player.position().distanceTo(homePosition) >= 2.0D) {
            PacketUtils.sendPacketToServer(new WaypointTeleportToServer(
                    EnumTeleportType.TP_HOME, HOME_NAME, homeDimension));
            appendStatus("SEND temporary-home-teleport");
            phase = Phase.HOME_TELEPORT;
            phaseTick = 0;
        } else if (phaseTick >= SYNC_TIMEOUT_TICKS) {
            throw new IllegalStateException("Could not move player away for teleport validation");
        }
    }

    private void runHomeTeleportTick(Minecraft client) {
        phaseTick++;
        if (client.player.position().distanceTo(homePosition) < 1.0D) {
            appendStatus("PASS temporary-home-teleport");
            PacketUtils.sendPacketToServer(new WaypointDelToServer(0, HOME_NAME, homeDimension));
            appendStatus("SEND temporary-home-delete");
            phase = Phase.HOME_DELETE;
            phaseTick = 0;
        } else if (phaseTick >= TELEPORT_TIMEOUT_TICKS) {
            throw new IllegalStateException("Temporary home teleport did not complete");
        }
    }

    private void runHomeDeleteTick(Minecraft client) {
        phaseTick++;
        if (!hasSmokeHome(client)) {
            appendStatus("PASS temporary-home-delete");
            finish(client);
        } else if (phaseTick >= SYNC_TIMEOUT_TICKS) {
            throw new IllegalStateException("Temporary home was not deleted");
        }
    }

    private static boolean hasSmokeHome(Minecraft client) {
        return PlayerTeleportData.getData(client.player).getHomeCoordinate().keySet().stream()
                .anyMatch(key -> HOME_NAME.equals(key.value()));
    }

    private static void verifyCleanEscapeCloses(Minecraft client) {
        if (client.screen == null) {
            throw new IllegalStateException("Notification type config disappeared before ESC");
        }
        Object openedScreen = client.screen;
        boolean handled = client.screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
        if (!handled || client.screen == openedScreen) {
            throw new IllegalStateException("Clean notification type config did not close on ESC"
                    + " (handled=" + handled + ", current="
                    + (client.screen == null ? "null" : client.screen.getClass().getName()) + ")");
        }
    }

    private void capture(Minecraft client, String name) {
        Path file = outputDir.resolve(name + ".png");
        try (NativeImage image = Screenshot.takeScreenshot(client.getMainRenderTarget())) {
            image.writeToFile(file);
            appendStatus("PASS " + name);
        } catch (IOException error) {
            throw new IllegalStateException("Could not save screenshot " + name, error);
        }
    }

    private void finish(Minecraft client) {
        phase = Phase.FINISHED;
        restoreAccessListFixtures();
        appendStatus("FINISHED " + LocalDateTime.now());
        LOGGER.info("Narcissus UI smoke finished; output: {}", outputDir);
        client.setScreen(null);
        if (options.exitOnFinish()) {
            client.stop();
        }
    }

    private void fail(Minecraft client, String step, Throwable error) {
        phase = Phase.FINISHED;
        restoreAccessListFixtures();
        appendStatus("FAILED " + step + ": " + error);
        LOGGER.error("Narcissus UI smoke failed at {}", step, error);
        if (options.exitOnFinish()) {
            client.stop();
        }
    }

    private void appendStatus(String line) {
        try {
            Files.write(outputDir.resolve("status.txt"), (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException error) {
            LOGGER.warn("Failed to write Narcissus UI smoke status", error);
        }
    }

    private static final class ScreenStep {
        private final String name;
        private final boolean capture;
        private final Consumer<Minecraft> opener;
        private final Class<?> expectedScreen;
        private final Consumer<Minecraft> verifier;

        private ScreenStep(String name, boolean capture, Consumer<Minecraft> opener) {
            this(name, capture, opener, null, null);
        }

        private ScreenStep(String name, boolean capture, Consumer<Minecraft> opener, Class<?> expectedScreen) {
            this(name, capture, opener, expectedScreen, null);
        }

        private ScreenStep(String name, boolean capture, Consumer<Minecraft> opener,
                           Class<?> expectedScreen, Consumer<Minecraft> verifier) {
            this.name = name;
            this.capture = capture;
            this.opener = opener;
            this.expectedScreen = expectedScreen;
            this.verifier = verifier;
        }
    }

    private enum Phase {
        WAITING, PRE_WORLD_UI, WORLD_LOADING, WORLD_UI, CONFIG_SYNC,
        HOME_ADD, MOVE_AWAY, HOME_TELEPORT, HOME_DELETE, FINISHED
    }
}
