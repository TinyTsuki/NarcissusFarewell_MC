package xin.vanilla.narcissus.internal.server.dev;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import xin.vanilla.banira.api.BaniraServer;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeConfigs;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeFixture;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeNotifications;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/** Dedicated smoke: real remote synchronization, sustained mod-created actors, restart verification. */
public final class NarcissusNetworkSmokeServerRunner {
    private static boolean ready;
    private static boolean finished;
    private static boolean measuredFinished;
    private static int shutdownTicks;
    private static long startedAt;
    private static long fixtureReadyAt;
    private static NarcissusMeasuredTeleports measured;
    private static ReflectiveSparkProfile sparkProfile;
    private static NarcissusNetworkSmokeNotifications notifications;
    private static boolean notificationsVerified;

    private NarcissusNetworkSmokeServerRunner() { }

    public static void register() {
        if (NarcissusNetworkSmokeStatus.enabled()) {
            NeoForge.EVENT_BUS.addListener(NarcissusNetworkSmokeServerRunner::onServerTick);
        }
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        try {
            MinecraftServer server = BaniraServer.currentAs(MinecraftServer.class);
            if (server == null || !server.isRunning()) return;
            if (startedAt == 0) startedAt = System.nanoTime();
            if (System.nanoTime() - startedAt > TimeUnit.SECONDS.toNanos(300)) {
                throw new IllegalStateException("Server smoke wall-clock limit exceeded");
            }
            if (sparkProfile != null && sparkProfile.writeWhenComplete()) {
                NarcissusNetworkSmokeStatus.append("PASS spark-report-written");
            }
            if (finished) { shutdownWhenSaved(server); return; }
            if (!ready) {
                NarcissusMeasuredTeleports.verifyProvider();
                ready = true;
                NarcissusNetworkSmokeStatus.append("PASS server-ready");
            }
            ServerPlayer player = server.getPlayerList().getPlayerByName("NetworkSmoke");
            if (player == null) return;
            if (notifications == null) notifications = new NarcissusNetworkSmokeNotifications(NarcissusNetworkSmokeStatus.phase());
            if (!notifications.sendWhenReady(player) || !waitForNotificationCheck()) return;
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            if ("phase-two".equals(NarcissusNetworkSmokeStatus.phase())) {
                NarcissusMeasuredTeleports.verifyRestart(player);
                verifyPlayerData(data);
                NarcissusNetworkSmokeStatus.append("PASS persisted-player-config");
                NarcissusNetworkSmokeStatus.append("PASS persisted-access-list");
                finish("phase-two");
                return;
            }
            if (!"phase-one".equals(NarcissusNetworkSmokeStatus.phase())) {
                throw new IllegalStateException("Unknown smoke phase");
            }
            if (measured == null) {
                if (data.getTeleportCountdownSeconds(EnumTeleportType.TP_HOME) != NarcissusNetworkSmokeFixture.COUNTDOWN) return;
                try { NarcissusNetworkSmokeFixture.verifyAccess(data.getAccess()); }
                catch (IllegalStateException awaitingEcho) { return; }
                measured = new NarcissusMeasuredTeleports(player);
                fixtureReadyAt = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
                return;
            }
            if (sparkProfile == null) {
                if (System.nanoTime() < fixtureReadyAt) return;
                sparkProfile = ReflectiveSparkProfile.start();
                NarcissusNetworkSmokeStatus.append("PASS spark-profiler-active");
                NarcissusNetworkSmokeStatus.append("PASS sustained-ready");
                return;
            }
            if (!measured.complete()) {
                measured.tick(sparkProfile::active);
                if (!sparkProfile.active()) throw new IllegalStateException("Workload exceeded the active sampling window");
                return;
            }
            if (!sparkProfile.written()) return;
            if (!measuredFinished) {
                measured.finish();
                measuredFinished = true;
            }
            if (!measured.actorsRemoved()) return;
            NarcissusNetworkSmokeStatus.append("PASS advanced-fake-player-cleanup");
            verifyPlayerData(data);
            data.save();
            NarcissusNetworkSmokeStatus.append("PASS server-config-roundtrip");
            NarcissusNetworkSmokeStatus.append("PASS server-access-list-roundtrip");
            finish("phase-one");
        } catch (Throwable error) {
            finished = true;
            NarcissusNetworkSmokeStatus.append("FAIL server " + error);
            throw error;
        }
    }

