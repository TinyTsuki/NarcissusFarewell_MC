package xin.vanilla.narcissus.config;

import net.minecraft.init.Blocks;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import xin.vanilla.narcissus.BuildConfig;
import xin.vanilla.narcissus.enums.ECardType;
import xin.vanilla.narcissus.enums.ECoolDownType;
import xin.vanilla.narcissus.enums.ECostType;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * 服务器配置
 */
public class ServerConfig {

    public static Configuration config;

    // region 基础设置

    private final static String CATEGORY_BASE = "base";

    /**
     * 传送卡
     */
    public static boolean TELEPORT_CARD = false;
    /**
     * 每日传送卡数量
     */
    public static int TELEPORT_CARD_DAILY = 0;
    /**
     * 传送卡应用方式
     */
    public static ECardType TELEPORT_CARD_TYPE = ECardType.REFUND_ALL_COST;

    /**
     * 历史传送记录数量限制
     */
    public static int TELEPORT_RECORD_LIMIT = 100;
    /**
     * 跨维度传送
     */
    public static boolean TELEPORT_ACROSS_DIMENSION = true;

    /**
     * 传送代价中传送距离最大取值
     */
    public static int TELEPORT_COST_DISTANCE_LIMIT = 10000;

    /**
     * 跨维度传送时传送代价中传送距离取值
     */
    public static int TELEPORT_COST_DISTANCE_ACROSS_DIMENSION = 10000;

    /**
     * 传送至视线尽头时最远传送距离限制
     */
    public static int TELEPORT_VIEW_DISTANCE_LIMIT = 16 * 64;

    /**
     * 传送请求过期时间
     */
    public static int TELEPORT_REQUEST_EXPIRE_TIME = 60;

    /**
     * 传送请求冷却时间计算方式
     */
    public static ECoolDownType TELEPORT_REQUEST_COOLDOWN_TYPE = ECoolDownType.INDIVIDUAL;

    /**
     * 传送请求冷却时间
     */
    public static int TELEPORT_REQUEST_COOLDOWN = 10;

    /**
     * 随机传送距离限制
     */
    public static int TELEPORT_RANDOM_DISTANCE_LIMIT = 10000;

    /**
     * 家的数量
     */
    public static int TELEPORT_HOME_LIMIT = 5;

    /**
     * 命令前缀
     */
    public static String COMMAND_PREFIX = "narcissus";

    /**
     * 不安全的方块
     */
    public static String[] UNSAFE_BLOCKS = Stream.of(
                    Blocks.LAVA,
                    Blocks.FIRE,
                    Blocks.CACTUS
            ).map(block -> block.getRegistryName().toString())
            .toArray(String[]::new);

    /**
     * 窒息的方块
     */
    public static String[] SUFFOCATING_BLOCKS = Stream.of(
                    Blocks.LAVA,
                    Blocks.WATER
            ).map(block -> block.getRegistryName().toString())
            .toArray(String[]::new);

    /**
     * 当安全传送未找到安全坐标时，是否在脚下放置方块
     */
    public static boolean SETBLOCK_WHEN_SAFE_NOT_FOUND = false;

    /**
     * 当安全传送未找到安全坐标时，是否从背包中获取被放置的方块
     */
    public static boolean GETBLOCK_FROM_INVENTORY = true;

    /**
     * 当安全传送未找到安全坐标时，放置的方块类型
     */
    public static String[] SAFE_BLOCKS = Stream.of(
                    Blocks.GRASS,
                    Blocks.GRASS_PATH,
                    Blocks.DIRT,
                    Blocks.COBBLESTONE
            ).map(block -> block.getRegistryName().toString())
            .toArray(String[]::new);

    // endregion 基础设置

    // region 功能开关

    private final static String CATEGORY_SWITCH = "switch";

    /**
     * 自杀或毒杀 开关
     */
    public static boolean SWITCH_FEED = true;

    /**
     * 传送到指定坐标 开关
     */
    public static boolean SWITCH_TP_COORDINATE = true;

    /**
     * 传送到指定结构 开关
     */
    public static boolean SWITCH_TP_STRUCTURE = true;

    /**
     * 请求传送至玩家 开关
     */
    public static boolean SWITCH_TP_ASK = true;

    /**
     * 请求将玩家传送至当前位置 开关
     */
    public static boolean SWITCH_TP_HERE = true;

    /**
     * 随机传送 开关
     */
    public static boolean SWITCH_TP_RANDOM = true;

    /**
     * 传送到玩家重生点 开关
     */
    public static boolean SWITCH_TP_SPAWN = true;

    /**
     * 传送到世界重生点 开关
     */
    public static boolean SWITCH_TP_WORLD_SPAWN = true;

    /**
     * 传送到顶部 开关
     */
    public static boolean SWITCH_TP_TOP = true;

    /**
     * 传送到底部 开关
     */
    public static boolean SWITCH_TP_BOTTOM = true;

    /**
     * 传送到上方 开关
     */
    public static boolean SWITCH_TP_UP = true;

    /**
     * 传送到下方 开关
     */
    public static boolean SWITCH_TP_DOWN = true;

    /**
     * 传送至视线尽头 开关
     */
    public static boolean SWITCH_TP_VIEW = true;

    /**
     * 传送到家 开关
     */
    public static boolean SWITCH_TP_HOME = true;

    /**
     * 传送到驿站 开关
     */
    public static boolean SWITCH_TP_STAGE = true;

    /**
     * 传送到上次传送点 开关
     */
    public static boolean SWITCH_TP_BACK = true;

    // endregion 功能开关

    // region 指令权限

    private final static String CATEGORY_PERMISSION = "permission";

    public static int PERMISSION_FEED_OTHER = 2;

    public static int PERMISSION_TP_COORDINATE = 2;

    public static int PERMISSION_TP_STRUCTURE = 2;

    public static int PERMISSION_TP_ASK = 0;

    public static int PERMISSION_TP_HERE = 0;

    public static int PERMISSION_TP_RANDOM = 1;

    public static int PERMISSION_TP_SPAWN = 0;

    public static int PERMISSION_TP_SPAWN_OTHER = 2;

    public static int PERMISSION_TP_WORLD_SPAWN = 0;

    public static int PERMISSION_TP_TOP = 1;

    public static int PERMISSION_TP_BOTTOM = 1;

    public static int PERMISSION_TP_UP = 1;

    public static int PERMISSION_TP_DOWN = 1;

    public static int PERMISSION_TP_VIEW = 1;

    public static int PERMISSION_TP_HOME = 0;

    public static int PERMISSION_TP_STAGE = 0;

    public static int PERMISSION_SET_STAGE = 2;

    public static int PERMISSION_DEL_STAGE = 2;

    public static int PERMISSION_TP_BACK = 0;

    /**
     * 跨维度传送到指定坐标权限
     */
    public static int PERMISSION_TP_COORDINATE_ACROSS_DIMENSION = 2;

    /**
     * 跨维度传送到指定结构权限
     */
    public static int PERMISSION_TP_STRUCTURE_ACROSS_DIMENSION = 2;

    /**
     * 跨维度请求传送至玩家权限
     */
    public static int PERMISSION_TP_ASK_ACROSS_DIMENSION = 0;

    /**
     * 跨维度请求将玩家传送至当前位置权限
     */
    public static int PERMISSION_TP_HERE_ACROSS_DIMENSION = 0;

    /**
     * 跨维度随机传送权限
     */
    public static int PERMISSION_TP_RANDOM_ACROSS_DIMENSION = 0;

    /**
     * 跨维度传送到玩家重生点权限
     */
    public static int PERMISSION_TP_SPAWN_ACROSS_DIMENSION = 0;

    /**
     * 跨维度传送到世界重生点权限
     */
    public static int PERMISSION_TP_WORLD_SPAWN_ACROSS_DIMENSION = 0;

    /**
     * 跨维度传送到家权限
     */
    public static int PERMISSION_TP_HOME_ACROSS_DIMENSION = 0;

    /**
     * 跨维度传送到驿站权限
     */
    public static int PERMISSION_TP_STAGE_ACROSS_DIMENSION = 0;

    /**
     * 跨维度传送到上次传送点权限
     */
    public static int PERMISSION_TP_BACK_ACROSS_DIMENSION = 0;

    // endregion 指令权限

    // region 冷却时间

    private final static String CATEGORY_COOLDOWN = "cooldown";

    /**
     * 传送到指定坐标冷却时间
     */
    public static int COOLDOWN_TP_COORDINATE = 10;

    /**
     * 传送到指定结构冷却时间
     */
    public static int COOLDOWN_TP_STRUCTURE = 10;

    /**
     * 请求传送至玩家冷却时间
     */
    public static int COOLDOWN_TP_ASK = 10;

    /**
     * 请求将玩家传送至当前位置冷却时间
     */
    public static int COOLDOWN_TP_HERE = 10;

    /**
     * 随机传送冷却时间
     */
    public static int COOLDOWN_TP_RANDOM = 10;

    /**
     * 传送到玩家重生点冷却时间
     */
    public static int COOLDOWN_TP_SPAWN = 10;

    /**
     * 传送到世界重生点冷却时间
     */
    public static int COOLDOWN_TP_WORLD_SPAWN = 10;

    /**
     * 传送到顶部冷却时间
     */
    public static int COOLDOWN_TP_TOP = 10;

    /**
     * 传送到底部冷却时间
     */
    public static int COOLDOWN_TP_BOTTOM = 10;

    /**
     * 传送到上方冷却时间
     */
    public static int COOLDOWN_TP_UP = 10;

    /**
     * 传送到下方冷却时间
     */
    public static int COOLDOWN_TP_DOWN = 10;

    /**
     * 传送至视线尽头冷却时间
     */
    public static int COOLDOWN_TP_VIEW = 10;

    /**
     * 传送到家冷却时间
     */
    public static int COOLDOWN_TP_HOME = 10;

    /**
     * 传送到驿站冷却时间
     */
    public static int COOLDOWN_TP_STAGE = 10;

    /**
     * 传送到上次传送点冷却时间
     */
    public static int COOLDOWN_TP_BACK = 10;

    // endregion 冷却时间

    // region 自定义指令

    private final static String CATEGORY_COMMAND = "command";

    /**
     * 获取当前世界的维度ID
     */
    public static String COMMAND_DIMENSION = "dim";

