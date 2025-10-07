package xin.vanilla.narcissus.config;

import net.minecraftforge.common.ForgeConfigSpec;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.enums.EnumCardType;

/**
 * 服务器配置
 */
public class CommonConfig {

    public static final ForgeConfigSpec COMMON_CONFIG;

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
    public static final ForgeConfigSpec.ConfigValue<String> TELEPORT_CARD_TYPE;

    /**
     * 是否禁用原版TP指令
     */
    public static final ForgeConfigSpec.BooleanValue REMOVE_ORIGINAL_TP;

    /**
     * 创造飞行最低速度
     */
    public static final ForgeConfigSpec.DoubleValue FLY_SPEED_MIN;
    /**
     * 创造飞行最高速度
     */
    public static final ForgeConfigSpec.DoubleValue FLY_SPEED_MAX;

    // endregion 基础设置


    // region 功能开关

    /**
     * 分享坐标 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_SHARE;

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

    /**
     * 创造飞行 开关
     */
    public static final ForgeConfigSpec.BooleanValue SWITCH_FLY;

    // endregion 功能开关


    // region 自定义指令

    /**
     * 命令前缀
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_PREFIX;

    /**
     * 设置语言
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_LANGUAGE;

    /**
     * 获取玩家的UUID
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_UUID;

    /**
     * 获取当前世界的维度ID
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_DIMENSION;

    /**
     * 获取传送卡数量
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_CARD;

    /**
     * 分享坐标
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_SHARE;

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
     * 取消传送至玩家的请求
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_ASK_CANCEL;

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
     * 取消将玩家传送至当前位置的请求
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_TP_HERE_CANCEL;

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
     * 创造飞行
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_FLY;

    /**
     * 设置虚拟权限
     */
    public static final ForgeConfigSpec.ConfigValue<String> COMMAND_VIRTUAL_OP;

    // endregion 自定义指令


    // region 简化指令

    /**
     * 设置语言
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_LANGUAGE;

    /**
     * 获取玩家的UUID
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_UUID;

    /**
     * 获取当前世界的维度ID
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_DIMENSION;

    /**
     * 获取传送卡数量
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_CARD;

    /**
     * 分享坐标
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_SHARE;

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
     * 取消传送至玩家的请求
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_ASK_CANCEL;

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
     * 取消将玩家传送至当前位置的请求
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_TP_HERE_CANCEL;

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
     * 创造飞行
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_FLY;

    /**
     * 设置虚拟权限
     */
    public static final ForgeConfigSpec.BooleanValue CONCISE_VIRTUAL_OP;


    // endregion 简化指令


