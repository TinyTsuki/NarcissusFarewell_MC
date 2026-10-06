package xin.vanilla.narcissus.internal.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.enums.EnumWhiteListMode;
import xin.vanilla.narcissus.internal.client.NarcissusClientSyncState;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeConfigs;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeFixture;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus;
import xin.vanilla.narcissus.network.packet.AccessListEditToServer;
import xin.vanilla.narcissus.network.packet.PlayerConfigSyncToServer;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 自动连接独立服务端并验证玩家配置与访问名单的真实网络往返。
 */
public final class NarcissusNetworkSmokeClientRunner {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final long UI_CYCLE_NANOS = TimeUnit.MILLISECONDS.toNanos(500);

    private static NarcissusNetworkSmokeClientRunner instance;

    private State state = State.CONNECT;
    private int ticks;
    private long syncGeneration;
    private boolean disconnectRequested;
    private boolean configsVerified;
    private NarcissusNetworkSmokeClientPlan plan;
    private NarcissusNetworkSmokeScreens ui;
    private ReflectiveClientSparkProfile spark;
    private long uiStartedAt;
    private long lastUiCycleAt;
    private int uiCycles;
    private final NarcissusNetworkSmokeNotificationsCheck notifications = new NarcissusNetworkSmokeNotificationsCheck();

    private NarcissusNetworkSmokeClientRunner() {
    }

    public static void register() {
        if (NarcissusNetworkSmokeStatus.enabled() && instance == null) {
            instance = new NarcissusNetworkSmokeClientRunner();
        }
    }

    public static void tick(@Nonnull Minecraft client) {
        if (instance != null) {
            instance.runTick(client);
        }
    }

    private void runTick(Minecraft client) {
        if (state == State.FINISHED) return;
        try {
            long now = System.nanoTime();
            if (plan == null) plan = new NarcissusNetworkSmokeClientPlan(NarcissusNetworkSmokeStatus.phase(), now);
            if (plan.timedOut(now)) throw new IllegalStateException("Timed out in state " + state);
            readServerStatus();
            if (state != State.CONNECT && state != State.LOGIN_SYNC && !disconnectRequested && !remote(client)) {
                throw new IllegalStateException("Remote connection lost in " + state);
            }
            ticks++;
            switch (state) {
                case CONNECT:
                    if (ticks >= 20) {
                        connect(client);
                    }
                    break;
                case LOGIN_SYNC:
                    waitForLoginSync(client);
                    break;
                case WAIT_C2S_READY:
                    waitForCustomChannel(client);
                    break;
                case CONFIG_ECHO:
                    waitForConfigEcho(client);
                    break;
                case ACCESS_ECHO:
                    waitForAccessEcho(client);
                    break;
                case WAIT_SUSTAINED:
                    waitForSustainedWorkload(client);
                    break;
                case UI_WORKLOAD:
                    runUiWorkload(client);
                    break;
                case WAIT_SERVER:
                    waitForServerShutdown(client);
                    break;
                case FINISHED:
                    break;
                default:
                    throw new IllegalStateException("Unknown client state " + state);
            }
        } catch (Throwable error) {
            fail(client, error.toString());
        }
    }

    private void connect(Minecraft client) {
        if (client.getOverlay() != null) return;
        String host = System.getProperty("narcissus.networkSmoke.host", "127.0.0.1");
        int port = Integer.getInteger("narcissus.networkSmoke.port", 25576);
        ServerData server = new ServerData("Narcissus Network Smoke", host + ":" + port, ServerData.Type.OTHER);
        // Smoke 使用普通多人服务器连接，不进入 Quick Play 流程。
        ConnectScreen.startConnecting(client.screen, client, ServerAddress.parseString(server.ip), server, false, null);
        NarcissusNetworkSmokeStatus.append("CONNECT " + host + ":" + port);
        transition(State.LOGIN_SYNC);
    }

    private static boolean remote(Minecraft client) {
        return client.player != null && client.level != null && client.getConnection() != null
                && client.getConnection().getConnection().isConnected()
                && !client.getConnection().getConnection().isMemoryConnection()
                && client.getSingleplayerServer() == null;
    }

    private void waitForLoginSync(Minecraft client) {
        if (!remote(client) || NarcissusClientSyncState.playerDataGeneration() <= 0L) {
            return;
        }
        NarcissusNetworkSmokeStatus.append("PASS remote-login-sync");
        transition(State.WAIT_C2S_READY);
    }

