package xin.vanilla.narcissus.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.block.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvents;
import net.minecraftforge.fml.config.ModConfig;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ForgeConfigAdapter;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.enums.EnumCardType;
import xin.vanilla.narcissus.enums.EnumCoolDownType;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 通用配置（COMMON）。配置 GUI 的 {@link ConfigEntry.Gui.Tooltip} 与项目根目录 {@code narcissus_farewell-common.toml}、
 * {@code narcissus_farewell-server.toml}（对应 {@code server.*}）中的注释一致，请勿脱离 TOML 随意改写说明文案。
 * <p>
 * 运行时：{@code CommonConfig.get().base().teleportCard()}、{@code CommonConfig.get().server().general().defaultLanguage()}。
 */
@Getter
@Setter
@Accessors(chain = true, fluent = true)
@Config(name = "narcissus_farewell-common", type = ModConfig.Type.COMMON)
public class CommonConfig implements ConfigData {

    // region 配置结构（COMMON）

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
    @ConfigEntry.Gui.Tooltip(zh_cn = "服务端", en_us = "Server-side")
    private ServerCategory server = new ServerCategory();

    private final ConfigHolder holder;
    private final Base baseApi;
    private final FeatureSwitch featureSwitchApi;
    private final CommandNames commandNamesApi;
    private final Concise conciseApi;
    private final ServerRoot serverApi;

    // endregion 配置结构（COMMON）

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
        @ConfigEntry.Gui.Tooltip(zh_cn = "设置语言的指令。", en_us = "This command is used to set the language.")
        private String commandLanguage = "language";
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
        @ConfigEntry.Gui.Tooltip(zh_cn = "设置虚拟权限的指令。", en_us = "The command to set virtual permission.")
        private String commandVirtualOp = "opv";
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

    public static final class Key {
        private Key() {
        }

        public static final String BASE_TELEPORT_CARD = "base.teleportCard";
        public static final String BASE_TELEPORT_CARD_DAILY = "base.teleportCardDaily";
        public static final String BASE_TELEPORT_CARD_TYPE = "base.teleportCardType";
        public static final String BASE_REMOVE_ORIGINAL_TP = "base.removeOriginalTp";
        public static final String BASE_FLY_SPEED_MIN = "base.flySpeedMin";
        public static final String BASE_FLY_SPEED_MAX = "base.flySpeedMax";
        public static final String FS_SWITCH_SHARE = "featureSwitch.switchShare";
        public static final String FS_SWITCH_FEED = "featureSwitch.switchFeed";
        public static final String FS_SWITCH_TP_COORDINATE = "featureSwitch.switchTpCoordinate";
        public static final String FS_SWITCH_TP_STRUCTURE = "featureSwitch.switchTpStructure";
        public static final String FS_SWITCH_TP_ASK = "featureSwitch.switchTpAsk";
        public static final String FS_SWITCH_TP_HERE = "featureSwitch.switchTpHere";
        public static final String FS_SWITCH_TP_RANDOM = "featureSwitch.switchTpRandom";
        public static final String FS_SWITCH_TP_SPAWN = "featureSwitch.switchTpSpawn";
        public static final String FS_SWITCH_TP_WORLD_SPAWN = "featureSwitch.switchTpWorldSpawn";
        public static final String FS_SWITCH_TP_TOP = "featureSwitch.switchTpTop";
        public static final String FS_SWITCH_TP_BOTTOM = "featureSwitch.switchTpBottom";
        public static final String FS_SWITCH_TP_UP = "featureSwitch.switchTpUp";
        public static final String FS_SWITCH_TP_DOWN = "featureSwitch.switchTpDown";
        public static final String FS_SWITCH_TP_VIEW = "featureSwitch.switchTpView";
        public static final String FS_SWITCH_TP_HOME = "featureSwitch.switchTpHome";
        public static final String FS_SWITCH_TP_STAGE = "featureSwitch.switchTpStage";
        public static final String FS_SWITCH_TP_BACK = "featureSwitch.switchTpBack";
        public static final String FS_SWITCH_TP_GRAVE = "featureSwitch.switchTpGrave";
        public static final String FS_SWITCH_FLY = "featureSwitch.switchFly";
        public static final String CN_COMMAND_PREFIX = "commandNames.commandPrefix";
        public static final String CN_COMMAND_LANGUAGE = "commandNames.commandLanguage";
        public static final String CN_COMMAND_UUID = "commandNames.commandUuid";
        public static final String CN_COMMAND_DIMENSION = "commandNames.commandDimension";
        public static final String CN_COMMAND_CARD = "commandNames.commandCard";
        public static final String CN_COMMAND_SHARE = "commandNames.commandShare";
        public static final String CN_COMMAND_FEED = "commandNames.commandFeed";
        public static final String CN_COMMAND_TP_COORDINATE = "commandNames.commandTpCoordinate";
        public static final String CN_COMMAND_TP_STRUCTURE = "commandNames.commandTpStructure";
        public static final String CN_TP_ASK_COMMAND_TP_ASK = "commandNames.tpAsk.commandTpAsk";
        public static final String CN_TP_ASK_COMMAND_TP_ASK_YES = "commandNames.tpAsk.commandTpAskYes";
        public static final String CN_TP_ASK_COMMAND_TP_ASK_NO = "commandNames.tpAsk.commandTpAskNo";
        public static final String CN_TP_ASK_COMMAND_TP_ASK_CANCEL = "commandNames.tpAsk.commandTpAskCancel";
        public static final String CN_TP_HERE_COMMAND_TP_HERE = "commandNames.tpHere.commandTpHere";
        public static final String CN_TP_HERE_COMMAND_TP_HERE_YES = "commandNames.tpHere.commandTpHereYes";
        public static final String CN_TP_HERE_COMMAND_TP_HERE_NO = "commandNames.tpHere.commandTpHereNo";
        public static final String CN_TP_HERE_COMMAND_TP_HERE_CANCEL = "commandNames.tpHere.commandTpHereCancel";
        public static final String CN_COMMAND_TP_RANDOM = "commandNames.commandTpRandom";
        public static final String CN_COMMAND_TP_SPAWN = "commandNames.commandTpSpawn";
        public static final String CN_COMMAND_TP_WORLD_SPAWN = "commandNames.commandTpWorldSpawn";
        public static final String CN_COMMAND_TP_TOP = "commandNames.commandTpTop";
        public static final String CN_COMMAND_TP_BOTTOM = "commandNames.commandTpBottom";
        public static final String CN_COMMAND_TP_UP = "commandNames.commandTpUp";
        public static final String CN_COMMAND_TP_DOWN = "commandNames.commandTpDown";
        public static final String CN_COMMAND_TP_VIEW = "commandNames.commandTpView";
        public static final String CN_TP_HOME_COMMAND_TP_HOME = "commandNames.tpHome.commandTpHome";
        public static final String CN_TP_HOME_COMMAND_SET_HOME = "commandNames.tpHome.commandSetHome";
        public static final String CN_TP_HOME_COMMAND_DEL_HOME = "commandNames.tpHome.commandDelHome";
        public static final String CN_TP_HOME_COMMAND_GET_HOME = "commandNames.tpHome.commandGetHome";
        public static final String CN_TP_STAGE_COMMAND_TP_STAGE = "commandNames.tpStage.commandTpStage";
        public static final String CN_TP_STAGE_COMMAND_SET_STAGE = "commandNames.tpStage.commandSetStage";
        public static final String CN_TP_STAGE_COMMAND_DEL_STAGE = "commandNames.tpStage.commandDelStage";
        public static final String CN_TP_STAGE_COMMAND_GET_STAGE = "commandNames.tpStage.commandGetStage";
        public static final String CN_COMMAND_TP_BACK = "commandNames.commandTpBack";
        public static final String CN_COMMAND_TP_GRAVE = "commandNames.commandTpGrave";
        public static final String CN_COMMAND_FLY = "commandNames.commandFly";
        public static final String CN_COMMAND_VIRTUAL_OP = "commandNames.commandVirtualOp";
        public static final String CC_CONCISE_LANGUAGE = "conciseCommands.conciseLanguage";
        public static final String CC_CONCISE_UUID = "conciseCommands.conciseUuid";
        public static final String CC_CONCISE_DIMENSION = "conciseCommands.conciseDimension";
        public static final String CC_CONCISE_CARD = "conciseCommands.conciseCard";
        public static final String CC_CONCISE_SHARE = "conciseCommands.conciseShare";
        public static final String CC_CONCISE_FEED = "conciseCommands.conciseFeed";
        public static final String CC_CONCISE_TP_COORDINATE = "conciseCommands.conciseTpCoordinate";
        public static final String CC_CONCISE_TP_STRUCTURE = "conciseCommands.conciseTpStructure";
        public static final String CC_TP_ASK_CONCISE_TP_ASK = "conciseCommands.tpAsk.conciseTpAsk";
        public static final String CC_TP_ASK_CONCISE_TP_ASK_YES = "conciseCommands.tpAsk.conciseTpAskYes";
        public static final String CC_TP_ASK_CONCISE_TP_ASK_NO = "conciseCommands.tpAsk.conciseTpAskNo";
        public static final String CC_TP_ASK_CONCISE_TP_ASK_CANCEL = "conciseCommands.tpAsk.conciseTpAskCancel";
        public static final String CC_TP_HERE_CONCISE_TP_HERE = "conciseCommands.tpHere.conciseTpHere";
        public static final String CC_TP_HERE_CONCISE_TP_HERE_YES = "conciseCommands.tpHere.conciseTpHereYes";
        public static final String CC_TP_HERE_CONCISE_TP_HERE_NO = "conciseCommands.tpHere.conciseTpHereNo";
        public static final String CC_TP_HERE_CONCISE_TP_HERE_CANCEL = "conciseCommands.tpHere.conciseTpHereCancel";
        public static final String CC_CONCISE_TP_RANDOM = "conciseCommands.conciseTpRandom";
        public static final String CC_CONCISE_TP_SPAWN = "conciseCommands.conciseTpSpawn";
        public static final String CC_CONCISE_TP_WORLD_SPAWN = "conciseCommands.conciseTpWorldSpawn";
        public static final String CC_CONCISE_TP_TOP = "conciseCommands.conciseTpTop";
        public static final String CC_CONCISE_TP_BOTTOM = "conciseCommands.conciseTpBottom";
        public static final String CC_CONCISE_TP_UP = "conciseCommands.conciseTpUp";
        public static final String CC_CONCISE_TP_DOWN = "conciseCommands.conciseTpDown";
        public static final String CC_CONCISE_TP_VIEW = "conciseCommands.conciseTpView";
        public static final String CC_TP_HOME_CONCISE_TP_HOME = "conciseCommands.tpHome.conciseTpHome";
        public static final String CC_TP_HOME_CONCISE_SET_HOME = "conciseCommands.tpHome.conciseSetHome";
        public static final String CC_TP_HOME_CONCISE_DEL_HOME = "conciseCommands.tpHome.conciseDelHome";
        public static final String CC_TP_HOME_CONCISE_GET_HOME = "conciseCommands.tpHome.conciseGetHome";
        public static final String CC_TP_STAGE_CONCISE_TP_STAGE = "conciseCommands.tpStage.conciseTpStage";
        public static final String CC_TP_STAGE_CONCISE_SET_STAGE = "conciseCommands.tpStage.conciseSetStage";
        public static final String CC_TP_STAGE_CONCISE_DEL_STAGE = "conciseCommands.tpStage.conciseDelStage";
        public static final String CC_TP_STAGE_CONCISE_GET_STAGE = "conciseCommands.tpStage.conciseGetStage";
        public static final String CC_CONCISE_TP_BACK = "conciseCommands.conciseTpBack";
        public static final String CC_CONCISE_TP_GRAVE = "conciseCommands.conciseTpGrave";
        public static final String CC_CONCISE_FLY = "conciseCommands.conciseFly";
        public static final String CC_CONCISE_VIRTUAL_OP = "conciseCommands.conciseVirtualOp";
    }

    public static final class Base {
        private final ConfigHolder holder;

        Base(ConfigHolder holder) {
            this.holder = holder;
        }

        public boolean teleportCard() {
            if (holder == null) return false;
            Boolean v = holder.get(Key.BASE_TELEPORT_CARD);
            return v != null && v;
        }

        public Base teleportCard(boolean value) {
            if (holder != null) holder.set(Key.BASE_TELEPORT_CARD, value);
            return this;
        }

        public int teleportCardDaily() {
            if (holder == null) return 0;
            Integer v = holder.get(Key.BASE_TELEPORT_CARD_DAILY);
            return v != null ? v : 0;
        }

        public EnumCardType teleportCardType() {
            if (holder == null) return EnumCardType.REFUND_ALL_COST;
            EnumCardType v = holder.get(Key.BASE_TELEPORT_CARD_TYPE);
            return v != null ? v : EnumCardType.REFUND_ALL_COST;
        }

        public boolean removeOriginalTp() {
            if (holder == null) return false;
            Boolean v = holder.get(Key.BASE_REMOVE_ORIGINAL_TP);
            return v != null && v;
        }

        public double flySpeedMin() {
            if (holder == null) return -5d;
            Double v = holder.get(Key.BASE_FLY_SPEED_MIN);
            return v != null ? v : -5d;
        }

        public double flySpeedMax() {
            if (holder == null) return 5d;
            Double v = holder.get(Key.BASE_FLY_SPEED_MAX);
            return v != null ? v : 5d;
        }
    }

    public static final class FeatureSwitch {
        private final ConfigHolder holder;

        FeatureSwitch(ConfigHolder holder) {
            this.holder = holder;
        }

        private boolean gb(String k, boolean def) {
            if (holder == null) return def;
            Boolean v = holder.get(k);
            return v != null ? v : def;
        }