    static {
        ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();

        // 定义服务器基础设置
        {
            SERVER_BUILDER.comment("Base Settings", "基础设置").push("common");

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
                    .comment("Teleport Card Usage Modes:",
                            "NONE: A teleport will consume one teleport card and also charge the teleport cost. If there are not enough teleport cards, the teleport will fail.",
                            "LIKE_COST: A teleport will consume the same number of teleport cards as the teleport cost, and also charge the teleport cost. If there are not enough cards, the teleport will fail.",
                            "REFUND_COST: A teleport will consume teleport cards at a 1:1 ratio to offset the teleport cost. If cards are insufficient, the remaining cost will be charged.",
                            "REFUND_ALL_COST: A teleport will consume one teleport card to offset the entire teleport cost. If there are not enough cards, the full cost will be charged.",
                            "REFUND_COOLDOWN: A teleport will consume one teleport card to skip the cooldown period, but the teleport cost still applies.",
                            "REFUND_COST_AND_COOLDOWN: A teleport will consume teleport cards at a 1:1 ratio to offset both the teleport cost and cooldown. If cards are insufficient, the remaining cost will be charged.",
                            "REFUND_ALL_COST_AND_COOLDOWN: A teleport will consume one teleport card to offset all teleport costs and cooldown. If there are not enough cards, the full cost will be charged.",
                            "If both teleport cards and cost are insufficient, the teleport will fail. If you want teleport cards to cover the cost but prevent teleporting when insufficient, please set the cost to zero in the config.",
                            "传送卡的使用方式：",
                            "NONE: 传送会消耗一张传送卡，并同时收取代价。若传送卡不足，则无法传送。",
                            "LIKE_COST: 传送会消耗与代价数量一致的传送卡，并同时收取代价。若传送卡不足，则无法传送。",
                            "REFUND_COST: 传送会按一比一比例消耗传送卡以抵消对应代价。若传送卡不足，则收取剩余代价。",
                            "REFUND_ALL_COST: 传送会消耗一张传送卡以抵消所有代价。若传送卡不足，则收取对应代价。",
                            "REFUND_COOLDOWN: 传送会消耗一张传送卡并抵消冷却时间，但仍需支付代价。",
                            "REFUND_COST_AND_COOLDOWN: 传送会按一比一比例消耗传送卡以抵消对应代价并同时抵消冷却时间。若传送卡不足，则收取剩余代价。",
                            "REFUND_ALL_COST_AND_COOLDOWN: 传送会消耗一张传送卡以抵消所有代价并同时抵消冷却时间。若传送卡不足，则收取对应代价。",
                            "若传送卡与代价都不足，则无法传送。若希望传送卡能够抵消代价但在不足时禁止传送，请在配置中将代价设置为零。")
                    .define("teleportCardType", EnumCardType.REFUND_ALL_COST.name());

            // 是否禁用原版TP指令
            REMOVE_ORIGINAL_TP = SERVER_BUILDER
                    .comment("Whether to disable the original TP command.",
                            "是否禁用原版TP指令。")
                    .define("removeOriginalTp", false);

            // 创造飞行最低速度
            FLY_SPEED_MIN = SERVER_BUILDER
                    .comment("The minimum speed of creative flight.",
                            "创造飞行允许设置的最低速度。")
                    .defineInRange("flySpeedMin", -5d, -1.0f * Integer.MAX_VALUE, 1.0f * Integer.MAX_VALUE);

            // 创造飞行最高速度
            FLY_SPEED_MAX = SERVER_BUILDER
                    .comment("The maximum speed of creative flight.",
                            "创造飞行允许设置的最高速度。")
                    .defineInRange("flySpeedMax", 5d, -1.0f * Integer.MAX_VALUE, 1.0f * Integer.MAX_VALUE);

            SERVER_BUILDER.pop();
        }


        // 定义功能开关
        {
            SERVER_BUILDER.comment("Function Switch", "功能开关").push("switch");

            SWITCH_SHARE = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Share coordinate'."
                            , "是否启用坐标分享。")
                    .define("switchShare", true);

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

            SWITCH_FLY = SERVER_BUILDER
                    .comment("Enable or disable the option to 'Fly'."
                            , "是否启用创造飞行指令。")
                    .define("switchFly", true);

            SERVER_BUILDER.pop();
        }


        // 定义自定义指令配置
        {
            SERVER_BUILDER.comment("Custom Command Settings, don't add prefix '/'", "自定义指令，请勿添加前缀'/'").push("command");

            // 命令前缀
            COMMAND_PREFIX = SERVER_BUILDER
                    .comment("The prefix of the command, please only use English characters and underscores, otherwise it may cause problems.",
                            "指令前缀，请仅使用英文字母及下划线，否则可能会出现问题。")
                    .define("commandPrefix", NarcissusFarewell.DEFAULT_COMMAND_PREFIX);

            // 设置语言
            COMMAND_LANGUAGE = SERVER_BUILDER
                    .comment("This command is used to set the language."
                            , "设置语言的指令。")
                    .define("commandLanguage", "language");

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

            // 获取传送卡数量
            COMMAND_CARD = SERVER_BUILDER
                    .comment("This command is used to get the number of Teleport Card."
                            , "获取传送卡数量的指令。")
                    .define("commandCard", "card");

            // 分享坐标
            COMMAND_SHARE = SERVER_BUILDER
                    .comment("This command is used to share the stage, the personal home, and the current coordinate of player."
                            , "分享驿站、玩家的私人传送点、玩家当前坐标的指令。")
                    .define("commandShare", "share");

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

            COMMAND_TP_ASK_CANCEL = SERVER_BUILDER
                    .comment("This command is used to cancel the request to teleport to other players."
                            , "取消请求传送至玩家的指令。")
                    .define("commandTpAskCancel", "tpac");
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

            COMMAND_TP_HERE_CANCEL = SERVER_BUILDER
                    .comment("This command is used to cancel the request to teleport to other players."
                            , "取消请求将玩家传送至当前位置的指令。")
                    .define("commandTpHereCancel", "tphc");
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

            COMMAND_FLY = SERVER_BUILDER
                    .comment("The command to fly."
                            , "开启创造飞行的指令。")
                    .define("commandFly", "fly");

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

            CONCISE_LANGUAGE = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Set the language' command.",
                            "是否启用无前缀版本的 '设置语言' 指令。")
                    .define("conciseLanguage", false);

