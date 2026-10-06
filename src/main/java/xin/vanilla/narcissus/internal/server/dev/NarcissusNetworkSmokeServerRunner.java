package xin.vanilla.narcissus.internal.server.dev;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * 在独立服务端内复核网络往返，并在第二阶段验证重启后的玩家数据。
 */
public final class NarcissusNetworkSmokeServerRunner {
    private static boolean ready;
    private static boolean finished;
    private static int shutdownTicks;
    private static int gameplayTicks;
    private static int fakeTeleportTicks;
    private static double gameplayOriginX;
    private static double gameplayOriginZ;
    private static GameplayStep gameplayStep = GameplayStep.PREPARE_RANDOM;
    private static ReflectiveSparkProfile sparkProfile;
    private static NarcissusNetworkSmokeWorkload workload;
    private static NarcissusCarpetFakePlayers fakePlayers;
    private static int workloadSearches;

    private static final int GAMEPLAY_TIMEOUT_TICKS = 800;
    private static final int FAKE_TELEPORT_TIMEOUT_TICKS = 160;
    private static final double PREPARED_GROUND_PLAYER_Y = 65.0D;
    private static final int WORKLOAD_SEARCHES_PER_TICK = 4;

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
            if (finished) {
                shutdownWhenSaved(server);
                return;
            }
            if (sparkProfile != null && sparkProfile.writeWhenComplete()) {
                NarcissusNetworkSmokeStatus.append("PASS spark-report-written");
            }
            if (!ready) {
                ready = true;
                NarcissusNetworkSmokeStatus.append("PASS server-ready");
            }
            List<ServerPlayer> players = server.getPlayerList().getPlayers();
            ServerPlayer player = players.stream()
                    .filter(candidate -> fakePlayers == null
                            || !fakePlayers.isFixturePlayerName(candidate.getGameProfile().getName()))
                    .findFirst()
                    .orElse(null);
            if (player == null) {
                return;
            }
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            if ("phase-one".equals(NarcissusNetworkSmokeStatus.phase())
                    && !runTeleportGameplaySmoke(server, player)) {
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
    private static boolean runTeleportGameplaySmoke(MinecraftServer server, ServerPlayer player) {
        if (++gameplayTicks > GAMEPLAY_TIMEOUT_TICKS) {
            throw new IllegalStateException("Teleport gameplay smoke timed out in " + gameplayStep);
        }
        ServerLevel level = player.serverLevel();
        switch (gameplayStep) {
            case PREPARE_RANDOM:
                if (sparkProfile == null) {
                    sparkProfile = ReflectiveSparkProfile.start();
                    NarcissusNetworkSmokeStatus.append("PASS spark-profiler-active");
                }
                prepareSafeGround(level);
                player.teleportTo(level, 0.5D, 70.0D, 0.5D, -90.0F, 0.0F);
                gameplayOriginX = player.getX();
                gameplayOriginZ = player.getZ();
                // 保留真实的随机横向坐标与异步安全搜索，但固定在烟测铺设地面的相邻高度。
                // 默认 NONE 模式会跨完整世界高度做径向搜索，随机 Y 会让烟测偶发地变成无界性能测试。
                SafeWorldCoordinate random = SafeWorldCoordinate.random(player, 8, level.dimension()).safe(true);
                random.y(PREPARED_GROUND_PLAYER_Y);
                NarcissusUtils.teleportTo(player, random, EnumTeleportType.TP_RANDOM, 8);
                NarcissusNetworkSmokeStatus.append("START random-safe-teleport");
                gameplayStep = GameplayStep.WAIT_RANDOM;
                gameplayTicks = 0;
                return false;
            case WAIT_RANDOM:
                if (!movedFromGameplayOrigin(player)) {
                    return false;
                }
                if (!player.onGround()) {
                    return false;
                }
                assertSafeGround(player);
                NarcissusNetworkSmokeStatus.append("PASS random-safe-teleport");
                player.teleportTo(level, 0.5D, 70.0D, 0.5D, -90.0F, 0.0F);
                gameplayOriginX = player.getX();
                gameplayOriginZ = player.getZ();
                prepareViewCollisionRing(level);
                gameplayStep = GameplayStep.PREPARE_VIEW;
                return false;
            case PREPARE_VIEW:
                gameplayOriginX = player.getX();
                gameplayOriginZ = player.getZ();
                SafeWorldCoordinate viewTarget = NarcissusUtils.findViewEndCandidate(player, false, 16);
                if (viewTarget == null) {
                    throw new IllegalStateException("View-end candidate was not found on prepared terrain: "
                            + describeView(player));
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
                player.teleportTo(level, 0.5D, 70.0D, 0.5D, -90.0F, 0.0F);
                fakePlayers = new NarcissusCarpetFakePlayers();
                fakePlayers.spawn(server);
                NarcissusNetworkSmokeStatus.append("START carpet-fake-player-fixture");
                gameplayStep = GameplayStep.WAIT_FAKE_PLAYERS;
                gameplayTicks = 0;
                return false;
            case WAIT_FAKE_PLAYERS:
                if (!fakePlayers.allPresent(server)) {
                    return false;
                }
                prepareFakeTeleportFixtures(server, fakePlayers.resolve(server));
                NarcissusNetworkSmokeStatus.append("PASS carpet-fake-players-ready");
                NarcissusNetworkSmokeStatus.append("START carpet-fake-player-teleports");
                gameplayStep = GameplayStep.WAIT_FAKE_TELEPORTS;
                gameplayTicks = 0;
                fakeTeleportTicks = 0;
                return false;
            case WAIT_FAKE_TELEPORTS:
                if (!fakeTeleportFixtureComplete(server)) {
                    if (++fakeTeleportTicks >= FAKE_TELEPORT_TIMEOUT_TICKS) {
                        throw new IllegalStateException("Carpet fake-player teleport fixture did not settle: "
                                + describeFakeTeleportFixture(server));
                    }
                    return false;
                }
                NarcissusNetworkSmokeStatus.append("PASS carpet-fake-player-teleports");
                workload = new NarcissusNetworkSmokeWorkload(gameplayTicks);
                NarcissusNetworkSmokeStatus.append("START sustained-coordinate-workload");
                gameplayStep = GameplayStep.SUSTAINED;
                return false;
            case SUSTAINED:
                if (!runSustainedCoordinateWorkload(server, player)) return false;
                NarcissusNetworkSmokeStatus.append("PASS sustained-coordinate-workload");
                fakePlayers.cleanup(server);
                NarcissusNetworkSmokeStatus.append("START carpet-fake-player-cleanup");
                gameplayStep = GameplayStep.WAIT_FAKE_CLEANUP;
                return false;
            case WAIT_FAKE_CLEANUP:
                if (!fakePlayers.allRemoved(server)) {
                    return false;
                }
                NarcissusNetworkSmokeStatus.append("PASS carpet-fake-player-cleanup");
                fakePlayers = null;
                gameplayStep = GameplayStep.COMPLETE;
                return true;
            case COMPLETE:
                return true;
            default:
                throw new IllegalStateException("Unknown gameplay smoke step " + gameplayStep);
        }
    }

    private static boolean runSustainedCoordinateWorkload(MinecraftServer server, ServerPlayer player) {
        if (workload == null) throw new IllegalStateException("Missing sustained coordinate workload");
        runSustainedCoordinateSearches(player, true);
        for (ServerPlayer fakePlayer : fakePlayers.resolve(server)) {
            runSustainedCoordinateSearches(fakePlayer, false);
        }
        if (!workload.completeAt(gameplayTicks)) return false;
        if (workloadSearches <= 0) throw new IllegalStateException("Sustained coordinate workload did not execute");
        return true;
    }

    private static void runSustainedCoordinateSearches(ServerPlayer player, boolean includeViewSearch) {
        ServerLevel level = player.serverLevel();
        SafeWorldCoordinate seed = new SafeWorldCoordinate(0.5D, PREPARED_GROUND_PLAYER_Y, 0.5D, level.dimension()).safe(true);
        for (int index = 0; index < WORKLOAD_SEARCHES_PER_TICK; index++) {
            if (NarcissusUtils.findSafeCoordinate(seed.clone(), player, false) == null) {
                throw new IllegalStateException("Sustained safe-coordinate search found no result");
            }
            workloadSearches++;
            if (includeViewSearch) {
                player.setYRot(-90.0F);
                player.setXRot(0.0F);
                if (NarcissusUtils.findViewEndCandidate(player, false, 16) == null) {
                    throw new IllegalStateException("Sustained view-end search found no result");
                }
                workloadSearches++;
            }
        }
    }

    private static void prepareFakeTeleportFixtures(MinecraftServer server, List<ServerPlayer> players) {
        ServerLevel overworld = server.overworld();
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether == null) {
            throw new IllegalStateException("Network smoke did not provide the Nether");
        }
        prepareSafeGround(nether);
        prepareViewCollisionRing(nether);
        for (int index = 0; index < players.size(); index++) {
            ServerPlayer player = players.get(index);
            double x = 2.5D + index * 4.0D;
            player.teleportTo(overworld, x, 70.0D, 2.5D, -90.0F, 0.0F);
            PlayerTeleportData.getData(player).setTeleportCountdownSeconds(EnumTeleportType.TP_COORDINATE, 0);

            Wolf follower = new Wolf(EntityType.WOLF, overworld);
            follower.tame(player);
            follower.setOrderedToSit(false);
            follower.moveTo(x + 1.0D, 70.0D, 2.5D, 0.0F, 0.0F);
            overworld.addFreshEntity(follower);

            SafeWorldCoordinate target = new SafeWorldCoordinate(x, 70.0D, 2.5D, nether.dimension()).safe(false);
            NarcissusUtils.teleportTo(player, target, EnumTeleportType.TP_COORDINATE);
        }
    }

    private static boolean fakeTeleportFixtureComplete(MinecraftServer server) {
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether == null) return false;
        List<ServerPlayer> players = fakePlayers.resolve(server);
        if (players.size() != 2) return false;
        for (ServerPlayer player : players) {
            if (player.serverLevel() != nether || !hasTeleportedFollower(nether, player)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasTeleportedFollower(ServerLevel level, ServerPlayer player) {
        for (Wolf wolf : level.getEntitiesOfClass(Wolf.class, player.getBoundingBox().inflate(8.0D))) {
            if (player.getUUID().equals(wolf.getOwnerUUID())) {
                return true;
            }
        }
        return false;
    }

    private static String describeFakeTeleportFixture(MinecraftServer server) {
        ServerLevel nether = server.getLevel(Level.NETHER);
        ServerLevel overworld = server.overworld();
        StringBuilder result = new StringBuilder();
        for (ServerPlayer player : fakePlayers.resolve(server)) {
            if (result.length() > 0) result.append("; ");
            int netherFollowers = countOwnedFollowers(nether, player);
            int overworldFollowers = countOwnedFollowers(overworld, player);
            result.append(player.getGameProfile().getName())
                    .append(" dimension=").append(player.serverLevel().dimension().location())
                    .append(" followersInNether=").append(netherFollowers)
                    .append(" followersInOverworld=").append(overworldFollowers);
        }
        return result.toString();
    }

    private static int countOwnedFollowers(ServerLevel level, ServerPlayer player) {
        if (level == null) return 0;
        int count = 0;
        for (Wolf wolf : level.getEntitiesOfClass(Wolf.class, player.getBoundingBox().inflate(128.0D))) {
            if (player.getUUID().equals(wolf.getOwnerUUID())) count++;
        }
        return count;
    }

    private static void prepareSafeGround(ServerLevel level) {
        for (int x = -64; x <= 64; x++) {
            for (int z = -64; z <= 64; z++) {
                level.setBlock(new BlockPos(x, 64, z), Blocks.STONE.defaultBlockState(), 3);
            }
        }
    }

    /**
     * 视线终点算法依赖可见碰撞体；环形低墙使测试不受玩家朝向或地形生成影响。
     */
    private static void prepareViewCollisionRing(ServerLevel level) {
        prepareViewCollisionRing((position, block) -> level.setBlock(position, block, 3));
    }

    static void prepareViewCollisionRing(BiConsumer<BlockPos, BlockState> writer) {
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                BlockState block = (Math.abs(x) == 8 || Math.abs(z) == 8 ? Blocks.STONE : Blocks.AIR)
                        .defaultBlockState();
                for (int y = 65; y <= 75; y++) {
                    writer.accept(new BlockPos(x, y, z), block);
                }
            }
        }
    }

    private static boolean movedFromGameplayOrigin(ServerPlayer player) {
        double dx = player.getX() - gameplayOriginX;
        double dz = player.getZ() - gameplayOriginZ;
        return dx * dx + dz * dz >= 4.0D;
    }

    private static String describeView(ServerPlayer player) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 view = player.getViewVector(1.0F);
        return "position=" + player.getX() + ',' + player.getY() + ',' + player.getZ()
                + " rotation=" + player.getYRot() + ',' + player.getXRot()
                + " eye=" + eye.x + ',' + eye.y + ',' + eye.z
                + " view=" + view.x + ',' + view.y + ',' + view.z;
    }