        private FeatureSwitch sb(String k, boolean v) {
            if (holder != null) holder.set(k, v);
            return this;
        }

        public boolean switchShare() {
            return gb(Key.FS_SWITCH_SHARE, true);
        }

        public FeatureSwitch switchShare(boolean v) {
            return sb(Key.FS_SWITCH_SHARE, v);
        }

        public boolean switchFeed() {
            return gb(Key.FS_SWITCH_FEED, true);
        }

        public FeatureSwitch switchFeed(boolean v) {
            return sb(Key.FS_SWITCH_FEED, v);
        }

        public boolean switchTpCoordinate() {
            return gb(Key.FS_SWITCH_TP_COORDINATE, true);
        }

        public FeatureSwitch switchTpCoordinate(boolean v) {
            return sb(Key.FS_SWITCH_TP_COORDINATE, v);
        }

        public boolean switchTpStructure() {
            return gb(Key.FS_SWITCH_TP_STRUCTURE, true);
        }

        public FeatureSwitch switchTpStructure(boolean v) {
            return sb(Key.FS_SWITCH_TP_STRUCTURE, v);
        }

        public boolean switchTpAsk() {
            return gb(Key.FS_SWITCH_TP_ASK, true);
        }

        public FeatureSwitch switchTpAsk(boolean v) {
            return sb(Key.FS_SWITCH_TP_ASK, v);
        }

        public boolean switchTpHere() {
            return gb(Key.FS_SWITCH_TP_HERE, true);
        }

        public FeatureSwitch switchTpHere(boolean v) {
            return sb(Key.FS_SWITCH_TP_HERE, v);
        }

        public boolean switchTpRandom() {
            return gb(Key.FS_SWITCH_TP_RANDOM, true);
        }

        public FeatureSwitch switchTpRandom(boolean v) {
            return sb(Key.FS_SWITCH_TP_RANDOM, v);
        }

        public boolean switchTpSpawn() {
            return gb(Key.FS_SWITCH_TP_SPAWN, true);
        }

        public FeatureSwitch switchTpSpawn(boolean v) {
            return sb(Key.FS_SWITCH_TP_SPAWN, v);
        }

        public boolean switchTpWorldSpawn() {
            return gb(Key.FS_SWITCH_TP_WORLD_SPAWN, true);
        }

        public FeatureSwitch switchTpWorldSpawn(boolean v) {
            return sb(Key.FS_SWITCH_TP_WORLD_SPAWN, v);
        }

        public boolean switchTpTop() {
            return gb(Key.FS_SWITCH_TP_TOP, true);
        }

        public FeatureSwitch switchTpTop(boolean v) {
            return sb(Key.FS_SWITCH_TP_TOP, v);
        }

        public boolean switchTpBottom() {
            return gb(Key.FS_SWITCH_TP_BOTTOM, true);
        }

        public FeatureSwitch switchTpBottom(boolean v) {
            return sb(Key.FS_SWITCH_TP_BOTTOM, v);
        }

        public boolean switchTpUp() {
            return gb(Key.FS_SWITCH_TP_UP, true);
        }

        public FeatureSwitch switchTpUp(boolean v) {
            return sb(Key.FS_SWITCH_TP_UP, v);
        }

        public boolean switchTpDown() {
            return gb(Key.FS_SWITCH_TP_DOWN, true);
        }

        public FeatureSwitch switchTpDown(boolean v) {
            return sb(Key.FS_SWITCH_TP_DOWN, v);
        }

        public boolean switchTpView() {
            return gb(Key.FS_SWITCH_TP_VIEW, true);
        }

        public FeatureSwitch switchTpView(boolean v) {
            return sb(Key.FS_SWITCH_TP_VIEW, v);
        }

        public boolean switchTpHome() {
            return gb(Key.FS_SWITCH_TP_HOME, true);
        }

        public FeatureSwitch switchTpHome(boolean v) {
            return sb(Key.FS_SWITCH_TP_HOME, v);
        }

        public boolean switchTpStage() {
            return gb(Key.FS_SWITCH_TP_STAGE, true);
        }

        public FeatureSwitch switchTpStage(boolean v) {
            return sb(Key.FS_SWITCH_TP_STAGE, v);
        }

        public boolean switchTpBack() {
            return gb(Key.FS_SWITCH_TP_BACK, true);
        }

        public FeatureSwitch switchTpBack(boolean v) {
            return sb(Key.FS_SWITCH_TP_BACK, v);
        }

        public boolean switchTpGrave() {
            return gb(Key.FS_SWITCH_TP_GRAVE, true);
        }

        public FeatureSwitch switchTpGrave(boolean v) {
            return sb(Key.FS_SWITCH_TP_GRAVE, v);
        }

        public boolean switchFly() {
            return gb(Key.FS_SWITCH_FLY, true);
        }

        public FeatureSwitch switchFly(boolean v) {
            return sb(Key.FS_SWITCH_FLY, v);
        }
    }

    public static final class CommandNames {
        private final ConfigHolder holder;

        CommandNames(ConfigHolder holder) {
            this.holder = holder;
        }

        private String gs(String k, String def) {
            if (holder == null) return def;
            String v = holder.get(k);
            return v != null && !v.isEmpty() ? v : def;
        }

        private CommandNames ss(String k, String v) {
            if (holder != null) holder.set(k, v);
            return this;
        }

        public String commandPrefix() {
            return gs(Key.CN_COMMAND_PREFIX, NarcissusFarewell.DEFAULT_COMMAND_PREFIX);
        }

        public CommandNames commandPrefix(String v) {
            return ss(Key.CN_COMMAND_PREFIX, v);
        }

        public String commandLanguage() {
            return gs(Key.CN_COMMAND_LANGUAGE, "language");
        }

        public CommandNames commandLanguage(String v) {
            return ss(Key.CN_COMMAND_LANGUAGE, v);
        }

        public String commandUuid() {
            return gs(Key.CN_COMMAND_UUID, "uuid");
        }

        public CommandNames commandUuid(String v) {
            return ss(Key.CN_COMMAND_UUID, v);
        }

        public String commandDimension() {
            return gs(Key.CN_COMMAND_DIMENSION, "dim");
        }

        public CommandNames commandDimension(String v) {
            return ss(Key.CN_COMMAND_DIMENSION, v);
        }

        public String commandCard() {
            return gs(Key.CN_COMMAND_CARD, "card");
        }

        public CommandNames commandCard(String v) {
            return ss(Key.CN_COMMAND_CARD, v);
        }

        public String commandShare() {
            return gs(Key.CN_COMMAND_SHARE, "share");
        }

        public CommandNames commandShare(String v) {
            return ss(Key.CN_COMMAND_SHARE, v);
        }

        public String commandFeed() {
            return gs(Key.CN_COMMAND_FEED, "feed");
        }

        public CommandNames commandFeed(String v) {
            return ss(Key.CN_COMMAND_FEED, v);
        }

        public String commandTpCoordinate() {
            return gs(Key.CN_COMMAND_TP_COORDINATE, "tpx");
        }

        public CommandNames commandTpCoordinate(String v) {
            return ss(Key.CN_COMMAND_TP_COORDINATE, v);
        }

        public String commandTpStructure() {
            return gs(Key.CN_COMMAND_TP_STRUCTURE, "tpst");
        }

        public CommandNames commandTpStructure(String v) {
            return ss(Key.CN_COMMAND_TP_STRUCTURE, v);
        }

        public String commandTpAsk() {
            return gs(Key.CN_TP_ASK_COMMAND_TP_ASK, "tpa");
        }

        public CommandNames commandTpAsk(String v) {
            return ss(Key.CN_TP_ASK_COMMAND_TP_ASK, v);
        }

        public String commandTpAskYes() {
            return gs(Key.CN_TP_ASK_COMMAND_TP_ASK_YES, "tpay");
        }

        public CommandNames commandTpAskYes(String v) {
            return ss(Key.CN_TP_ASK_COMMAND_TP_ASK_YES, v);
        }

        public String commandTpAskNo() {
            return gs(Key.CN_TP_ASK_COMMAND_TP_ASK_NO, "tpan");
        }

        public CommandNames commandTpAskNo(String v) {
            return ss(Key.CN_TP_ASK_COMMAND_TP_ASK_NO, v);
        }

        public String commandTpAskCancel() {
            return gs(Key.CN_TP_ASK_COMMAND_TP_ASK_CANCEL, "tpac");
        }

        public CommandNames commandTpAskCancel(String v) {
            return ss(Key.CN_TP_ASK_COMMAND_TP_ASK_CANCEL, v);
        }

        public String commandTpHere() {
            return gs(Key.CN_TP_HERE_COMMAND_TP_HERE, "tph");
        }

        public CommandNames commandTpHere(String v) {
            return ss(Key.CN_TP_HERE_COMMAND_TP_HERE, v);
        }

        public String commandTpHereYes() {
            return gs(Key.CN_TP_HERE_COMMAND_TP_HERE_YES, "tphy");
        }

        public CommandNames commandTpHereYes(String v) {
            return ss(Key.CN_TP_HERE_COMMAND_TP_HERE_YES, v);
        }

        public String commandTpHereNo() {
            return gs(Key.CN_TP_HERE_COMMAND_TP_HERE_NO, "tphn");
        }

        public CommandNames commandTpHereNo(String v) {
            return ss(Key.CN_TP_HERE_COMMAND_TP_HERE_NO, v);
        }

        public String commandTpHereCancel() {
            return gs(Key.CN_TP_HERE_COMMAND_TP_HERE_CANCEL, "tphc");
        }

        public CommandNames commandTpHereCancel(String v) {
            return ss(Key.CN_TP_HERE_COMMAND_TP_HERE_CANCEL, v);
        }

        public String commandTpRandom() {
            return gs(Key.CN_COMMAND_TP_RANDOM, "tpr");
        }

        public CommandNames commandTpRandom(String v) {
            return ss(Key.CN_COMMAND_TP_RANDOM, v);
        }

        public String commandTpSpawn() {
            return gs(Key.CN_COMMAND_TP_SPAWN, "tpsp");
        }

        public CommandNames commandTpSpawn(String v) {
            return ss(Key.CN_COMMAND_TP_SPAWN, v);
        }

        public String commandTpWorldSpawn() {
            return gs(Key.CN_COMMAND_TP_WORLD_SPAWN, "tpws");
        }

        public CommandNames commandTpWorldSpawn(String v) {
            return ss(Key.CN_COMMAND_TP_WORLD_SPAWN, v);
        }

        public String commandTpTop() {
            return gs(Key.CN_COMMAND_TP_TOP, "tpt");
        }

        public CommandNames commandTpTop(String v) {
            return ss(Key.CN_COMMAND_TP_TOP, v);
        }

        public String commandTpBottom() {
            return gs(Key.CN_COMMAND_TP_BOTTOM, "tpb");
        }

        public CommandNames commandTpBottom(String v) {
            return ss(Key.CN_COMMAND_TP_BOTTOM, v);
        }

        public String commandTpUp() {
            return gs(Key.CN_COMMAND_TP_UP, "tpu");
        }

        public CommandNames commandTpUp(String v) {
            return ss(Key.CN_COMMAND_TP_UP, v);
        }

        public String commandTpDown() {
            return gs(Key.CN_COMMAND_TP_DOWN, "tpd");
        }

        public CommandNames commandTpDown(String v) {
            return ss(Key.CN_COMMAND_TP_DOWN, v);
        }

        public String commandTpView() {
            return gs(Key.CN_COMMAND_TP_VIEW, "tpv");
        }

        public CommandNames commandTpView(String v) {
            return ss(Key.CN_COMMAND_TP_VIEW, v);
        }

        public String commandTpHome() {
            return gs(Key.CN_TP_HOME_COMMAND_TP_HOME, "home");
        }

        public CommandNames commandTpHome(String v) {
            return ss(Key.CN_TP_HOME_COMMAND_TP_HOME, v);
        }

        public String commandSetHome() {
            return gs(Key.CN_TP_HOME_COMMAND_SET_HOME, "sethome");
        }

        public CommandNames commandSetHome(String v) {
            return ss(Key.CN_TP_HOME_COMMAND_SET_HOME, v);
        }

        public String commandDelHome() {
            return gs(Key.CN_TP_HOME_COMMAND_DEL_HOME, "delhome");
        }

        public CommandNames commandDelHome(String v) {
            return ss(Key.CN_TP_HOME_COMMAND_DEL_HOME, v);
        }

        public String commandGetHome() {
            return gs(Key.CN_TP_HOME_COMMAND_GET_HOME, "gethome");
        }

        public CommandNames commandGetHome(String v) {
            return ss(Key.CN_TP_HOME_COMMAND_GET_HOME, v);
        }

        public String commandTpStage() {
            return gs(Key.CN_TP_STAGE_COMMAND_TP_STAGE, "stage");
        }

        public CommandNames commandTpStage(String v) {
            return ss(Key.CN_TP_STAGE_COMMAND_TP_STAGE, v);
        }

        public String commandSetStage() {
            return gs(Key.CN_TP_STAGE_COMMAND_SET_STAGE, "setstage");
        }

        public CommandNames commandSetStage(String v) {
            return ss(Key.CN_TP_STAGE_COMMAND_SET_STAGE, v);
        }

        public String commandDelStage() {
            return gs(Key.CN_TP_STAGE_COMMAND_DEL_STAGE, "delstage");
        }

        public CommandNames commandDelStage(String v) {
            return ss(Key.CN_TP_STAGE_COMMAND_DEL_STAGE, v);
        }

        public String commandGetStage() {
            return gs(Key.CN_TP_STAGE_COMMAND_GET_STAGE, "getstage");
        }

        public CommandNames commandGetStage(String v) {
            return ss(Key.CN_TP_STAGE_COMMAND_GET_STAGE, v);
        }

        public String commandTpBack() {
            return gs(Key.CN_COMMAND_TP_BACK, "back");
        }

