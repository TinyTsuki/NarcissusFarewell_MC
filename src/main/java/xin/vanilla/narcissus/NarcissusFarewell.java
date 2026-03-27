package xin.vanilla.narcissus;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.client.gui.quickaction.QuickActionContext;
import xin.vanilla.banira.client.gui.quickaction.QuickActionContextMenuItem;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry;
import xin.vanilla.banira.common.config.ForgeConfigAdapter;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.network.ModLoadedPresence;
import xin.vanilla.banira.common.util.BaniraEventBus;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.EnvironmentUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.command.NarcissusCommand;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeBlock;
import xin.vanilla.narcissus.data.TeleportCost;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.event.ClientModEventHandler;
import xin.vanilla.narcissus.event.EventHandlerProxy;
import xin.vanilla.narcissus.integration.ScreenHelper;
import xin.vanilla.narcissus.network.NetworkInit;
import xin.vanilla.narcissus.network.packet.CostConfigSyncToClient;
import xin.vanilla.narcissus.network.packet.StageDataSyncToClient;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

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

    public NarcissusFarewell(FMLJavaModLoadingContext context) {
        // 注册网络通道
        NetworkInit.registerPackets();

        // 注册配置
        ForgeConfigAdapter.register(CommonConfig.class, MODID);
        ForgeConfigAdapter.register(ClientConfig.class, MODID);

        BaniraEventBus.Server.onStopping(server -> PlayerTeleportData.clear());
        BaniraEventBus.Server.onTick(EventHandlerProxy::onServerTick);
        BaniraEventBus.Player.onClone(EventHandlerProxy::onPlayerCloned);
        BaniraEventBus.EntityEvents.onJoinWorld(EventHandlerProxy::onEntityJoinWorld);
        BaniraEventBus.EntityEvents.onTeleport(EventHandlerProxy::onEntityTeleport);
        BaniraEventBus.Commands.onRegister(event -> NarcissusCommand.register(event.getDispatcher()));

        BaniraEventBus.ModLifecycle.onCommonSetup(event -> {
            ModLoadedPresence.register(MODID, player -> {
                // 同步玩家传送数据到客户端
                PlayerTeleportData.syncPlayerData(player);
                // 同步驿站数据到客户端
                PacketUtils.sendPacketToPlayer(NetworkInit.INSTANCE, new StageDataSyncToClient(WorldStageData.get().getStageCoordinate()), player);
                // 同步传送代价配置到客户端
                Map<EnumTeleportType, TeleportCost> costMap = new HashMap<>();
                costMap.put(EnumTeleportType.TP_HOME, NarcissusUtils.getCommandCost(EnumTeleportType.TP_HOME));
                costMap.put(EnumTeleportType.TP_STAGE, NarcissusUtils.getCommandCost(EnumTeleportType.TP_STAGE));
                costMap.put(EnumTeleportType.TP_BACK, NarcissusUtils.getCommandCost(EnumTeleportType.TP_BACK));
                PacketUtils.sendPacketToPlayer(NetworkInit.INSTANCE, new CostConfigSyncToClient(costMap,
                        CommonConfig.get().general().teleportCostDistanceLimit(),
                        CommonConfig.get().general().teleportCostDistanceAcrossDimension()), player);
                // 刷新权限信息
                CommandUtils.refreshPermission(player);
            });
        });

        if (EnvironmentUtils.isClient()) {
            ClientProxy.init();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ClientProxy {
        public static void init() {
            ClientModEventHandler.bootstrap();

            BaniraClientEventHub.ModLifecycle.onClientSetup(event -> {
                ResourceLocation texture = Identifier.id().create("gui/quick_icon.png");
                Component label = NarcissusComponent.get().transClient("key.narcissus_farewell.categories");
                Consumer<QuickActionContext> action = ctx -> ScreenHelper.openScreen();
                QuickActionContextMenuItem editClientConfig = new QuickActionContextMenuItem(NarcissusComponent.get().transClientAuto("edit_client_config"), ctx ->
                        ConfigEditorScreen.open(ClientConfig.get().holder(), ctx.currentScreen())
                );
                QuickActionContextMenuItem editCommonConfig = new QuickActionContextMenuItem(NarcissusComponent.get().transClientAuto("edit_common_config"), ctx ->
                        ConfigEditorScreen.open(CommonConfig.get().holder(), ctx.currentScreen())
                );
                QuickActionContextMenuItem editPlayerConfig = new QuickActionContextMenuItem(NarcissusComponent.get().transClientAuto("edit_player_config"), ctx ->
                        ScreenHelper.openPlayerTeleportPrefsScreen()
                );
                QuickActionContextMenuItem editAccessConfig = new QuickActionContextMenuItem(NarcissusComponent.get().transClientAuto("edit_access_config"), ctx ->
                        ScreenHelper.openAccessListScreen()
                );
                QuickActionRegistry.get().registerIcon(MODID + ":quick", texture, label, action, editAccessConfig, editPlayerConfig, editClientConfig, editCommonConfig);
            });
        }
    }

}