    private void waitForCustomChannel(Minecraft client) {
        if (client.player == null || !NarcissusNetworkSmokeClientPlan.isCustomChannelReady(
                ticks, PlayerUtils.isRemoteServerModInstalled(client.player, NarcissusFarewell.MODID))) return;
        if (!notifications.verifyWhenReceived(client, NarcissusNetworkSmokeStatus.phase())) return;
        if ("phase-one".equals(NarcissusNetworkSmokeStatus.phase())) {
            CompoundTag countdowns = new CompoundTag();
            countdowns.putInt(EnumTeleportType.TP_HOME.name(), NarcissusNetworkSmokeFixture.COUNTDOWN);
            syncGeneration = NarcissusClientSyncState.playerDataGeneration();
            PacketUtils.sendPacketToServer(new PlayerConfigSyncToServer(countdowns));
            transition(State.CONFIG_ECHO);
        } else if ("phase-two".equals(NarcissusNetworkSmokeStatus.phase())) {
            verifyPersistedClientData(client);
            transition(State.WAIT_SERVER);
        } else {
            throw new IllegalStateException("Unknown network smoke phase: " + NarcissusNetworkSmokeStatus.phase());
        }
    }

    private void waitForConfigEcho(Minecraft client) {
        if (NarcissusClientSyncState.playerDataGeneration() <= syncGeneration) {
            return;
        }
        PlayerTeleportData data = PlayerTeleportData.getData(client.player);
        if (data.getTeleportCountdownSeconds(EnumTeleportType.TP_HOME) != NarcissusNetworkSmokeFixture.COUNTDOWN) {
            return;
        }
        NarcissusNetworkSmokeStatus.append("PASS config-roundtrip");
        syncGeneration = NarcissusClientSyncState.playerDataGeneration();
        PacketUtils.sendPacketToServer(new AccessListEditToServer(
                2, EnumWhiteListMode.AUTO_ACCEPT_TPA.name(), NarcissusNetworkSmokeFixture.ACCESS_UUID));
        transition(State.ACCESS_ECHO);
    }

    private void waitForAccessEcho(Minecraft client) {
        if (NarcissusClientSyncState.playerDataGeneration() <= syncGeneration) {
            return;
        }
        xin.vanilla.narcissus.data.PlayerAccess access = PlayerTeleportData.getData(client.player).getAccess();
        if (!access.getWhiteList().contains(NarcissusNetworkSmokeFixture.ACCESS_UUID)
                || !access.getAutoTpaList().contains(NarcissusNetworkSmokeFixture.ACCESS_UUID)) {
            syncGeneration = NarcissusClientSyncState.playerDataGeneration();
            return;
        }
        NarcissusNetworkSmokeFixture.verifyAccess(access);
        NarcissusNetworkSmokeStatus.append("PASS access-list-roundtrip");
        transition(State.WAIT_SUSTAINED);
    }

    private void waitForSustainedWorkload(Minecraft client) {
        if (!notifications.verified()) return;
        if (!plan.readyForUi() || client.getOverlay() != null) return;
        // Status and packet delivery are independent. Do not sample an empty, not-yet-synchronized list.
        if (!NarcissusNetworkSmokeScreens.hasSyncedHomes(client)) return;
        NarcissusNetworkSmokeStatus.append("PASS populated-waypoints-client homes=48 source=server-sync");
        ui = new NarcissusNetworkSmokeScreens(() -> spark != null && !spark.future.isDone());
        ui.open(client);
        spark = ReflectiveClientSparkProfile.start();
        spark.verifyInterval(plan);
        uiStartedAt = System.nanoTime();
        lastUiCycleAt = uiStartedAt;
        uiCycles = 1;
        NarcissusNetworkSmokeStatus.append("PASS client-ui-spark-profiler-active");
        transition(State.UI_WORKLOAD);
    }

    private void runUiWorkload(Minecraft client) throws IOException {
        plan.requireServerSamplingWindow();
        long now = System.nanoTime();
        boolean ready = ui.readyForCycle(client);
        if (!spark.future.isDone() && ready && now - lastUiCycleAt >= UI_CYCLE_NANOS) {
            ui.runCycle(client, ++uiCycles);
            lastUiCycleAt = System.nanoTime();
        }
        if (spark.writeWhenComplete()) {
            readServerStatus();
            spark.verifyInterval(plan);
            plan.requireServerSamplingWindow();
            NarcissusNetworkSmokeStatus.append("PASS client-ui-spark-report-written");
            NarcissusNetworkSmokeStatus.append("INFO client-ui-final requested-cycles=" + uiCycles
                    + " completed-cycles=" + ui.completedCycles() + " " + ui.summary()
                    + " " + ui.diagnostics());
        }
        if (NarcissusNetworkSmokeClientPlan.uiComplete(spark.future.isDone(), spark.written,
                now - uiStartedAt, ui.verified())) {
            NarcissusNetworkSmokeStatus.append("PASS client-ui-sustained-workload cycles=" + ui.completedCycles()
                    + " requested-cycles=" + uiCycles + " duration-wall-ns=" + (now - uiStartedAt) + " " + ui.summary());
            ui.close(client);
            transition(State.WAIT_SERVER);
        }
    }