            CONCISE_UUID = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Get the UUID of the player' command.",
                            "是否启用无前缀版本的 '获取玩家的UUID' 指令。")
                    .define("conciseUuid", false);

            CONCISE_DIMENSION = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Get the dimension ID of the current world' command.",
                            "是否启用无前缀版本的 '获取当前世界的维度ID' 指令。")
                    .define("conciseDimension", false);

            CONCISE_CARD = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Get the number of Teleport Card of the player' command.",
                            "是否启用无前缀版本的 '获取玩家的传送卡数量' 指令。")
                    .define("conciseCard", false);

            CONCISE_SHARE = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Share the stage, the personal home, and the current coordinate of player' command.",
                            "是否启用无前缀版本的 '分享驿站、玩家的私人传送点、玩家当前坐标' 指令。")
                    .define("conciseShare", false);

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
                    .define("conciseTpAskNo", true);

            CONCISE_TP_ASK_CANCEL = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Cancel the request to teleport to other players' command.",
                            "是否启用无前缀版本的 '取消请求传送至玩家' 指令。")
                    .define("conciseTpAskCancel", true);
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
                    .define("conciseTpHereNo", true);

            CONCISE_TP_HERE_CANCEL = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Cancel the request to teleport to other players' command.",
                            "是否启用无前缀版本的 '取消请求将玩家传送至当前位置' 指令。")
                    .define("conciseTpHereCancel", true);
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
                    .define("conciseTpHomeSet", true);

            CONCISE_DEL_HOME = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Delete the home' command.",
                            "是否启用无前缀版本的 '删除家' 指令。")
                    .define("conciseTpHomeDel", true);

            CONCISE_GET_HOME = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Get the home info' command.",
                            "是否启用无前缀版本的 '查询家' 指令。")
                    .define("conciseTpHomeGet", true);
            SERVER_BUILDER.pop();

            SERVER_BUILDER.comment("Teleport to the stage", "传送到驿站").push("TpStage");
            CONCISE_TP_STAGE = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the stage' command.",
                            "是否启用无前缀版本的 '传送到驿站' 指令。")
                    .define("conciseTpStage", true);

            CONCISE_SET_STAGE = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Set the stage' command.",
                            "是否启用无前缀版本的 '设置驿站' 指令。")
                    .define("conciseTpStageSet", true);

            CONCISE_DEL_STAGE = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Delete the stage' command.",
                            "是否启用无前缀版本的 '删除驿站' 指令。")
                    .define("conciseTpStageDel", true);

            CONCISE_GET_STAGE = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Get the stage info' command.",
                            "是否启用无前缀版本的 '查询驿站' 指令。")
                    .define("conciseTpStageGet", true);
            SERVER_BUILDER.pop();

            CONCISE_TP_BACK = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Teleport to the previous location' command.",
                            "是否启用无前缀版本的 '传送到上次传送点' 指令。")
                    .define("conciseTpBack", true);

            CONCISE_FLY = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Fly' command.",
                            "是否启用无前缀版本的 '创造飞行' 指令。")
                    .define("conciseFly", true);

            CONCISE_VIRTUAL_OP = SERVER_BUILDER
                    .comment("Enable or disable the concise version of the 'Set virtual permission' command.",
                            "是否启用无前缀版本的 '设置虚拟权限' 指令。")
                    .define("conciseVirtualOp", false);

            SERVER_BUILDER.pop();

        }

        COMMON_CONFIG = SERVER_BUILDER.build();
    }

    /**
     * 重置服务器配置文件
     */
    public static void resetConfig() {
        TELEPORT_CARD.set(false);
        TELEPORT_CARD_DAILY.set(0);
        TELEPORT_CARD_TYPE.set(EnumCardType.REFUND_ALL_COST.name());
        REMOVE_ORIGINAL_TP.set(false);
        FLY_SPEED_MIN.set(-5d);
        FLY_SPEED_MAX.set(5d);

        SWITCH_SHARE.set(true);
        SWITCH_FEED.set(true);
        SWITCH_TP_COORDINATE.set(true);
        SWITCH_TP_STRUCTURE.set(true);
        SWITCH_TP_ASK.set(true);
        SWITCH_TP_HERE.set(true);
        SWITCH_TP_RANDOM.set(true);
        SWITCH_TP_SPAWN.set(true);
        SWITCH_TP_WORLD_SPAWN.set(true);
        SWITCH_TP_TOP.set(true);
        SWITCH_TP_BOTTOM.set(true);
        SWITCH_TP_UP.set(true);
        SWITCH_TP_DOWN.set(true);
        SWITCH_TP_VIEW.set(true);
        SWITCH_TP_HOME.set(true);
        SWITCH_TP_STAGE.set(true);
        SWITCH_TP_BACK.set(true);
        SWITCH_FLY.set(true);

        COMMAND_PREFIX.set(NarcissusFarewell.DEFAULT_COMMAND_PREFIX);
        COMMAND_LANGUAGE.set("language");
        COMMAND_UUID.set("uuid");
        COMMAND_DIMENSION.set("dim");
        COMMAND_CARD.set("card");
        COMMAND_SHARE.set("share");
        COMMAND_FEED.set("feed");
        COMMAND_TP_COORDINATE.set("tpx");
        COMMAND_TP_STRUCTURE.set("tpst");
        COMMAND_TP_ASK.set("tpa");
        COMMAND_TP_ASK_YES.set("tpay");
        COMMAND_TP_ASK_NO.set("tpan");
        COMMAND_TP_ASK_CANCEL.set("tpac");
        COMMAND_TP_HERE.set("tph");
        COMMAND_TP_HERE_YES.set("tphy");
        COMMAND_TP_HERE_NO.set("tphn");
        COMMAND_TP_HERE_CANCEL.set("tphc");
        COMMAND_TP_RANDOM.set("tpr");
        COMMAND_TP_SPAWN.set("tpsp");
        COMMAND_TP_WORLD_SPAWN.set("tpws");
        COMMAND_TP_TOP.set("tpt");
        COMMAND_TP_BOTTOM.set("tpb");
        COMMAND_TP_UP.set("tpu");
        COMMAND_TP_DOWN.set("tpd");
        COMMAND_TP_VIEW.set("tpv");
        COMMAND_TP_HOME.set("home");
        COMMAND_SET_HOME.set("sethome");
        COMMAND_DEL_HOME.set("delhome");
        COMMAND_GET_HOME.set("gethome");
        COMMAND_TP_STAGE.set("stage");
        COMMAND_SET_STAGE.set("setstage");
        COMMAND_DEL_STAGE.set("delstage");
        COMMAND_GET_STAGE.set("getstage");
        COMMAND_TP_BACK.set("back");
        COMMAND_FLY.set("fly");
        COMMAND_VIRTUAL_OP.set("opv");

        CONCISE_LANGUAGE.set(false);
        CONCISE_UUID.set(false);
        CONCISE_DIMENSION.set(false);
        CONCISE_CARD.set(false);
        CONCISE_SHARE.set(false);
        CONCISE_FEED.set(false);
        CONCISE_TP_COORDINATE.set(true);
        CONCISE_TP_STRUCTURE.set(true);
        CONCISE_TP_ASK.set(true);
        CONCISE_TP_ASK_YES.set(true);
        CONCISE_TP_ASK_NO.set(true);
        CONCISE_TP_ASK_CANCEL.set(true);
        CONCISE_TP_HERE.set(true);
        CONCISE_TP_HERE_YES.set(true);
        CONCISE_TP_HERE_NO.set(true);
        CONCISE_TP_HERE_CANCEL.set(true);
        CONCISE_TP_RANDOM.set(false);
        CONCISE_TP_SPAWN.set(true);
        CONCISE_TP_WORLD_SPAWN.set(false);
        CONCISE_TP_TOP.set(false);
        CONCISE_TP_BOTTOM.set(false);
        CONCISE_TP_UP.set(false);
        CONCISE_TP_DOWN.set(false);
        CONCISE_TP_VIEW.set(false);
        CONCISE_TP_HOME.set(true);
        CONCISE_SET_HOME.set(true);
        CONCISE_DEL_HOME.set(true);
        CONCISE_GET_HOME.set(true);
        CONCISE_TP_STAGE.set(true);
        CONCISE_SET_STAGE.set(true);
        CONCISE_DEL_STAGE.set(true);
        CONCISE_GET_STAGE.set(true);
        CONCISE_TP_BACK.set(true);
        CONCISE_FLY.set(true);
        CONCISE_VIRTUAL_OP.set(false);

        COMMON_CONFIG.save();
    }

    /**
     * 经典模式</br></br>
     * 驿站指令改为warp</br>
     * back返回时不再忽略使用back指令产生的传送记录</br>
     * 操作家与驿站的前缀后移为后缀，使之更易于输入
     */
    public static void resetConfigWithMode1() {
        resetConfig();

        COMMAND_TP_HOME.set("home");
        COMMAND_SET_HOME.set("home_set");
        COMMAND_DEL_HOME.set("home_del");
        COMMAND_GET_HOME.set("home_get");

        COMMAND_TP_STAGE.set("warp");
        COMMAND_SET_STAGE.set("warp_set");
        COMMAND_DEL_STAGE.set("warp_del");
        COMMAND_GET_STAGE.set("warp_get");

        COMMAND_TP_TOP.set("top");
        COMMAND_TP_UP.set("up");
        COMMAND_TP_DOWN.set("down");
        COMMAND_TP_BOTTOM.set("bottom");

        COMMON_CONFIG.save();
    }

    /**
     * 简洁模式</br></br>
     * 在经典模式的基础上禁用不常用的功能
     */
    public static void resetConfigWithMode2() {
        resetConfigWithMode1();

        SWITCH_FEED.set(false);
        SWITCH_TP_STRUCTURE.set(false);
        SWITCH_TP_RANDOM.set(false);
        SWITCH_TP_SPAWN.set(false);
        SWITCH_TP_WORLD_SPAWN.set(false);
        SWITCH_TP_BOTTOM.set(false);
        SWITCH_TP_DOWN.set(false);
        SWITCH_TP_UP.set(false);
        SWITCH_TP_VIEW.set(false);

        COMMON_CONFIG.save();
    }

    /**
     * 进阶模式</br></br>
     * 使用推荐的配置，并启用传送代价
     */
    public static void resetConfigWithMode3() {
        resetConfig();

        CONCISE_TP_ASK_CANCEL.set(false);
        CONCISE_TP_HERE_CANCEL.set(false);
        CONCISE_TP_RANDOM.set(false);
        CONCISE_TP_SPAWN.set(false);
        CONCISE_TP_WORLD_SPAWN.set(false);
        CONCISE_TP_TOP.set(false);
        CONCISE_TP_UP.set(false);
        CONCISE_TP_BOTTOM.set(false);
        CONCISE_TP_DOWN.set(false);
        CONCISE_TP_VIEW.set(false);

        COMMON_CONFIG.save();
    }

}
