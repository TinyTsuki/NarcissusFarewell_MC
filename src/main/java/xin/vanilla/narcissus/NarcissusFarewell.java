package xin.vanilla.narcissus;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.command.FarewellCommand;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.event.ClientEventHandler;
import xin.vanilla.narcissus.network.ModNetworkHandler;
import xin.vanilla.narcissus.network.SplitPacket;
import xin.vanilla.narcissus.util.LogoModifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(NarcissusFarewell.MODID)
public class NarcissusFarewell {

    public final static String DEFAULT_COMMAND_PREFIX = "narcissus";

    public static final String MODID = "narcissus_farewell";

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 服务端实例
     */
    @Getter
    private static MinecraftServer serverInstance;

    /**
     * 是否有对应的服务端
     */
    @Getter
    @Setter
    private static boolean enabled;
    /**
     * 玩家权限等级
     */
    @Getter
    @Setter
    private static int permissionLevel;
    /**
     * 玩家默认语言
     */
    @Getter
    @Setter
    private static String defaultLanguage = "en_us";

    /**
     * 分片网络包缓存
     */
    @Getter
    private static final Map<String, List<? extends SplitPacket>> packetCache = new ConcurrentHashMap<>();

    /**
     * 玩家能力同步状态
     */
    @Getter
    private static final Map<String, Boolean> playerCapabilityStatus = new ConcurrentHashMap<>();

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

    public NarcissusFarewell() {

        // 注册网络通道
        ModNetworkHandler.registerPackets();
        // 注册服务器启动和关闭事件
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);

        // 注册当前实例到MinecraftForge的事件总线，以便监听和处理游戏内的各种事件
        MinecraftForge.EVENT_BUS.register(this);

        // 注册服务器和客户端配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SERVER_CONFIG);

        // 注册客户端设置事件到MOD事件总线
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    }

    /**
     * 在客户端设置阶段触发的事件处理方法
     * 此方法主要用于接收 FML 客户端设置事件，并执行相应的初始化操作
     */
    @SubscribeEvent
    public void onClientSetup(final FMLClientSetupEvent event) {
        // 注册键绑定
        LOGGER.debug("Registering key bindings");
        ClientEventHandler.registerKeyBindings();
        // 修改logo为随机logo
        ModList.get().getMods().stream()
                .filter(info -> info.getModId().equals(MODID))
                .findFirst()
                .ifPresent(LogoModifier::modifyLogo);
    }

    // 服务器启动时加载数据
    private void onServerStarting(FMLServerStartingEvent event) {
        serverInstance = event.getServer();
        LOGGER.debug("Registering commands");
        FarewellCommand.register(event.getServer().getCommands().getDispatcher());
    }

    /**
     * 玩家注销事件
     *
     * @param event 玩家注销事件对象，通过该对象可以获取到注销的玩家对象
     */
    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LOGGER.debug("Player has logged out.");
        // 获取退出的玩家对象
        PlayerEntity player = event.getPlayer();
        // 判断是否在客户端并且退出的玩家是客户端的当前玩家
        if (player.getCommandSenderWorld().isClientSide) {
            if (Minecraft.getInstance().player.getUUID().equals(player.getUUID())) {
                LOGGER.debug("Current player has logged out.");
                // 当前客户端玩家与退出的玩家相同
                enabled = false;
            }
        }
    }
}