    private void waitForServerShutdown(Minecraft client) {
        if (!notifications.verified()) return;
        if (!plan.canFinish(ui != null && ui.verified(), spark != null && spark.written)) return;
        if (!configsVerified) {
            NarcissusNetworkSmokeConfigs.completeClient(NarcissusNetworkSmokeStatus.phase());
            configsVerified = true;
        }
        if (!disconnectRequested && client.getConnection() != null) {
            disconnectRequested = true;
            client.getConnection().getConnection().disconnect(Component.literal("Narcissus network smoke complete"));
            return;
        }
        if (client.getConnection() != null && client.getConnection().getConnection().isConnected()) return;
        finish(client, NarcissusNetworkSmokeStatus.phase());
    }

    private void readServerStatus() throws IOException {
        Path serverStatus = NarcissusNetworkSmokeStatus.serverStatusPath(
                configuredPath(NarcissusNetworkSmokeStatus.STATUS_PROPERTY));
        if (Files.isRegularFile(serverStatus)) plan.accept(Files.readAllLines(serverStatus, StandardCharsets.UTF_8));
    }

    private static Path configuredPath(String property) {
        String configured = System.getProperty(property, "").trim();
        if (configured.isEmpty()) throw new IllegalStateException("Missing " + property);
        return Paths.get(configured).toAbsolutePath();
    }

    private void transition(State next) {
        state = next;
        plan.transition(System.nanoTime());
        ticks = 0;
    }

    private void verifyPersistedClientData(Minecraft client) {
        PlayerTeleportData data = PlayerTeleportData.getData(client.player);
        if (data.getTeleportCountdownSeconds(EnumTeleportType.TP_HOME) != NarcissusNetworkSmokeFixture.COUNTDOWN) {
            throw new IllegalStateException("Persisted player countdown was not synchronized");
        }
        NarcissusNetworkSmokeStatus.append("PASS persisted-player-config-client");
        NarcissusNetworkSmokeFixture.verifyAccess(data.getAccess());
        NarcissusNetworkSmokeStatus.append("PASS persisted-access-list-client");
        if (!NarcissusNetworkSmokeScreens.hasSyncedHomes(client)) {
            throw new IllegalStateException("Persisted 48 player homes were not synchronized");
        }
        NarcissusNetworkSmokeStatus.append("PASS persisted-waypoints-client homes=48 source=server-sync");
    }

    private void finish(Minecraft client, String phase) {
        state = State.FINISHED;
        NarcissusNetworkSmokeStatus.append("FINISHED " + phase);
        LOGGER.info("Narcissus network smoke client finished {}", phase);
        client.stop();
    }

    private void fail(Minecraft client, String message) {
        state = State.FINISHED;
        if (ui != null) {
            try {
                ui.close(client);
            } catch (Throwable cleanupError) {
                LOGGER.warn("Unable to restore client smoke UI state", cleanupError);
            }
        }
        NarcissusNetworkSmokeStatus.append("FAIL client " + message);
        LOGGER.error("Narcissus network smoke client failed: {}", message);
        client.stop();
    }

    private enum State {
        CONNECT,
        LOGIN_SYNC,
        WAIT_C2S_READY,
        CONFIG_ECHO,
        ACCESS_ECHO,
        WAIT_SUSTAINED,
        UI_WORKLOAD,
        WAIT_SERVER,
        FINISHED
    }

    /**
     * Spark client plugin, Render thread only, native protobuf export.
     */
    private static final class ReflectiveClientSparkProfile {
        private final Object sampler;
        private final Future<?> future;
        private final Object platform;
        private final Object plugin;
        private final Path report;
        private boolean written;

        private ReflectiveClientSparkProfile(Object sampler, Future<?> future, Object platform, Object plugin, Path report) {
            this.sampler = sampler;
            this.future = future;
            this.platform = platform;
            this.plugin = plugin;
            this.report = report;
        }

