package xin.vanilla.narcissus.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.access.CommonConfigAccess;
import xin.vanilla.narcissus.enums.EnumCardType;
import xin.vanilla.narcissus.enums.EnumCoolDownType;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 通用配置（COMMON）。配置 GUI 的 {@link ConfigEntry.Gui.Tooltip} 与项目根目录 {@code narcissus_farewell-common.toml}
 * 中各分类段的注释一致，请勿脱离 TOML 随意改写说明文案。
 * <p>
 * 运行时通过 {@link #get()} 返回的 {@link RootView} 访问配置，例如 {@code CommonConfig.get().base().teleportCard()}、
 * {@code CommonConfig.get().general().defaultLanguage()}；路径与字段一致（如 {@code general.*}、{@code permission.command.*}）。
 */
@Config(name = "narcissus_farewell-common", type = ConfigScope.COMMON)
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
    private CommandNamesCategory commandNames = new CommandNamesCategory();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "简化指令", en_us = "Concise Command Settings")
    private ConciseCategory conciseCommands = new ConciseCategory();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "基础设置", en_us = "Base Settings")
    private GeneralCategory general = new GeneralCategory();

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "指令权限", en_us = "Command Permission")
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


    public static RootView get() {
        return CommonConfigAccess.root(BaniraConfigs.holder(CommonConfig.class));
    }

    public static void save() {
        ConfigHolder h = BaniraConfigs.holder(CommonConfig.class);
        if (h != null) {
            h.save();
        }
    }

    public static void resetConfig() {
        CommonConfigAccess.resetConfig(BaniraConfigs.holder(CommonConfig.class));
    }

    public static void resetConfigWithMode1() {
        CommonConfigAccess.resetConfigWithMode1(BaniraConfigs.holder(CommonConfig.class));
    }

    public static void resetConfigWithMode2() {
        CommonConfigAccess.resetConfigWithMode2(BaniraConfigs.holder(CommonConfig.class));
    }

    public static void resetConfigWithMode3() {
        CommonConfigAccess.resetConfigWithMode3(BaniraConfigs.holder(CommonConfig.class));
    }


    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class BaseCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送卡。", en_us = "Enable or disable the option to 'Teleport Card'.")
        private boolean teleportCard = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "每日可获得的传送卡数量。", en_us = "The number of Teleport Card that can be obtained daily.")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int teleportCardDaily = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送卡的使用方式：", en_us = "Teleport Card Usage Modes:")
        private EnumCardType teleportCardType = EnumCardType.REFUND_ALL_COST;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否禁用原版TP指令。", en_us = "Whether to disable the original TP command.")
        private boolean removeOriginalTp = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "创造模式飞行最低速度。", en_us = "Minimum creative flight speed (GUI / config unit).")
        @ConfigEntry.BoundedDouble(min = -1.0d * Integer.MAX_VALUE, max = 1.0d * Integer.MAX_VALUE, decimalPlaces = 3)
        private double flySpeedMin = -5d;
        @ConfigEntry.Gui.Tooltip(zh_cn = "创造模式飞行最高速度。", en_us = "Maximum creative flight speed (GUI / config unit).")
        @ConfigEntry.BoundedDouble(min = -1.0d * Integer.MAX_VALUE, max = 1.0d * Integer.MAX_VALUE, decimalPlaces = 3)
        private double flySpeedMax = 5d;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class FeatureSwitchCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用坐标分享。", en_us = "Enable or disable the option to 'Share safeWorldCoordinate'.")
        private boolean switchShare = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用自杀或毒杀。", en_us = "Enable or disable the option to 'Suicide or poisoning'.")
        private boolean switchFeed = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送到指定坐标。", en_us = "Enable or disable the option to 'Teleport to the specified coordinates'.")
        private boolean switchTpCoordinate = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送到指定结构。", en_us = "Enable or disable the option to 'Teleport to the specified structure'.")
        private boolean switchTpStructure = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送请求。", en_us = "Enable or disable the option to 'Request to teleport oneself to other players'.")
        private boolean switchTpAsk = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用请求将玩家传送至当前位置。", en_us = "Enable or disable the option to 'Request the transfer of other players to oneself'.")
        private boolean switchTpHere = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用随机传送。", en_us = "Enable or disable the option to 'Teleport to a random location'.")
        private boolean switchTpRandom = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送到玩家重生点。", en_us = "Enable or disable the option to 'Teleport to the spawn of the player'.")
        private boolean switchTpSpawn = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送到世界重生点。", en_us = "Enable or disable the option to 'Teleport to the spawn of the world'.")
        private boolean switchTpWorldSpawn = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送到顶部。", en_us = "Enable or disable the option to 'Teleport to the top of current position'.")
        private boolean switchTpTop = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送到底部。", en_us = "Enable or disable the option to 'Teleport to the bottom of current position'.")
        private boolean switchTpBottom = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送到上方。", en_us = "Enable or disable the option to 'Teleport to the upper of current position'.")
        private boolean switchTpUp = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送到下方。", en_us = "Enable or disable the option to 'Teleport to the lower of current position'.")
        private boolean switchTpDown = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送至视线尽头。该功能与玩家设置的视距无关。", en_us = "Enable or disable the option to 'Teleport to the end of the line of sight'. This function is independent of the player's render distance setting.")
        private boolean switchTpView = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送到家。", en_us = "Enable or disable the option to 'Teleport to the home'.")
        private boolean switchTpHome = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送到驿站。", en_us = "Enable or disable the option to 'Teleport to the stage'.")
        private boolean switchTpStage = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用传送到上次传送点。", en_us = "Enable or disable the option to 'Teleport to the previous location'.")
        private boolean switchTpBack = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用坟墓传送（传送到上次死亡点）。", en_us = "Enable or disable the option to 'Teleport to the grave / last death location'.")
        private boolean switchTpGrave = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用飞行相关指令。", en_us = "Enable or disable the option to 'Creative flight command'.")
        private boolean switchFly = true;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CommandNamesCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "指令前缀，请仅使用英文字母及下划线，否则可能会出现问题。", en_us = "The prefix of the command, please only use English characters and underscores, otherwise it may cause problems.")
        private String commandPrefix = NarcissusFarewell.DEFAULT_COMMAND_PREFIX;
        @ConfigEntry.Gui.Tooltip(zh_cn = "获取玩家的UUID的指令。", en_us = "This command is used to get the UUID of the player.")
        private String commandUuid = "uuid";
        @ConfigEntry.Gui.Tooltip(zh_cn = "获取当前世界的维度ID的指令。", en_us = "This command is used to get the dimension ID of the current world.")
        private String commandDimension = "dim";
        @ConfigEntry.Gui.Tooltip(zh_cn = "获取传送卡数量的指令。", en_us = "This command is used to get the number of Teleport Card.")
        private String commandCard = "card";
        @ConfigEntry.Gui.Tooltip(zh_cn = "分享驿站、玩家的私人传送点、玩家当前坐标的指令。", en_us = "This command is used to share the stage, the personal home, and the current safeWorldCoordinate of player.")
        private String commandShare = "share";
        @ConfigEntry.Gui.Tooltip(zh_cn = "自杀或毒杀的指令，水仙是有毒的可不能食用哦。", en_us = "This command is used to suicide or poisoning, narcissus are poisonous and should not be eaten.")
        private String commandFeed = "feed";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定坐标的指令。", en_us = "This command is used to teleport to the specified coordinates.")
        private String commandTpCoordinate = "tpx";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定结构的指令。", en_us = "This command is used to teleport to the specified structure.")
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
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送的指令。", en_us = "The command to teleport to a random location.")
        private String commandTpRandom = "tpr";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到玩家重生点的指令。", en_us = "The command to teleport to the spawn of the player.")
        private String commandTpSpawn = "tpsp";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到世界重生点的指令。", en_us = "The command to teleport to the spawn of the world.")
        private String commandTpWorldSpawn = "tpws";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到顶部的指令。", en_us = "The command to teleport to the top of current position.")
        private String commandTpTop = "tpt";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到底部的指令。", en_us = "The command to teleport to the bottom of current position.")
        private String commandTpBottom = "tpb";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上方的指令。", en_us = "The command to teleport to the upper of current position.")
        private String commandTpUp = "tpu";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到下方的指令。", en_us = "The command to teleport to the lower of current position.")
        private String commandTpDown = "tpd";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至视线尽头的指令。该功能与玩家设置的视距无关。", en_us = "The command to teleport to the end of the line of sight. This function is independent of the player's render distance setting.")
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
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上次传送点的指令。", en_us = "The command to teleport to the previous location.")
        private String commandTpBack = "back";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到坟墓（上次死亡点）的指令。", en_us = "The command to teleport to the grave / last death location.")
        private String commandTpGrave = "grave";
        @ConfigEntry.Gui.Tooltip(zh_cn = "切换创造模式飞行的指令。", en_us = "The command to toggle creative flight.")
        private String commandFly = "fly";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class GeneralCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送记录数量限制，数量为0表示不限制。", en_us = "The limit of teleport records, 0 means no limit.")
        @ConfigEntry.BoundedDiscrete(max = 99999)
        private int teleportRecordLimit = 100;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送回时忽略的传送类型。", en_us = "The teleport back skip type.")
        private List<String> teleportBackSkipType = new ArrayList<String>() {{
            add(EnumTeleportType.TP_BACK.name());
        }};
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用跨维度传送。", en_us = "Is the teleport across dimensions enabled?")
        private boolean teleportAcrossDimension = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送代价中传送距离计算限制，值为0表示不限制。(此配置项并非限制传送距离，而是限制计算传送代价时使用的距离乘数。)", en_us = "The distance calculation limit for teleport cost, 0 means no limit. (This config item is not the limit of teleport distance, but the limit of the distance multiplier used when calculating teleport cost.)")
        @ConfigEntry.BoundedDiscrete()
        private int teleportCostDistanceLimit = 10000;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送时传送代价中传送距离取值，值为0表示不限制。", en_us = "The distance value for teleport cost when teleport across dimensions, 0 means no limit.")
        @ConfigEntry.BoundedDiscrete()
        private int teleportCostDistanceAcrossDimension = 10000;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至视线尽头时最远传送距离限制，值为0表示不限制。", en_us = "The distance limit for teleporting to the view, 0 means no limit.")
        @ConfigEntry.BoundedDiscrete()
        private int teleportViewDistanceLimit = 16 * 64;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送请求过期时间，单位为秒。", en_us = "The expire time for teleport request, in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 3600)
        private int teleportRequestExpireTime = 60;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送请求冷却时间的计算方式：", en_us = "The method used to calculate the cooldown time for teleport requests.")
        private EnumCoolDownType teleportRequestCooldownType = EnumCoolDownType.INDIVIDUAL;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送请求的全局冷却时间，单位为秒。", en_us = "The global cooldown time for teleport requests, measured in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int teleportRequestCooldown = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送与传送至指定结构的最大距离限制。", en_us = "The maximum distance limit for random teleportation or teleportation to a specified structure.")
        @ConfigEntry.BoundedDiscrete(min = 5)
        private int teleportRandomDistanceLimit = 10000;
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送开启安全传送时，若当前随机目标未找到安全落脚点，重新随机目标坐标的次数。", en_us = "When random teleport uses safe teleport, how many times to pick a new random target if no safe spot is found.")
        @ConfigEntry.BoundedDiscrete(max = 64)
        private int tpRandomSafeNotFoundRetries = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送时搜索死亡点/坟墓位置的范围上限。", en_us = "The search range limit for grave teleport, in blocks (chunk-related logic uses this as radius cap).")
        @ConfigEntry.BoundedDiscrete(min = 1, max = 256)
        private int graveSearchRangeLimit = 32;
        @ConfigEntry.Gui.Tooltip(zh_cn = "玩家可设置的家的数量。", en_us = "The maximum number of homes that can be set by the player.")
        @ConfigEntry.BoundedDiscrete(min = 1, max = 9999)
        private int teleportHomeLimit = 5;
        @ConfigEntry.Gui.Tooltip(zh_cn = "帮助指令信息头部内容。", en_us = "The header content of the help command.")
        private String helpHeader = "-----==== Narcissus Farewell Help (%d/%d) ====-----";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送时的音效。", en_us = "The sound effect when teleporting.")
        private String tpSound = Registry.SOUND_EVENT.getKey(SoundEvents.ENDERMAN_TELEPORT).toString();
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否允许载具一起传送。", en_us = "Whether to allow vehicles to be teleported together.")
        private boolean tpWithVehicle = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否允许跟随的实体一起传送。", en_us = "Whether to allow followers to be teleported together.")
        private boolean tpWithFollower = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跟随的实体识别范围半径。", en_us = "The range of followers to be recognized, in blocks.")
        @ConfigEntry.BoundedDiscrete(min = 1, max = 256)
        private int tpWithFollowerRange = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "每页显示的帮助信息数量。", en_us = "The number of help information displayed per page.")
        @ConfigEntry.BoundedDiscrete(min = 1, max = 9999)
        private int helpInfoNumPerPage = 5;
        @ConfigEntry.Gui.Tooltip(zh_cn = "服务器默认语言。", en_us = "The default language of the server.")
        private String defaultLanguage = "en_us";
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否在被敌对生物锁定（仇恨）时限制玩家进行传送操作。", en_us = "Whether to restrict teleportation when the player is targeted (agroed) by hostile mobs.")
        private boolean tpWithEnemy = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "当玩家没有个人重生点（床/重生锚等）时，tpsp 取世界出生点的维度。填 CURRENT 或 AUTO（不区分大小写）表示使用玩家当前维度；否则填维度 ID（如 minecraft:overworld）。维度 ID 解析失败或世界未加载时亦使用玩家当前维度。", en_us = "When the player has no personal respawn (bed/anchor, etc.), which dimension's world spawn tpsp uses. Use CURRENT or AUTO (case-insensitive) for the player's current dimension; otherwise a dimension ID (e.g. minecraft:overworld). On parse failure or if the world is not loaded, uses the current dimension.")
        private String tpSpawnNoBedWorldDimension = "minecraft:overworld";
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "安全传送", en_us = "Safe Teleport")
        private SafeTeleportCategory safeTeleport = new SafeTeleportCategory();
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class SafeTeleportCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "不安全的方块列表，玩家不会传送到这些方块上。", en_us = "The list of unsafe blocks, players will not be teleported to these blocks.")
        private List<String> unsafeBlocks = Stream.of(Blocks.LAVA, Blocks.FIRE, Blocks.CAMPFIRE, Blocks.SOUL_FIRE, Blocks.SOUL_CAMPFIRE, Blocks.CACTUS, Blocks.MAGMA_BLOCK, Blocks.SWEET_BERRY_BUSH).map(block -> {
            ResourceLocation rl = Registry.BLOCK.getKey(block);
            return rl == null ? "" : rl.toString();
        }).collect(Collectors.toList());
        @ConfigEntry.Gui.Tooltip(zh_cn = "窒息的方块列表，玩家头不会处于这些方块里面。", en_us = "The list of suffocating blocks, players will not be teleported to these blocks.")
        private List<String> suffocatingBlocks = Stream.of(Blocks.LAVA, Blocks.WATER).map(block -> {
            ResourceLocation rl = Registry.BLOCK.getKey(block);
            return rl == null ? "" : rl.toString();
        }).collect(Collectors.toList());
        @ConfigEntry.Gui.Tooltip(zh_cn = "当进行安全传送时，如果未找到安全坐标，是否在脚下放置方块。", en_us = "When performing a safe teleport, whether to place a block underfoot if a safe safeWorldCoordinate is not found.")
        private boolean setBlockWhenSafeNotFound = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "当进行安全传送时，如果未找到安全坐标，是否仅从背包中获取可放置的方块。", en_us = "When performing a safe teleport, whether to only use placeable blocks from the player's inventory if a safe safeWorldCoordinate is not found.")
        private boolean getBlockFromInventory = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "当进行安全传送时，如果未找到安全坐标，放置方块的列表。若'getBlockFromInventory'为false，则始终使用列表中的第一个方块。", en_us = "When performing a safe teleport, the list of blocks to place if a safe safeWorldCoordinate is not found. If 'getBlockFromInventory' is set to false, the first block in the list will always be used.")
        private List<String> safeBlocks = Stream.of(Blocks.GRASS_BLOCK, Blocks.GRASS_PATH, Blocks.DIRT, Blocks.COBBLESTONE).map(block -> {
            ResourceLocation rl = Registry.BLOCK.getKey(block);
            return rl == null ? "" : rl.toString();
        }).collect(Collectors.toList());
        @ConfigEntry.Gui.Tooltip(zh_cn = "当进行安全传送时，寻找安全坐标的半径，单位为区块。", en_us = "The chunk range for finding a safe safeWorldCoordinate, in chunks.")
        @ConfigEntry.BoundedDiscrete(min = 1, max = 16)
        private int safeChunkRange = 1;
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
        @ConfigEntry.Gui.Tooltip(zh_cn = "毒杀指令所需的权限等级。", en_us = "The permission level required to use the 'Poisoning others' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionFeedOther = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定坐标指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to the specified coordinates' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpCoordinate = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定结构指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to the specified structure' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpStructure = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家指令所需的权限等级。", en_us = "The permission level required to use the 'Request to teleport oneself to other players' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpAsk = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传送至当前位置指令所需的权限等级。", en_us = "The permission level required to use the 'Request the transfer of other players to oneself' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpHere = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to a random location' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpRandom = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到玩家重生点指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to the spawn of the player' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpSpawn = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到其他玩家重生点指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to the spawn of the other player' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpSpawnOther = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到世界重生点指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to the spawn of the world' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpWorldSpawn = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到顶部指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to the top of current position' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpTop = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到底部指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to the bottom of current position' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpBottom = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上方指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to the upper of current position' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpUp = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到下方指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to the lower of current position' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpDown = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至视线尽头指令所需的权限等级。该功能与玩家设置的视距无关。", en_us = "The permission level required to use the 'Teleport to the end of the line of sight' command. This function is independent of the player's render distance setting.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpView = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to the home' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpHome = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to the stage' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpStage = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "设置驿站指令所需的权限等级。", en_us = "The permission level required to use the 'Set the stage' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpStageSet = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "删除驿站指令所需的权限等级。", en_us = "The permission level required to use the 'Delete the stage' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpStageDel = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "查询驿站指令所需的权限等级。", en_us = "The permission level required to use the 'Get the stage info' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpStageGet = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上次传送点指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to the previous location' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpBack = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送指令所需的权限等级。", en_us = "The permission level required to use the 'Teleport to the grave' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionTpGrave = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "飞行指令所需的权限等级。", en_us = "The permission level required to use the 'Creative flight' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionFly = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "设置虚拟权限指令所需的权限等级，同时用于控制使用'修改服务器配置指令'的权限。", en_us = "The permission level required to use the 'Set virtual permission' command, and also used as the permission level for modifying server configuration.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionVirtualOp = 4;
        @ConfigEntry.Gui.Tooltip(zh_cn = "设置玩家传送卡数量指令所需的权限等级。", en_us = "The permission level required to use the 'Set the number of Teleport Card of the player' command.")
        @ConfigEntry.BoundedDiscrete(max = 4)
        private int permissionSetCard = 2;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class PermissionAcrossCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到指定坐标指令所需的权限等级，若为-1则禁用跨维度传送。", en_us = "The permission level required to use the 'Teleport to the specified coordinates' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpCoordinateAcrossDimension = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到指定结构指令所需的权限等级，若为-1则禁用跨维度传送。", en_us = "The permission level required to use the 'Teleport to the specified structure' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpStructureAcrossDimension = 2;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度请求传送至玩家指令所需的权限等级，若为-1则禁用跨维度传送。", en_us = "The permission level required to use the 'Request to teleport oneself to other players' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpAskAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到当前位置指令所需的权限等级，若为-1则禁用跨维度传送。", en_us = "The permission level required to use the 'Teleport to the current position' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpHereAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到随机位置指令所需的权限等级，若为-1则禁用跨维度传送。", en_us = "The permission level required to use the 'Teleport to the random position' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpRandomAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到当前维度的出生点指令所需的权限等级，若为-1则禁用跨维度传送。", en_us = "The permission level required to use the 'Teleport to the spawn of the current dimension' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpSpawnAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到当前维度的世界出生点指令所需的权限等级，若为-1则禁用跨维度传送。", en_us = "The permission level required to use the 'Teleport to the world spawn of the current dimension' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpWorldSpawnAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到家指令所需的权限等级，若为-1则禁用跨维度传送。", en_us = "The permission level required to use the 'Teleport to the home' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpHomeAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到驿站指令所需的权限等级，若为-1则禁用跨维度传送。", en_us = "The permission level required to use the 'Teleport to the stage' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpStageAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度传送到上次传送点指令所需的权限等级，若为-1则禁用跨维度传送。", en_us = "The permission level required to use the 'Teleport to the previous location' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpBackAcrossDimension = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度坟墓传送指令所需的权限等级，若为-1则禁用跨维度传送。", en_us = "The permission level required to use the 'Teleport to the grave' command across dimensions, -1 means disabled.")
        @ConfigEntry.BoundedDiscrete(min = -1, max = 4)
        private int permissionTpGraveAcrossDimension = 0;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CooldownCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定坐标的冷却时间，单位为秒。", en_us = "The cooldown time for 'Teleport to the specified coordinates', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpCoordinate = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定结构的冷却时间，单位为秒。", en_us = "The cooldown time for 'Teleport to the specified structure', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpStructure = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家的冷却时间，单位为秒。", en_us = "The cooldown time for 'Request to teleport oneself to other players', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpAsk = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传送至当前位置的冷却时间，单位为秒。", en_us = "The cooldown time for 'Request the transfer of other players to oneself', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpHere = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送的冷却时间，单位为秒。", en_us = "The cooldown time for 'Teleport to a random location', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpRandom = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到玩家重生点的冷却时间，单位为秒。", en_us = "The cooldown time for 'Teleport to the spawn of the player', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpSpawn = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到世界重生点的冷却时间，单位为秒。", en_us = "The cooldown time for 'Teleport to the spawn of the world', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpWorldSpawn = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到顶部的冷却时间，单位为秒。", en_us = "The cooldown time for 'Teleport to the top of current position', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpTop = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到底部的冷却时间，单位为秒。", en_us = "The cooldown time for 'Teleport to the bottom of current position', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpBottom = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上方的冷却时间，单位为秒。", en_us = "The cooldown time for 'Teleport to the upper of current position', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpUp = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到下方的冷却时间，单位为秒。", en_us = "The cooldown time for 'Teleport to the lower of current position', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpDown = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至视线尽头的冷却时间，单位为秒。该功能与玩家设置的视距无关。", en_us = "The cooldown time for 'Teleport to the end of the line of sight', in seconds. This function is independent of the player's render distance setting.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpView = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家的冷却时间，单位为秒。", en_us = "The cooldown time for 'Teleport to the home', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpHome = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站的冷却时间，单位为秒。", en_us = "The cooldown time for 'Teleport to the stage', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpStage = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上次传送点的冷却时间，单位为秒。", en_us = "The cooldown time for 'Teleport to the previous location', in seconds.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int cooldownTpBack = 10;
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送的冷却时间，单位为秒。", en_us = "The cooldown time for 'Teleport to the grave', in seconds.")
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
        @ConfigEntry.Gui.Tooltip(zh_cn = "为 true 时仅使用「各传送类型服务端倒计时」中的值，忽略玩家个人设置。", en_us = "If true, only server per-type countdowns apply; player preferences are ignored.")
        private boolean forceServerCountdown = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "玩家可在指令/GUI 中配置的倒计时下限（秒）。", en_us = "Minimum seconds players may set for their teleport countdown preference.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int playerCountdownRangeMin = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "玩家可在指令/GUI 中配置的倒计时上限（秒）。", en_us = "Maximum seconds players may set for their teleport countdown preference.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int playerCountdownRangeMax = 300;
        @ConfigEntry.Gui.Tooltip(zh_cn = "为 true 时玩家移动将打断传送倒计时并取消传送。", en_us = "If true, player movement cancels the teleport countdown and the teleport.")
        private boolean cancelCountdownOnPlayerMove = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "为 true 时玩家受伤将打断传送倒计时并取消传送。", en_us = "If true, taking damage cancels the teleport countdown and the teleport.")
        private boolean cancelCountdownOnPlayerDamage = false;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class ServerPerTypeTeleportCountdownGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定坐标。", en_us = "Teleport to coordinates.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpCoordinate = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定结构。", en_us = "Teleport to structure.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpStructure = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家。", en_us = "TPA.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpAsk = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传至身边。", en_us = "TPHere.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpHere = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送。", en_us = "Random TP.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpRandom = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到玩家重生点。", en_us = "TP player spawn.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpSpawn = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到世界重生点。", en_us = "TP world spawn.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpWorldSpawn = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到顶部。", en_us = "TP top.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpTop = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到底部。", en_us = "TP bottom.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpBottom = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上方。", en_us = "TP up.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpUp = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到下方。", en_us = "TP down.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpDown = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "视线传送。", en_us = "TP view.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpView = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家。", en_us = "TP home.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpHome = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站。", en_us = "TP stage.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpStage = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上次传送点。", en_us = "TP back.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpBack = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送。", en_us = "TP grave.")
        @ConfigEntry.BoundedDiscrete(max = 86400)
        private int serverCountdownTpGrave = 0;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CostCategory {
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定坐标", en_us = "Teleport to the specified coordinates")
        private TpCoordinateCostGroup tpCoordinate = new TpCoordinateCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定结构", en_us = "Teleport to the specified structure")
        private TpStructureCostGroup tpStructure = new TpStructureCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家", en_us = "Request to teleport oneself to other players")
        private TpAskCostGroup tpAsk = new TpAskCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传送至当前位置", en_us = "Request the transfer of other players to oneself")
        private TpHereCostGroup tpHere = new TpHereCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送", en_us = "Teleport to a random location")
        private TpRandomCostGroup tpRandom = new TpRandomCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到玩家重生点", en_us = "Teleport to the spawn of the player")
        private TpSpawnCostGroup tpSpawn = new TpSpawnCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到世界重生点", en_us = "Teleport to the spawn of the world")
        private TpWorldSpawnCostGroup tpWorldSpawn = new TpWorldSpawnCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到顶部", en_us = "Teleport to the top of current position")
        private TpTopCostGroup tpTop = new TpTopCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到底部", en_us = "Teleport to the bottom of current position")
        private TpBottomCostGroup tpBottom = new TpBottomCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上方", en_us = "Teleport to the upper of current position")
        private TpUpCostGroup tpUp = new TpUpCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到下方", en_us = "Teleport to the lower of current position")
        private TpDownCostGroup tpDown = new TpDownCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至视线尽头", en_us = "Teleport to the end of the line of sight")
        private TpViewCostGroup tpView = new TpViewCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家", en_us = "Teleport to the home")
        private TpHomeCostGroup tpHome = new TpHomeCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站", en_us = "Teleport to the stage")
        private TpStageCostGroup tpStage = new TpStageCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上次传送点", en_us = "Teleport to the previous location")
        private TpBackCostGroup tpBack = new TpBackCostGroup();
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送", en_us = "Teleport to the grave")
        private TpGraveCostGroup tpGrave = new TpGraveCostGroup();
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CommandTpAskNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家的指令。", en_us = "This command is used to request to teleport oneself to other players.")
        private String commandTpAsk = "tpa";
        @ConfigEntry.Gui.Tooltip(zh_cn = "接受请求传送至玩家的指令。", en_us = "This command is used to accept teleportation of other players to oneself.")
        private String commandTpAskYes = "tpay";
        @ConfigEntry.Gui.Tooltip(zh_cn = "拒绝请求传送至玩家的指令。", en_us = "This command is used to refuse teleportation of other players to oneself.")
        private String commandTpAskNo = "tpan";
        @ConfigEntry.Gui.Tooltip(zh_cn = "取消请求传送至玩家的指令。", en_us = "This command is used to cancel the request to teleport to other players.")
        private String commandTpAskCancel = "tpac";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CommandTpHereNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传送至当前位置的指令。", en_us = "This command is used to request the transfer of other players to oneself.")
        private String commandTpHere = "tph";
        @ConfigEntry.Gui.Tooltip(zh_cn = "接受请求将玩家传送至当前位置的指令。", en_us = "This command is used to accept teleportation to other players.")
        private String commandTpHereYes = "tphy";
        @ConfigEntry.Gui.Tooltip(zh_cn = "拒绝请求将玩家传送至当前位置的指令。", en_us = "This command is used to refuse teleportation to other players.")
        private String commandTpHereNo = "tphn";
        @ConfigEntry.Gui.Tooltip(zh_cn = "取消请求将玩家传送至当前位置的指令。", en_us = "This command is used to cancel the request to teleport to other players.")
        private String commandTpHereCancel = "tphc";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CommandTpHomeNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家的指令。", en_us = "The command to teleport to the home.")
        private String commandTpHome = "home";
        @ConfigEntry.Gui.Tooltip(zh_cn = "设置家的指令。", en_us = "The command to set the home.")
        private String commandSetHome = "sethome";
        @ConfigEntry.Gui.Tooltip(zh_cn = "删除家的指令。", en_us = "The command to delete the home.")
        private String commandDelHome = "delhome";
        @ConfigEntry.Gui.Tooltip(zh_cn = "查询家的信息的指令。", en_us = "The command to get the home info.")
        private String commandGetHome = "gethome";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class CommandTpStageNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站的指令。", en_us = "The command to teleport to the stage.")
        private String commandTpStage = "stage";
        @ConfigEntry.Gui.Tooltip(zh_cn = "设置驿站的指令。", en_us = "The command to set the stage.")
        private String commandSetStage = "setstage";
        @ConfigEntry.Gui.Tooltip(zh_cn = "删除驿站的指令。", en_us = "The command to delete the stage.")
        private String commandDelStage = "delstage";
        @ConfigEntry.Gui.Tooltip(zh_cn = "查询驿站的信息的的指令。", en_us = "The command to get the stage info.")
        private String commandGetStage = "getstage";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class ConciseCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '设置语言' 指令。", en_us = "Enable or disable the concise version of the 'Set the language' command.")
        private boolean conciseLanguage = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '获取玩家的UUID' 指令。", en_us = "Enable or disable the concise version of the 'Get the UUID of the player' command.")
        private boolean conciseUuid = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '获取当前世界的维度ID' 指令。", en_us = "Enable or disable the concise version of the 'Get the dimension ID of the current world' command.")
        private boolean conciseDimension = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '获取玩家的传送卡数量' 指令。", en_us = "Enable or disable the concise version of the 'Get the number of Teleport Card of the player' command.")
        private boolean conciseCard = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '分享驿站、玩家的私人传送点、玩家当前坐标' 指令。", en_us = "Enable or disable the concise version of the 'Share the stage, the personal home, and the current safeWorldCoordinate of player' command.")
        private boolean conciseShare = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '自杀或毒杀' 指令。", en_us = "Enable or disable the concise version of the 'Suicide or poisoning' command.")
        private boolean conciseFeed = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '传送到指定坐标' 指令。", en_us = "Enable or disable the concise version of the 'Teleport to the specified coordinates' command.")
        private boolean conciseTpCoordinate = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '传送到指定结构' 指令。", en_us = "Enable or disable the concise version of the 'Teleport to the specified structure' command.")
        private boolean conciseTpStructure = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '随机传送' 指令。", en_us = "Enable or disable the concise version of the 'Teleport to a random location' command.")
        private boolean conciseTpRandom = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '传送到玩家重生点' 指令。", en_us = "Enable or disable the concise version of the 'Teleport to the spawn of the player' command.")
        private boolean conciseTpSpawn = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '传送到世界重生点' 指令。", en_us = "Enable or disable the concise version of the 'Teleport to the spawn of the world' command.")
        private boolean conciseTpWorldSpawn = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '传送到顶部' 指令。", en_us = "Enable or disable the concise version of the 'Teleport to the top of current position' command.")
        private boolean conciseTpTop = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '传送到底部' 指令。", en_us = "Enable or disable the concise version of the 'Teleport to the bottom of current position' command.")
        private boolean conciseTpBottom = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '传送到上方' 指令。", en_us = "Enable or disable the concise version of the 'Teleport to the upper of current position' command.")
        private boolean conciseTpUp = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '传送到下方' 指令。", en_us = "Enable or disable the concise version of the 'Teleport to the lower of current position' command.")
        private boolean conciseTpDown = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '传送至视线尽头' 指令。该功能与玩家设置的视距无关。", en_us = "Enable or disable the concise version of the 'Teleport to the end of the line of sight' command. This function is independent of the player's render distance setting.")
        private boolean conciseTpView = false;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '传送到上次传送点' 指令。", en_us = "Enable or disable the concise version of the 'Teleport to the previous location' command.")
        private boolean conciseTpBack = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '坟墓传送' 指令。", en_us = "Enable or disable the concise version of the 'Teleport to the grave / last death location' command.")
        private boolean conciseTpGrave = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '飞行' 指令。", en_us = "Enable or disable the concise version of the 'Creative flight' command.")
        private boolean conciseFly = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '设置虚拟权限' 指令。", en_us = "Enable or disable the concise version of the 'Set virtual permission' command.")
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
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '请求传送至玩家' 指令。", en_us = "Enable or disable the concise version of the 'Request to teleport oneself to other players' command.")
        private boolean conciseTpAsk = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '接受请求传送至玩家' 指令。", en_us = "Enable or disable the concise version of the 'Accept teleportation of other players to oneself' command.")
        private boolean conciseTpAskYes = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '拒绝请求传送至玩家' 指令。", en_us = "Enable or disable the concise version of the 'Refuse teleportation of other players to oneself' command.")
        private boolean conciseTpAskNo = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '取消请求传送至玩家' 指令。", en_us = "Enable or disable the concise version of the 'Cancel the request to teleport to other players' command.")
        private boolean conciseTpAskCancel = true;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class ConciseTpHereNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '请求将玩家传送至当前位置' 指令。", en_us = "Enable or disable the concise version of the 'Request the transfer of other players to oneself' command.")
        private boolean conciseTpHere = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '接受请求将玩家传送至当前位置' 指令。", en_us = "Enable or disable the concise version of the 'Accept teleportation to other players' command.")
        private boolean conciseTpHereYes = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '拒绝请求将玩家传送至当前位置' 指令。", en_us = "Enable or disable the concise version of the 'Refuse teleportation to other players' command.")
        private boolean conciseTpHereNo = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '取消请求将玩家传送至当前位置' 指令。", en_us = "Enable or disable the concise version of the 'Cancel the request to teleport to other players' command.")
        private boolean conciseTpHereCancel = true;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class ConciseTpHomeNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '传送到家' 指令。", en_us = "Enable or disable the concise version of the 'Teleport to the home' command.")
        private boolean conciseTpHome = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '设置家' 指令。", en_us = "Enable or disable the concise version of the 'Set the home' command.")
        private boolean conciseSetHome = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '删除家' 指令。", en_us = "Enable or disable the concise version of the 'Delete the home' command.")
        private boolean conciseDelHome = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '查询家' 指令。", en_us = "Enable or disable the concise version of the 'Get the home info' command.")
        private boolean conciseGetHome = true;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class ConciseTpStageNames {
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '传送到驿站' 指令。", en_us = "Enable or disable the concise version of the 'Teleport to the stage' command.")
        private boolean conciseTpStage = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '设置驿站' 指令。", en_us = "Enable or disable the concise version of the 'Set the stage' command.")
        private boolean conciseSetStage = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '删除驿站' 指令。", en_us = "Enable or disable the concise version of the 'Delete the stage' command.")
        private boolean conciseDelStage = true;
        @ConfigEntry.Gui.Tooltip(zh_cn = "是否启用无前缀版本的 '查询驿站' 指令。", en_us = "Enable or disable the concise version of the 'Get the stage info' command.")
        private boolean conciseGetStage = true;
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpCoordinateCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定坐标的代价类型。", en_us = "The cost type for 'Teleport to the specified coordinates'")
        private EnumCostType costTpCoordinateType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定坐标的代价数量。", en_us = "The number of cost for 'Teleport to the specified coordinates'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpCoordinateNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定坐标的代价配置：", en_us = "The configuration for 'Teleport to the specified coordinates'.")
        private String costTpCoordinateConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定坐标的代价倍率，代价会乘以传送前后坐标之间的距离。", en_us = "The cost rate for 'Teleport to the specified coordinates', the cost will be multiplied by the distance between the two coordinates.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpCoordinateRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpCoordinateNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpCoordinateNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpCoordinateExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpStructureCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定结构的代价类型。", en_us = "The cost type for 'Teleport to the specified structure'")
        private EnumCostType costTpStructureType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定结构的代价数量。", en_us = "The number of cost for 'Teleport to the specified structure'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpStructureNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定结构的代价配置：", en_us = "The configuration for 'Teleport to the specified structure'.")
        private String costTpStructureConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到指定结构的代价倍率，代价会乘以传送前后坐标之间的距离。", en_us = "The cost rate for 'Teleport to the specified structure', the cost will be multiplied by the distance between the two coordinates.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpStructureRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpStructureNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpStructureNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpStructureExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpAskCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家的代价类型。", en_us = "The cost type for 'Request to teleport oneself to other players'")
        private EnumCostType costTpAskType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家的代价数量。", en_us = "The number of cost for 'Request to teleport oneself to other players'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpAskNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家的代价配置：", en_us = "The configuration for 'Request to teleport oneself to other players'.")
        private String costTpAskConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求传送至玩家的代价倍率，代价会乘以两个玩家之间的距离。", en_us = "The cost rate for 'Request to teleport oneself to other players', the cost will be multiplied by the distance between the two players")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpAskRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpAskNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpAskNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpAskExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpHereCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传送至当前位置的代价类型。", en_us = "The cost type for 'Request the transfer of other players to oneself'")
        private EnumCostType costTpHereType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传送至当前位置的代价数量。", en_us = "The number of cost for 'Request the transfer of other players to oneself'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpHereNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传送至当前位置的代价配置：", en_us = "The configuration for 'Request the transfer of other players to oneself'.")
        private String costTpHereConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "请求将玩家传送至当前位置的代价倍率，代价会乘以两个玩家之间的距离。", en_us = "The cost rate for 'Request the transfer of other players to oneself', the cost will be multiplied by the distance between the two players")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpHereRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpHereNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpHereNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpHereExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpRandomCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送的代价类型。", en_us = "The cost type for 'Teleport to a random location'")
        private EnumCostType costTpRandomType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送的代价数量。", en_us = "The number of cost for 'Teleport to a random location'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpRandomNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送的代价配置：", en_us = "The configuration for 'Teleport to a random location'.")
        private String costTpRandomConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "随机传送的代价倍率，代价会乘以传送前后坐标之间的距离。", en_us = "The cost rate for 'Teleport to a random location', the cost will be multiplied by the distance between the two coordinates.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpRandomRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpRandomNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpRandomNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpRandomExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpSpawnCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到玩家重生点的代价类型。", en_us = "The cost type for 'Teleport to the spawn of the player'")
        private EnumCostType costTpSpawnType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到玩家重生点的代价数量。", en_us = "The number of cost for 'Teleport to the spawn of the player'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpSpawnNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到玩家重生点的代价配置：", en_us = "The configuration for 'Teleport to the spawn of the player'.")
        private String costTpSpawnConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到玩家重生点的代价倍率，代价会乘以传送前后坐标之间的距离。", en_us = "The cost rate for 'Teleport to the spawn of the player', the cost will be multiplied by the distance between the two coordinates.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpSpawnRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpSpawnNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpSpawnNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpSpawnExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpWorldSpawnCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到世界重生点的代价类型。", en_us = "The cost type for 'Teleport to the spawn of the world'")
        private EnumCostType costTpWorldSpawnType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到世界重生点的代价数量。", en_us = "The number of cost for 'Teleport to the spawn of the world'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpWorldSpawnNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到世界重生点的代价配置：", en_us = "The configuration for 'Teleport to the spawn of the world'.")
        private String costTpWorldSpawnConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到世界重生点的代价倍率，代价会乘以传送前后坐标之间的距离。", en_us = "The cost rate for 'Teleport to the spawn of the world', the cost will be multiplied by the distance between the two coordinates.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpWorldSpawnRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpWorldSpawnNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpWorldSpawnNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpWorldSpawnExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpTopCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到顶部的代价类型。", en_us = "The cost type for 'Teleport to the top of current position'")
        private EnumCostType costTpTopType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到顶部的代价数量。", en_us = "The number of cost for 'Teleport to the top of current position'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpTopNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到顶部的代价配置：", en_us = "The configuration for 'Teleport to the top of current position'.")
        private String costTpTopConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到顶部的代价倍率，代价会乘以传送前后坐标之间的距离。", en_us = "The cost rate for 'Teleport to the top of current position', the cost will be multiplied by the distance between the two coordinates.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpTopRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpTopNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpTopNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpTopExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpBottomCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到底部的代价类型。", en_us = "The cost type for 'Teleport to the bottom of current position'")
        private EnumCostType costTpBottomType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到底部的代价数量。", en_us = "The number of cost for 'Teleport to the bottom of current position'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpBottomNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到底部的代价配置：", en_us = "The configuration for 'Teleport to the bottom of current position'.")
        private String costTpBottomConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到底部的代价倍率，代价会乘以传送前后坐标之间的距离。", en_us = "The cost rate for 'Teleport to the bottom of current position', the cost will be multiplied by the distance between the two coordinates.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpBottomRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpBottomNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpBottomNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpBottomExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpUpCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上方的代价类型。", en_us = "The cost type for 'Teleport to the upper of current position'")
        private EnumCostType costTpUpType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上方的代价数量。", en_us = "The number of cost for 'Teleport to the upper of current position'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpUpNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上方的代价配置：", en_us = "The configuration for 'Teleport to the upper of current position'.")
        private String costTpUpConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上方的代价倍率，代价会乘以传送前后坐标之间的距离。", en_us = "The cost rate for 'Teleport to the upper of current position', the cost will be multiplied by the distance between the two coordinates.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpUpRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpUpNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpUpNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpUpExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpDownCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到下方的代价类型。", en_us = "The cost type for 'Teleport to the lower of current position'")
        private EnumCostType costTpDownType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到下方的代价数量。", en_us = "The number of cost for 'Teleport to the lower of current position'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpDownNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到下方的代价配置：", en_us = "The configuration for 'Teleport to the lower of current position'.")
        private String costTpDownConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到下方的代价倍率，代价会乘以传送前后坐标之间的距离。", en_us = "The cost rate for 'Teleport to the lower of current position', the cost will be multiplied by the distance between the two coordinates.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpDownRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpDownNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpDownNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpDownExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpViewCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至视线尽头的代价类型。该功能与玩家设置的视距无关。", en_us = "The cost type for 'Teleport to the end of the line of sight'. This function is independent of the player's render distance setting.")
        private EnumCostType costTpViewType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至视线尽头的代价数量。该功能与玩家设置的视距无关。", en_us = "The number of cost for 'Teleport to the end of the line of sight'. This function is independent of the player's render distance setting.")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpViewNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至视线尽头的代价配置：该功能与玩家设置的视距无关。", en_us = "The configuration for 'Teleport to the end of the line of sight'. This function is independent of the player's render distance setting.")
        private String costTpViewConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送至视线尽头的代价倍率，代价会乘以传送前后坐标之间的距离。该功能与玩家设置的视距无关。", en_us = "The cost rate for 'Teleport to the end of the line of sight', the cost will be multiplied by the distance between the two coordinates. This function is independent of the player's render distance setting.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpViewRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpViewNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpViewNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpViewExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpHomeCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家的代价类型。", en_us = "The cost type for 'Teleport to the home'")
        private EnumCostType costTpHomeType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家的代价数量。", en_us = "The number of cost for 'Teleport to the home'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpHomeNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家的代价配置：", en_us = "The configuration for 'Teleport to the home'.")
        private String costTpHomeConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到家的代价倍率，代价会乘以传送前后坐标之间的距离。", en_us = "The cost rate for 'Teleport to the home', the cost will be multiplied by the distance between the two coordinates.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpHomeRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpHomeNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpHomeNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpHomeExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpStageCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站的代价类型。", en_us = "The cost type for 'Teleport to the stage'")
        private EnumCostType costTpStageType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站的代价数量。", en_us = "The number of cost for 'Teleport to the stage'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpStageNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站的代价配置：", en_us = "The configuration for 'Teleport to the stage'.")
        private String costTpStageConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到驿站的代价倍率，代价会乘以传送前后坐标之间的距离。", en_us = "The cost rate for 'Teleport to the stage', the cost will be multiplied by the distance between the two coordinates.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpStageRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpStageNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpStageNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpStageExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpBackCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上次传送点的代价类型。", en_us = "The cost type for 'Teleport to the previous location'")
        private EnumCostType costTpBackType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上次传送点的代价数量。", en_us = "The number of cost for 'Teleport to the previous location'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpBackNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上次传送点的代价配置：", en_us = "The configuration for 'Teleport to the previous location'.")
        private String costTpBackConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送到上次传送点的代价倍率，代价会乘以传送前后坐标之间的距离。", en_us = "The cost rate for 'Teleport to the previous location', the cost will be multiplied by the distance between the two coordinates.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpBackRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpBackNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpBackNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpBackExp = "num * distance * rate";
    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class TpGraveCostGroup {
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送的代价类型。", en_us = "The cost type for 'Teleport to the grave'")
        private EnumCostType costTpGraveType = EnumCostType.NONE;
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送的代价数量。", en_us = "The number of cost for 'Teleport to the grave'")
        @ConfigEntry.BoundedDiscrete(max = 9999)
        private int costTpGraveNum = 1;
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送的代价配置：", en_us = "The configuration for 'Teleport to the grave'.")
        private String costTpGraveConf = "";
        @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送的代价倍率，代价会乘以传送前后坐标之间的距离。", en_us = "The cost rate for 'Teleport to the grave', the cost will be multiplied by the distance between the two coordinates.")
        @ConfigEntry.BoundedDouble(max = 9999, decimalPlaces = 4)
        private double costTpGraveRate = 0.002;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价上限（与其它传送代价子表同义）。", en_us = "Upper cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpGraveNumUpper = 20;
        @ConfigEntry.Gui.Tooltip(zh_cn = "计算后代价下限（与其它传送代价子表同义）。", en_us = "Lower cap of computed cost after formula (same meaning as other Tp* cost groups).")
        @ConfigEntry.BoundedDiscrete()
        private int costTpGraveNumLower = 0;
        @ConfigEntry.Gui.Tooltip(zh_cn = "代价计算公式字符串（变量以模组说明为准；默认使用 num、distance、rate 等）。", en_us = "Cost expression string (variables depend on mod documentation; default uses num, distance, rate).")
        private String costTpGraveExp = "num * distance * rate";
    }


    // region 运行时视图接口

    public interface RootView {
        BaseView base();

        FeatureSwitchView featureSwitch();

        CommandNamesView commandNames();

        ConciseCommandsView conciseCommands();

        ConciseCommandsView concise();

        GeneralView general();

        PermissionView permission();

        CooldownView cooldown();

        TeleportCountdownView teleportCountdown();

        CostView cost();

        ConfigHolder holder();

        void save();
    }

    public interface BaseView {
        boolean teleportCard();

        BaseView teleportCard(boolean value);

        int teleportCardDaily();

        BaseView teleportCardDaily(int value);

        EnumCardType teleportCardType();

        BaseView teleportCardType(EnumCardType value);

        boolean removeOriginalTp();

        BaseView removeOriginalTp(boolean value);

        double flySpeedMin();

        BaseView flySpeedMin(double value);

        double flySpeedMax();

        BaseView flySpeedMax(double value);
    }

    public interface FeatureSwitchView {
        boolean switchShare();

        FeatureSwitchView switchShare(boolean value);

        boolean switchFeed();

        FeatureSwitchView switchFeed(boolean value);

        boolean switchTpCoordinate();

        FeatureSwitchView switchTpCoordinate(boolean value);

        boolean switchTpStructure();

        FeatureSwitchView switchTpStructure(boolean value);

        boolean switchTpAsk();

        FeatureSwitchView switchTpAsk(boolean value);

        boolean switchTpHere();

        FeatureSwitchView switchTpHere(boolean value);

        boolean switchTpRandom();

        FeatureSwitchView switchTpRandom(boolean value);

        boolean switchTpSpawn();

        FeatureSwitchView switchTpSpawn(boolean value);

        boolean switchTpWorldSpawn();

        FeatureSwitchView switchTpWorldSpawn(boolean value);

        boolean switchTpTop();

        FeatureSwitchView switchTpTop(boolean value);

        boolean switchTpBottom();

        FeatureSwitchView switchTpBottom(boolean value);

        boolean switchTpUp();

        FeatureSwitchView switchTpUp(boolean value);

        boolean switchTpDown();

        FeatureSwitchView switchTpDown(boolean value);

        boolean switchTpView();

        FeatureSwitchView switchTpView(boolean value);

        boolean switchTpHome();

        FeatureSwitchView switchTpHome(boolean value);

        boolean switchTpStage();

        FeatureSwitchView switchTpStage(boolean value);

        boolean switchTpBack();

        FeatureSwitchView switchTpBack(boolean value);

        boolean switchTpGrave();

        FeatureSwitchView switchTpGrave(boolean value);

        boolean switchFly();

        FeatureSwitchView switchFly(boolean value);
    }

    public interface CommandNamesView {
        String commandPrefix();

        CommandNamesView commandPrefix(String value);

        String commandUuid();

        CommandNamesView commandUuid(String value);

        String commandDimension();

        CommandNamesView commandDimension(String value);

        String commandCard();

        CommandNamesView commandCard(String value);

        String commandShare();

        CommandNamesView commandShare(String value);

        String commandFeed();

        CommandNamesView commandFeed(String value);

        String commandTpCoordinate();

        CommandNamesView commandTpCoordinate(String value);

        String commandTpStructure();

        CommandNamesView commandTpStructure(String value);

        String commandTpRandom();

        CommandNamesView commandTpRandom(String value);

        String commandTpSpawn();

        CommandNamesView commandTpSpawn(String value);

        String commandTpWorldSpawn();

        CommandNamesView commandTpWorldSpawn(String value);

        String commandTpTop();

        CommandNamesView commandTpTop(String value);

        String commandTpBottom();

        CommandNamesView commandTpBottom(String value);

        String commandTpUp();

        CommandNamesView commandTpUp(String value);

        String commandTpDown();

        CommandNamesView commandTpDown(String value);

        String commandTpView();

        CommandNamesView commandTpView(String value);

        String commandTpBack();

        CommandNamesView commandTpBack(String value);

        String commandTpGrave();

        CommandNamesView commandTpGrave(String value);

        String commandFly();

        CommandNamesView commandFly(String value);

        String commandTpAsk();

        CommandNamesView commandTpAsk(String value);

        String commandTpAskYes();

        CommandNamesView commandTpAskYes(String value);

        String commandTpAskNo();

        CommandNamesView commandTpAskNo(String value);

        String commandTpAskCancel();

        CommandNamesView commandTpAskCancel(String value);

        String commandTpHere();

        CommandNamesView commandTpHere(String value);

        String commandTpHereYes();

        CommandNamesView commandTpHereYes(String value);

        String commandTpHereNo();

        CommandNamesView commandTpHereNo(String value);

        String commandTpHereCancel();

        CommandNamesView commandTpHereCancel(String value);

        String commandTpHome();

        CommandNamesView commandTpHome(String value);

        String commandSetHome();

        CommandNamesView commandSetHome(String value);

        String commandDelHome();

        CommandNamesView commandDelHome(String value);

        String commandGetHome();

        CommandNamesView commandGetHome(String value);

        String commandTpStage();

        CommandNamesView commandTpStage(String value);

        String commandSetStage();

        CommandNamesView commandSetStage(String value);

        String commandDelStage();

        CommandNamesView commandDelStage(String value);

        String commandGetStage();

        CommandNamesView commandGetStage(String value);

        CommandTpAskView tpAsk();

        CommandTpHereView tpHere();

        CommandTpHomeView tpHome();

        CommandTpStageView tpStage();
    }

    public interface CommandTpAskView {
        String commandTpAsk();

        CommandTpAskView commandTpAsk(String value);

        String commandTpAskYes();

        CommandTpAskView commandTpAskYes(String value);

        String commandTpAskNo();

        CommandTpAskView commandTpAskNo(String value);

        String commandTpAskCancel();

        CommandTpAskView commandTpAskCancel(String value);
    }

    public interface CommandTpHereView {
        String commandTpHere();

        CommandTpHereView commandTpHere(String value);

        String commandTpHereYes();

        CommandTpHereView commandTpHereYes(String value);

        String commandTpHereNo();

        CommandTpHereView commandTpHereNo(String value);

        String commandTpHereCancel();

        CommandTpHereView commandTpHereCancel(String value);
    }

    public interface CommandTpHomeView {
        String commandTpHome();

        CommandTpHomeView commandTpHome(String value);

        String commandSetHome();

        CommandTpHomeView commandSetHome(String value);

        String commandDelHome();

        CommandTpHomeView commandDelHome(String value);

        String commandGetHome();

        CommandTpHomeView commandGetHome(String value);
    }

    public interface CommandTpStageView {
        String commandTpStage();

        CommandTpStageView commandTpStage(String value);

        String commandSetStage();

        CommandTpStageView commandSetStage(String value);

        String commandDelStage();

        CommandTpStageView commandDelStage(String value);

        String commandGetStage();

        CommandTpStageView commandGetStage(String value);
    }

    public interface ConciseCommandsView {
        boolean conciseLanguage();

        ConciseCommandsView conciseLanguage(boolean value);

        boolean conciseUuid();

        ConciseCommandsView conciseUuid(boolean value);

        boolean conciseDimension();

        ConciseCommandsView conciseDimension(boolean value);

        boolean conciseCard();

        ConciseCommandsView conciseCard(boolean value);

        boolean conciseShare();

        ConciseCommandsView conciseShare(boolean value);

        boolean conciseFeed();

        ConciseCommandsView conciseFeed(boolean value);

        boolean conciseTpCoordinate();

        ConciseCommandsView conciseTpCoordinate(boolean value);

        boolean conciseTpStructure();

        ConciseCommandsView conciseTpStructure(boolean value);

        boolean conciseTpRandom();

        ConciseCommandsView conciseTpRandom(boolean value);

        boolean conciseTpSpawn();

        ConciseCommandsView conciseTpSpawn(boolean value);

        boolean conciseTpWorldSpawn();

        ConciseCommandsView conciseTpWorldSpawn(boolean value);

        boolean conciseTpTop();

        ConciseCommandsView conciseTpTop(boolean value);

        boolean conciseTpBottom();

        ConciseCommandsView conciseTpBottom(boolean value);

        boolean conciseTpUp();

        ConciseCommandsView conciseTpUp(boolean value);

        boolean conciseTpDown();

        ConciseCommandsView conciseTpDown(boolean value);

        boolean conciseTpView();

        ConciseCommandsView conciseTpView(boolean value);

        boolean conciseTpBack();

        ConciseCommandsView conciseTpBack(boolean value);

        boolean conciseTpGrave();

        ConciseCommandsView conciseTpGrave(boolean value);

        boolean conciseFly();

        ConciseCommandsView conciseFly(boolean value);

        boolean conciseVirtualOp();

        ConciseCommandsView conciseVirtualOp(boolean value);

        boolean conciseTpAsk();

        ConciseCommandsView conciseTpAsk(boolean value);

        boolean conciseTpAskYes();

        ConciseCommandsView conciseTpAskYes(boolean value);

        boolean conciseTpAskNo();

        ConciseCommandsView conciseTpAskNo(boolean value);

        boolean conciseTpAskCancel();

        ConciseCommandsView conciseTpAskCancel(boolean value);

        boolean conciseTpHere();

        ConciseCommandsView conciseTpHere(boolean value);

        boolean conciseTpHereYes();

        ConciseCommandsView conciseTpHereYes(boolean value);

        boolean conciseTpHereNo();

        ConciseCommandsView conciseTpHereNo(boolean value);

        boolean conciseTpHereCancel();

        ConciseCommandsView conciseTpHereCancel(boolean value);

        boolean conciseTpHome();

        ConciseCommandsView conciseTpHome(boolean value);

        boolean conciseSetHome();

        ConciseCommandsView conciseSetHome(boolean value);

        boolean conciseDelHome();

        ConciseCommandsView conciseDelHome(boolean value);

        boolean conciseGetHome();

        ConciseCommandsView conciseGetHome(boolean value);

        boolean conciseTpStage();

        ConciseCommandsView conciseTpStage(boolean value);

        boolean conciseSetStage();

        ConciseCommandsView conciseSetStage(boolean value);

        boolean conciseDelStage();

        ConciseCommandsView conciseDelStage(boolean value);

        boolean conciseGetStage();

        ConciseCommandsView conciseGetStage(boolean value);

        ConciseTpAskView tpAsk();

        ConciseTpHereView tpHere();

        ConciseTpHomeView tpHome();

        ConciseTpStageView tpStage();
    }

    public interface ConciseTpAskView {
        boolean conciseTpAsk();

        ConciseTpAskView conciseTpAsk(boolean value);

        boolean conciseTpAskYes();

        ConciseTpAskView conciseTpAskYes(boolean value);

        boolean conciseTpAskNo();

        ConciseTpAskView conciseTpAskNo(boolean value);

        boolean conciseTpAskCancel();

        ConciseTpAskView conciseTpAskCancel(boolean value);
    }

    public interface ConciseTpHereView {
        boolean conciseTpHere();

        ConciseTpHereView conciseTpHere(boolean value);

        boolean conciseTpHereYes();

        ConciseTpHereView conciseTpHereYes(boolean value);

        boolean conciseTpHereNo();

        ConciseTpHereView conciseTpHereNo(boolean value);

        boolean conciseTpHereCancel();

        ConciseTpHereView conciseTpHereCancel(boolean value);
    }

    public interface ConciseTpHomeView {
        boolean conciseTpHome();

        ConciseTpHomeView conciseTpHome(boolean value);

        boolean conciseSetHome();

        ConciseTpHomeView conciseSetHome(boolean value);

        boolean conciseDelHome();

        ConciseTpHomeView conciseDelHome(boolean value);

        boolean conciseGetHome();

        ConciseTpHomeView conciseGetHome(boolean value);
    }

    public interface ConciseTpStageView {
        boolean conciseTpStage();

        ConciseTpStageView conciseTpStage(boolean value);

        boolean conciseSetStage();

        ConciseTpStageView conciseSetStage(boolean value);

        boolean conciseDelStage();

        ConciseTpStageView conciseDelStage(boolean value);

        boolean conciseGetStage();

        ConciseTpStageView conciseGetStage(boolean value);
    }

    public interface GeneralView {
        int teleportRecordLimit();

        GeneralView teleportRecordLimit(int value);

        List<String> teleportBackSkipType();

        GeneralView teleportBackSkipType(List<String> value);

        boolean teleportAcrossDimension();

        GeneralView teleportAcrossDimension(boolean value);

        int teleportCostDistanceLimit();

        GeneralView teleportCostDistanceLimit(int value);

        int teleportCostDistanceAcrossDimension();

        GeneralView teleportCostDistanceAcrossDimension(int value);

        int teleportViewDistanceLimit();

        GeneralView teleportViewDistanceLimit(int value);

        int teleportRequestExpireTime();

        GeneralView teleportRequestExpireTime(int value);

        EnumCoolDownType teleportRequestCooldownType();

        GeneralView teleportRequestCooldownType(EnumCoolDownType value);

        int teleportRequestCooldown();

        GeneralView teleportRequestCooldown(int value);

        int teleportRandomDistanceLimit();

        GeneralView teleportRandomDistanceLimit(int value);

        int tpRandomSafeNotFoundRetries();

        GeneralView tpRandomSafeNotFoundRetries(int value);

        int graveSearchRangeLimit();

        GeneralView graveSearchRangeLimit(int value);

        int teleportHomeLimit();

        GeneralView teleportHomeLimit(int value);

        String helpHeader();

        GeneralView helpHeader(String value);

        String tpSound();

        GeneralView tpSound(String value);

        boolean tpWithVehicle();

        GeneralView tpWithVehicle(boolean value);

        boolean tpWithFollower();

        GeneralView tpWithFollower(boolean value);

        int tpWithFollowerRange();

        GeneralView tpWithFollowerRange(int value);

        int helpInfoNumPerPage();

        GeneralView helpInfoNumPerPage(int value);

        String defaultLanguage();

        GeneralView defaultLanguage(String value);

        boolean tpWithEnemy();

        GeneralView tpWithEnemy(boolean value);

        String tpSpawnNoBedWorldDimension();

        GeneralView tpSpawnNoBedWorldDimension(String value);

        SafeTeleportView safeTeleport();
    }

    public interface SafeTeleportView {
        List<String> unsafeBlocks();

        SafeTeleportView unsafeBlocks(List<String> value);

        List<String> suffocatingBlocks();

        SafeTeleportView suffocatingBlocks(List<String> value);

        boolean setBlockWhenSafeNotFound();

        SafeTeleportView setBlockWhenSafeNotFound(boolean value);

        boolean getBlockFromInventory();

        SafeTeleportView getBlockFromInventory(boolean value);

        List<String> safeBlocks();

        SafeTeleportView safeBlocks(List<String> value);

        int safeChunkRange();

        SafeTeleportView safeChunkRange(int value);
    }

    public interface PermissionView {
        int permissionFeedOther();

        PermissionView permissionFeedOther(int value);

        int permissionTpCoordinate();

        PermissionView permissionTpCoordinate(int value);

        int permissionTpStructure();

        PermissionView permissionTpStructure(int value);

        int permissionTpAsk();

        PermissionView permissionTpAsk(int value);

        int permissionTpHere();

        PermissionView permissionTpHere(int value);

        int permissionTpRandom();

        PermissionView permissionTpRandom(int value);

        int permissionTpSpawn();

        PermissionView permissionTpSpawn(int value);

        int permissionTpSpawnOther();

        PermissionView permissionTpSpawnOther(int value);

        int permissionTpWorldSpawn();

        PermissionView permissionTpWorldSpawn(int value);

        int permissionTpTop();

        PermissionView permissionTpTop(int value);

        int permissionTpBottom();

        PermissionView permissionTpBottom(int value);

        int permissionTpUp();

        PermissionView permissionTpUp(int value);

        int permissionTpDown();

        PermissionView permissionTpDown(int value);

        int permissionTpView();

        PermissionView permissionTpView(int value);

        int permissionTpHome();

        PermissionView permissionTpHome(int value);

        int permissionTpStage();

        PermissionView permissionTpStage(int value);

        int permissionTpStageSet();

        PermissionView permissionTpStageSet(int value);

        int permissionTpStageDel();

        PermissionView permissionTpStageDel(int value);

        int permissionTpStageGet();

        PermissionView permissionTpStageGet(int value);

        int permissionTpBack();

        PermissionView permissionTpBack(int value);

        int permissionTpGrave();

        PermissionView permissionTpGrave(int value);

        int permissionFly();

        PermissionView permissionFly(int value);

        int permissionVirtualOp();

        PermissionView permissionVirtualOp(int value);

        int permissionSetCard();

        PermissionView permissionSetCard(int value);

        int permissionTpCoordinateAcrossDimension();

        PermissionView permissionTpCoordinateAcrossDimension(int value);

        int permissionTpStructureAcrossDimension();

        PermissionView permissionTpStructureAcrossDimension(int value);

        int permissionTpAskAcrossDimension();

        PermissionView permissionTpAskAcrossDimension(int value);

        int permissionTpHereAcrossDimension();

        PermissionView permissionTpHereAcrossDimension(int value);

        int permissionTpRandomAcrossDimension();

        PermissionView permissionTpRandomAcrossDimension(int value);

        int permissionTpSpawnAcrossDimension();

        PermissionView permissionTpSpawnAcrossDimension(int value);

        int permissionTpWorldSpawnAcrossDimension();

        PermissionView permissionTpWorldSpawnAcrossDimension(int value);

        int permissionTpHomeAcrossDimension();

        PermissionView permissionTpHomeAcrossDimension(int value);

        int permissionTpStageAcrossDimension();

        PermissionView permissionTpStageAcrossDimension(int value);

        int permissionTpBackAcrossDimension();

        PermissionView permissionTpBackAcrossDimension(int value);

        int permissionTpGraveAcrossDimension();

        PermissionView permissionTpGraveAcrossDimension(int value);
    }

    public interface CooldownView {
        int cooldownTpCoordinate();

        CooldownView cooldownTpCoordinate(int value);

        int cooldownTpStructure();

        CooldownView cooldownTpStructure(int value);

        int cooldownTpAsk();

        CooldownView cooldownTpAsk(int value);

        int cooldownTpHere();

        CooldownView cooldownTpHere(int value);

        int cooldownTpRandom();

        CooldownView cooldownTpRandom(int value);

        int cooldownTpSpawn();

        CooldownView cooldownTpSpawn(int value);

        int cooldownTpWorldSpawn();

        CooldownView cooldownTpWorldSpawn(int value);

        int cooldownTpTop();

        CooldownView cooldownTpTop(int value);

        int cooldownTpBottom();

        CooldownView cooldownTpBottom(int value);

        int cooldownTpUp();

        CooldownView cooldownTpUp(int value);

        int cooldownTpDown();

        CooldownView cooldownTpDown(int value);

        int cooldownTpView();

        CooldownView cooldownTpView(int value);

        int cooldownTpHome();

        CooldownView cooldownTpHome(int value);

        int cooldownTpStage();

        CooldownView cooldownTpStage(int value);

        int cooldownTpBack();

        CooldownView cooldownTpBack(int value);

        int cooldownTpGrave();

        CooldownView cooldownTpGrave(int value);
    }

    public interface TeleportCountdownView {
        ServerPerTypeTeleportCountdownView server();

        boolean forceServerCountdown();

        TeleportCountdownView forceServerCountdown(boolean value);

        int playerCountdownRangeMin();

        TeleportCountdownView playerCountdownRangeMin(int value);

        int playerCountdownRangeMax();

        TeleportCountdownView playerCountdownRangeMax(int value);

        boolean cancelCountdownOnPlayerMove();

        TeleportCountdownView cancelCountdownOnPlayerMove(boolean value);

        boolean cancelCountdownOnPlayerDamage();

        TeleportCountdownView cancelCountdownOnPlayerDamage(boolean value);
    }

    public interface ServerPerTypeTeleportCountdownView {
        int serverCountdownTpCoordinate();

        ServerPerTypeTeleportCountdownView serverCountdownTpCoordinate(int value);

        int serverCountdownTpStructure();

        ServerPerTypeTeleportCountdownView serverCountdownTpStructure(int value);

        int serverCountdownTpAsk();

        ServerPerTypeTeleportCountdownView serverCountdownTpAsk(int value);

        int serverCountdownTpHere();

        ServerPerTypeTeleportCountdownView serverCountdownTpHere(int value);

        int serverCountdownTpRandom();

        ServerPerTypeTeleportCountdownView serverCountdownTpRandom(int value);

        int serverCountdownTpSpawn();

        ServerPerTypeTeleportCountdownView serverCountdownTpSpawn(int value);

        int serverCountdownTpWorldSpawn();

        ServerPerTypeTeleportCountdownView serverCountdownTpWorldSpawn(int value);

        int serverCountdownTpTop();

        ServerPerTypeTeleportCountdownView serverCountdownTpTop(int value);

        int serverCountdownTpBottom();

        ServerPerTypeTeleportCountdownView serverCountdownTpBottom(int value);

        int serverCountdownTpUp();

        ServerPerTypeTeleportCountdownView serverCountdownTpUp(int value);

        int serverCountdownTpDown();

        ServerPerTypeTeleportCountdownView serverCountdownTpDown(int value);

        int serverCountdownTpView();

        ServerPerTypeTeleportCountdownView serverCountdownTpView(int value);

        int serverCountdownTpHome();

        ServerPerTypeTeleportCountdownView serverCountdownTpHome(int value);

        int serverCountdownTpStage();

        ServerPerTypeTeleportCountdownView serverCountdownTpStage(int value);

        int serverCountdownTpBack();

        ServerPerTypeTeleportCountdownView serverCountdownTpBack(int value);

        int serverCountdownTpGrave();

        ServerPerTypeTeleportCountdownView serverCountdownTpGrave(int value);
    }

    public interface CostView {
        TeleportCostGroupView tpCoordinate();

        TeleportCostGroupView tpStructure();

        TeleportCostGroupView tpAsk();

        TeleportCostGroupView tpHere();

        TeleportCostGroupView tpRandom();

        TeleportCostGroupView tpSpawn();

        TeleportCostGroupView tpWorldSpawn();

        TeleportCostGroupView tpTop();

        TeleportCostGroupView tpBottom();

        TeleportCostGroupView tpUp();

        TeleportCostGroupView tpDown();

        TeleportCostGroupView tpView();

        TeleportCostGroupView tpHome();

        TeleportCostGroupView tpStage();

        TeleportCostGroupView tpBack();

        TeleportCostGroupView tpGrave();
    }

    public interface TeleportCostGroupView {
        EnumCostType type();

        TeleportCostGroupView type(EnumCostType value);

        int num();

        TeleportCostGroupView num(int value);

        String conf();

        TeleportCostGroupView conf(String value);

        double rate();

        TeleportCostGroupView rate(double value);

        int numUpper();

        TeleportCostGroupView numUpper(int value);

        int numLower();

        TeleportCostGroupView numLower(int value);

        String exp();

        TeleportCostGroupView exp(String value);
    }

    // endregion 运行时视图接口

}
