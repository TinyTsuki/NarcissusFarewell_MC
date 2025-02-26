package xin.vanilla.narcissus;

import com.mojang.brigadier.CommandDispatcher;
import lombok.Getter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.command.FarewellCommand;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.event.ClientEventHandler;
import xin.vanilla.narcissus.network.ModNetworkHandler;
import xin.vanilla.narcissus.network.packet.SplitPacket;

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
     * 默认语言
     */
    public static final String DEFAULT_LANGUAGE = "en_us";

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

    public NarcissusFarewell(IEventBus modEventBus) {

        // 注册网络通道
        ModNetworkHandler.registerPackets();
        // 注册服务器启动和关闭事件
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);

        // 注册服务端配置
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SERVER_CONFIG);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(ClientEventHandler::onClientTick);
        }

        // 注册当前实例到MinecraftForge的事件总线，以便监听和处理游戏内的各种事件
        NeoForge.EVENT_BUS.register(this);
    }

    // 服务器启动时加载数据
    private void onServerStarting(ServerStartingEvent event) {
        serverInstance = event.getServer();
        // 注册传送命令到事件调度器
        LOGGER.debug("Registering commands");
        FarewellCommand.register(commandDispatcher);
    }

    private static CommandDispatcher<CommandSourceStack> commandDispatcher;

    /**
     * 注册命令事件的处理方法
     * 当注册命令事件被触发时，此方法将被调用
     * 该方法主要用于注册传送命令到事件调度器
     *
     * @param event 注册命令事件对象，通过该对象可以获取到事件调度器
     */
    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        commandDispatcher = event.getDispatcher();
    }

}
