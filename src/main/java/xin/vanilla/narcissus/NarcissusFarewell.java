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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

// 先运行 gradle tasks: generateBuildConfig 生成 BuildConfig
@Mod(
    modid = BuildConfig.MODID,
    name = BuildConfig.NAME,
    version = BuildConfig.VERSION,
    useMetadata = true,
    acceptableRemoteVersions = "*")
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
        Arrays.stream(ECommandType.values())
            .filter(it -> Objects.nonNull(it.getInstance()))
            .filter(NarcissusUtils::isConciseEnabled)
            .map(ECommandType::getInstance)
            .map(Supplier::get)
            .forEach(event::registerServerCommand);

        event.registerServerCommand(new FarewellCommand());
    }

}
