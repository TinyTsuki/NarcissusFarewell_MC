package xin.vanilla.narcissus.config;

import net.minecraft.block.Blocks;
import net.minecraft.command.arguments.BlockStateParser;
import net.minecraftforge.common.ForgeConfigSpec;
import xin.vanilla.narcissus.enums.ECardType;
import xin.vanilla.narcissus.enums.ECoolDownType;
import xin.vanilla.narcissus.enums.ECostType;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 服务器配置
 */
public class ServerConfig {

    public static final ForgeConfigSpec SERVER_CONFIG;

    // region 基础设置

    /**
     * 传送卡
     */
    public static final ForgeConfigSpec.BooleanValue TELEPORT_CARD;
    /**
     * 每日传送卡数量
     */
    public static final ForgeConfigSpec.IntValue TELEPORT_CARD_DAILY;
    /**
     * 传送卡应用方式
     */
    public static final ForgeConfigSpec.EnumValue<ECardType> TELEPORT_CARD_TYPE;

    /**
     * 历史传送记录数量限制
     */
    public static final ForgeConfigSpec.IntValue TELEPORT_RECORD_LIMIT;
    /**
     * 跨维度传送
     */
    public static final ForgeConfigSpec.BooleanValue TELEPORT_ACROSS_DIMENSION;

    /**
     * 传送代价中传送距离最大取值
     */
    public static final ForgeConfigSpec.IntValue TELEPORT_COST_DISTANCE_LIMIT;

    /**
     * 跨维度传送时传送代价中传送距离取值
     */
    public static final ForgeConfigSpec.IntValue TELEPORT_COST_DISTANCE_ACROSS_DIMENSION;

    /**
     * 传送至视线尽头时最远传送距离限制
     */
    public static final ForgeConfigSpec.IntValue TELEPORT_VIEW_DISTANCE_LIMIT;

    /**
     * 传送请求过期时间
     */
    public static final ForgeConfigSpec.IntValue TELEPORT_REQUEST_EXPIRE_TIME;

    /**
     * 传送请求冷却时间计算方式
     */
    public static final ForgeConfigSpec.EnumValue<ECoolDownType> TELEPORT_REQUEST_COOLDOWN_TYPE;

    /**
     * 传送请求冷却时间
     */
    public static final ForgeConfigSpec.IntValue TELEPORT_REQUEST_COOLDOWN;

    /**
     * 随机传送距离限制
     */
    public static final ForgeConfigSpec.IntValue TELEPORT_RANDOM_DISTANCE_LIMIT;

    /**
     * 家的数量
     */
    public static final ForgeConfigSpec.IntValue TELEPORT_HOME_LIMIT;

    /**
     * 命令前缀
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_PREFIX;

    /**
     * 不安全的方块
     */
    public static final ForgeConfigSpec.ConfigValue<List<String>> UNSAFE_BLOCKS;

    /**
     * 窒息的方块
     */
    public static final ForgeConfigSpec.ConfigValue<List<String>> SUFFOCATING_BLOCKS;

    /**
     * 当安全传送未找到安全坐标时，是否在脚下放置方块
     */
    public static final ForgeConfigSpec.BooleanValue SETBLOCK_WHEN_SAFE_NOT_FOUND;

    /**
     * 当安全传送未找到安全坐标时，是否从背包中获取被放置的方块
     */
    public static final ForgeConfigSpec.BooleanValue GETBLOCK_FROM_INVENTORY;

    /**
     * 当安全传送未找到安全坐标时，放置的方块类型
     */
    public static final ForgeConfigSpec.ConfigValue<List<String>> SAFE_BLOCKS;

    /**
     * 寻找安全坐标的区块范围
     */
    public static final ForgeConfigSpec.IntValue SAFE_CHUNK_RANGE;

    /**
     * 虚拟权限
     */
    public static final ForgeConfigSpec.ConfigValue<String> OP_LIST;

    /**
     * 帮助指令信息头部内容
     */
    public static final ForgeConfigSpec.ConfigValue<String> HELP_HEADER;

    // endregion 基础设置

    // region 功能开关

    /**
     * 自杀或毒杀 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_FEED;

    /**
     * 传送到指定坐标 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_COORDINATE;

    /**
     * 传送到指定结构 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_STRUCTURE;

    /**
     * 请求传送至玩家 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_ASK;

    /**
     * 请求将玩家传送至当前位置 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_HERE;

    /**
     * 随机传送 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_RANDOM;

    /**
     * 传送到玩家重生点 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_SPAWN;

    /**
     * 传送到世界重生点 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_WORLD_SPAWN;

    /**
     * 传送到顶部 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_TOP;

    /**
     * 传送到底部 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_BOTTOM;

    /**
     * 传送到上方 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_UP;

    /**
     * 传送到下方 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_DOWN;

    /**
     * 传送至视线尽头 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_VIEW;

    /**
     * 传送到家 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_HOME;

    /**
     * 传送到驿站 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_STAGE;

    /**
     * 传送到上次传送点 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_TP_BACK;

    // endregion 功能开关

    // region 指令权限

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_COORDINATE;

    public static final ForgeConfigSpec.IntValue PERMISSION_FEED_OTHER;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_STRUCTURE;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_ASK;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_HERE;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_RANDOM;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_SPAWN;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_SPAWN_OTHER;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_WORLD_SPAWN;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_TOP;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_BOTTOM;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_UP;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_DOWN;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_VIEW;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_HOME;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_STAGE;

    public static final ForgeConfigSpec.IntValue PERMISSION_SET_STAGE;

    public static final ForgeConfigSpec.IntValue PERMISSION_DEL_STAGE;

    public static final ForgeConfigSpec.IntValue PERMISSION_GET_STAGE;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_BACK;

    public static final ForgeConfigSpec.IntValue PERMISSION_VIRTUAL_OP;

    /**
     * 跨维度传送到指定坐标权限
     */
    public static final ForgeConfigSpec.IntValue PERMISSION_TP_COORDINATE_ACROSS_DIMENSION;

    /**
     * 跨维度传送到指定结构权限
     */
    public static final ForgeConfigSpec.IntValue PERMISSION_TP_STRUCTURE_ACROSS_DIMENSION;

    /**
     * 跨维度请求传送至玩家权限
     */
    public static final ForgeConfigSpec.IntValue PERMISSION_TP_ASK_ACROSS_DIMENSION;

    /**
     * 跨维度请求将玩家传送至当前位置权限
     */
    public static final ForgeConfigSpec.IntValue PERMISSION_TP_HERE_ACROSS_DIMENSION;

    /**
     * 跨维度随机传送权限
     */
    public static final ForgeConfigSpec.IntValue PERMISSION_TP_RANDOM_ACROSS_DIMENSION;

    /**
     * 跨维度传送到玩家重生点权限
     */
    public static final ForgeConfigSpec.IntValue PERMISSION_TP_SPAWN_ACROSS_DIMENSION;

    /**
     * 跨维度传送到世界重生点权限
     */
    public static final ForgeConfigSpec.IntValue PERMISSION_TP_WORLD_SPAWN_ACROSS_DIMENSION;

    /**
     * 跨维度传送到家权限
     */
    public static final ForgeConfigSpec.IntValue PERMISSION_TP_HOME_ACROSS_DIMENSION;

    /**
     * 跨维度传送到驿站权限
     */
    public static final ForgeConfigSpec.IntValue PERMISSION_TP_STAGE_ACROSS_DIMENSION;

    /**
     * 跨维度传送到上次传送点权限
     */
    public static final ForgeConfigSpec.IntValue PERMISSION_TP_BACK_ACROSS_DIMENSION;

    // endregion 指令权限

    // region 冷却时间

    /**
     * 传送到指定坐标冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_COORDINATE;

    /**
     * 传送到指定结构冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_STRUCTURE;

    /**
     * 请求传送至玩家冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_ASK;

    /**
     * 请求将玩家传送至当前位置冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_HERE;

    /**
     * 随机传送冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_RANDOM;

    /**
     * 传送到玩家重生点冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_SPAWN;

    /**
     * 传送到世界重生点冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_WORLD_SPAWN;

    /**
     * 传送到顶部冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_TOP;

    /**
     * 传送到底部冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_BOTTOM;

    /**
     * 传送到上方冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_UP;

    /**
     * 传送到下方冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_DOWN;

    /**
     * 传送至视线尽头冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_VIEW;

    /**
     * 传送到家冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_HOME;

    /**
     * 传送到驿站冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_STAGE;

    /**
     * 传送到上次传送点冷却时间
     */
    public static final ForgeConfigSpec.IntValue COOLDOWN_TP_BACK;

    // endregion 冷却时间

