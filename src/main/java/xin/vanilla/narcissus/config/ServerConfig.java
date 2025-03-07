package xin.vanilla.narcissus.config;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import xin.vanilla.narcissus.BuildConfig;
import xin.vanilla.narcissus.enums.ECardType;
import xin.vanilla.narcissus.enums.ECoolDownType;
import xin.vanilla.narcissus.enums.ECostType;

import java.io.File;
import java.util.Arrays;

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
    public static String[] UNSAFE_BLOCKS = {
            "minecraft:lava",
            "minecraft:fire",
            "minecraft:cactus",
    };

    /**
     * 窒息的方块
     */
    public static String[] SUFFOCATING_BLOCKS = {
            "minecraft:lava",
            "minecraft:water",
    };

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
    public static String[] SAFE_BLOCKS = {
            "minecraft:dirt",
            "minecraft:cobblestone",
    };

    /**
     * 寻找安全坐标的区块范围
     */
    public static int SAFE_CHUNK_RANGE = 1;

    /**
     * 虚拟权限
     */
    public static String OP_LIST = "";

    /**
     * 帮助指令信息头部内容
     */
    public static String HELP_HEADER = "-----==== Narcissus Farewell Help (%d/%d) ====-----";

    /**
     * 传送音效
     */
    public static String TP_SOUND;

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

    public static int PERMISSION_GET_STAGE = 0;

    public static int PERMISSION_TP_BACK = 0;

    public static int PERMISSION_VIRTUAL_OP = 4;

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
     * 获取玩家的UUID
     */
    public static String COMMAND_UUID = "uuid";

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
     * 查询家
     */
    public static String COMMAND_GET_HOME = "gethome";

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
     * 查询驿站
     */
    public static String COMMAND_GET_STAGE = "getstage";

    /**
     * 传送到上次传送点
     */
    public static String COMMAND_TP_BACK = "back";

    /**
     * 设置虚拟权限
     */
    public static String COMMAND_VIRTUAL_OP = "opv";

    // endregion 自定义指令

    // region 简化指令

    private final static String CATEGORY_CONCISE = "concise";

    /**
     * 获取玩家的UUID
     */
    public static boolean CONCISE_UUID = false;

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
     * 查询家
     */
    public static boolean CONCISE_GET_HOME = false;

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
     * 查询驿站
     */
    public static boolean CONCISE_GET_STAGE = false;

    /**
     * 传送到上次传送点
     */
    public static boolean CONCISE_TP_BACK = true;

    /**
     * 设置虚拟权限
     */
    public static boolean CONCISE_VIRTUAL_OP = false;

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
    public static double COST_TP_COORDINATE_RATE = 0.001f;

    public static ECostType COST_TP_STRUCTURE_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_STRUCTURE_NUM = 1;
    public static String COST_TP_STRUCTURE_CONF = "";
    public static double COST_TP_STRUCTURE_RATE = 0.001f;

    public static ECostType COST_TP_ASK_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_ASK_NUM = 1;
    public static String COST_TP_ASK_CONF = "";
    public static double COST_TP_ASK_RATE = 0.001f;

    public static ECostType COST_TP_HERE_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_HERE_NUM = 1;
    public static String COST_TP_HERE_CONF = "";
    public static double COST_TP_HERE_RATE = 0.001f;

    public static ECostType COST_TP_RANDOM_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_RANDOM_NUM = 1;
    public static String COST_TP_RANDOM_CONF = "";
    public static double COST_TP_RANDOM_RATE = 0.001f;

    public static ECostType COST_TP_SPAWN_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_SPAWN_NUM = 1;
    public static String COST_TP_SPAWN_CONF = "";
    public static double COST_TP_SPAWN_RATE = 0.001f;

    public static ECostType COST_TP_WORLD_SPAWN_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_WORLD_SPAWN_NUM = 1;
    public static String COST_TP_WORLD_SPAWN_CONF = "";
    public static double COST_TP_WORLD_SPAWN_RATE = 0.001f;

    public static ECostType COST_TP_TOP_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_TOP_NUM = 1;
    public static String COST_TP_TOP_CONF = "";
    public static double COST_TP_TOP_RATE = 0.001f;

    public static ECostType COST_TP_BOTTOM_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_BOTTOM_NUM = 1;
    public static String COST_TP_BOTTOM_CONF = "";
    public static double COST_TP_BOTTOM_RATE = 0.001f;

    public static ECostType COST_TP_UP_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_UP_NUM = 1;
    public static String COST_TP_UP_CONF = "";
    public static double COST_TP_UP_RATE = 0.001f;

    public static ECostType COST_TP_DOWN_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_DOWN_NUM = 1;
    public static String COST_TP_DOWN_CONF = "";
    public static double COST_TP_DOWN_RATE = 0.001f;

    public static ECostType COST_TP_VIEW_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_VIEW_NUM = 1;
    public static String COST_TP_VIEW_CONF = "";
    public static double COST_TP_VIEW_RATE = 0.001f;

    public static ECostType COST_TP_HOME_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_HOME_NUM = 1;
    public static String COST_TP_HOME_CONF = "";
    public static double COST_TP_HOME_RATE = 0.001f;

    public static ECostType COST_TP_STAGE_TYPE = ECostType.EXP_POINT;
    public static int COST_TP_STAGE_NUM = 1;
    public static String COST_TP_STAGE_CONF = "";
    public static double COST_TP_STAGE_RATE = 0.001f;

    public static ECostType COST_TP_BACK_TYPE = ECostType.HUNGER;
    public static int COST_TP_BACK_NUM = 1;
    public static String COST_TP_BACK_CONF = "";
    public static double COST_TP_BACK_RATE = 0.001f;

    // endregion 传送代价

    /**
     * 初始化配置
     */
    public static void init(FMLPreInitializationEvent event) {
        config = new Configuration(new File(event.getModConfigurationDirectory(), String.format("%s-server.cfg", BuildConfig.MODID)));
        loadConfig();
    }

    /**
     * 保存配置
     */
    public static void saveAll() {
        try {
            init(true);
        } catch (Exception e) {
            System.out.println("Error change config file!");
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    private static void loadConfig() {
        try {
            setCategoryComment();
            init(false);
        } catch (Exception e) {
            System.out.println("Error loading config file!");
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }

    private static void init(boolean write) {
        if (!write)
            config.load();

        // 定义服务器基础设置
        {
            // 传送卡
            Property teleportCard = config.get(CATEGORY_BASE + ".TpCard", "teleportCard", false,
                    "Enable or disable the option to 'Teleport Card'.\n" +
                            "是否启用传送卡。\n"
            );
            if (write) teleportCard.set(TELEPORT_CARD);
            TELEPORT_CARD = teleportCard.getBoolean();

            // 每日传送卡数量
            Property teleportCardDaily = config.get(CATEGORY_BASE + ".TpCard", "teleportCardDaily", 0,
                    "The number of Teleport Card that can be obtained daily.\n" +
                            "每日可获得的传送卡数量。\n"
            );
            teleportCardDaily.setMinValue(0).setMaxValue(9999);
            teleportCardDaily.comment = (teleportCardDaily.comment + " [range: " + teleportCardDaily.getMinValue() + " ~ " + teleportCardDaily.getMaxValue() + ", default: " + teleportCardDaily.getDefault() + "]");
            if (write) teleportCardDaily.set(TELEPORT_CARD_DAILY);
            TELEPORT_CARD_DAILY = teleportCardDaily.getInt();

            // 传送卡应用方式
            Property teleportCardType = config.get(CATEGORY_BASE + ".TpCard", "teleportCardType", ECardType.REFUND_ALL_COST.name(),
                    "The application method of the Teleport Card:\n" +
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
                            "若传送卡与代价都不足，则无法传送。若希望传送卡能够抵消代价但在不足时禁止传送，请在配置中将代价设置为零。\n"
                    , Arrays.stream(ECardType.values()).map(ECardType::name).toArray(String[]::new));
            if (write) teleportCardType.set(TELEPORT_CARD_TYPE.name());
            TELEPORT_CARD_TYPE = ECardType.valueOf(teleportCardType.getString());

            // 历史传送记录数量限制
            Property teleportRecordLimit = config.get(CATEGORY_BASE, "teleportRecordLimit", 100,
                    "The limit of teleport records, 0 means no limit.\n" +
                            "传送记录数量限制，数量为0表示不限制。\n"
            );
            teleportRecordLimit.setMinValue(0).setMaxValue(9999);
            teleportRecordLimit.comment = (teleportRecordLimit.comment + " [range: " + teleportRecordLimit.getMinValue() + " ~ " + teleportRecordLimit.getMaxValue() + ", default: " + teleportRecordLimit.getDefault() + "]");
            if (write) teleportRecordLimit.set(TELEPORT_RECORD_LIMIT);
            TELEPORT_RECORD_LIMIT = teleportRecordLimit.getInt();

            // 跨维度传送
            Property teleportAcrossDimension = config.get(CATEGORY_BASE, "teleportAcrossDimension", true,
                    "Is the teleport across dimensions enabled?\n" +
                            "是否启用跨维度传送。\n"
            );
            if (write) teleportAcrossDimension.set(TELEPORT_ACROSS_DIMENSION);
            TELEPORT_ACROSS_DIMENSION = teleportAcrossDimension.getBoolean();

            // 传送代价中传送距离计算限制
            Property teleportDistanceLimit = config.get(CATEGORY_BASE, "teleportDistanceLimit", 10000,
                    "The distance calculation limit for teleport cost, 0 means no limit.\n" +
                            "(This config item is not the limit of teleport distance, but the limit of the distance multiplier used when calculating teleport cost.)\n" +
                            "传送代价中传送距离计算限制，值为0表示不限制。(此配置项并非限制传送距离，而是限制计算传送代价时使用的距离乘数。)\n"
            );
            teleportDistanceLimit.setMinValue(0).setMaxValue(Integer.MAX_VALUE);
            teleportDistanceLimit.comment = (teleportDistanceLimit.comment + " [range: " + teleportDistanceLimit.getMinValue() + " ~ " + teleportDistanceLimit.getMaxValue() + ", default: " + teleportDistanceLimit.getDefault() + "]");
            if (write) teleportDistanceLimit.set(TELEPORT_COST_DISTANCE_LIMIT);
            TELEPORT_COST_DISTANCE_LIMIT = teleportDistanceLimit.getInt();

            // 跨维度传送时传送代价中传送距离取值
            Property teleportDistanceAcrossDimension = config.get(CATEGORY_BASE, "teleportDistanceAcrossDimension", 10000,
                    "The distance value for teleport cost when teleport across dimensions, 0 means no limit.\n" +
                            "跨维度传送时传送代价中传送距离取值，值为0表示不限制。\n"
            );
            teleportDistanceAcrossDimension.setMinValue(0).setMaxValue(Integer.MAX_VALUE);
            teleportDistanceAcrossDimension.comment = (teleportDistanceAcrossDimension.comment + " [range: " + teleportDistanceAcrossDimension.getMinValue() + " ~ " + teleportDistanceAcrossDimension.getMaxValue() + ", default: " + teleportDistanceAcrossDimension.getDefault() + "]");
            if (write) teleportDistanceAcrossDimension.set(TELEPORT_COST_DISTANCE_ACROSS_DIMENSION);
            TELEPORT_COST_DISTANCE_ACROSS_DIMENSION = teleportDistanceAcrossDimension.getInt();

            // 传送至视线尽头时最远传送距离限制
            Property teleportViewDistanceLimit = config.get(CATEGORY_BASE, "teleportViewDistanceLimit", 16 * 64,
                    "The distance limit for teleporting to the view, 0 means no limit.\n" +
                            "传送至视线尽头时最远传送距离限制，值为0表示不限制。\n"
            );
            teleportViewDistanceLimit.setMinValue(0).setMaxValue(Integer.MAX_VALUE);
            teleportViewDistanceLimit.comment = (teleportViewDistanceLimit.comment + " [range: " + teleportViewDistanceLimit.getMinValue() + " ~ " + teleportViewDistanceLimit.getMaxValue() + ", default: " + teleportViewDistanceLimit.getDefault() + "]");
            if (write) teleportViewDistanceLimit.set(TELEPORT_VIEW_DISTANCE_LIMIT);
            TELEPORT_VIEW_DISTANCE_LIMIT = teleportViewDistanceLimit.getInt();

            // 传送请求过期时间
            Property teleportRequestExpireTime = config.get(CATEGORY_BASE, "teleportRequestExpireTime", 60,
                    "The expire time for teleport request, in seconds.\n" +
                            "传送请求过期时间，单位为秒。\n"
            );
            teleportRequestExpireTime.setMinValue(0).setMaxValue(60 * 60);
            teleportRequestExpireTime.comment = (teleportRequestExpireTime.comment + " [range: " + teleportRequestExpireTime.getMinValue() + " ~ " + teleportRequestExpireTime.getMaxValue() + ", default: " + teleportRequestExpireTime.getDefault() + "]");
            if (write) teleportRequestExpireTime.set(TELEPORT_REQUEST_EXPIRE_TIME);
            TELEPORT_REQUEST_EXPIRE_TIME = teleportRequestExpireTime.getInt();

            // 传送请求冷却时间计算方式
            Property teleportRequestCooldownType = config.get(CATEGORY_BASE, "teleportRequestCooldownType", ECoolDownType.INDIVIDUAL.name(),
                    "The method used to calculate the cooldown time for teleport requests.\n" +
                            "COMMON: All commands share the same global cooldown defined by 'teleportRequestCooldown'.\n" +
                            "INDIVIDUAL: Each command has a separate cooldown managed by the command itself.\n" +
                            "MIXED: Combines both methods, using both the global cooldown and individual cooldowns.\n" +
                            "传送请求冷却时间的计算方式：\n" +
                            "COMMON：所有传送共用全局冷却时间，由'teleportRequestCooldown'配置定义。\n" +
                            "INDIVIDUAL：每个指令有单独的冷却时间，由指令自身管理。\n" +
                            "MIXED：结合两种方式，同时使用全局冷却时间和单独冷却时间。\n"
                    , Arrays.stream(ECoolDownType.values()).map(ECoolDownType::name).toArray(String[]::new));
            if (write) teleportRequestCooldownType.set(TELEPORT_REQUEST_COOLDOWN_TYPE.name());
            TELEPORT_REQUEST_COOLDOWN_TYPE = ECoolDownType.valueOf(teleportRequestCooldownType.getString());

            // 传送请求冷却时间
            Property teleportRequestCooldown = config.get(CATEGORY_BASE, "teleportRequestCooldown", 10,
                    "The global cooldown time for teleport requests, measured in seconds.\n" +
                            "This value applies to all commands when the cooldown type is COMMON or MIXED.\n" +
                            "传送请求的全局冷却时间，单位为秒。\n" +
                            "当冷却时间计算方式为COMMON或MIXED时，此值对所有指令生效。\n"
            );
            teleportRequestCooldown.setMinValue(0).setMaxValue(60 * 60 * 24);
            teleportRequestCooldown.comment = (teleportRequestCooldown.comment + " [range: " + teleportRequestCooldown.getMinValue() + " ~ " + teleportRequestCooldown.getMaxValue() + ", default: " + teleportRequestCooldown.getDefault() + "]");
            if (write) teleportRequestCooldown.set(TELEPORT_REQUEST_COOLDOWN);
            TELEPORT_REQUEST_COOLDOWN = teleportRequestCooldown.getInt();

            // 随机传送距离限制
            Property teleportRandomDistanceLimit = config.get(CATEGORY_BASE, "teleportRandomDistanceLimit", 10000,
                    "The maximum distance limit for random teleportation or teleportation to a specified structure.\n" +
                            "随机传送与传送至指定结构的最大距离限制。\n"
            );
            teleportRandomDistanceLimit.setMinValue(0).setMaxValue(Integer.MAX_VALUE);
            teleportRandomDistanceLimit.comment = (teleportRandomDistanceLimit.comment + " [range: " + teleportRandomDistanceLimit.getMinValue() + " ~ " + teleportRandomDistanceLimit.getMaxValue() + ", default: " + teleportRandomDistanceLimit.getDefault() + "]");
            if (write) teleportRandomDistanceLimit.set(TELEPORT_RANDOM_DISTANCE_LIMIT);
            TELEPORT_RANDOM_DISTANCE_LIMIT = teleportRandomDistanceLimit.getInt();

            // 玩家可设置的家的数量
            Property teleportHomeLimit = config.get(CATEGORY_BASE, "teleportHomeLimit", 5,
                    "The maximum number of homes that can be set by the player.\n" +
                            "玩家可设置的家的数量。\n"
            );
            teleportHomeLimit.setMinValue(1).setMaxValue(9999);
            teleportHomeLimit.comment = (teleportHomeLimit.comment + " [range: " + teleportHomeLimit.getMinValue() + " ~ " + teleportHomeLimit.getMaxValue() + ", default: " + teleportHomeLimit.getDefault() + "]");
            if (write) teleportHomeLimit.set(TELEPORT_HOME_LIMIT);
            TELEPORT_HOME_LIMIT = teleportHomeLimit.getInt();

            // 命令前缀
            Property commandPrefix = config.get(CATEGORY_BASE, "commandPrefix", "narcissus",
                    "The prefix of the command, please only use English characters and underscores, otherwise it may cause problems.\n" +
                            "指令前缀，请仅使用英文字母及下划线，否则可能会出现问题。\n"
            );
            if (write) commandPrefix.set(COMMAND_PREFIX);
            COMMAND_PREFIX = commandPrefix.getString();

            // 虚拟权限
            Property opList = config.get(CATEGORY_BASE, "opList", "",
                    "Virtual permission list, in this list you can directly specify which players can use which mod commands without enabling cheat mode or setting the player as OP.\n" +
                            "Format: \"player UUID\":\"a comma-separated list of commands that the player can use\". \n" +
                            "虚拟权限列表，在这里可以直接指定某个玩家能够使用哪些mod内的指令，而不需要开启作弊模式或将他设置为OP。\n" +
                            "格式：\"玩家UUID\":\"逗号分隔的能够使用的指令列表\"\n" +
                            "Example: S:opList={\"23a23a23-od0o-23aa-2333-0d0o0d0033aa\":[\"VIRTUAL_OP\",\"TP_BACK\",\"TP_HOME\",\"TP_STAGE\",\"TP_ASK\",\"TP_HERE\",\"TP_SPAWN\",\"TP_SPAWN_OTHER\",\"DIMENSION\",\"TP_COORDINATE\",\"TP_STRUCTURE\",\"TP_TOP\",\"TP_DOWN\",\"TP_RANDOM\",\"FEED\",\"FEED_OTHER\",\"SET_STAGE\",\"DEL_STAGE\"]}\n"
            );
            if (write) opList.set(OP_LIST);
            OP_LIST = opList.getString();

            // 帮助指令信息头部内容
            Property helpHeader = config.get(CATEGORY_BASE, "helpHeader", "-----==== Narcissus Farewell Help (%d/%d) ====-----",
                    "The header content of the help command.\n" +
                            "帮助指令信息头部内容。\n"
            );
            if (write) helpHeader.set(HELP_HEADER);
            HELP_HEADER = helpHeader.getString();

            // 传送音效
            Property tpSound = config.get(CATEGORY_BASE, "tpSound", "mob.endermen.portal",
                    "The sound effect when teleporting.\n" +
                            "传送时的音效。\n"
            );
            if (write) tpSound.set(TP_SOUND);
            TP_SOUND = tpSound.getString();

            // 不安全的方块
            Property unsafeBlocks = config.get(CATEGORY_BASE + ".Safe", "unsafeBlocks", new String[]{"minecraft:lava", "minecraft:fire", "minecraft:cactus"},
                    "The list of unsafe blocks, players will not be teleported to these blocks.\n" +
                            "不安全的方块列表，玩家不会传送到这些方块上。\n"
            );
            if (write) unsafeBlocks.set(UNSAFE_BLOCKS);
            UNSAFE_BLOCKS = unsafeBlocks.getStringList();

            // 窒息的方块
            Property suffocatingBlocks = config.get(CATEGORY_BASE + ".Safe", "suffocatingBlocks", new String[]{"minecraft:lava", "minecraft:water"},
                    "The list of suffocating blocks, players will not be teleported to these blocks.\n" +
                            "窒息的方块列表，玩家头不会处于这些方块里面。\n"
            );
            if (write) suffocatingBlocks.set(SUFFOCATING_BLOCKS);
            SUFFOCATING_BLOCKS = suffocatingBlocks.getStringList();

            // 安全传送放置方块
            Property setBlockWhenSafeNotFound = config.get(CATEGORY_BASE + ".Safe", "setBlockWhenSafeNotFound", false,
                    "When performing a safe teleport, whether to place a block underfoot if a safe coordinate is not found.\n" +
                            "当进行安全传送时，如果未找到安全坐标，是否在脚下放置方块。\n"
            );
            if (write) setBlockWhenSafeNotFound.set(SETBLOCK_WHEN_SAFE_NOT_FOUND);
            SETBLOCK_WHEN_SAFE_NOT_FOUND = setBlockWhenSafeNotFound.getBoolean();

            // 从背包获取安全方块
            Property getBlockFromInventory = config.get(CATEGORY_BASE + ".Safe", "getBlockFromInventory", true,
                    "When performing a safe teleport, whether to only use placeable blocks from the player's inventory if a safe coordinate is not found.\n" +
                            "当进行安全传送时，如果未找到安全坐标，是否仅从背包中获取可放置的方块。\n"
            );
            if (write) getBlockFromInventory.set(GETBLOCK_FROM_INVENTORY);
            GETBLOCK_FROM_INVENTORY = getBlockFromInventory.getBoolean();

            // 寻找安全坐标的区块范围
            Property safeChunkRange = config.get(CATEGORY_BASE + ".Safe", "safeChunkRange", 1,
                    "The chunk range for finding a safe coordinate, in chunks.\n" +
                            "当进行安全传送时，寻找安全坐标的半径，单位为区块。\n"
            );
            safeChunkRange.setMinValue(1).setMaxValue(16);
            safeChunkRange.comment = (safeChunkRange.comment + " [range: " + safeChunkRange.getMinValue() + " ~ " + safeChunkRange.getMaxValue() + ", default: " + safeChunkRange.getDefault() + "]");
            if (write) safeChunkRange.set(SAFE_CHUNK_RANGE);
            SAFE_CHUNK_RANGE = safeChunkRange.getInt();

            // 安全方块类型
            Property safeBlocks = config.get(CATEGORY_BASE + ".Safe", "safeBlocks", new String[]{"minecraft:grass", "minecraft:grass_path", "minecraft:dirt", "minecraft:cobblestone"},
                    "When performing a safe teleport, the list of blocks to place if a safe coordinate is not found. If 'getBlockFromInventory' is set to false, the first block in the list will always be used.\n" +
                            "当进行安全传送时，如果未找到安全坐标，放置方块的列表。若'getBlockFromInventory'为false，则始终使用列表中的第一个方块。\n"
            );
            if (write) safeBlocks.set(SAFE_BLOCKS);
            SAFE_BLOCKS = safeBlocks.getStringList();
        }

        // 定义功能开关
        {
            Property switchFeed = config.get(CATEGORY_SWITCH, "switchFeed", true,
                    "Enable or disable the option to 'Suicide or poisoning'.\n" +
                            "是否启用自杀或毒杀。\n"
            );
            if (write) switchFeed.set(SWITCH_FEED);
            SWITCH_FEED = switchFeed.getBoolean();

            Property switchTpCoordinate = config.get(CATEGORY_SWITCH, "switchTpCoordinate", true,
                    "Enable or disable the option to 'Teleport to the specified coordinates'.\n" +
                            "是否启用传送到指定坐标。\n"
            );
            if (write) switchTpCoordinate.set(SWITCH_TP_COORDINATE);
            SWITCH_TP_COORDINATE = switchTpCoordinate.getBoolean();

            Property switchTpStructure = config.get(CATEGORY_SWITCH, "switchTpStructure", true,
                    "Enable or disable the option to 'Teleport to the specified structure'.\n" +
                            "是否启用传送到指定结构。\n"
            );
            if (write) switchTpStructure.set(SWITCH_TP_STRUCTURE);
            SWITCH_TP_STRUCTURE = switchTpStructure.getBoolean();

            Property switchTpAsk = config.get(CATEGORY_SWITCH, "switchTpAsk", true,
                    "Enable or disable the option to 'Request to teleport oneself to other players'.\n" +
                            "是否启用传送请求。\n"
            );
            if (write) switchTpAsk.set(SWITCH_TP_ASK);
            SWITCH_TP_ASK = switchTpAsk.getBoolean();

            Property switchTpHere = config.get(CATEGORY_SWITCH, "switchTpHere", true,
                    "Enable or disable the option to 'Request the transfer of other players to oneself'.\n" +
                            "是否启用请求将玩家传送至当前位置。\n"
            );
            if (write) switchTpHere.set(SWITCH_TP_HERE);
            SWITCH_TP_HERE = switchTpHere.getBoolean();

            Property switchTpRandom = config.get(CATEGORY_SWITCH, "switchTpRandom", true,
                    "Enable or disable the option to 'Teleport to a random location'.\n" +
                            "是否启用随机传送。\n"
            );
            if (write) switchTpRandom.set(SWITCH_TP_RANDOM);
            SWITCH_TP_RANDOM = switchTpRandom.getBoolean();

            Property switchTpSpawn = config.get(CATEGORY_SWITCH, "switchTpSpawn", true,
                    "Enable or disable the option to 'Teleport to the spawn of the player'.\n" +
                            "是否启用传送到玩家重生点。\n"
            );
            if (write) switchTpSpawn.set(SWITCH_TP_SPAWN);
            SWITCH_TP_SPAWN = switchTpSpawn.getBoolean();

            Property switchTpWorldSpawn = config.get(CATEGORY_SWITCH, "switchTpWorldSpawn", true,
                    "Enable or disable the option to 'Teleport to the spawn of the world'.\n" +
                            "是否启用传送到世界重生点。\n"
            );
            if (write) switchTpWorldSpawn.set(SWITCH_TP_WORLD_SPAWN);
            SWITCH_TP_WORLD_SPAWN = switchTpWorldSpawn.getBoolean();

            Property switchTpTop = config.get(CATEGORY_SWITCH, "switchTpTop", true,
                    "Enable or disable the option to 'Teleport to the top of current position'.\n" +
                            "是否启用传送到顶部。\n"
            );
            if (write) switchTpTop.set(SWITCH_TP_TOP);
            SWITCH_TP_TOP = switchTpTop.getBoolean();

            Property switchTpBottom = config.get(CATEGORY_SWITCH, "switchTpBottom", true,
                    "Enable or disable the option to 'Teleport to the bottom of current position'.\n" +
                            "是否启用传送到底部。\n"
            );
            if (write) switchTpBottom.set(SWITCH_TP_BOTTOM);
            SWITCH_TP_BOTTOM = switchTpBottom.getBoolean();

            Property switchTpUp = config.get(CATEGORY_SWITCH, "switchTpUp", true,
                    "Enable or disable the option to 'Teleport to the upper of current position'.\n" +
                            "是否启用传送到上方。\n"
            );
            if (write) switchTpUp.set(SWITCH_TP_UP);
            SWITCH_TP_UP = switchTpUp.getBoolean();

            Property switchTpDown = config.get(CATEGORY_SWITCH, "switchTpDown", true,
                    "Enable or disable the option to 'Teleport to the lower of current position'.\n" +
                            "是否启用传送到下方。\n"
            );
            if (write) switchTpDown.set(SWITCH_TP_DOWN);
            SWITCH_TP_DOWN = switchTpDown.getBoolean();

            Property switchTpView = config.get(CATEGORY_SWITCH, "switchTpView", true,
                    "Enable or disable the option to 'Teleport to the end of the line of sight'.\n" +
                            "This function is independent of the player's render distance setting.\n" +
                            "是否启用传送至视线尽头。\n" +
                            "该功能与玩家设置的视距无关。\n"
            );
            if (write) switchTpView.set(SWITCH_TP_VIEW);
            SWITCH_TP_VIEW = switchTpView.getBoolean();

            Property switchTpHome = config.get(CATEGORY_SWITCH, "switchTpHome", true,
                    "Enable or disable the option to 'Teleport to the home'.\n" +
                            "是否启用传送到家。\n"
            );
            if (write) switchTpHome.set(SWITCH_TP_HOME);
            SWITCH_TP_HOME = switchTpHome.getBoolean();

            Property switchTpStage = config.get(CATEGORY_SWITCH, "switchTpStage", true,
                    "Enable or disable the option to 'Teleport to the stage'.\n" +
                            "是否启用传送到驿站。\n"
            );
            if (write) switchTpStage.set(SWITCH_TP_STAGE);
            SWITCH_TP_STAGE = switchTpStage.getBoolean();

            Property switchTpBack = config.get(CATEGORY_SWITCH, "switchTpBack", true,
                    "Enable or disable the option to 'Teleport to the previous location'.\n" +
                            "是否启用传送到上次传送点。\n"
            );
            if (write) switchTpBack.set(SWITCH_TP_BACK);
            SWITCH_TP_BACK = switchTpBack.getBoolean();
        }

        // 定义指令权限
        {

            {
                Property permissionFeedOther = config.get(CATEGORY_PERMISSION + ".command", "permissionFeedOther", 2,
                        "The permission level required to use the 'Poisoning others' command.\n" +
                                "毒杀指令所需的权限等级。\n"
                );
                permissionFeedOther.setMinValue(0).setMaxValue(4);
                permissionFeedOther.comment = (permissionFeedOther.comment + " [range: " + permissionFeedOther.getMinValue() + " ~ " + permissionFeedOther.getMaxValue() + ", default: " + permissionFeedOther.getDefault() + "]");
                if (write) permissionFeedOther.set(PERMISSION_FEED_OTHER);
                PERMISSION_FEED_OTHER = permissionFeedOther.getInt();

                Property permissionTpCoordinate = config.get(CATEGORY_PERMISSION + ".command", "permissionTpCoordinate", 2,
                        "The permission level required to use the 'Teleport to the specified coordinates' command.\n" +
                                "传送到指定坐标指令所需的权限等级。\n"
                );
                permissionTpCoordinate.setMinValue(0).setMaxValue(4);
                permissionTpCoordinate.comment = (permissionTpCoordinate.comment + " [range: " + permissionTpCoordinate.getMinValue() + " ~ " + permissionTpCoordinate.getMaxValue() + ", default: " + permissionTpCoordinate.getDefault() + "]");
                if (write) permissionTpCoordinate.set(PERMISSION_TP_COORDINATE);
                PERMISSION_TP_COORDINATE = permissionTpCoordinate.getInt();

                Property permissionTpStructure = config.get(CATEGORY_PERMISSION + ".command", "permissionTpStructure", 2,
                        "The permission level required to use the 'Teleport to the specified structure' command.\n" +
                                "传送到指定结构指令所需的权限等级。\n"
                );
                permissionTpStructure.setMinValue(0).setMaxValue(4);
                permissionTpStructure.comment = (permissionTpStructure.comment + " [range: " + permissionTpStructure.getMinValue() + " ~ " + permissionTpStructure.getMaxValue() + ", default: " + permissionTpStructure.getDefault() + "]");
                PERMISSION_TP_STRUCTURE = permissionTpStructure.getInt();

                Property permissionTpAsk = config.get(CATEGORY_PERMISSION + ".command", "permissionTpAsk", 0,
                        "The permission level required to use the 'Request to teleport oneself to other players' command.\n" +
                                "请求传送至玩家指令所需的权限等级。\n"
                );
                permissionTpAsk.setMinValue(0).setMaxValue(4);
                permissionTpAsk.comment = (permissionTpAsk.comment + " [range: " + permissionTpAsk.getMinValue() + " ~ " + permissionTpAsk.getMaxValue() + ", default: " + permissionTpAsk.getDefault() + "]");
                if (write) permissionTpAsk.set(PERMISSION_TP_ASK);
                PERMISSION_TP_ASK = permissionTpAsk.getInt();

                Property permissionTpHere = config.get(CATEGORY_PERMISSION + ".command", "permissionTpHere", 0,
                        "The permission level required to use the 'Request the transfer of other players to oneself' command.\n" +
                                "请求将玩家传送至当前位置指令所需的权限等级。\n"
                );
                permissionTpHere.setMinValue(0).setMaxValue(4);
                permissionTpHere.comment = (permissionTpHere.comment + " [range: " + permissionTpHere.getMinValue() + " ~ " + permissionTpHere.getMaxValue() + ", default: " + permissionTpHere.getDefault() + "]");
                if (write) permissionTpAsk.set(PERMISSION_TP_HERE);
                PERMISSION_TP_HERE = permissionTpHere.getInt();

                Property permissionTpRandom = config.get(CATEGORY_PERMISSION + ".command", "permissionTpRandom", 1,
                        "The permission level required to use the 'Teleport to a random location' command.\n" +
                                "随机传送指令所需的权限等级。\n"
                );
                permissionTpRandom.setMinValue(0).setMaxValue(4);
                permissionTpRandom.comment = (permissionTpRandom.comment + " [range: " + permissionTpRandom.getMinValue() + " ~ " + permissionTpRandom.getMaxValue() + ", default: " + permissionTpRandom.getDefault() + "]");
                if (write) permissionTpRandom.set(PERMISSION_TP_RANDOM);
                PERMISSION_TP_RANDOM = permissionTpRandom.getInt();

                Property permissionTpSpawn = config.get(CATEGORY_PERMISSION + ".command", "permissionTpSpawn", 0,
                        "The permission level required to use the 'Teleport to the spawn of the player' command.\n" +
                                "传送到玩家重生点指令所需的权限等级。\n"
                );
                permissionTpSpawn.setMinValue(0).setMaxValue(4);
                permissionTpSpawn.comment = (permissionTpSpawn.comment + " [range: " + permissionTpSpawn.getMinValue() + " ~ " + permissionTpSpawn.getMaxValue() + ", default: " + permissionTpSpawn.getDefault() + "]");
                if (write) permissionTpSpawn.set(PERMISSION_TP_SPAWN);
                PERMISSION_TP_SPAWN = permissionTpSpawn.getInt();

                Property permissionTpSpawnOther = config.get(CATEGORY_PERMISSION + ".command", "permissionTpSpawnOther", 2,
                        "The permission level required to use the 'Teleport to the spawn of the other player' command.\n" +
                                "传送到其他玩家重生点指令所需的权限等级。\n"
                );
                permissionTpSpawnOther.setMinValue(0).setMaxValue(4);
                permissionTpSpawnOther.comment = (permissionTpSpawnOther.comment + " [range: " + permissionTpSpawnOther.getMinValue() + " ~ " + permissionTpSpawnOther.getMaxValue() + ", default: " + permissionTpSpawnOther.getDefault() + "]");
                if (write) permissionTpSpawnOther.set(PERMISSION_TP_SPAWN_OTHER);
                PERMISSION_TP_SPAWN_OTHER = permissionTpSpawnOther.getInt();

                Property permissionTpWorldSpawn = config.get(CATEGORY_PERMISSION + ".command", "permissionTpWorldSpawn", 0,
                        "The permission level required to use the 'Teleport to the spawn of the world' command.\n" +
                                "传送到世界重生点指令所需的权限等级。\n"
                );
                permissionTpWorldSpawn.setMinValue(0).setMaxValue(4);
                permissionTpWorldSpawn.comment = (permissionTpWorldSpawn.comment + " [range: " + permissionTpWorldSpawn.getMinValue() + " ~ " + permissionTpWorldSpawn.getMaxValue() + ", default: " + permissionTpWorldSpawn.getDefault() + "]");
                if (write) permissionTpWorldSpawn.set(PERMISSION_TP_WORLD_SPAWN);
                PERMISSION_TP_WORLD_SPAWN = permissionTpWorldSpawn.getInt();

                Property permissionTpTop = config.get(CATEGORY_PERMISSION + ".command", "permissionTpTop", 1,
                        "The permission level required to use the 'Teleport to the top of current position' command.\n" +
                                "传送到顶部指令所需的权限等级。\n"
                );
                permissionTpTop.setMinValue(0).setMaxValue(4);
                permissionTpTop.comment = (permissionTpTop.comment + " [range: " + permissionTpTop.getMinValue() + " ~ " + permissionTpTop.getMaxValue() + ", default: " + permissionTpTop.getDefault() + "]");
                if (write) permissionTpTop.set(PERMISSION_TP_TOP);
                PERMISSION_TP_TOP = permissionTpTop.getInt();

                Property permissionTpBottom = config.get(CATEGORY_PERMISSION + ".command", "permissionTpBottom", 1,
                        "The permission level required to use the 'Teleport to the bottom of current position' command.\n" +
                                "传送到底部指令所需的权限等级。\n"
                );
                permissionTpBottom.setMinValue(0).setMaxValue(4);
                permissionTpBottom.comment = (permissionTpBottom.comment + " [range: " + permissionTpBottom.getMinValue() + " ~ " + permissionTpBottom.getMaxValue() + ", default: " + permissionTpBottom.getDefault() + "]");
                if (write) permissionTpBottom.set(PERMISSION_TP_BOTTOM);
                PERMISSION_TP_BOTTOM = permissionTpBottom.getInt();

                Property permissionTpUp = config.get(CATEGORY_PERMISSION + ".command", "permissionTpUp", 1,
                        "The permission level required to use the 'Teleport to the upper of current position' command.\n" +
                                "传送到上方指令所需的权限等级。\n"
                );
                permissionTpUp.setMinValue(0).setMaxValue(4);
                permissionTpUp.comment = (permissionTpUp.comment + " [range: " + permissionTpUp.getMinValue() + " ~ " + permissionTpUp.getMaxValue() + ", default: " + permissionTpUp.getDefault() + "]");
                if (write) permissionTpUp.set(PERMISSION_TP_UP);
                PERMISSION_TP_UP = permissionTpUp.getInt();

                Property permissionTpDown = config.get(CATEGORY_PERMISSION + ".command", "permissionTpDown", 1,
                        "The permission level required to use the 'Teleport to the lower of current position' command.\n" +
                                "传送到下方指令所需的权限等级。\n"
                );
                permissionTpDown.setMinValue(0).setMaxValue(4);
                permissionTpDown.comment = (permissionTpDown.comment + " [range: " + permissionTpDown.getMinValue() + " ~ " + permissionTpDown.getMaxValue() + ", default: " + permissionTpDown.getDefault() + "]");
                if (write) permissionTpDown.set(PERMISSION_TP_DOWN);
                PERMISSION_TP_DOWN = permissionTpDown.getInt();

                Property permissionTpView = config.get(CATEGORY_PERMISSION + ".command", "permissionTpView", 1,
                        "The permission level required to use the 'Teleport to the end of the line of sight' command.\n" +
                                "This function is independent of the player's render distance setting.\n" +
                                "传送至视线尽头指令所需的权限等级。\n" +
                                "该功能与玩家设置的视距无关。\n"
                );
                permissionTpView.setMinValue(0).setMaxValue(4);
                permissionTpView.comment = (permissionTpView.comment + " [range: " + permissionTpView.getMinValue() + " ~ " + permissionTpView.getMaxValue() + ", default: " + permissionTpView.getDefault() + "]");
                if (write) permissionTpView.set(PERMISSION_TP_VIEW);
                PERMISSION_TP_VIEW = permissionTpView.getInt();

                Property permissionTpHome = config.get(CATEGORY_PERMISSION + ".command", "permissionTpHome", 0,
                        "The permission level required to use the 'Teleport to the home' command.\n" +
                                "传送到家指令所需的权限等级。\n"
                );
                permissionTpHome.setMinValue(0).setMaxValue(4);
                permissionTpHome.comment = (permissionTpHome.comment + " [range: " + permissionTpHome.getMinValue() + " ~ " + permissionTpHome.getMaxValue() + ", default: " + permissionTpHome.getDefault() + "]");
                if (write) permissionTpHome.set(PERMISSION_TP_HOME);
                PERMISSION_TP_HOME = permissionTpHome.getInt();

                Property permissionTpStage = config.get(CATEGORY_PERMISSION + ".command", "permissionTpStage", 0,
                        "The permission level required to use the 'Teleport to the stage' command.\n" +
                                "传送到驿站指令所需的权限等级。\n"
                );
                permissionTpStage.setMinValue(0).setMaxValue(4);
                permissionTpStage.comment = (permissionTpStage.comment + " [range: " + permissionTpStage.getMinValue() + " ~ " + permissionTpStage.getMaxValue() + ", default: " + permissionTpStage.getDefault() + "]");
                if (write) permissionTpStage.set(PERMISSION_TP_STAGE);
                PERMISSION_TP_STAGE = permissionTpStage.getInt();

                Property permissionSetStage = config.get(CATEGORY_PERMISSION + ".command", "permissionSetStage", 2,
                        "The permission level required to use the 'Set the stage' command.\n" +
                                "设置驿站指令所需的权限等级。\n"
                );
                permissionSetStage.setMinValue(0).setMaxValue(4);
                permissionSetStage.comment = (permissionSetStage.comment + " [range: " + permissionSetStage.getMinValue() + " ~ " + permissionSetStage.getMaxValue() + ", default: " + permissionSetStage.getDefault() + "]");
                if (write) permissionSetStage.set(PERMISSION_SET_STAGE);
                PERMISSION_SET_STAGE = permissionSetStage.getInt();

                Property permissionDelStage = config.get(CATEGORY_PERMISSION + ".command", "permissionDelStage", 2,
                        "The permission level required to use the 'Delete the stage' command.\n" +
                                "删除驿站指令所需的权限等级。\n"
                );
                permissionDelStage.setMinValue(0).setMaxValue(4);
                permissionDelStage.comment = (permissionDelStage.comment + " [range: " + permissionDelStage.getMinValue() + " ~ " + permissionDelStage.getMaxValue() + ", default: " + permissionDelStage.getDefault() + "]");
                if (write) permissionDelStage.set(PERMISSION_DEL_STAGE);
                PERMISSION_DEL_STAGE = permissionDelStage.getInt();

                Property permissionGetStage = config.get(CATEGORY_PERMISSION + ".command", "permissionTpStageGet", 0,
                        "The permission level required to use the 'Get the stage info' command.\n" +
                                "查询驿站指令所需的权限等级。\n"
                );
                permissionGetStage.setMinValue(0).setMaxValue(4);
                permissionGetStage.comment = (permissionGetStage.comment + " [range: " + permissionGetStage.getMinValue() + " ~ " + permissionGetStage.getMaxValue() + ", default: " + permissionGetStage.getDefault() + "]");
                if (write) permissionGetStage.set(PERMISSION_GET_STAGE);
                PERMISSION_GET_STAGE = permissionGetStage.getInt();

                Property permissionTpBack = config.get(CATEGORY_PERMISSION + ".command", "permissionTpBack", 0,
                        "The permission level required to use the 'Teleport to the previous location' command.\n" +
                                "传送到上次传送点指令所需的权限等级。\n"
                );
                permissionTpBack.setMinValue(0).setMaxValue(4);
                permissionTpBack.comment = (permissionTpBack.comment + " [range: " + permissionTpBack.getMinValue() + " ~ " + permissionTpBack.getMaxValue() + ", default: " + permissionTpBack.getDefault() + "]");
                if (write) permissionTpBack.set(PERMISSION_TP_BACK);
                PERMISSION_TP_BACK = permissionTpBack.getInt();

                Property permissionVirtualOp = config.get(CATEGORY_PERMISSION + ".command", "permissionVirtualOp", 4,
                        "The permission level required to use the 'Set virtual permission' command, and also used as the permission level for modifying server configuration.\n" +
                                "设置虚拟权限指令所需的权限等级，同时用于控制使用'修改服务器配置指令'的权限。\n"
                );
                permissionVirtualOp.setMinValue(0).setMaxValue(4);
                permissionVirtualOp.comment = (permissionVirtualOp.comment + " [range: " + permissionVirtualOp.getMinValue() + " ~ " + permissionVirtualOp.getMaxValue() + ", default: " + permissionVirtualOp.getDefault() + "]");
                if (write) permissionVirtualOp.set(PERMISSION_VIRTUAL_OP);
                PERMISSION_VIRTUAL_OP = permissionVirtualOp.getInt();
            }

            {
                Property permissionTpCoordinateAcrossDimension = config.get(CATEGORY_PERMISSION + ".across", "permissionTpCoordinateAcrossDimension", 2,
                        "The permission level required to use the 'Teleport to the specified coordinates' command across dimensions, -1 means disabled.\n" +
                                "跨维度传送到指定坐标指令所需的权限等级，若为-1则禁用跨维度传送。\n"
                );
                permissionTpCoordinateAcrossDimension.setMinValue(-1).setMaxValue(4);
                permissionTpCoordinateAcrossDimension.comment = (permissionTpCoordinateAcrossDimension.comment + " [range: " + permissionTpCoordinateAcrossDimension.getMinValue() + " ~ " + permissionTpCoordinateAcrossDimension.getMaxValue() + ", default: " + permissionTpCoordinateAcrossDimension.getDefault() + "]");
                if (write) permissionTpCoordinateAcrossDimension.set(PERMISSION_TP_COORDINATE_ACROSS_DIMENSION);
                PERMISSION_TP_COORDINATE_ACROSS_DIMENSION = permissionTpCoordinateAcrossDimension.getInt();

                Property permissionTpStructureAcrossDimension = config.get(CATEGORY_PERMISSION + ".across", "permissionTpStructureAcrossDimension", 2,
                        "The permission level required to use the 'Teleport to the specified structure' command across dimensions, -1 means disabled.\n" +
                                "跨维度传送到指定结构指令所需的权限等级，若为-1则禁用跨维度传送。\n"
                );
                permissionTpStructureAcrossDimension.setMinValue(-1).setMaxValue(4);
                permissionTpStructureAcrossDimension.comment = (permissionTpStructureAcrossDimension.comment + " [range: " + permissionTpStructureAcrossDimension.getMinValue() + " ~ " + permissionTpStructureAcrossDimension.getMaxValue() + ", default: " + permissionTpStructureAcrossDimension.getDefault() + "]");
                if (write) permissionTpStructureAcrossDimension.set(PERMISSION_TP_STRUCTURE_ACROSS_DIMENSION);
                PERMISSION_TP_STRUCTURE_ACROSS_DIMENSION = permissionTpStructureAcrossDimension.getInt();

                Property permissionTpAskAcrossDimension = config.get(CATEGORY_PERMISSION + ".across", "permissionTpAskAcrossDimension", 0,
                        "The permission level required to use the 'Request to teleport oneself to other players' command across dimensions, -1 means disabled.\n" +
                                "跨维度请求传送至玩家指令所需的权限等级，若为-1则禁用跨维度传送。\n"
                );
                permissionTpAskAcrossDimension.setMinValue(-1).setMaxValue(4);
                permissionTpAskAcrossDimension.comment = (permissionTpAskAcrossDimension.comment + " [range: " + permissionTpAskAcrossDimension.getMinValue() + " ~ " + permissionTpAskAcrossDimension.getMaxValue() + ", default: " + permissionTpAskAcrossDimension.getDefault() + "]");
                if (write) permissionTpAskAcrossDimension.set(PERMISSION_TP_ASK_ACROSS_DIMENSION);
                PERMISSION_TP_ASK_ACROSS_DIMENSION = permissionTpAskAcrossDimension.getInt();

                Property permissionTpHereAcrossDimension = config.get(CATEGORY_PERMISSION + ".across", "permissionTpHereAcrossDimension", 0,
                        "The permission level required to use the 'Teleport to the current position' command across dimensions, -1 means disabled.\n" +
                                "跨维度传送到当前位置指令所需的权限等级，若为-1则禁用跨维度传送。\n"
                );
                permissionTpHereAcrossDimension.setMinValue(-1).setMaxValue(4);
                permissionTpHereAcrossDimension.comment = (permissionTpHereAcrossDimension.comment + " [range: " + permissionTpHereAcrossDimension.getMinValue() + " ~ " + permissionTpHereAcrossDimension.getMaxValue() + ", default: " + permissionTpHereAcrossDimension.getDefault() + "]");
                if (write) permissionTpHereAcrossDimension.set(PERMISSION_TP_HERE_ACROSS_DIMENSION);
                PERMISSION_TP_HERE_ACROSS_DIMENSION = permissionTpHereAcrossDimension.getInt();

                Property permissionTpRandomAcrossDimension = config.get(CATEGORY_PERMISSION + ".across", "permissionTpRandomAcrossDimension", 0,
                        "The permission level required to use the 'Teleport to the random position' command across dimensions, -1 means disabled.\n" +
                                "跨维度传送到随机位置指令所需的权限等级，若为-1则禁用跨维度传送。\n"
                );
                permissionTpRandomAcrossDimension.setMinValue(-1).setMaxValue(4);
                permissionTpRandomAcrossDimension.comment = (permissionTpRandomAcrossDimension.comment + " [range: " + permissionTpRandomAcrossDimension.getMinValue() + " ~ " + permissionTpRandomAcrossDimension.getMaxValue() + ", default: " + permissionTpRandomAcrossDimension.getDefault() + "]");
                if (write) permissionTpRandomAcrossDimension.set(PERMISSION_TP_RANDOM_ACROSS_DIMENSION);
                PERMISSION_TP_RANDOM_ACROSS_DIMENSION = permissionTpRandomAcrossDimension.getInt();

                Property permissionTpSpawnAcrossDimension = config.get(CATEGORY_PERMISSION + ".across", "permissionTpSpawnAcrossDimension", 0,
                        "The permission level required to use the 'Teleport to the spawn of the current dimension' command across dimensions, -1 means disabled.\n" +
                                "跨维度传送到当前维度的出生点指令所需的权限等级，若为-1则禁用跨维度传送。\n"
                );
                permissionTpSpawnAcrossDimension.setMinValue(-1).setMaxValue(4);
                permissionTpSpawnAcrossDimension.comment = (permissionTpSpawnAcrossDimension.comment + " [range: " + permissionTpSpawnAcrossDimension.getMinValue() + " ~ " + permissionTpSpawnAcrossDimension.getMaxValue() + ", default: " + permissionTpSpawnAcrossDimension.getDefault() + "]");
                if (write) permissionTpSpawnAcrossDimension.set(PERMISSION_TP_SPAWN_ACROSS_DIMENSION);
                PERMISSION_TP_SPAWN_ACROSS_DIMENSION = permissionTpSpawnAcrossDimension.getInt();

                Property permissionTpWorldSpawnAcrossDimension = config.get(CATEGORY_PERMISSION + ".across", "permissionTpWorldSpawnAcrossDimension", 0,
                        "The permission level required to use the 'Teleport to the world spawn of the current dimension' command across dimensions, -1 means disabled.\n" +
                                "跨维度传送到当前维度的世界出生点指令所需的权限等级，若为-1则禁用跨维度传送。\n"
                );
                permissionTpWorldSpawnAcrossDimension.setMinValue(-1).setMaxValue(4);
                permissionTpWorldSpawnAcrossDimension.comment = (permissionTpWorldSpawnAcrossDimension.comment + " [range: " + permissionTpWorldSpawnAcrossDimension.getMinValue() + " ~ " + permissionTpWorldSpawnAcrossDimension.getMaxValue() + ", default: " + permissionTpWorldSpawnAcrossDimension.getDefault() + "]");
                if (write) permissionTpWorldSpawnAcrossDimension.set(PERMISSION_TP_WORLD_SPAWN_ACROSS_DIMENSION);
                PERMISSION_TP_WORLD_SPAWN_ACROSS_DIMENSION = permissionTpWorldSpawnAcrossDimension.getInt();

                Property permissionTpHomeAcrossDimension = config.get(CATEGORY_PERMISSION + ".across", "permissionTpHomeAcrossDimension", 0,
                        "The permission level required to use the 'Teleport to the home' command across dimensions, -1 means disabled.\n" +
                                "跨维度传送到家指令所需的权限等级，若为-1则禁用跨维度传送。\n"
                );
                permissionTpHomeAcrossDimension.setMinValue(-1).setMaxValue(4);
                permissionTpHomeAcrossDimension.comment = (permissionTpHomeAcrossDimension.comment + " [range: " + permissionTpHomeAcrossDimension.getMinValue() + " ~ " + permissionTpHomeAcrossDimension.getMaxValue() + ", default: " + permissionTpHomeAcrossDimension.getDefault() + "]");
                if (write) permissionTpHomeAcrossDimension.set(PERMISSION_TP_HOME_ACROSS_DIMENSION);
                PERMISSION_TP_HOME_ACROSS_DIMENSION = permissionTpHomeAcrossDimension.getInt();

                Property permissionTpStageAcrossDimension = config.get(CATEGORY_PERMISSION + ".across", "permissionTpStageAcrossDimension", 0,
                        "The permission level required to use the 'Teleport to the stage' command across dimensions, -1 means disabled.\n" +
                                "跨维度传送到驿站指令所需的权限等级，若为-1则禁用跨维度传送。\n"
                );
                permissionTpStageAcrossDimension.setMinValue(-1).setMaxValue(4);
                permissionTpStageAcrossDimension.comment = (permissionTpStageAcrossDimension.comment + " [range: " + permissionTpStageAcrossDimension.getMinValue() + " ~ " + permissionTpStageAcrossDimension.getMaxValue() + ", default: " + permissionTpStageAcrossDimension.getDefault() + "]");
                if (write) permissionTpStageAcrossDimension.set(PERMISSION_TP_STAGE_ACROSS_DIMENSION);
                PERMISSION_TP_STAGE_ACROSS_DIMENSION = permissionTpStageAcrossDimension.getInt();

                Property permissionTpBackAcrossDimension = config.get(CATEGORY_PERMISSION + ".across", "permissionTpBackAcrossDimension", 0,
                        "The permission level required to use the 'Teleport to the previous location' command across dimensions, -1 means disabled.\n" +
                                "跨维度传送到上次传送点指令所需的权限等级，若为-1则禁用跨维度传送。\n"
                );
                permissionTpBackAcrossDimension.setMinValue(-1).setMaxValue(4);
                permissionTpBackAcrossDimension.comment = (permissionTpBackAcrossDimension.comment + " [range: " + permissionTpBackAcrossDimension.getMinValue() + " ~ " + permissionTpBackAcrossDimension.getMaxValue() + ", default: " + permissionTpBackAcrossDimension.getDefault() + "]");
                if (write) permissionTpBackAcrossDimension.set(PERMISSION_TP_BACK_ACROSS_DIMENSION);
                PERMISSION_TP_BACK_ACROSS_DIMENSION = permissionTpBackAcrossDimension.getInt();
            }

        }

        // 定义冷却时间
        {
            Property cooldownTpCoordinate = config.get(CATEGORY_COOLDOWN, "cooldownTpCoordinate", 10,
                    "The cooldown time for 'Teleport to the specified coordinates', in seconds.\n" +
                            "传送到指定坐标的冷却时间，单位为秒。\n"
            );
            cooldownTpCoordinate.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpCoordinate.comment = (cooldownTpCoordinate.comment + " [range: " + cooldownTpCoordinate.getMinValue() + " ~ " + cooldownTpCoordinate.getMaxValue() + ", default: " + cooldownTpCoordinate.getDefault() + "]");
            if (write) cooldownTpCoordinate.set(COOLDOWN_TP_COORDINATE);
            COOLDOWN_TP_COORDINATE = cooldownTpCoordinate.getInt();

            Property cooldownTpStructure = config.get(CATEGORY_COOLDOWN, "cooldownTpStructure", 10,
                    "The cooldown time for 'Teleport to the specified structure', in seconds.\n" +
                            "传送到指定结构的冷却时间，单位为秒。\n"
            );
            cooldownTpStructure.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpStructure.comment = (cooldownTpStructure.comment + " [range: " + cooldownTpStructure.getMinValue() + " ~ " + cooldownTpStructure.getMaxValue() + ", default: " + cooldownTpStructure.getDefault() + "]");
            if (write) cooldownTpStructure.set(COOLDOWN_TP_STRUCTURE);
            COOLDOWN_TP_STRUCTURE = cooldownTpStructure.getInt();

            Property cooldownTpAsk = config.get(CATEGORY_COOLDOWN, "cooldownTpAsk", 10,
                    "The cooldown time for 'Request to teleport oneself to other players', in seconds.\n" +
                            "请求传送至玩家的冷却时间，单位为秒。\n"
            );
            cooldownTpAsk.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpAsk.comment = (cooldownTpAsk.comment + " [range: " + cooldownTpAsk.getMinValue() + " ~ " + cooldownTpAsk.getMaxValue() + ", default: " + cooldownTpAsk.getDefault() + "]");
            if (write) cooldownTpAsk.set(COOLDOWN_TP_ASK);
            COOLDOWN_TP_ASK = cooldownTpAsk.getInt();

            Property cooldownTpHere = config.get(CATEGORY_COOLDOWN, "cooldownTpHere", 10,
                    "The cooldown time for 'Request the transfer of other players to oneself', in seconds.\n" +
                            "请求将玩家传送至当前位置的冷却时间，单位为秒。\n"
            );
            cooldownTpHere.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpHere.comment = (cooldownTpHere.comment + " [range: " + cooldownTpHere.getMinValue() + " ~ " + cooldownTpHere.getMaxValue() + ", default: " + cooldownTpHere.getDefault() + "]");
            if (write) cooldownTpHere.set(COOLDOWN_TP_HERE);
            COOLDOWN_TP_HERE = cooldownTpHere.getInt();

            Property cooldownTpRandom = config.get(CATEGORY_COOLDOWN, "cooldownTpRandom", 10,
                    "The cooldown time for 'Teleport to a random location', in seconds.\n" +
                            "随机传送的冷却时间，单位为秒。\n"
            );
            cooldownTpRandom.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpRandom.comment = (cooldownTpRandom.comment + " [range: " + cooldownTpRandom.getMinValue() + " ~ " + cooldownTpRandom.getMaxValue() + ", default: " + cooldownTpRandom.getDefault() + "]");
            if (write) cooldownTpRandom.set(COOLDOWN_TP_RANDOM);
            COOLDOWN_TP_RANDOM = cooldownTpRandom.getInt();

            Property cooldownTpSpawn = config.get(CATEGORY_COOLDOWN, "cooldownTpSpawn", 10,
                    "The cooldown time for 'Teleport to the spawn of the player', in seconds.\n" +
                            "传送到玩家重生点的冷却时间，单位为秒。\n"
            );
            cooldownTpSpawn.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpSpawn.comment = (cooldownTpSpawn.comment + " [range: " + cooldownTpSpawn.getMinValue() + " ~ " + cooldownTpSpawn.getMaxValue() + ", default: " + cooldownTpSpawn.getDefault() + "]");
            if (write) cooldownTpSpawn.set(COOLDOWN_TP_SPAWN);
            COOLDOWN_TP_SPAWN = cooldownTpSpawn.getInt();

            Property cooldownTpWorldSpawn = config.get(CATEGORY_COOLDOWN, "cooldownTpWorldSpawn", 10,
                    "The cooldown time for 'Teleport to the spawn of the world', in seconds.\n" +
                            "传送到世界重生点的冷却时间，单位为秒。\n"
            );
            cooldownTpWorldSpawn.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpWorldSpawn.comment = (cooldownTpWorldSpawn.comment + " [range: " + cooldownTpWorldSpawn.getMinValue() + " ~ " + cooldownTpWorldSpawn.getMaxValue() + ", default: " + cooldownTpWorldSpawn.getDefault() + "]");
            if (write) cooldownTpWorldSpawn.set(COOLDOWN_TP_WORLD_SPAWN);
            COOLDOWN_TP_WORLD_SPAWN = cooldownTpWorldSpawn.getInt();

            Property cooldownTpTop = config.get(CATEGORY_COOLDOWN, "cooldownTpTop", 10,
                    "The cooldown time for 'Teleport to the top of current position', in seconds.\n" +
                            "传送到顶部的冷却时间，单位为秒。\n"
            );
            cooldownTpTop.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpTop.comment = (cooldownTpTop.comment + " [range: " + cooldownTpTop.getMinValue() + " ~ " + cooldownTpTop.getMaxValue() + ", default: " + cooldownTpTop.getDefault() + "]");
            if (write) cooldownTpTop.set(COOLDOWN_TP_TOP);
            COOLDOWN_TP_TOP = cooldownTpTop.getInt();

            Property cooldownTpBottom = config.get(CATEGORY_COOLDOWN, "cooldownTpBottom", 10,
                    "The cooldown time for 'Teleport to the bottom of current position', in seconds.\n" +
                            "传送到底部的冷却时间，单位为秒。\n"
            );
            cooldownTpBottom.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpBottom.comment = (cooldownTpBottom.comment + " [range: " + cooldownTpBottom.getMinValue() + " ~ " + cooldownTpBottom.getMaxValue() + ", default: " + cooldownTpBottom.getDefault() + "]");
            if (write) cooldownTpBottom.set(COOLDOWN_TP_BOTTOM);
            COOLDOWN_TP_BOTTOM = cooldownTpBottom.getInt();

            Property cooldownTpUp = config.get(CATEGORY_COOLDOWN, "cooldownTpUp", 10,
                    "The cooldown time for 'Teleport to the upper of current position', in seconds.\n" +
                            "传送到上方的冷却时间，单位为秒。\n"
            );
            cooldownTpUp.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpUp.comment = (cooldownTpUp.comment + " [range: " + cooldownTpUp.getMinValue() + " ~ " + cooldownTpUp.getMaxValue() + ", default: " + cooldownTpUp.getDefault() + "]");
            if (write) cooldownTpUp.set(COOLDOWN_TP_UP);
            COOLDOWN_TP_UP = cooldownTpUp.getInt();

            Property cooldownTpDown = config.get(CATEGORY_COOLDOWN, "cooldownTpDown", 10,
                    "The cooldown time for 'Teleport to the lower of current position', in seconds.\n" +
                            "传送到下方的冷却时间，单位为秒。\n"
            );
            cooldownTpDown.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpDown.comment = (cooldownTpDown.comment + " [range: " + cooldownTpDown.getMinValue() + " ~ " + cooldownTpDown.getMaxValue() + ", default: " + cooldownTpDown.getDefault() + "]");
            if (write) cooldownTpDown.set(COOLDOWN_TP_DOWN);
            COOLDOWN_TP_DOWN = cooldownTpDown.getInt();

            Property cooldownTpView = config.get(CATEGORY_COOLDOWN, "cooldownTpView", 10,
                    "The cooldown time for 'Teleport to the end of the line of sight', in seconds.\n" +
                            "This function is independent of the player's render distance setting.\n" +
                            "传送至视线尽头的冷却时间，单位为秒。\n" +
                            "该功能与玩家设置的视距无关。\n"
            );
            cooldownTpView.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpView.comment = (cooldownTpView.comment + " [range: " + cooldownTpView.getMinValue() + " ~ " + cooldownTpView.getMaxValue() + ", default: " + cooldownTpView.getDefault() + "]");
            if (write) cooldownTpView.set(COOLDOWN_TP_VIEW);
            COOLDOWN_TP_VIEW = cooldownTpView.getInt();

            Property cooldownTpHome = config.get(CATEGORY_COOLDOWN, "cooldownTpHome", 10,
                    "The cooldown time for 'Teleport to the home', in seconds.\n" +
                            "传送到家的冷却时间，单位为秒。\n"
            );
            cooldownTpHome.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpHome.comment = (cooldownTpHome.comment + " [range: " + cooldownTpHome.getMinValue() + " ~ " + cooldownTpHome.getMaxValue() + ", default: " + cooldownTpHome.getDefault() + "]");
            if (write) cooldownTpHome.set(COOLDOWN_TP_HOME);
            COOLDOWN_TP_HOME = cooldownTpHome.getInt();

            Property cooldownTpStage = config.get(CATEGORY_COOLDOWN, "cooldownTpStage", 10,
                    "The cooldown time for 'Teleport to the stage', in seconds.\n" +
                            "传送到驿站的冷却时间，单位为秒。\n"
            );
            cooldownTpStage.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpStage.comment = (cooldownTpStage.comment + " [range: " + cooldownTpStage.getMinValue() + " ~ " + cooldownTpStage.getMaxValue() + ", default: " + cooldownTpStage.getDefault() + "]");
            if (write) cooldownTpStage.set(COOLDOWN_TP_STAGE);
            COOLDOWN_TP_STAGE = cooldownTpStage.getInt();

            Property cooldownTpBack = config.get(CATEGORY_COOLDOWN, "cooldownTpBack", 10,
                    "The cooldown time for 'Teleport to the previous location', in seconds.\n" +
                            "传送到上次传送点的冷却时间，单位为秒。\n"
            );
            cooldownTpBack.setMinValue(0).setMaxValue(60 * 60 * 24);
            cooldownTpBack.comment = (cooldownTpBack.comment + " [range: " + cooldownTpBack.getMinValue() + " ~ " + cooldownTpBack.getMaxValue() + ", default: " + cooldownTpBack.getDefault() + "]");
            if (write) cooldownTpBack.set(COOLDOWN_TP_BACK);
            COOLDOWN_TP_BACK = cooldownTpBack.getInt();
        }

        // 定义自定义指令配置
        {
            // 获取玩家的UUID
            Property commandUuid = config.get(CATEGORY_COMMAND, "commandUuid", "uuid",
                    "This command is used to get the UUID of the player.\n" +
                            "获取玩家的UUID的指令。\n"
            );
            if (write) commandUuid.set(COMMAND_UUID);
            COMMAND_UUID = commandUuid.getString();

            // 获取当前世界的维度ID
            Property commandDimension = config.get(CATEGORY_COMMAND, "commandDimension", "dim",
                    "This command is used to get the dimension ID of the current world.\n" +
                            "获取当前世界的维度ID的指令。\n"
            );
            if (write) commandDimension.set(COMMAND_DIMENSION);
            COMMAND_DIMENSION = commandDimension.getString();

            // 自杀或毒杀
            Property commandFeed = config.get(CATEGORY_COMMAND, "commandFeed", "feed",
                    "This command is used to suicide or poisoning, narcissus are poisonous and should not be eaten.\n" +
                            "自杀或毒杀的指令，水仙是有毒的可不能食用哦。\n"
            );
            if (write) commandFeed.set(COMMAND_FEED);
            COMMAND_FEED = commandFeed.getString();

            // 传送到指定坐标
            Property commandTpCoordinate = config.get(CATEGORY_COMMAND, "commandTpCoordinate", "tpx",
                    "This command is used to teleport to the specified coordinates.\n" +
                            "传送到指定坐标的指令。\n"
            );
            if (write) commandTpCoordinate.set(COMMAND_TP_COORDINATE);
            COMMAND_TP_COORDINATE = commandTpCoordinate.getString();

            // 传送到指定结构
            Property commandTpStructure = config.get(CATEGORY_COMMAND, "commandTpStructure", "tpst",
                    "This command is used to teleport to the specified structure.\n" +
                            "传送到指定结构的指令。\n"
            );
            if (write) commandTpStructure.set(COMMAND_TP_STRUCTURE);
            COMMAND_TP_STRUCTURE = commandTpStructure.getString();

            // 请求传送至玩家指令
            Property commandTpAsk = config.get(CATEGORY_COMMAND + ".TpAsk", "commandTpAsk", "tpa",
                    "This command is used to request to teleport oneself to other players.\n" +
                            "请求传送至玩家的指令。\n"
            );
            if (write) commandTpAsk.set(COMMAND_TP_ASK);
            COMMAND_TP_ASK = commandTpAsk.getString();

            Property commandTpAskYes = config.get(CATEGORY_COMMAND + ".TpAsk", "commandTpAskYes", "tpay",
                    "This command is used to accept teleportation of other players to oneself.\n" +
                            "I can't translate it clearly either, as long as you understand the meaning. >_<\n" +
                            "接受请求传送至玩家的指令。\n" +
                            "我也翻译不清楚了，你懂意思就行。>_<\n"
            );
            if (write) commandTpAskYes.set(COMMAND_TP_ASK_YES);
            COMMAND_TP_ASK_YES = commandTpAskYes.getString();

            Property commandTpAskNo = config.get(CATEGORY_COMMAND + ".TpAsk", "commandTpAskNo", "tpan",
                    "This command is used to refuse teleportation of other players to oneself.\n" +
                            "I can't translate it clearly either, as long as you understand the meaning. >_<\n" +
                            "拒绝请求传送至玩家的指令。\n" +
                            "我也翻译不清楚了，你懂意思就行。>_<\n"
            );
            if (write) commandTpAskNo.set(COMMAND_TP_ASK_NO);
            COMMAND_TP_ASK_NO = commandTpAskNo.getString();

            // 请求将玩家传送至当前位置
            Property commandTpHere = config.get(CATEGORY_COMMAND + ".TpHere", "commandTpHere", "tph",
                    "This command is used to request the transfer of other players to oneself.\n" +
                            "请求将玩家传送至当前位置的指令。\n"
            );
            if (write) commandTpHere.set(COMMAND_TP_HERE);
            COMMAND_TP_HERE = commandTpHere.getString();

            Property commandTpHereYes = config.get(CATEGORY_COMMAND + ".TpHere", "commandTpHereYes", "tphy",
                    "This command is used to accept teleportation to other players.\n" +
                            "I can't translate it clearly either, as long as you understand the meaning. >_<\n" +
                            "接受请求将玩家传送至当前位置的指令。\n" +
                            "我也翻译不清楚了，你懂意思就行。>_<\n"
            );
            if (write) commandTpHereYes.set(COMMAND_TP_HERE_YES);
            COMMAND_TP_HERE_YES = commandTpHereYes.getString();

            Property commandTpHereNo = config.get(CATEGORY_COMMAND + ".TpHere", "commandTpHereNo", "tphn",
                    "This command is used to refuse teleportation to other players.\n" +
                            "I can't translate it clearly either, as long as you understand the meaning. >_<\n" +
                            "拒绝请求将玩家传送至当前位置的指令。\n" +
                            "我也翻译不清楚了，你懂意思就行。>_<\n"
            );
            if (write) commandTpHereNo.set(COMMAND_TP_HERE_NO);
            COMMAND_TP_HERE_NO = commandTpHereNo.getString();

            // 随机传送
            Property commandTpRandom = config.get(CATEGORY_COMMAND, "commandTpRandom", "tpr",
                    "The command to teleport to a random location.\n" +
                            "随机传送的指令。\n"
            );
            if (write) commandTpRandom.set(COMMAND_TP_RANDOM);
            COMMAND_TP_RANDOM = commandTpRandom.getString();

            // 传送到玩家重生点
            Property commandTpSpawn = config.get(CATEGORY_COMMAND, "commandTpSpawn", "tpsp",
                    "The command to teleport to the spawn of the player.\n" +
                            "传送到玩家重生点的指令。\n"
            );
            if (write) commandTpSpawn.set(COMMAND_TP_SPAWN);
            COMMAND_TP_SPAWN = commandTpSpawn.getString();

            // 传送到世界重生点
            Property commandTpWorldSpawn = config.get(CATEGORY_COMMAND, "commandTpWorldSpawn", "tpws",
                    "The command to teleport to the spawn of the world.\n" +
                            "传送到世界重生点的指令。\n"
            );
            if (write) commandTpWorldSpawn.set(COMMAND_TP_WORLD_SPAWN);
            COMMAND_TP_WORLD_SPAWN = commandTpWorldSpawn.getString();

            // 传送到顶部
            Property commandTpTop = config.get(CATEGORY_COMMAND, "commandTpTop", "tpt",
                    "The command to teleport to the top of current position.\n" +
                            "传送到顶部的指令。\n"
            );
            if (write) commandTpTop.set(COMMAND_TP_TOP);
            COMMAND_TP_TOP = commandTpTop.getString();

            // 传送到底部
            Property commandTpBottom = config.get(CATEGORY_COMMAND, "commandTpBottom", "tpb",
                    "The command to teleport to the bottom of current position.\n" +
                            "传送到底部的指令。\n"
            );
            if (write) commandTpBottom.set(COMMAND_TP_BOTTOM);
            COMMAND_TP_BOTTOM = commandTpBottom.getString();

            // 传送到上方
            Property commandTpUp = config.get(CATEGORY_COMMAND, "commandTpUp", "tpu",
                    "The command to teleport to the upper of current position.\n" +
                            "传送到上方的指令。\n"
            );
            if (write) commandTpUp.set(COMMAND_TP_UP);
            COMMAND_TP_UP = commandTpUp.getString();

            // 传送到下方
            Property commandTpDown = config.get(CATEGORY_COMMAND, "commandTpDown", "tpd",
                    "The command to teleport to the lower of current position.\n" +
                            "传送到下方的指令。\n"
            );
            if (write) commandTpDown.set(COMMAND_TP_DOWN);
            COMMAND_TP_DOWN = commandTpDown.getString();

            // 传送至视线尽头
            Property commandTpView = config.get(CATEGORY_COMMAND, "commandTpView", "tpv",
                    "The command to teleport to the end of the line of sight.\n" +
                            "This function is independent of the player's render distance setting.\n" +
                            "传送至视线尽头的指令。\n" +
                            "该功能与玩家设置的视距无关。\n"
            );
            if (write) commandTpView.set(COMMAND_TP_VIEW);
            COMMAND_TP_VIEW = commandTpView.getString();

            // 传送到家
            Property commandTpHome = config.get(CATEGORY_COMMAND + ".TpHome", "commandTpHome", "home",
                    "The command to teleport to the home.\n" +
                            "传送到家的指令。\n"
            );
            if (write) commandTpHome.set(COMMAND_TP_HOME);
            COMMAND_TP_HOME = commandTpHome.getString();

            // 设置家
            Property commandSetHome = config.get(CATEGORY_COMMAND + ".TpHome", "commandTpHomeSet", "sethome",
                    "The command to set the home.\n" +
                            "设置家的指令。\n"
            );
            if (write) commandSetHome.set(COMMAND_SET_HOME);
            COMMAND_SET_HOME = commandSetHome.getString();

            // 删除家
            Property commandDelHome = config.get(CATEGORY_COMMAND + ".TpHome", "commandTpHomeDel", "delhome",
                    "The command to delete the home.\n" +
                            "删除家的指令。\n"
            );
            if (write) commandDelHome.set(COMMAND_DEL_HOME);
            COMMAND_DEL_HOME = commandDelHome.getString();

            // 查询家
            Property commandGetHome = config.get(CATEGORY_COMMAND + ".TpHome", "commandTpHomeGet", "gethome",
                    "The command to get the home info.\n" +
                            "查询家的信息的指令。\n"
            );
            if (write) commandGetHome.set(COMMAND_GET_HOME);
            COMMAND_GET_HOME = commandGetHome.getString();

            // 传送到驿站
            Property commandTpStage = config.get(CATEGORY_COMMAND + ".TpStage", "commandTpStage", "stage",
                    "The command to teleport to the stage.\n" +
                            "传送到驿站的指令。\n"
            );
            if (write) commandTpStage.set(COMMAND_TP_STAGE);
            COMMAND_TP_STAGE = commandTpStage.getString();

            // 设置驿站
            Property commandSetStage = config.get(CATEGORY_COMMAND + ".TpStage", "commandTpStageSet", "setstage",
                    "The command to set the stage.\n" +
                            "设置驿站的指令。\n"
            );
            if (write) commandSetStage.set(COMMAND_SET_STAGE);
            COMMAND_SET_STAGE = commandSetStage.getString();

            // 删除驿站
            Property commandDelStage = config.get(CATEGORY_COMMAND + ".TpStage", "commandTpStageDel", "delstage",
                    "The command to delete the stage.\n" +
                            "删除驿站的指令。\n"
            );
            if (write) commandDelStage.set(COMMAND_DEL_STAGE);
            COMMAND_DEL_STAGE = commandDelStage.getString();

            // 查询驿站
            Property commandGetStage = config.get(CATEGORY_COMMAND + ".TpStage", "commandTpStageGet", "getstage",
                    "The command to get the stage info.\n" +
                            "查询驿站的信息的的指令。\n"
            );
            if (write) commandGetStage.set(COMMAND_GET_STAGE);
            COMMAND_GET_STAGE = commandGetStage.getString();

            // 传送到上次传送点
            Property commandTpBack = config.get(CATEGORY_COMMAND, "commandTpBack", "back",
                    "The command to teleport to the previous location.\n" +
                            "传送到上次传送点的指令。\n"
            );
            if (write) commandTpBack.set(COMMAND_TP_BACK);
            COMMAND_TP_BACK = commandTpBack.getString();

            // 设置虚拟权限
            Property commandVirtualOp = config.get(CATEGORY_COMMAND, "commandVirtualOp", "opv",
                    "The command to set virtual permission.\n" +
                            "设置虚拟权限的指令。\n"
            );
            if (write) commandVirtualOp.set(COMMAND_VIRTUAL_OP);
            COMMAND_VIRTUAL_OP = commandVirtualOp.getString();

        }

        // 定义简化指令
        {
            Property conciseUuid = config.get(CATEGORY_CONCISE, "conciseUuid", false,
                    "Enable or disable the concise version of the 'Get the UUID of the player' command.\n" +
                            "是否启用无前缀版本的 '获取玩家的UUID' 指令。\n"
            );
            if (write) conciseUuid.set(CONCISE_UUID);
            CONCISE_UUID = conciseUuid.getBoolean();

            Property conciseDimension = config.get(CATEGORY_CONCISE, "conciseDimension", false,
                    "Enable or disable the concise version of the 'Get the dimension ID of the current world' command.\n" +
                            "是否启用无前缀版本的 '获取当前世界的维度ID' 指令。\n"
            );
            if (write) conciseDimension.set(CONCISE_DIMENSION);
            CONCISE_DIMENSION = conciseDimension.getBoolean();

            Property conciseFeed = config.get(CATEGORY_CONCISE, "conciseFeed", false,
                    "Enable or disable the concise version of the 'Suicide or poisoning' command.\n" +
                            "是否启用无前缀版本的 '自杀或毒杀' 指令。\n"
            );
            if (write) conciseFeed.set(CONCISE_FEED);
            CONCISE_FEED = conciseFeed.getBoolean();

            Property conciseTpCoordinate = config.get(CATEGORY_CONCISE, "conciseTpCoordinate", true,
                    "Enable or disable the concise version of the 'Teleport to the specified coordinates' command.\n" +
                            "是否启用无前缀版本的 '传送到指定坐标' 指令。\n"
            );
            if (write) conciseTpCoordinate.set(CONCISE_TP_COORDINATE);
            CONCISE_TP_COORDINATE = conciseTpCoordinate.getBoolean();

            Property conciseTpStructure = config.get(CATEGORY_CONCISE, "conciseTpStructure", true,
                    "Enable or disable the concise version of the 'Teleport to the specified structure' command.\n" +
                            "是否启用无前缀版本的 '传送到指定结构' 指令。\n"
            );
            if (write) conciseTpStructure.set(CONCISE_TP_STRUCTURE);
            CONCISE_TP_STRUCTURE = conciseTpStructure.getBoolean();

            Property conciseTpAsk = config.get(CATEGORY_CONCISE + ".TpAsk", "conciseTpAsk", true,
                    "Enable or disable the concise version of the 'Request to teleport oneself to other players' command.\n" +
                            "是否启用无前缀版本的 '请求传送至玩家' 指令。\n"
            );
            if (write) conciseTpAsk.set(CONCISE_TP_ASK);
            CONCISE_TP_ASK = conciseTpAsk.getBoolean();

            Property conciseTpAskYes = config.get(CATEGORY_CONCISE + ".TpAsk", "conciseTpAskYes", true,
                    "Enable or disable the concise version of the 'Accept teleportation of other players to oneself' command.\n" +
                            "是否启用无前缀版本的 '接受请求传送至玩家' 指令。\n"
            );
            if (write) conciseTpAskYes.set(CONCISE_TP_ASK_YES);
            CONCISE_TP_ASK_YES = conciseTpAskYes.getBoolean();

            Property conciseTpAskNo = config.get(CATEGORY_CONCISE + ".TpAsk", "conciseTpAskNo", false,
                    "Enable or disable the concise version of the 'Refuse teleportation of other players to oneself' command.\n" +
                            "是否启用无前缀版本的 '拒绝请求传送至玩家' 指令。\n"
            );
            if (write) conciseTpAskNo.set(CONCISE_TP_ASK_NO);
            CONCISE_TP_ASK_NO = conciseTpAskNo.getBoolean();

            Property conciseTpHere = config.get(CATEGORY_CONCISE + ".TpHere", "conciseTpHere", true,
                    "Enable or disable the concise version of the 'Request the transfer of other players to oneself' command.\n" +
                            "是否启用无前缀版本的 '请求将玩家传送至当前位置' 指令。\n"
            );
            if (write) conciseTpHere.set(CONCISE_TP_HERE);
            CONCISE_TP_HERE = conciseTpHere.getBoolean();

            Property conciseTpHereYes = config.get(CATEGORY_CONCISE + ".TpHere", "conciseTpHereYes", true,
                    "Enable or disable the concise version of the 'Accept teleportation to other players' command.\n" +
                            "是否启用无前缀版本的 '接受请求将玩家传送至当前位置' 指令。\n"
            );
            if (write) conciseTpHereYes.set(CONCISE_TP_HERE_YES);
            CONCISE_TP_HERE_YES = conciseTpHereYes.getBoolean();

            Property conciseTpHereNo = config.get(CATEGORY_CONCISE + ".TpHere", "conciseTpHereNo", false,
                    "Enable or disable the concise version of the 'Refuse teleportation to other players' command.\n" +
                            "是否启用无前缀版本的 '拒绝请求将玩家传送至当前位置' 指令。\n"
            );
            if (write) conciseTpHereNo.set(CONCISE_TP_HERE_NO);
            CONCISE_TP_HERE_NO = conciseTpHereNo.getBoolean();

            Property conciseTpRandom = config.get(CATEGORY_CONCISE, "conciseTpRandom", false,
                    "Enable or disable the concise version of the 'Teleport to a random location' command.\n" +
                            "是否启用无前缀版本的 '随机传送' 指令。\n"
            );
            if (write) conciseTpRandom.set(CONCISE_TP_RANDOM);
            CONCISE_TP_RANDOM = conciseTpRandom.getBoolean();

            Property conciseTpSpawn = config.get(CATEGORY_CONCISE, "conciseTpSpawn", true,
                    "Enable or disable the concise version of the 'Teleport to the spawn of the player' command.\n" +
                            "是否启用无前缀版本的 '传送到玩家重生点' 指令。\n"
            );
            if (write) conciseTpSpawn.set(CONCISE_TP_SPAWN);
            CONCISE_TP_SPAWN = conciseTpSpawn.getBoolean();

            Property conciseTpWorldSpawn = config.get(CATEGORY_CONCISE, "conciseTpWorldSpawn", false,
                    "Enable or disable the concise version of the 'Teleport to the spawn of the world' command.\n" +
                            "是否启用无前缀版本的 '传送到世界重生点' 指令。\n"
            );
            if (write) conciseTpWorldSpawn.set(CONCISE_TP_WORLD_SPAWN);
            CONCISE_TP_WORLD_SPAWN = conciseTpWorldSpawn.getBoolean();

            Property conciseTpTop = config.get(CATEGORY_CONCISE, "conciseTpTop", false,
                    "Enable or disable the concise version of the 'Teleport to the top of current position' command.\n" +
                            "是否启用无前缀版本的 '传送到顶部' 指令。\n"
            );
            if (write) conciseTpTop.set(CONCISE_TP_TOP);
            CONCISE_TP_TOP = conciseTpTop.getBoolean();

            Property conciseTpBottom = config.get(CATEGORY_CONCISE, "conciseTpBottom", false,
                    "Enable or disable the concise version of the 'Teleport to the bottom of current position' command.\n" +
                            "是否启用无前缀版本的 '传送到底部' 指令。\n"
            );
            if (write) conciseTpBottom.set(CONCISE_TP_BOTTOM);
            CONCISE_TP_BOTTOM = conciseTpBottom.getBoolean();

            Property conciseTpUp = config.get(CATEGORY_CONCISE, "conciseTpUp", false,
                    "Enable or disable the concise version of the 'Teleport to the upper of current position' command.\n" +
                            "是否启用无前缀版本的 '传送到上方' 指令。\n"
            );
            if (write) conciseTpUp.set(CONCISE_TP_UP);
            CONCISE_TP_UP = conciseTpUp.getBoolean();

            Property conciseTpDown = config.get(CATEGORY_CONCISE, "conciseTpDown", false,
                    "Enable or disable the concise version of the 'Teleport to the lower of current position' command.\n" +
                            "是否启用无前缀版本的 '传送到下方' 指令。\n"
            );
            if (write) conciseTpDown.set(CONCISE_TP_DOWN);
            CONCISE_TP_DOWN = conciseTpDown.getBoolean();

            Property conciseTpView = config.get(CATEGORY_CONCISE, "conciseTpView", false,
                    "Enable or disable the concise version of the 'Teleport to the end of the line of sight' command.\n" +
                            "This function is independent of the player's render distance setting.\n" +
                            "是否启用无前缀版本的 '传送至视线尽头' 指令。\n" +
                            "该功能与玩家设置的视距无关。\n"
            );
            if (write) conciseTpView.set(CONCISE_TP_VIEW);
            CONCISE_TP_VIEW = conciseTpView.getBoolean();

            Property conciseTpHome = config.get(CATEGORY_CONCISE + ".TpHome", "conciseTpHome", true,
                    "Enable or disable the concise version of the 'Teleport to the home' command.\n" +
                            "是否启用无前缀版本的 '传送到家' 指令。\n"
            );
            if (write) conciseTpHome.set(CONCISE_TP_HOME);
            CONCISE_TP_HOME = conciseTpHome.getBoolean();

            Property conciseSetHome = config.get(CATEGORY_CONCISE + ".TpHome", "conciseTpHomeSet", false,
                    "Enable or disable the concise version of the 'Set the home' command.\n" +
                            "是否启用无前缀版本的 '设置家' 指令。\n"
            );
            if (write) conciseSetHome.set(CONCISE_SET_HOME);
            CONCISE_SET_HOME = conciseSetHome.getBoolean();

            Property conciseDelHome = config.get(CATEGORY_CONCISE + ".TpHome", "conciseTpHomeDel", false,
                    "Enable or disable the concise version of the 'Delete the home' command.\n" +
                            "是否启用无前缀版本的 '删除家' 指令。\n"
            );
            if (write) conciseDelHome.set(CONCISE_DEL_HOME);
            CONCISE_DEL_HOME = conciseDelHome.getBoolean();

            Property conciseGetHome = config.get(CATEGORY_CONCISE + ".TpHome", "conciseTpHomeGet", false,
                    "Enable or disable the concise version of the 'Get the home info' command.\n" +
                            "是否启用无前缀版本的 '查询家' 指令。\n"
            );
            if (write) conciseGetHome.set(CONCISE_GET_HOME);
            CONCISE_GET_HOME = conciseGetHome.getBoolean();

            Property conciseTpStage = config.get(CATEGORY_CONCISE + ".TpStage", "conciseTpStage", true,
                    "Enable or disable the concise version of the 'Teleport to the stage' command.\n" +
                            "是否启用无前缀版本的 '传送到驿站' 指令。\n"
            );
            if (write) conciseTpStage.set(CONCISE_TP_STAGE);
            CONCISE_TP_STAGE = conciseTpStage.getBoolean();

            Property conciseSetStage = config.get(CATEGORY_CONCISE + ".TpStage", "conciseTpStageSet", false,
                    "Enable or disable the concise version of the 'Set the stage' command.\n" +
                            "是否启用无前缀版本的 '设置驿站' 指令。\n"
            );
            if (write) conciseSetStage.set(CONCISE_SET_STAGE);
            CONCISE_SET_STAGE = conciseSetStage.getBoolean();

            Property conciseDelStage = config.get(CATEGORY_CONCISE + ".TpStage", "conciseTpStageDel", false,
                    "Enable or disable the concise version of the 'Delete the stage' command.\n" +
                            "是否启用无前缀版本的 '删除驿站' 指令。\n"
            );
            if (write) conciseDelStage.set(CONCISE_DEL_STAGE);
            CONCISE_DEL_STAGE = conciseDelStage.getBoolean();

            Property conciseGetStage = config.get(CATEGORY_CONCISE + ".TpStage", "conciseTpStageGet", false,
                    "Enable or disable the concise version of the 'Get the stage info' command.\n" +
                            "是否启用无前缀版本的 '查询驿站' 指令。\n"
            );
            if (write) conciseGetStage.set(CONCISE_GET_STAGE);
            CONCISE_GET_STAGE = conciseGetStage.getBoolean();

            Property conciseTpBack = config.get(CATEGORY_CONCISE, "conciseTpBack", true,
                    "Enable or disable the concise version of the 'Teleport to the previous location' command.\n" +
                            "是否启用无前缀版本的 '传送到上次传送点' 指令。\n"
            );
            if (write) conciseTpBack.set(CONCISE_TP_BACK);
            CONCISE_TP_BACK = conciseTpBack.getBoolean();

            Property conciseVirtualOp = config.get(CATEGORY_CONCISE, "conciseVirtualOp", false,
                    "Enable or disable the concise version of the 'Set virtual permission' command.\n" +
                            "是否启用无前缀版本的 '设置虚拟权限' 指令。\n"
            );
            if (write) conciseVirtualOp.set(CONCISE_VIRTUAL_OP);
            CONCISE_VIRTUAL_OP = conciseVirtualOp.getBoolean();

        }

        // 定义传送代价
        {
            {
                Property costTpCoordinateType = config.get(CATEGORY_COST + ".TpCoordinate", "costTpCoordinateType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Teleport to the specified coordinates'.\n" +
                                "传送到指定坐标的代价类型。\n"
                        , Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpCoordinateType.set(COST_TP_COORDINATE_TYPE.name());
                COST_TP_COORDINATE_TYPE = ECostType.valueOf(costTpCoordinateType.getString());

                Property costTpCoordinateNum = config.get(CATEGORY_COST + ".TpCoordinate", "costTpCoordinateNum", 1,
                        "The number of cost for 'Teleport to the specified coordinates'.\n" +
                                "传送到指定坐标的代价数量。\n"
                );
                costTpCoordinateNum.setMinValue(0).setMaxValue(9999);
                costTpCoordinateNum.comment = (costTpCoordinateNum.comment + " [range: " + costTpCoordinateNum.getMinValue() + " ~ " + costTpCoordinateNum.getMaxValue() + ", default: " + costTpCoordinateNum.getDefault() + "]");
                if (write) costTpCoordinateNum.set(COST_TP_COORDINATE_NUM);
                COST_TP_COORDINATE_NUM = costTpCoordinateNum.getInt();

                Property costTpCoordinateConf = config.get(CATEGORY_COST + ".TpCoordinate", "costTpCoordinateConf", "",
                        "The configuration for 'Teleport to the specified coordinates'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "传送到指定坐标的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n"
                );
                if (write) costTpCoordinateConf.set(COST_TP_COORDINATE_CONF);
                COST_TP_COORDINATE_CONF = costTpCoordinateConf.getString();

                Property costTpCoordinateRate = config.get(CATEGORY_COST + ".TpCoordinate", "costTpCoordinateRate", 0.001d,
                        "The cost rate for 'Teleport to the specified coordinates', the cost will be multiplied by the distance between the two coordinates.\n" +
                                "传送到指定坐标的代价倍率，代价会乘以传送前后坐标之间的距离。\n"
                );
                costTpCoordinateRate.setMinValue(0).setMaxValue(9999);
                costTpCoordinateRate.comment = (costTpCoordinateRate.comment + " [range: " + costTpCoordinateRate.getMinValue() + " ~ " + costTpCoordinateRate.getMaxValue() + ", default: " + costTpCoordinateRate.getDefault() + "]");
                if (write) costTpCoordinateRate.set(COST_TP_COORDINATE_RATE);
                COST_TP_COORDINATE_RATE = costTpCoordinateRate.getDouble();
            }

            {
                Property costTpStructureType = config.get(CATEGORY_COST + ".TpStructure", "costTpStructureType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Teleport to the specified structure'.\n" +
                                "传送到指定结构的代价类型。\n"
                        , Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpStructureType.set(COST_TP_STRUCTURE_TYPE.name());
                COST_TP_STRUCTURE_TYPE = ECostType.valueOf(costTpStructureType.getString());

                Property costTpStructureNum = config.get(CATEGORY_COST + ".TpStructure", "costTpStructureNum", 1,
                        "The number of cost for 'Teleport to the specified structure'.\n" +
                                "传送到指定结构的代价数量。\n"
                );
                costTpStructureNum.setMinValue(0).setMaxValue(9999);
                costTpStructureNum.comment = (costTpStructureNum.comment + " [range: " + costTpStructureNum.getMinValue() + " ~ " + costTpStructureNum.getMaxValue() + ", default: " + costTpStructureNum.getDefault() + "]");
                if (write) costTpStructureNum.set(COST_TP_STRUCTURE_NUM);
                COST_TP_STRUCTURE_NUM = costTpStructureNum.getInt();

                Property costTpStructureConf = config.get(CATEGORY_COST + ".TpStructure", "costTpStructureConf", "",
                        "The configuration for 'Teleport to the specified structure'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "传送到指定结构的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n"
                );
                if (write) costTpStructureConf.set(COST_TP_STRUCTURE_CONF);
                COST_TP_STRUCTURE_CONF = costTpStructureConf.getString();

                Property costTpStructureRate = config.get(CATEGORY_COST + ".TpStructure", "costTpStructureRate", 0.001d,
                        "The cost rate for 'Teleport to the specified structure', the cost will be multiplied by the distance between the two players.\n" +
                                "传送到指定结构的代价倍率，代价会乘以两个玩家之间的距离。\n"
                );
                costTpStructureRate.setMinValue(0).setMaxValue(9999);
                costTpStructureRate.comment = (costTpStructureRate.comment + " [range: " + costTpStructureRate.getMinValue() + " ~ " + costTpStructureRate.getMaxValue() + ", default: " + costTpStructureRate.getDefault() + "]");
                if (write) costTpStructureRate.set(COST_TP_STRUCTURE_RATE);
                COST_TP_STRUCTURE_RATE = costTpStructureRate.getDouble();
            }

            {
                Property costTpAskType = config.get(CATEGORY_COST + ".TpAsk", "costTpAskType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Request to teleport oneself to other players'.\n" +
                                "请求传送至玩家的代价类型。\n"
                        , Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpAskType.set(COST_TP_ASK_TYPE.name());
                COST_TP_ASK_TYPE = ECostType.valueOf(costTpAskType.getString());

                Property costTpAskNum = config.get(CATEGORY_COST + ".TpAsk", "costTpAskNum", 1,
                        "The number of cost for 'Request to teleport oneself to other players'.\n" +
                                "请求传送至玩家的代价数量。\n"
                );
                costTpAskNum.setMinValue(0).setMaxValue(9999);
                costTpAskNum.comment = (costTpAskNum.comment + " [range: " + costTpAskNum.getMinValue() + " ~ " + costTpAskNum.getMaxValue() + ", default: " + costTpAskNum.getDefault() + "]");
                if (write) costTpAskNum.set(COST_TP_ASK_NUM);
                COST_TP_ASK_NUM = costTpAskNum.getInt();

                Property costTpAskConf = config.get(CATEGORY_COST + ".TpAsk", "costTpAskConf", "",
                        "The configuration for 'Request to teleport oneself to other players'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "请求传送至玩家的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n"
                );
                if (write) costTpAskConf.set(COST_TP_ASK_CONF);
                COST_TP_ASK_CONF = costTpAskConf.getString();

                Property costTpAskRate = config.get(CATEGORY_COST + ".TpAsk", "costTpAskRate", 0.001d,
                        "The cost rate for 'Request to teleport oneself to other players', the cost will be multiplied by the distance between the two players.\n" +
                                "请求传送至玩家的代价倍率，代价会乘以两个玩家之间的距离。\n"
                );
                costTpAskRate.setMinValue(0).setMaxValue(9999);
                costTpAskRate.comment = (costTpAskRate.comment + " [range: " + costTpAskRate.getMinValue() + " ~ " + costTpAskRate.getMaxValue() + ", default: " + costTpAskRate.getDefault() + "]");
                if (write) costTpAskRate.set(COST_TP_ASK_RATE);
                COST_TP_ASK_RATE = costTpAskRate.getDouble();
            }

            {
                Property costTpHereType = config.get(CATEGORY_COST + ".TpHere", "costTpHereType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Request the transfer of other players to oneself'.\n" +
                                "请求将玩家传送至当前位置的代价类型。\n"
                        , Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpHereType.set(COST_TP_HERE_TYPE.name());
                COST_TP_HERE_TYPE = ECostType.valueOf(costTpHereType.getString());

                Property costTpHereNum = config.get(CATEGORY_COST + ".TpHere", "costTpHereNum", 1,
                        "The number of cost for 'Request the transfer of other players to oneself'.\n" +
                                "请求将玩家传送至当前位置的代价数量。\n"
                );
                costTpHereNum.setMinValue(0).setMaxValue(9999);
                costTpHereNum.comment = (costTpHereNum.comment + " [range: " + costTpHereNum.getMinValue() + " ~ " + costTpHereNum.getMaxValue() + ", default: " + costTpHereNum.getDefault() + "]");
                if (write) costTpHereNum.set(COST_TP_HERE_NUM);
                COST_TP_HERE_NUM = costTpHereNum.getInt();

                Property costTpHereConf = config.get(CATEGORY_COST + ".TpHere", "costTpHereConf", "",
                        "The configuration for 'Request the transfer of other players to oneself'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "请求将玩家传送至当前位置的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n"
                );
                if (write) costTpHereConf.set(COST_TP_HERE_CONF);
                COST_TP_HERE_CONF = costTpHereConf.getString();

                Property costTpHereRate = config.get(CATEGORY_COST + ".TpHere", "costTpHereRate", 0.001d,
                        "The cost rate for 'Request the transfer of other players to oneself', the cost will be multiplied by the distance between the two coordinates.\n" +
                                "请求将玩家传送至当前位置的代价倍率，代价会乘以传送前后坐标之间的距离。\n"
                );
                costTpHereRate.setMinValue(0).setMaxValue(9999);
                costTpHereRate.comment = (costTpHereRate.comment + " [range: " + costTpHereRate.getMinValue() + " ~ " + costTpHereRate.getMaxValue() + ", default: " + costTpHereRate.getDefault() + "]");
                if (write) costTpHereRate.set(COST_TP_HERE_RATE);
                COST_TP_HERE_RATE = costTpHereRate.getDouble();
            }

            {
                Property costTpRandomType = config.get(CATEGORY_COST + ".TpRandom", "costTpRandomType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Teleport to a random location'.\n" +
                                "随机传送的代价类型。\n"
                        , Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpRandomType.set(COST_TP_RANDOM_TYPE.name());
                COST_TP_RANDOM_TYPE = ECostType.valueOf(costTpRandomType.getString());

                Property costTpRandomNum = config.get(CATEGORY_COST + ".TpRandom", "costTpRandomNum", 1,
                        "The number of cost for 'Teleport to a random location'.\n" +
                                "随机传送的代价数量。\n"
                );
                costTpRandomNum.setMinValue(0).setMaxValue(9999);
                costTpRandomNum.comment = (costTpRandomNum.comment + " [range: " + costTpRandomNum.getMinValue() + " ~ " + costTpRandomNum.getMaxValue() + ", default: " + costTpRandomNum.getDefault() + "]");
                if (write) costTpRandomNum.set(COST_TP_RANDOM_NUM);
                COST_TP_RANDOM_NUM = costTpRandomNum.getInt();

                Property costTpRandomConf = config.get(CATEGORY_COST + ".TpRandom", "costTpRandomConf", "",
                        "The configuration for 'Teleport to a random location'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "随机传送的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n"
                );
                if (write) costTpRandomConf.set(COST_TP_RANDOM_CONF);
                COST_TP_RANDOM_CONF = costTpRandomConf.getString();

                Property costTpRandomRate = config.get(CATEGORY_COST + ".TpRandom", "costTpRandomRate", 0.001d,
                        "The cost rate for 'Teleport to a random location', the cost will be multiplied by the distance between the two coordinates.\n" +
                                "随机传送的代价倍率，代价会乘以传送前后坐标之间的距离。\n"
                );
                costTpRandomRate.setMinValue(0).setMaxValue(9999);
                costTpRandomRate.comment = (costTpRandomRate.comment + " [range: " + costTpRandomRate.getMinValue() + " ~ " + costTpRandomRate.getMaxValue() + ", default: " + costTpRandomRate.getDefault() + "]");
                if (write) costTpRandomRate.set(COST_TP_RANDOM_RATE);
                COST_TP_RANDOM_RATE = costTpRandomRate.getDouble();
            }

            {
                Property costTpSpawnType = config.get(CATEGORY_COST + ".TpSpawn", "costTpSpawnType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Teleport to the spawn of the player'.\n" +
                                "传送到玩家重生点的代价类型。\n"
                        , Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpSpawnType.set(COST_TP_SPAWN_TYPE.name());
                COST_TP_SPAWN_TYPE = ECostType.valueOf(costTpSpawnType.getString());

                Property costTpSpawnNum = config.get(CATEGORY_COST + ".TpSpawn", "costTpSpawnNum", 1,
                        "The number of cost for 'Teleport to the spawn of the player'.\n" +
                                "传送到玩家重生点的代价数量。\n"
                );
                costTpSpawnNum.setMinValue(0).setMaxValue(9999);
                costTpSpawnNum.comment = (costTpSpawnNum.comment + " [range: " + costTpSpawnNum.getMinValue() + " ~ " + costTpSpawnNum.getMaxValue() + ", default: " + costTpSpawnNum.getDefault() + "]");
                if (write) costTpSpawnNum.set(COST_TP_SPAWN_NUM);
                COST_TP_SPAWN_NUM = costTpSpawnNum.getInt();

                Property costTpSpawnConf = config.get(CATEGORY_COST + ".TpSpawn", "costTpSpawnConf", "",
                        "The configuration for 'Teleport to the spawn of the player'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "传送到玩家重生点的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n"
                );
                if (write) costTpSpawnConf.set(COST_TP_SPAWN_CONF);
                COST_TP_SPAWN_CONF = costTpSpawnConf.getString();

                Property costTpSpawnRate = config.get(CATEGORY_COST + ".TpSpawn", "costTpSpawnRate", 0.001d,
                        "The cost rate for 'Teleport to the spawn of the player', the cost will be multiplied by the distance between the two coordinates.\n" +
                                "传送到玩家重生点的代价倍率，代价会乘以传送前后坐标之间的距离。\n"
                );
                costTpSpawnRate.setMinValue(0).setMaxValue(9999);
                costTpSpawnRate.comment = (costTpSpawnRate.comment + " [range: " + costTpSpawnRate.getMinValue() + " ~ " + costTpSpawnRate.getMaxValue() + ", default: " + costTpSpawnRate.getDefault() + "]");
                if (write) costTpSpawnRate.set(COST_TP_SPAWN_RATE);
                COST_TP_SPAWN_RATE = costTpSpawnRate.getDouble();
            }

            {
                Property costTpWorldSpawnType = config.get(CATEGORY_COST + ".TpWorldSpawn", "costTpWorldSpawnType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Teleport to the spawn of the world'.\n" +
                                "传送到世界重生点的代价类型。\n"
                        , Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpWorldSpawnType.set(COST_TP_WORLD_SPAWN_TYPE.name());
                COST_TP_WORLD_SPAWN_TYPE = ECostType.valueOf(costTpWorldSpawnType.getString());

                Property costTpWorldSpawnNum = config.get(CATEGORY_COST + ".TpWorldSpawn", "costTpWorldSpawnNum", 1,
                        "The number of cost for 'Teleport to the spawn of the world'.\n" +
                                "传送到世界重生点的代价数量。\n"
                );
                costTpWorldSpawnNum.setMinValue(0).setMaxValue(9999);
                if (write) costTpWorldSpawnNum.set(COST_TP_WORLD_SPAWN_NUM);
                COST_TP_WORLD_SPAWN_NUM = costTpWorldSpawnNum.getInt();

                Property costTpWorldSpawnConf = config.get(CATEGORY_COST + ".TpWorldSpawn", "costTpWorldSpawnConf", "",
                        "The configuration for 'Teleport to the spawn of the world'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "传送到世界重生点的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n"
                );
                if (write) costTpWorldSpawnConf.set(COST_TP_WORLD_SPAWN_CONF);
                COST_TP_WORLD_SPAWN_CONF = costTpWorldSpawnConf.getString();

                Property costTpWorldSpawnRate = config.get(CATEGORY_COST + ".TpWorldSpawn", "costTpWorldSpawnRate", 0.001d,
                        "The cost rate for 'Teleport to the spawn of the world', the cost will be multiplied by the distance between the two coordinates.\n" +
                                "传送到世界重生点的代价倍率，代价会乘以传送前后坐标之间的距离。\n"
                );
                costTpWorldSpawnRate.setMinValue(0).setMaxValue(9999);
                if (write) costTpWorldSpawnRate.set(COST_TP_WORLD_SPAWN_RATE);
                COST_TP_WORLD_SPAWN_RATE = costTpWorldSpawnRate.getDouble();
            }

            {
                Property costTpTopType = config.get(CATEGORY_COST + ".TpTop", "costTpTopType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Teleport to the top of current position'.\n" +
                                "传送到顶部的代价类型。\n"
                        , Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpTopType.set(COST_TP_TOP_TYPE.name());
                COST_TP_TOP_TYPE = ECostType.valueOf(costTpTopType.getString());

                Property costTpTopNum = config.get(CATEGORY_COST + ".TpTop", "costTpTopNum", 1,
                        "The number of cost for 'Teleport to the top of current position'.\n" +
                                "传送到顶部的代价数量。\n"
                );
                costTpTopNum.setMinValue(0).setMaxValue(9999);
                if (write) costTpTopNum.set(COST_TP_TOP_NUM);
                COST_TP_TOP_NUM = costTpTopNum.getInt();

                Property costTpTopConf = config.get(CATEGORY_COST + ".TpTop", "costTpTopConf", "",
                        "The configuration for 'Teleport to the top of current position'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "传送到顶部的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n"
                );
                if (write) costTpTopConf.set(COST_TP_TOP_CONF);
                COST_TP_TOP_CONF = costTpTopConf.getString();

                Property costTpTopRate = config.get(CATEGORY_COST + ".TpTop", "costTpTopRate", 0.001d,
                        "The cost rate for 'Teleport to the top of current position', the cost will be multiplied by the distance between the two coordinates.\n" +
                                "传送到顶部的代价倍率，代价会乘以传送前后坐标之间的距离。\n"
                );
                costTpTopRate.setMinValue(0).setMaxValue(9999);
                if (write) costTpTopRate.set(COST_TP_TOP_RATE);
                COST_TP_TOP_RATE = costTpTopRate.getDouble();
            }

            {
                Property costTpBottomType = config.get(CATEGORY_COST + ".TpBottom", "costTpBottomType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Teleport to the bottom of current position'.\n" +
                                "传送到底部的代价类型。\n"
                        , Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpBottomType.set(COST_TP_BOTTOM_TYPE.name());
                COST_TP_BOTTOM_TYPE = ECostType.valueOf(costTpBottomType.getString());

                Property costTpBottomNum = config.get(CATEGORY_COST + ".TpBottom", "costTpBottomNum", 1,
                        "The number of cost for 'Teleport to the bottom of current position'.\n" +
                                "传送到底部的代价数量。\n"
                );
                costTpBottomNum.setMinValue(0).setMaxValue(9999);
                if (write) costTpBottomNum.set(COST_TP_BOTTOM_NUM);
                COST_TP_BOTTOM_NUM = costTpBottomNum.getInt();

                Property costTpBottomConf = config.get(CATEGORY_COST + ".TpBottom", "costTpBottomConf", "",
                        "The configuration for 'Teleport to the bottom of current position'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "传送到底部的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n"
                );
                if (write) costTpBottomConf.set(COST_TP_BOTTOM_CONF);
                COST_TP_BOTTOM_CONF = costTpBottomConf.getString();

                Property costTpBottomRate = config.get(CATEGORY_COST + ".TpBottom", "costTpBottomRate", 0.001d,
                        "The cost rate for 'Teleport to the bottom of current position', the cost will be multiplied by the distance between the two coordinates.\n" +
                                "传送到底部的代价倍率，代价会乘以传送前后坐标之间的距离。\n"
                );
                costTpBottomRate.setMinValue(0).setMaxValue(9999);
                if (write) costTpBottomRate.set(COST_TP_BOTTOM_RATE);
                COST_TP_BOTTOM_RATE = costTpBottomRate.getDouble();
            }

            {
                Property costTpUpType = config.get(CATEGORY_COST + ".TpUp", "costTpUpType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Teleport to the upper of current position'.\n" +
                                "传送到上方的代价类型。\n"
                        , Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpUpType.set(COST_TP_UP_TYPE.name());
                COST_TP_UP_TYPE = ECostType.valueOf(costTpUpType.getString());

                Property costTpUpNum = config.get(CATEGORY_COST + ".TpUp", "costTpUpNum", 1,
                        "The number of cost for 'Teleport to the upper of current position'.\n" +
                                "传送到上方的代价数量。\n"
                );
                costTpUpNum.setMinValue(0).setMaxValue(9999);
                if (write) costTpUpNum.set(COST_TP_UP_NUM);
                COST_TP_UP_NUM = costTpUpNum.getInt();

                Property costTpUpConf = config.get(CATEGORY_COST + ".TpUp", "costTpUpConf", "",
                        "The configuration for 'Teleport to the upper of current position'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "传送到上方的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n"
                );
                if (write) costTpUpConf.set(COST_TP_UP_CONF);
                COST_TP_UP_CONF = costTpUpConf.getString();

                Property costTpUpRate = config.get(CATEGORY_COST + ".TpUp", "costTpUpRate", 0.001d,
                        "The cost rate for 'Teleport to the upper of current position', the cost will be multiplied by the distance between the two coordinates.\n" +
                                "传送到上方的代价倍率，代价会乘以传送前后坐标之间的距离。\n"
                );
                costTpUpRate.setMinValue(0).setMaxValue(9999);
                if (write) costTpUpRate.set(COST_TP_UP_RATE);
                COST_TP_UP_RATE = costTpUpRate.getDouble();
            }

            {
                Property costTpDownType = config.get(CATEGORY_COST + ".TpDown", "costTpDownType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Teleport to the lower of current position'.\n" +
                                "传送到下方的代价类型。\n"
                        , Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpDownType.set(COST_TP_DOWN_TYPE.name());
                COST_TP_DOWN_TYPE = ECostType.valueOf(costTpDownType.getString());

                Property costTpDownNum = config.get(CATEGORY_COST + ".TpDown", "costTpDownNum", 1,
                        "The number of cost for 'Teleport to the lower of current position'.\n" +
                                "传送到下方的代价数量。\n"
                );
                costTpDownNum.setMinValue(0).setMaxValue(9999);
                if (write) costTpDownNum.set(COST_TP_DOWN_NUM);
                COST_TP_DOWN_NUM = costTpDownNum.getInt();

                Property costTpDownConf = config.get(CATEGORY_COST + ".TpDown", "costTpDownConf", "",
                        "The configuration for 'Teleport to the lower of current position'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "传送到下方的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n"
                );
                if (write) costTpDownConf.set(COST_TP_DOWN_CONF);
                COST_TP_DOWN_CONF = costTpDownConf.getString();

                Property costTpDownRate = config.get(CATEGORY_COST + ".TpDown", "costTpDownRate", 0.001d,
                        "The cost rate for 'Teleport to the lower of current position', the cost will be multiplied by the distance between the two coordinates.\n" +
                                "传送到下方的代价倍率，代价会乘以传送前后坐标之间的距离。\n"
                );
                costTpDownRate.setMinValue(0).setMaxValue(9999);
                if (write) costTpDownRate.set(COST_TP_DOWN_RATE);
                COST_TP_DOWN_RATE = costTpDownRate.getDouble();
            }

            {
                Property costTpViewType = config.get(CATEGORY_COST + ".TpView", "costTpViewType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Teleport to the end of the line of sight'.\n" +
                                "传送至视线尽头的代价类型。\n"
                        , Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpViewType.set(COST_TP_VIEW_TYPE.name());
                COST_TP_VIEW_TYPE = ECostType.valueOf(costTpViewType.getString());

                Property costTpViewNum = config.get(CATEGORY_COST + ".TpView", "costTpViewNum", 1,
                        "The number of cost for 'Teleport to the end of the line of sight'.\n" +
                                "传送至视线尽头的代价数量。\n"
                );
                costTpViewNum.setMinValue(0).setMaxValue(9999);
                if (write) costTpViewNum.set(COST_TP_VIEW_NUM);
                COST_TP_VIEW_NUM = costTpViewNum.getInt();

                Property costTpViewConf = config.get(CATEGORY_COST + ".TpView", "costTpViewConf", "",
                        "The configuration for 'Teleport to the end of the line of sight'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "传送至视线尽头的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n"
                );
                if (write) costTpViewConf.set(COST_TP_VIEW_CONF);
                COST_TP_VIEW_CONF = costTpViewConf.getString();

                Property costTpViewRate = config.get(CATEGORY_COST + ".TpView", "costTpViewRate", 0.001d,
                        "The cost rate for 'Teleport to the end of the line of sight', the cost will be multiplied by the distance between the two coordinates.\n" +
                                "传送至视线尽头的代价倍率，代价会乘以传送前后坐标之间的距离。\n"
                );
                costTpViewRate.setMinValue(0).setMaxValue(9999);
                if (write) costTpViewRate.set(COST_TP_VIEW_RATE);
                COST_TP_VIEW_RATE = costTpViewRate.getDouble();
            }

            {
                Property costTpHomeType = config.get(CATEGORY_COST + ".TpHome", "costTpHomeType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Teleport to the home'.\n" +
                                "传送到家的代价类型。\n",
                        Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpHomeType.set(COST_TP_HOME_TYPE.name());
                COST_TP_HOME_TYPE = ECostType.valueOf(costTpHomeType.getString());

                Property costTpHomeNum = config.get(CATEGORY_COST + ".TpHome", "costTpHomeNum", 1,
                        "The number of cost for 'Teleport to the home'.\n" +
                                "传送到家的代价数量。\n");
                costTpHomeNum.setMinValue(0).setMaxValue(9999);
                if (write) costTpHomeNum.set(COST_TP_HOME_NUM);
                COST_TP_HOME_NUM = costTpHomeNum.getInt();

                Property costTpHomeConf = config.get(CATEGORY_COST + ".TpHome", "costTpHomeConf", "",
                        "The configuration for 'Teleport to the home'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "传送到家的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n");
                if (write) costTpHomeConf.set(COST_TP_HOME_CONF);
                COST_TP_HOME_CONF = costTpHomeConf.getString();

                Property costTpHomeRate = config.get(CATEGORY_COST + ".TpHome", "costTpHomeRate", 0.001d,
                        "The cost rate for 'Teleport to the home', the cost will be multiplied by the distance between the two coordinates.\n" +
                                "传送到家的代价倍率，代价会乘以传送前后坐标之间的距离。\n");
                costTpHomeRate.setMinValue(0).setMaxValue(9999);
                if (write) costTpHomeRate.set(COST_TP_HOME_RATE);
                COST_TP_HOME_RATE = costTpHomeRate.getDouble();
            }

            {
                Property costTpStageType = config.get(CATEGORY_COST + ".TpStage", "costTpStageType", ECostType.EXP_POINT.name(),
                        "The cost type for 'Teleport to the stage'.\n" +
                                "传送到驿站的代价类型。\n",
                        Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpStageType.set(COST_TP_STAGE_TYPE.name());
                COST_TP_STAGE_TYPE = ECostType.valueOf(costTpStageType.getString());

                Property costTpStageNum = config.get(CATEGORY_COST + ".TpStage", "costTpStageNum", 1,
                        "The number of cost for 'Teleport to the stage'.\n" +
                                "传送到驿站的代价数量。\n");
                costTpStageNum.setMinValue(0).setMaxValue(9999);
                if (write) costTpStageNum.set(COST_TP_STAGE_NUM);
                COST_TP_STAGE_NUM = costTpStageNum.getInt();

                Property costTpStageConf = config.get(CATEGORY_COST + ".TpStage", "costTpStageConf", "",
                        "The configuration for 'Teleport to the stage'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "传送到驿站的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n");
                if (write) costTpStageConf.set(COST_TP_STAGE_CONF);
                COST_TP_STAGE_CONF = costTpStageConf.getString();

                Property costTpStageRate = config.get(CATEGORY_COST + ".TpStage", "costTpStageRate", 0.001d,
                        "The cost rate for 'Teleport to the stage', the cost will be multiplied by the distance between the two coordinates.\n" +
                                "传送到驿站的代价倍率，代价会乘以传送前后坐标之间的距离。\n");
                costTpStageRate.setMinValue(0).setMaxValue(9999);
                if (write) costTpStageRate.set(COST_TP_STAGE_RATE);
                COST_TP_STAGE_RATE = costTpStageRate.getDouble();
            }

            {
                Property costTpBackType = config.get(CATEGORY_COST + ".TpBack", "costTpBackType", ECostType.HUNGER.name(),
                        "The cost type for 'Teleport to the previous location'.\n" +
                                "传送到上次传送点的代价类型。\n",
                        Arrays.stream(ECostType.values()).map(ECostType::name).toArray(String[]::new));
                if (write) costTpBackType.set(COST_TP_BACK_TYPE.name());
                COST_TP_BACK_TYPE = ECostType.valueOf(costTpBackType.getString());

                Property costTpBackNum = config.get(CATEGORY_COST + ".TpBack", "costTpBackNum", 1,
                        "The number of cost for 'Teleport to the previous location'.\n" +
                                "传送到上次传送点的代价数量。\n");
                costTpBackNum.setMinValue(0).setMaxValue(9999);
                if (write) costTpBackNum.set(COST_TP_BACK_NUM);
                COST_TP_BACK_NUM = costTpBackNum.getInt();

                Property costTpBackConf = config.get(CATEGORY_COST + ".TpBack", "costTpBackConf", "",
                        "The configuration for 'Teleport to the previous location'.\n" +
                                "If the type is ITEM, the value should be the item ID with optional NBT data.\n" +
                                "If the type is COMMAND, the value should be a specific command string.\n" +
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.\n" +
                                "传送到上次传送点的代价配置：\n" +
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。\n" +
                                "若类型为 COMMAND，则值为具体的指令字符串。\n" +
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。\n");
                if (write) costTpBackConf.set(COST_TP_BACK_CONF);
                COST_TP_BACK_CONF = costTpBackConf.getString();

                Property costTpBackRate = config.get(CATEGORY_COST + ".TpBack", "costTpBackRate", 0.001d,
                        "The cost rate for 'Teleport to the previous location', the cost will be multiplied by the distance between the two coordinates.\n" +
                                "传送到上次传送点的代价倍率，代价会乘以传送前后坐标之间的距离。\n");
                costTpBackRate.setMinValue(0).setMaxValue(9999);
                if (write) costTpBackRate.set(COST_TP_BACK_RATE);
                COST_TP_BACK_RATE = costTpBackRate.getDouble();
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