    private static void verifyPlayerData(PlayerTeleportData data) {
        if (data.getTeleportCountdownSeconds(EnumTeleportType.TP_HOME) != NarcissusNetworkSmokeFixture.COUNTDOWN) {
            throw new IllegalStateException("Countdown sentinel was not preserved");
        }
        NarcissusNetworkSmokeFixture.verifyAccess(data.getAccess());
    }

    private static boolean waitForNotificationCheck() {
        if (notificationsVerified) return true;
        String phase = NarcissusNetworkSmokeStatus.phase();
        Path status = Paths.get(System.getProperty(NarcissusNetworkSmokeStatus.STATUS_PROPERTY, "")).toAbsolutePath();
        if (!status.getFileName().toString().equals("server-" + phase + ".status")) {
            throw new IllegalStateException("Unexpected smoke server status path " + status);
        }
        Path clientStatus = status.resolveSibling("client-" + phase + ".status");
        try {
            if (!Files.isRegularFile(clientStatus) || !NarcissusNetworkSmokeNotifications.clientVerified(
                    phase, Files.readAllLines(clientStatus, StandardCharsets.UTF_8))) return false;
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot read notification client acknowledgement", error);
        }
        notificationsVerified = true;
        NarcissusNetworkSmokeStatus.append("PASS notification-client-verified phase=" + phase);
        return true;
    }

    private static void finish(String phase) {
        if (!notificationsVerified) throw new IllegalStateException("Cannot finish before notification client check");
        NarcissusNetworkSmokeConfigs.completeServer(phase);
        finished = true;
        NarcissusNetworkSmokeStatus.append("FINISHED " + phase);
    }

    private static void shutdownWhenSaved(MinecraftServer server) {
        if (server.getPlayerList().getPlayerCount() > 0) { shutdownTicks = 0; return; }
        if (++shutdownTicks >= 40) {
            NarcissusNetworkSmokeStatus.append("PASS server-shutdown");
            server.halt(false);
        }
    }