        public CommandNames commandTpBack(String v) {
            return ss(Key.CN_COMMAND_TP_BACK, v);
        }

        public String commandTpGrave() {
            return gs(Key.CN_COMMAND_TP_GRAVE, "grave");
        }

        public CommandNames commandTpGrave(String v) {
            return ss(Key.CN_COMMAND_TP_GRAVE, v);
        }

        public String commandFly() {
            return gs(Key.CN_COMMAND_FLY, "fly");
        }

        public CommandNames commandFly(String v) {
            return ss(Key.CN_COMMAND_FLY, v);
        }

        public String commandVirtualOp() {
            return gs(Key.CN_COMMAND_VIRTUAL_OP, "opv");
        }

        public CommandNames commandVirtualOp(String v) {
            return ss(Key.CN_COMMAND_VIRTUAL_OP, v);
        }
    }

    public static final class Concise {
        private final ConfigHolder holder;

        Concise(ConfigHolder holder) {
            this.holder = holder;
        }

        private boolean gb(String k, boolean def) {
            if (holder == null) return def;
            Boolean v = holder.get(k);
            return v != null ? v : def;
        }

        private Concise sb(String k, boolean v) {
            if (holder != null) holder.set(k, v);
            return this;
        }

        public boolean conciseLanguage() {
            return gb(Key.CC_CONCISE_LANGUAGE, false);
        }

        public boolean conciseUuid() {
            return gb(Key.CC_CONCISE_UUID, false);
        }

        public boolean conciseDimension() {
            return gb(Key.CC_CONCISE_DIMENSION, false);
        }

        public boolean conciseCard() {
            return gb(Key.CC_CONCISE_CARD, false);
        }

        public boolean conciseShare() {
            return gb(Key.CC_CONCISE_SHARE, false);
        }

        public boolean conciseFeed() {
            return gb(Key.CC_CONCISE_FEED, false);
        }

        public boolean conciseTpCoordinate() {
            return gb(Key.CC_CONCISE_TP_COORDINATE, true);
        }

        public boolean conciseTpStructure() {
            return gb(Key.CC_CONCISE_TP_STRUCTURE, true);
        }

        public boolean conciseTpAsk() {
            return gb(Key.CC_TP_ASK_CONCISE_TP_ASK, true);
        }

        public Concise conciseTpAsk(boolean v) {
            return sb(Key.CC_TP_ASK_CONCISE_TP_ASK, v);
        }

        public boolean conciseTpAskYes() {
            return gb(Key.CC_TP_ASK_CONCISE_TP_ASK_YES, true);
        }

        public boolean conciseTpAskNo() {
            return gb(Key.CC_TP_ASK_CONCISE_TP_ASK_NO, true);
        }

        public boolean conciseTpAskCancel() {
            return gb(Key.CC_TP_ASK_CONCISE_TP_ASK_CANCEL, true);
        }

        public Concise conciseTpAskCancel(boolean v) {
            return sb(Key.CC_TP_ASK_CONCISE_TP_ASK_CANCEL, v);
        }

        public boolean conciseTpHere() {
            return gb(Key.CC_TP_HERE_CONCISE_TP_HERE, true);
        }

        public boolean conciseTpHereYes() {
            return gb(Key.CC_TP_HERE_CONCISE_TP_HERE_YES, true);
        }

        public boolean conciseTpHereNo() {
            return gb(Key.CC_TP_HERE_CONCISE_TP_HERE_NO, true);
        }

        public boolean conciseTpHereCancel() {
            return gb(Key.CC_TP_HERE_CONCISE_TP_HERE_CANCEL, true);
        }

        public Concise conciseTpHereCancel(boolean v) {
            return sb(Key.CC_TP_HERE_CONCISE_TP_HERE_CANCEL, v);
        }

        public boolean conciseTpRandom() {
            return gb(Key.CC_CONCISE_TP_RANDOM, false);
        }

        public boolean conciseTpSpawn() {
            return gb(Key.CC_CONCISE_TP_SPAWN, true);
        }

        public boolean conciseTpWorldSpawn() {
            return gb(Key.CC_CONCISE_TP_WORLD_SPAWN, false);
        }

        public boolean conciseTpTop() {
            return gb(Key.CC_CONCISE_TP_TOP, false);
        }

        public boolean conciseTpBottom() {
            return gb(Key.CC_CONCISE_TP_BOTTOM, false);
        }

        public boolean conciseTpUp() {
            return gb(Key.CC_CONCISE_TP_UP, false);
        }

        public boolean conciseTpDown() {
            return gb(Key.CC_CONCISE_TP_DOWN, false);
        }

        public boolean conciseTpView() {
            return gb(Key.CC_CONCISE_TP_VIEW, false);
        }

        public boolean conciseTpHome() {
            return gb(Key.CC_TP_HOME_CONCISE_TP_HOME, true);
        }

        public boolean conciseSetHome() {
            return gb(Key.CC_TP_HOME_CONCISE_SET_HOME, true);
        }

        public boolean conciseDelHome() {
            return gb(Key.CC_TP_HOME_CONCISE_DEL_HOME, true);
        }

        public boolean conciseGetHome() {
            return gb(Key.CC_TP_HOME_CONCISE_GET_HOME, true);
        }

        public boolean conciseTpStage() {
            return gb(Key.CC_TP_STAGE_CONCISE_TP_STAGE, true);
        }

        public boolean conciseSetStage() {
            return gb(Key.CC_TP_STAGE_CONCISE_SET_STAGE, true);
        }

        public boolean conciseDelStage() {
            return gb(Key.CC_TP_STAGE_CONCISE_DEL_STAGE, true);
        }

        public boolean conciseGetStage() {
            return gb(Key.CC_TP_STAGE_CONCISE_GET_STAGE, true);
        }

        public boolean conciseTpBack() {
            return gb(Key.CC_CONCISE_TP_BACK, true);
        }

        public boolean conciseTpGrave() {
            return gb(Key.CC_CONCISE_TP_GRAVE, true);
        }

        public boolean conciseFly() {
            return gb(Key.CC_CONCISE_FLY, true);
        }

        public boolean conciseVirtualOp() {
            return gb(Key.CC_CONCISE_VIRTUAL_OP, false);
        }
    }

    private CommonConfig() {
        this(null);
    }

    CommonConfig(ConfigHolder holder) {
        this.holder = holder;
        this.baseApi = new Base(holder);
        this.featureSwitchApi = new FeatureSwitch(holder);
        this.commandNamesApi = new CommandNames(holder);
        this.conciseApi = new Concise(holder);
        this.serverApi = new ServerRoot(holder);
    }

    public static CommonConfig get() {
        return new CommonConfig(ForgeConfigAdapter.getHolder(CommonConfig.class));
    }

    public Base base() {
        return baseApi;
    }

    public FeatureSwitch featureSwitch() {
        return featureSwitchApi;
    }

    public CommandNames commandNames() {
        return commandNamesApi;
    }

    public Concise concise() {
        return conciseApi;
    }

    public ServerRoot server() {
        return serverApi;
    }

    public ConfigHolder holder() {
        return holder;
    }

    public void save() {
        if (holder != null) {
            holder.save();
        }
    }

    public static void resetConfig() {
        CommonConfig c = get();
        if (c.holder == null) return;
        ConfigHolder h = c.holder;
        h.set(Key.BASE_TELEPORT_CARD, false);
        h.set(Key.BASE_TELEPORT_CARD_DAILY, 0);
        h.set(Key.BASE_TELEPORT_CARD_TYPE, EnumCardType.REFUND_ALL_COST);
        h.set(Key.BASE_REMOVE_ORIGINAL_TP, false);
        h.set(Key.BASE_FLY_SPEED_MIN, -5d);
        h.set(Key.BASE_FLY_SPEED_MAX, 5d);
        h.set(Key.FS_SWITCH_SHARE, true);
        h.set(Key.FS_SWITCH_FEED, true);
        h.set(Key.FS_SWITCH_TP_COORDINATE, true);
        h.set(Key.FS_SWITCH_TP_STRUCTURE, true);
        h.set(Key.FS_SWITCH_TP_ASK, true);
        h.set(Key.FS_SWITCH_TP_HERE, true);
        h.set(Key.FS_SWITCH_TP_RANDOM, true);
        h.set(Key.FS_SWITCH_TP_SPAWN, true);
        h.set(Key.FS_SWITCH_TP_WORLD_SPAWN, true);
        h.set(Key.FS_SWITCH_TP_TOP, true);
        h.set(Key.FS_SWITCH_TP_BOTTOM, true);
        h.set(Key.FS_SWITCH_TP_UP, true);
        h.set(Key.FS_SWITCH_TP_DOWN, true);
        h.set(Key.FS_SWITCH_TP_VIEW, true);
        h.set(Key.FS_SWITCH_TP_HOME, true);
        h.set(Key.FS_SWITCH_TP_STAGE, true);
        h.set(Key.FS_SWITCH_TP_BACK, true);
        h.set(Key.FS_SWITCH_TP_GRAVE, true);
        h.set(Key.FS_SWITCH_FLY, true);
        h.set(Key.CN_COMMAND_PREFIX, NarcissusFarewell.DEFAULT_COMMAND_PREFIX);
        h.set(Key.CN_COMMAND_LANGUAGE, "language");
        h.set(Key.CN_COMMAND_UUID, "uuid");
        h.set(Key.CN_COMMAND_DIMENSION, "dim");
        h.set(Key.CN_COMMAND_CARD, "card");
        h.set(Key.CN_COMMAND_SHARE, "share");
        h.set(Key.CN_COMMAND_FEED, "feed");
        h.set(Key.CN_COMMAND_TP_COORDINATE, "tpx");
        h.set(Key.CN_COMMAND_TP_STRUCTURE, "tpst");
        h.set(Key.CN_TP_ASK_COMMAND_TP_ASK, "tpa");
        h.set(Key.CN_TP_ASK_COMMAND_TP_ASK_YES, "tpay");
        h.set(Key.CN_TP_ASK_COMMAND_TP_ASK_NO, "tpan");
        h.set(Key.CN_TP_ASK_COMMAND_TP_ASK_CANCEL, "tpac");
        h.set(Key.CN_TP_HERE_COMMAND_TP_HERE, "tph");
        h.set(Key.CN_TP_HERE_COMMAND_TP_HERE_YES, "tphy");
        h.set(Key.CN_TP_HERE_COMMAND_TP_HERE_NO, "tphn");
        h.set(Key.CN_TP_HERE_COMMAND_TP_HERE_CANCEL, "tphc");
        h.set(Key.CN_COMMAND_TP_RANDOM, "tpr");
        h.set(Key.CN_COMMAND_TP_SPAWN, "tpsp");
        h.set(Key.CN_COMMAND_TP_WORLD_SPAWN, "tpws");
        h.set(Key.CN_COMMAND_TP_TOP, "tpt");
        h.set(Key.CN_COMMAND_TP_BOTTOM, "tpb");
        h.set(Key.CN_COMMAND_TP_UP, "tpu");
        h.set(Key.CN_COMMAND_TP_DOWN, "tpd");
        h.set(Key.CN_COMMAND_TP_VIEW, "tpv");
        h.set(Key.CN_TP_HOME_COMMAND_TP_HOME, "home");
        h.set(Key.CN_TP_HOME_COMMAND_SET_HOME, "sethome");
        h.set(Key.CN_TP_HOME_COMMAND_DEL_HOME, "delhome");
        h.set(Key.CN_TP_HOME_COMMAND_GET_HOME, "gethome");
        h.set(Key.CN_TP_STAGE_COMMAND_TP_STAGE, "stage");
        h.set(Key.CN_TP_STAGE_COMMAND_SET_STAGE, "setstage");
        h.set(Key.CN_TP_STAGE_COMMAND_DEL_STAGE, "delstage");
        h.set(Key.CN_TP_STAGE_COMMAND_GET_STAGE, "getstage");
        h.set(Key.CN_COMMAND_TP_BACK, "back");
        h.set(Key.CN_COMMAND_TP_GRAVE, "grave");
        h.set(Key.CN_COMMAND_FLY, "fly");
        h.set(Key.CN_COMMAND_VIRTUAL_OP, "opv");
        h.set(Key.CC_CONCISE_LANGUAGE, false);
        h.set(Key.CC_CONCISE_UUID, false);
        h.set(Key.CC_CONCISE_DIMENSION, false);
        h.set(Key.CC_CONCISE_CARD, false);
        h.set(Key.CC_CONCISE_SHARE, false);
        h.set(Key.CC_CONCISE_FEED, false);
        h.set(Key.CC_CONCISE_TP_COORDINATE, true);
        h.set(Key.CC_CONCISE_TP_STRUCTURE, true);
        h.set(Key.CC_TP_ASK_CONCISE_TP_ASK, true);
        h.set(Key.CC_TP_ASK_CONCISE_TP_ASK_YES, true);
        h.set(Key.CC_TP_ASK_CONCISE_TP_ASK_NO, true);
        h.set(Key.CC_TP_ASK_CONCISE_TP_ASK_CANCEL, true);
        h.set(Key.CC_TP_HERE_CONCISE_TP_HERE, true);
        h.set(Key.CC_TP_HERE_CONCISE_TP_HERE_YES, true);
        h.set(Key.CC_TP_HERE_CONCISE_TP_HERE_NO, true);
        h.set(Key.CC_TP_HERE_CONCISE_TP_HERE_CANCEL, true);
        h.set(Key.CC_CONCISE_TP_RANDOM, false);
        h.set(Key.CC_CONCISE_TP_SPAWN, true);
        h.set(Key.CC_CONCISE_TP_WORLD_SPAWN, false);
        h.set(Key.CC_CONCISE_TP_TOP, false);
        h.set(Key.CC_CONCISE_TP_BOTTOM, false);
        h.set(Key.CC_CONCISE_TP_UP, false);
        h.set(Key.CC_CONCISE_TP_DOWN, false);
        h.set(Key.CC_CONCISE_TP_VIEW, false);
        h.set(Key.CC_TP_HOME_CONCISE_TP_HOME, true);
        h.set(Key.CC_TP_HOME_CONCISE_SET_HOME, true);
        h.set(Key.CC_TP_HOME_CONCISE_DEL_HOME, true);
        h.set(Key.CC_TP_HOME_CONCISE_GET_HOME, true);
        h.set(Key.CC_TP_STAGE_CONCISE_TP_STAGE, true);
        h.set(Key.CC_TP_STAGE_CONCISE_SET_STAGE, true);
        h.set(Key.CC_TP_STAGE_CONCISE_DEL_STAGE, true);
        h.set(Key.CC_TP_STAGE_CONCISE_GET_STAGE, true);
        h.set(Key.CC_CONCISE_TP_BACK, true);
        h.set(Key.CC_CONCISE_TP_GRAVE, true);
        h.set(Key.CC_CONCISE_FLY, true);
        h.set(Key.CC_CONCISE_VIRTUAL_OP, false);
        h.save();
    }

