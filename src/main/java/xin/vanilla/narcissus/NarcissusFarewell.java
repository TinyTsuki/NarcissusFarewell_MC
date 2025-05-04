package xin.vanilla.narcissus;

import com.mojang.brigadier.CommandDispatcher;
import lombok.Getter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.command.FarewellCommand;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerDataAttachment;
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

    public NarcissusFarewell(IEventBus modEventBus, ModContainer modContainer) {

        // 注册网络通道
        modEventBus.addListener(ModNetworkHandler::registerPackets);
        // 注册服务器启动和关闭事件
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);

        // 注册服务端配置
        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SERVER_CONFIG);
        // 注册数据附件
        PlayerDataAttachment.ATTACHMENT_TYPES.register(modEventBus);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.addListener(ClientEventHandler::onClientTick);
        }

        // 注册当前实例到MinecraftForge的事件总线，以便监听和处理游戏内的各种事件
        NeoForge.EVENT_BUS.register(this);
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
