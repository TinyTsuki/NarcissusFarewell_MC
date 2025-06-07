package xin.vanilla.narcissus;

import lombok.Getter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.command.FarewellCommand;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.CustomConfig;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.event.ClientModEventHandler;
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

        // 注册当前实例到事件总线
        MinecraftForge.EVENT_BUS.register(this);

        // 注册配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.COMMON_CONFIG);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SERVER_CONFIG);

        // 注册客户端设置事件
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        // 注册公共设置事件
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
    }

    /**
     * 客户端设置阶段事件
     */
    @SubscribeEvent
    public void onClientSetup(final FMLClientSetupEvent event) {
        // 注册键绑定
        LOGGER.debug("Registering key bindings");
        ClientModEventHandler.registerKeyBindings();
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

    private void onServerStarting(FMLServerStartingEvent event) {
        serverInstance = event.getServer();
        LOGGER.debug("Registering commands");
        FarewellCommand.register(event.getServer().getCommands().getDispatcher());
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