    /**
     * 自杀或毒杀(水仙是有毒的可不能吃哦)
     */
    public static String COMMAND_FEED = "feed";

    /**
     * 传送到指定坐标
     */
    public static String COMMAND_TP_COORDINATE = "tpx";

    /**
     * 传送到指定结构
     */
    public static String COMMAND_TP_STRUCTURE = "tpst";

    /**
     * 请求传送至玩家
     */
    public static String COMMAND_TP_ASK = "tpa";

    /**
     * 接受请求传送至玩家
     */
    public static String COMMAND_TP_ASK_YES = "tpay";

    /**
     * 拒绝请求传送至玩家
     */
    public static String COMMAND_TP_ASK_NO = "tpan";

    /**
     * 请求将玩家传送至当前位置
     */
    public static String COMMAND_TP_HERE = "tph";

    /**
     * 接受请求将玩家传送至当前位置
     */
    public static String COMMAND_TP_HERE_YES = "tphy";

    /**
     * 拒绝请求将玩家传送至当前位置
     */
    public static String COMMAND_TP_HERE_NO = "tphn";

    /**
     * 随机传送
     */
    public static String COMMAND_TP_RANDOM = "tpr";

    /**
     * 传送到玩家重生点
     */
    public static String COMMAND_TP_SPAWN = "tpsp";

    /**
     * 传送到世界重生点
     */
    public static String COMMAND_TP_WORLD_SPAWN = "tpws";

    /**
     * 传送到顶部
     */
    public static String COMMAND_TP_TOP = "tpt";

    /**
     * 传送到底部
     */
    public static String COMMAND_TP_BOTTOM = "tpb";

    /**
     * 传送到上方
     */
    public static String COMMAND_TP_UP = "tpu";

    /**
     * 传送到下方
     */
    public static String COMMAND_TP_DOWN = "tpd";

    /**
     * 传送至视线尽头
     */
    public static String COMMAND_TP_VIEW = "tpv";

    /**
     * 传送到家
     */
    public static String COMMAND_TP_HOME = "home";

    /**
     * 设置家
     */
    public static String COMMAND_SET_HOME = "sethome";

    /**
     * 删除家
     */
    public static String COMMAND_DEL_HOME = "delhome";

    /**
     * 传送到驿站
     */
    public static String COMMAND_TP_STAGE = "stage";

    /**
     * 设置驿站
     */
    public static String COMMAND_SET_STAGE = "setstage";

    /**
     * 删除驿站
     */
    public static String COMMAND_DEL_STAGE = "delstage";

    /**
     * 传送到上次传送点
     */
    public static String COMMAND_TP_BACK = "back";

    // endregion 自定义指令

    // region 简化指令

    private final static String CATEGORY_CONCISE = "concise";

    /**
     * 获取当前世界的维度ID
     */
    public static boolean CONCISE_DIMENSION = false;

    /**
     * 自杀或毒杀
     */
    public static boolean CONCISE_FEED = false;

    /**
     * 传送到指定坐标
     */
    public static boolean CONCISE_TP_COORDINATE = true;

    /**
     * 传送到指定结构
     */
    public static boolean CONCISE_TP_STRUCTURE = true;

    /**
     * 请求传送至玩家
     */
    public static boolean CONCISE_TP_ASK = true;

    /**
     * 接受请求传送至玩家
     */
    public static boolean CONCISE_TP_ASK_YES = true;

    /**
     * 拒绝请求传送至玩家
     */
    public static boolean CONCISE_TP_ASK_NO = false;

    /**
     * 请求将玩家传送至当前位置
     */
    public static boolean CONCISE_TP_HERE = true;

    /**
     * 接受请求将玩家传送至当前位置
     */
    public static boolean CONCISE_TP_HERE_YES = true;

    /**
     * 拒绝请求将玩家传送至当前位置
     */
    public static boolean CONCISE_TP_HERE_NO = false;

    /**
     * 随机传送
     */
    public static boolean CONCISE_TP_RANDOM = false;

    /**
     * 传送到玩家重生点
     */
    public static boolean CONCISE_TP_SPAWN = true;

    /**
     * 传送到世界重生点
     */
    public static boolean CONCISE_TP_WORLD_SPAWN = false;

    /**
     * 传送到顶部
     */
    public static boolean CONCISE_TP_TOP = false;

    /**
     * 传送到底部
     */
    public static boolean CONCISE_TP_BOTTOM = false;

    /**
     * 传送到上方
     */
    public static boolean CONCISE_TP_UP = false;

    /**
     * 传送到下方
     */
    public static boolean CONCISE_TP_DOWN = false;

    /**
     * 传送至视线尽头
     */
    public static boolean CONCISE_TP_VIEW = false;

    /**
     * 传送到家
     */
    public static boolean CONCISE_TP_HOME = true;

    /**
     * 设置家
     */
    public static boolean CONCISE_SET_HOME = false;

    /**
     * 删除家
     */
    public static boolean CONCISE_DEL_HOME = false;

    /**
     * 传送到驿站
     */
    public static boolean CONCISE_TP_STAGE = true;

    /**
     * 设置驿站
     */
    public static boolean CONCISE_SET_STAGE = false;

    /**
     * 删除驿站
     */
    public static boolean CONCISE_DEL_STAGE = false;

    /**
     * 传送到上次传送点
     */
    public static boolean CONCISE_TP_BACK = true;

    // endregion 简化指令

    // region 传送代价

    private final static String CATEGORY_COST = "cost";

    /**
     * 代价类型
     */
    public static ECostType COST_TP_COORDINATE_TYPE = ECostType.EXP_POINT;
    /**
     * 代价数量
     */
    public static int COST_TP_COORDINATE_NUM = 1;
    /**
     * 代价配置
     */
    public static String COST_TP_COORDINATE_CONF = "";
    /**
     * 代价倍率(以距离为基准)
     */
    public static float COST_TP_COORDINATE_RATE = 0.001f;

    public static ECostType COST_TP_STRUCTURE_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_STRUCTURE_NUM = 1;
    public static String COST_TP_STRUCTURE_CONF = "";
    public static float COST_TP_STRUCTURE_RATE = 0.001f;

    public static ECostType COST_TP_ASK_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_ASK_NUM = 1;
    public static String COST_TP_ASK_CONF = "";
    public static float COST_TP_ASK_RATE = 0.001f;

    public static ECostType COST_TP_HERE_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_HERE_NUM = 1;
    public static String COST_TP_HERE_CONF = "";
    public static float COST_TP_HERE_RATE = 0.001f;

    public static ECostType COST_TP_RANDOM_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_RANDOM_NUM;
    public static String COST_TP_RANDOM_CONF = "";
    public static float COST_TP_RANDOM_RATE = 0.001f;

    public static ECostType COST_TP_SPAWN_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_SPAWN_NUM = 1;
    public static String COST_TP_SPAWN_CONF = "";
    public static float COST_TP_SPAWN_RATE = 0.001f;

    public static ECostType COST_TP_WORLD_SPAWN_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_WORLD_SPAWN_NUM = 1;
    public static String COST_TP_WORLD_SPAWN_CONF = "";
    public static float COST_TP_WORLD_SPAWN_RATE = 0.001f;

    public static ECostType COST_TP_TOP_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_TOP_NUM = 1;
    public static String COST_TP_TOP_CONF = "";
    public static float COST_TP_TOP_RATE = 0.001f;

    public static ECostType COST_TP_BOTTOM_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_BOTTOM_NUM = 1;
    public static String COST_TP_BOTTOM_CONF = "";
    public static float COST_TP_BOTTOM_RATE = 0.001f;

    public static ECostType COST_TP_UP_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_UP_NUM = 1;
    public static String COST_TP_UP_CONF = "";
    public static float COST_TP_UP_RATE = 0.001f;

    public static ECostType COST_TP_DOWN_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_DOWN_NUM = 1;
    public static String COST_TP_DOWN_CONF = "";
    public static float COST_TP_DOWN_RATE = 0.001f;

    public static ECostType COST_TP_VIEW_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_VIEW_NUM = 1;
    public static String COST_TP_VIEW_CONF = "";
    public static float COST_TP_VIEW_RATE = 0.001f;

    public static ECostType COST_TP_HOME_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_HOME_NUM = 1;
    public static String COST_TP_HOME_CONF = "";
    public static float COST_TP_HOME_RATE = 0.001f;

    public static ECostType COST_TP_STAGE_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_STAGE_NUM = 1;
    public static String COST_TP_STAGE_CONF = "";
    public static float COST_TP_STAGE_RATE = 0.001f;

    public static ECostType COST_TP_BACK_TYPE = ECostType.HUNGER;
    public static int COST_TP_BACK_NUM = 1;
    public static String COST_TP_BACK_CONF = "";
    public static float COST_TP_BACK_RATE = 0.001f;

    // endregion 传送代价

    /**
     * 初始化配置
     */
    public static void init(FMLPreInitializationEvent event) {
        config = new Configuration(new File(event.getModConfigurationDirectory(), String.format("%s-server.cfg", BuildConfig.MODID)));
        loadConfig();
    }