    private static void assertSafeGround(ServerPlayer player) {
        BlockPos below = player.blockPosition().below();
        if (!player.serverLevel().getBlockState(below).blocksMotion()) {
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
        xin.vanilla.narcissus.config.CommonConfig.save();
        xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeConfigs.verify(false);
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
        xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeConfigs.verify(false);
        NarcissusNetworkSmokeStatus.append("FINISHED phase-two");
        finished = true;
    }

    private enum GameplayStep {
        PREPARE_RANDOM,
        WAIT_RANDOM,
        PREPARE_VIEW,
        WAIT_VIEW,
        WAIT_FAKE_PLAYERS,
        WAIT_FAKE_TELEPORTS,
        SUSTAINED,
        WAIT_FAKE_CLEANUP,
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
                builderType.getMethod("threadGrouper", Supplier.class).invoke(builder, threadGrouper);
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
                Supplier<Object> classSourceLookup = () -> invokeClassSourceLookup(platform);
                Method mergeStrategy = findOptionalMethod(propsType, "mergeStrategy", 1);
                if (mergeStrategy != null) {
                    Class<?> mergeStrategyType = Class.forName(
                            "me.lucko.spark.common.sampler.java.MergeStrategy", true, loader);
                    Object sameMethod = mergeStrategyType.getField("SAME_METHOD").get(null);
                    mergeStrategy.invoke(props, sameMethod);
                } else {
                    Supplier<Object> mergeMode = ReflectiveSparkProfile::newMergeMode;
                    propsType.getMethod("mergeMode", Supplier.class).invoke(props, mergeMode);
                }
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

        private static Method findOptionalMethod(Class<?> type, String name, int parameterCount) {
            for (Method method : type.getMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
                    return method;
                }
            }
            return null;
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
