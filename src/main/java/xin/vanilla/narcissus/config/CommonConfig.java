package xin.vanilla.narcissus.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.preset.CommonConfigPresets;
import xin.vanilla.narcissus.enums.EnumCardType;
import xin.vanilla.narcissus.enums.EnumCoolDownType;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 通用配置
 */
@Config(name = NarcissusFarewell.MODID + "-common", type = ConfigScope.COMMON,
        generateView = true, viewUnbound = Config.UnboundAccess.DEFAULTS)
public class CommonConfig implements ConfigData {

    public CommonConfig() {
    }

    // region 配置结构

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "基础设置", en_us = "Base Settings")
    private BaseCategory base = new BaseCategory();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "功能开关", en_us = "Function Switch")
    private FeatureSwitchCategory featureSwitch = new FeatureSwitchCategory();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "自定义指令，请勿添加前缀'/'", en_us = "Custom Command Settings, don't add prefix '/'")
    private CommandCategory command = new CommandCategory();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "无前缀简短指令开关", en_us = "Concise (no-prefix) command toggles")
    private ConciseCategory concise = new ConciseCategory();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "各指令所需权限等级", en_us = "Permission levels for commands")
    private PermissionCategory permission = new PermissionCategory();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "冷却时间", en_us = "Cooldown Time")
    private CooldownCategory cooldown = new CooldownCategory();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "传送倒计时", en_us = "Teleport Countdown")
    private TeleportCountdownCategory teleportCountdown = new TeleportCountdownCategory();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "传送代价", en_us = "Teleport Cost")
    private CostCategory cost = new CostCategory();

    // endregion 配置结构


    public static CommonConfigView get() {
        return CommonConfigView.get();
    }

    public static void save() {
        ConfigHolder h = BaniraConfigs.holder(CommonConfig.class);
        if (h != null) {
            h.save();
        }
    }

    public static void resetConfig() {
        CommonConfigPresets.resetConfig(BaniraConfigs.holder(CommonConfig.class));
    }

    public static void resetConfigWithMode1() {
        CommonConfigPresets.resetConfigWithMode1(BaniraConfigs.holder(CommonConfig.class));
    }

    public static void resetConfigWithMode2() {
        CommonConfigPresets.resetConfigWithMode2(BaniraConfigs.holder(CommonConfig.class));
    }

    public static void resetConfigWithMode3() {
        CommonConfigPresets.resetConfigWithMode3(BaniraConfigs.holder(CommonConfig.class));
    }


    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class BaseCategory {
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "其他设置", en_us = "Other Settings")
        private OtherCategory other = new OtherCategory();

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送限制", en_us = "Teleport Limit")
        private TeleportLimitCategory teleportLimit = new TeleportLimitCategory();

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送跟随", en_us = "Teleport Together")
        private TeleportTogetherCategory teleportTogether = new TeleportTogetherCategory();

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送请求", en_us = "Teleport Request")
        private TeleportRequestCategory teleportRequest = new TeleportRequestCategory();

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "安全传送", en_us = "Safe Teleport")
        private SafeTeleportCategory safeTeleport = new SafeTeleportCategory();

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送", en_us = "Random Teleport")
        private RandomTeleportCategory randomTeleport = new RandomTeleportCategory();

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "创造模式飞行", en_us = "Creative Flight")
        private CreativeFlightCategory creativeFlight = new CreativeFlightCategory();
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class FeatureSwitchCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用坐标分享", en_us = "Enable or disable the option to 'Share safeWorldCoordinate'.")
        private boolean switchShare = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用自杀或毒杀", en_us = "Enable or disable the option to 'Suicide or poisoning'.")
        private boolean switchFeed = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送到指定坐标", en_us = "Enable or disable the option to 'Teleport to the specified coordinates'.")
        private boolean switchTpCoordinate = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送到指定结构", en_us = "Enable or disable the option to 'Teleport to the specified structure'.")
        private boolean switchTpStructure = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送请求", en_us = "Enable or disable the option to 'Request to teleport oneself to other players'.")
        private boolean switchTpAsk = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用请求将玩家传送至当前位置", en_us = "Enable or disable the option to 'Request the transfer of other players to oneself'.")
        private boolean switchTpHere = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用随机传送", en_us = "Enable or disable the option to 'Teleport to a random location'.")
        private boolean switchTpRandom = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送到玩家重生点", en_us = "Enable or disable the option to 'Teleport to the spawn of the player'.")
        private boolean switchTpSpawn = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送到世界重生点", en_us = "Enable or disable the option to 'Teleport to the spawn of the world'.")
        private boolean switchTpWorldSpawn = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送到顶部", en_us = "Enable or disable the option to 'Teleport to the top of current position'.")
        private boolean switchTpTop = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送到底部", en_us = "Enable or disable the option to 'Teleport to the bottom of current position'.")
        private boolean switchTpBottom = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送到上方", en_us = "Enable or disable the option to 'Teleport to the upper of current position'.")
        private boolean switchTpUp = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送到下方", en_us = "Enable or disable the option to 'Teleport to the lower of current position'.")
        private boolean switchTpDown = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送至视线尽头\n该功能与玩家设置的视距无关", en_us = "Enable or disable the option to 'Teleport to the end of the line of sight'. This function is independent of the player's render distance setting.")
        private boolean switchTpView = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送到家", en_us = "Enable or disable the option to 'Teleport to the home'.")
        private boolean switchTpHome = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送到驿站", en_us = "Enable or disable the option to 'Teleport to the stage'.")
        private boolean switchTpStage = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送到上次传送点", en_us = "Enable or disable the option to 'Teleport to the previous location'.")
        private boolean switchTpBack = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用坟墓传送（传送到上次死亡点）", en_us = "Enable or disable the option to 'Teleport to the grave / last death location'.")
        private boolean switchTpGrave = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用飞行相关指令", en_us = "Enable or disable the option to 'Creative flight command'.")
        private boolean switchFly = true;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CommandCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "指令前缀，请仅使用英文字母及下划线，否则可能会出现问题", en_us = "The prefix of the command, please only use English characters and underscores, otherwise it may cause problems.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandPrefix = NarcissusFarewell.DEFAULT_COMMAND_PREFIX;
        @ConfigEntry.Gui.Tooltip(zh_cn = "获取玩家的UUID的指令", en_us = "This command is used to get the UUID of the player.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandUuid = "uuid";
        @ConfigEntry.Gui.Tooltip(zh_cn = "获取当前世界的维度ID的指令", en_us = "This command is used to get the dimension ID of the current world.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandDimension = "dim";
        @ConfigEntry.Gui.Tooltip(zh_cn = "获取传送卡数量的指令", en_us = "This command is used to get the number of Teleport Card.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandCard = "card";
        @ConfigEntry.Gui.Tooltip(zh_cn = "分享驿站、玩家的私人传送点、玩家当前坐标的指令", en_us = "This command is used to share the stage, the personal home, and the current safeWorldCoordinate of player.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandShare = "share";
        @ConfigEntry.Gui.Tooltip(zh_cn = "自杀或毒杀的指令，水仙是有毒的可不能食用哦", en_us = "This command is used to suicide or poisoning, narcissus are poisonous and should not be eaten.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandFeed = "feed";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定坐标的指令", en_us = "This command is used to teleport to the specified coordinates.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpCoordinate = "tpx";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定结构的指令", en_us = "This command is used to teleport to the specified structure.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpStructure = "tpst";
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家", en_us = "Request to teleport oneself to other players")
        private CommandTpAskNames tpAsk = new CommandTpAskNames();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传送至当前位置", en_us = "Request the transfer of other players to oneself")
        private CommandTpHereNames tpHere = new CommandTpHereNames();
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送的指令", en_us = "The command to teleport to a random location.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpRandom = "tpr";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到玩家重生点的指令", en_us = "The command to teleport to the spawn of the player.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpSpawn = "tpsp";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到世界重生点的指令", en_us = "The command to teleport to the spawn of the world.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpWorldSpawn = "tpws";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到顶部的指令", en_us = "The command to teleport to the top of current position.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpTop = "tpt";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到底部的指令", en_us = "The command to teleport to the bottom of current position.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpBottom = "tpb";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上方的指令", en_us = "The command to teleport to the upper of current position.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpUp = "tpu";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到下方的指令", en_us = "The command to teleport to the lower of current position.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpDown = "tpd";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至视线尽头的指令\n该功能与玩家设置的视距无关", en_us = "The command to teleport to the end of the line of sight. This function is independent of the player's render distance setting.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpView = "tpv";
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家", en_us = "Teleport to the home")
        private CommandTpHomeNames tpHome = new CommandTpHomeNames();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站", en_us = "Teleport to the stage")
        private CommandTpStageNames tpStage = new CommandTpStageNames();
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上次传送点的指令", en_us = "The command to teleport to the previous location.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpBack = "back";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到坟墓（上次死亡点）的指令", en_us = "The command to teleport to the grave / last death location.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpGrave = "grave";
        @ConfigEntry.Gui.Tooltip(zh_cn = "切换创造模式飞行的指令", en_us = "The command to toggle creative flight.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandFly = "fly";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class OtherCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否禁用原版TP指令", en_us = "Whether to disable the original TP command.")
        private boolean removeOriginalTp = false;

        @ConfigEntry.Gui.Tooltip(zh_cn = "传送时的音效", en_us = "The sound effect when teleporting.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String tpSound = "minecraft:entity.enderman.teleport";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TeleportLimitCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送回时忽略的传送类型", en_us = "The teleport back skip type.")
        private List<String> teleportBackSkipType = new ArrayList<String>() {{
            add(EnumTeleportType.TP_BACK.name());
        }};
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送记录数量限制，数量为0表示不限制", en_us = "The limit of teleport records, 0 means no limit.")
        @ConfigEntry.BoundedDiscrete(max = 99999)
        private int teleportRecordLimit = 100;
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送时搜索死亡点/坟墓位置的范围上限", en_us = "The search range limit for grave teleport, in blocks (chunk-related logic uses this as radius cap).")
        @ConfigEntry.BoundedDiscrete(min = 1, max = 256)
        private int graveSearchRangeLimit = 32;
        @ConfigEntry.Gui.Tooltip(zh_cn = "玩家可设置的家的数量", en_us = "The maximum number of homes that can be set by the player.")
        @ConfigEntry.BoundedDiscrete(min = 1, max = 9999)
        private int teleportHomeLimit = 5;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至视线尽头时最远传送距离限制，值为0表示不限制", en_us = "The distance limit for teleporting to the view, 0 means no limit.")
        @ConfigEntry.BoundedDiscrete()
        private int teleportViewDistanceLimit = 16 * 64;

        @ConfigEntry.Gui.Tooltip(zh_cn = "当玩家没有个人重生点（床/重生锚等）时，\ncommandTpSpawn 取世界出生点的维度\n填 CURRENT 或 AUTO（不区分大小写）表示使用玩家当前维度；\n否则填维度 ID（如 minecraft:overworld）\n维度 ID 解析失败或世界未加载时亦使用玩家当前维度", en_us = "When the player has no personal respawn (bed/anchor, etc.), which dimension's world spawn commandTpSpawn uses. Use CURRENT or AUTO (case-insensitive) for the player's current dimension; otherwise a dimension ID (e.g. minecraft:overworld). On parse failure or if the world is not loaded, uses the current dimension.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String tpSpawnNoBedWorldDimension = "minecraft:overworld";
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用跨维度传送", en_us = "Is the teleport across dimensions enabled?")
        private boolean teleportAcrossDimension = true;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TeleportTogetherCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "允许载具一起传送", en_us = "Whether to allow vehicles to be teleported together.")
        private boolean tpWithVehicle = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "允许跟随的实体一起传送", en_us = "Whether to allow followers to be teleported together.")
        private boolean tpWithFollower = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跟随的实体识别范围半径", en_us = "The range of followers to be recognized, in blocks.")
        @ConfigEntry.BoundedDiscrete(min = 1, max = 256)
        private int tpWithFollowerRange = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "在被敌对生物锁定（仇恨）时限制玩家进行传送操作", en_us = "Whether to restrict teleportation when the player is targeted (agroed) by hostile mobs.")
        private boolean tpWithEnemy = false;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TeleportRequestCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送请求过期时间，单位为秒", en_us = "The expire time for teleport request, in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 3600)
        private int teleportRequestExpireTime = 60;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送请求冷却时间的计算方式：", en_us = "The method used to calculate the cooldown time for teleport requests.")
        private EnumCoolDownType teleportRequestCooldownType = EnumCoolDownType.INDIVIDUAL;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送请求的全局冷却时间，单位为秒", en_us = "The global cooldown time for teleport requests, measured in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int teleportRequestCooldown = 10;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class RandomTeleportCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送与传送至指定结构的最大距离限制", en_us = "The maximum distance limit for random teleportation or teleportation to a specified structure.")
        @ConfigEntry.BoundedDiscrete(min = 5)
        private int teleportRandomDistanceLimit = 10000;
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送开启安全传送时，若当前随机目标未找到安全落脚点，\n重新随机目标坐标的次数", en_us = "When random teleport uses safe teleport, how many times to pick a new random target if no safe spot is found.")
        @ConfigEntry.BoundedDiscrete(max = 64)
        private int tpRandomSafeNotFoundRetries = 0;
    }

    @Getter
    @Setter
        @Accessors(chain = true, fluent = true)
    public static class SafeTeleportCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "不安全的方块列表，玩家不会传送到这些方块上", en_us = "The list of unsafe blocks, players will not be teleported to these blocks.")
        private List<String> unsafeBlocks = new ArrayList<>(Arrays.asList(
                "minecraft:lava", "minecraft:fire", "minecraft:campfire", "minecraft:soul_fire",
                "minecraft:soul_campfire", "minecraft:cactus", "minecraft:magma_block",
                "minecraft:sweet_berry_bush"));
        @ConfigEntry.Gui.Tooltip(zh_cn = "窒息的方块列表，玩家头不会处于这些方块里面", en_us = "The list of suffocating blocks, players will not be teleported to these blocks.")
        private List<String> suffocatingBlocks = new ArrayList<>(Arrays.asList(
                "minecraft:lava", "minecraft:water"));
        @ConfigEntry.Gui.Tooltip(zh_cn = "安全传送未找到安全坐标时，在脚下放置方块", en_us = "When performing a safe teleport, whether to place a block underfoot if a safe safeWorldCoordinate is not found.")
        private boolean setBlockWhenSafeNotFound = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "安全传送未找到安全坐标时，仅使用背包内可放置方块", en_us = "When performing a safe teleport, whether to only use placeable blocks from the player's inventory if a safe safeWorldCoordinate is not found.")
        private boolean getBlockFromInventory = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "当进行安全传送时，如果未找到安全坐标，放置方块的列表\n若'getBlockFromInventory'为false，\n则始终使用列表中的第一个方块", en_us = "When performing a safe teleport, the list of blocks to place if a safe safeWorldCoordinate is not found. If 'getBlockFromInventory' is set to false, the first block in the list will always be used.")
        private List<String> safeBlocks = new ArrayList<>(Arrays.asList(
                "minecraft:grass_block", "minecraft:grass_path", "minecraft:dirt",
                "minecraft:cobblestone"));
        @ConfigEntry.Gui.Tooltip(zh_cn = "当进行安全传送时，寻找安全坐标的半径，单位为区块", en_us = "The chunk range for finding a safe safeWorldCoordinate, in chunks.")
        @ConfigEntry.BoundedDiscrete(min = 1, max = 16)
        private int safeChunkRange = 1;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CreativeFlightCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "创造模式飞行最低速度", en_us = "Minimum creative flight speed (GUI / config unit).")
        @ConfigEntry.BoundedDouble(min = -1.0d * Integer.MAX_VALUE, max = 1.0d * Integer.MAX_VALUE, decimalPlaces = 3)
        private double flySpeedMin = -5d;
        @ConfigEntry.Gui.Tooltip(zh_cn = "创造模式飞行最高速度", en_us = "Maximum creative flight speed (GUI / config unit).")
        @ConfigEntry.BoundedDouble(min = -1.0d * Integer.MAX_VALUE, max = 1.0d * Integer.MAX_VALUE, decimalPlaces = 3)
        private double flySpeedMax = 5d;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class PermissionCategory {
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "指令权限", en_us = "Command Permission")
        private PermissionCommandCategory command = new PermissionCommandCategory();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度权限", en_us = "Across dimensions Switch")
        private PermissionAcrossCategory across = new PermissionAcrossCategory();
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class PermissionCommandCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "毒杀指令所需的权限等级", en_us = "The permission level required to use the 'Poisoning others' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionFeedOther = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定坐标指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to the specified coordinates' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpCoordinate = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定结构指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to the specified structure' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpStructure = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家指令所需的权限等级", en_us = "The permission level required to use the 'Request to teleport oneself to other players' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpAsk = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传送至当前位置指令所需的权限等级", en_us = "The permission level required to use the 'Request the transfer of other players to oneself' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpHere = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to a random location' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpRandom = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到玩家重生点指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to the spawn of the player' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpSpawn = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到其他玩家重生点指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to the spawn of the other player' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpSpawnOther = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到世界重生点指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to the spawn of the world' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpWorldSpawn = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到顶部指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to the top of current position' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpTop = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到底部指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to the bottom of current position' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpBottom = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上方指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to the upper of current position' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpUp = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到下方指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to the lower of current position' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpDown = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至视线尽头指令所需的权限等级\n该功能与玩家设置的视距无关", en_us = "The permission level required to use the 'Teleport to the end of the line of sight' command. This function is independent of the player's render distance setting.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpView = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to the home' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpHome = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to the stage' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpStage = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "设置驿站指令所需的权限等级", en_us = "The permission level required to use the 'Set the stage' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpStageSet = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "删除驿站指令所需的权限等级", en_us = "The permission level required to use the 'Delete the stage' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpStageDel = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "查询驿站指令所需的权限等级", en_us = "The permission level required to use the 'Get the stage info' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpStageGet = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上次传送点指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to the previous location' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpBack = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送指令所需的权限等级", en_us = "The permission level required to use the 'Teleport to the grave' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpGrave = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "飞行指令所需的权限等级", en_us = "The permission level required to use the 'Creative flight' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionFly = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "设置虚拟权限指令所需的权限等级\n同时控制修改服务器配置指令的权限", en_us = "The permission level required to use the 'Set virtual permission' command, and also used as the permission level for modifying server configuration.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionVirtualOp = 4;
        @ConfigEntry.Gui.Tooltip(zh_cn = "设置玩家传送卡数量指令所需的权限等级", en_us = "The permission level required to use the 'Set the number of Teleport Card of the player' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionSetCard = 2;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class PermissionAcrossCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到指定坐标指令所需的权限等级，若为-1则禁用跨维度传送", en_us = "The permission level required to use the 'Teleport to the specified coordinates' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpCoordinateAcrossDimension = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到指定结构指令所需的权限等级，若为-1则禁用跨维度传送", en_us = "The permission level required to use the 'Teleport to the specified structure' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpStructureAcrossDimension = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度请求传送至玩家指令所需的权限等级，若为-1则禁用跨维度传送", en_us = "The permission level required to use the 'Request to teleport oneself to other players' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpAskAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到当前位置指令所需的权限等级，若为-1则禁用跨维度传送", en_us = "The permission level required to use the 'Teleport to the current position' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpHereAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到随机位置指令所需的权限等级，若为-1则禁用跨维度传送", en_us = "The permission level required to use the 'Teleport to the random position' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpRandomAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到当前维度的出生点指令所需的权限等级，若为-1则禁用跨维度传送", en_us = "The permission level required to use the 'Teleport to the spawn of the current dimension' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpSpawnAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到当前维度的世界出生点指令所需的权限等级，\n若为-1则禁用跨维度传送", en_us = "The permission level required to use the 'Teleport to the world spawn of the current dimension' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpWorldSpawnAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到家指令所需的权限等级，若为-1则禁用跨维度传送", en_us = "The permission level required to use the 'Teleport to the home' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpHomeAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到驿站指令所需的权限等级，若为-1则禁用跨维度传送", en_us = "The permission level required to use the 'Teleport to the stage' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpStageAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到上次传送点指令所需的权限等级，若为-1则禁用跨维度传送", en_us = "The permission level required to use the 'Teleport to the previous location' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpBackAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度坟墓传送指令所需的权限等级，若为-1则禁用跨维度传送", en_us = "The permission level required to use the 'Teleport to the grave' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpGraveAcrossDimension = 0;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CooldownCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定坐标的冷却时间，单位为秒", en_us = "The cooldown time for 'Teleport to the specified coordinates', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpCoordinate = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定结构的冷却时间，单位为秒", en_us = "The cooldown time for 'Teleport to the specified structure', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpStructure = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家的冷却时间，单位为秒", en_us = "The cooldown time for 'Request to teleport oneself to other players', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpAsk = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传送至当前位置的冷却时间，单位为秒", en_us = "The cooldown time for 'Request the transfer of other players to oneself', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpHere = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送的冷却时间，单位为秒", en_us = "The cooldown time for 'Teleport to a random location', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpRandom = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到玩家重生点的冷却时间，单位为秒", en_us = "The cooldown time for 'Teleport to the spawn of the player', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpSpawn = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到世界重生点的冷却时间，单位为秒", en_us = "The cooldown time for 'Teleport to the spawn of the world', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpWorldSpawn = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到顶部的冷却时间，单位为秒", en_us = "The cooldown time for 'Teleport to the top of current position', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpTop = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到底部的冷却时间，单位为秒", en_us = "The cooldown time for 'Teleport to the bottom of current position', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpBottom = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上方的冷却时间，单位为秒", en_us = "The cooldown time for 'Teleport to the upper of current position', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpUp = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到下方的冷却时间，单位为秒", en_us = "The cooldown time for 'Teleport to the lower of current position', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpDown = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至视线尽头的冷却时间，单位为秒\n该功能与玩家设置的视距无关", en_us = "The cooldown time for 'Teleport to the end of the line of sight', in seconds. This function is independent of the player's render distance setting.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpView = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家的冷却时间，单位为秒", en_us = "The cooldown time for 'Teleport to the home', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpHome = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站的冷却时间，单位为秒", en_us = "The cooldown time for 'Teleport to the stage', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpStage = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上次传送点的冷却时间，单位为秒", en_us = "The cooldown time for 'Teleport to the previous location', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpBack = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送的冷却时间，单位为秒", en_us = "The cooldown time for 'Teleport to the grave', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpGrave = 10;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TeleportCountdownCategory {
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "各传送类型服务端倒计时（秒）", en_us = "Server countdown seconds per teleport type.")
        private ServerPerTypeTeleportCountdownGroup server = new ServerPerTypeTeleportCountdownGroup();
        @ConfigEntry.Gui.Tooltip(zh_cn = "为 true 时仅使用「各传送类型服务端倒计时」中的值，忽略玩家个人设置", en_us = "If true, only server per-type countdowns apply; player preferences are ignored.")
        private boolean forceServerCountdown = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "玩家可在指令/GUI 中配置的倒计时下限（秒）", en_us = "Minimum seconds players may set for their teleport countdown preference.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int playerCountdownRangeMin = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "玩家可在指令/GUI 中配置的倒计时上限（秒）", en_us = "Maximum seconds players may set for their teleport countdown preference.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int playerCountdownRangeMax = 300;
        @ConfigEntry.Gui.Tooltip(zh_cn = "为 true 时玩家移动将打断传送倒计时并取消传送", en_us = "If true, player movement cancels the teleport countdown and the teleport.")
        private boolean cancelCountdownOnPlayerMove = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "为 true 时玩家受伤将打断传送倒计时并取消传送", en_us = "If true, taking damage cancels the teleport countdown and the teleport.")
        private boolean cancelCountdownOnPlayerDamage = false;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class ServerPerTypeTeleportCountdownGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定坐标", en_us = "Teleport to coordinates.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpCoordinate = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定结构", en_us = "Teleport to structure.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpStructure = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家", en_us = "TPA.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpAsk = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传至身边", en_us = "TPHere.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpHere = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送", en_us = "Random TP.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpRandom = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到玩家重生点", en_us = "TP player spawn.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpSpawn = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到世界重生点", en_us = "TP world spawn.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpWorldSpawn = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到顶部", en_us = "TP top.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpTop = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到底部", en_us = "TP bottom.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpBottom = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上方", en_us = "TP up.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpUp = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到下方", en_us = "TP down.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpDown = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "视线传送", en_us = "TP view.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpView = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家", en_us = "TP home.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpHome = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站", en_us = "TP stage.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpStage = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上次传送点", en_us = "TP back.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpBack = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送", en_us = "TP grave.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpGrave = 0;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CostCategory {
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "坐标传送", en_us = "coordinate teleport cost")
        private CostSettings coordinate = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "结构传送", en_us = "structure teleport cost")
        private CostSettings structure = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至玩家", en_us = "ask teleport cost")
        private CostSettings ask = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "将玩家传送至当前位置", en_us = "here teleport cost")
        private CostSettings here = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送", en_us = "random teleport cost")
        private CostSettings random = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "个人重生点", en_us = "spawn teleport cost")
        private CostSettings spawn = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "世界重生点", en_us = "worldSpawn teleport cost")
        private CostSettings worldSpawn = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "顶部", en_us = "top teleport cost")
        private CostSettings top = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "底部", en_us = "bottom teleport cost")
        private CostSettings bottom = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "上方", en_us = "up teleport cost")
        private CostSettings up = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "下方", en_us = "down teleport cost")
        private CostSettings down = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "视线尽头", en_us = "view teleport cost")
        private CostSettings view = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "家", en_us = "home teleport cost")
        private CostSettings home = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "驿站", en_us = "stage teleport cost")
        private CostSettings stage = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "返回", en_us = "back teleport cost")
        private CostSettings back = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓", en_us = "grave teleport cost")
        private CostSettings grave = new CostSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "费用计算距离", en_us = "Cost distance")
        private CostDistanceSettings distance = new CostDistanceSettings();
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送卡", en_us = "Teleport cards")
        private CostCardSettings cards = new CostCardSettings();
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CommandTpAskNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家的指令", en_us = "This command is used to request to teleport oneself to other players.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpAsk = "tpa";
        @ConfigEntry.Gui.Tooltip(zh_cn = "接受请求传送至玩家的指令", en_us = "This command is used to accept teleportation of other players to oneself.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpAskYes = "tpay";
        @ConfigEntry.Gui.Tooltip(zh_cn = "拒绝请求传送至玩家的指令", en_us = "This command is used to refuse teleportation of other players to oneself.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpAskNo = "tpan";
        @ConfigEntry.Gui.Tooltip(zh_cn = "取消请求传送至玩家的指令", en_us = "This command is used to cancel the request to teleport to other players.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpAskCancel = "tpac";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CommandTpHereNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传送至当前位置的指令", en_us = "This command is used to request the transfer of other players to oneself.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpHere = "tph";
        @ConfigEntry.Gui.Tooltip(zh_cn = "接受请求将玩家传送至当前位置的指令", en_us = "This command is used to accept teleportation to other players.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpHereYes = "tphy";
        @ConfigEntry.Gui.Tooltip(zh_cn = "拒绝请求将玩家传送至当前位置的指令", en_us = "This command is used to refuse teleportation to other players.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpHereNo = "tphn";
        @ConfigEntry.Gui.Tooltip(zh_cn = "取消请求将玩家传送至当前位置的指令", en_us = "This command is used to cancel the request to teleport to other players.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpHereCancel = "tphc";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CommandTpHomeNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家的指令", en_us = "The command to teleport to the home.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpHome = "home";
        @ConfigEntry.Gui.Tooltip(zh_cn = "设置家的指令", en_us = "The command to set the home.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandSetHome = "sethome";
        @ConfigEntry.Gui.Tooltip(zh_cn = "删除家的指令", en_us = "The command to delete the home.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandDelHome = "delhome";
        @ConfigEntry.Gui.Tooltip(zh_cn = "查询家的信息的指令", en_us = "The command to get the home info.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandGetHome = "gethome";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CommandTpStageNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站的指令", en_us = "The command to teleport to the stage.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandTpStage = "stage";
        @ConfigEntry.Gui.Tooltip(zh_cn = "设置驿站的指令", en_us = "The command to set the stage.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandSetStage = "setstage";
        @ConfigEntry.Gui.Tooltip(zh_cn = "删除驿站的指令", en_us = "The command to delete the stage.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandDelStage = "delstage";
        @ConfigEntry.Gui.Tooltip(zh_cn = "查询驿站的信息的的指令", en_us = "The command to get the stage info.")
        @ConfigEntry.Access(emptyString = ConfigEntry.Access.EmptyPolicy.DEFAULT)
        private String commandGetStage = "getstage";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class ConciseCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '设置语言' 指令", en_us = "Enable or disable the concise version of the 'Set the language' command.")
        private boolean conciseLanguage = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '获取玩家的UUID' 指令", en_us = "Enable or disable the concise version of the 'Get the UUID of the player' command.")
        private boolean conciseUuid = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '获取当前世界的维度ID' 指令", en_us = "Enable or disable the concise version of the 'Get the dimension ID of the current world' command.")
        private boolean conciseDimension = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '获取玩家的传送卡数量' 指令", en_us = "Enable or disable the concise version of the 'Get the number of Teleport Card of the player' command.")
        private boolean conciseCard = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '分享驿站、玩家的私人传送点、玩家当前坐标' 指令", en_us = "Enable or disable the concise version of the 'Share the stage, the personal home, and the current safeWorldCoordinate of player' command.")
        private boolean conciseShare = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '自杀或毒杀' 指令", en_us = "Enable or disable the concise version of the 'Suicide or poisoning' command.")
        private boolean conciseFeed = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '传送到指定坐标' 指令", en_us = "Enable or disable the concise version of the 'Teleport to the specified coordinates' command.")
        private boolean conciseTpCoordinate = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '传送到指定结构' 指令", en_us = "Enable or disable the concise version of the 'Teleport to the specified structure' command.")
        private boolean conciseTpStructure = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '随机传送' 指令", en_us = "Enable or disable the concise version of the 'Teleport to a random location' command.")
        private boolean conciseTpRandom = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '传送到玩家重生点' 指令", en_us = "Enable or disable the concise version of the 'Teleport to the spawn of the player' command.")
        private boolean conciseTpSpawn = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '传送到世界重生点' 指令", en_us = "Enable or disable the concise version of the 'Teleport to the spawn of the world' command.")
        private boolean conciseTpWorldSpawn = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '传送到顶部' 指令", en_us = "Enable or disable the concise version of the 'Teleport to the top of current position' command.")
        private boolean conciseTpTop = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '传送到底部' 指令", en_us = "Enable or disable the concise version of the 'Teleport to the bottom of current position' command.")
        private boolean conciseTpBottom = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '传送到上方' 指令", en_us = "Enable or disable the concise version of the 'Teleport to the upper of current position' command.")
        private boolean conciseTpUp = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '传送到下方' 指令", en_us = "Enable or disable the concise version of the 'Teleport to the lower of current position' command.")
        private boolean conciseTpDown = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '传送至视线尽头' 指令\n该功能与玩家设置的视距无关", en_us = "Enable or disable the concise version of the 'Teleport to the end of the line of sight' command. This function is independent of the player's render distance setting.")
        private boolean conciseTpView = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '传送到上次传送点' 指令", en_us = "Enable or disable the concise version of the 'Teleport to the previous location' command.")
        private boolean conciseTpBack = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '坟墓传送' 指令", en_us = "Enable or disable the concise version of the 'Teleport to the grave / last death location' command.")
        private boolean conciseTpGrave = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '飞行' 指令", en_us = "Enable or disable the concise version of the 'Creative flight' command.")
        private boolean conciseFly = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '设置虚拟权限' 指令", en_us = "Enable or disable the concise version of the 'Set virtual permission' command.")
        private boolean conciseVirtualOp = false;
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家", en_us = "Request to teleport oneself to other players")
        private ConciseTpAskNames tpAsk = new ConciseTpAskNames();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传送至当前位置", en_us = "Request the transfer of other players to oneself")
        private ConciseTpHereNames tpHere = new ConciseTpHereNames();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家", en_us = "Teleport to the home")
        private ConciseTpHomeNames tpHome = new ConciseTpHomeNames();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站", en_us = "Teleport to the stage")
        private ConciseTpStageNames tpStage = new ConciseTpStageNames();
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class ConciseTpAskNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '请求传送至玩家' 指令", en_us = "Enable or disable the concise version of the 'Request to teleport oneself to other players' command.")
        private boolean conciseTpAsk = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '接受请求传送至玩家' 指令", en_us = "Enable or disable the concise version of the 'Accept teleportation of other players to oneself' command.")
        private boolean conciseTpAskYes = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '拒绝请求传送至玩家' 指令", en_us = "Enable or disable the concise version of the 'Refuse teleportation of other players to oneself' command.")
        private boolean conciseTpAskNo = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '取消请求传送至玩家' 指令", en_us = "Enable or disable the concise version of the 'Cancel the request to teleport to other players' command.")
        private boolean conciseTpAskCancel = true;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class ConciseTpHereNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '请求将玩家传送至当前位置' 指令", en_us = "Enable or disable the concise version of the 'Request the transfer of other players to oneself' command.")
        private boolean conciseTpHere = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '接受请求将玩家传送至当前位置' 指令", en_us = "Enable or disable the concise version of the 'Accept teleportation to other players' command.")
        private boolean conciseTpHereYes = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '拒绝请求将玩家传送至当前位置' 指令", en_us = "Enable or disable the concise version of the 'Refuse teleportation to other players' command.")
        private boolean conciseTpHereNo = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '取消请求将玩家传送至当前位置' 指令", en_us = "Enable or disable the concise version of the 'Cancel the request to teleport to other players' command.")
        private boolean conciseTpHereCancel = true;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class ConciseTpHomeNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '传送到家' 指令", en_us = "Enable or disable the concise version of the 'Teleport to the home' command.")
        private boolean conciseTpHome = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '设置家' 指令", en_us = "Enable or disable the concise version of the 'Set the home' command.")
        private boolean conciseSetHome = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '删除家' 指令", en_us = "Enable or disable the concise version of the 'Delete the home' command.")
        private boolean conciseDelHome = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '查询家' 指令", en_us = "Enable or disable the concise version of the 'Get the home info' command.")
        private boolean conciseGetHome = true;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class ConciseTpStageNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '传送到驿站' 指令", en_us = "Enable or disable the concise version of the 'Teleport to the stage' command.")
        private boolean conciseTpStage = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '设置驿站' 指令", en_us = "Enable or disable the concise version of the 'Set the stage' command.")
        private boolean conciseSetStage = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '删除驿站' 指令", en_us = "Enable or disable the concise version of the 'Delete the stage' command.")
        private boolean conciseDelStage = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用无前缀版本的 '查询驿站' 指令", en_us = "Enable or disable the concise version of the 'Get the stage info' command.")
        private boolean conciseGetStage = true;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CostSettings {
        @ConfigEntry.Gui.Tooltip(zh_cn = "费用类型，NONE 表示免费", en_us = "Resource charged; NONE means free")
        private EnumCostType type = EnumCostType.NONE;
        @ConfigEntry.BoundedDouble(min = 0, max = Integer.MAX_VALUE, decimalPlaces = 4)
        @ConfigEntry.Gui.Tooltip(zh_cn = "固定费用\n默认费用 = 固定费用 + 每格费用 × 计算距离", en_us = "Fixed fee\nDefault fee = fixed amount + per-block amount * cost distance")
        private double fixedAmount = 0;
        @ConfigEntry.BoundedDouble(min = 0, max = Integer.MAX_VALUE, decimalPlaces = 6)
        @ConfigEntry.Gui.Tooltip(zh_cn = "每格距离的费用", en_us = "Fee per block of cost distance")
        private double perBlockAmount = .002;
        @ConfigEntry.BoundedDiscrete(min = 0)
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后费用下限", en_us = "Minimum calculated fee")
        private int minAmount = 0;
        @ConfigEntry.BoundedDiscrete(min = -1)
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后费用上限\n-1 表示不限制，0 表示费用为零", en_us = "Maximum calculated fee\n-1 means unlimited; 0 caps the fee at zero")
        private int maxAmount = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "ITEM 类型使用的物品 ID，可附带 SNBT\n最多 8192 个字符", en_us = "Item ID with optional SNBT for ITEM fees\nUp to 8192 characters")
        private String item = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "COMMAND 类型执行的指令，{amount} 替换为实际费用\n最多 8192 个字符", en_us = "Command for COMMAND fees; {amount} is the payable fee\nUp to 8192 characters")
        private String command = "";
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "自定义费用公式", en_us = "Custom cost formula")
        private CustomCostSettings custom = new CustomCostSettings();
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CustomCostSettings {
        @ConfigEntry.Gui.Tooltip(zh_cn = "cost/sources 下的 Java 文件相对路径，留空使用默认计算\n最多 128 个字符，修改源码后使用 cost reload", en_us = "Relative Java file under cost/sources; blank uses default arithmetic\nUp to 128 characters; use cost reload after editing code")
        private String file = "";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CostDistanceSettings {
        @ConfigEntry.BoundedDiscrete(min = 0)
        @ConfigEntry.Gui.Tooltip(zh_cn = "同维度计算距离上限，0 表示不限制\n不限制实际传送距离", en_us = "Same-dimension cost distance cap; 0 means unlimited\nDoes not restrict teleport distance")
        private int maxDistance = 10000;
        @ConfigEntry.BoundedDiscrete(min = 0)
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送使用的计算距离，0 表示距离为零", en_us = "Cost distance for cross-dimension teleports; 0 means zero distance")
        private int crossDimensionDistance = 10000;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CostCardSettings {
        @ConfigEntry.Gui.Tooltip(zh_cn = "启用传送卡", en_us = "Enable teleport cards")
        private boolean enabled = false;
        @ConfigEntry.BoundedDiscrete(min = 0)
        @ConfigEntry.Gui.Tooltip(zh_cn = "每日首次上线发放的传送卡数量", en_us = "Cards granted on the first login of each day")
        private int dailyGrant = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送卡的使用方式\n免费传送不会消耗传送卡", en_us = "How cards affect payable costs\nFree teleports do not consume cards")
        private EnumCardType mode = EnumCardType.WAIVE_COST;
    }
}