        private static ReflectiveClientSparkProfile start() {
            try {
                Path report = configuredPath("narcissus.networkSmoke.clientSparkReport");
                Object plugin = plugin();
                ClassLoader loader = plugin.getClass().getClassLoader();
                Field platformField = base(plugin).getDeclaredField("platform");
                platformField.setAccessible(true);
                Object platform = platformField.get(plugin);
                Class<?> builderType = Class.forName("me.lucko.spark.common.sampler.SamplerBuilder", true, loader);
                Object builder = builderType.getConstructor().newInstance();
                builderType.getMethod("samplingInterval", double.class).invoke(builder, 4.0D);
                builderType.getMethod("completeAfter", long.class, TimeUnit.class).invoke(builder, 20L, TimeUnit.SECONDS);
                builderType.getMethod("forceJavaSampler", boolean.class).invoke(builder, true);
                Class<?> dumperType = Class.forName("me.lucko.spark.common.sampler.ThreadDumper", true, loader);
                Thread renderThread = Thread.currentThread();
                if (!"Render thread".equals(renderThread.getName())) {
                    throw new IllegalStateException("Client smoke is not running on Render thread");
                }
                Class<?> specific = Class.forName("me.lucko.spark.common.sampler.ThreadDumper$Specific", true, loader);
                Object dumper = specific.getConstructor(Thread.class).newInstance(renderThread);
                builderType.getMethod("threadDumper", dumperType).invoke(builder, dumper);
                Class<?> grouperType = Class.forName("me.lucko.spark.common.sampler.ThreadGrouper", true, loader);
                builderType.getMethod("threadGrouper", Supplier.class).invoke(builder, grouperType.getField("BY_POOL").get(null));
                // SamplerBuilder.start starts sampling; a second sampler.start would double the samples.
                Object sampler = method(builderType, "start", 1).invoke(builder, platform);
                Future<?> future = (Future<?>) method(sampler.getClass(), "getFuture", 0).invoke(sampler);
                return new ReflectiveClientSparkProfile(sampler, future, platform, plugin, report);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to start client Spark sampler", error);
            }
        }

        private boolean writeWhenComplete() {
            if (written || !future.isDone()) return false;
            try {
                future.get();
                ClassLoader loader = plugin.getClass().getClassLoader();
                Class<?> propsType = Class.forName("me.lucko.spark.common.sampler.Sampler$ExportProps", true, loader);
                Object props = propsType.getConstructor().newInstance();
                Class<?> senderData = Class.forName("me.lucko.spark.common.command.sender.CommandSender$Data", true, loader);
                propsType.getMethod("creator", senderData).invoke(props,
                        senderData.getConstructor(String.class, UUID.class).newInstance("Narcissus client UI smoke", null));
                propsType.getMethod("comment", String.class).invoke(props, "Narcissus client UI smoke");
                Class<?> strategyType = Class.forName("me.lucko.spark.common.sampler.java.MergeStrategy", true, loader);
                Object lookup = base(plugin).getMethod("createClassSourceLookup").invoke(plugin);
                propsType.getMethod("mergeStrategy", strategyType).invoke(props, strategyType.getField("SAME_METHOD").get(null));
                propsType.getMethod("classSourceLookup", Supplier.class).invoke(props, (Supplier<Object>) () -> lookup);
                Object proto = method(sampler.getClass(), "toProto", 2).invoke(sampler, platform, props);
                byte[] bytes = (byte[]) proto.getClass().getMethod("toByteArray").invoke(proto);
                if (bytes.length == 0) throw new IllegalStateException("Client Spark report was empty");
                Files.createDirectories(report.getParent());
                Files.write(report, bytes);
                written = true;
                return true;
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted writing client Spark report", error);
            } catch (ReflectiveOperationException | IOException | java.util.concurrent.ExecutionException error) {
                throw new IllegalStateException("Unable to write client Spark report", error);
            }
        }

        private void verifyInterval(NarcissusNetworkSmokeClientPlan plan) {
            try {
                long start = (Long) method(sampler.getClass(), "getStartTime", 0).invoke(sampler);
                long end = (Long) method(sampler.getClass(), "getAutoEndTime", 0).invoke(sampler);
                plan.requireSampleInterval(start, end);
                NarcissusNetworkSmokeStatus.append("PASS client-sampling-window start-ms=" + start + " end-ms=" + end);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Cannot read actual Spark sampling interval", error);
            }
        }

        private static Object plugin() throws ReflectiveOperationException {
            Field listeners = NeoForge.EVENT_BUS.getClass().getDeclaredField("listeners");
            listeners.setAccessible(true);
            for (Object candidate : ((Map<?, ?>) listeners.get(NeoForge.EVENT_BUS)).keySet()) {
                if (candidate != null && candidate.getClass().getName()
                        .equals("me.lucko.spark.neoforge.plugin.NeoForgeClientSparkPlugin")) return candidate;
            }
            throw new IllegalStateException("Spark client plugin was not registered");
        }

        private static Class<?> base(Object plugin) {
            Class<?> type = plugin.getClass();
            while (type != null && !type.getName().equals("me.lucko.spark.neoforge.plugin.NeoForgeSparkPlugin"))
                type = type.getSuperclass();
            if (type == null) throw new IllegalStateException("Spark base plugin was not found");
            return type;
        }

        private static Method method(Class<?> type, String name, int parameters) {
            for (Method candidate : type.getMethods()) {
                if (candidate.getName().equals(name) && candidate.getParameterCount() == parameters) return candidate;
            }
            throw new IllegalStateException("Missing Spark method " + type.getName() + '#' + name);
        }
    }
}
