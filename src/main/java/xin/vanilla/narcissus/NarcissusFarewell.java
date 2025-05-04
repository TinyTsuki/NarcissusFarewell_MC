package xin.vanilla.narcissus;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.logging.LogUtils;
import lombok.Getter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
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

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 服务端实例
     */
    @Getter
    private static MinecraftServer serverInstance;

    /**
     * 服务器是否已启动
     */
    private boolean serverStarted = false;

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

    /**
     * 命令调度器
     */
    private CommandDispatcher<CommandSourceStack> dispatcher;

    public NarcissusFarewell() {

        // 注册网络通道
        ModNetworkHandler.registerPackets();
        // 注册服务器启动和关闭事件
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);

        // 注册服务端配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SERVER_CONFIG);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(ClientEventHandler::registerKeyMappings);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        }

        // 注册当前实例到MinecraftForge的事件总线，以便监听和处理游戏内的各种事件
        MinecraftForge.EVENT_BUS.register(this);
    }

    /**
     * 在客户端设置阶段触发的事件处理方法
     * 此方法主要用于接收 FML 客户端设置事件，并执行相应的初始化操作
     */
    @SubscribeEvent
    public void onClientSetup(final FMLClientSetupEvent event) {
        // 修改logo为随机logo
        ModList.get().getMods().stream()
                .filter(info -> info.getModId().equals(MODID))
                .findFirst()
                .ifPresent(LogoModifier::modifyLogo);
    }

    private void onServerStarting(ServerStartingEvent event) {
        serverInstance = event.getServer();
    }

    private void onServerStarted(ServerStartedEvent event) {
        this.serverStarted = true;
        this.registerCommands();
    }

    private void onServerStopping(ServerStoppingEvent event) {
        this.serverStarted = false;
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        this.dispatcher = event.getDispatcher();
        this.registerCommands();
    }

    private void registerCommands() {
        if (serverStarted && dispatcher != null) {
            LOGGER.debug("Registering commands");
            // 注册传送命令到事件调度器
            FarewellCommand.register(this.dispatcher);
        }
    }

}
