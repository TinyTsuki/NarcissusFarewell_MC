package xin.vanilla.narcissus.internal.server.dev;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import xin.vanilla.banira.api.BaniraServer;
import xin.vanilla.banira.api.event.BaniraEvents;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeFixture;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.List;

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

    private static final int GAMEPLAY_TIMEOUT_TICKS = 400;
    private static final int PROFILER_SETTLE_TICKS = 260;

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
                prepareSafeGround(level);
                player.teleportTo(level, 0.5D, 65.0D, 0.5D, -90.0F, 0.0F);
                gameplayOriginX = player.getX();
                gameplayOriginZ = player.getZ();
                if (!CommandUtils.executeCommand(player, "spark profiler start", 4, false)) {
                    throw new IllegalStateException("Spark profiler command was unavailable");
                }
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
                player.teleportTo(level, 0.5D, 65.0D, 0.5D, -90.0F, 0.0F);
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
                NarcissusNetworkSmokeStatus.append("WAIT spark-profiler");
                gameplayStep = GameplayStep.WAIT_PROFILER;
                gameplayTicks = 0;
                return false;
            case WAIT_PROFILER:
                if (gameplayTicks < PROFILER_SETTLE_TICKS) {
                    return false;
                }
                if (!CommandUtils.executeCommand(player, "spark profiler stop", 4, false)) {
                    throw new IllegalStateException("Spark profiler did not stop cleanly");
                }
                NarcissusNetworkSmokeStatus.append("PASS spark-profiler-window");
                gameplayStep = GameplayStep.COMPLETE;
                return true;
            case COMPLETE:
                return true;
            default:
                throw new IllegalStateException("Unknown gameplay smoke step " + gameplayStep);
        }
    }

    private static void prepareSafeGround(ServerLevel level) {
        for (int x = -16; x <= 24; x++) {
            for (int z = -16; z <= 16; z++) {
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
        WAIT_PROFILER,
        COMPLETE
    }
}