    public static void resetConfigWithMode1() {
        resetConfig();
        CommonConfig c = get();
        if (c.holder == null) return;
        ConfigHolder h = c.holder;
        h.set(Key.CN_TP_HOME_COMMAND_TP_HOME, "home");
        h.set(Key.CN_TP_HOME_COMMAND_SET_HOME, "home_set");
        h.set(Key.CN_TP_HOME_COMMAND_DEL_HOME, "home_del");
        h.set(Key.CN_TP_HOME_COMMAND_GET_HOME, "home_get");
        h.set(Key.CN_TP_STAGE_COMMAND_TP_STAGE, "warp");
        h.set(Key.CN_TP_STAGE_COMMAND_SET_STAGE, "warp_set");
        h.set(Key.CN_TP_STAGE_COMMAND_DEL_STAGE, "warp_del");
        h.set(Key.CN_TP_STAGE_COMMAND_GET_STAGE, "warp_get");
        h.set(Key.CN_COMMAND_TP_TOP, "top");
        h.set(Key.CN_COMMAND_TP_UP, "up");
        h.set(Key.CN_COMMAND_TP_DOWN, "down");
        h.set(Key.CN_COMMAND_TP_BOTTOM, "bottom");
        h.save();
    }

    public static void resetConfigWithMode2() {
        resetConfigWithMode1();
        CommonConfig c = get();
        if (c.holder == null) return;
        ConfigHolder h = c.holder;
        h.set(Key.FS_SWITCH_FEED, false);
        h.set(Key.FS_SWITCH_TP_STRUCTURE, false);
        h.set(Key.FS_SWITCH_TP_RANDOM, false);
        h.set(Key.FS_SWITCH_TP_SPAWN, false);
        h.set(Key.FS_SWITCH_TP_WORLD_SPAWN, false);
        h.set(Key.FS_SWITCH_TP_BOTTOM, false);
        h.set(Key.FS_SWITCH_TP_DOWN, false);
        h.set(Key.FS_SWITCH_TP_UP, false);
        h.set(Key.FS_SWITCH_TP_VIEW, false);
        h.save();
    }

    public static void resetConfigWithMode3() {
        resetConfig();
        CommonConfig c = get();
        if (c.holder == null) return;
        ConfigHolder h = c.holder;
        h.set(Key.CC_TP_ASK_CONCISE_TP_ASK_CANCEL, false);
        h.set(Key.CC_TP_HERE_CONCISE_TP_HERE_CANCEL, false);
        h.set(Key.CC_CONCISE_TP_RANDOM, false);
        h.set(Key.CC_CONCISE_TP_SPAWN, false);
        h.set(Key.CC_CONCISE_TP_WORLD_SPAWN, false);
        h.set(Key.CC_CONCISE_TP_TOP, false);
        h.set(Key.CC_CONCISE_TP_UP, false);
        h.set(Key.CC_CONCISE_TP_BOTTOM, false);
        h.set(Key.CC_CONCISE_TP_DOWN, false);
        h.set(Key.CC_CONCISE_TP_VIEW, false);
        h.save();
    }

    // region 服务端配置

    public static class Server {

        @Getter
        @Setter
        @Accessors(chain = true, fluent = true)
        public static class ServerGeneralCategory {
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
            @ConfigEntry.Gui.Tooltip(zh_cn = "坟墓传送时搜索死亡点/坟墓位置的范围上限。", en_us = "The search range limit for grave teleport, in blocks (chunk-related logic uses this as radius cap).")
            @ConfigEntry.BoundedDiscrete(min = 1, max = 256)
            private int graveSearchRangeLimit = 32;
            @ConfigEntry.Gui.Tooltip(zh_cn = "玩家可设置的家的数量。", en_us = "The maximum number of homes that can be set by the player.")
            @ConfigEntry.BoundedDiscrete(min = 1, max = 9999)
            private int teleportHomeLimit = 5;
            @ConfigEntry.Gui.Tooltip(zh_cn = "帮助指令信息头部内容。", en_us = "The header content of the help command.")
            private String helpHeader = "-----==== Narcissus Farewell Help (%d/%d) ====-----";
            @ConfigEntry.Gui.Tooltip(zh_cn = "传送时的音效。", en_us = "The sound effect when teleporting.")
            private String tpSound = SoundEvents.ENDERMAN_TELEPORT.getRegistryName().toString();
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
            @Getter(AccessLevel.NONE)
            @Setter(AccessLevel.NONE)
            @ConfigEntry.Gui.CollapsibleObject
            @ConfigEntry.Gui.Tooltip(zh_cn = "安全传送", en_us = "Safe Teleport")
            private ServerSafeCategory safeTeleport = new ServerSafeCategory();
        }

        @Getter
        @Setter
        @Accessors(chain = true, fluent = true)
        public static class ServerSafeCategory {
            @ConfigEntry.Gui.Tooltip(zh_cn = "不安全的方块列表，玩家不会传送到这些方块上。", en_us = "The list of unsafe blocks, players will not be teleported to these blocks.")
            private List<String> unsafeBlocks = Stream.of(Blocks.LAVA, Blocks.FIRE, Blocks.CAMPFIRE, Blocks.SOUL_FIRE, Blocks.SOUL_CAMPFIRE, Blocks.CACTUS, Blocks.MAGMA_BLOCK, Blocks.SWEET_BERRY_BUSH).map(block -> {
                ResourceLocation rl = block.getRegistryName();
                return rl == null ? "" : rl.toString();
            }).collect(Collectors.toList());
            @ConfigEntry.Gui.Tooltip(zh_cn = "窒息的方块列表，玩家头不会处于这些方块里面。", en_us = "The list of suffocating blocks, players will not be teleported to these blocks.")
            private List<String> suffocatingBlocks = Stream.of(Blocks.LAVA, Blocks.WATER).map(block -> {
                ResourceLocation rl = block.getRegistryName();
                return rl == null ? "" : rl.toString();
            }).collect(Collectors.toList());
            @ConfigEntry.Gui.Tooltip(zh_cn = "当进行安全传送时，如果未找到安全坐标，是否在脚下放置方块。", en_us = "When performing a safe teleport, whether to place a block underfoot if a safe safeWorldCoordinate is not found.")
            private boolean setBlockWhenSafeNotFound = false;
            @ConfigEntry.Gui.Tooltip(zh_cn = "当进行安全传送时，如果未找到安全坐标，是否仅从背包中获取可放置的方块。", en_us = "When performing a safe teleport, whether to only use placeable blocks from the player's inventory if a safe safeWorldCoordinate is not found.")
            private boolean getBlockFromInventory = true;
            @ConfigEntry.Gui.Tooltip(zh_cn = "当进行安全传送时，如果未找到安全坐标，放置方块的列表。若'getBlockFromInventory'为false，则始终使用列表中的第一个方块。", en_us = "When performing a safe teleport, the list of blocks to place if a safe safeWorldCoordinate is not found. If 'getBlockFromInventory' is set to false, the first block in the list will always be used.")
            private List<String> safeBlocks = Stream.of(Blocks.GRASS_BLOCK, Blocks.GRASS_PATH, Blocks.DIRT, Blocks.COBBLESTONE).map(block -> {
                ResourceLocation rl = block.getRegistryName();
                return rl == null ? "" : rl.toString();
            }).collect(Collectors.toList());
            @ConfigEntry.Gui.Tooltip(zh_cn = "当进行安全传送时，寻找安全坐标的半径，单位为区块。", en_us = "The chunk range for finding a safe safeWorldCoordinate, in chunks.")
            @ConfigEntry.BoundedDiscrete(min = 1, max = 16)
            private int safeChunkRange = 1;
        }

        @Getter
        @Setter
        @Accessors(chain = true, fluent = true)
        public static class ServerPermissionCategory {
            @Getter(AccessLevel.NONE)
            @Setter(AccessLevel.NONE)
            @ConfigEntry.Gui.CollapsibleObject
            @ConfigEntry.Gui.Tooltip(zh_cn = "指令权限", en_us = "Command Permission")
            private ServerPermissionCommandCategory command = new ServerPermissionCommandCategory();
            @Getter(AccessLevel.NONE)
            @Setter(AccessLevel.NONE)
            @ConfigEntry.Gui.CollapsibleObject
            @ConfigEntry.Gui.Tooltip(zh_cn = "跨维度权限", en_us = "Across dimensions Switch")
            private ServerPermissionAcrossCategory across = new ServerPermissionAcrossCategory();
        }

        @Getter
        @Setter
        @Accessors(chain = true, fluent = true)
        public static class ServerPermissionCommandCategory {
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
        public static class ServerPermissionAcrossCategory {
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
        public static class ServerCooldownCategory {
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
        public static class ServerCostCategory {
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

        public static final class Key {
            private Key() {
            }

