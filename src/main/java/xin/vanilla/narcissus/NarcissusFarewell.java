package xin.vanilla.narcissus;

import lombok.Getter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.api.BaniraDataPaths;
import xin.vanilla.banira.api.BaniraModPresence;
import xin.vanilla.banira.common.util.BaniraEventBus;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.EnvironmentUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.client.NarcissusClientBootstrap;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeBlock;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.event.EventHandlerProxy;
import xin.vanilla.narcissus.internal.forge.cost.ForgeCostMigrationFile;
import xin.vanilla.narcissus.internal.forge.event.ForgeNarcissusGameEventAdapter;
import xin.vanilla.narcissus.internal.server.NarcissusCostService;
import xin.vanilla.narcissus.internal.server.NarcissusSearchService;
import xin.vanilla.narcissus.internal.server.dev.NarcissusNetworkSmokeServerRunner;
import xin.vanilla.narcissus.network.NetworkInit;
import xin.vanilla.narcissus.network.packet.StageDataSyncToClient;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.TeleportCountdownTracker;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(NarcissusFarewell.MODID)
public class NarcissusFarewell {

    private static final Logger LOGGER = LogManager.getLogger();

    public final static String DEFAULT_COMMAND_PREFIX = "narcissus";

    public static final String MODID = "narcissus_farewell";

    @Getter
    private static final Map<ServerPlayer, ServerPlayer> lastTeleportRequest = new ConcurrentHashMap<>();

    @Getter
    private static final Map<String, TeleportRequest> teleportRequest = new ConcurrentHashMap<>();

    @Getter
    private static final SafeBlock safeBlock = new SafeBlock();

    private static void migrateCosts() {
        try {
            new ForgeCostMigrationFile().migrateBeforeRegistration(
                    BaniraDataPaths.gameConfigPath().resolve(MODID + "-common.toml"),
                    BaniraDataPaths.configPath().resolve(MODID));
        } catch (IOException error) {
            throw new IllegalStateException("Cost migration blocked; original configuration has been preserved", error);
        }
    }

    public NarcissusFarewell(FMLJavaModLoadingContext context) {
        migrateCosts();
        // 注册配置
        BaniraConfigs.register(CommonConfig.class, MODID);
        BaniraConfigs.register(ClientConfig.class, MODID);

        // 注册网络通道
        NetworkInit.registerPackets();

        BaniraEventBus.Server.onStarting(event -> {
            MinecraftServer server = event.serverAs(MinecraftServer.class);
            NarcissusCostService.start(server);
            NarcissusSearchService.start(server);
        });
        BaniraEventBus.Server.onStopping(event -> {
            TeleportCountdownTracker.clear();
            NarcissusSearchService.stop();
            NarcissusCostService.stop();
            getTeleportRequest().clear();
            getLastTeleportRequest().clear();
            PlayerTeleportData.clear();
        });
        BaniraEventBus.Player.onLoggedOut(event -> {
            NarcissusCostService service = NarcissusCostService.get();
            if (service != null && event.uuid() != null) service.disconnect(event.uuid());
            NarcissusSearchService search = NarcissusSearchService.get();
            if (search != null && event.uuid() != null) search.disconnect(event.uuid());
        });
        BaniraEventBus.Server.onTick(event -> {
            EventHandlerProxy.onServerTick();
            NarcissusCostService service = NarcissusCostService.get();
            if (service != null) service.tick();
            NarcissusSearchService search = NarcissusSearchService.get();
            if (search != null) search.tick();
        });
        ForgeNarcissusGameEventAdapter.register();
        NarcissusNetworkSmokeServerRunner.register();

        BaniraEventBus.ModLifecycle.onCommonSetup(event -> {
            NarcissusNotificationTypes.registerAllOnServer();
            BaniraModPresence.register(MODID, player -> {
                if (!(player instanceof ServerPlayer)) {
                    return;
                }
                ServerPlayer serverPlayer = (ServerPlayer) player;
                // 同步玩家传送数据到客户端
                PlayerTeleportData.syncPlayerData(serverPlayer);
                // 同步驿站数据到客户端
                PacketUtils.sendPacketToPlayer(new StageDataSyncToClient(WorldStageData.get().getStageCoordinate()), serverPlayer);
                // 同步传送代价配置到客户端
                NarcissusCostService service = NarcissusCostService.get();
                if (service != null) service.connect(serverPlayer);
                // 刷新权限信息
                CommandUtils.refreshPermission(serverPlayer);
            });
        });

        if (EnvironmentUtils.isClient()) {
            NarcissusClientBootstrap.init();
        }
    }
}
