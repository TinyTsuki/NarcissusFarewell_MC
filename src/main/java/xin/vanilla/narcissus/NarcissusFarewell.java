package xin.vanilla.narcissus;

import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.api.BaniraModPresence;
import xin.vanilla.banira.api.event.BaniraEvents;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeBlock;
import java.io.IOException;
import net.minecraft.server.MinecraftServer;
import xin.vanilla.banira.api.BaniraDataPaths;
import xin.vanilla.narcissus.internal.server.NarcissusCostService;
import xin.vanilla.narcissus.internal.server.NarcissusSearchService;
import xin.vanilla.narcissus.internal.fabric.cost.FabricCostMigrationFile;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.event.EventHandlerProxy;
import xin.vanilla.narcissus.internal.server.dev.NarcissusNetworkSmokeServerRunner;
import xin.vanilla.narcissus.network.NetworkInit;
import xin.vanilla.narcissus.network.packet.StageDataSyncToClient;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.NarcissusUtils;
import xin.vanilla.narcissus.util.TeleportCountdownTracker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 加载器无关的 Narcissus 公共初始化与共享状态。
 */
public final class NarcissusFarewell {
    public static final String DEFAULT_COMMAND_PREFIX = "narcissus";
    public static final String MODID = "narcissus_farewell";

    private static boolean initialized;

    @Getter
    private static final Map<ServerPlayer, ServerPlayer> lastTeleportRequest = new ConcurrentHashMap<>();
    @Getter
    private static final Map<String, TeleportRequest> teleportRequest = new ConcurrentHashMap<>();
    @Getter
    private static final SafeBlock safeBlock = new SafeBlock();

    private NarcissusFarewell() {
    }

    /**
     * 由加载器公共 entrypoint 调用一次。
     */
    private static void migrateCosts() {
        try {
            new FabricCostMigrationFile().migrateBeforeRegistration(
                    BaniraDataPaths.gameConfigPath().resolve(MODID + "-common.toml"),
                    BaniraDataPaths.configPath().resolve(MODID));
        } catch (IOException error) {
            throw new IllegalStateException("Cost migration blocked; original configuration has been preserved", error);
        }
    }

    public static synchronized void bootstrapCommon() {
        if (initialized) {
            return;
        }
        initialized = true;
        migrateCosts();

        BaniraConfigs.register(CommonConfig.class, MODID);
        BaniraConfigs.register(ClientConfig.class, MODID);
        NetworkInit.registerPackets();

        BaniraEvents.Server.onStarting(event -> {
            MinecraftServer server = event.serverAs(MinecraftServer.class);
            NarcissusCostService.start(server);
            NarcissusSearchService.start(server);
        });
        BaniraEvents.Server.onStopping(event -> {
            TeleportCountdownTracker.clear();
            NarcissusSearchService.stop();
            NarcissusCostService.stop();
            getTeleportRequest().clear();
            getLastTeleportRequest().clear();
            PlayerTeleportData.clear();
        });
        BaniraEvents.Player.onLoggedOut(event -> {
            NarcissusCostService service = NarcissusCostService.get();
            if (service != null && event.uuid() != null) service.disconnect(event.uuid());
            NarcissusSearchService search = NarcissusSearchService.get();
            if (search != null && event.uuid() != null) search.disconnect(event.uuid());
        });
        BaniraEvents.Server.onTick(event -> {
            EventHandlerProxy.onServerTick();
            NarcissusCostService service = NarcissusCostService.get();
            if (service != null) service.tick();
            NarcissusSearchService search = NarcissusSearchService.get();
            if (search != null) search.tick();
        });
        BaniraEvents.Player.onLoggedIn(event -> {
            ServerPlayer player = event.playerAs(ServerPlayer.class);
            if (player != null) {
                EventHandlerProxy.onPlayerJoinWorld(player);
            }
        });
        BaniraEvents.Player.onLoggedOut(event -> {
            if (event.uuid() != null) {
                TeleportCountdownTracker.onPlayerLogout(event.uuid());
            }
        });
        NarcissusNetworkSmokeServerRunner.register();

        // Fabric 没有延后的 common-setup 阶段，公共注册在 entrypoint 中立即完成。
        NarcissusNotificationTypes.registerAllOnServer();
        BaniraModPresence.register(MODID, player -> {
            if (!(player instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer serverPlayer = (ServerPlayer) player;
            PlayerTeleportData.syncPlayerData(serverPlayer);
            PacketUtils.sendPacketToPlayer(
                    new StageDataSyncToClient(WorldStageData.get().getStageCoordinate()), serverPlayer);
            NarcissusCostService service = NarcissusCostService.get();
                if (service != null) service.connect(serverPlayer);
            CommandUtils.refreshPermission(serverPlayer);
        });
    }
}
