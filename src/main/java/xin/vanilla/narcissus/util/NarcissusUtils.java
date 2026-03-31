package xin.vanilla.narcissus.util;

import lombok.NonNull;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.BaniraCodex;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.narcissus.Identifier;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.TeleportCountdownHelper;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportCost;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumCardType;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.mixin.LivingEntityInvoker;
import xin.vanilla.narcissus.mixin.TemptGoalAccessor;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@SuppressWarnings("resource")
public class NarcissusUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    // region 指令相关

    public static String getCommandPrefix() {
        String commandPrefix = CommonConfig.get().commandNames().commandPrefix();
        if (StringUtils.isNullOrEmptyEx(commandPrefix) || !commandPrefix.matches("^(\\w ?)+$")) {
            CommonConfig.get().commandNames().commandPrefix(NarcissusFarewell.DEFAULT_COMMAND_PREFIX);
        }
        return CommonConfig.get().commandNames().commandPrefix().trim();
    }

    /**
     * 判断指令类型是否开启
     *
     * @param type 指令类型
     */
    public static boolean isCommandEnabled(EnumCommandType type) {
        return switch (type) {
            case CARD, SET_CARD, CARD_CONCISE, SET_CARD_CONCISE -> CommonConfig.get().base().teleportCard();
            case SHARE, SHARE_CONCISE -> CommonConfig.get().featureSwitch().switchShare();
            case FEED, FEED_OTHER, FEED_CONCISE, FEED_OTHER_CONCISE -> CommonConfig.get().featureSwitch().switchFeed();
            case TP_COORDINATE, TP_COORDINATE_CONCISE -> CommonConfig.get().featureSwitch().switchTpCoordinate();
            case TP_STRUCTURE, TP_STRUCTURE_CONCISE -> CommonConfig.get().featureSwitch().switchTpStructure();
            case TP_ASK, TP_ASK_YES, TP_ASK_NO, TP_ASK_CANCEL, TP_ASK_CONCISE, TP_ASK_YES_CONCISE, TP_ASK_NO_CONCISE,
                 TP_ASK_CANCEL_CONCISE -> CommonConfig.get().featureSwitch().switchTpAsk();
            case TP_HERE, TP_HERE_YES, TP_HERE_NO, TP_HERE_CANCEL, TP_HERE_CONCISE, TP_HERE_YES_CONCISE,
                 TP_HERE_NO_CONCISE, TP_HERE_CANCEL_CONCISE -> CommonConfig.get().featureSwitch().switchTpHere();
            case TP_RANDOM, TP_RANDOM_CONCISE -> CommonConfig.get().featureSwitch().switchTpRandom();
            case TP_SPAWN, TP_SPAWN_OTHER, TP_SPAWN_CONCISE, TP_SPAWN_OTHER_CONCISE ->
                    CommonConfig.get().featureSwitch().switchTpSpawn();
            case TP_WORLD_SPAWN, TP_WORLD_SPAWN_CONCISE -> CommonConfig.get().featureSwitch().switchTpWorldSpawn();
            case TP_TOP, TP_TOP_CONCISE -> CommonConfig.get().featureSwitch().switchTpTop();
            case TP_BOTTOM, TP_BOTTOM_CONCISE -> CommonConfig.get().featureSwitch().switchTpBottom();
            case TP_UP, TP_UP_CONCISE -> CommonConfig.get().featureSwitch().switchTpUp();
            case TP_DOWN, TP_DOWN_CONCISE -> CommonConfig.get().featureSwitch().switchTpDown();
            case TP_VIEW, TP_VIEW_CONCISE -> CommonConfig.get().featureSwitch().switchTpView();
            case TP_HOME, SET_HOME, DEL_HOME, GET_HOME, TP_HOME_CONCISE, SET_HOME_CONCISE, DEL_HOME_CONCISE,
                 GET_HOME_CONCISE -> CommonConfig.get().featureSwitch().switchTpHome();
            case TP_STAGE, SET_STAGE, DEL_STAGE, GET_STAGE, TP_STAGE_CONCISE, SET_STAGE_CONCISE, DEL_STAGE_CONCISE,
                 GET_STAGE_CONCISE -> CommonConfig.get().featureSwitch().switchTpStage();
            case TP_BACK, TP_BACK_CONCISE -> CommonConfig.get().featureSwitch().switchTpBack();
            case TP_GRAVE, TP_GRAVE_CONCISE -> CommonConfig.get().featureSwitch().switchTpGrave();
            case FLY, FLY_CONCISE -> CommonConfig.get().featureSwitch().switchFly();
            default -> true;
        };
    }

    public static String getCommand(EnumTeleportType type) {
        return switch (type) {
            case TP_COORDINATE -> CommonConfig.get().commandNames().commandTpCoordinate();
            case TP_STRUCTURE -> CommonConfig.get().commandNames().commandTpStructure();
            case TP_ASK -> CommonConfig.get().commandNames().commandTpAsk();
            case TP_HERE -> CommonConfig.get().commandNames().commandTpHere();
            case TP_RANDOM -> CommonConfig.get().commandNames().commandTpRandom();
            case TP_SPAWN -> CommonConfig.get().commandNames().commandTpSpawn();
            case TP_WORLD_SPAWN -> CommonConfig.get().commandNames().commandTpWorldSpawn();
            case TP_TOP -> CommonConfig.get().commandNames().commandTpTop();
            case TP_BOTTOM -> CommonConfig.get().commandNames().commandTpBottom();
            case TP_UP -> CommonConfig.get().commandNames().commandTpUp();
            case TP_DOWN -> CommonConfig.get().commandNames().commandTpDown();
            case TP_VIEW -> CommonConfig.get().commandNames().commandTpView();
            case TP_HOME -> CommonConfig.get().commandNames().commandTpHome();
            case TP_STAGE -> CommonConfig.get().commandNames().commandTpStage();
            case TP_BACK -> CommonConfig.get().commandNames().commandTpBack();
            case TP_GRAVE -> CommonConfig.get().commandNames().commandTpGrave();
            default -> "";
        };
    }

    public static String getCommand(EnumCommandType type) {
        String prefix = NarcissusUtils.getCommandPrefix();
        switch (type) {
            case HELP:
                return prefix + " help";
            case DIMENSION:
                return prefix + " " + CommonConfig.get().commandNames().commandDimension();
            case DIMENSION_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandDimension() : "";
            case UUID:
                return prefix + " " + CommonConfig.get().commandNames().commandUuid();
            case UUID_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandUuid() : "";
            case CARD:
            case SET_CARD:
                return prefix + " " + CommonConfig.get().commandNames().commandCard();
            case CARD_CONCISE:
            case SET_CARD_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandCard() : "";
            case SHARE:
                return prefix + " " + CommonConfig.get().commandNames().commandShare();
            case SHARE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandShare() : "";
            case FEED:
            case FEED_OTHER:
                return prefix + " " + CommonConfig.get().commandNames().commandFeed();
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandFeed() : "";
            case TP_COORDINATE:
                return prefix + " " + CommonConfig.get().commandNames().commandTpCoordinate();
            case TP_COORDINATE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpCoordinate() : "";
            case TP_STRUCTURE:
                return prefix + " " + CommonConfig.get().commandNames().commandTpStructure();
            case TP_STRUCTURE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpStructure() : "";
            case TP_ASK:
                return prefix + " " + CommonConfig.get().commandNames().commandTpAsk();
            case TP_ASK_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpAsk() : "";
            case TP_ASK_YES:
                return prefix + " " + CommonConfig.get().commandNames().commandTpAskYes();
            case TP_ASK_YES_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpAskYes() : "";
            case TP_ASK_NO:
                return prefix + " " + CommonConfig.get().commandNames().commandTpAskNo();
            case TP_ASK_NO_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpAskNo() : "";
            case TP_ASK_CANCEL:
                return prefix + " " + CommonConfig.get().commandNames().commandTpAskCancel();
            case TP_ASK_CANCEL_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpAskCancel() : "";
            case TP_HERE:
                return prefix + " " + CommonConfig.get().commandNames().commandTpHere();
            case TP_HERE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpHere() : "";
            case TP_HERE_YES:
                return prefix + " " + CommonConfig.get().commandNames().commandTpHereYes();
            case TP_HERE_YES_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpHereYes() : "";
            case TP_HERE_NO:
                return prefix + " " + CommonConfig.get().commandNames().commandTpHereNo();
            case TP_HERE_NO_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpHereNo() : "";
            case TP_HERE_CANCEL:
                return prefix + " " + CommonConfig.get().commandNames().commandTpHereCancel();
            case TP_HERE_CANCEL_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpHereCancel() : "";
            case TP_RANDOM:
                return prefix + " " + CommonConfig.get().commandNames().commandTpRandom();
            case TP_RANDOM_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpRandom() : "";
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
                return prefix + " " + CommonConfig.get().commandNames().commandTpSpawn();
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpSpawn() : "";
            case TP_WORLD_SPAWN:
                return prefix + " " + CommonConfig.get().commandNames().commandTpWorldSpawn();
            case TP_WORLD_SPAWN_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpWorldSpawn() : "";
            case TP_TOP:
                return prefix + " " + CommonConfig.get().commandNames().commandTpTop();
            case TP_TOP_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpTop() : "";
            case TP_BOTTOM:
                return prefix + " " + CommonConfig.get().commandNames().commandTpBottom();
            case TP_BOTTOM_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpBottom() : "";
            case TP_UP:
                return prefix + " " + CommonConfig.get().commandNames().commandTpUp();
            case TP_UP_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpUp() : "";
            case TP_DOWN:
                return prefix + " " + CommonConfig.get().commandNames().commandTpDown();
            case TP_DOWN_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpDown() : "";
            case TP_VIEW:
                return prefix + " " + CommonConfig.get().commandNames().commandTpView();
            case TP_VIEW_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpView() : "";
            case TP_HOME:
                return prefix + " " + CommonConfig.get().commandNames().commandTpHome();
            case TP_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpHome() : "";
            case SET_HOME:
                return prefix + " " + CommonConfig.get().commandNames().commandSetHome();
            case SET_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandSetHome() : "";
            case DEL_HOME:
                return prefix + " " + CommonConfig.get().commandNames().commandDelHome();
            case DEL_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandDelHome() : "";
            case GET_HOME:
                return prefix + " " + CommonConfig.get().commandNames().commandGetHome();
            case GET_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandGetHome() : "";
            case TP_STAGE:
                return prefix + " " + CommonConfig.get().commandNames().commandTpStage();
            case TP_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpStage() : "";
            case SET_STAGE:
                return prefix + " " + CommonConfig.get().commandNames().commandSetStage();
            case SET_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandSetStage() : "";
            case DEL_STAGE:
                return prefix + " " + CommonConfig.get().commandNames().commandDelStage();
            case DEL_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandDelStage() : "";
            case GET_STAGE:
                return prefix + " " + CommonConfig.get().commandNames().commandGetStage();
            case GET_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandGetStage() : "";
            case TP_BACK:
                return prefix + " " + CommonConfig.get().commandNames().commandTpBack();
            case TP_BACK_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpBack() : "";
            case TP_GRAVE:
                return prefix + " " + CommonConfig.get().commandNames().commandTpGrave();
            case TP_GRAVE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandTpGrave() : "";
            case FLY:
                return prefix + " " + CommonConfig.get().commandNames().commandFly();
            case FLY_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().commandNames().commandFly() : "";
            case CONFIG:
                return prefix + " config";
            case BLACKLIST:
                return prefix + " config black";
            case WHITELIST:
                return prefix + " config white";
            default:
                return "";
        }
    }

    public static int getCommandPermissionLevel(EnumCommandType type) {
        switch (type) {
            case SET_CARD:
            case SET_CARD_CONCISE:
                return CommonConfig.get().permission().permissionSetCard();
            case FEED_OTHER:
            case FEED_OTHER_CONCISE:
                return CommonConfig.get().permission().permissionFeedOther();
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return CommonConfig.get().permission().permissionTpCoordinate();
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return CommonConfig.get().permission().permissionTpStructure();
            case TP_ASK:
            case TP_ASK_CANCEL:
                // case TP_ASK_YES:
                // case TP_ASK_NO:
            case TP_ASK_CONCISE:
            case TP_ASK_CANCEL_CONCISE:
                // case TP_ASK_YES_CONCISE:
                // case TP_ASK_NO_CONCISE:
                return CommonConfig.get().permission().permissionTpAsk();
            case TP_HERE:
            case TP_HERE_CANCEL:
                // case TP_HERE_YES:
                // case TP_HERE_NO:
            case TP_HERE_CONCISE:
            case TP_HERE_CANCEL_CONCISE:
                // case TP_HERE_YES_CONCISE:
                // case TP_HERE_NO_CONCISE:
                return CommonConfig.get().permission().permissionTpHere();
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return CommonConfig.get().permission().permissionTpRandom();
            case TP_SPAWN:
            case TP_SPAWN_CONCISE:
                return CommonConfig.get().permission().permissionTpSpawn();
            case TP_SPAWN_OTHER:
            case TP_SPAWN_OTHER_CONCISE:
                return CommonConfig.get().permission().permissionTpSpawnOther();
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return CommonConfig.get().permission().permissionTpWorldSpawn();
            case TP_TOP:
            case TP_TOP_CONCISE:
                return CommonConfig.get().permission().permissionTpTop();
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return CommonConfig.get().permission().permissionTpBottom();
            case TP_UP:
            case TP_UP_CONCISE:
                return CommonConfig.get().permission().permissionTpUp();
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return CommonConfig.get().permission().permissionTpDown();
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return CommonConfig.get().permission().permissionTpView();
            case TP_HOME:
            case SET_HOME:
            case DEL_HOME:
            case GET_HOME:
            case TP_HOME_CONCISE:
            case SET_HOME_CONCISE:
            case DEL_HOME_CONCISE:
            case GET_HOME_CONCISE:
                return CommonConfig.get().permission().permissionTpHome();
            case TP_STAGE:
            case TP_STAGE_CONCISE:
                return CommonConfig.get().permission().permissionTpStage();
            case SET_STAGE:
            case SET_STAGE_CONCISE:
                return CommonConfig.get().permission().permissionTpStageSet();
            case DEL_STAGE:
            case DEL_STAGE_CONCISE:
                return CommonConfig.get().permission().permissionTpStageDel();
            case GET_STAGE:
            case GET_STAGE_CONCISE:
                return CommonConfig.get().permission().permissionTpStageGet();
            case TP_BACK:
            case TP_BACK_CONCISE:
                return CommonConfig.get().permission().permissionTpBack();
            case TP_GRAVE:
            case TP_GRAVE_CONCISE:
                return CommonConfig.get().permission().permissionTpGrave();
            case FLY:
            case FLY_CONCISE:
                return CommonConfig.get().permission().permissionFly();
            case VIRTUAL_OP:
            case VIRTUAL_OP_CONCISE:
                return CommonConfig.get().permission().permissionVirtualOp();
            default:
                return 0;
        }
    }

    public static int getCommandPermissionLevel(EnumTeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return CommonConfig.get().permission().permissionTpCoordinate();
            case TP_STRUCTURE:
                return CommonConfig.get().permission().permissionTpStructure();
            case TP_ASK:
                return CommonConfig.get().permission().permissionTpAsk();
            case TP_HERE:
                return CommonConfig.get().permission().permissionTpHere();
            case TP_RANDOM:
                return CommonConfig.get().permission().permissionTpRandom();
            case TP_SPAWN:
                return CommonConfig.get().permission().permissionTpSpawn();
            case TP_WORLD_SPAWN:
                return CommonConfig.get().permission().permissionTpWorldSpawn();
            case TP_TOP:
                return CommonConfig.get().permission().permissionTpTop();
            case TP_BOTTOM:
                return CommonConfig.get().permission().permissionTpBottom();
            case TP_UP:
                return CommonConfig.get().permission().permissionTpUp();
            case TP_DOWN:
                return CommonConfig.get().permission().permissionTpDown();
            case TP_VIEW:
                return CommonConfig.get().permission().permissionTpView();
            case TP_HOME:
                return CommonConfig.get().permission().permissionTpHome();
            case TP_STAGE:
                return CommonConfig.get().permission().permissionTpStage();
            case TP_BACK:
                return CommonConfig.get().permission().permissionTpBack();
            default:
                return 0;
        }
    }

    public static boolean isConciseEnabled(EnumCommandType type) {
        return switch (type) {
            case LANGUAGE, LANGUAGE_CONCISE -> CommonConfig.get().concise().conciseLanguage();
            case UUID, UUID_CONCISE -> CommonConfig.get().concise().conciseUuid();
            case DIMENSION, DIMENSION_CONCISE -> CommonConfig.get().concise().conciseDimension();
            case CARD, CARD_CONCISE, SET_CARD, SET_CARD_CONCISE -> CommonConfig.get().concise().conciseCard();
            case SHARE, SHARE_CONCISE -> CommonConfig.get().concise().conciseShare();
            case FEED, FEED_OTHER, FEED_CONCISE, FEED_OTHER_CONCISE -> CommonConfig.get().concise().conciseFeed();
            case TP_COORDINATE, TP_COORDINATE_CONCISE -> CommonConfig.get().concise().conciseTpCoordinate();
            case TP_STRUCTURE, TP_STRUCTURE_CONCISE -> CommonConfig.get().concise().conciseTpStructure();
            case TP_ASK, TP_ASK_CONCISE -> CommonConfig.get().concise().conciseTpAsk();
            case TP_ASK_YES, TP_ASK_YES_CONCISE -> CommonConfig.get().concise().conciseTpAskYes();
            case TP_ASK_NO, TP_ASK_NO_CONCISE -> CommonConfig.get().concise().conciseTpAskNo();
            case TP_ASK_CANCEL, TP_ASK_CANCEL_CONCISE -> CommonConfig.get().concise().conciseTpAskCancel();
            case TP_HERE, TP_HERE_CONCISE -> CommonConfig.get().concise().conciseTpHere();
            case TP_HERE_YES, TP_HERE_YES_CONCISE -> CommonConfig.get().concise().conciseTpHereYes();
            case TP_HERE_NO, TP_HERE_NO_CONCISE -> CommonConfig.get().concise().conciseTpHereNo();
            case TP_HERE_CANCEL, TP_HERE_CANCEL_CONCISE -> CommonConfig.get().concise().conciseTpHereCancel();
            case TP_RANDOM, TP_RANDOM_CONCISE -> CommonConfig.get().concise().conciseTpRandom();
            case TP_SPAWN, TP_SPAWN_OTHER, TP_SPAWN_CONCISE, TP_SPAWN_OTHER_CONCISE ->
                    CommonConfig.get().concise().conciseTpSpawn();
            case TP_WORLD_SPAWN, TP_WORLD_SPAWN_CONCISE -> CommonConfig.get().concise().conciseTpWorldSpawn();
            case TP_TOP, TP_TOP_CONCISE -> CommonConfig.get().concise().conciseTpTop();
            case TP_BOTTOM, TP_BOTTOM_CONCISE -> CommonConfig.get().concise().conciseTpBottom();
            case TP_UP, TP_UP_CONCISE -> CommonConfig.get().concise().conciseTpUp();
            case TP_DOWN, TP_DOWN_CONCISE -> CommonConfig.get().concise().conciseTpDown();
            case TP_VIEW, TP_VIEW_CONCISE -> CommonConfig.get().concise().conciseTpView();
            case TP_HOME, TP_HOME_CONCISE -> CommonConfig.get().concise().conciseTpHome();
            case SET_HOME, SET_HOME_CONCISE -> CommonConfig.get().concise().conciseSetHome();
            case DEL_HOME, DEL_HOME_CONCISE -> CommonConfig.get().concise().conciseDelHome();
            case GET_HOME, GET_HOME_CONCISE -> CommonConfig.get().concise().conciseGetHome();
            case TP_STAGE, TP_STAGE_CONCISE -> CommonConfig.get().concise().conciseTpStage();
            case SET_STAGE, SET_STAGE_CONCISE -> CommonConfig.get().concise().conciseSetStage();
            case DEL_STAGE, DEL_STAGE_CONCISE -> CommonConfig.get().concise().conciseDelStage();
            case GET_STAGE, GET_STAGE_CONCISE -> CommonConfig.get().concise().conciseGetStage();
            case TP_BACK, TP_BACK_CONCISE -> CommonConfig.get().concise().conciseTpBack();
            case TP_GRAVE, TP_GRAVE_CONCISE -> CommonConfig.get().concise().conciseTpGrave();
            case FLY, FLY_CONCISE -> CommonConfig.get().concise().conciseFly();
            case VIRTUAL_OP, VIRTUAL_OP_CONCISE -> CommonConfig.get().concise().conciseVirtualOp();
            default -> false;
        };
    }

    public static boolean hasCommandPermission(CommandSourceStack source, EnumCommandType type) {
        return source.hasPermission(getCommandPermissionLevel(type)) || CommandUtils.hasVirtualPermission(source.getEntity(), type);
    }

    // endregion 指令相关

    // region 安全坐标

    public static ServerLevel getServerLevel() {
        return BaniraCodex.serverInstance().key().getAllLevels().iterator().next();
    }

    public static SafeWorldCoordinate findTopCandidate(ServerPlayer player, SafeWorldCoordinate start) {
        return new SafeCoordinateFinder(player.level(), player).findTopCandidate(start);
    }

    public static SafeWorldCoordinate findBottomCandidate(ServerPlayer player, SafeWorldCoordinate start) {
        return new SafeCoordinateFinder(player.level(), player).findBottomCandidate(start);
    }

    public static SafeWorldCoordinate findUpCandidate(ServerPlayer player, SafeWorldCoordinate start) {
        return new SafeCoordinateFinder(player.level(), player).findUpCandidate(start);
    }

    public static SafeWorldCoordinate findDownCandidate(ServerPlayer player, SafeWorldCoordinate start) {
        return new SafeCoordinateFinder(player.level(), player).findDownCandidate(start);
    }

    public static SafeWorldCoordinate findViewEndCandidate(ServerPlayer player, boolean safe, int range) {
        return new SafeCoordinateFinder(player.level(), player).findViewEndCandidate(player, safe, range);
    }

    public static SafeWorldCoordinate findSafeCoordinate(SafeWorldCoordinate safeWorldCoordinate, ServerPlayer player, boolean belowAllowAir) {
        Level world = DimensionUtils.getLevel(safeWorldCoordinate.dimension());
        int chunkX = safeWorldCoordinate.chunkX();
        int chunkZ = safeWorldCoordinate.chunkZ();
        SafeWorldCoordinate result = new SafeCoordinateFinder(world, player).searchInChunk(safeWorldCoordinate, chunkX, chunkZ, belowAllowAir);
        LOGGER.debug("Target:{}, {}, {} | Safe:{}, {}, {}", safeWorldCoordinate.xInt(), safeWorldCoordinate.yInt(), safeWorldCoordinate.zInt(), result == null ? "null" : result.xInt(), result == null ? "null" : result.yInt(), result == null ? "null" : result.zInt());
        return result == null ? safeWorldCoordinate : result;
    }

    // endregion 安全坐标

    // region 坐标查找

    public static String getHomeDimensionByName(ServerPlayer player, String name) {
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        List<KeyValue<String, String>> list = data.getHomeCoordinate().keySet().stream()
                .filter(key -> key.value().equals(name))
                .toList();
        if (list.size() == 1) {
            return list.get(0).key();
        } else {
            return list.stream()
                    .filter(key -> key.key().equals(DimensionUtils.getDimensionId(player)))
                    .findFirst()
                    .map(KeyValue::key)
                    .orElse(null);
        }
    }

    public static KeyValue<String, String> getPlayerHomeKey(ServerPlayer player, ResourceKey<Level> dimension, String name) {
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        Map<KeyValue<String, String>, SafeWorldCoordinate> homeCoordinate = data.getHomeCoordinate();
        Map<String, String> defaultHome = data.getDefaultHome();
        String currentDimStr = DimensionUtils.getDimensionId(player);
        String targetDimStr = dimension != null ? DimensionUtils.getDimensionId(dimension) : null;

        // 保持插入顺序
        List<KeyValue<String, String>> orderedKeys = new ArrayList<>(homeCoordinate.keySet());

        List<KeyValue<String, String>> candidates = orderedKeys.stream()
                .filter(kv -> targetDimStr == null || kv.key().equals(targetDimStr))
                .filter(kv -> StringUtils.isNullOrEmpty(name) || StringUtils.matches(kv.value(), name))
                .toList();

        if (candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        // 指定维度
        if (targetDimStr != null) {
            KeyValue<String, String> defaultKey = defaultHome.containsKey(targetDimStr)
                    ? new KeyValue<>(targetDimStr, defaultHome.get(targetDimStr))
                    : null;
            if (defaultKey != null && homeCoordinate.containsKey(defaultKey) && candidates.contains(defaultKey)) {
                return defaultKey;
            }
        }

        // 当前维度默认家
        KeyValue<String, String> currentDefault = defaultHome.containsKey(currentDimStr)
                ? new KeyValue<>(currentDimStr, defaultHome.get(currentDimStr))
                : null;
        if (currentDefault != null && homeCoordinate.containsKey(currentDefault) && candidates.contains(currentDefault)) {
            return currentDefault;
        }
        // 其他维度默认家
        for (Map.Entry<String, String> entry : defaultHome.entrySet()) {
            if (!entry.getKey().equals(currentDimStr)) {
                KeyValue<String, String> kv = new KeyValue<>(entry.getKey(), entry.getValue());
                if (homeCoordinate.containsKey(kv) && candidates.contains(kv)) {
                    return kv;
                }
            }
        }
        // 最后添加的家
        boolean useDistanceTiebreaker = targetDimStr == null || targetDimStr.equals(currentDimStr);
        Comparator<KeyValue<String, String>> baseComparator = StringUtils.isNotNullOrEmpty(name)
                ? Comparator.comparingInt((KeyValue<String, String> kv) -> -StringUtils.matchDegree(kv.value(), name))
                .thenComparingInt((KeyValue<String, String> kv) -> -kv.value().length())
                .thenComparingInt(orderedKeys::indexOf)
                : Comparator.comparingInt(orderedKeys::indexOf);
        if (useDistanceTiebreaker) {
            baseComparator = baseComparator.thenComparingDouble((KeyValue<String, String> kv) -> {
                if (!kv.key().equals(currentDimStr)) return -Double.MAX_VALUE;
                SafeWorldCoordinate c = homeCoordinate.get(kv);
                if (c == null) return -Double.MAX_VALUE;
                double dx = c.x() - player.getX();
                double dy = c.y() - player.getY();
                double dz = c.z() - player.getZ();
                return -(dx * dx + dy * dy + dz * dz);
            });
        }
        return candidates.stream().max(baseComparator).orElse(null);
    }

    /**
     * 获取指定玩家的家坐标
     *
     * @param player    玩家
     * @param dimension 维度
     * @param name      名称
     */
    public static SafeWorldCoordinate getPlayerHome(ServerPlayer player, ResourceKey<Level> dimension, String name) {
        return PlayerTeleportData.getData(player).getHomeCoordinate().getOrDefault(getPlayerHomeKey(player, dimension, name), null);
    }

    public static String getStageDimensionByName(@Nullable ServerPlayer player, String name) {
        WorldStageData stageData = WorldStageData.get();
        List<KeyValue<String, String>> list = stageData.getStageCoordinate().keySet().stream()
                .filter(key -> key.value().equals(name))
                .toList();
        if (list.size() == 1) {
            return list.get(0).key();
        } else if (player != null) {
            return list.stream()
                    .filter(key -> key.key().equals(DimensionUtils.getDimensionId(player)))
                    .findFirst()
                    .map(KeyValue::key)
                    .orElse(null);
        }
        return null;
    }

    public static KeyValue<String, String> getStageKey(ServerPlayer player, ResourceKey<Level> dimension, String name) {
        WorldStageData stageData = WorldStageData.get();
        Map<KeyValue<String, String>, SafeWorldCoordinate> stageCoordinate = stageData.getStageCoordinate();
        String currentDimStr = DimensionUtils.getDimensionId(player);
        String targetDimStr = dimension != null ? DimensionUtils.getDimensionId(dimension) : null;

        List<KeyValue<String, String>> orderedKeys = new ArrayList<>(stageCoordinate.keySet());

        // 指定维度
        if (targetDimStr != null) {
            List<KeyValue<String, String>> candidates = orderedKeys.stream()
                    .filter(kv -> kv.key().equals(targetDimStr))
                    .filter(kv -> StringUtils.isNullOrEmpty(name) || StringUtils.matches(kv.value(), name))
                    .toList();
            if (candidates.isEmpty()) return null;
            if (candidates.size() == 1) return candidates.get(0);
            if (StringUtils.isNotNullOrEmpty(name)) {
                return candidates.stream()
                        .max(Comparator.comparingInt((KeyValue<String, String> kv) -> -StringUtils.matchDegree(kv.value(), name))
                                .thenComparingInt((KeyValue<String, String> kv) -> -kv.value().length())
                                .thenComparingInt(orderedKeys::indexOf))
                        .orElse(null);
            }
            return candidates.stream().max(Comparator.comparingInt(orderedKeys::indexOf)).orElse(null);
        }

        // 未指定维度与名称: 当前维度最近的 > 最后添加的
        if (StringUtils.isNullOrEmpty(name)) {
            KeyValue<String, String> nearest = findNearestStageInDimension(stageCoordinate, player, currentDimStr);
            if (nearest != null) return nearest;
            return orderedKeys.stream().max(Comparator.comparingInt(orderedKeys::indexOf)).orElse(null);
        }

        // 指定名称: 当前维度包含匹配 > 其他维度包含匹配
        List<KeyValue<String, String>> currentDimMatches = orderedKeys.stream()
                .filter(kv -> kv.key().equals(currentDimStr))
                .filter(kv -> StringUtils.matches(kv.value(), name))
                .toList();
        if (!currentDimMatches.isEmpty()) {
            return currentDimMatches.stream()
                    .max(Comparator.comparingInt((KeyValue<String, String> kv) -> -StringUtils.matchDegree(kv.value(), name))
                            .thenComparingInt((KeyValue<String, String> kv) -> -kv.value().length())
                            .thenComparingInt(orderedKeys::indexOf))
                    .orElse(null);
        }
        List<KeyValue<String, String>> otherDimMatches = orderedKeys.stream()
                .filter(kv -> !kv.key().equals(currentDimStr))
                .filter(kv -> StringUtils.matches(kv.value(), name))
                .toList();
        if (!otherDimMatches.isEmpty()) {
            return otherDimMatches.stream()
                    .max(Comparator.comparingInt((KeyValue<String, String> kv) -> -StringUtils.matchDegree(kv.value(), name))
                            .thenComparingInt((KeyValue<String, String> kv) -> -kv.value().length())
                            .thenComparingInt(orderedKeys::indexOf))
                    .orElse(null);
        }
        return null;
    }

    /**
     * 在指定维度内找距离玩家最近的驿站
     */
    private static KeyValue<String, String> findNearestStageInDimension(Map<KeyValue<String, String>, SafeWorldCoordinate> stageCoordinate,
                                                                        ServerPlayer player, String dimensionStr) {
        return stageCoordinate.entrySet().stream()
                .filter(entry -> entry.getKey().key().equals(dimensionStr))
                .min(Comparator.comparingDouble(entry -> {
                    SafeWorldCoordinate c = entry.getValue();
                    double dx = c.x() - player.getX();
                    double dy = c.y() - player.getY();
                    double dz = c.z() - player.getZ();
                    return dx * dx + dy * dy + dz * dz;
                }))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * 获取驿站坐标
     */
    public static SafeWorldCoordinate getStageCoordinate(ServerPlayer player, ResourceKey<Level> dimension, String name) {
        KeyValue<String, String> key = getStageKey(player, dimension, name);
        return key != null ? WorldStageData.get().getStageCoordinate().get(key) : null;
    }

    /**
     * 获取玩家离开的坐标
     *
     * @param player    玩家
     * @param type      传送类型
     * @param dimension 维度
     * @return 查询到的离开坐标（如果未找到则返回 null）
     */
    public static TeleportRecord getBackTeleportRecord(ServerPlayer player, @Nullable EnumTeleportType type, @Nullable ResourceKey<Level> dimension) {
        TeleportRecord result = null;
        // 获取玩家的传送数据
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        List<TeleportRecord> records = data.getTeleportRecords();
        Stream<TeleportRecord> stream = records.stream()
                .filter(record -> type == null || record.getTeleportType() == type);
        for (String s : CommonConfig.get().general().teleportBackSkipType()) {
            EnumTeleportType value = EnumTeleportType.valueOfEx(s);
            stream = stream
                    .filter(record -> type == value || record.getTeleportType() != value);
        }
        Optional<TeleportRecord> optionalRecord = stream
                .filter(record -> dimension == null || record.getBefore().dimension().equals(dimension))
                .max(Comparator.comparing(TeleportRecord::getTeleportTime));
        if (optionalRecord.isPresent()) {
            result = optionalRecord.get();
        }
        return result;
    }

    public static void removeBackTeleportRecord(ServerPlayer player, TeleportRecord record) {
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        data.getTeleportRecords().remove(record);
        data.save();
        PlayerTeleportData.syncPlayerData(player);
    }

    // endregion 坐标查找

    // region 传送相关

    /**
     * 在真正执行传送前按玩家配置进行倒计时；{@code teleportAction} 在倒计时结束时于服务端主线程执行（调用方应自行解析在线玩家等）。
     */
    private static void executeTeleportWithCountdown(ServerPlayer player, EnumTeleportType type, Runnable teleportAction) {
        MinecraftServer server = player.getServer();
        int sec = TeleportCountdownHelper.getEffectiveCountdownSeconds(player, type);
        if (sec <= 0 || server == null) {
            teleportAction.run();
            return;
        }
        TeleportCountdownTracker.Session countdownSession = TeleportCountdownTracker.begin(player,
                CommonConfig.get().teleportCountdown().cancelCountdownOnPlayerMove(),
                CommonConfig.get().teleportCountdown().cancelCountdownOnPlayerDamage());
        UUID uuid = player.getUUID();
        for (int i = 0; i < sec; i++) {
            final int display = sec - i;
            BaniraScheduler.scheduleAfterMillis(server, i * 1000.0, () -> {
                if (countdownSession.isCancelled()) {
                    return;
                }
                ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p == null) {
                    return;
                }
                MessageUtils.sendActionBarMessage(p, NarcissusComponent.get().transAuto("tp_countdown_actionbar", String.valueOf(display)));
            });
        }
        BaniraScheduler.scheduleAfterMillis(server, sec * 1000.0, () -> {
            if (!countdownSession.tryMarkCompleteAndRemove()) {
                return;
            }
            teleportAction.run();
        });
    }

    /**
     * 检查传送范围
     */
    public static int checkRange(ServerPlayer player, EnumTeleportType type, int range) {
        int maxRange;
        switch (type) {
            case TP_VIEW:
                maxRange = CommonConfig.get().general().teleportViewDistanceLimit();
                break;
            default:
                maxRange = CommonConfig.get().general().teleportRandomDistanceLimit();
                break;
        }
        if (range > maxRange) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("range_too_large", maxRange));
        } else if (range <= 0) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("range_too_small", 1));
        }
        return Math.min(Math.max(range, 1), maxRange);
    }

    /**
     * 执行传送请求
     */
    public static void teleportTo(@NonNull TeleportRequest request) {
        teleportTo(request.getRequester(), request.getTarget(), request.getTeleportType(), request.isSafe());
    }

    /**
     * 传送玩家到指定玩家
     *
     * @param from 传送者
     * @param to   目标玩家
     */
    public static void teleportTo(@NonNull ServerPlayer from, @NonNull ServerPlayer to, EnumTeleportType type, boolean safe) {
        if (EnumTeleportType.TP_HERE == type) {
            teleportTo(to, new SafeWorldCoordinate(from).safe(safe), type);
        } else {
            teleportTo(from, new SafeWorldCoordinate(to).safe(safe), type);
        }
    }

    /**
     * 传送玩家到指定坐标
     *
     * @param player 玩家
     * @param after  坐标
     */
    public static void teleportTo(@NonNull ServerPlayer player, @NonNull SafeWorldCoordinate after, EnumTeleportType type) {
        SafeWorldCoordinate before = new SafeWorldCoordinate(player);
        Level world = player.level();
        if (world != null) {
            ServerLevel level = DimensionUtils.getLevel(after.dimension());
            if (level != null) {
                if (after.safe()) {
                    // 异步的代价就是粪吗
                    MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("safe_searching"));
                    new Thread(() -> {
                        SafeWorldCoordinate finalAfter = after.clone();
                        finalAfter = findSafeCoordinate(finalAfter, player, false);
                        Runnable runnable;
                        // 判断是否需要在脚下放置方块
                        SafeBlockChecker checker = new SafeBlockChecker(level, player);
                        if (CommonConfig.get().general().safeTeleport().setBlockWhenSafeNotFound() && !checker.isSafeBlock(finalAfter.toBlockPos(), false)) {
                            BlockState blockState;
                            List<ItemStack> playerItemList = ItemUtils.getAllPlayerItems(player);
                            if (CollectionUtils.isNotNullOrEmpty(NarcissusFarewell.getSafeBlock().getSafeBlocksState())) {
                                if (CommonConfig.get().general().safeTeleport().getBlockFromInventory()) {
                                    blockState = NarcissusFarewell.getSafeBlock().getSafeBlocksState().stream()
                                            .filter(block -> playerItemList.stream().map(ItemStack::getItem).anyMatch(item -> new ItemStack(block.getBlock()).getItem().equals(item)))
                                            .findFirst().orElse(null);
                                } else {
                                    blockState = NarcissusFarewell.getSafeBlock().getSafeBlocksState().get(0);
                                }
                            } else {
                                blockState = null;
                            }
                            if (blockState != null) {
                                SafeWorldCoordinate airSafeWorldCoordinate = findSafeCoordinate(finalAfter, player, true);
                                if (!airSafeWorldCoordinate.xyzString().equals(finalAfter.xyzString())) {
                                    finalAfter = airSafeWorldCoordinate;
                                    runnable = () -> {
                                        Item blockItem = new ItemStack(blockState.getBlock()).getItem();
                                        Item remove = playerItemList.stream().map(ItemStack::getItem).filter(blockItem::equals).findFirst().orElse(null);
                                        if (remove != null) {
                                            ItemStack itemStack = new ItemStack(remove);
                                            itemStack.setCount(1);
                                            if (ItemUtils.removePlayerItem(player, itemStack)) {
                                                level.setBlockAndUpdate(airSafeWorldCoordinate.toBlockPos().below(), blockState.getBlock().defaultBlockState());
                                            }
                                        }
                                    };
                                } else {
                                    runnable = null;
                                }
                            } else {
                                runnable = null;
                            }
                        } else {
                            runnable = null;
                        }
                        SafeWorldCoordinate finalAfter1 = finalAfter;
                        MinecraftServer srv = player.server;
                        UUID pid = player.getUUID();
                        player.server.submit(() -> {
                            if (runnable != null) runnable.run();
                            ServerPlayer online = srv.getPlayerList().getPlayer(pid);
                            if (online == null) {
                                return;
                            }
                            executeTeleportWithCountdown(online, type, () -> {
                                ServerPlayer pl = srv.getPlayerList().getPlayer(pid);
                                if (pl != null) {
                                    teleportPlayer(pl, finalAfter1, type, before, level);
                                }
                            });
                        });
                    }).start();
                } else {
                    MinecraftServer srv = player.getServer();
                    UUID pid = player.getUUID();
                    executeTeleportWithCountdown(player, type, () -> {
                        ServerPlayer pl = srv != null ? srv.getPlayerList().getPlayer(pid) : null;
                        if (pl != null) {
                            teleportPlayer(pl, after, type, before, level);
                        }
                    });
                }
            }
        }
    }

    private static void teleportPlayer(@NonNull ServerPlayer player, @NonNull SafeWorldCoordinate after, EnumTeleportType type, SafeWorldCoordinate before, ServerLevel level) {
        ResourceLocation sound = Identifier.id().parse(CommonConfig.get().general().tpSound());
        NarcissusUtils.playSound(player, sound, 1.0f, 1.0f);
        after.y(Math.floor(after.y()) + 0.1);

        // 传送跟随者
        teleportFollowers(player, after, level);
        // 传送载体与乘客
        Entity vehicle = teleportPassengers(player, null, player.getRootVehicle(), after, level);
        // 传送玩家
        doTeleport(player, after, level);
        // 使玩家重新坐上载体
        if (vehicle != null) {
            player.startRiding(vehicle, true);
            // 同步客户端状态
            PacketUtils.broadcastPacket(new ClientboundSetPassengersPacket(vehicle));
        }

        NarcissusUtils.playSound(player, sound, 1.0f, 1.0f);
        TeleportRecord record = new TeleportRecord();
        record.setTeleportTime(new Date());
        record.setTeleportType(type);
        record.setBefore(before);
        record.setAfter(after);
        PlayerTeleportData.getData(player).addTeleportRecords(record);
        PlayerTeleportData.syncPlayerData(player);
    }

    /**
     * 传送载具及其所有乘客
     *
     * @param parent              载具
     * @param passenger           乘客
     * @param safeWorldCoordinate 目标坐标
     * @param level               目标世界
     * @return 玩家的坐骑
     */
    private static @Nullable Entity teleportPassengers(ServerPlayer player, Entity parent, Entity passenger, @NonNull SafeWorldCoordinate safeWorldCoordinate, ServerLevel level) {
        if (!CommonConfig.get().general().tpWithVehicle() || passenger == null) return null;

        Entity playerVehicle = null;
        List<Entity> passengers = new ArrayList<>(passenger.getPassengers());

        // 递归传送所有乘客
        for (Entity entity : passengers) {
            if (CollectionUtils.isNotNullOrEmpty(entity.getPassengers())) {
                Entity value = teleportPassengers(player, passenger, entity, safeWorldCoordinate, level);
                if (value != null) {
                    playerVehicle = value;
                }
            }
        }

        passengers.forEach(Entity::stopRiding);

        // 传送载具
        if (parent == null) {
            passenger = doTeleport(passenger, safeWorldCoordinate, level);
        }
        // 传送所有乘客
        for (Entity entity : passengers) {
            if (entity == player) {
                playerVehicle = passenger;
            } else if (entity.getVehicle() == null) {
                int oldId = entity.getId();
                entity = doTeleport(entity, safeWorldCoordinate, level);
                entity.startRiding(passenger, true);
                // 更新玩家乘坐的实体对象
                if (playerVehicle != null && oldId == playerVehicle.getId()) {
                    playerVehicle = entity;
                }
            }
        }
        // 同步客户端状态
        PacketUtils.broadcastPacket(new ClientboundSetPassengersPacket(passenger));
        return playerVehicle;
    }

    /**
     * 传送跟随的实体
     */
    private static void teleportFollowers(@NonNull ServerPlayer player, @NonNull SafeWorldCoordinate safeWorldCoordinate, ServerLevel level) {
        if (!CommonConfig.get().general().tpWithFollower()) return;

        int followerRange = CommonConfig.get().general().tpWithFollowerRange();

        // 传送主动跟随的实体
        for (TamableAnimal entity : player.level().getEntitiesOfClass(TamableAnimal.class, player.getBoundingBox().inflate(followerRange))) {
            if (entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(player.getUUID()) && !entity.isOrderedToSit()) {
                doTeleport(entity, safeWorldCoordinate, level);
            }
        }

        // 传送拴绳实体
        for (Mob entity : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(followerRange))) {
            if (entity.getLeashHolder() == player) {
                doTeleport(entity, safeWorldCoordinate, level);
            }
        }

        // 传送被吸引的非敌对实体
        for (Mob entity : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(followerRange))) {
            // 排除敌对生物
            if (entity instanceof Monster) continue;

            if (entity.goalSelector.getAvailableGoals().stream()
                    .anyMatch(goal -> goal.isRunning()
                            && (goal.getGoal() instanceof TemptGoal)
                            && ((TemptGoalAccessor) goal.getGoal()).narcissus$player() == player
                    )) {
                doTeleport(entity, safeWorldCoordinate, level);
            }
        }
    }

    private static Entity doTeleport(@NonNull Entity entity, @NonNull SafeWorldCoordinate safeWorldCoordinate, ServerLevel level) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(level, safeWorldCoordinate.x(), safeWorldCoordinate.y(), safeWorldCoordinate.z()
                    , safeWorldCoordinate.yaw() == 0 ? player.getYRot() : (float) safeWorldCoordinate.yaw()
                    , safeWorldCoordinate.pitch() == 0 ? player.getXRot() : (float) safeWorldCoordinate.pitch());
        } else {
            if (level == entity.level()) {
                entity.teleportToWithTicket(safeWorldCoordinate.x(), safeWorldCoordinate.y(), safeWorldCoordinate.z());
            } else {
                entity = entity.changeDimension(level, new ITeleporter() {
                    @Override
                    public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
                        // 计算目标区块坐标
                        int chunkX = safeWorldCoordinate.chunkX();
                        int chunkZ = safeWorldCoordinate.chunkZ();
                        // 确保目标区块已加载
                        destWorld.getChunkSource().addRegionTicket(
                                TicketType.POST_TELEPORT,
                                new ChunkPos(chunkX, chunkZ),
                                4, // 加载等级
                                entity.getId()
                        );
                        // 复制实体，并且不生成传送门
                        Entity newEntity = repositionEntity.apply(false);
                        newEntity.moveTo(safeWorldCoordinate.x(), safeWorldCoordinate.y(), safeWorldCoordinate.z(), yaw, newEntity.getXRot());
                        return newEntity;
                    }
                });
            }
        }
        return entity;
    }

    // endregion 传送相关


    // region 跨维度传送

    public static boolean isTeleportAcrossDimensionEnabled(ServerPlayer player, ResourceKey<Level> to, EnumTeleportType type) {
        boolean result = true;
        if (player.level().dimension() != to) {
            if (CommonConfig.get().general().teleportAcrossDimension()) {
                if (!NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, type)) {
                    result = false;
                    MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("across_dimension_not_enable_for", getCommand(type)));
                }
            } else {
                result = false;
                MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("across_dimension_not_enable"));
            }
        }
        return result;
    }

    /**
     * 判断传送类型跨维度传送是否开启
     */
    public static boolean isTeleportTypeAcrossDimensionEnabled(ServerPlayer player, EnumTeleportType type) {
        int permission;
        switch (type) {
            case TP_COORDINATE:
                permission = CommonConfig.get().permission().permissionTpCoordinateAcrossDimension();
                break;
            case TP_STRUCTURE:
                permission = CommonConfig.get().permission().permissionTpStructureAcrossDimension();
                break;
            case TP_ASK:
                permission = CommonConfig.get().permission().permissionTpAskAcrossDimension();
                break;
            case TP_HERE:
                permission = CommonConfig.get().permission().permissionTpHereAcrossDimension();
                break;
            case TP_RANDOM:
                permission = CommonConfig.get().permission().permissionTpRandomAcrossDimension();
                break;
            case TP_SPAWN:
                permission = CommonConfig.get().permission().permissionTpSpawnAcrossDimension();
                break;
            case TP_WORLD_SPAWN:
                permission = CommonConfig.get().permission().permissionTpWorldSpawnAcrossDimension();
                break;
            case TP_HOME:
                permission = CommonConfig.get().permission().permissionTpHomeAcrossDimension();
                break;
            case TP_STAGE:
                permission = CommonConfig.get().permission().permissionTpStageAcrossDimension();
                break;
            case TP_BACK:
                permission = CommonConfig.get().permission().permissionTpBackAcrossDimension();
                break;
            case TP_GRAVE:
                permission = CommonConfig.get().permission().permissionTpGraveAcrossDimension();
                break;
            default:
                permission = 0;
                break;
        }
        return permission > -1 && player.hasPermissions(permission);
    }

    // endregion 跨维度传送

    // region 传送冷却

    /**
     * 获取传送/传送请求冷却时间
     *
     * @param player 玩家
     * @param type   传送类型
     */
    public static int getTeleportCoolDown(ServerPlayer player, EnumTeleportType type) {
        // 如果传送卡类型为抵消冷却时间，则不计算冷却时间
        if (EnumCardType.REFUND_COOLDOWN.name().equalsIgnoreCase(CommonConfig.get().base().teleportCardType().name())
                || EnumCardType.REFUND_ALL_COST_AND_COOLDOWN.name().equalsIgnoreCase(CommonConfig.get().base().teleportCardType().name())
        ) {
            if (PlayerTeleportData.getData(player).getTeleportCard() > 0) {
                return 0;
            }
        }
        Instant current = Instant.now();
        int commandCoolDown = getCommandCoolDown(type);
        Instant lastTpTime = PlayerTeleportData.getData(player).getTeleportRecords(type).stream()
                .map(TeleportRecord::getTeleportTime)
                .max(Comparator.comparing(Date::toInstant))
                .orElse(new Date(0)).toInstant();
        switch (CommonConfig.get().general().teleportRequestCooldownType()) {
            case COMMON:
                return calculateCooldown(player.getUUID(), current, lastTpTime, CommonConfig.get().general().teleportRequestCooldown(), null);
            case INDIVIDUAL:
                return calculateCooldown(player.getUUID(), current, lastTpTime, commandCoolDown, type);
            case MIXED:
                int globalCommandCoolDown = CommonConfig.get().general().teleportRequestCooldown();
                int individualCooldown = calculateCooldown(player.getUUID(), current, lastTpTime, commandCoolDown, type);
                int globalCooldown = calculateCooldown(player.getUUID(), current, lastTpTime, globalCommandCoolDown, null);
                return Math.max(individualCooldown, globalCooldown);
            default:
                return 0;
        }
    }

    /**
     * 获取传送命令冷却时间
     *
     * @param type 传送类型
     */
    public static int getCommandCoolDown(EnumTeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return CommonConfig.get().cooldown().cooldownTpCoordinate();
            case TP_STRUCTURE:
                return CommonConfig.get().cooldown().cooldownTpStructure();
            case TP_ASK:
                return CommonConfig.get().cooldown().cooldownTpAsk();
            case TP_HERE:
                return CommonConfig.get().cooldown().cooldownTpHere();
            case TP_RANDOM:
                return CommonConfig.get().cooldown().cooldownTpRandom();
            case TP_SPAWN:
                return CommonConfig.get().cooldown().cooldownTpSpawn();
            case TP_WORLD_SPAWN:
                return CommonConfig.get().cooldown().cooldownTpWorldSpawn();
            case TP_TOP:
                return CommonConfig.get().cooldown().cooldownTpTop();
            case TP_BOTTOM:
                return CommonConfig.get().cooldown().cooldownTpBottom();
            case TP_UP:
                return CommonConfig.get().cooldown().cooldownTpUp();
            case TP_DOWN:
                return CommonConfig.get().cooldown().cooldownTpDown();
            case TP_VIEW:
                return CommonConfig.get().cooldown().cooldownTpView();
            case TP_HOME:
                return CommonConfig.get().cooldown().cooldownTpHome();
            case TP_STAGE:
                return CommonConfig.get().cooldown().cooldownTpStage();
            case TP_BACK:
                return CommonConfig.get().cooldown().cooldownTpBack();
            case TP_GRAVE:
                return CommonConfig.get().cooldown().cooldownTpGrave();
            default:
                return 0;
        }
    }

    private static int calculateCooldown(UUID uuid, Instant current, Instant lastTpTime, int cooldown, EnumTeleportType type) {
        Optional<TeleportRequest> latestRequest = NarcissusFarewell.getTeleportRequest().values().stream()
                .filter(request -> request.getRequester().getUUID().equals(uuid))
                .filter(request -> type == null || request.getTeleportType() == type)
                .max(Comparator.comparing(TeleportRequest::getRequestTime));

        Instant lastRequestTime = latestRequest.map(r -> r.getRequestTime().toInstant()).orElse(current.minusSeconds(cooldown));
        return Math.max(0, Math.max(cooldown - (int) Duration.between(lastRequestTime, current).getSeconds(), cooldown - (int) Duration.between(lastTpTime, current).getSeconds()));
    }

    // endregion 传送冷却

    // region 传送代价

    /**
     * 验证传送代价
     *
     * @param player 请求传送的玩家
     * @param target 目标坐标
     * @param type   传送类型
     * @param submit 是否收取代价
     * @return 是否验证通过
     */
    public static boolean validTeleportCost(ServerPlayer player, SafeWorldCoordinate target, EnumTeleportType type, boolean submit) {
        return validateCost(player, target.dimension(), calculateDistance(new SafeWorldCoordinate(player), target), type, submit);
    }

    /**
     * 验证并收取传送代价
     *
     * @param request 传送请求
     * @param submit  是否收取代价
     * @return 是否验证通过
     */
    public static boolean validTeleportCost(TeleportRequest request, boolean submit) {
        SafeWorldCoordinate requesterSafeWorldCoordinate = new SafeWorldCoordinate(request.getRequester());
        SafeWorldCoordinate targetSafeWorldCoordinate = new SafeWorldCoordinate(request.getTarget());
        return validateCost(request.getRequester(), request.getTarget().level().dimension(), calculateDistance(requesterSafeWorldCoordinate, targetSafeWorldCoordinate), request.getTeleportType(), submit);
    }

    /**
     * 通用的传送代价验证逻辑
     *
     * @param player       请求传送的玩家
     * @param targetDim    目标维度
     * @param distance     计算的距离
     * @param teleportType 传送类型
     * @param submit       是否收取代价
     * @return 是否验证通过
     */
    private static boolean validateCost(ServerPlayer player, ResourceKey<Level> targetDim, double distance, EnumTeleportType teleportType, boolean submit) {
        TeleportCost teleportCost = NarcissusUtils.getCommandCost(teleportType);
        if (teleportCost.getType() == EnumCostType.NONE) return true;
        PlayerTeleportData data = PlayerTeleportData.getData(player);

        double adjustedDistance;
        if (player.level().dimension() == targetDim) {
            int limit = CommonConfig.get().general().teleportCostDistanceLimit();
            adjustedDistance = limit == 0 ? distance : Math.min(limit, distance);
        } else {
            adjustedDistance = CommonConfig.get().general().teleportCostDistanceAcrossDimension();
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("distance", adjustedDistance);
        vars.put("num", (double) teleportCost.getNum());
        vars.put("rate", teleportCost.getRate());

        double need;
        try {
            need = new SafeExpressionEvaluator(teleportCost.getExp()).evaluateDouble(vars);
        } catch (Exception e) {
            LOGGER.error("Failed to calculate cost with expression: {}", teleportCost.getExp(), e);
            need = teleportCost.getNum() * adjustedDistance * teleportCost.getRate();
        }
        need = Math.min(need, teleportCost.getUpper());
        need = Math.max(need, teleportCost.getLower());

        int cardNeed = getTeleportCardNeed(need);
        int costNeed = getTeleportCostNeed(data, cardNeed, (int) Math.ceil(need));
        boolean result = false;

        if (costNeed < 0) {
            MessageUtils.sendMessage(player
                    , NarcissusComponent.get().transAuto("cost_not_enough"
                            , NarcissusComponent.get().transAuto("teleport_card")
                            , cardNeed
                    ));
        }

        switch (teleportCost.getType()) {
            case EXP_POINT:
                result = player.totalExperience >= costNeed;
                if (!result) {
                    MessageUtils.sendMessage(player
                            , NarcissusComponent.get().transAuto("cost_not_enough"
                                    , NarcissusComponent.get().transAuto("exp_point")
                                    , costNeed
                            ));
                } else if (submit) {
                    player.giveExperiencePoints(-costNeed);
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case EXP_LEVEL:
                result = player.experienceLevel >= costNeed;
                if (!result) {
                    MessageUtils.sendMessage(player
                            , NarcissusComponent.get().transAuto("cost_not_enough"
                                    , NarcissusComponent.get().transAuto("exp_level")
                                    , costNeed
                            ));
                } else if (submit) {
                    player.giveExperienceLevels(-costNeed);
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case HEALTH:
                result = player.getHealth() > costNeed;
                if (!result) {
                    MessageUtils.sendMessage(player
                            , NarcissusComponent.get().transAuto("cost_not_enough"
                                    , NarcissusComponent.get().transAuto("health")
                                    , costNeed
                            ));
                } else if (submit) {
                    try {
                        EntityDataAccessor<? super Float> DATA_HEALTH_ID = ((LivingEntityInvoker) player).narcissus$dataHealthId();
                        Float health = (Float) player.getEntityData().get(DATA_HEALTH_ID);
                        player.getEntityData().set(DATA_HEALTH_ID, health - costNeed);
                    } catch (Exception e) {
                        player.hurt(player.level().damageSources().magic(), costNeed);
                    }
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case HUNGER:
                result = player.getFoodData().getFoodLevel() >= costNeed;
                if (!result) {
                    MessageUtils.sendMessage(player
                            , NarcissusComponent.get().transAuto("cost_not_enough"
                                    , NarcissusComponent.get().transAuto("hunger")
                                    , costNeed
                            ));
                } else if (submit) {
                    player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - costNeed);
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case ITEM:
                try {
                    ItemStack itemStack = ItemUtils.deserializeItemStack(teleportCost.getConf());
                    result = getItemCount(player.getInventory().items, itemStack) >= costNeed;
                    itemStack.setCount(costNeed);
                    if (!result) {
                        MessageUtils.sendMessage(player
                                , NarcissusComponent.get().transAuto("cost_not_enough"
                                        , NarcissusComponent.get().literal(ItemUtils.getItemHoverNameString(itemStack))
                                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(itemStack)))
                                        , costNeed
                                )
                        );
                    } else if (submit) {
                        result = ItemUtils.removePlayerItem(player, itemStack);
                        // 代价不足
                        if (result) {
                            data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                        } else {
                            MessageUtils.sendMessage(player
                                    , NarcissusComponent.get().transAuto("cost_not_enough"
                                            , NarcissusComponent.get().literal(ItemUtils.getItemHoverNameString(itemStack))
                                                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(itemStack)))
                                            , costNeed
                                    )
                            );
                        }
                    }
                } catch (Exception e) {
                    result = false;
                    LOGGER.error("Failed to teleport with item cost:", e);
                }
                break;
            case COMMAND:
                try {
                    result = costNeed == 0;
                    if (result && submit) {
                        String command = teleportCost.getConf().replaceAll("\\[num]", String.valueOf(costNeed));
                        result = CommandUtils.executeCommand(player, command);
                        if (result) {
                            data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                        }
                    }
                } catch (Exception e) {
                    result = false;
                    LOGGER.error("Failed to teleport with command cost:", e);
                }
                break;
        }
        if (submit && result) {
            PlayerTeleportData.syncPlayerData(player);
        }
        return result;
    }

    /**
     * 须支付多少传送卡
     */
    public static int getTeleportCardNeed(double need) {
        int ceil = (int) Math.ceil(need);
        if (!CommonConfig.get().base().teleportCard()) return 0;
        switch (CommonConfig.get().base().teleportCardType()) {
            case LIKE_COST:
            case REFUND_COST:
            case REFUND_COST_AND_COOLDOWN:
                return ceil;
            case NONE:
            case REFUND_ALL_COST:
            case REFUND_COOLDOWN:
            case REFUND_ALL_COST_AND_COOLDOWN:
            default:
                return 1;
        }
    }

    /**
     * 使用传送卡后还须支付多少代价
     *
     * @return -1：传送卡不足    0：传送卡足以抵消代价    >0：还须支付多少代价
     */
    public static int getTeleportCostNeed(PlayerTeleportData data, int card, int need) {
        if (!CommonConfig.get().base().teleportCard()) return need;
        switch (CommonConfig.get().base().teleportCardType()) {
            case NONE:
                // card = 1
                return data.getTeleportCard() >= card ? need : -1;
            case LIKE_COST:
                // card = need
                return data.getTeleportCard() >= card ? card : -1;
            case REFUND_COOLDOWN:
                return need;
            case REFUND_ALL_COST:
            case REFUND_ALL_COST_AND_COOLDOWN:
                // card = 1
                return data.getTeleportCard() >= card ? 0 : need;
            case REFUND_COST:
            case REFUND_COST_AND_COOLDOWN:
                // card = need
            default:
                return Math.max(0, card - data.getTeleportCard());
        }
    }

    public static TeleportCost getCommandCost(EnumTeleportType type) {
        TeleportCost cost = new TeleportCost();
        switch (type) {
            case TP_COORDINATE:
                cost.setType(CommonConfig.get().cost().tpCoordinate().type());
                cost.setNum(CommonConfig.get().cost().tpCoordinate().num());
                cost.setRate(CommonConfig.get().cost().tpCoordinate().rate());
                cost.setConf(CommonConfig.get().cost().tpCoordinate().conf());
                cost.setLower(CommonConfig.get().cost().tpCoordinate().numLower());
                cost.setUpper(CommonConfig.get().cost().tpCoordinate().numUpper());
                cost.setExp(CommonConfig.get().cost().tpCoordinate().exp());
                break;
            case TP_STRUCTURE:
                cost.setType(CommonConfig.get().cost().tpStructure().type());
                cost.setNum(CommonConfig.get().cost().tpStructure().num());
                cost.setRate(CommonConfig.get().cost().tpStructure().rate());
                cost.setConf(CommonConfig.get().cost().tpStructure().conf());
                cost.setLower(CommonConfig.get().cost().tpStructure().numLower());
                cost.setUpper(CommonConfig.get().cost().tpStructure().numUpper());
                cost.setExp(CommonConfig.get().cost().tpStructure().exp());
                break;
            case TP_ASK:
                cost.setType(CommonConfig.get().cost().tpAsk().type());
                cost.setNum(CommonConfig.get().cost().tpAsk().num());
                cost.setRate(CommonConfig.get().cost().tpAsk().rate());
                cost.setConf(CommonConfig.get().cost().tpAsk().conf());
                cost.setLower(CommonConfig.get().cost().tpAsk().numLower());
                cost.setUpper(CommonConfig.get().cost().tpAsk().numUpper());
                cost.setExp(CommonConfig.get().cost().tpAsk().exp());
                break;
            case TP_HERE:
                cost.setType(CommonConfig.get().cost().tpHere().type());
                cost.setNum(CommonConfig.get().cost().tpHere().num());
                cost.setRate(CommonConfig.get().cost().tpHere().rate());
                cost.setConf(CommonConfig.get().cost().tpHere().conf());
                cost.setLower(CommonConfig.get().cost().tpHere().numLower());
                cost.setUpper(CommonConfig.get().cost().tpHere().numUpper());
                cost.setExp(CommonConfig.get().cost().tpHere().exp());
                break;
            case TP_RANDOM:
                cost.setType(CommonConfig.get().cost().tpRandom().type());
                cost.setNum(CommonConfig.get().cost().tpRandom().num());
                cost.setRate(CommonConfig.get().cost().tpRandom().rate());
                cost.setConf(CommonConfig.get().cost().tpRandom().conf());
                cost.setLower(CommonConfig.get().cost().tpRandom().numLower());
                cost.setUpper(CommonConfig.get().cost().tpRandom().numUpper());
                cost.setExp(CommonConfig.get().cost().tpRandom().exp());
                break;
            case TP_SPAWN:
                cost.setType(CommonConfig.get().cost().tpSpawn().type());
                cost.setNum(CommonConfig.get().cost().tpSpawn().num());
                cost.setRate(CommonConfig.get().cost().tpSpawn().rate());
                cost.setConf(CommonConfig.get().cost().tpSpawn().conf());
                cost.setLower(CommonConfig.get().cost().tpSpawn().numLower());
                cost.setUpper(CommonConfig.get().cost().tpSpawn().numUpper());
                cost.setExp(CommonConfig.get().cost().tpSpawn().exp());
                break;
            case TP_WORLD_SPAWN:
                cost.setType(CommonConfig.get().cost().tpWorldSpawn().type());
                cost.setNum(CommonConfig.get().cost().tpWorldSpawn().num());
                cost.setRate(CommonConfig.get().cost().tpWorldSpawn().rate());
                cost.setConf(CommonConfig.get().cost().tpWorldSpawn().conf());
                cost.setLower(CommonConfig.get().cost().tpWorldSpawn().numLower());
                cost.setUpper(CommonConfig.get().cost().tpWorldSpawn().numUpper());
                cost.setExp(CommonConfig.get().cost().tpWorldSpawn().exp());
                break;
            case TP_TOP:
                cost.setType(CommonConfig.get().cost().tpTop().type());
                cost.setNum(CommonConfig.get().cost().tpTop().num());
                cost.setRate(CommonConfig.get().cost().tpTop().rate());
                cost.setConf(CommonConfig.get().cost().tpTop().conf());
                cost.setLower(CommonConfig.get().cost().tpTop().numLower());
                cost.setUpper(CommonConfig.get().cost().tpTop().numUpper());
                cost.setExp(CommonConfig.get().cost().tpTop().exp());
                break;
            case TP_BOTTOM:
                cost.setType(CommonConfig.get().cost().tpBottom().type());
                cost.setNum(CommonConfig.get().cost().tpBottom().num());
                cost.setRate(CommonConfig.get().cost().tpBottom().rate());
                cost.setConf(CommonConfig.get().cost().tpBottom().conf());
                cost.setLower(CommonConfig.get().cost().tpBottom().numLower());
                cost.setUpper(CommonConfig.get().cost().tpBottom().numUpper());
                cost.setExp(CommonConfig.get().cost().tpBottom().exp());
                break;
            case TP_UP:
                cost.setType(CommonConfig.get().cost().tpUp().type());
                cost.setNum(CommonConfig.get().cost().tpUp().num());
                cost.setRate(CommonConfig.get().cost().tpUp().rate());
                cost.setConf(CommonConfig.get().cost().tpUp().conf());
                cost.setLower(CommonConfig.get().cost().tpUp().numLower());
                cost.setUpper(CommonConfig.get().cost().tpUp().numUpper());
                cost.setExp(CommonConfig.get().cost().tpUp().exp());
                break;
            case TP_DOWN:
                cost.setType(CommonConfig.get().cost().tpDown().type());
                cost.setNum(CommonConfig.get().cost().tpDown().num());
                cost.setRate(CommonConfig.get().cost().tpDown().rate());
                cost.setConf(CommonConfig.get().cost().tpDown().conf());
                cost.setLower(CommonConfig.get().cost().tpDown().numLower());
                cost.setUpper(CommonConfig.get().cost().tpDown().numUpper());
                cost.setExp(CommonConfig.get().cost().tpDown().exp());
                break;
            case TP_VIEW:
                cost.setType(CommonConfig.get().cost().tpView().type());
                cost.setNum(CommonConfig.get().cost().tpView().num());
                cost.setRate(CommonConfig.get().cost().tpView().rate());
                cost.setConf(CommonConfig.get().cost().tpView().conf());
                cost.setLower(CommonConfig.get().cost().tpView().numLower());
                cost.setUpper(CommonConfig.get().cost().tpView().numUpper());
                cost.setExp(CommonConfig.get().cost().tpView().exp());
                break;
            case TP_HOME:
                cost.setType(CommonConfig.get().cost().tpHome().type());
                cost.setNum(CommonConfig.get().cost().tpHome().num());
                cost.setRate(CommonConfig.get().cost().tpHome().rate());
                cost.setConf(CommonConfig.get().cost().tpHome().conf());
                cost.setLower(CommonConfig.get().cost().tpHome().numLower());
                cost.setUpper(CommonConfig.get().cost().tpHome().numUpper());
                cost.setExp(CommonConfig.get().cost().tpHome().exp());
                break;
            case TP_STAGE:
                cost.setType(CommonConfig.get().cost().tpStage().type());
                cost.setNum(CommonConfig.get().cost().tpStage().num());
                cost.setRate(CommonConfig.get().cost().tpStage().rate());
                cost.setConf(CommonConfig.get().cost().tpStage().conf());
                cost.setLower(CommonConfig.get().cost().tpStage().numLower());
                cost.setUpper(CommonConfig.get().cost().tpStage().numUpper());
                cost.setExp(CommonConfig.get().cost().tpStage().exp());
                break;
            case TP_BACK:
                cost.setType(CommonConfig.get().cost().tpBack().type());
                cost.setNum(CommonConfig.get().cost().tpBack().num());
                cost.setRate(CommonConfig.get().cost().tpBack().rate());
                cost.setConf(CommonConfig.get().cost().tpBack().conf());
                cost.setLower(CommonConfig.get().cost().tpBack().numLower());
                cost.setUpper(CommonConfig.get().cost().tpBack().numUpper());
                cost.setExp(CommonConfig.get().cost().tpBack().exp());
                break;
            case TP_GRAVE:
                cost.setType(CommonConfig.get().cost().tpGrave().type());
                cost.setNum(CommonConfig.get().cost().tpGrave().num());
                cost.setRate(CommonConfig.get().cost().tpGrave().rate());
                cost.setConf(CommonConfig.get().cost().tpGrave().conf());
                cost.setLower(CommonConfig.get().cost().tpGrave().numLower());
                cost.setUpper(CommonConfig.get().cost().tpGrave().numUpper());
                cost.setExp(CommonConfig.get().cost().tpGrave().exp());
                break;
            default:
                break;
        }
        return cost;
    }

    public static int getItemCount(List<ItemStack> items, ItemStack itemStack) {
        ItemStack copy = itemStack.copy();
        return items.stream().filter(item -> {
            copy.setCount(item.getCount());
            return ItemStack.matches(item, copy);
        }).mapToInt(ItemStack::getCount).sum();
    }

    public static double calculateDistance(SafeWorldCoordinate safeWorldCoordinate1, SafeWorldCoordinate safeWorldCoordinate2) {
        return safeWorldCoordinate1.distanceFrom(safeWorldCoordinate2);
    }

    // endregion 传送代价

    // region 杂项

    public static final ResourceKey<DamageType> MOD_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.id().create("mod"));

    public static DamageSource getModDamageSource(Level level, @Nullable ServerPlayer player) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(MOD_DAMAGE_TYPE), player);
    }

    /**
     * 强行使玩家死亡
     */
    public static boolean killPlayer(@Nullable ServerPlayer source, ServerPlayer target) {
        try {
            if (target.isSleeping() && !target.level().isClientSide) {
                target.stopSleeping();
            }
            float lethal = target.getHealth() + target.getAbsorptionAmount();
            DamageSource damageSource = getModDamageSource(target.level(), source);
            if (source != null)
                target.getCombatTracker().recordDamage(target.damageSources().playerAttack(source), lethal);
            target.getCombatTracker().recordDamage(damageSource, lethal);
            target.getEntityData().set(((LivingEntityInvoker) target).narcissus$dataHealthId(), 0f);
            ((LivingEntityInvoker) target).narcissus$invokeDie(damageSource);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 播放音效
     *
     * @param player 玩家
     * @param sound  音效
     * @param volume 音量
     * @param pitch  音调
     */
    public static void playSound(ServerPlayer player, ResourceLocation sound, float volume, float pitch) {
        SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(sound);
        if (soundEvent != null) {
            player.playNotifySound(soundEvent, SoundSource.PLAYERS, volume, pitch);
        }
    }

    /**
     * 判断玩家是否被任何敌对生物锁定为攻击目标
     */
    public static boolean isTargetedByHostile(ServerPlayer player) {
        return player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox()
                        .inflate(CommonConfig.get().general().tpWithFollowerRange()))
                .stream()
                .anyMatch(entity -> player.equals(entity.getTarget())
                        || (entity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) && player.equals(entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null))
                );
    }

    public static void setPlayerFlightMode(ServerPlayer player) {
        setPlayerFlightMode(player, null);
    }

    public static void setPlayerFlightMode(ServerPlayer player, Boolean enable) {
        setPlayerFlightMode(player, enable, null);
    }

    public static void setPlayerFlightMode(ServerPlayer player, Boolean enable, Float speed) {
        CompoundTag root = new CompoundTag();
        player.getAbilities().addSaveData(root);
        CompoundTag abilities = root.getCompound("abilities");
        if (enable == null) enable = !abilities.getBoolean("mayfly");
        abilities.putBoolean("mayfly", enable);
        if (!enable) abilities.putBoolean("flying", false);
        if (speed != null) abilities.putFloat("flySpeed", speed);
        player.getAbilities().loadSaveData(root);
        player.connection.send(new ClientboundPlayerAbilitiesPacket(player.getAbilities()));
    }

    // endregion 杂项
}