    // region 自定义指令

    /**
     * 获取玩家的UUID
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_UUID;

    /**
     * 获取当前世界的维度ID
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_DIMENSION;

    /**
     * 自杀或毒杀(水仙是有毒的可不能吃哦)
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_FEED;

    /**
     * 传送到指定坐标
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_COORDINATE;

    /**
     * 传送到指定结构
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_STRUCTURE;

    /**
     * 请求传送至玩家
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_ASK;

    /**
     * 接受请求传送至玩家
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_ASK_YES;

    /**
     * 拒绝请求传送至玩家
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_ASK_NO;

    /**
     * 请求将玩家传送至当前位置
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_HERE;

    /**
     * 接受请求将玩家传送至当前位置
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_HERE_YES;

    /**
     * 拒绝请求将玩家传送至当前位置
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_HERE_NO;

    /**
     * 随机传送
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_RANDOM;

    /**
     * 传送到玩家重生点
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_SPAWN;

    /**
     * 传送到世界重生点
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_WORLD_SPAWN;

    /**
     * 传送到顶部
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_TOP;

    /**
     * 传送到底部
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_BOTTOM;

    /**
     * 传送到上方
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_UP;

    /**
     * 传送到下方
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_DOWN;

    /**
     * 传送至视线尽头
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_VIEW;

    /**
     * 传送到家
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_HOME;

    /**
     * 设置家
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_SET_HOME;

    /**
     * 删除家
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_DEL_HOME;

    /**
     * 查询家
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_GET_HOME;

    /**
     * 传送到驿站
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_STAGE;

    /**
     * 设置驿站
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_SET_STAGE;

    /**
     * 删除驿站
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_DEL_STAGE;

    /**
     * 查询驿站
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_GET_STAGE;

    /**
     * 传送到上次传送点
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_BACK;

    /**
     * 设置虚拟权限
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_VIRTUAL_OP;

    // endregion 自定义指令

    // region 简化指令

    /**
     * 获取玩家的UUID
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_UUID;

    /**
     * 获取当前世界的维度ID
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_DIMENSION;

    /**
     * 自杀或毒杀
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_FEED;

    /**
     * 传送到指定坐标
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_COORDINATE;

    /**
     * 传送到指定结构
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_STRUCTURE;

    /**
     * 请求传送至玩家
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_ASK;

    /**
     * 接受请求传送至玩家
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_ASK_YES;

    /**
     * 拒绝请求传送至玩家
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_ASK_NO;

    /**
     * 请求将玩家传送至当前位置
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_HERE;

    /**
     * 接受请求将玩家传送至当前位置
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_HERE_YES;

    /**
     * 拒绝请求将玩家传送至当前位置
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_HERE_NO;

    /**
     * 随机传送
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_RANDOM;

    /**
     * 传送到玩家重生点
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_SPAWN;

    /**
     * 传送到世界重生点
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_WORLD_SPAWN;

    /**
     * 传送到顶部
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_TOP;

    /**
     * 传送到底部
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_BOTTOM;

    /**
     * 传送到上方
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_UP;

    /**
     * 传送到下方
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_DOWN;

    /**
     * 传送至视线尽头
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_VIEW;

    /**
     * 传送到家
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_HOME;

    /**
     * 设置家
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_SET_HOME;

    /**
     * 删除家
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_DEL_HOME;

    /**
     * 查询家
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_GET_HOME;

    /**
     * 传送到驿站
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_STAGE;

    /**
     * 设置驿站
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_SET_STAGE;

    /**
     * 删除驿站
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_DEL_STAGE;

    /**
     * 查询驿站
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_GET_STAGE;

    /**
     * 传送到上次传送点
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_BACK;

    /**
     * 设置虚拟权限
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_VIRTUAL_OP;

    // endregion 简化指令

    // region 传送代价

    /**
     * 代价类型
     */
    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_COORDINATE_TYPE;
    /**
     * 代价数量
     */
    public static final ForgeConfigSpec.IntValue COST_TP_COORDINATE_NUM;
    /**
     * 代价配置
     */
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_COORDINATE_CONF;
    /**
     * 代价倍率(以距离为基准)
     */
    public static final ForgeConfigSpec.DoubleValue COST_TP_COORDINATE_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_STRUCTURE_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_STRUCTURE_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_STRUCTURE_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_STRUCTURE_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_ASK_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_ASK_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_ASK_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_ASK_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_HERE_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_HERE_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_HERE_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_HERE_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_RANDOM_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_RANDOM_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_RANDOM_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_RANDOM_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_SPAWN_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_SPAWN_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_SPAWN_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_SPAWN_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_WORLD_SPAWN_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_WORLD_SPAWN_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_WORLD_SPAWN_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_WORLD_SPAWN_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_TOP_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_TOP_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_TOP_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_TOP_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_BOTTOM_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_BOTTOM_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_BOTTOM_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_BOTTOM_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_UP_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_UP_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_UP_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_UP_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_DOWN_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_DOWN_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_DOWN_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_DOWN_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_VIEW_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_VIEW_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_VIEW_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_VIEW_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_HOME_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_HOME_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_HOME_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_HOME_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_STAGE_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_STAGE_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_STAGE_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_STAGE_RATE;

    public static final ForgeConfigSpec.EnumValue<ECostType> COST_TP_BACK_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_BACK_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_BACK_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_BACK_RATE;

    // endregion 传送代价

