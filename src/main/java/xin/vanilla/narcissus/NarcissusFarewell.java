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
import xin.vanilla.narcissus.data.TeleportCost;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.event.EventHandlerProxy;
import xin.vanilla.narcissus.internal.server.dev.NarcissusNetworkSmokeServerRunner;
import xin.vanilla.narcissus.network.NetworkInit;
import xin.vanilla.narcissus.network.packet.CostConfigSyncToClient;
import xin.vanilla.narcissus.network.packet.StageDataSyncToClient;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.NarcissusUtils;
import xin.vanilla.narcissus.util.TeleportCountdownTracker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 加载器无关的 Narcissus 公共初始化与共享状态。 */
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

    /** 由加载器公共 entrypoint 调用一次。 */
    public static synchronized void bootstrapCommon() {
        if (initialized) {
            return;
        }
        initialized = true;

        BaniraConfigs.register(CommonConfig.class, MODID);
        BaniraConfigs.register(ClientConfig.class, MODID);
        NetworkInit.registerPackets();

        BaniraEvents.Server.onStopping(server -> PlayerTeleportData.clear());
        BaniraEvents.Server.onTick(event -> EventHandlerProxy.onServerTick());
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
            Map<EnumTeleportType, TeleportCost> costMap = new HashMap<>();
            costMap.put(EnumTeleportType.TP_HOME, NarcissusUtils.getCommandCost(EnumTeleportType.TP_HOME));
            costMap.put(EnumTeleportType.TP_STAGE, NarcissusUtils.getCommandCost(EnumTeleportType.TP_STAGE));
            costMap.put(EnumTeleportType.TP_BACK, NarcissusUtils.getCommandCost(EnumTeleportType.TP_BACK));
            PacketUtils.sendPacketToPlayer(new CostConfigSyncToClient(costMap,
                    CommonConfig.get().general().teleportCostDistanceLimit(),
                    CommonConfig.get().general().teleportCostDistanceAcrossDimension()), serverPlayer);
            CommandUtils.refreshPermission(serverPlayer);
        });
    }
}
