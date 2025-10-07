package xin.vanilla.narcissus.config;

import net.minecraft.block.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.common.ForgeConfigSpec;
import xin.vanilla.narcissus.enums.EnumCoolDownType;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.ArrayList;
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
     * 历史传送记录数量限制
     */
    public static final ForgeConfigSpec.IntValue TELEPORT_RECORD_LIMIT;
    /**
     * back指令默认忽略的传送类型
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TELEPORT_BACK_SKIP_TYPE;
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
    public static final ForgeConfigSpec.ConfigValue<String> TELEPORT_REQUEST_COOLDOWN_TYPE;

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
     * 不安全的方块
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> UNSAFE_BLOCKS;

    /**
     * 窒息的方块
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SUFFOCATING_BLOCKS;

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
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> SAFE_BLOCKS;

    /**
     * 寻找安全坐标的区块范围
     */
    public static final ForgeConfigSpec.IntValue SAFE_CHUNK_RANGE;

    /**
     * 帮助指令信息头部内容
     */
    public static final ForgeConfigSpec.ConfigValue<String> HELP_HEADER;

    /**
     * 传送音效
     */
    public static final ForgeConfigSpec.ConfigValue<String> TP_SOUND;

    /**
     * 是否允许载具一起传送
     */
    public static final ForgeConfigSpec.BooleanValue TP_WITH_VEHICLE;

    /**
     * 是否允许跟随的实体一起传送
     */
    public static final ForgeConfigSpec.BooleanValue TP_WITH_FOLLOWER;

    /**
     * 跟随的实体识别范围半径
     */
    public static final ForgeConfigSpec.IntValue TP_WITH_FOLLOWER_RANGE;

    /**
     * 帮助信息每页显示的数量
     */
    public static final ForgeConfigSpec.IntValue HELP_INFO_NUM_PER_PAGE;

    /**
     * 服务器默认语言
     */
    public static final ForgeConfigSpec.ConfigValue<String> DEFAULT_LANGUAGE;

    /**
     * 被敌对生物锁定时是否限制传送
     */
    public static final ForgeConfigSpec.BooleanValue TP_WITH_ENEMY;

    // endregion 基础设置


    // region 指令权限

    public static final ForgeConfigSpec.IntValue PERMISSION_FEED_OTHER;

    public static final ForgeConfigSpec.IntValue PERMISSION_TP_COORDINATE;

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

    public static final ForgeConfigSpec.IntValue PERMISSION_FLY;

    public static final ForgeConfigSpec.IntValue PERMISSION_VIRTUAL_OP;

    public static final ForgeConfigSpec.IntValue PERMISSION_SET_CARD;

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


    // region 传送代价

    /**
     * 代价类型
     */
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_COORDINATE_TYPE;
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
    /**
     * 代价数量上限
     */
    public static final ForgeConfigSpec.IntValue COST_TP_COORDINATE_NUM_UPPER;
    /**
     * 代价数量下限
     */
    public static final ForgeConfigSpec.IntValue COST_TP_COORDINATE_NUM_LOWER;
    /**
     * 代价算法表达式
     */
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_COORDINATE_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_STRUCTURE_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_STRUCTURE_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_STRUCTURE_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_STRUCTURE_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_STRUCTURE_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_STRUCTURE_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_STRUCTURE_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_ASK_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_ASK_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_ASK_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_ASK_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_ASK_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_ASK_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_ASK_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_HERE_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_HERE_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_HERE_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_HERE_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_HERE_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_HERE_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_HERE_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_RANDOM_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_RANDOM_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_RANDOM_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_RANDOM_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_RANDOM_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_RANDOM_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_RANDOM_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_SPAWN_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_SPAWN_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_SPAWN_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_SPAWN_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_SPAWN_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_SPAWN_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_SPAWN_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_WORLD_SPAWN_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_WORLD_SPAWN_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_WORLD_SPAWN_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_WORLD_SPAWN_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_WORLD_SPAWN_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_WORLD_SPAWN_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_WORLD_SPAWN_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_TOP_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_TOP_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_TOP_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_TOP_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_TOP_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_TOP_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_TOP_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_BOTTOM_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_BOTTOM_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_BOTTOM_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_BOTTOM_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_BOTTOM_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_BOTTOM_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_BOTTOM_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_UP_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_UP_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_UP_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_UP_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_UP_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_UP_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_UP_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_DOWN_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_DOWN_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_DOWN_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_DOWN_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_DOWN_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_DOWN_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_DOWN_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_VIEW_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_VIEW_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_VIEW_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_VIEW_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_VIEW_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_VIEW_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_VIEW_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_HOME_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_HOME_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_HOME_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_HOME_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_HOME_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_HOME_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_HOME_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_STAGE_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_STAGE_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_STAGE_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_STAGE_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_STAGE_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_STAGE_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_STAGE_EXP;

    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_BACK_TYPE;
    public static final ForgeConfigSpec.IntValue COST_TP_BACK_NUM;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_BACK_CONF;
    public static final ForgeConfigSpec.DoubleValue COST_TP_BACK_RATE;
    public static final ForgeConfigSpec.IntValue COST_TP_BACK_NUM_UPPER;
    public static final ForgeConfigSpec.IntValue COST_TP_BACK_NUM_LOWER;
    public static final ForgeConfigSpec.ConfigValue<String> COST_TP_BACK_EXP;

    // endregion 传送代价


    static {
        ForgeConfigSpec.Builder SERVER_BUILDER = new ForgeConfigSpec.Builder();

        // 定义服务器基础设置
        {
            SERVER_BUILDER.comment("Base Settings", "基础设置").push("common");

            // 历史传送记录数量限制
            TELEPORT_RECORD_LIMIT = SERVER_BUILDER
                    .comment("The limit of teleport records, 0 means no limit."
                            , "传送记录数量限制，数量为0表示不限制。")
                    .defineInRange("teleportRecordLimit", 100, 0, 99999);

            // 传送指令默认忽略的类型
            TELEPORT_BACK_SKIP_TYPE = SERVER_BUILDER
                    .comment("The teleport back skip type."
                            , "传送回时忽略的传送类型。"
                            , "Allowed Values: " + EnumTeleportType.names())
                    .defineList("teleportBackSkipType", new ArrayList<String>() {{
                        add(EnumTeleportType.TP_BACK.name());
                    }}, s -> s instanceof String);

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
                            "MIXED：结合两种方式，同时使用全局冷却时间和单独冷却时间。",
                            "Allowed Values: " + EnumCoolDownType.names()
                    )
                    .define("teleportRequestCooldownType", EnumCoolDownType.INDIVIDUAL.name());

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

            // 帮助指令信息头部内容
            HELP_HEADER = SERVER_BUILDER
                    .comment("The header content of the help command.",
                            "帮助指令信息头部内容。")
                    .define("helpHeader", "-----==== Narcissus Farewell Help (%d/%d) ====-----");

            // 传送音效
            TP_SOUND = SERVER_BUILDER
                    .comment("The sound effect when teleporting.",
                            "传送时的音效。")
                    .define("tpSound", SoundEvents.ENDERMAN_TELEPORT.getRegistryName().toString());

            // 是否允许载具一起传送
            TP_WITH_VEHICLE = SERVER_BUILDER
                    .comment("Whether to allow vehicles to be teleported together.",
                            "是否允许载具一起传送。")
                    .define("tpWithVehicle", true);

            // 是否允许跟随的实体一起传送
            TP_WITH_FOLLOWER = SERVER_BUILDER
                    .comment("Whether to allow followers to be teleported together.",
                            "是否允许跟随的实体一起传送。")
                    .define("tpWithFollower", true);

            // 跟随的实体识别范围
            TP_WITH_FOLLOWER_RANGE = SERVER_BUILDER
                    .comment("The range of followers to be recognized, in blocks.",
                            "跟随的实体识别范围半径。")
                    .defineInRange("tpWithFollowerRange", 10, 1, 16 * 16);

            // 帮助信息每页显示的数量
            HELP_INFO_NUM_PER_PAGE = SERVER_BUILDER
                    .comment("The number of help information displayed per page.",
                            "每页显示的帮助信息数量。")
                    .defineInRange("helpInfoNumPerPage", 5, 1, 9999);

            // 服务器默认语言
            DEFAULT_LANGUAGE = SERVER_BUILDER
                    .comment("The default language of the server."
                            , "服务器默认语言。")
                    .define("defaultLanguage", "en_us");

            // 被敌对生物锁定时是否限制传送
            TP_WITH_ENEMY = SERVER_BUILDER
                    .comment("Whether to restrict teleportation when the player is targeted (agroed) by hostile mobs.",
                            "是否在被敌对生物锁定（仇恨）时限制玩家进行传送操作。")
                    .define("tpWithEnemy", false);


            {
                SERVER_BUILDER.comment("Safe Teleport", "安全传送").push("Safe");

                // 不安全的方块
                UNSAFE_BLOCKS = SERVER_BUILDER
                        .comment("The list of unsafe blocks, players will not be teleported to these blocks.",
                                "不安全的方块列表，玩家不会传送到这些方块上。")
                        .defineList("unsafeBlocks", Stream.of(
                                                Blocks.LAVA,
                                                Blocks.FIRE,
                                                Blocks.CAMPFIRE,
                                                Blocks.SOUL_FIRE,
                                                Blocks.SOUL_CAMPFIRE,
                                                Blocks.CACTUS,
                                                Blocks.MAGMA_BLOCK,
                                                Blocks.SWEET_BERRY_BUSH
                                        ).map(block -> {
                                            ResourceLocation location = block.getRegistryName();
                                            return location == null ? "" : location.toString();
                                        })
                                        .collect(Collectors.toList())
                                , s -> s instanceof String
                        );

                // 窒息的方块
                SUFFOCATING_BLOCKS = SERVER_BUILDER
                        .comment("The list of suffocating blocks, players will not be teleported to these blocks.",
                                "窒息的方块列表，玩家头不会处于这些方块里面。")
                        .defineList("suffocatingBlocks", Stream.of(
                                                Blocks.LAVA,
                                                Blocks.WATER
                                        ).map(block -> {
                                            ResourceLocation location = block.getRegistryName();
                                            return location == null ? "" : location.toString();
                                        })
                                        .collect(Collectors.toList())
                                , s -> s instanceof String
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
                        .defineList("safeBlocks", Stream.of(
                                                Blocks.GRASS_BLOCK,
                                                Blocks.GRASS_PATH,
                                                Blocks.DIRT,
                                                Blocks.COBBLESTONE
                                        ).map(block -> {
                                            ResourceLocation location = block.getRegistryName();
                                            return location == null ? "" : location.toString();
                                        })
                                        .collect(Collectors.toList())
                                , s -> s instanceof String
                        );

                // 寻找安全坐标的区块范围
                SAFE_CHUNK_RANGE = SERVER_BUILDER
                        .comment("The chunk range for finding a safe coordinate, in chunks.",
                                "当进行安全传送时，寻找安全坐标的半径，单位为区块。")
                        .defineInRange("safeChunkRange", 1, 1, 16);

                SERVER_BUILDER.pop();
            }

            SERVER_BUILDER.pop();
        }


        // 定义指令权限
        {
            SERVER_BUILDER.comment("Command Permission", "指令权限").push("permission");

            {
                SERVER_BUILDER.comment("Command Permission", "指令权限").push("command");

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

                PERMISSION_FLY = SERVER_BUILDER
                        .comment("The permission level required to use the 'Fly' command."
                                , "飞行指令所需的权限等级。")
                        .defineInRange("permissionFly", 2, 0, 4);

                PERMISSION_VIRTUAL_OP = SERVER_BUILDER
                        .comment("The permission level required to use the 'Set virtual permission' command, and also used as the permission level for modifying server configuration."
                                , "设置虚拟权限指令所需的权限等级，同时用于控制使用'修改服务器配置指令'的权限。")
                        .defineInRange("permissionVirtualOp", 4, 0, 4);

                PERMISSION_SET_CARD = SERVER_BUILDER
                        .comment("The permission level required to use the 'Set the number of Teleport Card of the player' command."
                                , "设置玩家传送卡数量指令所需的权限等级。")
                        .defineInRange("permissionSetCard", 2, 0, 4);

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Across dimensions Switch", "跨维度权限").push("across");

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
                SERVER_BUILDER.pop();
            }

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


        // 定义传送代价
        {
            SERVER_BUILDER.comment("Teleport Cost", "传送代价").push("cost");

            {
                SERVER_BUILDER.comment("Teleport to the specified coordinates", "传送到指定坐标").push("TpCoordinate");

                COST_TP_COORDINATE_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the specified coordinates'"
                                , "传送到指定坐标的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpCoordinateType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpCoordinateRate", 0.002, 0, 9999);

                COST_TP_COORDINATE_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Teleport to the specified coordinates'."
                                , "传送到指定坐标的代价数量上限。")
                        .defineInRange("costTpCoordinateNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_COORDINATE_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Teleport to the specified coordinates'."
                                , "传送到指定坐标的代价数量下限。")
                        .defineInRange("costTpCoordinateNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_COORDINATE_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Teleport to the specified coordinates', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "传送到指定坐标的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpCoordinateExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Teleport to the specified structure", "传送到指定结构").push("TpStructure");

                COST_TP_STRUCTURE_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the specified structure'"
                                , "传送到指定结构的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpStructureType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpStructureRate", 0.002, 0, 9999);

                COST_TP_STRUCTURE_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Teleport to the specified structure'."
                                , "传送到指定结构的代价数量上限。")
                        .defineInRange("costTpStructureNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_STRUCTURE_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Teleport to the specified structure'."
                                , "传送到指定结构的代价数量下限。")
                        .defineInRange("costTpStructureNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_STRUCTURE_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Teleport to the specified structure', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "传送到指定结构的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpStructureExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Request to teleport oneself to other players", "请求传送至玩家").push("TpAsk");

                COST_TP_ASK_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Request to teleport oneself to other players'"
                                , "请求传送至玩家的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpAskType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpAskRate", 0.002, 0, 9999);

                COST_TP_ASK_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Request to teleport oneself to other players'."
                                , "请求传送至玩家的代价数量上限。")
                        .defineInRange("costTpAskNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_ASK_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Request to teleport oneself to other players'."
                                , "请求传送至玩家的代价数量下限。")
                        .defineInRange("costTpAskNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_ASK_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Request to teleport oneself to other players', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "请求传送至玩家的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpAskExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Request the transfer of other players to oneself", "请求将玩家传送至当前位置").push("TpHere");

                COST_TP_HERE_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Request the transfer of other players to oneself'"
                                , "请求将玩家传送至当前位置的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpHereType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpHereRate", 0.002, 0, 9999);

                COST_TP_HERE_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Request the transfer of other players to oneself'."
                                , "请求将玩家传送至当前位置的代价数量上限。")
                        .defineInRange("costTpHereNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_HERE_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Request the transfer of other players to oneself'."
                                , "请求将玩家传送至当前位置的代价数量下限。")
                        .defineInRange("costTpHereNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_HERE_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Request the transfer of other players to oneself', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "请求将玩家传送至当前位置的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpHereExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Teleport to a random location", "随机传送").push("TpRandom");

                COST_TP_RANDOM_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to a random location'"
                                , "随机传送的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpRandomType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpRandomRate", 0.002, 0, 9999);

                COST_TP_RANDOM_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Teleport to a random location'."
                                , "随机传送的代价数量上限。")
                        .defineInRange("costTpRandomNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_RANDOM_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Teleport to a random location'."
                                , "随机传送的代价数量下限。")
                        .defineInRange("costTpRandomNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_RANDOM_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Teleport to a random location', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "随机传送的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpRandomExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Teleport to the spawn of the player", "传送到玩家重生点").push("TpSpawn");

                COST_TP_SPAWN_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the spawn of the player'"
                                , "传送到玩家重生点的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpSpawnType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpSpawnRate", 0.002, 0, 9999);

                COST_TP_SPAWN_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Teleport to the spawn of the player'."
                                , "传送到玩家重生点的代价数量上限。")
                        .defineInRange("costTpSpawnNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_SPAWN_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Teleport to the spawn of the player'."
                                , "传送到玩家重生点的代价数量下限。")
                        .defineInRange("costTpSpawnNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_SPAWN_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Teleport to the spawn of the player', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "传送到玩家重生点的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpSpawnExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Teleport to the spawn of the world", "传送到世界重生点").push("TpWorldSpawn");

                COST_TP_WORLD_SPAWN_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the spawn of the world'"
                                , "传送到世界重生点的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpWorldSpawnType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpWorldSpawnRate", 0.002, 0, 9999);

                COST_TP_WORLD_SPAWN_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Teleport to the spawn of the world'."
                                , "传送到世界重生点的代价数量上限。")
                        .defineInRange("costTpWorldSpawnNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_WORLD_SPAWN_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Teleport to the spawn of the world'."
                                , "传送到世界重生点的代价数量下限。")
                        .defineInRange("costTpWorldSpawnNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_WORLD_SPAWN_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Teleport to the spawn of the world', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "传送到世界重生点的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpWorldSpawnExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Teleport to the top of current position", "传送到顶部").push("TpTop");

                COST_TP_TOP_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the top of current position'"
                                , "传送到顶部的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpTopType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpTopRate", 0.002, 0, 9999);

                COST_TP_TOP_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Teleport to the top of current position'."
                                , "传送到顶部的代价数量上限。")
                        .defineInRange("costTpTopNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_TOP_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Teleport to the top of current position'."
                                , "传送到顶部的代价数量下限。")
                        .defineInRange("costTpTopNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_TOP_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Teleport to the top of current position', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "传送到顶部的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpTopExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Teleport to the bottom of current position", "传送到底部").push("TpBottom");

                COST_TP_BOTTOM_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the bottom of current position'"
                                , "传送到底部的代价类型。"
                                , "Allowed Values: " + EnumCostType.names()
                        )
                        .define("costTpBottomType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpBottomRate", 0.002, 0, 9999);

                COST_TP_BOTTOM_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Teleport to the bottom of current position'."
                                , "传送到底部的代价数量上限。")
                        .defineInRange("costTpBottomNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_BOTTOM_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Teleport to the bottom of current position'."
                                , "传送到底部的代价数量下限。")
                        .defineInRange("costTpBottomNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_BOTTOM_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Teleport to the bottom of current position', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "传送到底部的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpBottomExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Teleport to the upper of current position", "传送到上方").push("TpUp");

                COST_TP_UP_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the upper of current position'"
                                , "传送到上方的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpUpType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpUpRate", 0.002, 0, 9999);

                COST_TP_UP_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Teleport to the upper of current position'."
                                , "传送到上方的代价数量上限。")
                        .defineInRange("costTpUpNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_UP_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Teleport to the upper of current position'."
                                , "传送到上方的代价数量下限。")
                        .defineInRange("costTpUpNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_UP_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Teleport to the upper of current position', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "传送到上方的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpUpExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Teleport to the lower of current position", "传送到下方").push("TpDown");

                COST_TP_DOWN_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the lower of current position'"
                                , "传送到下方的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpDownType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpDownRate", 0.002, 0, 9999);

                COST_TP_DOWN_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Teleport to the lower of current position'."
                                , "传送到下方的代价数量上限。")
                        .defineInRange("costTpDownNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_DOWN_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Teleport to the lower of current position'."
                                , "传送到下方的代价数量下限。")
                        .defineInRange("costTpDownNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_DOWN_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Teleport to the lower of current position', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "传送到下方的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpDownExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Teleport to the end of the line of sight", "This function is independent of the player's render distance setting.", "传送至视线尽头", "该功能与玩家设置的视距无关。").push("TpView");

                COST_TP_VIEW_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the end of the line of sight'"
                                , "传送至视线尽头的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpViewType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpViewRate", 0.002, 0, 9999);

                COST_TP_VIEW_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Teleport to the end of the line of sight'."
                                , "传送至视线尽头的代价数量上限。")
                        .defineInRange("costTpViewNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_VIEW_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Teleport to the end of the line of sight'."
                                , "传送至视线尽头的代价数量下限。")
                        .defineInRange("costTpViewNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_VIEW_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Teleport to the end of the line of sight', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "传送至视线尽头的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpViewExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Teleport to the home", "传送到家").push("TpHome");

                COST_TP_HOME_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the home'"
                                , "传送到家的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpHomeType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpHomeRate", 0.002, 0, 9999);

                COST_TP_HOME_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Teleport to the home'."
                                , "传送到家的代价数量上限。")
                        .defineInRange("costTpHomeNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_HOME_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Teleport to the home'."
                                , "传送到家的代价数量下限。")
                        .defineInRange("costTpHomeNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_HOME_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Teleport to the home', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "传送到家的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpHomeExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Teleport to the stage", "传送到驿站").push("TpStage");

                COST_TP_STAGE_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the stage'"
                                , "传送到驿站的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpStageType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpStageRate", 0.002, 0, 9999);

                COST_TP_STAGE_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Teleport to the stage'."
                                , "传送到驿站的代价数量上限。")
                        .defineInRange("costTpStageNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_STAGE_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Teleport to the stage'."
                                , "传送到驿站的代价数量下限。")
                        .defineInRange("costTpStageNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_STAGE_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Teleport to the stage', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "传送到驿站的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpStageExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            {
                SERVER_BUILDER.comment("Teleport to the previous location", "传送到上次传送点").push("TpBack");

                COST_TP_BACK_TYPE = SERVER_BUILDER
                        .comment("The cost type for 'Teleport to the previous location'"
                                , "传送到上次传送点的代价类型。"
                                , "Allowed Values: NONE, EXP_POINT, EXP_LEVEL, HEALTH, HUNGER, ITEM, COMMAND")
                        .define("costTpBackType", EnumCostType.NONE.name());

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
                        .defineInRange("costTpBackRate", 0.002, 0, 9999);

                COST_TP_BACK_NUM_UPPER = SERVER_BUILDER
                        .comment("The upper limit of the cost for 'Teleport to the previous location'."
                                , "传送到上次传送点的代价数量上限。")
                        .defineInRange("costTpBackNumUpper", 20, 0, Integer.MAX_VALUE);

                COST_TP_BACK_NUM_LOWER = SERVER_BUILDER
                        .comment("The lower limit of the cost for 'Teleport to the previous location'."
                                , "传送到上次传送点的代价数量下限。")
                        .defineInRange("costTpBackNumLower", 0, 0, Integer.MAX_VALUE);

                COST_TP_BACK_EXP = SERVER_BUILDER
                        .comment("The expression used to calculate the cost for 'Teleport to the previous location', allowed functions: sqrt, pow, log, sin, cos, abs; allowed variables: num, distance, rate."
                                , "Example: sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)"
                                , "传送到上次传送点的代价计算表达式，支持的函数：sqrt、pow、log、sin、cos、abs；支持的变量：num、distance、rate。"
                                , "示例：sqrt(num * distance * rate)、pow(num, 2) / log(distance) + sin(num)、abs(cos(rate) - num) * random(1, 10)")
                        .define("costTpBackExp", "num * distance * rate");

                SERVER_BUILDER.pop();
            }

            SERVER_BUILDER.pop();
        }

        SERVER_CONFIG = SERVER_BUILDER.build();
    }

    /**
     * 重置服务器配置文件
     */
    public static void resetConfig() {
        TELEPORT_RECORD_LIMIT.set(100);
        TELEPORT_BACK_SKIP_TYPE.set(new ArrayList<String>() {{
            add(EnumTeleportType.TP_BACK.name());
        }});
        TELEPORT_ACROSS_DIMENSION.set(true);
        TELEPORT_COST_DISTANCE_LIMIT.set(10000);
        TELEPORT_COST_DISTANCE_ACROSS_DIMENSION.set(10000);
        TELEPORT_VIEW_DISTANCE_LIMIT.set(16 * 64);
        TELEPORT_REQUEST_EXPIRE_TIME.set(60);
        TELEPORT_REQUEST_COOLDOWN_TYPE.set(EnumCoolDownType.INDIVIDUAL.name());
        TELEPORT_REQUEST_COOLDOWN.set(10);
        TELEPORT_RANDOM_DISTANCE_LIMIT.set(10000);
        TELEPORT_HOME_LIMIT.set(5);
        UNSAFE_BLOCKS.set(Stream.of(
                        Blocks.LAVA,
                        Blocks.FIRE,
                        Blocks.CAMPFIRE,
                        Blocks.SOUL_FIRE,
                        Blocks.SOUL_CAMPFIRE,
                        Blocks.CACTUS,
                        Blocks.MAGMA_BLOCK,
                        Blocks.SWEET_BERRY_BUSH
                ).map(block -> {
                    ResourceLocation location = block.getRegistryName();
                    return location == null ? "" : location.toString();
                })
                .collect(Collectors.toList()));
        SUFFOCATING_BLOCKS.set(Stream.of(
                        Blocks.LAVA,
                        Blocks.WATER
                ).map(block -> {
                    ResourceLocation location = block.getRegistryName();
                    return location == null ? "" : location.toString();
                })
                .collect(Collectors.toList()));
        SETBLOCK_WHEN_SAFE_NOT_FOUND.set(false);
        GETBLOCK_FROM_INVENTORY.set(true);
        SAFE_BLOCKS.set(Stream.of(
                        Blocks.GRASS_BLOCK,
                        Blocks.GRASS_PATH,
                        Blocks.DIRT,
                        Blocks.COBBLESTONE
                ).map(block -> {
                    ResourceLocation location = block.getRegistryName();
                    return location == null ? "" : location.toString();
                })
                .collect(Collectors.toList()));
        SAFE_CHUNK_RANGE.set(1);
        HELP_HEADER.set("-----==== Narcissus Farewell Help (%d/%d) ====-----");
        TP_SOUND.set(SoundEvents.ENDERMAN_TELEPORT.getRegistryName().toString());
        TP_WITH_VEHICLE.set(true);
        TP_WITH_FOLLOWER.set(true);
        TP_WITH_FOLLOWER_RANGE.set(10);
        HELP_INFO_NUM_PER_PAGE.set(5);
        DEFAULT_LANGUAGE.set("en_us");

        PERMISSION_FEED_OTHER.set(2);
        PERMISSION_TP_COORDINATE.set(2);
        PERMISSION_TP_STRUCTURE.set(2);
        PERMISSION_TP_ASK.set(0);
        PERMISSION_TP_HERE.set(0);
        PERMISSION_TP_RANDOM.set(1);
        PERMISSION_TP_SPAWN.set(0);
        PERMISSION_TP_SPAWN_OTHER.set(2);
        PERMISSION_TP_WORLD_SPAWN.set(0);
        PERMISSION_TP_TOP.set(1);
        PERMISSION_TP_BOTTOM.set(1);
        PERMISSION_TP_UP.set(1);
        PERMISSION_TP_DOWN.set(1);
        PERMISSION_TP_VIEW.set(1);
        PERMISSION_TP_HOME.set(0);
        PERMISSION_TP_STAGE.set(0);
        PERMISSION_SET_STAGE.set(2);
        PERMISSION_DEL_STAGE.set(2);
        PERMISSION_GET_STAGE.set(0);
        PERMISSION_TP_BACK.set(0);
        PERMISSION_FLY.set(2);
        PERMISSION_VIRTUAL_OP.set(4);
        PERMISSION_SET_CARD.set(2);

        PERMISSION_TP_COORDINATE_ACROSS_DIMENSION.set(2);
        PERMISSION_TP_STRUCTURE_ACROSS_DIMENSION.set(2);
        PERMISSION_TP_ASK_ACROSS_DIMENSION.set(0);
        PERMISSION_TP_HERE_ACROSS_DIMENSION.set(0);
        PERMISSION_TP_RANDOM_ACROSS_DIMENSION.set(0);
        PERMISSION_TP_SPAWN_ACROSS_DIMENSION.set(0);
        PERMISSION_TP_WORLD_SPAWN_ACROSS_DIMENSION.set(0);
        PERMISSION_TP_HOME_ACROSS_DIMENSION.set(0);
        PERMISSION_TP_STAGE_ACROSS_DIMENSION.set(0);
        PERMISSION_TP_BACK_ACROSS_DIMENSION.set(0);

        COOLDOWN_TP_COORDINATE.set(10);
        COOLDOWN_TP_STRUCTURE.set(10);
        COOLDOWN_TP_ASK.set(10);
        COOLDOWN_TP_HERE.set(10);
        COOLDOWN_TP_RANDOM.set(10);
        COOLDOWN_TP_SPAWN.set(10);
        COOLDOWN_TP_WORLD_SPAWN.set(10);
        COOLDOWN_TP_TOP.set(10);
        COOLDOWN_TP_BOTTOM.set(10);
        COOLDOWN_TP_UP.set(10);
        COOLDOWN_TP_DOWN.set(10);
        COOLDOWN_TP_VIEW.set(10);
        COOLDOWN_TP_HOME.set(10);
        COOLDOWN_TP_STAGE.set(10);
        COOLDOWN_TP_BACK.set(10);

        COST_TP_COORDINATE_TYPE.set(EnumCostType.NONE.name());
        COST_TP_COORDINATE_NUM.set(1);
        COST_TP_COORDINATE_CONF.set("");
        COST_TP_COORDINATE_RATE.set(0.002);
        COST_TP_COORDINATE_NUM_UPPER.set(20);
        COST_TP_COORDINATE_NUM_LOWER.set(0);
        COST_TP_COORDINATE_EXP.set("num * distance * rate");

        COST_TP_STRUCTURE_TYPE.set(EnumCostType.NONE.name());
        COST_TP_STRUCTURE_NUM.set(1);
        COST_TP_STRUCTURE_CONF.set("");
        COST_TP_STRUCTURE_RATE.set(0.002);
        COST_TP_STRUCTURE_NUM_UPPER.set(20);
        COST_TP_STRUCTURE_NUM_LOWER.set(0);
        COST_TP_STRUCTURE_EXP.set("num * distance * rate");

        COST_TP_ASK_TYPE.set(EnumCostType.NONE.name());
        COST_TP_ASK_NUM.set(1);
        COST_TP_ASK_CONF.set("");
        COST_TP_ASK_RATE.set(0.002);
        COST_TP_ASK_NUM_UPPER.set(20);
        COST_TP_ASK_NUM_LOWER.set(0);
        COST_TP_ASK_EXP.set("num * distance * rate");

        COST_TP_HERE_TYPE.set(EnumCostType.NONE.name());
        COST_TP_HERE_NUM.set(1);
        COST_TP_HERE_CONF.set("");
        COST_TP_HERE_RATE.set(0.002);
        COST_TP_HERE_NUM_UPPER.set(20);
        COST_TP_HERE_NUM_LOWER.set(0);
        COST_TP_HERE_EXP.set("num * distance * rate");

        COST_TP_RANDOM_TYPE.set(EnumCostType.NONE.name());
        COST_TP_RANDOM_NUM.set(1);
        COST_TP_RANDOM_CONF.set("");
        COST_TP_RANDOM_RATE.set(0.002);
        COST_TP_RANDOM_NUM_UPPER.set(20);
        COST_TP_RANDOM_NUM_LOWER.set(0);
        COST_TP_RANDOM_EXP.set("num * distance * rate");

        COST_TP_SPAWN_TYPE.set(EnumCostType.NONE.name());
        COST_TP_SPAWN_NUM.set(1);
        COST_TP_SPAWN_CONF.set("");
        COST_TP_SPAWN_RATE.set(0.002);
        COST_TP_SPAWN_NUM_UPPER.set(20);
        COST_TP_SPAWN_NUM_LOWER.set(0);
        COST_TP_SPAWN_EXP.set("num * distance * rate");

        COST_TP_WORLD_SPAWN_TYPE.set(EnumCostType.NONE.name());
        COST_TP_WORLD_SPAWN_NUM.set(1);
        COST_TP_WORLD_SPAWN_CONF.set("");
        COST_TP_WORLD_SPAWN_RATE.set(0.002);
        COST_TP_WORLD_SPAWN_NUM_UPPER.set(20);
        COST_TP_WORLD_SPAWN_NUM_LOWER.set(0);
        COST_TP_WORLD_SPAWN_EXP.set("num * distance * rate");

        COST_TP_TOP_TYPE.set(EnumCostType.NONE.name());
        COST_TP_TOP_NUM.set(1);
        COST_TP_TOP_CONF.set("");
        COST_TP_TOP_RATE.set(0.002);
        COST_TP_TOP_NUM_UPPER.set(20);
        COST_TP_TOP_NUM_LOWER.set(0);
        COST_TP_TOP_EXP.set("num * distance * rate");

        COST_TP_BOTTOM_TYPE.set(EnumCostType.NONE.name());
        COST_TP_BOTTOM_NUM.set(1);
        COST_TP_BOTTOM_CONF.set("");
        COST_TP_BOTTOM_RATE.set(0.002);
        COST_TP_BOTTOM_NUM_UPPER.set(20);
        COST_TP_BOTTOM_NUM_LOWER.set(0);
        COST_TP_BOTTOM_EXP.set("num * distance * rate");

        COST_TP_UP_TYPE.set(EnumCostType.NONE.name());
        COST_TP_UP_NUM.set(1);
        COST_TP_UP_CONF.set("");
        COST_TP_UP_RATE.set(0.002);
        COST_TP_UP_NUM_UPPER.set(20);
        COST_TP_UP_NUM_LOWER.set(0);
        COST_TP_UP_EXP.set("num * distance * rate");

        COST_TP_DOWN_TYPE.set(EnumCostType.NONE.name());
        COST_TP_DOWN_NUM.set(1);
        COST_TP_DOWN_CONF.set("");
        COST_TP_DOWN_RATE.set(0.002);
        COST_TP_DOWN_NUM_UPPER.set(20);
        COST_TP_DOWN_NUM_LOWER.set(0);
        COST_TP_DOWN_EXP.set("num * distance * rate");

        COST_TP_VIEW_TYPE.set(EnumCostType.NONE.name());
        COST_TP_VIEW_NUM.set(1);
        COST_TP_VIEW_CONF.set("");
        COST_TP_VIEW_RATE.set(0.002);
        COST_TP_VIEW_NUM_UPPER.set(20);
        COST_TP_VIEW_NUM_LOWER.set(0);
        COST_TP_VIEW_EXP.set("num * distance * rate");

        COST_TP_HOME_TYPE.set(EnumCostType.NONE.name());
        COST_TP_HOME_NUM.set(1);
        COST_TP_HOME_CONF.set("");
        COST_TP_HOME_RATE.set(0.002);
        COST_TP_HOME_NUM_UPPER.set(20);
        COST_TP_HOME_NUM_LOWER.set(0);
        COST_TP_HOME_EXP.set("num * distance * rate");

        COST_TP_STAGE_TYPE.set(EnumCostType.NONE.name());
        COST_TP_STAGE_NUM.set(1);
        COST_TP_STAGE_CONF.set("");
        COST_TP_STAGE_RATE.set(0.002);
        COST_TP_STAGE_NUM_UPPER.set(20);
        COST_TP_STAGE_NUM_LOWER.set(0);
        COST_TP_STAGE_EXP.set("num * distance * rate");

        COST_TP_BACK_TYPE.set(EnumCostType.NONE.name());
        COST_TP_BACK_NUM.set(1);
        COST_TP_BACK_CONF.set("");
        COST_TP_BACK_RATE.set(0.002);
        COST_TP_BACK_NUM_UPPER.set(20);
        COST_TP_BACK_NUM_LOWER.set(0);
        COST_TP_BACK_EXP.set("num * distance * rate");

        SERVER_CONFIG.save();
    }

    /**
     * 经典模式</br></br>
     * 驿站指令改为warp</br>
     * back返回时不再忽略使用back指令产生的传送记录</br>
     * 操作家与驿站的前缀后移为后缀，使之更易于输入
     */
    public static void resetConfigWithMode1() {
        resetConfig();

        TELEPORT_BACK_SKIP_TYPE.set(new ArrayList<>());

        SERVER_CONFIG.save();
    }

    /**
     * 简洁模式</br></br>
     * 在经典模式的基础上禁用不常用的功能
     */
    public static void resetConfigWithMode2() {
        resetConfigWithMode1();

        SERVER_CONFIG.save();
    }

    /**
     * 进阶模式</br></br>
     * 使用推荐的配置，并启用传送代价
     */
    public static void resetConfigWithMode3() {
        resetConfig();

        COST_TP_COORDINATE_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_COORDINATE_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_STRUCTURE_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_ASK_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_HERE_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_RANDOM_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_SPAWN_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_WORLD_SPAWN_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_TOP_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_BOTTOM_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_UP_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_DOWN_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_VIEW_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_HOME_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_STAGE_TYPE.set(EnumCostType.EXP_POINT.name());
        COST_TP_BACK_TYPE.set(EnumCostType.HUNGER.name());

        SERVER_CONFIG.save();
    }

}