            public static final String SG_TELEPORT_RECORD_LIMIT = "server.general.teleportRecordLimit";
            public static final String SG_TELEPORT_BACK_SKIP_TYPE = "server.general.teleportBackSkipType";
            public static final String SG_TELEPORT_ACROSS_DIMENSION = "server.general.teleportAcrossDimension";
            public static final String SG_TELEPORT_COST_DISTANCE_LIMIT = "server.general.teleportCostDistanceLimit";
            public static final String SG_TELEPORT_COST_DISTANCE_ACROSS_DIMENSION = "server.general.teleportCostDistanceAcrossDimension";
            public static final String SG_TELEPORT_VIEW_DISTANCE_LIMIT = "server.general.teleportViewDistanceLimit";
            public static final String SG_TELEPORT_REQUEST_EXPIRE_TIME = "server.general.teleportRequestExpireTime";
            public static final String SG_TELEPORT_REQUEST_COOLDOWN_TYPE = "server.general.teleportRequestCooldownType";
            public static final String SG_TELEPORT_REQUEST_COOLDOWN = "server.general.teleportRequestCooldown";
            public static final String SG_TELEPORT_RANDOM_DISTANCE_LIMIT = "server.general.teleportRandomDistanceLimit";
            public static final String SG_GRAVE_SEARCH_RANGE_LIMIT = "server.general.graveSearchRangeLimit";
            public static final String SG_TELEPORT_HOME_LIMIT = "server.general.teleportHomeLimit";
            public static final String SG_HELP_HEADER = "server.general.helpHeader";
            public static final String SG_TP_SOUND = "server.general.tpSound";
            public static final String SG_TP_WITH_VEHICLE = "server.general.tpWithVehicle";
            public static final String SG_TP_WITH_FOLLOWER = "server.general.tpWithFollower";
            public static final String SG_TP_WITH_FOLLOWER_RANGE = "server.general.tpWithFollowerRange";
            public static final String SG_HELP_INFO_NUM_PER_PAGE = "server.general.helpInfoNumPerPage";
            public static final String SG_DEFAULT_LANGUAGE = "server.general.defaultLanguage";
            public static final String SG_TP_WITH_ENEMY = "server.general.tpWithEnemy";
            public static final String SG_UNSAFE_BLOCKS = "server.general.safeTeleport.unsafeBlocks";
            public static final String SG_SUFFOCATING_BLOCKS = "server.general.safeTeleport.suffocatingBlocks";
            public static final String SG_SET_BLOCK_WHEN_SAFE_NOT_FOUND = "server.general.safeTeleport.setBlockWhenSafeNotFound";
            public static final String SG_GET_BLOCK_FROM_INVENTORY = "server.general.safeTeleport.getBlockFromInventory";
            public static final String SG_SAFE_BLOCKS = "server.general.safeTeleport.safeBlocks";
            public static final String SG_SAFE_CHUNK_RANGE = "server.general.safeTeleport.safeChunkRange";
            public static final String SP_CMD_PERMISSIONFEEDOTHER = "server.permission.command.permissionFeedOther";
            public static final String SP_CMD_PERMISSIONTPCOORDINATE = "server.permission.command.permissionTpCoordinate";
            public static final String SP_CMD_PERMISSIONTPSTRUCTURE = "server.permission.command.permissionTpStructure";
            public static final String SP_CMD_PERMISSIONTPASK = "server.permission.command.permissionTpAsk";
            public static final String SP_CMD_PERMISSIONTPHERE = "server.permission.command.permissionTpHere";
            public static final String SP_CMD_PERMISSIONTPRANDOM = "server.permission.command.permissionTpRandom";
            public static final String SP_CMD_PERMISSIONTPSPAWN = "server.permission.command.permissionTpSpawn";
            public static final String SP_CMD_PERMISSIONTPSPAWNOTHER = "server.permission.command.permissionTpSpawnOther";
            public static final String SP_CMD_PERMISSIONTPWORLDSPAWN = "server.permission.command.permissionTpWorldSpawn";
            public static final String SP_CMD_PERMISSIONTPTOP = "server.permission.command.permissionTpTop";
            public static final String SP_CMD_PERMISSIONTPBOTTOM = "server.permission.command.permissionTpBottom";
            public static final String SP_CMD_PERMISSIONTPUP = "server.permission.command.permissionTpUp";
            public static final String SP_CMD_PERMISSIONTPDOWN = "server.permission.command.permissionTpDown";
            public static final String SP_CMD_PERMISSIONTPVIEW = "server.permission.command.permissionTpView";
            public static final String SP_CMD_PERMISSIONTPHOME = "server.permission.command.permissionTpHome";
            public static final String SP_CMD_PERMISSIONTPSTAGE = "server.permission.command.permissionTpStage";
            public static final String SP_CMD_PERMISSIONTPSTAGESET = "server.permission.command.permissionTpStageSet";
            public static final String SP_CMD_PERMISSIONTPSTAGEDEL = "server.permission.command.permissionTpStageDel";
            public static final String SP_CMD_PERMISSIONTPSTAGEGET = "server.permission.command.permissionTpStageGet";
            public static final String SP_CMD_PERMISSIONTPBACK = "server.permission.command.permissionTpBack";
            public static final String SP_CMD_PERMISSIONTPGRAVE = "server.permission.command.permissionTpGrave";
            public static final String SP_CMD_PERMISSIONFLY = "server.permission.command.permissionFly";
            public static final String SP_CMD_PERMISSIONVIRTUALOP = "server.permission.command.permissionVirtualOp";
            public static final String SP_CMD_PERMISSIONSETCARD = "server.permission.command.permissionSetCard";
            public static final String SP_ACR_PERMISSIONTPCOORDINATEACROSSDIMENSION = "server.permission.across.permissionTpCoordinateAcrossDimension";
            public static final String SP_ACR_PERMISSIONTPSTRUCTUREACROSSDIMENSION = "server.permission.across.permissionTpStructureAcrossDimension";
            public static final String SP_ACR_PERMISSIONTPASKACROSSDIMENSION = "server.permission.across.permissionTpAskAcrossDimension";
            public static final String SP_ACR_PERMISSIONTPHEREACROSSDIMENSION = "server.permission.across.permissionTpHereAcrossDimension";
            public static final String SP_ACR_PERMISSIONTPRANDOMACROSSDIMENSION = "server.permission.across.permissionTpRandomAcrossDimension";
            public static final String SP_ACR_PERMISSIONTPSPAWNACROSSDIMENSION = "server.permission.across.permissionTpSpawnAcrossDimension";
            public static final String SP_ACR_PERMISSIONTPWORLDSPAWNACROSSDIMENSION = "server.permission.across.permissionTpWorldSpawnAcrossDimension";
            public static final String SP_ACR_PERMISSIONTPHOMEACROSSDIMENSION = "server.permission.across.permissionTpHomeAcrossDimension";
            public static final String SP_ACR_PERMISSIONTPSTAGEACROSSDIMENSION = "server.permission.across.permissionTpStageAcrossDimension";
            public static final String SP_ACR_PERMISSIONTPBACKACROSSDIMENSION = "server.permission.across.permissionTpBackAcrossDimension";
            public static final String SP_ACR_PERMISSIONTPGRAVEACROSSDIMENSION = "server.permission.across.permissionTpGraveAcrossDimension";
            public static final String CD_COOLDOWNTPCOORDINATE = "server.cooldown.cooldownTpCoordinate";
            public static final String CD_COOLDOWNTPSTRUCTURE = "server.cooldown.cooldownTpStructure";
            public static final String CD_COOLDOWNTPASK = "server.cooldown.cooldownTpAsk";
            public static final String CD_COOLDOWNTPHERE = "server.cooldown.cooldownTpHere";
            public static final String CD_COOLDOWNTPRANDOM = "server.cooldown.cooldownTpRandom";
            public static final String CD_COOLDOWNTPSPAWN = "server.cooldown.cooldownTpSpawn";
            public static final String CD_COOLDOWNTPWORLDSPAWN = "server.cooldown.cooldownTpWorldSpawn";
            public static final String CD_COOLDOWNTPTOP = "server.cooldown.cooldownTpTop";
            public static final String CD_COOLDOWNTPBOTTOM = "server.cooldown.cooldownTpBottom";
            public static final String CD_COOLDOWNTPUP = "server.cooldown.cooldownTpUp";
            public static final String CD_COOLDOWNTPDOWN = "server.cooldown.cooldownTpDown";
            public static final String CD_COOLDOWNTPVIEW = "server.cooldown.cooldownTpView";
            public static final String CD_COOLDOWNTPHOME = "server.cooldown.cooldownTpHome";
            public static final String CD_COOLDOWNTPSTAGE = "server.cooldown.cooldownTpStage";
            public static final String CD_COOLDOWNTPBACK = "server.cooldown.cooldownTpBack";
            public static final String CD_COOLDOWNTPGRAVE = "server.cooldown.cooldownTpGrave";
            public static final String CT_COSTTPCOORDINATE_TYPE = "server.cost.tpCoordinate.costTpCoordinateType";
            public static final String CT_COSTTPCOORDINATE_NUM = "server.cost.tpCoordinate.costTpCoordinateNum";
            public static final String CT_COSTTPCOORDINATE_CONF = "server.cost.tpCoordinate.costTpCoordinateConf";
            public static final String CT_COSTTPCOORDINATE_RATE = "server.cost.tpCoordinate.costTpCoordinateRate";
            public static final String CT_COSTTPCOORDINATE_NUM_UPPER = "server.cost.tpCoordinate.costTpCoordinateNumUpper";
            public static final String CT_COSTTPCOORDINATE_NUM_LOWER = "server.cost.tpCoordinate.costTpCoordinateNumLower";
            public static final String CT_COSTTPCOORDINATE_EXP = "server.cost.tpCoordinate.costTpCoordinateExp";
            public static final String CT_COSTTPSTRUCTURE_TYPE = "server.cost.tpStructure.costTpStructureType";
            public static final String CT_COSTTPSTRUCTURE_NUM = "server.cost.tpStructure.costTpStructureNum";
            public static final String CT_COSTTPSTRUCTURE_CONF = "server.cost.tpStructure.costTpStructureConf";
            public static final String CT_COSTTPSTRUCTURE_RATE = "server.cost.tpStructure.costTpStructureRate";
            public static final String CT_COSTTPSTRUCTURE_NUM_UPPER = "server.cost.tpStructure.costTpStructureNumUpper";
            public static final String CT_COSTTPSTRUCTURE_NUM_LOWER = "server.cost.tpStructure.costTpStructureNumLower";
            public static final String CT_COSTTPSTRUCTURE_EXP = "server.cost.tpStructure.costTpStructureExp";
            public static final String CT_COSTTPASK_TYPE = "server.cost.tpAsk.costTpAskType";
            public static final String CT_COSTTPASK_NUM = "server.cost.tpAsk.costTpAskNum";
            public static final String CT_COSTTPASK_CONF = "server.cost.tpAsk.costTpAskConf";
            public static final String CT_COSTTPASK_RATE = "server.cost.tpAsk.costTpAskRate";
            public static final String CT_COSTTPASK_NUM_UPPER = "server.cost.tpAsk.costTpAskNumUpper";
            public static final String CT_COSTTPASK_NUM_LOWER = "server.cost.tpAsk.costTpAskNumLower";
            public static final String CT_COSTTPASK_EXP = "server.cost.tpAsk.costTpAskExp";
            public static final String CT_COSTTPHERE_TYPE = "server.cost.tpHere.costTpHereType";
            public static final String CT_COSTTPHERE_NUM = "server.cost.tpHere.costTpHereNum";
            public static final String CT_COSTTPHERE_CONF = "server.cost.tpHere.costTpHereConf";
            public static final String CT_COSTTPHERE_RATE = "server.cost.tpHere.costTpHereRate";
            public static final String CT_COSTTPHERE_NUM_UPPER = "server.cost.tpHere.costTpHereNumUpper";
            public static final String CT_COSTTPHERE_NUM_LOWER = "server.cost.tpHere.costTpHereNumLower";
            public static final String CT_COSTTPHERE_EXP = "server.cost.tpHere.costTpHereExp";
            public static final String CT_COSTTPRANDOM_TYPE = "server.cost.tpRandom.costTpRandomType";
            public static final String CT_COSTTPRANDOM_NUM = "server.cost.tpRandom.costTpRandomNum";
            public static final String CT_COSTTPRANDOM_CONF = "server.cost.tpRandom.costTpRandomConf";
            public static final String CT_COSTTPRANDOM_RATE = "server.cost.tpRandom.costTpRandomRate";
            public static final String CT_COSTTPRANDOM_NUM_UPPER = "server.cost.tpRandom.costTpRandomNumUpper";
            public static final String CT_COSTTPRANDOM_NUM_LOWER = "server.cost.tpRandom.costTpRandomNumLower";
            public static final String CT_COSTTPRANDOM_EXP = "server.cost.tpRandom.costTpRandomExp";
            public static final String CT_COSTTPSPAWN_TYPE = "server.cost.tpSpawn.costTpSpawnType";
            public static final String CT_COSTTPSPAWN_NUM = "server.cost.tpSpawn.costTpSpawnNum";
            public static final String CT_COSTTPSPAWN_CONF = "server.cost.tpSpawn.costTpSpawnConf";
            public static final String CT_COSTTPSPAWN_RATE = "server.cost.tpSpawn.costTpSpawnRate";
            public static final String CT_COSTTPSPAWN_NUM_UPPER = "server.cost.tpSpawn.costTpSpawnNumUpper";
            public static final String CT_COSTTPSPAWN_NUM_LOWER = "server.cost.tpSpawn.costTpSpawnNumLower";
            public static final String CT_COSTTPSPAWN_EXP = "server.cost.tpSpawn.costTpSpawnExp";
            public static final String CT_COSTTPWORLDSPAWN_TYPE = "server.cost.tpWorldSpawn.costTpWorldSpawnType";
            public static final String CT_COSTTPWORLDSPAWN_NUM = "server.cost.tpWorldSpawn.costTpWorldSpawnNum";
            public static final String CT_COSTTPWORLDSPAWN_CONF = "server.cost.tpWorldSpawn.costTpWorldSpawnConf";
            public static final String CT_COSTTPWORLDSPAWN_RATE = "server.cost.tpWorldSpawn.costTpWorldSpawnRate";
            public static final String CT_COSTTPWORLDSPAWN_NUM_UPPER = "server.cost.tpWorldSpawn.costTpWorldSpawnNumUpper";
            public static final String CT_COSTTPWORLDSPAWN_NUM_LOWER = "server.cost.tpWorldSpawn.costTpWorldSpawnNumLower";
            public static final String CT_COSTTPWORLDSPAWN_EXP = "server.cost.tpWorldSpawn.costTpWorldSpawnExp";
            public static final String CT_COSTTPTOP_TYPE = "server.cost.tpTop.costTpTopType";
            public static final String CT_COSTTPTOP_NUM = "server.cost.tpTop.costTpTopNum";
            public static final String CT_COSTTPTOP_CONF = "server.cost.tpTop.costTpTopConf";
            public static final String CT_COSTTPTOP_RATE = "server.cost.tpTop.costTpTopRate";
            public static final String CT_COSTTPTOP_NUM_UPPER = "server.cost.tpTop.costTpTopNumUpper";
            public static final String CT_COSTTPTOP_NUM_LOWER = "server.cost.tpTop.costTpTopNumLower";
            public static final String CT_COSTTPTOP_EXP = "server.cost.tpTop.costTpTopExp";
            public static final String CT_COSTTPBOTTOM_TYPE = "server.cost.tpBottom.costTpBottomType";
            public static final String CT_COSTTPBOTTOM_NUM = "server.cost.tpBottom.costTpBottomNum";
            public static final String CT_COSTTPBOTTOM_CONF = "server.cost.tpBottom.costTpBottomConf";
            public static final String CT_COSTTPBOTTOM_RATE = "server.cost.tpBottom.costTpBottomRate";
            public static final String CT_COSTTPBOTTOM_NUM_UPPER = "server.cost.tpBottom.costTpBottomNumUpper";
            public static final String CT_COSTTPBOTTOM_NUM_LOWER = "server.cost.tpBottom.costTpBottomNumLower";
            public static final String CT_COSTTPBOTTOM_EXP = "server.cost.tpBottom.costTpBottomExp";
            public static final String CT_COSTTPUP_TYPE = "server.cost.tpUp.costTpUpType";
            public static final String CT_COSTTPUP_NUM = "server.cost.tpUp.costTpUpNum";
            public static final String CT_COSTTPUP_CONF = "server.cost.tpUp.costTpUpConf";
            public static final String CT_COSTTPUP_RATE = "server.cost.tpUp.costTpUpRate";
            public static final String CT_COSTTPUP_NUM_UPPER = "server.cost.tpUp.costTpUpNumUpper";
            public static final String CT_COSTTPUP_NUM_LOWER = "server.cost.tpUp.costTpUpNumLower";
            public static final String CT_COSTTPUP_EXP = "server.cost.tpUp.costTpUpExp";
            public static final String CT_COSTTPDOWN_TYPE = "server.cost.tpDown.costTpDownType";
            public static final String CT_COSTTPDOWN_NUM = "server.cost.tpDown.costTpDownNum";
            public static final String CT_COSTTPDOWN_CONF = "server.cost.tpDown.costTpDownConf";
            public static final String CT_COSTTPDOWN_RATE = "server.cost.tpDown.costTpDownRate";
            public static final String CT_COSTTPDOWN_NUM_UPPER = "server.cost.tpDown.costTpDownNumUpper";
            public static final String CT_COSTTPDOWN_NUM_LOWER = "server.cost.tpDown.costTpDownNumLower";
            public static final String CT_COSTTPDOWN_EXP = "server.cost.tpDown.costTpDownExp";
            public static final String CT_COSTTPVIEW_TYPE = "server.cost.tpView.costTpViewType";
            public static final String CT_COSTTPVIEW_NUM = "server.cost.tpView.costTpViewNum";
            public static final String CT_COSTTPVIEW_CONF = "server.cost.tpView.costTpViewConf";
            public static final String CT_COSTTPVIEW_RATE = "server.cost.tpView.costTpViewRate";
            public static final String CT_COSTTPVIEW_NUM_UPPER = "server.cost.tpView.costTpViewNumUpper";
            public static final String CT_COSTTPVIEW_NUM_LOWER = "server.cost.tpView.costTpViewNumLower";
            public static final String CT_COSTTPVIEW_EXP = "server.cost.tpView.costTpViewExp";
            public static final String CT_COSTTPHOME_TYPE = "server.cost.tpHome.costTpHomeType";
            public static final String CT_COSTTPHOME_NUM = "server.cost.tpHome.costTpHomeNum";
            public static final String CT_COSTTPHOME_CONF = "server.cost.tpHome.costTpHomeConf";
            public static final String CT_COSTTPHOME_RATE = "server.cost.tpHome.costTpHomeRate";
            public static final String CT_COSTTPHOME_NUM_UPPER = "server.cost.tpHome.costTpHomeNumUpper";
            public static final String CT_COSTTPHOME_NUM_LOWER = "server.cost.tpHome.costTpHomeNumLower";
            public static final String CT_COSTTPHOME_EXP = "server.cost.tpHome.costTpHomeExp";
            public static final String CT_COSTTPSTAGE_TYPE = "server.cost.tpStage.costTpStageType";
            public static final String CT_COSTTPSTAGE_NUM = "server.cost.tpStage.costTpStageNum";
            public static final String CT_COSTTPSTAGE_CONF = "server.cost.tpStage.costTpStageConf";
            public static final String CT_COSTTPSTAGE_RATE = "server.cost.tpStage.costTpStageRate";
            public static final String CT_COSTTPSTAGE_NUM_UPPER = "server.cost.tpStage.costTpStageNumUpper";
            public static final String CT_COSTTPSTAGE_NUM_LOWER = "server.cost.tpStage.costTpStageNumLower";
            public static final String CT_COSTTPSTAGE_EXP = "server.cost.tpStage.costTpStageExp";
            public static final String CT_COSTTPBACK_TYPE = "server.cost.tpBack.costTpBackType";
            public static final String CT_COSTTPBACK_NUM = "server.cost.tpBack.costTpBackNum";
            public static final String CT_COSTTPBACK_CONF = "server.cost.tpBack.costTpBackConf";
            public static final String CT_COSTTPBACK_RATE = "server.cost.tpBack.costTpBackRate";
            public static final String CT_COSTTPBACK_NUM_UPPER = "server.cost.tpBack.costTpBackNumUpper";
            public static final String CT_COSTTPBACK_NUM_LOWER = "server.cost.tpBack.costTpBackNumLower";
            public static final String CT_COSTTPBACK_EXP = "server.cost.tpBack.costTpBackExp";
            public static final String CT_COSTTPGRAVE_TYPE = "server.cost.tpGrave.costTpGraveType";
            public static final String CT_COSTTPGRAVE_NUM = "server.cost.tpGrave.costTpGraveNum";
            public static final String CT_COSTTPGRAVE_CONF = "server.cost.tpGrave.costTpGraveConf";
            public static final String CT_COSTTPGRAVE_RATE = "server.cost.tpGrave.costTpGraveRate";
            public static final String CT_COSTTPGRAVE_NUM_UPPER = "server.cost.tpGrave.costTpGraveNumUpper";
            public static final String CT_COSTTPGRAVE_NUM_LOWER = "server.cost.tpGrave.costTpGraveNumLower";
            public static final String CT_COSTTPGRAVE_EXP = "server.cost.tpGrave.costTpGraveExp";
        }

