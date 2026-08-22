package xin.vanilla.narcissus.internal.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ConnectingScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.nbt.CompoundNBT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.enums.EnumWhiteListMode;
import xin.vanilla.narcissus.internal.client.NarcissusClientSyncState;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeFixture;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus;
import xin.vanilla.narcissus.network.packet.AccessListEditToServer;
import xin.vanilla.narcissus.network.packet.PlayerConfigSyncToServer;

import javax.annotation.Nonnull;

/**
 * 自动连接独立服务端并验证玩家配置与访问名单的真实网络往返。
 */
public final class NarcissusNetworkSmokeClientRunner {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int TIMEOUT_TICKS = 1200;
    private static final int SERVER_SETTLE_TICKS = 20;

    private static NarcissusNetworkSmokeClientRunner instance;

    private State state = State.CONNECT;
    private int ticks;
    private long syncGeneration;

    private NarcissusNetworkSmokeClientRunner() {
    }

    public static void register() {
        if (NarcissusNetworkSmokeStatus.enabled()) {
            instance = new NarcissusNetworkSmokeClientRunner();
        }
    }

    public static void tick(@Nonnull Minecraft client) {
        if (instance != null) {
            instance.runTick(client);
        }
    }

    private void runTick(Minecraft client) {
        if (++ticks > TIMEOUT_TICKS) {
            fail(client, "Timed out in state " + state);
            return;
        }
        try {
            switch (state) {
                case CONNECT:
                    if (ticks >= 20) {
                        connect(client);
                    }
                    break;
                case LOGIN_SYNC:
                    waitForLoginSync(client);
                    break;
                case CONFIG_ECHO:
                    waitForConfigEcho(client);
                    break;
                case ACCESS_ECHO:
                    waitForAccessEcho(client);
                    break;
                case SERVER_SETTLE:
                    waitForServerSettle(client);
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
        String host = System.getProperty("narcissus.networkSmoke.host", "127.0.0.1");
        int port = Integer.getInteger("narcissus.networkSmoke.port", 25576);
        ServerData server = new ServerData("Narcissus Network Smoke", host + ":" + port, false);
        client.setCurrentServer(server);
        client.setScreen(new ConnectingScreen(client.screen, client, server));
        NarcissusNetworkSmokeStatus.append("CONNECT " + host + ":" + port);
        state = State.LOGIN_SYNC;
        ticks = 0;
    }

    private void waitForLoginSync(Minecraft client) {
        if (client.player == null || client.level == null || client.getSingleplayerServer() != null
                || NarcissusClientSyncState.playerDataGeneration() <= 0L) {
            return;
        }
        NarcissusNetworkSmokeStatus.append("PASS remote-login-sync");
        if ("phase-one".equals(NarcissusNetworkSmokeStatus.phase())) {
            CompoundNBT countdowns = new CompoundNBT();
            countdowns.putInt(EnumTeleportType.TP_HOME.name(), NarcissusNetworkSmokeFixture.COUNTDOWN);
            syncGeneration = NarcissusClientSyncState.playerDataGeneration();
            PacketUtils.sendPacketToServer(new PlayerConfigSyncToServer(countdowns));
            state = State.CONFIG_ECHO;
        } else if ("phase-two".equals(NarcissusNetworkSmokeStatus.phase())) {
            verifyPersistedClientData(client);
            finish(client, "phase-two");
        } else {
            throw new IllegalStateException("Unknown network smoke phase: " + NarcissusNetworkSmokeStatus.phase());
        }
        ticks = 0;
    }

    private void waitForConfigEcho(Minecraft client) {
        if (NarcissusClientSyncState.playerDataGeneration() <= syncGeneration) {
            return;
        }
        PlayerTeleportData data = PlayerTeleportData.getData(client.player);
        if (data.getTeleportCountdownSeconds(EnumTeleportType.TP_HOME) != NarcissusNetworkSmokeFixture.COUNTDOWN) {
            throw new IllegalStateException("Player countdown did not round-trip");
        }
        NarcissusNetworkSmokeStatus.append("PASS config-roundtrip");
        syncGeneration = NarcissusClientSyncState.playerDataGeneration();
        PacketUtils.sendPacketToServer(new AccessListEditToServer(
                2, EnumWhiteListMode.AUTO_ACCEPT_TPA.name(), NarcissusNetworkSmokeFixture.ACCESS_UUID));
        state = State.ACCESS_ECHO;
        ticks = 0;
    }

    private void waitForAccessEcho(Minecraft client) {
        if (NarcissusClientSyncState.playerDataGeneration() <= syncGeneration) {
            return;
        }
        NarcissusNetworkSmokeFixture.verifyAccess(PlayerTeleportData.getData(client.player).getAccess());
        NarcissusNetworkSmokeStatus.append("PASS access-list-roundtrip");
        // 留出一个短窗口，让服务端 tick 在玩家离线前验证同一份数据。
        state = State.SERVER_SETTLE;
        ticks = 0;
    }

    private void waitForServerSettle(Minecraft client) {
        if (ticks >= SERVER_SETTLE_TICKS) {
            finish(client, "phase-one");
        }
    }

    private void verifyPersistedClientData(Minecraft client) {
        PlayerTeleportData data = PlayerTeleportData.getData(client.player);
        if (data.getTeleportCountdownSeconds(EnumTeleportType.TP_HOME) != NarcissusNetworkSmokeFixture.COUNTDOWN) {
            throw new IllegalStateException("Persisted player countdown was not synchronized");
        }
        NarcissusNetworkSmokeStatus.append("PASS persisted-player-config-client");
        NarcissusNetworkSmokeFixture.verifyAccess(data.getAccess());
        NarcissusNetworkSmokeStatus.append("PASS persisted-access-list-client");
    }

    private void finish(Minecraft client, String phase) {
        state = State.FINISHED;
        NarcissusNetworkSmokeStatus.append("FINISHED " + phase);
        LOGGER.info("Narcissus network smoke client finished {}", phase);
        client.stop();
    }

    private void fail(Minecraft client, String message) {
        state = State.FINISHED;
        NarcissusNetworkSmokeStatus.append("FAIL client " + message);
        LOGGER.error("Narcissus network smoke client failed: {}", message);
        client.stop();
    }

    private enum State {
        CONNECT,
        LOGIN_SYNC,
        CONFIG_ECHO,
        ACCESS_ECHO,
        SERVER_SETTLE,
        FINISHED
    }
}
