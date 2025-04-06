package xin.vanilla.narcissus;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.command.FarewellCommand;
import xin.vanilla.narcissus.command.concise.*;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.event.ClientEventHandler;
import xin.vanilla.narcissus.event.ForgeEventHandler;
import xin.vanilla.narcissus.network.ModNetworkHandler;
import xin.vanilla.narcissus.network.SplitPacket;
import xin.vanilla.narcissus.util.DimensionUtils;
import xin.vanilla.narcissus.util.LogoModifier;
import xin.vanilla.narcissus.util.NarcissusUtils;
import xin.vanilla.narcissus.util.ServerTaskExecutor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 先运行 gradle tasks: generateBuildConfig 生成 BuildConfig
@Mod(modid = BuildConfig.MODID, name = BuildConfig.NAME, version = BuildConfig.VERSION, useMetadata = true, acceptableRemoteVersions = "*")
public class NarcissusFarewell {
    @Mod.Instance(BuildConfig.MODID)
    public static NarcissusFarewell instance;

    public final static String DEFAULT_COMMAND_PREFIX = "narcissus";

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
    private static final Map<EntityPlayerMP, EntityPlayerMP> lastTeleportRequest = new ConcurrentHashMap<>();

    /**
     * 待处理的传送请求列表
     * reqId:req
     */
    @Getter
    private static final Map<String, TeleportRequest> teleportRequest = new ConcurrentHashMap<>();

    // @SidedProxy(clientSide = "xin.vanilla.narcissus.proxy.ClientProxy", serverSide = "xin.vanilla.narcissus.proxy.ServerProxy")
    // public static IProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // 注册网络通道
        ModNetworkHandler.registerPackets();

        // 注册服务端配置
        ServerConfig.init(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        new ForgeEventHandler();
        // ForgeChunkManager.setForcedChunkLoadingCallback(NarcissusFarewell.instance, new ChunkLoadingCallback());
        // 仅在客户端执行的代码
        if (event.getSide().isClient()) {
            // 注册键盘按键绑定
            ClientEventHandler.registerKeyBindings();
            new ClientEventHandler();

            // 修改logo为随机logo
            Loader.instance().getModList().stream()
                    .filter(info -> info.getModId().equals(BuildConfig.MODID))
                    .findFirst()
                    .ifPresent(LogoModifier::modifyLogo);
        }
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent event) {
        serverInstance = event.getServer();
        ServerTaskExecutor.init();
        DimensionUtils.init();
        LOGGER.debug("Registering commands");
        event.registerServerCommand(new FarewellCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.UUID))
            event.registerServerCommand(new UuidCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.CARD))
            event.registerServerCommand(new CardCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.DIMENSION))
            event.registerServerCommand(new DimensionCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.FEED))
            event.registerServerCommand(new FeedCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_COORDINATE))
            event.registerServerCommand(new CoordinateCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_STRUCTURE))
            event.registerServerCommand(new StructureCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_ASK))
            event.registerServerCommand(new AskCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_ASK_YES))
            event.registerServerCommand(new AskYesCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_ASK_NO))
            event.registerServerCommand(new AskNoCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_ASK_CANCEL))
            event.registerServerCommand(new AskCancelCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_HERE))
            event.registerServerCommand(new HereCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_HERE_YES))
            event.registerServerCommand(new HereYesCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_HERE_NO))
            event.registerServerCommand(new HereNoCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_HERE_CANCEL))
            event.registerServerCommand(new HereCancelCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_RANDOM))
            event.registerServerCommand(new RandomCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_SPAWN))
            event.registerServerCommand(new SpawnCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_WORLD_SPAWN))
            event.registerServerCommand(new WorldSpawnCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_TOP))
            event.registerServerCommand(new TopCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_BOTTOM))
            event.registerServerCommand(new BottomCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_UP))
            event.registerServerCommand(new UpCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_DOWN))
            event.registerServerCommand(new DownCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_VIEW))
            event.registerServerCommand(new ViewCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_HOME))
            event.registerServerCommand(new HomeCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.SET_HOME))
            event.registerServerCommand(new SetHomeCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.DEL_HOME))
            event.registerServerCommand(new DelHomeCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.GET_HOME))
            event.registerServerCommand(new GetHomeCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_STAGE))
            event.registerServerCommand(new StageCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.SET_STAGE))
            event.registerServerCommand(new SetStageCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.DEL_STAGE))
            event.registerServerCommand(new DelStageCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.GET_STAGE))
            event.registerServerCommand(new GetStageCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.TP_BACK))
            event.registerServerCommand(new BackCommand());
        if (NarcissusUtils.isConciseEnabled(ECommandType.VIRTUAL_OP))
            event.registerServerCommand(new VirtualOpCommand());
    }

}