        private static EnumCostType parseHeldCostType(Object v, EnumCostType def) {
            if (v instanceof EnumCostType) {
                return (EnumCostType) v;
            }
            if (v instanceof String) {
                try {
                    return EnumCostType.valueOf((String) v);
                } catch (IllegalArgumentException ignored) {
                    return def;
                }
            }
            return def;
        }

        private static EnumCoolDownType parseHeldCoolDownType(Object v, EnumCoolDownType def) {
            if (v instanceof EnumCoolDownType) {
                return (EnumCoolDownType) v;
            }
            if (v instanceof String) {
                try {
                    return EnumCoolDownType.valueOf((String) v);
                } catch (IllegalArgumentException ignored) {
                    return def;
                }
            }
            return def;
        }

        public static final class General {
            private final ConfigHolder holder;

            General(ConfigHolder holder) {
                this.holder = holder;
            }

            private int gi(String k, int def) {
                if (holder == null) return def;
                Integer v = holder.get(k);
                return v != null ? v : def;
            }

            private String gs(String k, String def) {
                if (holder == null) return def;
                String v = holder.get(k);
                return v != null ? v : def;
            }

            private boolean gb(String k, boolean def) {
                if (holder == null) return def;
                Boolean v = holder.get(k);
                return v != null ? v : def;
            }

            public List<String> teleportBackSkipType() {
                if (holder == null) return new ArrayList<>();
                List<String> v = holder.get(Key.SG_TELEPORT_BACK_SKIP_TYPE);
                return v != null ? v : new ArrayList<>();
            }

            public General teleportBackSkipType(List<String> v) {
                if (holder != null) holder.set(Key.SG_TELEPORT_BACK_SKIP_TYPE, v);
                return this;
            }

            public int teleportRecordLimit() {
                return gi(Key.SG_TELEPORT_RECORD_LIMIT, 100);
            }

            public boolean teleportAcrossDimension() {
                return gb(Key.SG_TELEPORT_ACROSS_DIMENSION, true);
            }

            public int teleportCostDistanceLimit() {
                return gi(Key.SG_TELEPORT_COST_DISTANCE_LIMIT, 10000);
            }

            public int teleportCostDistanceAcrossDimension() {
                return gi(Key.SG_TELEPORT_COST_DISTANCE_ACROSS_DIMENSION, 10000);
            }

            public int teleportViewDistanceLimit() {
                return gi(Key.SG_TELEPORT_VIEW_DISTANCE_LIMIT, 16 * 64);
            }

            public int teleportRequestExpireTime() {
                return gi(Key.SG_TELEPORT_REQUEST_EXPIRE_TIME, 60);
            }

            public EnumCoolDownType teleportRequestCooldownType() {
                if (holder == null) return EnumCoolDownType.INDIVIDUAL;
                return parseHeldCoolDownType(holder.get(Key.SG_TELEPORT_REQUEST_COOLDOWN_TYPE), EnumCoolDownType.INDIVIDUAL);
            }

            public int teleportRequestCooldown() {
                return gi(Key.SG_TELEPORT_REQUEST_COOLDOWN, 10);
            }

            public int teleportRandomDistanceLimit() {
                return gi(Key.SG_TELEPORT_RANDOM_DISTANCE_LIMIT, 10000);
            }

            public int graveSearchRangeLimit() {
                return gi(Key.SG_GRAVE_SEARCH_RANGE_LIMIT, 32);
            }

            public int teleportHomeLimit() {
                return gi(Key.SG_TELEPORT_HOME_LIMIT, 5);
            }

            public String helpHeader() {
                return gs(Key.SG_HELP_HEADER, "-----==== Narcissus Farewell Help (%d/%d) ====-----");
            }

            public String tpSound() {
                return gs(Key.SG_TP_SOUND, SoundEvents.ENDERMAN_TELEPORT.getRegistryName().toString());
            }

            public boolean tpWithVehicle() {
                return gb(Key.SG_TP_WITH_VEHICLE, true);
            }

            public boolean tpWithFollower() {
                return gb(Key.SG_TP_WITH_FOLLOWER, true);
            }

            public int tpWithFollowerRange() {
                return gi(Key.SG_TP_WITH_FOLLOWER_RANGE, 10);
            }

            public int helpInfoNumPerPage() {
                return gi(Key.SG_HELP_INFO_NUM_PER_PAGE, 5);
            }

            public String defaultLanguage() {
                return gs(Key.SG_DEFAULT_LANGUAGE, "en_us");
            }

            public General defaultLanguage(String v) {
                if (holder != null) holder.set(Key.SG_DEFAULT_LANGUAGE, v);
                return this;
            }

            public boolean tpWithEnemy() {
                return gb(Key.SG_TP_WITH_ENEMY, false);
            }

            public List<String> unsafeBlocks() {
                if (holder == null) return new ArrayList<>();
                List<String> v = holder.get(Key.SG_UNSAFE_BLOCKS);
                return v != null ? v : new ArrayList<>();
            }

            public List<String> suffocatingBlocks() {
                if (holder == null) return new ArrayList<>();
                List<String> v = holder.get(Key.SG_SUFFOCATING_BLOCKS);
                return v != null ? v : new ArrayList<>();
            }

            public boolean setBlockWhenSafeNotFound() {
                return gb(Key.SG_SET_BLOCK_WHEN_SAFE_NOT_FOUND, false);
            }

            public boolean getBlockFromInventory() {
                return gb(Key.SG_GET_BLOCK_FROM_INVENTORY, true);
            }

            public List<String> safeBlocks() {
                if (holder == null) return new ArrayList<>();
                List<String> v = holder.get(Key.SG_SAFE_BLOCKS);
                return v != null ? v : new ArrayList<>();
            }

            public int safeChunkRange() {
                return gi(Key.SG_SAFE_CHUNK_RANGE, 1);
            }
        }

        public static final class Permission {
            private final ConfigHolder holder;

            Permission(ConfigHolder holder) {
                this.holder = holder;
            }

            private int gi(String k, int def) {
                if (holder == null) return def;
                Integer v = holder.get(k);
                return v != null ? v : def;
            }

            public int permissionFeedOther() {
                return gi(Key.SP_CMD_PERMISSIONFEEDOTHER, 2);
            }

            public int permissionTpCoordinate() {
                return gi(Key.SP_CMD_PERMISSIONTPCOORDINATE, 2);
            }

            public int permissionTpStructure() {
                return gi(Key.SP_CMD_PERMISSIONTPSTRUCTURE, 2);
            }

            public int permissionTpAsk() {
                return gi(Key.SP_CMD_PERMISSIONTPASK, 0);
            }

            public int permissionTpHere() {
                return gi(Key.SP_CMD_PERMISSIONTPHERE, 0);
            }

            public int permissionTpRandom() {
                return gi(Key.SP_CMD_PERMISSIONTPRANDOM, 1);
            }

            public int permissionTpSpawn() {
                return gi(Key.SP_CMD_PERMISSIONTPSPAWN, 0);
            }

            public int permissionTpSpawnOther() {
                return gi(Key.SP_CMD_PERMISSIONTPSPAWNOTHER, 2);
            }

            public int permissionTpWorldSpawn() {
                return gi(Key.SP_CMD_PERMISSIONTPWORLDSPAWN, 0);
            }

            public int permissionTpTop() {
                return gi(Key.SP_CMD_PERMISSIONTPTOP, 1);
            }

            public int permissionTpBottom() {
                return gi(Key.SP_CMD_PERMISSIONTPBOTTOM, 1);
            }

            public int permissionTpUp() {
                return gi(Key.SP_CMD_PERMISSIONTPUP, 1);
            }

            public int permissionTpDown() {
                return gi(Key.SP_CMD_PERMISSIONTPDOWN, 1);
            }

            public int permissionTpView() {
                return gi(Key.SP_CMD_PERMISSIONTPVIEW, 1);
            }

            public int permissionTpHome() {
                return gi(Key.SP_CMD_PERMISSIONTPHOME, 0);
            }

            public int permissionTpStage() {
                return gi(Key.SP_CMD_PERMISSIONTPSTAGE, 0);
            }

            public int permissionTpStageSet() {
                return gi(Key.SP_CMD_PERMISSIONTPSTAGESET, 2);
            }

            public int permissionTpStageDel() {
                return gi(Key.SP_CMD_PERMISSIONTPSTAGEDEL, 2);
            }

            public int permissionTpStageGet() {
                return gi(Key.SP_CMD_PERMISSIONTPSTAGEGET, 0);
            }

            public int permissionTpBack() {
                return gi(Key.SP_CMD_PERMISSIONTPBACK, 0);
            }

            public int permissionTpGrave() {
                return gi(Key.SP_CMD_PERMISSIONTPGRAVE, 0);
            }

            public int permissionFly() {
                return gi(Key.SP_CMD_PERMISSIONFLY, 2);
            }

            public int permissionVirtualOp() {
                return gi(Key.SP_CMD_PERMISSIONVIRTUALOP, 4);
            }

            public int permissionSetCard() {
                return gi(Key.SP_CMD_PERMISSIONSETCARD, 2);
            }

            public int permissionTpCoordinateAcrossDimension() {
                return gi(Key.SP_ACR_PERMISSIONTPCOORDINATEACROSSDIMENSION, 2);
            }

            public int permissionTpStructureAcrossDimension() {
                return gi(Key.SP_ACR_PERMISSIONTPSTRUCTUREACROSSDIMENSION, 2);
            }

            public int permissionTpAskAcrossDimension() {
                return gi(Key.SP_ACR_PERMISSIONTPASKACROSSDIMENSION, 0);
            }

            public int permissionTpHereAcrossDimension() {
                return gi(Key.SP_ACR_PERMISSIONTPHEREACROSSDIMENSION, 0);
            }

            public int permissionTpRandomAcrossDimension() {
                return gi(Key.SP_ACR_PERMISSIONTPRANDOMACROSSDIMENSION, 0);
            }