    static {
        ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();
        // 定义服务器基础设置
        {
            SERVER_BUILDER.comment("Base Settings", "基础设置").push("common");

            SERVER_BUILDER.comment("Teleport Card", "传送卡").push("TpCard");
            // 传送卡
            TELEPORT_CARD = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport Card'."
                            , "是否启用传送卡。")
                    .define("teleportCard", false);

            // 每日传送卡数量
            TELEPORT_CARD_DAILY = SERVER_BUILDER
                    .comment("The number of Teleport Card that can be obtained daily."
                            , "每日可获得的传送卡数量。")
                    .defineInRange("teleportCardDaily", 0, 0, 9999);

            // 传送卡应用方式
            TELEPORT_CARD_TYPE = SERVER_BUILDER
                    .comment("The application method of the Teleport Card:",
                            "NONE: Teleportation consumes a Teleport Card and requires an additional cost. If the player has insufficient cards, teleportation is not allowed.",
                            "LIKE_COST: Teleportation consumes the same number of Teleport Cards as the cost and additionally charges the cost. If there are insufficient Teleport Cards, teleportation cannot proceed.",
                            "REFUND_COST: Teleportation consumes a Teleport Card to offset the cost at a 1:1 ratio. If the player has insufficient cards, the corresponding cost will be charged.",
                            "REFUND_ALL_COST: Teleportation consumes a Teleport Card to completely offset all costs. If the player has insufficient cards, the corresponding cost will be charged.",
                            "REFUND_COOLDOWN: Teleportation consumes a Teleport Card to offset the cooldown time, but the cost must still be paid. If the player has insufficient cards, the corresponding cost will be charged.",
                            "REFUND_COST_AND_COOLDOWN: Teleportation consumes a Teleport Card to offset the cost at a 1:1 ratio and offset the cooldown time. If the player has insufficient cards, the corresponding cost will be charged.",
                            "REFUND_ALL_COST_AND_COOLDOWN: Teleportation consumes a Teleport Card to completely offset all costs and cooldown time. If the player has insufficient cards, the corresponding cost will be charged.",
                            "If both Teleport Cards and the cost are insufficient, teleportation will not proceed. ",
                            "If you want the Teleport Card to offset the cost but prohibit teleportation when cards are insufficient, set the cost to zero in the configuration.",
                            "传送卡的使用方式：",
                            "NONE: 传送会消耗一个传送卡，并额外收取代价。若传送卡不足，则无法传送。",
                            "LIKE_COST: 传送会消耗与代价数量一致的传送卡，并额外收取代价。若传送卡不足，则无法传送。",
                            "REFUND_COST: 传送会消耗传送卡并按一比一比例抵消代价。若传送卡不足，则收取对应代价。",
                            "REFUND_ALL_COST: 传送会消耗传送卡并完全抵消所有代价。若传送卡不足，则收取对应代价。",
                            "REFUND_COOLDOWN: 传送会消耗传送卡并抵消冷却时间，但仍需支付代价。若传送卡不足，则收取对应代价。",
                            "REFUND_COST_AND_COOLDOWN: 传送会消耗传送卡，按一比一比例抵消代价并抵消冷却时间。若传送卡不足，则收取对应代价。",
                            "REFUND_ALL_COST_AND_COOLDOWN: 传送会消耗传送卡，完全抵消所有代价和冷却时间。若传送卡不足，则收取对应代价。",
                            "若传送卡与代价都不足，则无法传送。若希望传送卡能够抵消代价但在不足时禁止传送，请在配置中将代价设置为零。")
                    .defineEnum("teleportCardType", ECardType.REFUND_ALL_COST);
            SERVER_BUILDER.pop();

            // 历史传送记录数量限制
            TELEPORT_RECORD_LIMIT = SERVER_BUILDER
                    .comment("The limit of teleport records, 0 means no limit."
                            , "传送记录数量限制，数量为0表示不限制。")
                    .defineInRange("teleportRecordLimit", 100, 0, 99999);

            // 跨维度传送
            TELEPORT_ACROSS_DIMENSION = SERVER_BUILDER
                    .comment("Is the teleport across dimensions enabled?"
                            , "是否启用跨维度传送。")
                    .define("teleportAcrossDimension", true);

            // 传送代价中传送距离计算限制
            TELEPORT_COST_DISTANCE_LIMIT = SERVER_BUILDER
                    .comment("The distance calculation limit for teleport cost, 0 means no limit.",
                            "(This config item is not the limit of teleport distance, but the limit of the distance multiplier used when calculating teleport cost.)"
                            , "传送代价中传送距离计算限制，值为0表示不限制。(此配置项并非限制传送距离，而是限制计算传送代价时使用的距离乘数。)")
                    .defineInRange("teleportDistanceLimit", 10000, 0, Integer.MAX_VALUE);

            // 跨维度传送时传送代价中传送距离取值
            TELEPORT_COST_DISTANCE_ACROSS_DIMENSION = SERVER_BUILDER
                    .comment("The distance value for teleport cost when teleport across dimensions, 0 means no limit."
                            , "跨维度传送时传送代价中传送距离取值，值为0表示不限制。")
                    .defineInRange("teleportDistanceAcrossDimension", 10000, 0, Integer.MAX_VALUE);

            // 传送至视线尽头时最远传送距离限制
            TELEPORT_VIEW_DISTANCE_LIMIT = SERVER_BUILDER
                    .comment("The distance limit for teleporting to the view, 0 means no limit."
                            , "传送至视线尽头时最远传送距离限制，值为0表示不限制。")
                    .defineInRange("teleportViewDistanceLimit", 16 * 64, 0, Integer.MAX_VALUE);

            // 传送请求过期时间
            TELEPORT_REQUEST_EXPIRE_TIME = SERVER_BUILDER
                    .comment("The expire time for teleport request, in seconds."
                            , "传送请求过期时间，单位为秒。")
                    .defineInRange("teleportRequestExpireTime", 60, 0, 60 * 60);

            // 传送请求冷却时间计算方式
            TELEPORT_REQUEST_COOLDOWN_TYPE = SERVER_BUILDER
                    .comment(
                            "The method used to calculate the cooldown time for teleport requests.",
                            "COMMON: All commands share the same global cooldown defined by 'teleportRequestCooldown'.",
                            "INDIVIDUAL: Each command has a separate cooldown managed by the command itself.",
                            "MIXED: Combines both methods, using both the global cooldown and individual cooldowns.",
                            "传送请求冷却时间的计算方式：",
                            "COMMON：所有传送共用全局冷却时间，由'teleportRequestCooldown'配置定义。",
                            "INDIVIDUAL：每个指令有单独的冷却时间，由指令自身管理。",
                            "MIXED：结合两种方式，同时使用全局冷却时间和单独冷却时间。"
                    )
                    .defineEnum("teleportRequestCooldownType", ECoolDownType.INDIVIDUAL);

            // 传送请求冷却时间
            TELEPORT_REQUEST_COOLDOWN = SERVER_BUILDER
                    .comment(
                            "The global cooldown time for teleport requests, measured in seconds.",
                            "This value applies to all commands when the cooldown type is COMMON or MIXED.",
                            "传送请求的全局冷却时间，单位为秒。",
                            "当冷却时间计算方式为COMMON或MIXED时，此值对所有指令生效。"
                    )
                    .defineInRange("teleportRequestCooldown", 10, 0, 60 * 60 * 24);

            // 随机传送距离限制
            TELEPORT_RANDOM_DISTANCE_LIMIT = SERVER_BUILDER
                    .comment("The maximum distance limit for random teleportation or teleportation to a specified structure.",
                            "随机传送与传送至指定结构的最大距离限制。")
                    .defineInRange("teleportRandomDistanceLimit", 10000, 5, Integer.MAX_VALUE);

            // 玩家可设置的家的数量
            TELEPORT_HOME_LIMIT = SERVER_BUILDER
                    .comment("The maximum number of homes that can be set by the player.",
                            "玩家可设置的家的数量。")
                    .defineInRange("teleportHomeLimit", 5, 1, 9999);

            // 命令前缀
            COMMAND_PREFIX = SERVER_BUILDER
                    .comment("The prefix of the command, please only use English characters and underscores, otherwise it may cause problems.",
                            "指令前缀，请仅使用英文字母及下划线，否则可能会出现问题。")
                    .define("commandPrefix", "narcissus");

            // 虚拟权限
            OP_LIST = SERVER_BUILDER
                    .comment("Virtual permission list, in this list you can directly specify which players can use which mod commands without enabling cheat mode or setting the player as OP.",
                            "Format: \"player UUID\":\"a comma-separated list of commands that the player can use\". ",
                            "虚拟权限列表，在这里可以直接指定某个玩家能够使用哪些mod内的指令，而不需要开启作弊模式或将他设置为OP。",
                            "格式：\"玩家UUID\":\"逗号分隔的能够使用的指令列表\"",
                            "Example: opList = \"{\\\"23a23a23-od0o-23aa-2333-0d0o0d0033aa\\\":[\\\"VIRTUAL_OP\\\",\\\"TP_BACK\\\",\\\"TP_HOME\\\",\\\"TP_STAGE\\\",\\\"TP_ASK\\\",\\\"TP_HERE\\\",\\\"TP_SPAWN\\\",\\\"TP_SPAWN_OTHER\\\",\\\"DIMENSION\\\",\\\"TP_COORDINATE\\\",\\\"TP_STRUCTURE\\\",\\\"TP_TOP\\\",\\\"TP_DOWN\\\",\\\"TP_RANDOM\\\",\\\"FEED\\\",\\\"FEED_OTHER\\\",\\\"SET_STAGE\\\",\\\"DEL_STAGE\\\"]}\"")
                    .define("opList", "");

            // 帮助指令信息头部内容
            HELP_HEADER = SERVER_BUILDER
                    .comment("The header content of the help command.",
                            "帮助指令信息头部内容。")
                    .define("helpHeader", "-----==== Narcissus Farewell Help (%d/%d) ====-----");

            SERVER_BUILDER.comment("Safe Teleport", "安全传送").push("Safe");
            // 不安全的方块
            UNSAFE_BLOCKS = SERVER_BUILDER
                    .comment("The list of unsafe blocks, players will not be teleported to these blocks.",
                            "不安全的方块列表，玩家不会传送到这些方块上。")
                    .define("unsafeBlocks", Stream.of(
                                            Blocks.LAVA,
                                            Blocks.FIRE,
                                            Blocks.CAMPFIRE,
                                            Blocks.SOUL_FIRE,
                                            Blocks.SOUL_CAMPFIRE,
                                            Blocks.CACTUS,
                                            Blocks.MAGMA_BLOCK,
                                            Blocks.SWEET_BERRY_BUSH
                                    ).map(block -> BlockStateParser.serialize(block.defaultBlockState()))
                                    .collect(Collectors.toList())
                    );

            // 窒息的方块
            SUFFOCATING_BLOCKS = SERVER_BUILDER
                    .comment("The list of suffocating blocks, players will not be teleported to these blocks.",
                            "窒息的方块列表，玩家头不会处于这些方块里面。")
                    .define("suffocatingBlocks", Stream.of(
                                            Blocks.LAVA,
                                            Blocks.WATER
                                    ).map(block -> BlockStateParser.serialize(block.defaultBlockState()))
                                    .collect(Collectors.toList())
                    );

            // 安全传送放置方块
            SETBLOCK_WHEN_SAFE_NOT_FOUND = SERVER_BUILDER
                    .comment("When performing a safe teleport, whether to place a block underfoot if a safe coordinate is not found.",
                            "当进行安全传送时，如果未找到安全坐标，是否在脚下放置方块。")
                    .define("setBlockWhenSafeNotFound", false);

            // 从背包获取安全方块
            GETBLOCK_FROM_INVENTORY = SERVER_BUILDER
                    .comment("When performing a safe teleport, whether to only use placeable blocks from the player's inventory if a safe coordinate is not found.",
                            "当进行安全传送时，如果未找到安全坐标，是否仅从背包中获取可放置的方块。")
                    .define("getBlockFromInventory", true);

            // 安全方块类型
            SAFE_BLOCKS = SERVER_BUILDER
                    .comment("When performing a safe teleport, the list of blocks to place if a safe coordinate is not found. If 'getBlockFromInventory' is set to false, the first block in the list will always be used.",
                            "当进行安全传送时，如果未找到安全坐标，放置方块的列表。若'getBlockFromInventory'为false，则始终使用列表中的第一个方块。")
                    .define("safeBlocks", Stream.of(
                                            Blocks.GRASS_BLOCK,
                                            Blocks.GRASS_PATH,
                                            Blocks.DIRT,
                                            Blocks.COBBLESTONE
                                    ).map(block -> BlockStateParser.serialize(block.defaultBlockState()))
                                    .collect(Collectors.toList())
                    );

            // 寻找安全坐标的区块范围
            SAFE_CHUNK_RANGE = SERVER_BUILDER
                    .comment("The chunk range for finding a safe coordinate, in chunks.",
                            "当进行安全传送时，寻找安全坐标的半径，单位为区块。")
                    .defineInRange("safeChunkRange", 1, 1, 16);
            SERVER_BUILDER.pop();

            SERVER_BUILDER.pop();
        }

