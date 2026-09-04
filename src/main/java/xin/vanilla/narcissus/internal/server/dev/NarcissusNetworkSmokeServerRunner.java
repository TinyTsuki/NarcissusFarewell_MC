package xin.vanilla.narcissus.internal.server.dev;

import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
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

/**
 * 在独立服务端内复核网络往返，并在第二阶段验证重启后的玩家数据。
 */
public final class NarcissusNetworkSmokeServerRunner {
    private static final int TELEPORT_WORKLOAD_TIMEOUT_TICKS = 1200;
    private static final int SAFE_RANDOM_RANGE = 48;
    private static final int VIEW_RANGE = 160;
    private static final int FIXTURE_Y = 80;

    private static boolean ready;
    private static boolean finished;
    private static int shutdownTicks;
    private static int teleportWorkloadTicks;
    private static boolean teleportPending;
    private static int randomTeleportPasses;
    private static SafeWorldCoordinate pendingBefore;
    private static UUID followerId;
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
            List<ServerPlayerEntity> players = server.getPlayerList().getPlayers();
            if (players.isEmpty()) {
                return;
            }
            ServerPlayerEntity player = players.get(0);
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            if ("phase-one".equals(NarcissusNetworkSmokeStatus.phase())) {
                if (!runTeleportWorkload(player)) {
                    return;
                }
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

    private static boolean runTeleportWorkload(ServerPlayerEntity player) {
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

    private static boolean runSafeRandomTeleport(ServerPlayerEntity player) {
        if (!teleportPending) {
            configureTeleportFixture(player);
            SafeWorldCoordinate target = SafeWorldCoordinate.random(player, SAFE_RANDOM_RANGE);
            target.y(FIXTURE_Y);
            target.safe(true);
            preparePlatform((ServerWorld) player.level, target.xInt(), target.zInt());
            pendingBefore = new SafeWorldCoordinate(player);
            teleportPending = true;
            NarcissusUtils.teleportTo(player, target, EnumTeleportType.TP_RANDOM, SAFE_RANDOM_RANGE);
            return false;
        }
        if (!movedFromPendingStart(player)) {
            return false;
        }
        if (!new SafeBlockChecker(player.level).isSafeBlock(player.blockPosition(), false)) {
            throw new IllegalStateException("Safe random teleport landed on an unsafe block: " + player.blockPosition());
        }
        teleportPending = false;
        teleportWorkload.completeCurrent();
        if (++randomTeleportPasses == 3) {
            NarcissusNetworkSmokeStatus.append("PASS safe-random-teleport");
        }
        return false;
    }

    private static boolean runViewEndTeleport(ServerPlayerEntity player) {
        if (!teleportPending) {
            ServerWorld level = (ServerWorld) player.level;
            int startX = player.blockPosition().getX();
            int startZ = player.blockPosition().getZ();
            prepareViewFixture(level, startX, startZ);
            player.teleportTo(level, startX + 0.5D, FIXTURE_Y, startZ + 0.5D, -90.0F, 0.0F);
            SafeWorldCoordinate target = NarcissusUtils.findViewEndCandidate(player, true, VIEW_RANGE);
            if (target == null) {
                throw new IllegalStateException("View-end search did not find the prepared safe landing");
            }
            pendingBefore = new SafeWorldCoordinate(player);
            teleportPending = true;
            NarcissusUtils.teleportTo(player, target.safe(true), EnumTeleportType.TP_VIEW);
            return false;
        }
        if (!movedFromPendingStart(player)) {
            return false;
        }
        if (!new SafeBlockChecker(player.level).isSafeBlock(player.blockPosition(), false)) {
            throw new IllegalStateException("View-end teleport landed on an unsafe block: " + player.blockPosition());
        }
        teleportPending = false;
        teleportWorkload.completeCurrent();
        NarcissusNetworkSmokeStatus.append("PASS view-end-teleport");
        return false;
    }

    private static boolean runCrossDimensionFollowerTeleport(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        ServerWorld nether = server == null ? null : server.getLevel(World.NETHER);
        if (nether == null) {
            throw new IllegalStateException("Nether was unavailable for cross-dimension teleport smoke");
        }
        if (!teleportPending) {
            CommonConfig.get().base().teleportLimit().teleportAcrossDimension(true);
            CommonConfig.get().base().teleportTogether().tpWithFollower(true).tpWithFollowerRange(32);
            preparePlatform(nether, 24, 24);
            WolfEntity follower = EntityType.WOLF.create((ServerWorld) player.level);
            if (follower == null) {
                throw new IllegalStateException("Could not create follower fixture");
            }
            follower.setOwnerUUID(player.getUUID());
            follower.moveTo(player.getX() + 1.0D, player.getY(), player.getZ() + 1.0D, 0.0F, 0.0F);
            player.level.addFreshEntity(follower);
            followerId = follower.getUUID();
            pendingBefore = new SafeWorldCoordinate(player);
            teleportPending = true;
            NarcissusUtils.teleportTo(player,
                    new SafeWorldCoordinate(24.5D, FIXTURE_Y, 24.5D, World.NETHER), EnumTeleportType.OTHER);
            return false;
        }
        Entity follower = followerId == null ? null : nether.getEntity(followerId);
        if (player.level != nether || !(follower instanceof WolfEntity)
                || follower.distanceToSqr(player) > 16.0D) {
            return false;
        }
        teleportPending = false;
        teleportWorkload.completeCurrent();
        NarcissusNetworkSmokeStatus.append("PASS cross-dimension-follower-teleport");
        return false;
    }

    private static boolean movedFromPendingStart(ServerPlayerEntity player) {
        return pendingBefore != null && (!player.level.dimension().equals(pendingBefore.dimension())
                || player.distanceToSqr(pendingBefore.x(), pendingBefore.y(), pendingBefore.z()) > 4.0D);
    }

    private static void configureTeleportFixture(ServerPlayerEntity player) {
        if (sparkProfile == null) {
            sparkProfile = ReflectiveSparkProfile.start();
            NarcissusNetworkSmokeStatus.append("PASS spark-profiler-active");
        }
    }

    private static void prepareViewFixture(ServerWorld level, int startX, int startZ) {
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

    private static void preparePlatform(ServerWorld level, int centerX, int centerZ) {
        for (int xOffset = -3; xOffset <= 3; xOffset++) {
            for (int zOffset = -3; zOffset <= 3; zOffset++) {
                placeSafeFloor(level, centerX + xOffset, centerZ + zOffset);
            }
        }
    }

    private static void placeSafeFloor(ServerWorld level, int x, int z) {
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
                        .invoke(builder, 20L, TimeUnit.SECONDS);
                builderType.getMethod("forceJavaSampler", boolean.class).invoke(builder, true);
                Class<?> dumperType = Class.forName("me.lucko.spark.common.sampler.ThreadDumper", true, loader);
                Field threadDumper = base.getDeclaredField("threadDumper");
                threadDumper.setAccessible(true);
                Object setup = threadDumper.get(plugin);
                setup.getClass().getMethod("ensureSetup").invoke(setup);
                builderType.getMethod("threadDumper", dumperType)
                        .invoke(builder, base.getMethod("getDefaultThreadDumper").invoke(plugin));
                Class<?> grouperType = Class.forName("me.lucko.spark.common.sampler.ThreadGrouper", true, loader);
                builderType.getMethod("threadGrouper", grouperType).invoke(builder, grouperType.getField("BY_POOL").get(null));
                Object sampler = method(builderType, "start", 1).invoke(builder, platform);
                Future<?> future = (Future<?>) method(sampler.getClass(), "getFuture", 0).invoke(sampler);
                return new ReflectiveSparkProfile(sampler, future, platform, plugin, server, Paths.get(configured).toAbsolutePath());
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Unable to start Spark sampler", error);
            }
        }

        private boolean written() {
            return written;
        }

        private boolean writeWhenComplete() {
            if (written || !future.isDone()) {
                return false;
            }
            try {
                ClassLoader loader = plugin.getClass().getClassLoader();
                Class<?> sourceType = Class.forName("net.minecraft.command.ICommandSource", true, loader);
                Class<?> base = Class.forName("me.lucko.spark.forge.plugin.ForgeSparkPlugin", true, loader);
                Class<?> senderType = Class.forName("me.lucko.spark.forge.ForgeCommandSender", true, loader);
                Object sender = senderType.getConstructor(sourceType, base).newInstance(server, plugin);
                Class<?> orderType = Class.forName("me.lucko.spark.common.sampler.ThreadNodeOrder", true, loader);
                Class<?> disambiguatorType = Class.forName("me.lucko.spark.common.util.MethodDisambiguator", true, loader);
                Class<?> mergeType = Class.forName("me.lucko.spark.common.sampler.node.MergeMode", true, loader);
                Object merge = mergeType.getMethod("sameMethod", disambiguatorType)
                        .invoke(null, disambiguatorType.getConstructor().newInstance());
                Object lookup = base.getMethod("createClassSourceLookup").invoke(plugin);
                Object proto = method(sampler.getClass(), "toProto", 6).invoke(sampler, platform, sender,
                        orderType.getField("BY_TIME").get(null), "Narcissus teleport network smoke", merge, lookup);
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