            public int permissionTpSpawnAcrossDimension() {
                return gi(Key.SP_ACR_PERMISSIONTPSPAWNACROSSDIMENSION, 0);
            }

            public int permissionTpWorldSpawnAcrossDimension() {
                return gi(Key.SP_ACR_PERMISSIONTPWORLDSPAWNACROSSDIMENSION, 0);
            }

            public int permissionTpHomeAcrossDimension() {
                return gi(Key.SP_ACR_PERMISSIONTPHOMEACROSSDIMENSION, 0);
            }

            public int permissionTpStageAcrossDimension() {
                return gi(Key.SP_ACR_PERMISSIONTPSTAGEACROSSDIMENSION, 0);
            }

            public int permissionTpBackAcrossDimension() {
                return gi(Key.SP_ACR_PERMISSIONTPBACKACROSSDIMENSION, 0);
            }

            public int permissionTpGraveAcrossDimension() {
                return gi(Key.SP_ACR_PERMISSIONTPGRAVEACROSSDIMENSION, 0);
            }
        }

        public static final class Cooldown {
            private final ConfigHolder holder;

            Cooldown(ConfigHolder holder) {
                this.holder = holder;
            }

            private int gi(String k, int def) {
                if (holder == null) return def;
                Integer v = holder.get(k);
                return v != null ? v : def;
            }

            public int cooldownTpCoordinate() {
                return gi(Key.CD_COOLDOWNTPCOORDINATE, 10);
            }

            public int cooldownTpStructure() {
                return gi(Key.CD_COOLDOWNTPSTRUCTURE, 10);
            }

            public int cooldownTpAsk() {
                return gi(Key.CD_COOLDOWNTPASK, 10);
            }

            public int cooldownTpHere() {
                return gi(Key.CD_COOLDOWNTPHERE, 10);
            }

            public int cooldownTpRandom() {
                return gi(Key.CD_COOLDOWNTPRANDOM, 10);
            }

            public int cooldownTpSpawn() {
                return gi(Key.CD_COOLDOWNTPSPAWN, 10);
            }

            public int cooldownTpWorldSpawn() {
                return gi(Key.CD_COOLDOWNTPWORLDSPAWN, 10);
            }

            public int cooldownTpTop() {
                return gi(Key.CD_COOLDOWNTPTOP, 10);
            }

            public int cooldownTpBottom() {
                return gi(Key.CD_COOLDOWNTPBOTTOM, 10);
            }

            public int cooldownTpUp() {
                return gi(Key.CD_COOLDOWNTPUP, 10);
            }

            public int cooldownTpDown() {
                return gi(Key.CD_COOLDOWNTPDOWN, 10);
            }

            public int cooldownTpView() {
                return gi(Key.CD_COOLDOWNTPVIEW, 10);
            }

            public int cooldownTpHome() {
                return gi(Key.CD_COOLDOWNTPHOME, 10);
            }

            public int cooldownTpStage() {
                return gi(Key.CD_COOLDOWNTPSTAGE, 10);
            }

            public int cooldownTpBack() {
                return gi(Key.CD_COOLDOWNTPBACK, 10);
            }

            public int cooldownTpGrave() {
                return gi(Key.CD_COOLDOWNTPGRAVE, 10);
            }
        }

        public static final class CostGroup {
            private final ConfigHolder holder;
            private final String kt;
            private final String kn;
            private final String kc;
            private final String kr;
            private final String ku;
            private final String kl;
            private final String ke;

            CostGroup(ConfigHolder holder, String kt, String kn, String kc, String kr, String ku, String kl, String ke) {
                this.holder = holder;
                this.kt = kt;
                this.kn = kn;
                this.kc = kc;
                this.kr = kr;
                this.ku = ku;
                this.kl = kl;
                this.ke = ke;
            }

            public EnumCostType type() {
                if (holder == null) return EnumCostType.NONE;
                return parseHeldCostType(holder.get(kt), EnumCostType.NONE);
            }

            public int num() {
                return gi(kn, 1);
            }

            public String conf() {
                return gs(kc, "");
            }

            public double rate() {
                return gd(kr, 0.002);
            }

            public int numUpper() {
                return gi(ku, 20);
            }

            public int numLower() {
                return gi(kl, 0);
            }

            public String exp() {
                return gs(ke, "num * distance * rate");
            }

            private String gs(String k, String def) {
                if (holder == null) return def;
                String v = holder.get(k);
                return v != null ? v : def;
            }

            private int gi(String k, int def) {
                if (holder == null) return def;
                Integer v = holder.get(k);
                return v != null ? v : def;
            }

            private double gd(String k, double def) {
                if (holder == null) return def;
                Double v = holder.get(k);
                return v != null ? v : def;
            }
        }

        public static final class Cost {
            private final ConfigHolder holder;

            Cost(ConfigHolder holder) {
                this.holder = holder;
            }

            public CostGroup tpCoordinate() {
                return new CostGroup(holder, Key.CT_COSTTPCOORDINATE_TYPE, Key.CT_COSTTPCOORDINATE_NUM, Key.CT_COSTTPCOORDINATE_CONF, Key.CT_COSTTPCOORDINATE_RATE, Key.CT_COSTTPCOORDINATE_NUM_UPPER, Key.CT_COSTTPCOORDINATE_NUM_LOWER, Key.CT_COSTTPCOORDINATE_EXP);
            }

            public CostGroup tpStructure() {
                return new CostGroup(holder, Key.CT_COSTTPSTRUCTURE_TYPE, Key.CT_COSTTPSTRUCTURE_NUM, Key.CT_COSTTPSTRUCTURE_CONF, Key.CT_COSTTPSTRUCTURE_RATE, Key.CT_COSTTPSTRUCTURE_NUM_UPPER, Key.CT_COSTTPSTRUCTURE_NUM_LOWER, Key.CT_COSTTPSTRUCTURE_EXP);
            }

            public CostGroup tpAsk() {
                return new CostGroup(holder, Key.CT_COSTTPASK_TYPE, Key.CT_COSTTPASK_NUM, Key.CT_COSTTPASK_CONF, Key.CT_COSTTPASK_RATE, Key.CT_COSTTPASK_NUM_UPPER, Key.CT_COSTTPASK_NUM_LOWER, Key.CT_COSTTPASK_EXP);
            }

            public CostGroup tpHere() {
                return new CostGroup(holder, Key.CT_COSTTPHERE_TYPE, Key.CT_COSTTPHERE_NUM, Key.CT_COSTTPHERE_CONF, Key.CT_COSTTPHERE_RATE, Key.CT_COSTTPHERE_NUM_UPPER, Key.CT_COSTTPHERE_NUM_LOWER, Key.CT_COSTTPHERE_EXP);
            }

            public CostGroup tpRandom() {
                return new CostGroup(holder, Key.CT_COSTTPRANDOM_TYPE, Key.CT_COSTTPRANDOM_NUM, Key.CT_COSTTPRANDOM_CONF, Key.CT_COSTTPRANDOM_RATE, Key.CT_COSTTPRANDOM_NUM_UPPER, Key.CT_COSTTPRANDOM_NUM_LOWER, Key.CT_COSTTPRANDOM_EXP);
            }

            public CostGroup tpSpawn() {
                return new CostGroup(holder, Key.CT_COSTTPSPAWN_TYPE, Key.CT_COSTTPSPAWN_NUM, Key.CT_COSTTPSPAWN_CONF, Key.CT_COSTTPSPAWN_RATE, Key.CT_COSTTPSPAWN_NUM_UPPER, Key.CT_COSTTPSPAWN_NUM_LOWER, Key.CT_COSTTPSPAWN_EXP);
            }

            public CostGroup tpWorldSpawn() {
                return new CostGroup(holder, Key.CT_COSTTPWORLDSPAWN_TYPE, Key.CT_COSTTPWORLDSPAWN_NUM, Key.CT_COSTTPWORLDSPAWN_CONF, Key.CT_COSTTPWORLDSPAWN_RATE, Key.CT_COSTTPWORLDSPAWN_NUM_UPPER, Key.CT_COSTTPWORLDSPAWN_NUM_LOWER, Key.CT_COSTTPWORLDSPAWN_EXP);
            }

            public CostGroup tpTop() {
                return new CostGroup(holder, Key.CT_COSTTPTOP_TYPE, Key.CT_COSTTPTOP_NUM, Key.CT_COSTTPTOP_CONF, Key.CT_COSTTPTOP_RATE, Key.CT_COSTTPTOP_NUM_UPPER, Key.CT_COSTTPTOP_NUM_LOWER, Key.CT_COSTTPTOP_EXP);
            }

            public CostGroup tpBottom() {
                return new CostGroup(holder, Key.CT_COSTTPBOTTOM_TYPE, Key.CT_COSTTPBOTTOM_NUM, Key.CT_COSTTPBOTTOM_CONF, Key.CT_COSTTPBOTTOM_RATE, Key.CT_COSTTPBOTTOM_NUM_UPPER, Key.CT_COSTTPBOTTOM_NUM_LOWER, Key.CT_COSTTPBOTTOM_EXP);
            }

            public CostGroup tpUp() {
                return new CostGroup(holder, Key.CT_COSTTPUP_TYPE, Key.CT_COSTTPUP_NUM, Key.CT_COSTTPUP_CONF, Key.CT_COSTTPUP_RATE, Key.CT_COSTTPUP_NUM_UPPER, Key.CT_COSTTPUP_NUM_LOWER, Key.CT_COSTTPUP_EXP);
            }

            public CostGroup tpDown() {
                return new CostGroup(holder, Key.CT_COSTTPDOWN_TYPE, Key.CT_COSTTPDOWN_NUM, Key.CT_COSTTPDOWN_CONF, Key.CT_COSTTPDOWN_RATE, Key.CT_COSTTPDOWN_NUM_UPPER, Key.CT_COSTTPDOWN_NUM_LOWER, Key.CT_COSTTPDOWN_EXP);
            }

            public CostGroup tpView() {
                return new CostGroup(holder, Key.CT_COSTTPVIEW_TYPE, Key.CT_COSTTPVIEW_NUM, Key.CT_COSTTPVIEW_CONF, Key.CT_COSTTPVIEW_RATE, Key.CT_COSTTPVIEW_NUM_UPPER, Key.CT_COSTTPVIEW_NUM_LOWER, Key.CT_COSTTPVIEW_EXP);
            }

            public CostGroup tpHome() {
                return new CostGroup(holder, Key.CT_COSTTPHOME_TYPE, Key.CT_COSTTPHOME_NUM, Key.CT_COSTTPHOME_CONF, Key.CT_COSTTPHOME_RATE, Key.CT_COSTTPHOME_NUM_UPPER, Key.CT_COSTTPHOME_NUM_LOWER, Key.CT_COSTTPHOME_EXP);
            }

            public CostGroup tpStage() {
                return new CostGroup(holder, Key.CT_COSTTPSTAGE_TYPE, Key.CT_COSTTPSTAGE_NUM, Key.CT_COSTTPSTAGE_CONF, Key.CT_COSTTPSTAGE_RATE, Key.CT_COSTTPSTAGE_NUM_UPPER, Key.CT_COSTTPSTAGE_NUM_LOWER, Key.CT_COSTTPSTAGE_EXP);
            }

            public CostGroup tpBack() {
                return new CostGroup(holder, Key.CT_COSTTPBACK_TYPE, Key.CT_COSTTPBACK_NUM, Key.CT_COSTTPBACK_CONF, Key.CT_COSTTPBACK_RATE, Key.CT_COSTTPBACK_NUM_UPPER, Key.CT_COSTTPBACK_NUM_LOWER, Key.CT_COSTTPBACK_EXP);
            }

            public CostGroup tpGrave() {
                return new CostGroup(holder, Key.CT_COSTTPGRAVE_TYPE, Key.CT_COSTTPGRAVE_NUM, Key.CT_COSTTPGRAVE_CONF, Key.CT_COSTTPGRAVE_RATE, Key.CT_COSTTPGRAVE_NUM_UPPER, Key.CT_COSTTPGRAVE_NUM_LOWER, Key.CT_COSTTPGRAVE_EXP);
            }
        }

