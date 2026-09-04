package xin.vanilla.narcissus.internal.server.dev;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import xin.vanilla.banira.api.BaniraServer;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeFixture;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus;
import xin.vanilla.narcissus.util.NarcissusUtils;
import xin.vanilla.narcissus.util.SafeBlockChecker;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 在独立服务端内复核网络往返，并在第二阶段验证重启后的玩家数据。
 */
public final class NarcissusNetworkSmokeServerRunner {
    private static final int TELEPORT_WORKLOAD_TIMEOUT_TICKS = 1200;
    private static final int SAFE_RANDOM_RANGE = 48;
    private static final int VIEW_RANGE = 160;
    private static final int FIXTURE_Y = 80;
    private static final long SPARK_SAMPLE_SECONDS = 20L;

    private static boolean ready;
    private static boolean finished;
    private static boolean commonConfigCommandVerified;
    private static int shutdownTicks;
    private static int teleportWorkloadTicks;
    private static boolean teleportPending;
    private static int randomTeleportPasses;
    private static SafeWorldCoordinate pendingBefore;
    private static UUID followerId;
    private static PlayerTeleportData lastPlayerData;
    private static final NarcissusTeleportSmokeWorkload teleportWorkload = new NarcissusTeleportSmokeWorkload();
    private static ReflectiveSparkProfile sparkProfile;

    private NarcissusNetworkSmokeServerRunner() {
    }

    public static void register() {
        if (NarcissusNetworkSmokeStatus.enabled()) {
            MinecraftForge.EVENT_BUS.addListener(NarcissusNetworkSmokeServerRunner::onServerTick);
        }
    }

