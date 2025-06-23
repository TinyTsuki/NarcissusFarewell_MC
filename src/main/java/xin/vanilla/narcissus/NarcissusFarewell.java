package xin.vanilla.narcissus;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.command.FarewellCommand;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.CustomConfig;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.SafeBlock;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.event.ClientModEventHandler;
import xin.vanilla.narcissus.network.ModNetworkHandler;
import xin.vanilla.narcissus.network.packet.SplitPacket;
import xin.vanilla.narcissus.util.LogoModifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod(NarcissusFarewell.MODID)
public class NarcissusFarewell {

    public final static String DEFAULT_COMMAND_PREFIX = "narcissus";

    public static final String MODID = "narcissus_farewell";
    public static final String ARTIFACT_ID = "xin.vanilla";

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 服务端实例
     */
    @Getter
    private static MinecraftServer serverInstance;

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
    private static final Map<ServerPlayer, ServerPlayer> lastTeleportRequest = new ConcurrentHashMap<>();

    /**
     * 待处理的传送请求列表
     */
    @Getter
    private static final Map<String, TeleportRequest> teleportRequest = new ConcurrentHashMap<>();

    @Getter
    private static final SafeBlock safeBlock = new SafeBlock();

    public NarcissusFarewell(IEventBus modEventBus) {

        // 注册网络通道
        ModNetworkHandler.registerPackets();

        // 注册服务器启动和关闭事件
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);

        // 注册当前实例到事件总线
        NeoForge.EVENT_BUS.register(this);

        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.COMMON_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SERVER_CONFIG);

        // 注册客户端设置事件
        modEventBus.addListener(this::onClientSetup);
        // 注册公共设置事件
        modEventBus.addListener(this::onCommonSetup);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(ClientModEventHandler::registerKeyBindings);
        }
    }

    /**
     * 客户端设置阶段事件
     */
    @SubscribeEvent
    public void onClientSetup(final FMLClientSetupEvent event) {
        // 修改logo为随机logo
        ModList.get().getMods().stream()
                .filter(info -> info.getModId().equals(MODID))
                .findFirst()
                .ifPresent(LogoModifier::modifyLogo);
    }

    /**
     * 公共设置阶段事件
     */
    @SubscribeEvent
    public void onCommonSetup(final FMLCommonSetupEvent event) {
        CustomConfig.loadCustomConfig(false);
    }

    private void onServerStarting(ServerStartingEvent event) {
        serverInstance = event.getServer();
    }

    private void onServerStarted(ServerStartedEvent event) {
    }

    private void onServerStopping(ServerStoppingEvent event) {
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.debug("Registering commands");
        FarewellCommand.register(event.getDispatcher());
    }


    // region 资源ID

    public static ResourceLocation emptyResource() {
        return createResource("", "");
    }

    public static ResourceLocation createResource(String path) {
        return createResource(NarcissusFarewell.MODID, path);
    }

    public static ResourceLocation createResource(String namespace, String path) {
        return new ResourceLocation(namespace, path);
    }

    public static ResourceLocation parseResource(String location) {
        return ResourceLocation.tryParse(location);
    }

    // endregion 资源ID


    // region 外部方法
    public void reloadCustomConfig() {
        CustomConfig.loadCustomConfig(false);
    }
    // endregion 外部方法

}