        public static void resetConfig() {
            ConfigHolder h = CommonConfig.get().holder();
            if (h == null) return;
            h.set(Key.SG_TELEPORT_RECORD_LIMIT, 100);
            h.set(Key.SG_TELEPORT_BACK_SKIP_TYPE, new ArrayList<String>() {{
                add(EnumTeleportType.TP_BACK.name());
            }});
            h.set(Key.SG_TELEPORT_ACROSS_DIMENSION, true);
            h.set(Key.SG_TELEPORT_COST_DISTANCE_LIMIT, 10000);
            h.set(Key.SG_TELEPORT_COST_DISTANCE_ACROSS_DIMENSION, 10000);
            h.set(Key.SG_TELEPORT_VIEW_DISTANCE_LIMIT, 16 * 64);
            h.set(Key.SG_TELEPORT_REQUEST_EXPIRE_TIME, 60);
            h.set(Key.SG_TELEPORT_REQUEST_COOLDOWN_TYPE, EnumCoolDownType.INDIVIDUAL);
            h.set(Key.SG_TELEPORT_REQUEST_COOLDOWN, 10);
            h.set(Key.SG_TELEPORT_RANDOM_DISTANCE_LIMIT, 10000);
            h.set(Key.SG_GRAVE_SEARCH_RANGE_LIMIT, 32);
            h.set(Key.SG_TELEPORT_HOME_LIMIT, 5);
            h.set(Key.SG_HELP_HEADER, "-----==== Narcissus Farewell Help (%d/%d) ====-----");
            h.set(Key.SG_TP_SOUND, SoundEvents.ENDERMAN_TELEPORT.getRegistryName().toString());
            h.set(Key.SG_TP_WITH_VEHICLE, true);
            h.set(Key.SG_TP_WITH_FOLLOWER, true);
            h.set(Key.SG_TP_WITH_FOLLOWER_RANGE, 10);
            h.set(Key.SG_HELP_INFO_NUM_PER_PAGE, 5);
            h.set(Key.SG_DEFAULT_LANGUAGE, "en_us");
            h.set(Key.SG_TP_WITH_ENEMY, false);
            h.set(Key.SG_UNSAFE_BLOCKS, Stream.of(Blocks.LAVA, Blocks.FIRE, Blocks.CAMPFIRE, Blocks.SOUL_FIRE, Blocks.SOUL_CAMPFIRE, Blocks.CACTUS, Blocks.MAGMA_BLOCK, Blocks.SWEET_BERRY_BUSH).map(b -> {
                ResourceLocation rl = b.getRegistryName();
                return rl == null ? "" : rl.toString();
            }).collect(Collectors.toList()));
            h.set(Key.SG_SUFFOCATING_BLOCKS, Stream.of(Blocks.LAVA, Blocks.WATER).map(b -> {
                ResourceLocation rl = b.getRegistryName();
                return rl == null ? "" : rl.toString();
            }).collect(Collectors.toList()));
            h.set(Key.SG_SET_BLOCK_WHEN_SAFE_NOT_FOUND, false);
            h.set(Key.SG_GET_BLOCK_FROM_INVENTORY, true);
            h.set(Key.SG_SAFE_BLOCKS, Stream.of(Blocks.GRASS_BLOCK, Blocks.GRASS_PATH, Blocks.DIRT, Blocks.COBBLESTONE).map(b -> {
                ResourceLocation rl = b.getRegistryName();
                return rl == null ? "" : rl.toString();
            }).collect(Collectors.toList()));
            h.set(Key.SG_SAFE_CHUNK_RANGE, 1);
            h.set(Key.SP_CMD_PERMISSIONFEEDOTHER, 2);
            h.set(Key.SP_CMD_PERMISSIONTPCOORDINATE, 2);
            h.set(Key.SP_CMD_PERMISSIONTPSTRUCTURE, 2);
            h.set(Key.SP_CMD_PERMISSIONTPASK, 0);
            h.set(Key.SP_CMD_PERMISSIONTPHERE, 0);
            h.set(Key.SP_CMD_PERMISSIONTPRANDOM, 1);
            h.set(Key.SP_CMD_PERMISSIONTPSPAWN, 0);
            h.set(Key.SP_CMD_PERMISSIONTPSPAWNOTHER, 2);
            h.set(Key.SP_CMD_PERMISSIONTPWORLDSPAWN, 0);
            h.set(Key.SP_CMD_PERMISSIONTPTOP, 1);
            h.set(Key.SP_CMD_PERMISSIONTPBOTTOM, 1);
            h.set(Key.SP_CMD_PERMISSIONTPUP, 1);
            h.set(Key.SP_CMD_PERMISSIONTPDOWN, 1);
            h.set(Key.SP_CMD_PERMISSIONTPVIEW, 1);
            h.set(Key.SP_CMD_PERMISSIONTPHOME, 0);
            h.set(Key.SP_CMD_PERMISSIONTPSTAGE, 0);
            h.set(Key.SP_CMD_PERMISSIONTPSTAGESET, 2);
            h.set(Key.SP_CMD_PERMISSIONTPSTAGEDEL, 2);
            h.set(Key.SP_CMD_PERMISSIONTPSTAGEGET, 0);
            h.set(Key.SP_CMD_PERMISSIONTPBACK, 0);
            h.set(Key.SP_CMD_PERMISSIONTPGRAVE, 0);
            h.set(Key.SP_CMD_PERMISSIONFLY, 2);
            h.set(Key.SP_CMD_PERMISSIONVIRTUALOP, 4);
            h.set(Key.SP_CMD_PERMISSIONSETCARD, 2);
            h.set(Key.SP_ACR_PERMISSIONTPCOORDINATEACROSSDIMENSION, 2);
            h.set(Key.SP_ACR_PERMISSIONTPSTRUCTUREACROSSDIMENSION, 2);
            h.set(Key.SP_ACR_PERMISSIONTPASKACROSSDIMENSION, 0);
            h.set(Key.SP_ACR_PERMISSIONTPHEREACROSSDIMENSION, 0);
            h.set(Key.SP_ACR_PERMISSIONTPRANDOMACROSSDIMENSION, 0);
            h.set(Key.SP_ACR_PERMISSIONTPSPAWNACROSSDIMENSION, 0);
            h.set(Key.SP_ACR_PERMISSIONTPWORLDSPAWNACROSSDIMENSION, 0);
            h.set(Key.SP_ACR_PERMISSIONTPHOMEACROSSDIMENSION, 0);
            h.set(Key.SP_ACR_PERMISSIONTPSTAGEACROSSDIMENSION, 0);
            h.set(Key.SP_ACR_PERMISSIONTPBACKACROSSDIMENSION, 0);
            h.set(Key.SP_ACR_PERMISSIONTPGRAVEACROSSDIMENSION, 0);
            h.set(Key.CD_COOLDOWNTPCOORDINATE, 10);
            h.set(Key.CD_COOLDOWNTPSTRUCTURE, 10);
            h.set(Key.CD_COOLDOWNTPASK, 10);
            h.set(Key.CD_COOLDOWNTPHERE, 10);
            h.set(Key.CD_COOLDOWNTPRANDOM, 10);
            h.set(Key.CD_COOLDOWNTPSPAWN, 10);
            h.set(Key.CD_COOLDOWNTPWORLDSPAWN, 10);
            h.set(Key.CD_COOLDOWNTPTOP, 10);
            h.set(Key.CD_COOLDOWNTPBOTTOM, 10);
            h.set(Key.CD_COOLDOWNTPUP, 10);
            h.set(Key.CD_COOLDOWNTPDOWN, 10);
            h.set(Key.CD_COOLDOWNTPVIEW, 10);
            h.set(Key.CD_COOLDOWNTPHOME, 10);
            h.set(Key.CD_COOLDOWNTPSTAGE, 10);
            h.set(Key.CD_COOLDOWNTPBACK, 10);
            h.set(Key.CD_COOLDOWNTPGRAVE, 10);
            h.set(Key.CT_COSTTPCOORDINATE_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPCOORDINATE_NUM, 1);
            h.set(Key.CT_COSTTPCOORDINATE_CONF, "");
            h.set(Key.CT_COSTTPCOORDINATE_RATE, 0.002);
            h.set(Key.CT_COSTTPCOORDINATE_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPCOORDINATE_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPCOORDINATE_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPSTRUCTURE_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPSTRUCTURE_NUM, 1);
            h.set(Key.CT_COSTTPSTRUCTURE_CONF, "");
            h.set(Key.CT_COSTTPSTRUCTURE_RATE, 0.002);
            h.set(Key.CT_COSTTPSTRUCTURE_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPSTRUCTURE_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPSTRUCTURE_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPASK_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPASK_NUM, 1);
            h.set(Key.CT_COSTTPASK_CONF, "");
            h.set(Key.CT_COSTTPASK_RATE, 0.002);
            h.set(Key.CT_COSTTPASK_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPASK_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPASK_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPHERE_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPHERE_NUM, 1);
            h.set(Key.CT_COSTTPHERE_CONF, "");
            h.set(Key.CT_COSTTPHERE_RATE, 0.002);
            h.set(Key.CT_COSTTPHERE_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPHERE_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPHERE_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPRANDOM_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPRANDOM_NUM, 1);
            h.set(Key.CT_COSTTPRANDOM_CONF, "");
            h.set(Key.CT_COSTTPRANDOM_RATE, 0.002);
            h.set(Key.CT_COSTTPRANDOM_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPRANDOM_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPRANDOM_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPSPAWN_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPSPAWN_NUM, 1);
            h.set(Key.CT_COSTTPSPAWN_CONF, "");
            h.set(Key.CT_COSTTPSPAWN_RATE, 0.002);
            h.set(Key.CT_COSTTPSPAWN_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPSPAWN_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPSPAWN_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPWORLDSPAWN_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPWORLDSPAWN_NUM, 1);
            h.set(Key.CT_COSTTPWORLDSPAWN_CONF, "");
            h.set(Key.CT_COSTTPWORLDSPAWN_RATE, 0.002);
            h.set(Key.CT_COSTTPWORLDSPAWN_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPWORLDSPAWN_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPWORLDSPAWN_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPTOP_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPTOP_NUM, 1);
            h.set(Key.CT_COSTTPTOP_CONF, "");
            h.set(Key.CT_COSTTPTOP_RATE, 0.002);
            h.set(Key.CT_COSTTPTOP_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPTOP_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPTOP_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPBOTTOM_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPBOTTOM_NUM, 1);
            h.set(Key.CT_COSTTPBOTTOM_CONF, "");
            h.set(Key.CT_COSTTPBOTTOM_RATE, 0.002);
            h.set(Key.CT_COSTTPBOTTOM_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPBOTTOM_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPBOTTOM_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPUP_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPUP_NUM, 1);
            h.set(Key.CT_COSTTPUP_CONF, "");
            h.set(Key.CT_COSTTPUP_RATE, 0.002);
            h.set(Key.CT_COSTTPUP_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPUP_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPUP_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPDOWN_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPDOWN_NUM, 1);
            h.set(Key.CT_COSTTPDOWN_CONF, "");
            h.set(Key.CT_COSTTPDOWN_RATE, 0.002);
            h.set(Key.CT_COSTTPDOWN_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPDOWN_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPDOWN_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPVIEW_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPVIEW_NUM, 1);
            h.set(Key.CT_COSTTPVIEW_CONF, "");
            h.set(Key.CT_COSTTPVIEW_RATE, 0.002);
            h.set(Key.CT_COSTTPVIEW_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPVIEW_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPVIEW_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPHOME_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPHOME_NUM, 1);
            h.set(Key.CT_COSTTPHOME_CONF, "");
            h.set(Key.CT_COSTTPHOME_RATE, 0.002);
            h.set(Key.CT_COSTTPHOME_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPHOME_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPHOME_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPSTAGE_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPSTAGE_NUM, 1);
            h.set(Key.CT_COSTTPSTAGE_CONF, "");
            h.set(Key.CT_COSTTPSTAGE_RATE, 0.002);
            h.set(Key.CT_COSTTPSTAGE_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPSTAGE_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPSTAGE_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPBACK_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPBACK_NUM, 1);
            h.set(Key.CT_COSTTPBACK_CONF, "");
            h.set(Key.CT_COSTTPBACK_RATE, 0.002);
            h.set(Key.CT_COSTTPBACK_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPBACK_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPBACK_EXP, "num * distance * rate");
            h.set(Key.CT_COSTTPGRAVE_TYPE, EnumCostType.NONE);
            h.set(Key.CT_COSTTPGRAVE_NUM, 1);
            h.set(Key.CT_COSTTPGRAVE_CONF, "");
            h.set(Key.CT_COSTTPGRAVE_RATE, 0.002);
            h.set(Key.CT_COSTTPGRAVE_NUM_UPPER, 20);
            h.set(Key.CT_COSTTPGRAVE_NUM_LOWER, 0);
            h.set(Key.CT_COSTTPGRAVE_EXP, "num * distance * rate");
            h.save();
        }

        public static void resetConfigWithMode1() {
            resetConfig();
            ConfigHolder h = CommonConfig.get().holder();
            if (h == null) return;
            h.set(Key.SG_TELEPORT_BACK_SKIP_TYPE, new ArrayList<>());
            h.save();
        }

        public static void resetConfigWithMode2() {
            resetConfigWithMode1();
            CommonConfig.get().save();
        }

        public static void resetConfigWithMode3() {
            resetConfig();
            ConfigHolder h = CommonConfig.get().holder();
            if (h == null) return;
            h.set(Key.CT_COSTTPCOORDINATE_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPSTRUCTURE_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPASK_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPHERE_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPRANDOM_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPSPAWN_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPWORLDSPAWN_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPTOP_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPBOTTOM_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPUP_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPDOWN_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPVIEW_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPHOME_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPSTAGE_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPBACK_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPGRAVE_TYPE, EnumCostType.EXP_POINT);
            h.set(Key.CT_COSTTPGRAVE_TYPE, EnumCostType.HUNGER);
            h.save();
        }

    }

    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class ServerCategory {
        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "基础设置", en_us = "Base Settings")
        private Server.ServerGeneralCategory general = new Server.ServerGeneralCategory();

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "指令权限", en_us = "Command Permission")
        private Server.ServerPermissionCategory permission = new Server.ServerPermissionCategory();

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "冷却时间", en_us = "Cooldown Time")
        private Server.ServerCooldownCategory cooldown = new Server.ServerCooldownCategory();

        @Getter(AccessLevel.NONE)
        @Setter(AccessLevel.NONE)
        @ConfigEntry.Gui.CollapsibleObject
        @ConfigEntry.Gui.Tooltip(zh_cn = "传送代价", en_us = "Teleport Cost")
        private Server.ServerCostCategory cost = new Server.ServerCostCategory();
    }

    public static final class ServerRoot {
        private final Server.General general;
        private final Server.Permission permission;
        private final Server.Cooldown cooldown;
        private final Server.Cost cost;

        ServerRoot(ConfigHolder holder) {
            this.general = new Server.General(holder);
            this.permission = new Server.Permission(holder);
            this.cooldown = new Server.Cooldown(holder);
            this.cost = new Server.Cost(holder);
        }

        public Server.General general() {
            return general;
        }

        public Server.Permission permission() {
            return permission;
        }

        public Server.Cooldown cooldown() {
            return cooldown;
        }

        public Server.Cost cost() {
            return cost;
        }
    }

    // endregion 服务端配置
}
