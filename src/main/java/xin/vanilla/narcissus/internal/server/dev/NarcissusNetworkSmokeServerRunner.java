package xin.vanilla.narcissus.internal.server.dev;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import xin.vanilla.banira.api.BaniraServer;
import xin.vanilla.banira.api.event.BaniraEvents;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeFixture;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 在独立服务端内复核网络往返，并在第二阶段验证重启后的玩家数据。
 */
public final class NarcissusNetworkSmokeServerRunner {
    private static boolean ready;
    private static boolean finished;
    private static int shutdownTicks;
    private static int gameplayTicks;
    private static double gameplayOriginX;
    private static double gameplayOriginZ;
    private static GameplayStep gameplayStep = GameplayStep.PREPARE_RANDOM;
    private static ReflectiveSparkProfile sparkProfile;

    private static final int GAMEPLAY_TIMEOUT_TICKS = 800;

    private NarcissusNetworkSmokeServerRunner() {
    }

    public static void register() {
        if (NarcissusNetworkSmokeStatus.enabled()) {
            BaniraEvents.Server.onTick(event -> onServerTick());
        }
    }

    private static void onServerTick() {
        try {
            MinecraftServer server = BaniraServer.currentAs(MinecraftServer.class);
            if (server == null || !server.isRunning()) {
                return;
            }
            if (sparkProfile != null && sparkProfile.writeWhenComplete()) {
                NarcissusNetworkSmokeStatus.append("PASS spark-report-written");
            }
            if (finished) {
                shutdownWhenSaved(server);
                return;
            }
            if (!ready) {
                ready = true;
                NarcissusNetworkSmokeStatus.append("PASS server-ready");
            }
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) {
                return;
            }
            ServerPlayer player = players.get(0);
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            if ("phase-one".equals(NarcissusNetworkSmokeStatus.phase())
                    && !runTeleportGameplaySmoke(player)) {
                return;
            }
            if ("phase-one".equals(NarcissusNetworkSmokeStatus.phase())) {
                runWritePhase(data);
            } else if ("phase-two".equals(NarcissusNetworkSmokeStatus.phase())) {
                runVerifyPhase(data);
            } else {
                throw new IllegalStateException("Unknown network smoke phase: " + NarcissusNetworkSmokeStatus.phase());
            }
        } catch (Throwable error) {
            finished = true;
            NarcissusNetworkSmokeStatus.append("FAIL server " + error);
            throw error;
        }
    }

    /**
     * 在可控地面上经过真实异步安全搜索和传送流程，避免只对工具方法做脱离游戏的单测。
     */
    private static boolean runTeleportGameplaySmoke(ServerPlayer player) {
        if (++gameplayTicks > GAMEPLAY_TIMEOUT_TICKS) {
            throw new IllegalStateException("Teleport gameplay smoke timed out in " + gameplayStep);
        }
        ServerLevel level = player.getLevel();
        switch (gameplayStep) {
            case PREPARE_RANDOM:
                if (sparkProfile == null) {
                    sparkProfile = ReflectiveSparkProfile.start();
                    NarcissusNetworkSmokeStatus.append("PASS spark-profiler-active");
                }
                prepareSafeGround(level);
                player.teleportTo(level, 0.5D, 70.0D, 0.5D, -90.0F, 35.0F);
                gameplayOriginX = player.getX();
                gameplayOriginZ = player.getZ();
                SafeWorldCoordinate random = SafeWorldCoordinate.random(player, 8, level.dimension()).safe(true);
                NarcissusUtils.teleportTo(player, random, EnumTeleportType.TP_RANDOM, 8);
                NarcissusNetworkSmokeStatus.append("START random-safe-teleport");
                gameplayStep = GameplayStep.WAIT_RANDOM;
                gameplayTicks = 0;
                return false;
            case WAIT_RANDOM:
                if (!movedFromGameplayOrigin(player)) {
                    return false;
                }
                if (!player.isOnGround()) {
                    return false;
                }
                assertSafeGround(player);
                NarcissusNetworkSmokeStatus.append("PASS random-safe-teleport");
                player.teleportTo(level, 0.5D, 70.0D, 0.5D, -90.0F, 35.0F);
                gameplayOriginX = player.getX();
                gameplayOriginZ = player.getZ();
                SafeWorldCoordinate viewTarget = NarcissusUtils.findViewEndCandidate(player, false, 16);
                if (viewTarget == null) {
                    throw new IllegalStateException("View-end candidate was not found on prepared terrain");
                }
                viewTarget.safe(false);
                NarcissusUtils.teleportTo(player, viewTarget, EnumTeleportType.TP_VIEW);
                NarcissusNetworkSmokeStatus.append("START view-end-teleport");
                gameplayStep = GameplayStep.WAIT_VIEW;
                gameplayTicks = 0;
                return false;
            case WAIT_VIEW:
                if (!movedFromGameplayOrigin(player)) {
                    return false;
                }
                NarcissusNetworkSmokeStatus.append("PASS view-end-teleport");
                gameplayStep = GameplayStep.COMPLETE;
                return true;
            case COMPLETE:
                return true;
            default:
                throw new IllegalStateException("Unknown gameplay smoke step " + gameplayStep);
        }
    }

    private static void prepareSafeGround(ServerLevel level) {
        for (int x = -64; x <= 64; x++) {
            for (int z = -64; z <= 64; z++) {
                level.setBlock(new BlockPos(x, 64, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }

    private static boolean movedFromGameplayOrigin(ServerPlayer player) {
        double dx = player.getX() - gameplayOriginX;
        double dz = player.getZ() - gameplayOriginZ;
        return dx * dx + dz * dz >= 4.0D;
    }

    private static void assertSafeGround(ServerPlayer player) {
        BlockPos below = player.blockPosition().below();
        if (!player.getLevel().getBlockState(below).getMaterial().blocksMotion()) {
            throw new IllegalStateException("Teleport did not land on solid ground: " + below);
        }
    }

    /**
     * 玩家离线后留出两秒保存时间，再走 Minecraft 自身的正常关闭流程。
     */
    private static void shutdownWhenSaved(MinecraftServer server) {
        if (server.getPlayerList().getPlayerCount() > 0) {
            shutdownTicks = 0;
            return;
        }
        if (++shutdownTicks >= 40) {
            NarcissusNetworkSmokeStatus.append("PASS server-shutdown");
            server.halt(false);
        }
    }

    private static void runWritePhase(PlayerTeleportData data) {
        if (data.getTeleportCountdownSeconds(EnumTeleportType.TP_HOME)
                != NarcissusNetworkSmokeFixture.COUNTDOWN) {
            return;
        }
        try {
            NarcissusNetworkSmokeFixture.verifyAccess(data.getAccess());
        } catch (IllegalStateException ignored) {
            return;
        }
        data.save();
        NarcissusNetworkSmokeStatus.append("PASS server-config-roundtrip");
        NarcissusNetworkSmokeStatus.append("PASS server-access-list-roundtrip");
        NarcissusNetworkSmokeStatus.append("FINISHED phase-one");
        finished = true;
    }

    private static void runVerifyPhase(PlayerTeleportData data) {
        if (data.getTeleportCountdownSeconds(EnumTeleportType.TP_HOME)
                != NarcissusNetworkSmokeFixture.COUNTDOWN) {
            throw new IllegalStateException("Player countdown was not restored from disk");
        }
        NarcissusNetworkSmokeStatus.append("PASS persisted-player-config");
        NarcissusNetworkSmokeFixture.verifyAccess(data.getAccess());
        NarcissusNetworkSmokeStatus.append("PASS persisted-access-list");
        NarcissusNetworkSmokeStatus.append("FINISHED phase-two");
        finished = true;
    }

    private enum GameplayStep {
        PREPARE_RANDOM,
        WAIT_RANDOM,
        WAIT_VIEW,
        COMPLETE
    }

    /**
     * Spark 未提供稳定的跨加载器报告 API；烟测通过反射仅使用其原生 sampler，并保留 Spark 的二进制报告格式。
     */
    private static final class ReflectiveSparkProfile {
        private static final String REPORT_PROPERTY = "narcissus.networkSmoke.sparkReport";

        private final Object platform;
        private final Object sampler;
        private final Future<?> future;
        private final Path reportPath;
        private boolean written;

        private ReflectiveSparkProfile(Object platform, Object sampler, Future<?> future, Path reportPath) {
            this.platform = platform;
            this.sampler = sampler;
            this.future = future;
            this.reportPath = reportPath;
        }

        private static ReflectiveSparkProfile start() {
            try {
                Path reportPath = Paths.get(System.getProperty(REPORT_PROPERTY, "")).toAbsolutePath();
                if (System.getProperty(REPORT_PROPERTY, "").trim().isEmpty()) {
                    throw new IllegalStateException("Missing " + REPORT_PROPERTY);
                }
                Object platform = getPlatform();
                Object plugin = getServerPlugin();
                ClassLoader loader = platform.getClass().getClassLoader();
                Class<?> builderType = Class.forName("me.lucko.spark.common.sampler.SamplerBuilder", true, loader);
                Object builder = builderType.getConstructor().newInstance();
                Class<?> modeType = Class.forName("me.lucko.spark.common.sampler.SamplerMode", true, loader);
                Object executionMode = Enum.valueOf((Class) modeType, "EXECUTION");
                double interval = ((Number) modeType.getMethod("defaultInterval").invoke(executionMode)).doubleValue();
                builderType.getMethod("mode", modeType).invoke(builder, executionMode);
                builderType.getMethod("samplingInterval", double.class).invoke(builder, interval);
                builderType.getMethod("completeAfter", long.class, TimeUnit.class)
                        .invoke(builder, 20L, TimeUnit.SECONDS);
                builderType.getMethod("forceJavaSampler", boolean.class).invoke(builder, true);
                Class<?> threadDumperType = Class.forName("me.lucko.spark.common.sampler.ThreadDumper", true, loader);
                Object threadDumper = plugin.getClass().getMethod("getDefaultThreadDumper").invoke(plugin);
                builderType.getMethod("threadDumper", threadDumperType).invoke(builder, threadDumper);
                Class<?> threadGrouperType = Class.forName("me.lucko.spark.common.sampler.ThreadGrouper", true, loader);
                Object threadGrouper = threadGrouperType.getField("BY_POOL").get(null);
                builderType.getMethod("threadGrouper", threadGrouperType).invoke(builder, threadGrouper);
                Object sampler = findMethod(builderType, "start", 1).invoke(builder, platform);
                Object samplerContainer = platform.getClass().getMethod("getSamplerContainer").invoke(platform);
                findMethod(samplerContainer.getClass(), "setActiveSampler", 1).invoke(samplerContainer, sampler);
                Future<?> future = (Future<?>) findMethod(sampler.getClass(), "getFuture", 0).invoke(sampler);
                return new ReflectiveSparkProfile(platform, sampler, future, reportPath);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to start Spark sampler for network smoke", error);
            }
        }

        private boolean writeWhenComplete() {
            if (written || !future.isDone()) {
                return false;
            }
            try {
                ClassLoader loader = sampler.getClass().getClassLoader();
                Class<?> propsType = Class.forName("me.lucko.spark.common.sampler.Sampler$ExportProps", true, loader);
                Object props = propsType.getConstructor().newInstance();
                Class<?> senderDataType = Class.forName(
                        "me.lucko.spark.common.command.sender.CommandSender$Data", true, loader);
                Object creator = senderDataType.getConstructor(String.class, java.util.UUID.class)
                        .newInstance("Narcissus network smoke", null);
                propsType.getMethod("creator", senderDataType).invoke(props, creator);
                Supplier<Object> mergeMode = ReflectiveSparkProfile::newMergeMode;
                Supplier<Object> classSourceLookup = () -> invokeClassSourceLookup(platform);
                propsType.getMethod("mergeMode", Supplier.class).invoke(props, mergeMode);
                propsType.getMethod("classSourceLookup", Supplier.class).invoke(props, classSourceLookup);
                Object proto = findMethod(sampler.getClass(), "toProto", 2).invoke(sampler, platform, props);
                byte[] data = (byte[]) proto.getClass().getMethod("toByteArray").invoke(proto);
                if (data.length == 0) {
                    throw new IllegalStateException("Spark profile was empty");
                }
                Files.createDirectories(reportPath.getParent());
                Files.write(reportPath, data);
                written = true;
                return true;
            } catch (ReflectiveOperationException | java.io.IOException error) {
                throw new IllegalStateException("Unable to write Spark profile report", error);
            }
        }

        private static Object getPlatform() throws ReflectiveOperationException {
            Object plugin = getServerPlugin();
            Field platformField = plugin.getClass().getSuperclass().getDeclaredField("platform");
            platformField.setAccessible(true);
            return platformField.get(plugin);
        }

        private static Object getServerPlugin() throws ReflectiveOperationException {
            Class<?> modType = Class.forName("me.lucko.spark.fabric.FabricSparkMod");
            Field modField = modType.getDeclaredField("mod");
            modField.setAccessible(true);
            Object mod = modField.get(null);
            Field pluginField = modType.getDeclaredField("activeServerPlugin");
            pluginField.setAccessible(true);
            return pluginField.get(mod);
        }

        private static Method findMethod(Class<?> type, String name, int parameterCount) {
            for (Method method : type.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                    return method;
                }
            }
            throw new IllegalStateException("Missing Spark method " + type.getName() + '#' + name);
        }

        private static Object newMergeMode() {
            try {
                ClassLoader loader = ReflectiveSparkProfile.class.getClassLoader();
                Class<?> disambiguatorType = Class.forName(
                        "me.lucko.spark.common.util.MethodDisambiguator", true, loader);
                Object disambiguator = disambiguatorType.getConstructor().newInstance();
                Class<?> mergeModeType = Class.forName(
                        "me.lucko.spark.common.sampler.node.MergeMode", true, loader);
                return mergeModeType.getMethod("sameMethod", disambiguatorType).invoke(null, disambiguator);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to create Spark merge mode", error);
            }
        }

        private static Object invokeClassSourceLookup(Object platform) {
            try {
                return platform.getClass().getMethod("createClassSourceLookup").invoke(platform);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to create Spark class source lookup", error);
            }
        }
    }
}
