package xin.vanilla.narcissus;

import java.io.IOException;
import xin.vanilla.narcissus.util.TeleportCountdownTracker;
import xin.vanilla.narcissus.internal.server.NarcissusCostService;
import xin.vanilla.narcissus.internal.forge.cost.ForgeCostMigrationFile;
import xin.vanilla.banira.api.BaniraDataPaths;
import net.minecraft.server.MinecraftServer;
import lombok.Getter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.api.BaniraModPresence;
import xin.vanilla.banira.api.event.BaniraEvents;
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
import xin.vanilla.narcissus.internal.forge.event.ForgeNarcissusGameEventAdapter;
import xin.vanilla.narcissus.internal.server.dev.NarcissusNetworkSmokeServerRunner;
import xin.vanilla.narcissus.network.NetworkInit;
import xin.vanilla.narcissus.network.packet.StageDataSyncToClient;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(NarcissusFarewell.MODID)
public class NarcissusFarewell {

    private static final Logger LOGGER = LogManager.getLogger();

    public final static String DEFAULT_COMMAND_PREFIX = "narcissus";

    public static final String MODID = "narcissus_farewell";

    @Getter
    private static final Map<ServerPlayerEntity, ServerPlayerEntity> lastTeleportRequest = new ConcurrentHashMap<>();

    @Getter
    private static final Map<String, TeleportRequest> teleportRequest = new ConcurrentHashMap<>();

    @Getter
    private static final SafeBlock safeBlock = new SafeBlock();

    public NarcissusFarewell() {
        try {
            new ForgeCostMigrationFile().migrateBeforeRegistration(
                    BaniraDataPaths.gameConfigPath().resolve(MODID + "-common.toml"),
                    BaniraDataPaths.configPath().resolve(MODID));
        } catch (IOException error) {
            throw new IllegalStateException("Cost migration blocked; original configuration has been preserved", error);
        }
        // 注册配置
        BaniraConfigs.register(CommonConfig.class, MODID);
        BaniraConfigs.register(ClientConfig.class, MODID);

        // 注册网络通道
        NetworkInit.registerPackets();

        BaniraEvents.Server.onStarting(event -> NarcissusCostService.start(
                event.serverAs(MinecraftServer.class)));
        BaniraEvents.Server.onStopping(event -> {
            TeleportCountdownTracker.clear();
            NarcissusCostService.stop();
            getTeleportRequest().clear();
            getLastTeleportRequest().clear();
            PlayerTeleportData.clear();
        });
        BaniraEvents.Player.onLoggedOut(event -> {
            NarcissusCostService service = NarcissusCostService.get();
            if (service != null && event.uuid() != null) service.disconnect(event.uuid());
        });
        BaniraEvents.Server.onTick(event -> {
            EventHandlerProxy.onServerTick();
            NarcissusCostService service = NarcissusCostService.get();
            if (service != null) service.tick();
        });
        ForgeNarcissusGameEventAdapter.register();
        NarcissusNetworkSmokeServerRunner.register();

        BaniraEvents.onCommonSetup(event -> {
            NarcissusNotificationTypes.registerAllOnServer();
            BaniraModPresence.register(MODID, player -> {
                if (!(player instanceof ServerPlayerEntity)) {
                    return;
                }
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                // 同步玩家传送数据到客户端
                PlayerTeleportData.syncPlayerData(serverPlayer);
                // 同步驿站数据到客户端
                PacketUtils.sendPacketToPlayer(new StageDataSyncToClient(WorldStageData.get().getStageCoordinate()), serverPlayer);
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