        // 定义功能开关
        {
            SERVER_BUILDER.comment("Function Switch", "功能开关").push("switch");

            SWITCH_FEED = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Suicide or poisoning'."
                            , "是否启用自杀或毒杀。")
                    .define("switchFeed", true);

            SWITCH_TP_COORDINATE = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport to the specified coordinates'."
                            , "是否启用传送到指定坐标。")
                    .define("switchTpCoordinate", true);

            SWITCH_TP_STRUCTURE = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport to the specified structure'."
                            , "是否启用传送到指定结构。")
                    .define("switchTpStructure", true);

            SWITCH_TP_ASK = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Request to teleport oneself to other players'."
                            , "是否启用传送请求。")
                    .define("switchTpAsk", true);

            SWITCH_TP_HERE = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Request the transfer of other players to oneself'."
                            , "是否启用请求将玩家传送至当前位置。")
                    .define("switchTpHere", true);

            SWITCH_TP_RANDOM = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport to a random location'."
                            , "是否启用随机传送。")
                    .define("switchTpRandom", true);

            SWITCH_TP_SPAWN = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport to the spawn of the player'."
                            , "是否启用传送到玩家重生点。")
                    .define("switchTpSpawn", true);

            SWITCH_TP_WORLD_SPAWN = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport to the spawn of the world'."
                            , "是否启用传送到世界重生点。")
                    .define("switchTpWorldSpawn", true);

            SWITCH_TP_TOP = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport to the top of current position'."
                            , "是否启用传送到顶部。")
                    .define("switchTpTop", true);

            SWITCH_TP_BOTTOM = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport to the bottom of current position'."
                            , "是否启用传送到底部。")
                    .define("switchTpBottom", true);

            SWITCH_TP_UP = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport to the upper of current position'."
                            , "是否启用传送到上方。")
                    .define("switchTpUp", true);

            SWITCH_TP_DOWN = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport to the lower of current position'."
                            , "是否启用传送到下方。")
                    .define("switchTpDown", true);

            SWITCH_TP_VIEW = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport to the end of the line of sight'."
                            , "This function is independent of the player's render distance setting."
                            , "是否启用传送至视线尽头。"
                            , "该功能与玩家设置的视距无关。")
                    .define("switchTpView", true);

            SWITCH_TP_HOME = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport to the home'."
                            , "是否启用传送到家。")
                    .define("switchTpHome", true);

            SWITCH_TP_STAGE = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport to the stage'."
                            , "是否启用传送到驿站。")
                    .define("switchTpStage", true);

            SWITCH_TP_BACK = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Teleport to the previous location'."
                            , "是否启用传送到上次传送点。")
                    .define("switchTpBack", true);

            SERVER_BUILDER.pop();
        }

        // 定义指令权限
        {
            SERVER_BUILDER.comment("Command Permission", "指令权限").push("permission");

            SERVER_BUILDER.comment("Command Permission", "指令权限").push("command");
            {

                PERMISSION_FEED_OTHER = SERVER_BUILDER
                        .comment("The permission level required to use the 'Poisoning others' command."
                                , "毒杀指令所需的权限等级。")
                        .defineInRange("permissionFeedOther", 2, 0, 4);

                PERMISSION_TP_COORDINATE = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the specified coordinates' command."
                                , "传送到指定坐标指令所需的权限等级。")
                        .defineInRange("permissionTpCoordinate", 2, 0, 4);

                PERMISSION_TP_STRUCTURE = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the specified structure' command."
                                , "传送到指定结构指令所需的权限等级。")
                        .defineInRange("permissionTpStructure", 2, 0, 4);

                PERMISSION_TP_ASK = SERVER_BUILDER
                        .comment("The permission level required to use the 'Request to teleport oneself to other players' command."
                                , "请求传送至玩家指令所需的权限等级。")
                        .defineInRange("permissionTpAsk", 0, 0, 4);

                PERMISSION_TP_HERE = SERVER_BUILDER
                        .comment("The permission level required to use the 'Request the transfer of other players to oneself' command."
                                , "请求将玩家传送至当前位置指令所需的权限等级。")
                        .defineInRange("permissionTpHere", 0, 0, 4);

                PERMISSION_TP_RANDOM = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to a random location' command."
                                , "随机传送指令所需的权限等级。")
                        .defineInRange("permissionTpRandom", 1, 0, 4);

                PERMISSION_TP_SPAWN = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the spawn of the player' command."
                                , "传送到玩家重生点指令所需的权限等级。")
                        .defineInRange("permissionTpSpawn", 0, 0, 4);

                PERMISSION_TP_SPAWN_OTHER = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the spawn of the other player' command."
                                , "传送到其他玩家重生点指令所需的权限等级。")
                        .defineInRange("permissionTpSpawnOther", 2, 0, 4);

                PERMISSION_TP_WORLD_SPAWN = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the spawn of the world' command."
                                , "传送到世界重生点指令所需的权限等级。")
                        .defineInRange("permissionTpWorldSpawn", 0, 0, 4);

                PERMISSION_TP_TOP = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the top of current position' command."
                                , "传送到顶部指令所需的权限等级。")
                        .defineInRange("permissionTpTop", 1, 0, 4);

                PERMISSION_TP_BOTTOM = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the bottom of current position' command."
                                , "传送到底部指令所需的权限等级。")
                        .defineInRange("permissionTpBottom", 1, 0, 4);

                PERMISSION_TP_UP = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the upper of current position' command."
                                , "传送到上方指令所需的权限等级。")
                        .defineInRange("permissionTpUp", 1, 0, 4);

                PERMISSION_TP_DOWN = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the lower of current position' command."
                                , "传送到下方指令所需的权限等级。")
                        .defineInRange("permissionTpDown", 1, 0, 4);

                PERMISSION_TP_VIEW = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the end of the line of sight' command."
                                , "This function is independent of the player's render distance setting."
                                , "传送至视线尽头指令所需的权限等级。"
                                , "该功能与玩家设置的视距无关。")
                        .defineInRange("permissionTpView", 1, 0, 4);

                PERMISSION_TP_HOME = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the home' command."
                                , "传送到家指令所需的权限等级。")
                        .defineInRange("permissionTpHome", 0, 0, 4);

                PERMISSION_TP_STAGE = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the stage' command."
                                , "传送到驿站指令所需的权限等级。")
                        .defineInRange("permissionTpStage", 0, 0, 4);

                PERMISSION_SET_STAGE = SERVER_BUILDER
                        .comment("The permission level required to use the 'Set the stage' command."
                                , "设置驿站指令所需的权限等级。")
                        .defineInRange("permissionTpStageSet", 2, 0, 4);

                PERMISSION_DEL_STAGE = SERVER_BUILDER
                        .comment("The permission level required to use the 'Delete the stage' command."
                                , "删除驿站指令所需的权限等级。")
                        .defineInRange("permissionTpStageDel", 2, 0, 4);

                PERMISSION_GET_STAGE = SERVER_BUILDER
                        .comment("The permission level required to use the 'Get the stage info' command."
                                , "查询驿站指令所需的权限等级。")
                        .defineInRange("permissionTpStageGet", 0, 0, 4);

                PERMISSION_TP_BACK = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the previous location' command."
                                , "传送到上次传送点指令所需的权限等级。")
                        .defineInRange("permissionTpBack", 0, 0, 4);

                PERMISSION_VIRTUAL_OP = SERVER_BUILDER
                        .comment("The permission level required to use the 'Set virtual permission' command, and also used as the permission level for modifying server configuration."
                                , "设置虚拟权限指令所需的权限等级，同时用于控制使用'修改服务器配置指令'的权限。")
                        .defineInRange("permissionVirtualOp", 4, 0, 4);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Across dimensions Switch", "跨维度权限").push("across");
            {
                PERMISSION_TP_COORDINATE_ACROSS_DIMENSION = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the specified coordinates' command across dimensions, -1 means disabled."
                                , "跨维度传送到指定坐标指令所需的权限等级，若为-1则禁用跨维度传送。")
                        .defineInRange("permissionTpCoordinateAcrossDimension", 2, -1, 4);

                PERMISSION_TP_STRUCTURE_ACROSS_DIMENSION = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the specified structure' command across dimensions, -1 means disabled."
                                , "跨维度传送到指定结构指令所需的权限等级，若为-1则禁用跨维度传送。")
                        .defineInRange("permissionTpStructureAcrossDimension", 2, -1, 4);

                PERMISSION_TP_ASK_ACROSS_DIMENSION = SERVER_BUILDER
                        .comment("The permission level required to use the 'Request to teleport oneself to other players' command across dimensions, -1 means disabled."
                                , "跨维度请求传送至玩家指令所需的权限等级，若为-1则禁用跨维度传送。")
                        .defineInRange("permissionTpAskAcrossDimension", 0, -1, 4);

                PERMISSION_TP_HERE_ACROSS_DIMENSION = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the current position' command across dimensions, -1 means disabled."
                                , "跨维度传送到当前位置指令所需的权限等级，若为-1则禁用跨维度传送。")
                        .defineInRange("permissionTpHereAcrossDimension", 0, -1, 4);

                PERMISSION_TP_RANDOM_ACROSS_DIMENSION = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the random position' command across dimensions, -1 means disabled."
                                , "跨维度传送到随机位置指令所需的权限等级，若为-1则禁用跨维度传送。")
                        .defineInRange("permissionTpRandomAcrossDimension", 0, -1, 4);

                PERMISSION_TP_SPAWN_ACROSS_DIMENSION = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the spawn of the current dimension' command across dimensions, -1 means disabled."
                                , "跨维度传送到当前维度的出生点指令所需的权限等级，若为-1则禁用跨维度传送。")
                        .defineInRange("permissionTpSpawnAcrossDimension", 0, -1, 4);

                PERMISSION_TP_WORLD_SPAWN_ACROSS_DIMENSION = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the world spawn of the current dimension' command across dimensions, -1 means disabled."
                                , "跨维度传送到当前维度的世界出生点指令所需的权限等级，若为-1则禁用跨维度传送。")
                        .defineInRange("permissionTpWorldSpawnAcrossDimension", 0, -1, 4);

                PERMISSION_TP_HOME_ACROSS_DIMENSION = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the home' command across dimensions, -1 means disabled."
                                , "跨维度传送到家指令所需的权限等级，若为-1则禁用跨维度传送。")
                        .defineInRange("permissionTpHomeAcrossDimension", 0, -1, 4);

                PERMISSION_TP_STAGE_ACROSS_DIMENSION = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the stage' command across dimensions, -1 means disabled."
                                , "跨维度传送到驿站指令所需的权限等级，若为-1则禁用跨维度传送。")
                        .defineInRange("permissionTpStageAcrossDimension", 0, -1, 4);

                PERMISSION_TP_BACK_ACROSS_DIMENSION = SERVER_BUILDER
                        .comment("The permission level required to use the 'Teleport to the previous location' command across dimensions, -1 means disabled."
                                , "跨维度传送到上次传送点指令所需的权限等级，若为-1则禁用跨维度传送。")
                        .defineInRange("permissionTpBackAcrossDimension", 0, -1, 4);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.pop();
        }

        // 定义冷却时间
        {
            SERVER_BUILDER.comment("Cooldown Time", "冷却时间").push("cooldown");

            COOLDOWN_TP_COORDINATE = SERVER_BUILDER
                    .comment("The cooldown time for 'Teleport to the specified coordinates', in seconds."
                            , "传送到指定坐标的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpCoordinate", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_STRUCTURE = SERVER_BUILDER
                    .comment("The cooldown time for 'Teleport to the specified structure', in seconds."
                            , "传送到指定结构的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpStructure", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_ASK = SERVER_BUILDER
                    .comment("The cooldown time for 'Request to teleport oneself to other players', in seconds."
                            , "请求传送至玩家的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpAsk", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_HERE = SERVER_BUILDER
                    .comment("The cooldown time for 'Request the transfer of other players to oneself', in seconds."
                            , "请求将玩家传送至当前位置的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpHere", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_RANDOM = SERVER_BUILDER
                    .comment("The cooldown time for 'Teleport to a random location', in seconds."
                            , "随机传送的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpRandom", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_SPAWN = SERVER_BUILDER
                    .comment("The cooldown time for 'Teleport to the spawn of the player', in seconds."
                            , "传送到玩家重生点的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpSpawn", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_WORLD_SPAWN = SERVER_BUILDER
                    .comment("The cooldown time for 'Teleport to the spawn of the world', in seconds."
                            , "传送到世界重生点的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpWorldSpawn", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_TOP = SERVER_BUILDER
                    .comment("The cooldown time for 'Teleport to the top of current position', in seconds."
                            , "传送到顶部的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpTop", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_BOTTOM = SERVER_BUILDER
                    .comment("The cooldown time for 'Teleport to the bottom of current position', in seconds."
                            , "传送到底部的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpBottom", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_UP = SERVER_BUILDER
                    .comment("The cooldown time for 'Teleport to the upper of current position', in seconds."
                            , "传送到上方的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpUp", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_DOWN = SERVER_BUILDER
                    .comment("The cooldown time for 'Teleport to the lower of current position', in seconds."
                            , "传送到下方的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpDown", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_VIEW = SERVER_BUILDER
                    .comment("The cooldown time for 'Teleport to the end of the line of sight', in seconds."
                            , "This function is independent of the player's render distance setting."
                            , "传送至视线尽头的冷却时间，单位为秒。"
                            , "该功能与玩家设置的视距无关。")
                    .defineInRange("cooldownTpView", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_HOME = SERVER_BUILDER
                    .comment("The cooldown time for 'Teleport to the home', in seconds."
                            , "传送到家的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpHome", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_STAGE = SERVER_BUILDER
                    .comment("The cooldown time for 'Teleport to the stage', in seconds."
                            , "传送到驿站的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpStage", 10, 0, 60 * 60 * 24);

            COOLDOWN_TP_BACK = SERVER_BUILDER
                    .comment("The cooldown time for 'Teleport to the previous location', in seconds."
                            , "传送到上次传送点的冷却时间，单位为秒。")
                    .defineInRange("cooldownTpBack", 10, 0, 60 * 60 * 24);

            SERVER_BUILDER.pop();
        }

        // 定义自定义指令配置
        {
            SERVER_BUILDER.comment("Custom Command Settings, don't add prefix '/'", "自定义指令，请勿添加前缀'/'").push("command");

            // 获取玩家的UUID
            COMMAND_UUID = SERVER_BUILDER
                    .comment("This command is used to get the UUID of the player."
                            , "获取玩家的UUID的指令。")
                    .define("commandUuid", "uuid");

            // 获取当前世界的维度ID
            COMMAND_DIMENSION = SERVER_BUILDER
                    .comment("This command is used to get the dimension ID of the current world."
                            , "获取当前世界的维度ID的指令。")
                    .define("commandDimension", "dim");

            // 自杀或毒杀
            COMMAND_FEED = SERVER_BUILDER
                    .comment("This command is used to suicide or poisoning, narcissus are poisonous and should not be eaten."
                            , "自杀或毒杀的指令，水仙是有毒的可不能食用哦。")
                    .define("commandFeed", "feed");

            // 传送到指定坐标
            COMMAND_TP_COORDINATE = SERVER_BUILDER
                    .comment("This command is used to teleport to the specified coordinates."
                            , "传送到指定坐标的指令。")
                    .define("commandTpCoordinate", "tpx");

            // 传送到指定结构
            COMMAND_TP_STRUCTURE = SERVER_BUILDER
                    .comment("This command is used to teleport to the specified structure."
                            , "传送到指定结构的指令。")
                    .define("commandTpStructure", "tpst");

            SERVER_BUILDER.comment("Request to teleport oneself to other players", "请求传送至玩家").push("TpAsk");
            // 请求传送至玩家指令
            COMMAND_TP_ASK = SERVER_BUILDER
                    .comment("This command is used to request to teleport oneself to other players."
                            , "请求传送至玩家的指令。")
                    .define("commandTpAsk", "tpa");

            COMMAND_TP_ASK_YES = SERVER_BUILDER
                    .comment("This command is used to accept teleportation of other players to oneself."
                            , "I can't translate it clearly either, as long as you understand the meaning. >_<"
                            , "接受请求传送至玩家的指令。"
                            , "我也翻译不清楚了，你懂意思就行。>_<")
                    .define("commandTpAskYes", "tpay");

            COMMAND_TP_ASK_NO = SERVER_BUILDER
                    .comment("This command is used to refuse teleportation of other players to oneself."
                            , "I can't translate it clearly either, as long as you understand the meaning. >_<"
                            , "拒绝请求传送至玩家的指令。"
                            , "我也翻译不清楚了，你懂意思就行。>_<")
                    .define("commandTpAskNo", "tpan");
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Request the transfer of other players to oneself", "请求将玩家传送至当前位置").push("TpHere");
            // 请求将玩家传送至当前位置
            COMMAND_TP_HERE = SERVER_BUILDER
                    .comment("This command is used to request the transfer of other players to oneself."
                            , "请求将玩家传送至当前位置的指令。")
                    .define("commandTpHere", "tph");

            COMMAND_TP_HERE_YES = SERVER_BUILDER
                    .comment("This command is used to accept teleportation to other players."
                            , "I can't translate it clearly either, as long as you understand the meaning. >_<"
                            , "接受请求将玩家传送至当前位置的指令。"
                            , "我也翻译不清楚了，你懂意思就行。>_<")
                    .define("commandTpHereYes", "tphy");

            COMMAND_TP_HERE_NO = SERVER_BUILDER
                    .comment("This command is used to refuse teleportation to other players."
                            , "I can't translate it clearly either, as long as you understand the meaning. >_<"
                            , "拒绝请求将玩家传送至当前位置的指令。"
                            , "我也翻译不清楚了，你懂意思就行。>_<")
                    .define("commandTpHereNo", "tphn");
            SERVER_BUILDER.pop();

            // 随机传送
            COMMAND_TP_RANDOM = SERVER_BUILDER
                    .comment("The command to teleport to a random location."
                            , "随机传送的指令。")
                    .define("commandTpRandom", "tpr");

            // 传送到玩家重生点
            COMMAND_TP_SPAWN = SERVER_BUILDER
                    .comment("The command to teleport to the spawn of the player."
                            , "传送到玩家重生点的指令。")
                    .define("commandTpSpawn", "tpsp");

            // 传送到世界重生点
            COMMAND_TP_WORLD_SPAWN = SERVER_BUILDER
                    .comment("The command to teleport to the spawn of the world."
                            , "传送到世界重生点的指令。")
                    .define("commandTpWorldSpawn", "tpws");

            // 传送到顶部
            COMMAND_TP_TOP = SERVER_BUILDER
                    .comment("The command to teleport to the top of current position."
                            , "传送到顶部的指令。")
                    .define("commandTpTop", "tpt");

            // 传送到底部
            COMMAND_TP_BOTTOM = SERVER_BUILDER
                    .comment("The command to teleport to the bottom of current position."
                            , "传送到底部的指令。")
                    .define("commandTpBottom", "tpb");

            // 传送到上方
            COMMAND_TP_UP = SERVER_BUILDER
                    .comment("The command to teleport to the upper of current position."
                            , "传送到上方的指令。")
                    .define("commandTpUp", "tpu");

            // 传送到下方
            COMMAND_TP_DOWN = SERVER_BUILDER
                    .comment("The command to teleport to the lower of current position."
                            , "传送到下方的指令。")
                    .define("commandTpDown", "tpd");

            // 传送至视线尽头
            COMMAND_TP_VIEW = SERVER_BUILDER
                    .comment("The command to teleport to the end of the line of sight."
                            , "This function is independent of the player's render distance setting."
                            , "传送至视线尽头的指令。"
                            , "该功能与玩家设置的视距无关。")
                    .define("commandTpView", "tpv");

            SERVER_BUILDER.comment("Teleport to the home", "传送到家").push("TpHome");
            // 传送到家
            COMMAND_TP_HOME = SERVER_BUILDER
                    .comment("The command to teleport to the home."
                            , "传送到家的指令。")
                    .define("commandTpHome", "home");

            // 设置家
            COMMAND_SET_HOME = SERVER_BUILDER
                    .comment("The command to set the home."
                            , "设置家的指令。")
                    .define("commandTpHomeSet", "sethome");

            // 删除家
            COMMAND_DEL_HOME = SERVER_BUILDER
                    .comment("The command to delete the home."
                            , "删除家的指令。")
                    .define("commandTpHomeDel", "delhome");

            // 查询家
            COMMAND_GET_HOME = SERVER_BUILDER
                    .comment("The command to get the home info."
                            , "查询家的信息的指令。")
                    .define("commandTpHomeGet", "gethome");
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the stage", "传送到驿站").push("TpStage");
            // 传送到驿站
            COMMAND_TP_STAGE = SERVER_BUILDER
                    .comment("The command to teleport to the stage."
                            , "传送到驿站的指令。")
                    .define("commandTpStage", "stage");

            // 设置驿站
            COMMAND_SET_STAGE = SERVER_BUILDER
                    .comment("The command to set the stage."
                            , "设置驿站的指令。")
                    .define("commandTpStageSet", "setstage");

            // 删除驿站
            COMMAND_DEL_STAGE = SERVER_BUILDER
                    .comment("The command to delete the stage."
                            , "删除驿站的指令。")
                    .define("commandTpStageDel", "delstage");

            // 查询驿站
            COMMAND_GET_STAGE = SERVER_BUILDER
                    .comment("The command to get the stage info."
                            , "查询驿站的信息的的指令。")
                    .define("commandTpStageGet", "getstage");
            SERVER_BUILDER.pop();

            // 传送到上次传送点
            COMMAND_TP_BACK = SERVER_BUILDER
                    .comment("The command to teleport to the previous location."
                            , "传送到上次传送点的指令。")
                    .define("commandTpBack", "back");

            // 设置虚拟权限
            COMMAND_VIRTUAL_OP = SERVER_BUILDER
                    .comment("The command to set virtual permission."
                            , "设置虚拟权限的指令。")
                    .define("commandVirtualOp", "opv");

            SERVER_BUILDER.pop();
        }

        // 定义简化指令
        {
            SERVER_BUILDER.comment("Concise Command Settings", "简化指令").push("concise");

            CONCISE_UUID = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Get the UUID of the player' command.",
                            "是否启用无前缀版本的 '获取玩家的UUID' 指令。")
                    .define("conciseUuid", false);

            CONCISE_DIMENSION = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Get the dimension ID of the current world' command.",
                            "是否启用无前缀版本的 '获取当前世界的维度ID' 指令。")
                    .define("conciseDimension", false);

            CONCISE_FEED = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Suicide or poisoning' command.",
                            "是否启用无前缀版本的 '自杀或毒杀' 指令。")
                    .define("conciseFeed", false);

            CONCISE_TP_COORDINATE = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the specified coordinates' command.",
                            "是否启用无前缀版本的 '传送到指定坐标' 指令。")
                    .define("conciseTpCoordinate", true);

            CONCISE_TP_STRUCTURE = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the specified structure' command.",
                            "是否启用无前缀版本的 '传送到指定结构' 指令。")
                    .define("conciseTpStructure", true);

            SERVER_BUILDER.comment("Request to teleport oneself to other players", "请求传送至玩家").push("TpAsk");
            CONCISE_TP_ASK = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Request to teleport oneself to other players' command.",
                            "是否启用无前缀版本的 '请求传送至玩家' 指令。")
                    .define("conciseTpAsk", true);

            CONCISE_TP_ASK_YES = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Accept teleportation of other players to oneself' command.",
                            "是否启用无前缀版本的 '接受请求传送至玩家' 指令。")
                    .define("conciseTpAskYes", true);

            CONCISE_TP_ASK_NO = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Refuse teleportation of other players to oneself' command.",
                            "是否启用无前缀版本的 '拒绝请求传送至玩家' 指令。")
                    .define("conciseTpAskNo", false);
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Request the transfer of other players to oneself", "请求将玩家传送至当前位置").push("TpHere");
            CONCISE_TP_HERE = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Request the transfer of other players to oneself' command.",
                            "是否启用无前缀版本的 '请求将玩家传送至当前位置' 指令。")
                    .define("conciseTpHere", true);

            CONCISE_TP_HERE_YES = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Accept teleportation to other players' command.",
                            "是否启用无前缀版本的 '接受请求将玩家传送至当前位置' 指令。")
                    .define("conciseTpHereYes", true);

            CONCISE_TP_HERE_NO = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Refuse teleportation to other players' command.",
                            "是否启用无前缀版本的 '拒绝请求将玩家传送至当前位置' 指令。")
                    .define("conciseTpHereNo", false);
            SERVER_BUILDER.pop();

            CONCISE_TP_RANDOM = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to a random location' command.",
                            "是否启用无前缀版本的 '随机传送' 指令。")
                    .define("conciseTpRandom", false);

            CONCISE_TP_SPAWN = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the spawn of the player' command.",
                            "是否启用无前缀版本的 '传送到玩家重生点' 指令。")
                    .define("conciseTpSpawn", true);

            CONCISE_TP_WORLD_SPAWN = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the spawn of the world' command.",
                            "是否启用无前缀版本的 '传送到世界重生点' 指令。")
                    .define("conciseTpWorldSpawn", false);

            CONCISE_TP_TOP = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the top of current position' command.",
                            "是否启用无前缀版本的 '传送到顶部' 指令。")
                    .define("conciseTpTop", false);

            CONCISE_TP_BOTTOM = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the bottom of current position' command.",
                            "是否启用无前缀版本的 '传送到底部' 指令。")
                    .define("conciseTpBottom", false);

            CONCISE_TP_UP = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the upper of current position' command.",
                            "是否启用无前缀版本的 '传送到上方' 指令。")
                    .define("conciseTpUp", false);

            CONCISE_TP_DOWN = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the lower of current position' command.",
                            "是否启用无前缀版本的 '传送到下方' 指令。")
                    .define("conciseTpDown", false);

            CONCISE_TP_VIEW = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the end of the line of sight' command.",
                            "This function is independent of the player's render distance setting."
                            , "是否启用无前缀版本的 '传送至视线尽头' 指令。"
                            , "该功能与玩家设置的视距无关。")
                    .define("conciseTpView", false);

            SERVER_BUILDER.comment("Teleport to the home", "传送到家").push("TpHome");
            CONCISE_TP_HOME = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the home' command.",
                            "是否启用无前缀版本的 '传送到家' 指令。")
                    .define("conciseTpHome", true);

            CONCISE_SET_HOME = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Set the home' command.",
                            "是否启用无前缀版本的 '设置家' 指令。")
                    .define("conciseTpHomeSet", false);

            CONCISE_DEL_HOME = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Delete the home' command.",
                            "是否启用无前缀版本的 '删除家' 指令。")
                    .define("conciseTpHomeDel", false);

            CONCISE_GET_HOME = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Get the home info' command.",
                            "是否启用无前缀版本的 '查询家' 指令。")
                    .define("conciseTpHomeGet", false);
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the stage", "传送到驿站").push("TpStage");
            CONCISE_TP_STAGE = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the stage' command.",
                            "是否启用无前缀版本的 '传送到驿站' 指令。")
                    .define("conciseTpStage", true);

            CONCISE_SET_STAGE = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Set the stage' command.",
                            "是否启用无前缀版本的 '设置驿站' 指令。")
                    .define("conciseTpStageSet", false);

            CONCISE_DEL_STAGE = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Delete the stage' command.",
                            "是否启用无前缀版本的 '删除驿站' 指令。")
                    .define("conciseTpStageDel", false);

            CONCISE_GET_STAGE = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Get the stage info' command.",
                            "是否启用无前缀版本的 '查询驿站' 指令。")
                    .define("conciseTpStageGet", false);
            SERVER_BUILDER.pop();

            CONCISE_TP_BACK = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the previous location' command.",
                            "是否启用无前缀版本的 '传送到上次传送点' 指令。")
                    .define("conciseTpBack", true);

            CONCISE_VIRTUAL_OP = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Set virtual permission' command.",
                            "是否启用无前缀版本的 '设置虚拟权限' 指令。")
                    .define("conciseVirtualOp", false);

            SERVER_BUILDER.pop();

        }

        // 定义传送代价
        {
            SERVER_BUILDER.comment("Teleport Cost", "传送代价").push("cost");

            SERVER_BUILDER.comment("Teleport to the specified coordinates", "传送到指定坐标").push("TpCoordinate");
            {
                COST_TP_COORDINATE_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the specified coordinates'"
                                , "传送到指定坐标的代价类型。")
                        .defineEnum("costTpCoordinateType", ECostType.EXP_POINT);

                COST_TP_COORDINATE_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Teleport to the specified coordinates'"
                                , "传送到指定坐标的代价数量。")
                        .defineInRange("costTpCoordinateNum", 1, 0, 9999);

                COST_TP_COORDINATE_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Teleport to the specified coordinates'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "传送到指定坐标的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpCoordinateConf", "");

                COST_TP_COORDINATE_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Teleport to the specified coordinates', the cost will be multiplied by the distance between the two coordinates."
                                , "传送到指定坐标的代价倍率，代价会乘以传送前后坐标之间的距离。")
                        .defineInRange("costTpCoordinateRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the specified structure", "传送到指定结构").push("TpStructure");
            {
                COST_TP_STRUCTURE_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the specified structure'"
                                , "传送到指定结构的代价类型。")
                        .defineEnum("costTpStructureType", ECostType.EXP_POINT);

                COST_TP_STRUCTURE_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Teleport to the specified structure'"
                                , "传送到指定结构的代价数量。")
                        .defineInRange("costTpStructureNum", 1, 0, 9999);

                COST_TP_STRUCTURE_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Teleport to the specified structure'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "传送到指定结构的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpStructureConf", "");

                COST_TP_STRUCTURE_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Teleport to the specified structure', the cost will be multiplied by the distance between the two coordinates."
                                , "传送到指定结构的代价倍率，代价会乘以传送前后坐标之间的距离。")
                        .defineInRange("costTpStructureRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Request to teleport oneself to other players", "请求传送至玩家").push("TpAsk");
            {
                COST_TP_ASK_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Request to teleport oneself to other players'"
                                , "请求传送至玩家的代价类型。")
                        .defineEnum("costTpAskType", ECostType.EXP_POINT);

                COST_TP_ASK_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Request to teleport oneself to other players'"
                                , "请求传送至玩家的代价数量。")
                        .defineInRange("costTpAskNum", 1, 0, 9999);

                COST_TP_ASK_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Request to teleport oneself to other players'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "请求传送至玩家的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpAskConf", "");

                COST_TP_ASK_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Request to teleport oneself to other players', the cost will be multiplied by the distance between the two players"
                                , "请求传送至玩家的代价倍率，代价会乘以两个玩家之间的距离。")
                        .defineInRange("costTpAskRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Request the transfer of other players to oneself", "请求将玩家传送至当前位置").push("TpHere");
            {
                COST_TP_HERE_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Request the transfer of other players to oneself'"
                                , "请求将玩家传送至当前位置的代价类型。")
                        .defineEnum("costTpHereType", ECostType.EXP_POINT);

                COST_TP_HERE_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Request the transfer of other players to oneself'"
                                , "请求将玩家传送至当前位置的代价数量。")
                        .defineInRange("costTpHereNum", 1, 0, 9999);

                COST_TP_HERE_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Request the transfer of other players to oneself'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "请求将玩家传送至当前位置的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpHereConf", "");

                COST_TP_HERE_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Request the transfer of other players to oneself', the cost will be multiplied by the distance between the two players"
                                , "请求将玩家传送至当前位置的代价倍率，代价会乘以两个玩家之间的距离。")
                        .defineInRange("costTpHereRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to a random location", "随机传送").push("TpRandom");
            {
                COST_TP_RANDOM_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to a random location'"
                                , "随机传送的代价类型。")
                        .defineEnum("costTpRandomType", ECostType.EXP_POINT);

                COST_TP_RANDOM_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Teleport to a random location'"
                                , "随机传送的代价数量。")
                        .defineInRange("costTpRandomNum", 1, 0, 9999);

                COST_TP_RANDOM_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Teleport to a random location'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "随机传送的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpRandomConf", "");

                COST_TP_RANDOM_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Teleport to a random location', the cost will be multiplied by the distance between the two coordinates."
                                , "随机传送的代价倍率，代价会乘以传送前后坐标之间的距离。")
                        .defineInRange("costTpRandomRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the spawn of the player", "传送到玩家重生点").push("TpSpawn");
            {
                COST_TP_SPAWN_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the spawn of the player'"
                                , "传送到玩家重生点的代价类型。")
                        .defineEnum("costTpSpawnType", ECostType.EXP_POINT);

                COST_TP_SPAWN_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Teleport to the spawn of the player'"
                                , "传送到玩家重生点的代价数量。")
                        .defineInRange("costTpSpawnNum", 1, 0, 9999);

                COST_TP_SPAWN_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Teleport to the spawn of the player'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "传送到玩家重生点的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpSpawnConf", "");

                COST_TP_SPAWN_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Teleport to the spawn of the player', the cost will be multiplied by the distance between the two coordinates."
                                , "传送到玩家重生点的代价倍率，代价会乘以传送前后坐标之间的距离。")
                        .defineInRange("costTpSpawnRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the spawn of the world", "传送到世界重生点").push("TpWorldSpawn");
            {
                COST_TP_WORLD_SPAWN_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the spawn of the world'"
                                , "传送到世界重生点的代价类型。")
                        .defineEnum("costTpWorldSpawnType", ECostType.EXP_POINT);

                COST_TP_WORLD_SPAWN_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Teleport to the spawn of the world'"
                                , "传送到世界重生点的代价数量。")
                        .defineInRange("costTpWorldSpawnNum", 1, 0, 9999);

                COST_TP_WORLD_SPAWN_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Teleport to the spawn of the world'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "传送到世界重生点的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpWorldSpawnConf", "");

                COST_TP_WORLD_SPAWN_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Teleport to the spawn of the world', the cost will be multiplied by the distance between the two coordinates."
                                , "传送到世界重生点的代价倍率，代价会乘以传送前后坐标之间的距离。")
                        .defineInRange("costTpWorldSpawnRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the top of current position", "传送到顶部").push("TpTop");
            {
                COST_TP_TOP_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the top of current position'"
                                , "传送到顶部的代价类型。")
                        .defineEnum("costTpTopType", ECostType.EXP_POINT);

                COST_TP_TOP_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Teleport to the top of current position'"
                                , "传送到顶部的代价数量。")
                        .defineInRange("costTpTopNum", 1, 0, 9999);

                COST_TP_TOP_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Teleport to the top of current position'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "传送到顶部的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpTopConf", "");

                COST_TP_TOP_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Teleport to the top of current position', the cost will be multiplied by the distance between the two coordinates."
                                , "传送到顶部的代价倍率，代价会乘以传送前后坐标之间的距离。")
                        .defineInRange("costTpTopRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the bottom of current position", "传送到底部").push("TpBottom");
            {
                COST_TP_BOTTOM_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the bottom of current position'"
                                , "传送到底部的代价类型。")
                        .defineEnum("costTpBottomType", ECostType.EXP_POINT);

                COST_TP_BOTTOM_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Teleport to the bottom of current position'"
                                , "传送到底部的代价数量。")
                        .defineInRange("costTpBottomNum", 1, 0, 9999);

                COST_TP_BOTTOM_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Teleport to the bottom of current position'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "传送到底部的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpBottomConf", "");

                COST_TP_BOTTOM_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Teleport to the bottom of current position', the cost will be multiplied by the distance between the two coordinates."
                                , "传送到底部的代价倍率，代价会乘以传送前后坐标之间的距离。")
                        .defineInRange("costTpBottomRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the upper of current position", "传送到上方").push("TpUp");
            {
                COST_TP_UP_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the upper of current position'"
                                , "传送到上方的代价类型。")
                        .defineEnum("costTpUpType", ECostType.EXP_POINT);

                COST_TP_UP_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Teleport to the upper of current position'"
                                , "传送到上方的代价数量。")
                        .defineInRange("costTpUpNum", 1, 0, 9999);

                COST_TP_UP_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Teleport to the upper of current position'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "传送到上方的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpUpConf", "");

                COST_TP_UP_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Teleport to the upper of current position', the cost will be multiplied by the distance between the two coordinates."
                                , "传送到上方的代价倍率，代价会乘以传送前后坐标之间的距离。")
                        .defineInRange("costTpUpRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the lower of current position", "传送到下方").push("TpDown");
            {
                COST_TP_DOWN_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the lower of current position'"
                                , "传送到下方的代价类型。")
                        .defineEnum("costTpDownType", ECostType.EXP_POINT);

                COST_TP_DOWN_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Teleport to the lower of current position'"
                                , "传送到下方的代价数量。")
                        .defineInRange("costTpDownNum", 1, 0, 9999);

                COST_TP_DOWN_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Teleport to the lower of current position'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "传送到下方的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpDownConf", "");

                COST_TP_DOWN_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Teleport to the lower of current position', the cost will be multiplied by the distance between the two coordinates."
                                , "传送到下方的代价倍率，代价会乘以传送前后坐标之间的距离。")
                        .defineInRange("costTpDownRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the end of the line of sight", "This function is independent of the player's render distance setting.", "传送至视线尽头", "该功能与玩家设置的视距无关。").push("TpView");
            {
                COST_TP_VIEW_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the end of the line of sight'"
                                , "传送至视线尽头的代价类型。")
                        .defineEnum("costTpViewType", ECostType.EXP_POINT);

                COST_TP_VIEW_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Teleport to the end of the line of sight'"
                                , "传送至视线尽头的代价数量。")
                        .defineInRange("costTpViewNum", 1, 0, 9999);

                COST_TP_VIEW_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Teleport to the end of the line of sight'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "传送至视线尽头的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpViewConf", "");

                COST_TP_VIEW_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Teleport to the end of the line of sight', the cost will be multiplied by the distance between the two coordinates."
                                , "传送至视线尽头的代价倍率，代价会乘以传送前后坐标之间的距离。")
                        .defineInRange("costTpViewRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the home", "传送到家").push("TpHome");
            {
                COST_TP_HOME_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the home'"
                                , "传送到家的代价类型。")
                        .defineEnum("costTpHomeType", ECostType.EXP_POINT);

                COST_TP_HOME_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Teleport to the home'"
                                , "传送到家的代价数量。")
                        .defineInRange("costTpHomeNum", 1, 0, 9999);

                COST_TP_HOME_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Teleport to the home'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "传送到家的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpHomeConf", "");

                COST_TP_HOME_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Teleport to the home', the cost will be multiplied by the distance between the two coordinates."
                                , "传送到家的代价倍率，代价会乘以传送前后坐标之间的距离。")
                        .defineInRange("costTpHomeRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the stage", "传送到驿站").push("TpStage");
            {
                COST_TP_STAGE_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the stage'"
                                , "传送到驿站的代价类型。")
                        .defineEnum("costTpStageType", ECostType.EXP_POINT);

                COST_TP_STAGE_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Teleport to the stage'"
                                , "传送到驿站的代价数量。")
                        .defineInRange("costTpStageNum", 1, 0, 9999);

                COST_TP_STAGE_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Teleport to the stage'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "传送到驿站的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpStageConf", "");

                COST_TP_STAGE_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Teleport to the stage', the cost will be multiplied by the distance between the two coordinates."
                                , "传送到驿站的代价倍率，代价会乘以传送前后坐标之间的距离。")
                        .defineInRange("costTpStageRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the previous location", "传送到上次传送点").push("TpBack");
            {
                COST_TP_BACK_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the previous location'"
                                , "传送到上次传送点的代价类型。")
                        .defineEnum("costTpBackType", ECostType.HUNGER);

                COST_TP_BACK_NUM = SERVER_BUILDER
                        .comment("The number of cost for 'Teleport to the previous location'"
                                , "传送到上次传送点的代价数量。")
                        .defineInRange("costTpBackNum", 1, 0, 9999);

                COST_TP_BACK_CONF = SERVER_BUILDER
                        .comment("The configuration for 'Teleport to the previous location'.",
                                "If the type is ITEM, the value should be the item ID with optional NBT data.",
                                "If the type is COMMAND, the value should be a specific command string.",
                                "In the command, the placeholder '[num]' can be used to represent the cost amount.",
                                "传送到上次传送点的代价配置：",
                                "若类型为 ITEM，则值为物品 ID，可包含 NBT 数据。",
                                "若类型为 COMMAND，则值为具体的指令字符串。",
                                "在指令中，可使用占位符 '[num]' 来表示代价数量。")
                        .define("costTpBackConf", "");

                COST_TP_BACK_RATE = SERVER_BUILDER
                        .comment("The cost rate for 'Teleport to the previous location', the cost will be multiplied by the distance between the two coordinates."
                                , "传送到上次传送点的代价倍率，代价会乘以传送前后坐标之间的距离。")
                        .defineInRange("costTpBackRate", 0.001, 0, 9999);
            }
            SERVER_BUILDER.pop();

            SERVER_BUILDER.pop();
        }

        SERVER_CONFIG = SERVER_BUILDER.build();
    }

}