    /** Uses the NeoForge 1.21.1 Spark ExportProps API. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class ReflectiveSparkProfile {
        private final Object sampler;
        private final Future<?> future;
        private final Object platform;
        private final Object plugin;
        private final MinecraftServer server;
        private final Path report;
        private boolean written;

        private ReflectiveSparkProfile(Object sampler, Future<?> future, Object platform, Object plugin,
                                       MinecraftServer server, Path report) {
            this.sampler = sampler;
            this.future = future;
            this.platform = platform;
            this.plugin = plugin;
            this.server = server;
            this.report = report;
        }

        private static ReflectiveSparkProfile start() {
            try {
                String configured = NarcissusNetworkSmokeStatus.sparkReport();
                if (configured.isEmpty()) {
                    throw new IllegalStateException("Missing " + NarcissusNetworkSmokeStatus.SPARK_REPORT_PROPERTY);
                }
                MinecraftServer server = BaniraServer.currentAs(MinecraftServer.class);
                Object plugin = plugin();
                Class<?> base = base(plugin);
                Field platformField = base.getDeclaredField("platform");
                platformField.setAccessible(true);
                Object platform = platformField.get(plugin);
                ClassLoader loader = plugin.getClass().getClassLoader();
                Class<?> builderType = Class.forName("me.lucko.spark.common.sampler.SamplerBuilder", true, loader);
                Object builder = builderType.getConstructor().newInstance();
                builderType.getMethod("samplingInterval", double.class).invoke(builder, 10.0D);
                builderType.getMethod("completeAfter", long.class, TimeUnit.class)
                        .invoke(builder, 60L, TimeUnit.SECONDS);
                builderType.getMethod("forceJavaSampler", boolean.class).invoke(builder, true);
                Class<?> dumperType = Class.forName("me.lucko.spark.common.sampler.ThreadDumper", true, loader);
                builderType.getMethod("threadDumper", dumperType)
                        .invoke(builder, dumperType.getField("ALL").get(null));
                Class<?> grouperType = Class.forName("me.lucko.spark.common.sampler.ThreadGrouper", true, loader);
                builderType.getMethod("threadGrouper", Supplier.class).invoke(builder, grouperType.getField("BY_POOL").get(null));
                Object sampler = method(builderType, "start", 1).invoke(builder, platform);
                Future<?> future = (Future<?>) method(sampler.getClass(), "getFuture", 0).invoke(sampler);
                long start = (Long) method(sampler.getClass(), "getStartTime", 0).invoke(sampler);
                long end = (Long) method(sampler.getClass(), "getAutoEndTime", 0).invoke(sampler);
                NarcissusNetworkSmokeStatus.append("PASS server-sampling-window start-ms=" + start + " end-ms=" + end);
                return new ReflectiveSparkProfile(sampler, future, platform, plugin, server, Paths.get(configured).toAbsolutePath());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to start Spark sampler", error);
            }
        }

        private boolean active() { return !future.isDone() && !written; }

        private boolean written() {
            return written;
        }

        private boolean writeWhenComplete() {
            if (written || !future.isDone()) {
                return false;
            }
            try {
                future.get();
                ClassLoader loader = plugin.getClass().getClassLoader();
                Class<?> propsType = Class.forName("me.lucko.spark.common.sampler.Sampler$ExportProps", true, loader);
                Object props = propsType.getConstructor().newInstance();
                Class<?> senderData = Class.forName("me.lucko.spark.common.command.sender.CommandSender$Data", true, loader);
                propsType.getMethod("creator", senderData).invoke(props,
                        senderData.getConstructor(String.class, UUID.class).newInstance("Narcissus teleport network smoke", null));
                propsType.getMethod("comment", String.class).invoke(props, "Narcissus teleport network smoke");
                Class<?> strategyType = Class.forName("me.lucko.spark.common.sampler.java.MergeStrategy", true, loader);
                Object lookup = base(plugin).getMethod("createClassSourceLookup").invoke(plugin);
                propsType.getMethod("mergeStrategy", strategyType).invoke(props, strategyType.getField("SAME_METHOD").get(null));
                propsType.getMethod("classSourceLookup", Supplier.class).invoke(props, (Supplier<Object>) () -> lookup);
                Object proto = method(sampler.getClass(), "toProto", 2).invoke(sampler, platform, props);
                byte[] bytes = (byte[]) proto.getClass().getMethod("toByteArray").invoke(proto);
                if (bytes.length == 0) {
                    throw new IllegalStateException("Spark report was empty");
                }
                Files.createDirectories(report.getParent());
                Files.write(report, bytes);
                written = true;
                return true;
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while exporting sampler", error);
            } catch (ReflectiveOperationException | java.io.IOException | java.util.concurrent.ExecutionException error) {
                throw new IllegalStateException("Unable to write Spark report", error);
            }
        }

        private static Object plugin() throws ReflectiveOperationException {
            Field listeners = NeoForge.EVENT_BUS.getClass().getDeclaredField("listeners");
            listeners.setAccessible(true);
            for (Object candidate : ((Map<?, ?>) listeners.get(NeoForge.EVENT_BUS)).keySet()) {
                if (candidate != null && candidate.getClass().getName()
                        .equals("me.lucko.spark.neoforge.plugin.NeoForgeServerSparkPlugin")) {
                    return candidate;
                }
            }
            throw new IllegalStateException("Spark NeoForge server plugin was not registered");
        }

        private static Class<?> base(Object plugin) {
            Class<?> type = plugin.getClass();
            while (type != null && !type.getName().equals("me.lucko.spark.neoforge.plugin.NeoForgeSparkPlugin")) {
                type = type.getSuperclass();
            }
            if (type == null) {
                throw new IllegalStateException("Spark base plugin was not found");
            }
            return type;
        }

        private static Method method(Class<?> type, String name, int parameters) {
            for (Method candidate : type.getMethods()) {
                if (candidate.getName().equals(name) && candidate.getParameterCount() == parameters) {
                    return candidate;
                }
            }
            throw new IllegalStateException("Missing Spark method " + name);
        }
    }


}