    private static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        try {
            MinecraftServer server = BaniraServer.currentAs(MinecraftServer.class);
            if (server == null || !server.isRunning()) {
                return;
            }
            if (sparkProfile != null && sparkProfile.writeWhenComplete()) {
                NarcissusNetworkSmokeStatus.append("PASS spark-report-written");
            }
            if (finished) {
                if (sparkProfile != null && !sparkProfile.written()) {
                    return;
                }
                shutdownWhenSaved(server);
                return;
            }
            if (!ready) {
                ready = true;
                NarcissusNetworkSmokeStatus.append("PASS server-ready");
            }
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            if (!players.isEmpty()) {
                lastPlayerData = PlayerTeleportData.getData(players.get(0));
            }
            if (lastPlayerData == null) {
                return;
            }
            if ("phase-one".equals(NarcissusNetworkSmokeStatus.phase())) {
                if (teleportWorkload.current() != NarcissusTeleportSmokeWorkload.Step.COMPLETE) {
                    if (players.isEmpty() || !runTeleportWorkload(players.get(0))) {
                        return;
                    }
                }
                if (!players.isEmpty()) {
                    return;
                }
                if (!commonConfigCommandVerified) {
                    verifyCommonConfigCommand(server);
                    commonConfigCommandVerified = true;
                }
                runWritePhase(lastPlayerData);
            } else if ("phase-two".equals(NarcissusNetworkSmokeStatus.phase())) {
                runVerifyPhase(lastPlayerData);
            } else {
                throw new IllegalStateException("Unknown network smoke phase: " + NarcissusNetworkSmokeStatus.phase());
            }
        } catch (Throwable error) {
            finished = true;
            NarcissusNetworkSmokeStatus.append("FAIL server " + error);
            throw error;
        }
    }

    private static void verifyCommonConfigCommand(MinecraftServer server) {
        int result = server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withPermission(4),
                "narcissus config common base.teleportLimit.teleportRecordLimit "
                        + NarcissusNetworkSmokeFixture.TELEPORT_RECORD_LIMIT);
        if (result <= 0 || CommonConfig.get().base().teleportLimit().teleportRecordLimit()
                != NarcissusNetworkSmokeFixture.TELEPORT_RECORD_LIMIT) {
            throw new IllegalStateException("Common config command did not update teleportRecordLimit");
        }
        NarcissusNetworkSmokeStatus.append("PASS server-common-config-command");
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
        if (CommonConfig.get().base().teleportLimit().teleportRecordLimit()
                != NarcissusNetworkSmokeFixture.TELEPORT_RECORD_LIMIT) {
            return;
        }
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

    private static boolean runTeleportWorkload(ServerPlayer player) {
        if (++teleportWorkloadTicks > TELEPORT_WORKLOAD_TIMEOUT_TICKS) {
            throw new IllegalStateException("Teleport workload timed out in " + teleportWorkload.current());
        }
        switch (teleportWorkload.current()) {
            case SAFE_RANDOM:
                return runSafeRandomTeleport(player);
            case VIEW_END:
                return runViewEndTeleport(player);
            case CROSS_DIMENSION_FOLLOWER:
                return runCrossDimensionFollowerTeleport(player);
            case COMPLETE:
                return true;
            default:
                throw new IllegalStateException("Unknown teleport workload step " + teleportWorkload.current());
        }
    }

    private static boolean runSafeRandomTeleport(ServerPlayer player) {
        if (!teleportPending) {
            configureTeleportFixture(player);
            SafeWorldCoordinate target = SafeWorldCoordinate.random(player, SAFE_RANDOM_RANGE);
            target.y(FIXTURE_Y);
            target.safe(true);
            preparePlatform(player.serverLevel(), target.xInt(), target.zInt());
            pendingBefore = new SafeWorldCoordinate(player);
            teleportPending = true;
            NarcissusUtils.teleportTo(player, target, EnumTeleportType.TP_RANDOM, SAFE_RANDOM_RANGE);
            return false;
        }
        if (!movedFromPendingStart(player)) {
            return false;
        }
        if (!new SafeBlockChecker(player.serverLevel()).isSafeBlock(player.blockPosition(), false)) {
            throw new IllegalStateException("Safe random teleport landed on an unsafe block: " + player.blockPosition());
        }
        teleportPending = false;
        teleportWorkload.completeCurrent();
        if (++randomTeleportPasses == 3) {
            NarcissusNetworkSmokeStatus.append("PASS safe-random-teleport");
        }
        return false;
    }

    private static boolean runViewEndTeleport(ServerPlayer player) {
        if (!teleportPending) {
            ServerLevel level = player.serverLevel();
            int startX = player.blockPosition().getX();
            int startZ = player.blockPosition().getZ();
            prepareViewFixture(level, startX, startZ);
            player.teleportTo(level, startX + 0.5D, FIXTURE_Y, startZ + 0.5D, -90.0F, 0.0F);
            SafeWorldCoordinate target = NarcissusUtils.findViewEndCandidate(player, true, VIEW_RANGE);
            if (target == null) {
                throw new IllegalStateException("View-end search did not find the prepared safe landing");
            }
            target.y(FIXTURE_Y);
            preparePlatform(level, target.xInt(), target.zInt());
            pendingBefore = new SafeWorldCoordinate(player);
            teleportPending = true;
            NarcissusUtils.teleportTo(player, target.safe(true), EnumTeleportType.TP_VIEW);
            return false;
        }
        if (!movedFromPendingStart(player)) {
            return false;
        }
        if (!new SafeBlockChecker(player.serverLevel()).isSafeBlock(player.blockPosition(), false)) {
            throw new IllegalStateException("View-end teleport landed on an unsafe block: " + player.blockPosition());
        }
        teleportPending = false;
        teleportWorkload.completeCurrent();
        NarcissusNetworkSmokeStatus.append("PASS view-end-teleport");
        return false;
    }

    private static boolean runCrossDimensionFollowerTeleport(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        ServerLevel nether = server == null ? null : server.getLevel(Level.NETHER);
        if (nether == null) {
            throw new IllegalStateException("Nether was unavailable for cross-dimension teleport smoke");
        }
        if (!teleportPending) {
            CommonConfig.get().base().teleportLimit().teleportAcrossDimension(true);
            CommonConfig.get().base().teleportTogether().tpWithFollower(true).tpWithFollowerRange(32);
            preparePlatform(nether, 24, 24);
            Wolf follower = EntityType.WOLF.create(player.serverLevel());
            if (follower == null) {
                throw new IllegalStateException("Could not create follower fixture");
            }
            follower.setOwnerUUID(player.getUUID());
            follower.moveTo(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D, 0.0F, 0.0F);
            player.serverLevel().addFreshEntity(follower);
            followerId = follower.getUUID();
            pendingBefore = new SafeWorldCoordinate(player);
            teleportPending = true;
            NarcissusUtils.teleportTo(player,
                    new SafeWorldCoordinate(24.5D, FIXTURE_Y, 24.5D, Level.NETHER), EnumTeleportType.OTHER);
            return false;
        }
        Entity follower = followerId == null ? null : nether.getEntity(followerId);
        if (player.serverLevel() != nether || !(follower instanceof Wolf)
                || follower.distanceToSqr(player) > 16.0D) {
            return false;
        }
        teleportPending = false;
        teleportWorkload.completeCurrent();
        NarcissusNetworkSmokeStatus.append("PASS cross-dimension-follower-teleport");
        return false;
    }

    private static boolean movedFromPendingStart(ServerPlayer player) {
        return pendingBefore != null && (!player.serverLevel().dimension().equals(pendingBefore.dimension())
                || player.distanceToSqr(pendingBefore.x(), pendingBefore.y(), pendingBefore.z()) > 4.0D);
    }

    private static void configureTeleportFixture(ServerPlayer player) {
        if (sparkProfile == null) {
            sparkProfile = ReflectiveSparkProfile.start();
            NarcissusNetworkSmokeStatus.append("PASS spark-profiler-active");
        }
    }

    private static void prepareViewFixture(ServerLevel level, int startX, int startZ) {
        for (int offset = 0; offset <= VIEW_RANGE; offset++) {
            for (int zOffset = -2; zOffset <= 2; zOffset++) {
                placeSafeFloor(level, startX + offset, startZ + zOffset);
            }
        }
        for (int yOffset = 0; yOffset <= 5; yOffset++) {
            for (int zOffset = -4; zOffset <= 4; zOffset++) {
                level.setBlockAndUpdate(new BlockPos(startX + VIEW_RANGE - 2, FIXTURE_Y + yOffset, startZ + zOffset),
                        Blocks.STONE.defaultBlockState());
            }
        }
    }

    private static void preparePlatform(ServerLevel level, int centerX, int centerZ) {
        for (int xOffset = -3; xOffset <= 3; xOffset++) {
            for (int zOffset = -3; zOffset <= 3; zOffset++) {
                placeSafeFloor(level, centerX + xOffset, centerZ + zOffset);
            }
        }
    }

    private static void placeSafeFloor(ServerLevel level, int x, int z) {
        level.setBlockAndUpdate(new BlockPos(x, FIXTURE_Y - 1, z), Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(x, FIXTURE_Y, z), Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(x, FIXTURE_Y + 1, z), Blocks.AIR.defaultBlockState());
    }

    /** Spark 1.9 does not expose a stable cross-loader report API. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final class ReflectiveSparkProfile {
        private final Object sampler;
        private final Future<?> future;
        private final Object platform;
        private final Object plugin;
        private final MinecraftServer server;
        private final Path report;
        private final long startedAt = System.nanoTime();
        private boolean written;
        private boolean stopRequested;

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
                        .invoke(builder, SPARK_SAMPLE_SECONDS, TimeUnit.SECONDS);
                builderType.getMethod("forceJavaSampler", boolean.class).invoke(builder, true);
                Class<?> dumperType = Class.forName("me.lucko.spark.common.sampler.ThreadDumper", true, loader);
                Class<?> gameThread = Class.forName("me.lucko.spark.common.sampler.ThreadDumper$GameThread", true, loader);
                Object gameThreadDumper = gameThread.getConstructor().newInstance();
                gameThread.getMethod("setThread", Thread.class).invoke(gameThreadDumper, Thread.currentThread());
                builderType.getMethod("threadDumper", dumperType)
                        .invoke(builder, gameThread.getMethod("get").invoke(gameThreadDumper));
                Class<?> grouperType = Class.forName("me.lucko.spark.common.sampler.ThreadGrouper", true, loader);
                builderType.getMethod("threadGrouper", grouperType).invoke(builder, grouperType.getField("BY_POOL").get(null));
                Object container = platform.getClass().getMethod("getSamplerContainer").invoke(platform);
                method(container.getClass(), "stopActiveSampler", 1).invoke(container, false);
                Object sampler = method(builderType, "start", 1).invoke(builder, platform);
                Future<?> future = (Future<?>) method(sampler.getClass(), "getFuture", 0).invoke(sampler);
                method(container.getClass(), "setActiveSampler", 1).invoke(container, sampler);
                return new ReflectiveSparkProfile(sampler, future, platform, plugin, server, Paths.get(configured).toAbsolutePath());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to start Spark sampler", error);
            }
        }

        private boolean written() {
            return written;
        }

        private boolean writeWhenComplete() {
            if (!stopRequested && System.nanoTime() - startedAt >= TimeUnit.SECONDS.toNanos(SPARK_SAMPLE_SECONDS)) {
                try {
                    method(sampler.getClass(), "stop", 1).invoke(sampler, false);
                    stopRequested = true;
                } catch (ReflectiveOperationException error) {
                    throw new IllegalStateException("Unable to stop Spark sampler", error);
                }
            }
            if (written || (!stopRequested && !future.isDone())) {
                return false;
            }
            try {
                ClassLoader loader = plugin.getClass().getClassLoader();
                Class<?> propsType = Class.forName("me.lucko.spark.common.sampler.Sampler$ExportProps", true, loader);
                Object props = propsType.getConstructor().newInstance();
                Class<?> senderData = Class.forName("me.lucko.spark.common.command.sender.CommandSender$Data", true, loader);
                propsType.getMethod("creator", senderData).invoke(props,
                        senderData.getConstructor(String.class, UUID.class)
                                .newInstance("Narcissus teleport network smoke", null));
                Class<?> disambiguatorType = Class.forName("me.lucko.spark.common.util.MethodDisambiguator", true, loader);
                Class<?> mergeType = Class.forName("me.lucko.spark.common.sampler.node.MergeMode", true, loader);
                Supplier<Object> merge = () -> createMergeMode(mergeType, disambiguatorType);
                propsType.getMethod("mergeMode", Supplier.class).invoke(props, merge);
                propsType.getMethod("classSourceLookup", Supplier.class).invoke(props,
                        (Supplier<Object>) () -> createClassSourceLookup(plugin));
                Object proto = method(sampler.getClass(), "toProto", 2).invoke(sampler, platform, props);
                byte[] bytes = (byte[]) proto.getClass().getMethod("toByteArray").invoke(proto);
                if (bytes.length == 0) {
                    throw new IllegalStateException("Spark report was empty");
                }
                Files.createDirectories(report.getParent());
                Files.write(report, bytes);
                written = true;
                return true;
            } catch (ReflectiveOperationException | java.io.IOException error) {
                throw new IllegalStateException("Unable to write Spark report", error);
            }
        }

        private static Object plugin() throws ReflectiveOperationException {
            Field listeners = MinecraftForge.EVENT_BUS.getClass().getDeclaredField("listeners");
            listeners.setAccessible(true);
            for (Object candidate : ((Map<?, ?>) listeners.get(MinecraftForge.EVENT_BUS)).keySet()) {
                if (candidate != null && candidate.getClass().getName()
                        .equals("me.lucko.spark.forge.plugin.ForgeServerSparkPlugin")) {
                    return candidate;
                }
            }
            throw new IllegalStateException("Spark Forge server plugin was not registered");
        }

        private static Class<?> base(Object plugin) {
            Class<?> type = plugin.getClass();
            while (type != null && !type.getName().equals("me.lucko.spark.forge.plugin.ForgeSparkPlugin")) {
                type = type.getSuperclass();
            }
            if (type == null) {
                throw new IllegalStateException("Spark base plugin was not found");
            }
            return type;
        }

        private static Object createMergeMode(Class<?> mergeType, Class<?> disambiguatorType) {
            try {
                return mergeType.getMethod("sameMethod", disambiguatorType)
                        .invoke(null, disambiguatorType.getConstructor().newInstance());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to create Spark merge mode", error);
            }
        }

        private static Object createClassSourceLookup(Object plugin) {
            try {
                return plugin.getClass().getMethod("createClassSourceLookup").invoke(plugin);
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to create Spark class source lookup", error);
            }
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

    private static void runVerifyPhase(PlayerTeleportData data) {
        if (CommonConfig.get().base().teleportLimit().teleportRecordLimit()
                != NarcissusNetworkSmokeFixture.TELEPORT_RECORD_LIMIT) {
            throw new IllegalStateException("Common config command value was not restored from disk");
        }
        NarcissusNetworkSmokeStatus.append("PASS persisted-common-config");
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
}