    private static void loadConfig() {
        try {
            setCategoryComment();
            init();
        } catch (Exception e) {
            System.out.println("Error loading config file!");
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    private static void init() {
        config.load();

        // 定义服务器基础设置
        {
            // 传送卡
            TELEPORT_CARD = config.getBoolean("teleportCard", CATEGORY_BASE + ".TpCard", TELEPORT_CARD, "Enable or disable the option to 'Teleport Card'.\n是否启用传送卡。");

            // 每日传送卡数量
            TELEPORT_CARD_DAILY = config.getInt("teleportCardDaily", CATEGORY_BASE + ".TpCard", TELEPORT_CARD_DAILY, 0, 9999, "The number of Teleport Card that can be obtained daily.\n每日可获得的传送卡数量。");

            // 传送卡应用方式
            TELEPORT_CARD_TYPE = ECardType.valueOf(config.getString("teleportCardType", CATEGORY_BASE + ".TpCard", TELEPORT_CARD_TYPE.name(), "The application method of the Teleport Card:\n" +
                    "NONE: Teleportation consumes a Teleport Card and requires an additional cost. If the player has insufficient cards, teleportation is not allowed.\n" +
                    "LIKE_COST: Teleportation consumes the same number of Teleport Cards as the cost and additionally charges the cost. If there are insufficient Teleport Cards, teleportation cannot proceed.\n" +
                    "REFUND_COST: Teleportation consumes a Teleport Card to offset the cost at a 1:1 ratio. If the player has insufficient cards, the corresponding cost will be charged.\n" +
                    "REFUND_ALL_COST: Teleportation consumes a Teleport Card to completely offset all costs. If the player has insufficient cards, the corresponding cost will be charged.\n" +
                    "REFUND_COOLDOWN: Teleportation consumes a Teleport Card to offset the cooldown time, but the cost must still be paid. If the player has insufficient cards, the corresponding cost will be charged.\n" +
                    "REFUND_COST_AND_COOLDOWN: Teleportation consumes a Teleport Card to offset the cost at a 1:1 ratio and offset the cooldown time. If the player has insufficient cards, the corresponding cost will be charged.\n" +
                    "REFUND_ALL_COST_AND_COOLDOWN: Teleportation consumes a Teleport Card to completely offset all costs and cooldown time. If the player has insufficient cards, the corresponding cost will be charged.\n" +
                    "If both Teleport Cards and the cost are insufficient, teleportation will not proceed. \n" +
                    "If you want the Teleport Card to offset the cost but prohibit teleportation when cards are insufficient, set the cost to zero in the configuration.\n" +
                    "传送卡的使用方式：\n" +
                    "NONE: 传送会消耗一个传送卡，并额外收取代价。若传送卡不足，则无法传送。\n" +
                    "LIKE_COST: 传送会消耗与代价数量一致的传送卡，并额外收取代价。若传送卡不足，则无法传送。\n" +
                    "REFUND_COST: 传送会消耗传送卡并按一比一比例抵消代价。若传送卡不足，则收取对应代价。\n" +
                    "REFUND_ALL_COST: 传送会消耗传送卡并完全抵消所有代价。若传送卡不足，则收取对应代价。\n" +
                    "REFUND_COOLDOWN: 传送会消耗传送卡并抵消冷却时间，但仍需支付代价。若传送卡不足，则收取对应代价。\n" +
                    "REFUND_COST_AND_COOLDOWN: 传送会消耗传送卡，按一比一比例抵消代价并抵消冷却时间。若传送卡不足，则收取对应代价。\n" +
                    "REFUND_ALL_COST_AND_COOLDOWN: 传送会消耗传送卡，完全抵消所有代价和冷却时间。若传送卡不足，则收取对应代价。\n" +
                    "若传送卡与代价都不足，则无法传送。若希望传送卡能够抵消代价但在不足时禁止传送，请在配置中将代价设置为零。", Arrays.stream(ECardType.values()).map(ECardType::name).toArray(String[]::new)));

            // 历史传送记录数量限制
            TELEPORT_RECORD_LIMIT = config.getInt("teleportRecordLimit", CATEGORY_BASE, TELEPORT_RECORD_LIMIT, 0, 9999, "The limit of teleport records, 0 means no limit.\n传送记录数量限制，数量为0表示不限制。");

            // 跨维度传送
            TELEPORT_ACROSS_DIMENSION = config.getBoolean("teleportAcrossDimension", CATEGORY_BASE, TELEPORT_ACROSS_DIMENSION, "Is the teleport across dimensions enabled?\n是否启用跨维度传送。");

            // 传送代价中传送距离计算限制
            TELEPORT_COST_DISTANCE_LIMIT = config.getInt("teleportDistanceLimit", CATEGORY_BASE, TELEPORT_COST_DISTANCE_LIMIT, 0, Integer.MAX_VALUE, "The distance calculation limit for teleport cost, 0 means no limit.\n" +
                    "(This config item is not the limit of teleport distance, but the limit of the distance multiplier used when calculating teleport cost.)\n" +
                    "传送代价中传送距离计算限制，值为0表示不限制。(此配置项并非限制传送距离，而是限制计算传送代价时使用的距离乘数。)");

            // 跨维度传送时传送代价中传送距离取值
            TELEPORT_COST_DISTANCE_ACROSS_DIMENSION = config.getInt("teleportDistanceAcrossDimension", CATEGORY_BASE, TELEPORT_COST_DISTANCE_ACROSS_DIMENSION, 0, Integer.MAX_VALUE, "The distance value for teleport cost when teleport across dimensions, 0 means no limit.\n跨维度传送时传送代价中传送距离取值，值为0表示不限制。");

            // 传送至视线尽头时最远传送距离限制
            TELEPORT_VIEW_DISTANCE_LIMIT = config.getInt("teleportViewDistanceLimit", CATEGORY_BASE, TELEPORT_VIEW_DISTANCE_LIMIT, 0, Integer.MAX_VALUE, "The distance limit for teleporting to the view, 0 means no limit.\n传送至视线尽头时最远传送距离限制，值为0表示不限制。");

            // 传送请求过期时间
            TELEPORT_REQUEST_EXPIRE_TIME = config.getInt("teleportRequestExpireTime", CATEGORY_BASE, TELEPORT_REQUEST_EXPIRE_TIME, 0, 60 * 60, "The expire time for teleport request, in seconds.\n传送请求过期时间，单位为秒。");

            // 传送请求冷却时间计算方式
            TELEPORT_REQUEST_COOLDOWN_TYPE = ECoolDownType.valueOf(config.getString("teleportRequestCooldownType", CATEGORY_BASE, TELEPORT_REQUEST_COOLDOWN_TYPE.name(), "The method used to calculate the cooldown time for teleport requests.\n" +
                    "COMMON: All commands share the same global cooldown defined by 'teleportRequestCooldown'.\n" +
                    "INDIVIDUAL: Each command has a separate cooldown managed by the command itself.\n" +
                    "MIXED: Combines both methods, using both the global cooldown and individual cooldowns.\n" +
                    "传送请求冷却时间的计算方式：\n" +
                    "COMMON：所有传送共用全局冷却时间，由'teleportRequestCooldown'配置定义。\n" +
                    "INDIVIDUAL：每个指令有单独的冷却时间，由指令自身管理。\n" +
                    "MIXED：结合两种方式，同时使用全局冷却时间和单独冷却时间。", Arrays.stream(ECoolDownType.values()).map(ECoolDownType::name).toArray(String[]::new)));

            // 传送请求冷却时间
            TELEPORT_REQUEST_COOLDOWN = config.getInt("teleportRequestCooldown", CATEGORY_BASE, TELEPORT_REQUEST_COOLDOWN, 0, 60 * 60 * 24, "The global cooldown time for teleport requests, measured in seconds.\n" +
                    "This value applies to all commands when the cooldown type is COMMON or MIXED.\n" +
                    "传送请求的全局冷却时间，单位为秒。\n" +
                    "当冷却时间计算方式为COMMON或MIXED时，此值对所有指令生效。");

            // 随机传送距离限制
            TELEPORT_RANDOM_DISTANCE_LIMIT = config.getInt("teleportRandomDistanceLimit", CATEGORY_BASE, TELEPORT_RANDOM_DISTANCE_LIMIT, 0, Integer.MAX_VALUE, "The maximum distance limit for random teleportation or teleportation to a specified structure.\n随机传送与传送至指定结构的最大距离限制。");

            // 玩家可设置的家的数量
            TELEPORT_HOME_LIMIT = config.getInt("teleportHomeLimit", CATEGORY_BASE, TELEPORT_HOME_LIMIT, 1, 9999, "The maximum number of homes that can be set by the player.\n玩家可设置的家的数量。");

            // 命令前缀
            COMMAND_PREFIX = config.getString("commandPrefix", CATEGORY_BASE, COMMAND_PREFIX, "The prefix of the command, please only use English characters and underscores, otherwise it may cause problems.\n指令前缀，请仅使用英文字母及下划线，否则可能会出现问题。");

            // 不安全的方块
            UNSAFE_BLOCKS = config.getStringList("unsafeBlocks", CATEGORY_BASE + ".Safe", UNSAFE_BLOCKS, "The list of unsafe blocks, players will not be teleported to these blocks.\n不安全的方块列表，玩家不会传送到这些方块上。");

            // 窒息的方块
            SUFFOCATING_BLOCKS = config.getStringList("suffocatingBlocks", CATEGORY_BASE + ".Safe", SUFFOCATING_BLOCKS, "The list of suffocating blocks, players will not be teleported to these blocks.\n窒息的方块列表，玩家头不会处于这些方块里面。");

            // 安全传送放置方块
            SETBLOCK_WHEN_SAFE_NOT_FOUND = config.getBoolean("setBlockWhenSafeNotFound", CATEGORY_BASE + ".Safe", SETBLOCK_WHEN_SAFE_NOT_FOUND, "When performing a safe teleport, whether to place a block underfoot if a safe coordinate is not found.\n当进行安全传送时，如果未找到安全坐标，是否在脚下放置方块。");

            // 从背包获取安全方块
            GETBLOCK_FROM_INVENTORY = config.getBoolean("getBlockFromInventory", CATEGORY_BASE + ".Safe", GETBLOCK_FROM_INVENTORY, "When performing a safe teleport, whether to only use placeable blocks from the player's inventory if a safe coordinate is not found.\n当进行安全传送时，如果未找到安全坐标，是否仅从背包中获取可放置的方块。");

            // 安全方块类型
            SAFE_BLOCKS = config.getStringList("safeBlocks", CATEGORY_BASE + ".Safe", SAFE_BLOCKS, "When performing a safe teleport, the list of blocks to place if a safe coordinate is not found. If 'getBlockFromInventory' is set to false, the first block in the list will always be used.\n当进行安全传送时，如果未找到安全坐标，放置方块的列表。若'getBlockFromInventory'为false，则始终使用列表中的第一个方块。");
        }

        // 定义功能开关
        {
            SWITCH_FEED = config.getBoolean("switchFeed", CATEGORY_SWITCH, SWITCH_FEED, "Enable or disable the option to 'Suicide or poisoning'.\n是否启用自杀或毒杀。");

            SWITCH_TP_COORDINATE = config.getBoolean("switchTpCoordinate", CATEGORY_SWITCH, SWITCH_TP_COORDINATE, "Enable or disable the option to 'Teleport to the specified coordinates'.\n是否启用传送到指定坐标。");

            SWITCH_TP_STRUCTURE = config.getBoolean("switchTpStructure", CATEGORY_SWITCH, SWITCH_TP_STRUCTURE, "Enable or disable the option to 'Teleport to the specified structure'.\n是否启用传送到指定结构。");

            SWITCH_TP_ASK = config.getBoolean("switchTpAsk", CATEGORY_SWITCH, SWITCH_TP_ASK, "Enable or disable the option to 'Request to teleport oneself to other players'.\n是否启用传送请求。");

            SWITCH_TP_HERE = config.getBoolean("switchTpHere", CATEGORY_SWITCH, SWITCH_TP_HERE, "Enable or disable the option to 'Request the transfer of other players to oneself'.\n是否启用请求将玩家传送至当前位置。");

            SWITCH_TP_RANDOM = config.getBoolean("switchTpRandom", CATEGORY_SWITCH, SWITCH_TP_RANDOM, "Enable or disable the option to 'Teleport to a random location'.\n是否启用随机传送。");

            SWITCH_TP_SPAWN = config.getBoolean("switchTpSpawn", CATEGORY_SWITCH, SWITCH_TP_SPAWN, "Enable or disable the option to 'Teleport to the spawn of the player'.\n是否启用传送到玩家重生点。");

            SWITCH_TP_WORLD_SPAWN = config.getBoolean("switchTpWorldSpawn", CATEGORY_SWITCH, SWITCH_TP_WORLD_SPAWN, "Enable or disable the option to 'Teleport to the spawn of the world'.\n是否启用传送到世界重生点。");

            SWITCH_TP_TOP = config.getBoolean("switchTpTop", CATEGORY_SWITCH, SWITCH_TP_TOP, "Enable or disable the option to 'Teleport to the top of current position'.\n是否启用传送到顶部。");

            SWITCH_TP_BOTTOM = config.getBoolean("switchTpBottom", CATEGORY_SWITCH, SWITCH_TP_BOTTOM, "Enable or disable the option to 'Teleport to the bottom of current position'.\n是否启用传送到底部。");

            SWITCH_TP_UP = config.getBoolean("switchTpUp", CATEGORY_SWITCH, SWITCH_TP_UP, "Enable or disable the option to 'Teleport to the upper of current position'.\n是否启用传送到上方。");

            SWITCH_TP_DOWN = config.getBoolean("switchTpDown", CATEGORY_SWITCH, SWITCH_TP_DOWN, "Enable or disable the option to 'Teleport to the lower of current position'.\n是否启用传送到下方。");

            SWITCH_TP_VIEW = config.getBoolean("switchTpView", CATEGORY_SWITCH, SWITCH_TP_VIEW, "Enable or disable the option to 'Teleport to the end of the line of sight'.\nThis function is independent of the player's render distance setting.\n是否启用传送至视线尽头。\n该功能与玩家设置的视距无关。");

            SWITCH_TP_HOME = config.getBoolean("switchTpHome", CATEGORY_SWITCH, SWITCH_TP_HOME, "Enable or disable the option to 'Teleport to the home'.\n是否启用传送到家。");

            SWITCH_TP_STAGE = config.getBoolean("switchTpStage", CATEGORY_SWITCH, SWITCH_TP_STAGE, "Enable or disable the option to 'Teleport to the stage'.\n是否启用传送到驿站。");

            SWITCH_TP_BACK = config.getBoolean("switchTpBack", CATEGORY_SWITCH, SWITCH_TP_BACK, "Enable or disable the option to 'Teleport to the previous location'.\n是否启用传送到上次传送点。");
        }

        // 定义指令权限
        {

            {
                PERMISSION_FEED_OTHER = config.getInt("permissionFeedOther", CATEGORY_PERMISSION + ".command", PERMISSION_FEED_OTHER, 0, 4, "The permission level required to use the 'Poisoning others' command.\n毒杀指令所需的权限等级。");

                PERMISSION_TP_COORDINATE = config.getInt("permissionTpCoordinate", CATEGORY_PERMISSION + ".command", PERMISSION_TP_COORDINATE, 0, 4, "The permission level required to use the 'Teleport to the specified coordinates' command.\n传送到指定坐标指令所需的权限等级。");

                PERMISSION_TP_STRUCTURE = config.getInt("permissionTpStructure", CATEGORY_PERMISSION + ".command", PERMISSION_TP_STRUCTURE, 0, 4, "The permission level required to use the 'Teleport to the specified structure' command.\n传送到指定结构指令所需的权限等级。");

                PERMISSION_TP_ASK = config.getInt("permissionTpAsk", CATEGORY_PERMISSION + ".command", PERMISSION_TP_ASK, 0, 4, "The permission level required to use the 'Request to teleport oneself to other players' command.\n请求传送至玩家指令所需的权限等级。");

                PERMISSION_TP_HERE = config.getInt("permissionTpHere", CATEGORY_PERMISSION + ".command", PERMISSION_TP_HERE, 0, 4, "The permission level required to use the 'Request the transfer of other players to oneself' command.\n请求将玩家传送至当前位置指令所需的权限等级。");

                PERMISSION_TP_RANDOM = config.getInt("permissionTpRandom", CATEGORY_PERMISSION + ".command", PERMISSION_TP_RANDOM, 0, 4, "The permission level required to use the 'Teleport to a random location' command.\n随机传送指令所需的权限等级。");

                PERMISSION_TP_SPAWN = config.getInt("permissionTpSpawn", CATEGORY_PERMISSION + ".command", PERMISSION_TP_SPAWN, 0, 4, "The permission level required to use the 'Teleport to the spawn of the player' command.\n传送到玩家重生点指令所需的权限等级。");

                PERMISSION_TP_SPAWN_OTHER = config.getInt("permissionTpSpawnOther", CATEGORY_PERMISSION + ".command", PERMISSION_TP_SPAWN_OTHER, 0, 4, "The permission level required to use the 'Teleport to the spawn of the other player' command.\n传送到其他玩家重生点指令所需的权限等级。");

                PERMISSION_TP_WORLD_SPAWN = config.getInt("permissionTpWorldSpawn", CATEGORY_PERMISSION + ".command", PERMISSION_TP_WORLD_SPAWN, 0, 4, "The permission level required to use the 'Teleport to the spawn of the world' command.\n传送到世界重生点指令所需的权限等级。");

                PERMISSION_TP_TOP = config.getInt("permissionTpTop", CATEGORY_PERMISSION + ".command", PERMISSION_TP_TOP, 0, 4, "The permission level required to use the 'Teleport to the top of current position' command.\n传送到顶部指令所需的权限等级。");

                PERMISSION_TP_BOTTOM = config.getInt("permissionTpBottom", CATEGORY_PERMISSION + ".command", PERMISSION_TP_BOTTOM, 0, 4, "The permission level required to use the 'Teleport to the bottom of current position' command.\n传送到底部指令所需的权限等级。");

                PERMISSION_TP_UP = config.getInt("permissionTpUp", CATEGORY_PERMISSION + ".command", PERMISSION_TP_UP, 0, 4, "The permission level required to use the 'Teleport to the upper of current position' command.\n传送到上方指令所需的权限等级。");

                PERMISSION_TP_DOWN = config.getInt("permissionTpDown", CATEGORY_PERMISSION + ".command", PERMISSION_TP_DOWN, 0, 4, "The permission level required to use the 'Teleport to the lower of current position' command.\n传送到下方指令所需的权限等级。");

                PERMISSION_TP_VIEW = config.getInt("permissionTpView", CATEGORY_PERMISSION + ".command", PERMISSION_TP_VIEW, 0, 4, "The permission level required to use the 'Teleport to the end of the line of sight' command.\nThis function is independent of the player's render distance setting.\n传送至视线尽头指令所需的权限等级。\n该功能与玩家设置的视距无关。");

                PERMISSION_TP_HOME = config.getInt("permissionTpHome", CATEGORY_PERMISSION + ".command", PERMISSION_TP_HOME, 0, 4, "The permission level required to use the 'Teleport to the home' command.\n传送到家指令所需的权限等级。");

                PERMISSION_TP_STAGE = config.getInt("permissionTpStage", CATEGORY_PERMISSION + ".command", PERMISSION_TP_STAGE, 0, 4, "The permission level required to use the 'Teleport to the stage' command.\n传送到驿站指令所需的权限等级。");

                PERMISSION_SET_STAGE = config.getInt("permissionSetStage", CATEGORY_PERMISSION + ".command", PERMISSION_SET_STAGE, 0, 4, "The permission level required to use the 'Set the stage' command.\n设置驿站指令所需的权限等级。");

                PERMISSION_DEL_STAGE = config.getInt("permissionDelStage", CATEGORY_PERMISSION + ".command", PERMISSION_DEL_STAGE, 0, 4, "The permission level required to use the 'Delete the stage' command.\n删除驿站指令所需的权限等级。");

                PERMISSION_TP_BACK = config.getInt("permissionTpBack", CATEGORY_PERMISSION + ".command", PERMISSION_TP_BACK, 0, 4, "The permission level required to use the 'Teleport to the previous location' command.\n传送到上次传送点指令所需的权限等级。");
            }

            {
                PERMISSION_TP_COORDINATE_ACROSS_DIMENSION = config.getInt("permissionTpCoordinateAcrossDimension", CATEGORY_PERMISSION + ".across", PERMISSION_TP_COORDINATE_ACROSS_DIMENSION, -1, 4, "The permission level required to use the 'Teleport to the specified coordinates' command across dimensions, -1 means disabled.\n跨维度传送到指定坐标指令所需的权限等级，若为-1则禁用跨维度传送。");

                PERMISSION_TP_STRUCTURE_ACROSS_DIMENSION = config.getInt("permissionTpStructureAcrossDimension", CATEGORY_PERMISSION + ".across", PERMISSION_TP_STRUCTURE_ACROSS_DIMENSION, -1, 4, "The permission level required to use the 'Teleport to the specified structure' command across dimensions, -1 means disabled.\n跨维度传送到指定结构指令所需的权限等级，若为-1则禁用跨维度传送。");

                PERMISSION_TP_ASK_ACROSS_DIMENSION = config.getInt("permissionTpAskAcrossDimension", CATEGORY_PERMISSION + ".across", PERMISSION_TP_ASK_ACROSS_DIMENSION, -1, 4, "The permission level required to use the 'Request to teleport oneself to other players' command across dimensions, -1 means disabled.\n跨维度请求传送至玩家指令所需的权限等级，若为-1则禁用跨维度传送。");

                PERMISSION_TP_HERE_ACROSS_DIMENSION = config.getInt("permissionTpHereAcrossDimension", CATEGORY_PERMISSION + ".across", PERMISSION_TP_HERE_ACROSS_DIMENSION, -1, 4, "The permission level required to use the 'Teleport to the current position' command across dimensions, -1 means disabled.\n跨维度传送到当前位置指令所需的权限等级，若为-1则禁用跨维度传送。");

                PERMISSION_TP_RANDOM_ACROSS_DIMENSION = config.getInt("permissionTpRandomAcrossDimension", CATEGORY_PERMISSION + ".across", PERMISSION_TP_RANDOM_ACROSS_DIMENSION, -1, 4, "The permission level required to use the 'Teleport to the random position' command across dimensions, -1 means disabled.\n跨维度传送到随机位置指令所需的权限等级，若为-1则禁用跨维度传送。");

                PERMISSION_TP_SPAWN_ACROSS_DIMENSION = config.getInt("permissionTpSpawnAcrossDimension", CATEGORY_PERMISSION + ".across", PERMISSION_TP_SPAWN_ACROSS_DIMENSION, -1, 4, "The permission level required to use the 'Teleport to the spawn of the current dimension' command across dimensions, -1 means disabled.\n跨维度传送到当前维度的出生点指令所需的权限等级，若为-1则禁用跨维度传送。");

                PERMISSION_TP_WORLD_SPAWN_ACROSS_DIMENSION = config.getInt("permissionTpWorldSpawnAcrossDimension", CATEGORY_PERMISSION + ".across", PERMISSION_TP_WORLD_SPAWN_ACROSS_DIMENSION, -1, 4, "The permission level required to use the 'Teleport to the world spawn of the current dimension' command across dimensions, -1 means disabled.\n跨维度传送到当前维度的世界出生点指令所需的权限等级，若为-1则禁用跨维度传送。");

                PERMISSION_TP_HOME_ACROSS_DIMENSION = config.getInt("permissionTpHomeAcrossDimension", CATEGORY_PERMISSION + ".across", PERMISSION_TP_HOME_ACROSS_DIMENSION, -1, 4, "The permission level required to use the 'Teleport to the home' command across dimensions, -1 means disabled.\n跨维度传送到家指令所需的权限等级，若为-1则禁用跨维度传送。");

                PERMISSION_TP_STAGE_ACROSS_DIMENSION = config.getInt("permissionTpStageAcrossDimension", CATEGORY_PERMISSION + ".across", PERMISSION_TP_STAGE_ACROSS_DIMENSION, -1, 4, "The permission level required to use the 'Teleport to the stage' command across dimensions, -1 means disabled.\n跨维度传送到驿站指令所需的权限等级，若为-1则禁用跨维度传送。");

                PERMISSION_TP_BACK_ACROSS_DIMENSION = config.getInt("permissionTpBackAcrossDimension", CATEGORY_PERMISSION + ".across", PERMISSION_TP_BACK_ACROSS_DIMENSION, -1, 4, "The permission level required to use the 'Teleport to the previous location' command across dimensions, -1 means disabled.\n跨维度传送到上次传送点指令所需的权限等级，若为-1则禁用跨维度传送。");
            }

        }

        // 定义冷却时间
        {
            COOLDOWN_TP_COORDINATE = config.getInt("cooldownTpCoordinate", CATEGORY_COOLDOWN, COOLDOWN_TP_COORDINATE, 0, 60 * 60 * 24, "The cooldown time for 'Teleport to the specified coordinates', in seconds.\n传送到指定坐标的冷却时间，单位为秒。");

            COOLDOWN_TP_STRUCTURE = config.getInt("cooldownTpStructure", CATEGORY_COOLDOWN, COOLDOWN_TP_STRUCTURE, 0, 60 * 60 * 24, "The cooldown time for 'Teleport to the specified structure', in seconds.\n传送到指定结构的冷却时间，单位为秒。");

            COOLDOWN_TP_ASK = config.getInt("cooldownTpAsk", CATEGORY_COOLDOWN, COOLDOWN_TP_ASK, 0, 60 * 60 * 24, "The cooldown time for 'Request to teleport oneself to other players', in seconds.\n请求传送至玩家的冷却时间，单位为秒。");

            COOLDOWN_TP_HERE = config.getInt("cooldownTpHere", CATEGORY_COOLDOWN, COOLDOWN_TP_HERE, 0, 60 * 60 * 24, "The cooldown time for 'Request the transfer of other players to oneself', in seconds.\n请求将玩家传送至当前位置的冷却时间，单位为秒。");

            COOLDOWN_TP_RANDOM = config.getInt("cooldownTpRandom", CATEGORY_COOLDOWN, COOLDOWN_TP_RANDOM, 0, 60 * 60 * 24, "The cooldown time for 'Teleport to a random location', in seconds.\n随机传送的冷却时间，单位为秒。");

            COOLDOWN_TP_SPAWN = config.getInt("cooldownTpSpawn", CATEGORY_COOLDOWN, COOLDOWN_TP_SPAWN, 0, 60 * 60 * 24, "The cooldown time for 'Teleport to the spawn of the player', in seconds.\n传送到玩家重生点的冷却时间，单位为秒。");

            COOLDOWN_TP_WORLD_SPAWN = config.getInt("cooldownTpWorldSpawn", CATEGORY_COOLDOWN, COOLDOWN_TP_WORLD_SPAWN, 0, 60 * 60 * 24, "The cooldown time for 'Teleport to the spawn of the world', in seconds.\n传送到世界重生点的冷却时间，单位为秒。");

            COOLDOWN_TP_TOP = config.getInt("cooldownTpTop", CATEGORY_COOLDOWN, COOLDOWN_TP_TOP, 0, 60 * 60 * 24, "The cooldown time for 'Teleport to the top of current position', in seconds.\n传送到顶部的冷却时间，单位为秒。");

            COOLDOWN_TP_BOTTOM = config.getInt("cooldownTpBottom", CATEGORY_COOLDOWN, COOLDOWN_TP_BOTTOM, 0, 60 * 60 * 24, "The cooldown time for 'Teleport to the bottom of current position', in seconds.\n传送到底部的冷却时间，单位为秒。");

            COOLDOWN_TP_UP = config.getInt("cooldownTpUp", CATEGORY_COOLDOWN, COOLDOWN_TP_UP, 0, 60 * 60 * 24, "The cooldown time for 'Teleport to the upper of current position', in seconds.\n传送到上方的冷却时间，单位为秒。");

            COOLDOWN_TP_DOWN = config.getInt("cooldownTpDown", CATEGORY_COOLDOWN, COOLDOWN_TP_DOWN, 0, 60 * 60 * 24, "The cooldown time for 'Teleport to the lower of current position', in seconds.\n传送到下方的冷却时间，单位为秒。");

            COOLDOWN_TP_VIEW = config.getInt("cooldownTpView", CATEGORY_COOLDOWN, COOLDOWN_TP_VIEW, 0, 60 * 60 * 24, "The cooldown time for 'Teleport to the end of the line of sight', in seconds.\nThis function is independent of the player's render distance setting.\n传送至视线尽头的冷却时间，单位为秒。\n该功能与玩家设置的视距无关。");

            COOLDOWN_TP_HOME = config.getInt("cooldownTpHome", CATEGORY_COOLDOWN, COOLDOWN_TP_HOME, 0, 60 * 60 * 24, "The cooldown time for 'Teleport to the home', in seconds.\n传送到家的冷却时间，单位为秒。");

            COOLDOWN_TP_STAGE = config.getInt("cooldownTpStage", CATEGORY_COOLDOWN, COOLDOWN_TP_STAGE, 0, 60 * 60 * 24, "The cooldown time for 'Teleport to the stage', in seconds.\n传送到驿站的冷却时间，单位为秒。");

            COOLDOWN_TP_BACK = config.getInt("cooldownTpBack", CATEGORY_COOLDOWN, COOLDOWN_TP_BACK, 0, 60 * 60 * 24, "The cooldown time for 'Teleport to the previous location', in seconds.\n传送到上次传送点的冷却时间，单位为秒。");
        }

        // 定义自定义指令配置
        {
            // 自杀或毒杀
            COMMAND_FEED = config.getString("commandFeed", CATEGORY_COMMAND, COMMAND_FEED, "This command is used to suicide or poisoning, narcissus are poisonous and should not be eaten.\n自杀或毒杀的指令，水仙是有毒的可不能食用哦。");

            // 获取当前世界的维度ID
            COMMAND_DIMENSION = config.getString("commandDimension", CATEGORY_COMMAND, COMMAND_DIMENSION, "This command is used to get the dimension ID of the current world.\n获取当前世界的维度ID的指令。");

            // 传送到指定坐标
            COMMAND_TP_COORDINATE = config.getString("commandTpCoordinate", CATEGORY_COMMAND, COMMAND_TP_COORDINATE, "This command is used to teleport to the specified coordinates.\n传送到指定坐标的指令。");

            // 传送到指定结构
            COMMAND_TP_STRUCTURE = config.getString("commandTpStructure", CATEGORY_COMMAND, COMMAND_TP_STRUCTURE, "This command is used to teleport to the specified structure.\n传送到指定结构的指令。");

            // 请求传送至玩家指令
            COMMAND_TP_ASK = config.getString("commandTpAsk", CATEGORY_COMMAND + ".TpAsk", COMMAND_TP_ASK, "This command is used to request to teleport oneself to other players.\n请求传送至玩家的指令。");

            COMMAND_TP_ASK_YES = config.getString("commandTpAskYes", CATEGORY_COMMAND + ".TpAsk", COMMAND_TP_ASK_YES, "This command is used to accept teleportation of other players to oneself.\nI can't translate it clearly either, as long as you understand the meaning. >_<\n接受请求传送至玩家的指令。\n我也翻译不清楚了，你懂意思就行。>_<");

            COMMAND_TP_ASK_NO = config.getString("commandTpAskNo", CATEGORY_COMMAND + ".TpAsk", COMMAND_TP_ASK_NO, "This command is used to refuse teleportation of other players to oneself.\nI can't translate it clearly either, as long as you understand the meaning. >_<\n拒绝请求传送至玩家的指令。\n我也翻译不清楚了，你懂意思就行。>_<");

            // 请求将玩家传送至当前位置
            COMMAND_TP_HERE = config.getString("commandTpHere", CATEGORY_COMMAND + ".TpHere", COMMAND_TP_HERE, "This command is used to request the transfer of other players to oneself.\n请求将玩家传送至当前位置的指令。");

            COMMAND_TP_HERE_YES = config.getString("commandTpHereYes", CATEGORY_COMMAND + ".TpHere", COMMAND_TP_HERE_YES, "This command is used to accept teleportation to other players.\nI can't translate it clearly either, as long as you understand the meaning. >_<\n接受请求将玩家传送至当前位置的指令。\n我也翻译不清楚了，你懂意思就行。>_<");

            COMMAND_TP_HERE_NO = config.getString("commandTpHereNo", CATEGORY_COMMAND + ".TpHere", COMMAND_TP_HERE_NO, "This command is used to refuse teleportation to other players.\nI can't translate it clearly either, as long as you understand the meaning. >_<\n拒绝请求将玩家传送至当前位置的指令。\n我也翻译不清楚了，你懂意思就行。>_<");

            // 随机传送
            COMMAND_TP_RANDOM = config.getString("commandTpRandom", CATEGORY_COMMAND, COMMAND_TP_RANDOM, "The command to teleport to a random location.\n随机传送的指令。");

            // 传送到玩家重生点
            COMMAND_TP_SPAWN = config.getString("commandTpSpawn", CATEGORY_COMMAND, COMMAND_TP_SPAWN, "The command to teleport to the spawn of the player.\n传送到玩家重生点的指令。");

            // 传送到世界重生点
            COMMAND_TP_WORLD_SPAWN = config.getString("commandTpWorldSpawn", CATEGORY_COMMAND, COMMAND_TP_WORLD_SPAWN, "The command to teleport to the spawn of the world.\n传送到世界重生点的指令。");

            // 传送到顶部
            COMMAND_TP_TOP = config.getString("commandTpTop", CATEGORY_COMMAND, COMMAND_TP_TOP, "The command to teleport to the top of current position.\n传送到顶部的指令。");

            // 传送到底部
            COMMAND_TP_BOTTOM = config.getString("commandTpBottom", CATEGORY_COMMAND, COMMAND_TP_BOTTOM, "The command to teleport to the bottom of current position.\n传送到底部的指令。");

            // 传送到上方
            COMMAND_TP_UP = config.getString("commandTpUp", CATEGORY_COMMAND, COMMAND_TP_UP, "The command to teleport to the upper of current position.\n传送到上方的指令。");

            // 传送到下方
            COMMAND_TP_DOWN = config.getString("commandTpDown", CATEGORY_COMMAND, COMMAND_TP_DOWN, "The command to teleport to the lower of current position.\n传送到下方的指令。");

            // 传送至视线尽头
            COMMAND_TP_VIEW = config.getString("commandTpView", CATEGORY_COMMAND, COMMAND_TP_VIEW, "The command to teleport to the end of the line of sight.\nThis function is independent of the player's render distance setting.\n传送至视线尽头的指令。\n该功能与玩家设置的视距无关。");

            // 传送到家
            COMMAND_TP_HOME = config.getString("commandTpHome", CATEGORY_COMMAND + ".TpHome", COMMAND_TP_HOME, "The command to teleport to the home.\n传送到家的指令。");

            // 设置家
            COMMAND_SET_HOME = config.getString("commandTpHomeSet", CATEGORY_COMMAND + ".TpHome", COMMAND_SET_HOME, "The command to set the home.\n设置家的指令。");

            // 删除家
            COMMAND_DEL_HOME = config.getString("commandTpHomeDel", CATEGORY_COMMAND + ".TpHome", COMMAND_DEL_HOME, "The command to delete the home.\n删除家的指令。");

            // 传送到驿站
            COMMAND_TP_STAGE = config.getString("commandTpStage", CATEGORY_COMMAND + ".TpStage", COMMAND_TP_STAGE, "The command to teleport to the stage.\n传送到驿站的指令。");

            // 设置驿站
            COMMAND_SET_STAGE = config.getString("commandTpStageSet", CATEGORY_COMMAND + ".TpStage", COMMAND_SET_STAGE, "The command to set the stage.\n设置驿站的指令。");

            // 删除驿站
            COMMAND_DEL_STAGE = config.getString("commandTpStageDel", CATEGORY_COMMAND + ".TpStage", COMMAND_DEL_STAGE, "The command to delete the stage.\n删除驿站的指令。");

            // 传送到上次传送点
            COMMAND_TP_BACK = config.getString("commandTpBack", CATEGORY_COMMAND, COMMAND_TP_BACK, "The command to teleport to the previous location.\n传送到上次传送点的指令。");
        }

        // 定义简化指令
        {
            CONCISE_DIMENSION = config.getBoolean("conciseDimension", CATEGORY_CONCISE, CONCISE_DIMENSION, "Enable or disable the concise version of the 'Get the dimension ID of the current world' command.\n是否启用无前缀版本的 '获取当前世界的维度ID' 指令。");

            CONCISE_FEED = config.getBoolean("conciseFeed", CATEGORY_CONCISE, CONCISE_FEED, "Enable or disable the concise version of the 'Suicide or poisoning' command.\n是否启用无前缀版本的 '自杀或毒杀' 指令。");

            CONCISE_TP_COORDINATE = config.getBoolean("conciseTpCoordinate", CATEGORY_CONCISE, CONCISE_TP_COORDINATE, "Enable or disable the concise version of the 'Teleport to the specified coordinates' command.\n是否启用无前缀版本的 '传送到指定坐标' 指令。");

            CONCISE_TP_STRUCTURE = config.getBoolean("conciseTpStructure", CATEGORY_CONCISE, CONCISE_TP_STRUCTURE, "Enable or disable the concise version of the 'Teleport to the specified structure' command.\n是否启用无前缀版本的 '传送到指定结构' 指令。");

            CONCISE_TP_ASK = config.getBoolean("conciseTpAsk", CATEGORY_CONCISE + ".TpAsk", CONCISE_TP_ASK, "Enable or disable the concise version of the 'Request to teleport oneself to other players' command.\n是否启用无前缀版本的 '请求传送至玩家' 指令。");

            CONCISE_TP_ASK_YES = config.getBoolean("conciseTpAskYes", CATEGORY_CONCISE + ".TpAsk", CONCISE_TP_ASK_YES, "Enable or disable the concise version of the 'Accept teleportation of other players to oneself' command.\n是否启用无前缀版本的 '接受请求传送至玩家' 指令。");

            CONCISE_TP_ASK_NO = config.getBoolean("conciseTpAskNo", CATEGORY_CONCISE + ".TpAsk", CONCISE_TP_ASK_NO, "Enable or disable the concise version of the 'Refuse teleportation of other players to oneself' command.\n是否启用无前缀版本的 '拒绝请求传送至玩家' 指令。");

            CONCISE_TP_HERE = config.getBoolean("conciseTpHere", CATEGORY_CONCISE + ".TpHere", CONCISE_TP_HERE, "Enable or disable the concise version of the 'Request the transfer of other players to oneself' command.\n是否启用无前缀版本的 '请求将玩家传送至当前位置' 指令。");

            CONCISE_TP_HERE_YES = config.getBoolean("conciseTpHereYes", CATEGORY_CONCISE + ".TpHere", CONCISE_TP_HERE_YES, "Enable or disable the concise version of the 'Accept teleportation to other players' command.\n是否启用无前缀版本的 '接受请求将玩家传送至当前位置' 指令。");

            CONCISE_TP_HERE_NO = config.getBoolean("conciseTpHereNo", CATEGORY_CONCISE + ".TpHere", CONCISE_TP_HERE_NO, "Enable or disable the concise version of the 'Refuse teleportation to other players' command.\n是否启用无前缀版本的 '拒绝请求将玩家传送至当前位置' 指令。");

            CONCISE_TP_RANDOM = config.getBoolean("conciseTpRandom", CATEGORY_CONCISE, CONCISE_TP_RANDOM, "Enable or disable the concise version of the 'Teleport to a random location' command.\n是否启用无前缀版本的 '随机传送' 指令。");

            CONCISE_TP_SPAWN = config.getBoolean("conciseTpSpawn", CATEGORY_CONCISE, CONCISE_TP_SPAWN, "Enable or disable the concise version of the 'Teleport to the spawn of the player' command.\n是否启用无前缀版本的 '传送到玩家重生点' 指令。");

            CONCISE_TP_WORLD_SPAWN = config.getBoolean("conciseTpWorldSpawn", CATEGORY_CONCISE, CONCISE_TP_WORLD_SPAWN, "Enable or disable the concise version of the 'Teleport to the spawn of the world' command.\n是否启用无前缀版本的 '传送到世界重生点' 指令。");

            CONCISE_TP_TOP = config.getBoolean("conciseTpTop", CATEGORY_CONCISE, CONCISE_TP_TOP, "Enable or disable the concise version of the 'Teleport to the top of current position' command.\n是否启用无前缀版本的 '传送到顶部' 指令。");

            CONCISE_TP_BOTTOM = config.getBoolean("conciseTpBottom", CATEGORY_CONCISE, CONCISE_TP_BOTTOM, "Enable or disable the concise version of the 'Teleport to the bottom of current position' command.\n是否启用无前缀版本的 '传送到底部' 指令。");

            CONCISE_TP_UP = config.getBoolean("conciseTpUp", CATEGORY_CONCISE, CONCISE_TP_UP, "Enable or disable the concise version of the 'Teleport to the upper of current position' command.\n是否启用无前缀版本的 '传送到上方' 指令。");

            CONCISE_TP_DOWN = config.getBoolean("conciseTpDown", CATEGORY_CONCISE, CONCISE_TP_DOWN, "Enable or disable the concise version of the 'Teleport to the lower of current position' command.\n是否启用无前缀版本的 '传送到下方' 指令。");

            CONCISE_TP_VIEW = config.getBoolean("conciseTpView", CATEGORY_CONCISE, CONCISE_TP_VIEW, "Enable or disable the concise version of the 'Teleport to the end of the line of sight' command.\nThis function is independent of the player's render distance setting.\n是否启用无前缀版本的 '传送至视线尽头' 指令。\n该功能与玩家设置的视距无关。");

            CONCISE_TP_HOME = config.getBoolean("conciseTpHome", CATEGORY_CONCISE + ".TpHome", CONCISE_TP_HOME, "Enable or disable the concise version of the 'Teleport to the home' command.\n是否启用无前缀版本的 '传送到家' 指令。");

            CONCISE_SET_HOME = config.getBoolean("conciseTpHomeSet", CATEGORY_CONCISE + ".TpHome", CONCISE_SET_HOME, "Enable or disable the concise version of the 'Set the home' command.\n是否启用无前缀版本的 '设置家' 指令。");

            CONCISE_DEL_HOME = config.getBoolean("conciseTpHomeDel", CATEGORY_CONCISE + ".TpHome", CONCISE_DEL_HOME, "Enable or disable the concise version of the 'Delete the home' command.\n是否启用无前缀版本的 '删除家' 指令。");

            CONCISE_TP_STAGE = config.getBoolean("conciseTpStage", CATEGORY_CONCISE + ".TpStage", CONCISE_TP_STAGE, "Enable or disable the concise version of the 'Teleport to the stage' command.\n是否启用无前缀版本的 '传送到驿站' 指令。");

            CONCISE_SET_STAGE = config.getBoolean("conciseTpStageSet", CATEGORY_CONCISE + ".TpStage", CONCISE_SET_STAGE, "Enable or disable the concise version of the 'Set the stage' command.\n是否启用无前缀版本的 '设置驿站' 指令。");

            CONCISE_DEL_STAGE = config.getBoolean("conciseTpStageDel", CATEGORY_CONCISE + ".TpStage", CONCISE_DEL_STAGE, "Enable or disable the concise version of the 'Delete the stage' command.\n是否启用无前缀版本的 '删除驿站' 指令。");

            CONCISE_TP_BACK = config.getBoolean("conciseTpBack", CATEGORY_CONCISE, CONCISE_TP_BACK, "Enable or disable the concise version of the 'Teleport to the previous location' command.\n是否启用无前缀版本的 '传送到上次传送点' 指令。");
        }

        // 定义传送代价
        {
            {
                COST_TP_COORDINATE_TYPE = ECostType.valueOf(config.getString("costTpCoordinateType", CATEGORY_COST + ".TpCoordinate", COST_TP_COORDINATE_TYPE.name(), "The cost type for 'Teleport to the specified coordinates'.\n传送到指定坐标的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_COORDINATE_NUM = config.getInt("costTpCoordinateNum", CATEGORY_COST + ".TpCoordinate", COST_TP_COORDINATE_NUM, 0, 9999, "The number of cost for 'Teleport to the specified coordinates'.\n传送到指定坐标的代价数量。");

                COST_TP_COORDINATE_CONF = config.getString("costTpCoordinateConf", CATEGORY_COST + ".TpCoordinate", COST_TP_COORDINATE_CONF, "The configuration for 'Teleport to the specified coordinates'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n传送到指定坐标的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_COORDINATE_RATE = config.getFloat("costTpCoordinateRate", CATEGORY_COST + ".TpCoordinate", COST_TP_COORDINATE_RATE, 0, 9999, "The cost rate for 'Teleport to the specified coordinates', the cost will be multiplied by the distance between the two coordinates.\n传送到指定坐标的代价倍率，代价会乘以传送前后坐标之间的距离。");
            }

            {
                COST_TP_STRUCTURE_TYPE = ECostType.valueOf(config.getString("costTpStructureType", CATEGORY_COST + ".TpStructure", COST_TP_STRUCTURE_TYPE.name(), "The cost type for 'Teleport to the specified structure'.\n传送到指定结构的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_STRUCTURE_NUM = config.getInt("costTpStructureNum", CATEGORY_COST + ".TpStructure", COST_TP_STRUCTURE_NUM, 0, 9999, "The number of cost for 'Teleport to the specified structure'.\n传送到指定结构的代价数量。");

                COST_TP_STRUCTURE_CONF = config.getString("costTpStructureConf", CATEGORY_COST + ".TpStructure", COST_TP_STRUCTURE_CONF, "The configuration for 'Teleport to the specified structure'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n传送到指定结构的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_STRUCTURE_RATE = config.getFloat("costTpStructureRate", CATEGORY_COST + ".TpStructure", COST_TP_STRUCTURE_RATE, 0, 9999, "The cost rate for 'Teleport to the specified structure', the cost will be multiplied by the distance between the two players.\n传送到指定结构的代价倍率，代价会乘以两个玩家之间的距离。");
            }

            {
                COST_TP_ASK_TYPE = ECostType.valueOf(config.getString("costTpAskType", CATEGORY_COST + ".TpAsk", COST_TP_ASK_TYPE.name(), "The cost type for 'Request to teleport oneself to other players'.\n请求传送至玩家的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_ASK_NUM = config.getInt("costTpAskNum", CATEGORY_COST + ".TpAsk", COST_TP_ASK_NUM, 0, 9999, "The number of cost for 'Request to teleport oneself to other players'.\n请求传送至玩家的代价数量。");

                COST_TP_ASK_CONF = config.getString("costTpAskConf", CATEGORY_COST + ".TpAsk", COST_TP_ASK_CONF, "The configuration for 'Request to teleport oneself to other players'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n请求传送至玩家的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_ASK_RATE = config.getFloat("costTpAskRate", CATEGORY_COST + ".TpAsk", COST_TP_ASK_RATE, 0, 9999, "The cost rate for 'Request to teleport oneself to other players', the cost will be multiplied by the distance between the two players.\n请求传送至玩家的代价倍率，代价会乘以两个玩家之间的距离。");
            }

            {
                COST_TP_HERE_TYPE = ECostType.valueOf(config.getString("costTpHereType", CATEGORY_COST + ".TpHere", COST_TP_HERE_TYPE.name(), "The cost type for 'Request the transfer of other players to oneself'.\n请求将玩家传送至当前位置的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_HERE_NUM = config.getInt("costTpHereNum", CATEGORY_COST + ".TpHere", COST_TP_HERE_NUM, 0, 9999, "The number of cost for 'Request the transfer of other players to oneself'.\n请求将玩家传送至当前位置的代价数量。");

                COST_TP_HERE_CONF = config.getString("costTpHereConf", CATEGORY_COST + ".TpHere", COST_TP_HERE_CONF, "The configuration for 'Request the transfer of other players to oneself'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n请求将玩家传送至当前位置的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_HERE_RATE = config.getFloat("costTpHereRate", CATEGORY_COST + ".TpHere", COST_TP_HERE_RATE, 0, 9999, "The cost rate for 'Request the transfer of other players to oneself', the cost will be multiplied by the distance between the two coordinates.\n请求将玩家传送至当前位置的代价倍率，代价会乘以传送前后坐标之间的距离。");
            }

            {
                COST_TP_RANDOM_TYPE = ECostType.valueOf(config.getString("costTpRandomType", CATEGORY_COST + ".TpRandom", COST_TP_RANDOM_TYPE.name(), "The cost type for 'Teleport to a random location'.\n随机传送的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_RANDOM_NUM = config.getInt("costTpRandomNum", CATEGORY_COST + ".TpRandom", COST_TP_RANDOM_NUM, 0, 9999, "The number of cost for 'Teleport to a random location'.\n随机传送的代价数量。");

                COST_TP_RANDOM_CONF = config.getString("costTpRandomConf", CATEGORY_COST + ".TpRandom", COST_TP_RANDOM_CONF, "The configuration for 'Teleport to a random location'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n随机传送的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_RANDOM_RATE = config.getFloat("costTpRandomRate", CATEGORY_COST + ".TpRandom", COST_TP_RANDOM_RATE, 0, 9999, "The cost rate for 'Teleport to a random location', the cost will be multiplied by the distance between the two coordinates.\n随机传送的代价倍率，代价会乘以传送前后坐标之间的距离。");
            }

            {
                COST_TP_SPAWN_TYPE = ECostType.valueOf(config.getString("costTpSpawnType", CATEGORY_COST + ".TpSpawn", COST_TP_SPAWN_TYPE.name(), "The cost type for 'Teleport to the spawn of the player'.\n传送到玩家重生点的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_SPAWN_NUM = config.getInt("costTpSpawnNum", CATEGORY_COST + ".TpSpawn", COST_TP_SPAWN_NUM, 0, 9999, "The number of cost for 'Teleport to the spawn of the player'.\n传送到玩家重生点的代价数量。");

                COST_TP_SPAWN_CONF = config.getString("costTpSpawnConf", CATEGORY_COST + ".TpSpawn", COST_TP_SPAWN_CONF, "The configuration for 'Teleport to the spawn of the player'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n传送到玩家重生点的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_SPAWN_RATE = config.getFloat("costTpSpawnRate", CATEGORY_COST + ".TpSpawn", COST_TP_SPAWN_RATE, 0, 9999, "The cost rate for 'Teleport to the spawn of the player', the cost will be multiplied by the distance between the two coordinates.\n传送到玩家重生点的代价倍率，代价会乘以传送前后坐标之间的距离。");
            }

            {
                COST_TP_WORLD_SPAWN_TYPE = ECostType.valueOf(config.getString("costTpWorldSpawnType", CATEGORY_COST + ".TpWorldSpawn", COST_TP_WORLD_SPAWN_TYPE.name(), "The cost type for 'Teleport to the spawn of the world'.\n传送到世界重生点的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_WORLD_SPAWN_NUM = config.getInt("costTpWorldSpawnNum", CATEGORY_COST + ".TpWorldSpawn", COST_TP_WORLD_SPAWN_NUM, 0, 9999, "The number of cost for 'Teleport to the spawn of the world'.\n传送到世界重生点的代价数量。");

                COST_TP_WORLD_SPAWN_CONF = config.getString("costTpWorldSpawnConf", CATEGORY_COST + ".TpWorldSpawn", COST_TP_WORLD_SPAWN_CONF, "The configuration for 'Teleport to the spawn of the world'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n传送到世界重生点的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_WORLD_SPAWN_RATE = config.getFloat("costTpWorldSpawnRate", CATEGORY_COST + ".TpWorldSpawn", COST_TP_WORLD_SPAWN_RATE, 0, 9999, "The cost rate for 'Teleport to the spawn of the world', the cost will be multiplied by the distance between the two coordinates.\n传送到世界重生点的代价倍率，代价会乘以传送前后坐标之间的距离。");
            }

            {
                COST_TP_TOP_TYPE = ECostType.valueOf(config.getString("costTpTopType", CATEGORY_COST + ".TpTop", COST_TP_TOP_TYPE.name(), "The cost type for 'Teleport to the top of current position'.\n传送到顶部的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_TOP_NUM = config.getInt("costTpTopNum", CATEGORY_COST + ".TpTop", COST_TP_TOP_NUM, 0, 9999, "The number of cost for 'Teleport to the top of current position'.\n传送到顶部的代价数量。");

                COST_TP_TOP_CONF = config.getString("costTpTopConf", CATEGORY_COST + ".TpTop", COST_TP_TOP_CONF, "The configuration for 'Teleport to the top of current position'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n传送到顶部的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_TOP_RATE = config.getFloat("costTpTopRate", CATEGORY_COST + ".TpTop", COST_TP_TOP_RATE, 0, 9999, "The cost rate for 'Teleport to the top of current position', the cost will be multiplied by the distance between the two coordinates.\n传送到顶部的代价倍率，代价会乘以传送前后坐标之间的距离。");
            }

            {
                COST_TP_BOTTOM_TYPE = ECostType.valueOf(config.getString("costTpBottomType", CATEGORY_COST + ".TpBottom", COST_TP_BOTTOM_TYPE.name(), "The cost type for 'Teleport to the bottom of current position'.\n传送到底部的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_BOTTOM_NUM = config.getInt("costTpBottomNum", CATEGORY_COST + ".TpBottom", COST_TP_BOTTOM_NUM, 0, 9999, "The number of cost for 'Teleport to the bottom of current position'.\n传送到底部的代价数量。");

                COST_TP_BOTTOM_CONF = config.getString("costTpBottomConf", CATEGORY_COST + ".TpBottom", COST_TP_BOTTOM_CONF, "The configuration for 'Teleport to the bottom of current position'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n传送到底部的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_BOTTOM_RATE = config.getFloat("costTpBottomRate", CATEGORY_COST + ".TpBottom", COST_TP_BOTTOM_RATE, 0, 9999, "The cost rate for 'Teleport to the bottom of current position', the cost will be multiplied by the distance between the two coordinates.\n传送到底部的代价倍率，代价会乘以传送前后坐标之间的距离。");
            }

            {
                COST_TP_UP_TYPE = ECostType.valueOf(config.getString("costTpUpType", CATEGORY_COST + ".TpUp", COST_TP_UP_TYPE.name(), "The cost type for 'Teleport to the upper of current position'.\n传送到上方的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_UP_NUM = config.getInt("costTpUpNum", CATEGORY_COST + ".TpUp", COST_TP_UP_NUM, 0, 9999, "The number of cost for 'Teleport to the upper of current position'.\n传送到上方的代价数量。");

                COST_TP_UP_CONF = config.getString("costTpUpConf", CATEGORY_COST + ".TpUp", COST_TP_UP_CONF, "The configuration for 'Teleport to the upper of current position'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n传送到上方的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_UP_RATE = config.getFloat("costTpUpRate", CATEGORY_COST + ".TpUp", COST_TP_UP_RATE, 0, 9999, "The cost rate for 'Teleport to the upper of current position', the cost will be multiplied by the distance between the two coordinates.\n传送到上方的代价倍率，代价会乘以传送前后坐标之间的距离。");
            }

            {
                COST_TP_DOWN_TYPE = ECostType.valueOf(config.getString("costTpDownType", CATEGORY_COST + ".TpDown", COST_TP_DOWN_TYPE.name(), "The cost type for 'Teleport to the lower of current position'.\n传送到下方的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_DOWN_NUM = config.getInt("costTpDownNum", CATEGORY_COST + ".TpDown", COST_TP_DOWN_NUM, 0, 9999, "The number of cost for 'Teleport to the lower of current position'.\n传送到下方的代价数量。");

                COST_TP_DOWN_CONF = config.getString("costTpDownConf", CATEGORY_COST + ".TpDown", COST_TP_DOWN_CONF, "The configuration for 'Teleport to the lower of current position'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n传送到下方的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_DOWN_RATE = config.getFloat("costTpDownRate", CATEGORY_COST + ".TpDown", COST_TP_DOWN_RATE, 0, 9999, "The cost rate for 'Teleport to the lower of current position', the cost will be multiplied by the distance between the two coordinates.\n传送到下方的代价倍率，代价会乘以传送前后坐标之间的距离。");
            }

            {
                COST_TP_VIEW_TYPE = ECostType.valueOf(config.getString("costTpViewType", CATEGORY_COST + ".TpView", COST_TP_VIEW_TYPE.name(), "The cost type for 'Teleport to the end of the line of sight'.\n传送至视线尽头的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_VIEW_NUM = config.getInt("costTpViewNum", CATEGORY_COST + ".TpView", COST_TP_VIEW_NUM, 0, 9999, "The number of cost for 'Teleport to the end of the line of sight'.\n传送至视线尽头的代价数量。");

                COST_TP_VIEW_CONF = config.getString("costTpViewConf", CATEGORY_COST + ".TpView", COST_TP_VIEW_CONF, "The configuration for 'Teleport to the end of the line of sight'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n传送至视线尽头的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_VIEW_RATE = config.getFloat("costTpViewRate", CATEGORY_COST + ".TpView", COST_TP_VIEW_RATE, 0, 9999, "The cost rate for 'Teleport to the end of the line of sight', the cost will be multiplied by the distance between the two coordinates.\n传送至视线尽头的代价倍率，代价会乘以传送前后坐标之间的距离。");
            }

            {
                COST_TP_HOME_TYPE = ECostType.valueOf(config.getString("costTpHomeType", CATEGORY_COST + ".TpHome", COST_TP_HOME_TYPE.name(), "The cost type for 'Teleport to the home'.\n传送到家的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_HOME_NUM = config.getInt("costTpHomeNum", CATEGORY_COST + ".TpHome", COST_TP_HOME_NUM, 0, 9999, "The number of cost for 'Teleport to the home'.\n传送到家的代价数量。");

                COST_TP_HOME_CONF = config.getString("costTpHomeConf", CATEGORY_COST + ".TpHome", COST_TP_HOME_CONF, "The configuration for 'Teleport to the home'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n传送到家的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_HOME_RATE = config.getFloat("costTpHomeRate", CATEGORY_COST + ".TpHome", COST_TP_HOME_RATE, 0, 9999, "The cost rate for 'Teleport to the home', the cost will be multiplied by the distance between the two coordinates.\n传送到家的代价倍率，代价会乘以传送前后坐标之间的距离。");
            }

            {
                COST_TP_STAGE_TYPE = ECostType.valueOf(config.getString("costTpStageType", CATEGORY_COST + ".TpStage", COST_TP_STAGE_TYPE.name(), "The cost type for 'Teleport to the stage'.\n传送到驿站的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_STAGE_NUM = config.getInt("costTpStageNum", CATEGORY_COST + ".TpStage", COST_TP_STAGE_NUM, 0, 9999, "The number of cost for 'Teleport to the stage'.\n传送到驿站的代价数量。");

                COST_TP_STAGE_CONF = config.getString("costTpStageConf", CATEGORY_COST + ".TpStage", COST_TP_STAGE_CONF, "The configuration for 'Teleport to the stage'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n传送到驿站的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_STAGE_RATE = config.getFloat("costTpStageRate", CATEGORY_COST + ".TpStage", COST_TP_STAGE_RATE, 0, 9999, "The cost rate for 'Teleport to the stage', the cost will be multiplied by the distance between the two coordinates.\n传送到驿站的代价倍率，代价会乘以传送前后坐标之间的距离。");
            }

            {
                COST_TP_BACK_TYPE = ECostType.valueOf(config.getString("costTpBackType", CATEGORY_COST + ".TpBack", COST_TP_BACK_TYPE.name(), "The cost type for 'Teleport to the previous location'.\n传送到上次传送点的代价类型。", Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new)));

                COST_TP_BACK_NUM = config.getInt("costTpBackNum", CATEGORY_COST + ".TpBack", COST_TP_BACK_NUM, 0, 9999, "The number of cost for 'Teleport to the previous location'.\n传送到上次传送点的代价数量。");

                COST_TP_BACK_CONF = config.getString("costTpBackConf", CATEGORY_COST + ".TpBack", COST_TP_BACK_CONF, "The configuration for 'Teleport to the previous location'.\nIf the type is ITEM, the value should be the item ID with optional NBT data.\nIf the type is COMMAND, the value should be a specific command string.\nIn the command, the placeholder '[num]' can be used to represent the cost amount.\n传送到上次传送点的代价配置：\n若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n若类型为 COMMAND，则值为具体的指令字符串。\n在指令中，可使用占位符 '[num]' 来表示代价数量。");

                COST_TP_BACK_RATE = config.getFloat("costTpBackRate", CATEGORY_COST + ".TpBack", COST_TP_BACK_RATE, 0, 9999, "TpBack cost rate, the cost will be multiplied by the distance between the two coordinates.\n传送到上次传送点的代价倍率，代价会乘以传送前后坐标之间的距离。");
            }
        }
    }

    private static void setCategoryComment() {
        config.addCustomCategoryComment(CATEGORY_BASE, "Base Settings\n基础设置");
        config.addCustomCategoryComment(CATEGORY_SWITCH, "Switch\n功能开关");
        config.addCustomCategoryComment(CATEGORY_PERMISSION + ".command", "Permission\n指令权限");
        config.addCustomCategoryComment(CATEGORY_PERMISSION + ".across", "Across dimensions Switch\n跨维度权限");
        config.addCustomCategoryComment(CATEGORY_COOLDOWN, "Cooldown\n冷却时间");
        config.addCustomCategoryComment(CATEGORY_COMMAND, "Custom Command Settings, don't add prefix '/'\n自定义指令，请勿添加前缀'/'");
        config.addCustomCategoryComment(CATEGORY_CONCISE, "Concise Command Settings\n简化指令");
        config.addCustomCategoryComment(CATEGORY_COST, "Teleport Cost\n传送代价");
    }

    /**
     * 更新配置
     */
    public static void reloadConfig() {
        if (config != null) {
            loadConfig();
            config.save();
        }
    }
}
