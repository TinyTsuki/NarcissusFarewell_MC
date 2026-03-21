package xin.vanilla.narcissus;

import lombok.Getter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.FolderName;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.Mod;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.client.event.BaniraClientEventHub;
import xin.vanilla.banira.client.gui.ConfigEditorScreen;
import xin.vanilla.banira.client.gui.quickaction.QuickActionContext;
import xin.vanilla.banira.client.gui.quickaction.QuickActionContextMenuItem;
import xin.vanilla.banira.client.gui.quickaction.QuickActionRegistry;
import xin.vanilla.banira.common.config.ForgeConfigAdapter;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.player.PlayerDataManager;
import xin.vanilla.banira.common.util.BaniraEventBus;
import xin.vanilla.banira.common.util.EnvironmentUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.command.NarcissusCommand;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeBlock;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.event.ClientModEventHandler;
import xin.vanilla.narcissus.event.EventHandlerProxy;
import xin.vanilla.narcissus.integration.ScreenHelper;
import xin.vanilla.narcissus.network.ModNetworkHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Mod(NarcissusFarewell.MODID)
public class NarcissusFarewell {

    public final static String DEFAULT_COMMAND_PREFIX = "narcissus";

    public static final String MODID = "narcissus_farewell";

    /**
     * 玩家数据管理器
     */
    public static final PlayerDataManager playerDataManager = PlayerDataManager.getOrCreateInstance(() ->
                    BaniraCodex.serverInstance().key().getWorldPath(FolderName.PLAYER_DATA_DIR)
            , MODID
            , StringUtils.reverseBySeparatorElegant(BaniraCodex.ARTIFACT_ID, ".")
    );

    /**
     * 最近一次传送请求
     */
    @Getter
    private static final Map<ServerPlayerEntity, ServerPlayerEntity> lastTeleportRequest = new ConcurrentHashMap<>();

    /**
     * 待处理的传送请求列表
     */
    @Getter
    private static final Map<String, TeleportRequest> teleportRequest = new ConcurrentHashMap<>();

    @Getter
    private static final SafeBlock safeBlock = new SafeBlock();

    public NarcissusFarewell() {
        // 注册网络通道
        ModNetworkHandler.registerPackets();

        BaniraEventBus.Server.onStopping(server -> PlayerTeleportData.clear());
        BaniraEventBus.Server.onStarting(server -> playerDataManager.clearCache());
        BaniraEventBus.Server.onTick(EventHandlerProxy::onServerTick);
        BaniraEventBus.Player.onClone(EventHandlerProxy::onPlayerCloned);
        BaniraEventBus.EntityEvents.onJoinWorld(EventHandlerProxy::onEntityJoinWorld);
        BaniraEventBus.EntityEvents.onTeleport(EventHandlerProxy::onEntityTeleport);
        BaniraEventBus.Commands.onRegister(event -> NarcissusCommand.register(event.getDispatcher()));
        BaniraEventBus.Save.onPlayerSave(player -> playerDataManager.saveToDisk(PlayerUtils.getPlayerUUID(player)));

        // 注册配置
        ForgeConfigAdapter.register(CommonConfig.class, MODID);
        ForgeConfigAdapter.register(ClientConfig.class, MODID);

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
                Component label = Component.transClient(MODID, "key.narcissus_farewell.categories");
                Consumer<QuickActionContext> action = ctx -> ScreenHelper.openScreen();
                QuickActionContextMenuItem editClientConfig = new QuickActionContextMenuItem(Component.transClientAuto(MODID, "edit_client_config"), ctx ->
                        ConfigEditorScreen.open(ClientConfig.get().holder(), ctx.currentScreen())
                );
                QuickActionContextMenuItem editCommonConfig = new QuickActionContextMenuItem(Component.transClientAuto(MODID, "edit_common_config"), ctx ->
                        ConfigEditorScreen.open(CommonConfig.get().holder(), ctx.currentScreen())
                );
                QuickActionRegistry.get().registerIcon(MODID + ":quick", texture, label, action, editClientConfig, editCommonConfig);
            });
        }
    }

}
