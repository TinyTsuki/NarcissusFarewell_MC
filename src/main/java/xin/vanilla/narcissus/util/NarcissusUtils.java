package xin.vanilla.narcissus.util;

import java.util.function.BooleanSupplier;
import lombok.NonNull;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumNotificationVanillaFallback;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.narcissus.Identifier;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.internal.server.NarcissusCostService;
import xin.vanilla.narcissus.internal.server.NarcissusSearchService;
import xin.vanilla.narcissus.data.cost.CostPaymentPlan;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.TeleportCountdownHelper;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.server.teleport.RidingTransfer;
import xin.vanilla.narcissus.mixin.LivingEntityInvoker;
import xin.vanilla.narcissus.mixin.TemptGoalAccessor;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class NarcissusUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    // region 指令相关

    public static String getCommandPrefix() {
        String commandPrefix = CommonConfig.get().command().commandPrefix();
        if (StringUtils.isNullOrEmptyEx(commandPrefix) || !commandPrefix.matches("^(\\w ?)+$")) {
            CommonConfig.get().command().commandPrefix(NarcissusFarewell.DEFAULT_COMMAND_PREFIX);
        }
        return CommonConfig.get().command().commandPrefix().trim();
    }

    /**
     * 判断指令类型是否开启
     *
     * @param type 指令类型
     */
    public static boolean isCommandEnabled(EnumCommandType type) {
        return switch (type) {
            case CARD, SET_CARD, CARD_CONCISE, SET_CARD_CONCISE ->
                    CommonConfig.get().cost().cards().enabled();
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
            case TP_COORDINATE -> CommonConfig.get().command().commandTpCoordinate();
            case TP_STRUCTURE -> CommonConfig.get().command().commandTpStructure();
            case TP_ASK -> CommonConfig.get().command().tpAsk().commandTpAsk();
            case TP_HERE -> CommonConfig.get().command().tpHere().commandTpHere();
            case TP_RANDOM -> CommonConfig.get().command().commandTpRandom();
            case TP_SPAWN -> CommonConfig.get().command().commandTpSpawn();
            case TP_WORLD_SPAWN -> CommonConfig.get().command().commandTpWorldSpawn();
            case TP_TOP -> CommonConfig.get().command().commandTpTop();
            case TP_BOTTOM -> CommonConfig.get().command().commandTpBottom();
            case TP_UP -> CommonConfig.get().command().commandTpUp();
            case TP_DOWN -> CommonConfig.get().command().commandTpDown();
            case TP_VIEW -> CommonConfig.get().command().commandTpView();
            case TP_HOME -> CommonConfig.get().command().tpHome().commandTpHome();
            case TP_STAGE -> CommonConfig.get().command().tpStage().commandTpStage();
            case TP_BACK -> CommonConfig.get().command().commandTpBack();
            case TP_GRAVE -> CommonConfig.get().command().commandTpGrave();
            default -> "";
        };
    }

    public static String getCommand(EnumCommandType type) {
        String prefix = NarcissusUtils.getCommandPrefix();
        switch (type) {
            case HELP:
                return prefix + " help";
            case DIMENSION:
                return prefix + " " + CommonConfig.get().command().commandDimension();
            case DIMENSION_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandDimension() : "";
            case UUID:
                return prefix + " " + CommonConfig.get().command().commandUuid();
            case UUID_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandUuid() : "";
            case CARD:
            case SET_CARD:
                return prefix + " " + CommonConfig.get().command().commandCard();
            case CARD_CONCISE:
            case SET_CARD_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandCard() : "";
            case SHARE:
                return prefix + " " + CommonConfig.get().command().commandShare();
            case SHARE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandShare() : "";
            case FEED:
            case FEED_OTHER:
                return prefix + " " + CommonConfig.get().command().commandFeed();
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandFeed() : "";
            case TP_COORDINATE:
                return prefix + " " + CommonConfig.get().command().commandTpCoordinate();
            case TP_COORDINATE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpCoordinate() : "";
            case TP_STRUCTURE:
                return prefix + " " + CommonConfig.get().command().commandTpStructure();
            case TP_STRUCTURE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpStructure() : "";
            case TP_ASK:
                return prefix + " " + CommonConfig.get().command().tpAsk().commandTpAsk();
            case TP_ASK_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpAsk().commandTpAsk() : "";
            case TP_ASK_YES:
                return prefix + " " + CommonConfig.get().command().tpAsk().commandTpAskYes();
            case TP_ASK_YES_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpAsk().commandTpAskYes() : "";
            case TP_ASK_NO:
                return prefix + " " + CommonConfig.get().command().tpAsk().commandTpAskNo();
            case TP_ASK_NO_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpAsk().commandTpAskNo() : "";
            case TP_ASK_CANCEL:
                return prefix + " " + CommonConfig.get().command().tpAsk().commandTpAskCancel();
            case TP_ASK_CANCEL_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpAsk().commandTpAskCancel() : "";
            case TP_HERE:
                return prefix + " " + CommonConfig.get().command().tpHere().commandTpHere();
            case TP_HERE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpHere().commandTpHere() : "";
            case TP_HERE_YES:
                return prefix + " " + CommonConfig.get().command().tpHere().commandTpHereYes();
            case TP_HERE_YES_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpHere().commandTpHereYes() : "";
            case TP_HERE_NO:
                return prefix + " " + CommonConfig.get().command().tpHere().commandTpHereNo();
            case TP_HERE_NO_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpHere().commandTpHereNo() : "";
            case TP_HERE_CANCEL:
                return prefix + " " + CommonConfig.get().command().tpHere().commandTpHereCancel();
            case TP_HERE_CANCEL_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpHere().commandTpHereCancel() : "";
            case TP_RANDOM:
                return prefix + " " + CommonConfig.get().command().commandTpRandom();
            case TP_RANDOM_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpRandom() : "";
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
                return prefix + " " + CommonConfig.get().command().commandTpSpawn();
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpSpawn() : "";
            case TP_WORLD_SPAWN:
                return prefix + " " + CommonConfig.get().command().commandTpWorldSpawn();
            case TP_WORLD_SPAWN_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpWorldSpawn() : "";
            case TP_TOP:
                return prefix + " " + CommonConfig.get().command().commandTpTop();
            case TP_TOP_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpTop() : "";
            case TP_BOTTOM:
                return prefix + " " + CommonConfig.get().command().commandTpBottom();
            case TP_BOTTOM_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpBottom() : "";
            case TP_UP:
                return prefix + " " + CommonConfig.get().command().commandTpUp();
            case TP_UP_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpUp() : "";
            case TP_DOWN:
                return prefix + " " + CommonConfig.get().command().commandTpDown();
            case TP_DOWN_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpDown() : "";
            case TP_VIEW:
                return prefix + " " + CommonConfig.get().command().commandTpView();
            case TP_VIEW_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpView() : "";
            case TP_HOME:
                return prefix + " " + CommonConfig.get().command().tpHome().commandTpHome();
            case TP_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpHome().commandTpHome() : "";
            case SET_HOME:
                return prefix + " " + CommonConfig.get().command().tpHome().commandSetHome();
            case SET_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpHome().commandSetHome() : "";
            case DEL_HOME:
                return prefix + " " + CommonConfig.get().command().tpHome().commandDelHome();
            case DEL_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpHome().commandDelHome() : "";
            case GET_HOME:
                return prefix + " " + CommonConfig.get().command().tpHome().commandGetHome();
            case GET_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpHome().commandGetHome() : "";
            case TP_STAGE:
                return prefix + " " + CommonConfig.get().command().tpStage().commandTpStage();
            case TP_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpStage().commandTpStage() : "";
            case SET_STAGE:
                return prefix + " " + CommonConfig.get().command().tpStage().commandSetStage();
            case SET_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpStage().commandSetStage() : "";
            case DEL_STAGE:
                return prefix + " " + CommonConfig.get().command().tpStage().commandDelStage();
            case DEL_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpStage().commandDelStage() : "";
            case GET_STAGE:
                return prefix + " " + CommonConfig.get().command().tpStage().commandGetStage();
            case GET_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().tpStage().commandGetStage() : "";
            case TP_BACK:
                return prefix + " " + CommonConfig.get().command().commandTpBack();
            case TP_BACK_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpBack() : "";
            case TP_GRAVE:
                return prefix + " " + CommonConfig.get().command().commandTpGrave();
            case TP_GRAVE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpGrave() : "";
            case FLY:
                return prefix + " " + CommonConfig.get().command().commandFly();
            case FLY_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandFly() : "";
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
                return CommonConfig.get().permission().command().permissionSetCard();
            case FEED_OTHER:
            case FEED_OTHER_CONCISE:
                return CommonConfig.get().permission().command().permissionFeedOther();
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return CommonConfig.get().permission().command().permissionTpCoordinate();
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return CommonConfig.get().permission().command().permissionTpStructure();
            case TP_ASK:
            case TP_ASK_CANCEL:
                // case TP_ASK_YES:
                // case TP_ASK_NO:
            case TP_ASK_CONCISE:
            case TP_ASK_CANCEL_CONCISE:
                // case TP_ASK_YES_CONCISE:
                // case TP_ASK_NO_CONCISE:
                return CommonConfig.get().permission().command().permissionTpAsk();
            case TP_HERE:
            case TP_HERE_CANCEL:
                // case TP_HERE_YES:
                // case TP_HERE_NO:
            case TP_HERE_CONCISE:
            case TP_HERE_CANCEL_CONCISE:
                // case TP_HERE_YES_CONCISE:
                // case TP_HERE_NO_CONCISE:
                return CommonConfig.get().permission().command().permissionTpHere();
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return CommonConfig.get().permission().command().permissionTpRandom();
            case TP_SPAWN:
            case TP_SPAWN_CONCISE:
                return CommonConfig.get().permission().command().permissionTpSpawn();
            case TP_SPAWN_OTHER:
            case TP_SPAWN_OTHER_CONCISE:
                return CommonConfig.get().permission().command().permissionTpSpawnOther();
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return CommonConfig.get().permission().command().permissionTpWorldSpawn();
            case TP_TOP:
            case TP_TOP_CONCISE:
                return CommonConfig.get().permission().command().permissionTpTop();
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return CommonConfig.get().permission().command().permissionTpBottom();
            case TP_UP:
            case TP_UP_CONCISE:
                return CommonConfig.get().permission().command().permissionTpUp();
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return CommonConfig.get().permission().command().permissionTpDown();
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return CommonConfig.get().permission().command().permissionTpView();
            case TP_HOME:
            case SET_HOME:
            case DEL_HOME:
            case GET_HOME:
            case TP_HOME_CONCISE:
            case SET_HOME_CONCISE:
            case DEL_HOME_CONCISE:
            case GET_HOME_CONCISE:
                return CommonConfig.get().permission().command().permissionTpHome();
            case TP_STAGE:
            case TP_STAGE_CONCISE:
                return CommonConfig.get().permission().command().permissionTpStage();
            case SET_STAGE:
            case SET_STAGE_CONCISE:
                return CommonConfig.get().permission().command().permissionTpStageSet();
            case DEL_STAGE:
            case DEL_STAGE_CONCISE:
                return CommonConfig.get().permission().command().permissionTpStageDel();
            case GET_STAGE:
            case GET_STAGE_CONCISE:
                return CommonConfig.get().permission().command().permissionTpStageGet();
            case TP_BACK:
            case TP_BACK_CONCISE:
                return CommonConfig.get().permission().command().permissionTpBack();
            case TP_GRAVE:
            case TP_GRAVE_CONCISE:
                return CommonConfig.get().permission().command().permissionTpGrave();
            case FLY:
            case FLY_CONCISE:
                return CommonConfig.get().permission().command().permissionFly();
            case VIRTUAL_OP:
            case VIRTUAL_OP_CONCISE:
                return CommonConfig.get().permission().command().permissionVirtualOp();
            default:
                return 0;
        }
    }

    public static int getCommandPermissionLevel(EnumTeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return CommonConfig.get().permission().command().permissionTpCoordinate();
            case TP_STRUCTURE:
                return CommonConfig.get().permission().command().permissionTpStructure();
            case TP_ASK:
                return CommonConfig.get().permission().command().permissionTpAsk();
            case TP_HERE:
                return CommonConfig.get().permission().command().permissionTpHere();
            case TP_RANDOM:
                return CommonConfig.get().permission().command().permissionTpRandom();
            case TP_SPAWN:
                return CommonConfig.get().permission().command().permissionTpSpawn();
            case TP_WORLD_SPAWN:
                return CommonConfig.get().permission().command().permissionTpWorldSpawn();
            case TP_TOP:
                return CommonConfig.get().permission().command().permissionTpTop();
            case TP_BOTTOM:
                return CommonConfig.get().permission().command().permissionTpBottom();
            case TP_UP:
                return CommonConfig.get().permission().command().permissionTpUp();
            case TP_DOWN:
                return CommonConfig.get().permission().command().permissionTpDown();
            case TP_VIEW:
                return CommonConfig.get().permission().command().permissionTpView();
            case TP_HOME:
                return CommonConfig.get().permission().command().permissionTpHome();
            case TP_STAGE:
                return CommonConfig.get().permission().command().permissionTpStage();
            case TP_BACK:
                return CommonConfig.get().permission().command().permissionTpBack();
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
            case TP_ASK, TP_ASK_CONCISE -> CommonConfig.get().concise().tpAsk().conciseTpAsk();
            case TP_ASK_YES, TP_ASK_YES_CONCISE -> CommonConfig.get().concise().tpAsk().conciseTpAskYes();
            case TP_ASK_NO, TP_ASK_NO_CONCISE -> CommonConfig.get().concise().tpAsk().conciseTpAskNo();
            case TP_ASK_CANCEL, TP_ASK_CANCEL_CONCISE -> CommonConfig.get().concise().tpAsk().conciseTpAskCancel();
            case TP_HERE, TP_HERE_CONCISE -> CommonConfig.get().concise().tpHere().conciseTpHere();
            case TP_HERE_YES, TP_HERE_YES_CONCISE -> CommonConfig.get().concise().tpHere().conciseTpHereYes();
            case TP_HERE_NO, TP_HERE_NO_CONCISE -> CommonConfig.get().concise().tpHere().conciseTpHereNo();
            case TP_HERE_CANCEL, TP_HERE_CANCEL_CONCISE -> CommonConfig.get().concise().tpHere().conciseTpHereCancel();
            case TP_RANDOM, TP_RANDOM_CONCISE -> CommonConfig.get().concise().conciseTpRandom();
            case TP_SPAWN, TP_SPAWN_OTHER, TP_SPAWN_CONCISE, TP_SPAWN_OTHER_CONCISE ->
                    CommonConfig.get().concise().conciseTpSpawn();
            case TP_WORLD_SPAWN, TP_WORLD_SPAWN_CONCISE -> CommonConfig.get().concise().conciseTpWorldSpawn();
            case TP_TOP, TP_TOP_CONCISE -> CommonConfig.get().concise().conciseTpTop();
            case TP_BOTTOM, TP_BOTTOM_CONCISE -> CommonConfig.get().concise().conciseTpBottom();
            case TP_UP, TP_UP_CONCISE -> CommonConfig.get().concise().conciseTpUp();
            case TP_DOWN, TP_DOWN_CONCISE -> CommonConfig.get().concise().conciseTpDown();
            case TP_VIEW, TP_VIEW_CONCISE -> CommonConfig.get().concise().conciseTpView();
            case TP_HOME, TP_HOME_CONCISE -> CommonConfig.get().concise().tpHome().conciseTpHome();
            case SET_HOME, SET_HOME_CONCISE -> CommonConfig.get().concise().tpHome().conciseSetHome();
            case DEL_HOME, DEL_HOME_CONCISE -> CommonConfig.get().concise().tpHome().conciseDelHome();
            case GET_HOME, GET_HOME_CONCISE -> CommonConfig.get().concise().tpHome().conciseGetHome();
            case TP_STAGE, TP_STAGE_CONCISE -> CommonConfig.get().concise().tpStage().conciseTpStage();
            case SET_STAGE, SET_STAGE_CONCISE -> CommonConfig.get().concise().tpStage().conciseSetStage();
            case DEL_STAGE, DEL_STAGE_CONCISE -> CommonConfig.get().concise().tpStage().conciseDelStage();
            case GET_STAGE, GET_STAGE_CONCISE -> CommonConfig.get().concise().tpStage().conciseGetStage();
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

    public static SafeWorldCoordinate findTopCandidate(ServerLevel world, SafeWorldCoordinate start) {
        return new SafeCoordinateFinder(world).findTopCandidate(start);
    }

    public static SafeWorldCoordinate findBottomCandidate(ServerLevel world, SafeWorldCoordinate start) {
        return new SafeCoordinateFinder(world).findBottomCandidate(start);
    }

    public static SafeWorldCoordinate findUpCandidate(ServerLevel world, SafeWorldCoordinate start) {
        return new SafeCoordinateFinder(world).findUpCandidate(start);
    }

    public static SafeWorldCoordinate findDownCandidate(ServerLevel world, SafeWorldCoordinate start) {
        return new SafeCoordinateFinder(world).findDownCandidate(start);
    }

    public static SafeWorldCoordinate findViewEndCandidate(ServerPlayer player, boolean safe, int range) {
        return new SafeCoordinateFinder(player.getLevel()).findViewEndCandidate(player, safe, range);
    }

    public static SafeWorldCoordinate findSafeCoordinate(SafeWorldCoordinate safeWorldCoordinate, boolean belowAllowAir) {
        Level world = DimensionUtils.getLevel(safeWorldCoordinate.dimension());
        int chunkX = safeWorldCoordinate.chunkX();
        int chunkZ = safeWorldCoordinate.chunkZ();
        SafeWorldCoordinate result = new SafeCoordinateFinder(world).searchInChunk(safeWorldCoordinate, chunkX, chunkZ, belowAllowAir);
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
        for (String s : CommonConfig.get().base().teleportLimit().teleportBackSkipType()) {
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
    private static TeleportCountdownTracker.Session executeTeleportWithCountdown(ServerPlayer player, EnumTeleportType type, Runnable teleportAction, Runnable cancelAction) {
        MinecraftServer server = player.getServer();
        int sec = TeleportCountdownHelper.getEffectiveCountdownSeconds(player, type);
        if (sec <= 0 || server == null) {
            teleportAction.run();
            return null;
        }
        TeleportCountdownTracker.Session countdownSession = TeleportCountdownTracker.begin(player,
                CommonConfig.get().teleportCountdown().cancelCountdownOnPlayerMove(),
                CommonConfig.get().teleportCountdown().cancelCountdownOnPlayerDamage(), cancelAction);
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
                // 倒计时仍回退到原版操作栏，但允许安装 Banira 的客户端按类型接管
                MessageUtils.sendNotification(p,
                        NarcissusComponent.get().transAuto("tp_countdown_actionbar", String.valueOf(display)),
                        EnumPosition.TOP_CENTER, EnumMoveType.AUTO, 1200L,
                        EnumNotificationStyle.NORMAL, EnumNotificationVanillaFallback.ACTION_BAR,
                        NarcissusNotificationTypes.TELEPORT_GUARD);
            });
        }
        BaniraScheduler.scheduleAfterMillis(server, sec * 1000.0, () -> {
            if (!countdownSession.tryMarkCompleteAndRemove()) {
                return;
            }
            teleportAction.run();
        });
        return countdownSession;
    }

    /**
     * 检查传送范围
     */
    public static int checkRange(ServerPlayer player, EnumTeleportType type, int range) {
        int maxRange;
        switch (type) {
            case TP_VIEW:
                maxRange = CommonConfig.get().base().teleportLimit().teleportViewDistanceLimit();
                break;
            default:
                maxRange = CommonConfig.get().base().randomTeleport().teleportRandomDistanceLimit();
                break;
        }
        if (range > maxRange) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("range_too_large", maxRange), NarcissusNotificationTypes.TELEPORT_ERROR);
        } else if (range <= 0) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("range_too_small", 1), NarcissusNotificationTypes.TELEPORT_ERROR);
        }
        return Math.min(Math.max(range, 1), maxRange);
    }

    /**
     * 执行传送请求
     */
    public static void teleportTo(@NonNull TeleportRequest request) {
        ServerPlayer moving = request.getTeleportType() == EnumTeleportType.TP_HERE ? request.getTarget() : request.getRequester();
        ServerPlayer endpoint = request.getTeleportType() == EnumTeleportType.TP_HERE ? request.getRequester() : request.getTarget();
        teleportTo(moving, new SafeWorldCoordinate(endpoint).safe(request.isSafe()), request.getTeleportType(), -1,
                request.getRequester(), request.getTarget(), request, () -> { }, () -> true, null);
    }

    /**
     * 传送玩家到指定玩家
     *
     * @param from 传送者
     * @param to   目标玩家
     */
    public static void teleportTo(@NonNull ServerPlayer from, @NonNull ServerPlayer to, EnumTeleportType type, boolean safe) {
        teleportTo(type == EnumTeleportType.TP_HERE ? to : from,
                new SafeWorldCoordinate(type == EnumTeleportType.TP_HERE ? from : to).safe(safe), type, -1,
                from, to, null, () -> { }, () -> true, null);
    }

    /**
     * 传送玩家到指定坐标
     *
     * @param player 玩家
     * @param after  坐标
     */
    public static void teleportTo(@NonNull ServerPlayer player, @NonNull SafeWorldCoordinate after, EnumTeleportType type) {
        teleportTo(player, after, type, -1);
    }

    /**
     * @param tprHorizontalRange 仅用于随机传送安全搜索失败时的重试
     */
    public static void teleportTo(@NonNull ServerPlayer player, @NonNull SafeWorldCoordinate after, EnumTeleportType type, int tprHorizontalRange) {
        teleportTo(player, after, type, tprHorizontalRange, player, null, null, () -> { }, () -> true, null);
    }

    public static void teleportTo(ServerPlayer player, SafeWorldCoordinate after, EnumTeleportType type, Runnable onSuccess) {
        teleportTo(player, after, type, onSuccess, () -> true);
    }

    public static void teleportTo(ServerPlayer player, SafeWorldCoordinate after, EnumTeleportType type,
                                  Runnable onSuccess, BooleanSupplier targetValid) {
        teleportTo(player, after, type, -1, player, null, null, onSuccess, targetValid, null);
    }

    public static NarcissusCostService.Ticket beginTeleport(ServerPlayer player, EnumTeleportType type) {
        NarcissusCostService service = NarcissusCostService.get();
        if (service == null) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("cost_unavailable"), NarcissusNotificationTypes.TELEPORT_ERROR);
            return null;
        }
        try { return service.begin(player, player, null, null, type); }
        catch (RuntimeException error) {
            LOGGER.error("Teleport configuration unavailable", error);
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("cost_unavailable"), NarcissusNotificationTypes.TELEPORT_ERROR);
            return null;
        }
    }

    public static void teleportTo(ServerPlayer player, SafeWorldCoordinate after, EnumTeleportType type,
                                  NarcissusCostService.Ticket prepared) {
        teleportTo(player, after, type, -1, player, null, null, () -> { }, () -> true, prepared);
    }

    private static void teleportTo(ServerPlayer player, SafeWorldCoordinate after, EnumTeleportType type,
                                   int tprHorizontalRange, ServerPlayer payer, ServerPlayer targetPlayer,
                                   TeleportRequest request, Runnable onSuccess, BooleanSupplier targetValid, NarcissusCostService.Ticket prepared) {
        NarcissusCostService service = NarcissusCostService.get();
        if (service == null) {
            MessageUtils.sendNotification(payer, NarcissusComponent.get().transAuto("cost_unavailable"), NarcissusNotificationTypes.TELEPORT_ERROR);
            return;
        }
        NarcissusCostService.Ticket ticket;
        try { ticket = prepared != null ? prepared : service.begin(player, payer, targetPlayer, request, type); }
        catch (RuntimeException error) {
            LOGGER.error("Teleport configuration unavailable", error);
            MessageUtils.sendNotification(payer, NarcissusComponent.get().transAuto("cost_unavailable"), NarcissusNotificationTypes.TELEPORT_ERROR);
            return;
        }
        if (ticket == null || !ticket.live()) return;
        ticket.targetGuard(targetValid);
        SafeWorldCoordinate before = new SafeWorldCoordinate(player);
        Level world = player.level;
        if (world != null) {
            ServerLevel level = DimensionUtils.getLevel(after.dimension());
            if (level != null) {
                if (after.safe()) {
                    MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("safe_searching"), NarcissusNotificationTypes.TELEPORT_SEARCH);
                    NarcissusSearchService search = NarcissusSearchService.get();
                    if (search == null) {
                        ticket.cancel();
                        MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("search_failed"), NarcissusNotificationTypes.TELEPORT_ERROR);
                        return;
                    }
                    search.searchDestination(player, after, type, tprHorizontalRange, ticket,
                            session -> finishSearch(search, session, ticket, player, payer, type, before, level, onSuccess));
                } else {
                    if (!ticket.resolve(after)) { ticket.cancel(); return; }
                    executeTeleportWithCountdown(player, type,
                            () -> finishTeleport(ticket, player, payer, type, before, level, null, onSuccess), ticket::cancel);
                }
            } else ticket.cancel();
        } else ticket.cancel();
    }

    public static void teleportSearched(ServerPlayer player, NarcissusSearchService.Session session,
                                        EnumTeleportType type, NarcissusCostService.Ticket ticket) {
        NarcissusSearchService search = NarcissusSearchService.get();
        if (search == null || !ticket.live()) { session.cancel(); return; }
        SafeWorldCoordinate after = session.destination();
        SafeWorldCoordinate before = new SafeWorldCoordinate(player);
        ServerLevel level = player.server.getLevel(after.dimension());
        if (level == null) { session.cancel(); return; }
        if (after.safe()) {
            search.continueDestination(session, after, type, -1,
                    next -> finishSearch(search, next, ticket, player, player, type, before, level, () -> { }));
        } else finishSearch(search, session, ticket, player, player, type, before, level, () -> { });
    }

    private static void finishSearch(NarcissusSearchService search, NarcissusSearchService.Session session,
                                      NarcissusCostService.Ticket ticket, ServerPlayer player, ServerPlayer payer,
                                      EnumTeleportType type, SafeWorldCoordinate before, ServerLevel level, Runnable onSuccess) {
        ItemStack supportItem = session.supportItem();
        if (!supportItem.isEmpty()) ticket.requireSupportItem(supportItem);
        if (!ticket.resolve(session.destination())) { session.cancel(); return; }
        BlockState supportState = session.supportState();
        Runnable supportBlock = supportState == null ? null : () -> {
            if (!supportItem.isEmpty() && !ItemUtils.removePlayerItem(player, supportItem.copy())) {
                throw new IllegalStateException("Teleport support item disappeared after payment");
            }
            if (!level.setBlockAndUpdate(session.destination().toBlockPos().below(), supportState.getBlock().defaultBlockState())) {
                throw new IllegalStateException("Teleport support block could not be placed");
            }
        };
        session.waitForCountdown();
        TeleportCountdownTracker.Session countdown = executeTeleportWithCountdown(player, type,
                () -> search.complete(session, () -> finishTeleport(ticket, player, payer, type, before, level, supportBlock, onSuccess)), session::cancel);
        session.attachCountdown(countdown);
    }

    private static void finishTeleport(NarcissusCostService.Ticket ticket, ServerPlayer player,
                                       ServerPlayer payer, EnumTeleportType type, SafeWorldCoordinate before,
                                       ServerLevel level, Runnable supportBlock, Runnable onSuccess) {
        CostPaymentPlan plan = ticket.commit();
        if (!plan.isCommitted()) {
            MessageUtils.sendNotification(payer, NarcissusComponent.get().transAuto("cost_failure",
                    plan.failure().enumDescription()), NarcissusNotificationTypes.TELEPORT_ERROR);
            return;
        }
        if (supportBlock != null) supportBlock.run();
        if (!teleportPlayer(player, ticket.destination(), type, before, level)) return;
        ticket.completed();
        onSuccess.run();
    }

    private static boolean teleportPlayer(@NonNull ServerPlayer player, @NonNull SafeWorldCoordinate after, EnumTeleportType type, SafeWorldCoordinate before, ServerLevel level) {
        ResourceLocation sound = Identifier.id().parse(CommonConfig.get().base().other().tpSound());
        NarcissusUtils.playSound(player, sound, 1.0f, 1.0f);
        after.y(Math.floor(after.y()) + 0.1);

        List<Entity> followers = collectFollowers(player);
        boolean withVehicle = CommonConfig.get().base().teleportTogether().tpWithVehicle();
        Entity root = withVehicle ? player.getRootVehicle() : player;
        Set<Entity> transferred = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean moved;
        try {
            moved = RidingTransfer.transfer(root, player, new RidingTransfer.Backend<Entity>() {
                public List<Entity> passengers(Entity entity) { return withVehicle ? entity.getPassengers() : Collections.emptyList(); }
                public List<Entity> attachmentOrder(Entity vehicle, List<Entity> passengers) {
                    if (vehicle.getControllingPassenger() instanceof Player) return passengers;
                    // Native addPassenger prepends players on vehicles without a player controller.
                    List<Entity> order = new ArrayList<>(passengers.size());
                    for (Entity passenger : passengers) {
                        if (passenger instanceof Player) order.add(0, passenger);
                        else order.add(passenger);
                    }
                    return order;
                }
                public Entity vehicle(Entity entity) { return entity.getVehicle(); }
                public void detach(Entity entity) { entity.stopRiding(); }
                public Entity move(Entity entity) {
                    transferred.add(entity);
                    return doTeleport(entity, after, level);
                }
                public boolean alive(Entity entity) { return entity != null && !entity.isRemoved(); }
                public boolean atDestination(Entity entity) {
                    return entity.level == level && Math.abs(entity.getX() - after.x()) < 0.001
                            && Math.abs(entity.getY() - after.y()) < 0.001 && Math.abs(entity.getZ() - after.z()) < 0.001;
                }
                public boolean sameWorld(Entity first, Entity second) { return first.level == second.level; }
                public boolean attach(Entity entity, Entity vehicle) { return entity.startRiding(vehicle, true); }
            });
        } catch (RuntimeException error) {
            LOGGER.error("Teleport riding transfer failed for {}", player.getUUID(), error);
            moved = false;
        }
        if (!moved) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("teleport_failed"), NarcissusNotificationTypes.TELEPORT_ERROR);
            return false;
        }
        for (Entity follower : followers) {
            if (transferred.add(follower) && !follower.isRemoved()) {
                try {
                    Entity result = doTeleport(follower, after, level);
                    if (result == null || result.isRemoved() || result.level != level) LOGGER.warn("Follower transfer failed for {}", follower.getUUID());
                } catch (RuntimeException error) {
                    LOGGER.warn("Follower transfer failed for {}", follower.getUUID(), error);
                }
            }
        }

        NarcissusUtils.playSound(player, sound, 1.0f, 1.0f);
        TeleportRecord record = new TeleportRecord();
        record.setTeleportTime(new Date());
        record.setTeleportType(type);
        record.setBefore(before);
        record.setAfter(after);
        PlayerTeleportData.getData(player).addTeleportRecords(record);
        PlayerTeleportData.syncPlayerData(player);
        return true;
    }

    /**
     * 传送跟随的实体
     */
    private static List<Entity> collectFollowers(@NonNull ServerPlayer player) {
        if (!CommonConfig.get().base().teleportTogether().tpWithFollower()) return Collections.emptyList();
        Set<Entity> followers = new LinkedHashSet<>();

        int followerRange = CommonConfig.get().base().teleportTogether().tpWithFollowerRange();

        // 传送主动跟随的实体
        for (TamableAnimal entity : player.level.getEntitiesOfClass(TamableAnimal.class, player.getBoundingBox().inflate(followerRange))) {
            if (entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(player.getUUID()) && !entity.isOrderedToSit()) {
                followers.add(entity);
            }
        }

        // 传送拴绳实体
        for (Mob entity : player.level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(followerRange))) {
            if (entity.getLeashHolder() == player) {
                followers.add(entity);
            }
        }

        // 传送被吸引的非敌对实体
        for (Mob entity : player.level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(followerRange))) {
            // 排除敌对生物
            if (entity instanceof Monster) continue;

            if (entity.goalSelector.getRunningGoals()
                    .anyMatch(goal -> goal.isRunning()
                            && (goal.getGoal() instanceof TemptGoal)
                            && ((TemptGoalAccessor) goal.getGoal()).narcissus$player() == player
                    )) {
                followers.add(entity);
            }
        }
        return new ArrayList<>(followers);
    }

    private static Entity doTeleport(@NonNull Entity entity, @NonNull SafeWorldCoordinate safeWorldCoordinate, ServerLevel level) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(level, safeWorldCoordinate.x(), safeWorldCoordinate.y(), safeWorldCoordinate.z()
                    , safeWorldCoordinate.yaw() == 0 ? player.getYRot() : (float) safeWorldCoordinate.yaw()
                    , safeWorldCoordinate.pitch() == 0 ? player.getXRot() : (float) safeWorldCoordinate.pitch());
        } else {
            if (level == entity.level) {
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
                        if (newEntity == null) return null;
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
        if (player.level.dimension() != to) {
            if (CommonConfig.get().base().teleportLimit().teleportAcrossDimension()) {
                if (!NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, type)) {
                    result = false;
                    MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("across_dimension_not_enable_for", getCommand(type)), NarcissusNotificationTypes.TELEPORT_ERROR);
                }
            } else {
                result = false;
                MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("across_dimension_not_enable"), NarcissusNotificationTypes.TELEPORT_ERROR);
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
                permission = CommonConfig.get().permission().across().permissionTpCoordinateAcrossDimension();
                break;
            case TP_STRUCTURE:
                permission = CommonConfig.get().permission().across().permissionTpStructureAcrossDimension();
                break;
            case TP_ASK:
                permission = CommonConfig.get().permission().across().permissionTpAskAcrossDimension();
                break;
            case TP_HERE:
                permission = CommonConfig.get().permission().across().permissionTpHereAcrossDimension();
                break;
            case TP_RANDOM:
                permission = CommonConfig.get().permission().across().permissionTpRandomAcrossDimension();
                break;
            case TP_SPAWN:
                permission = CommonConfig.get().permission().across().permissionTpSpawnAcrossDimension();
                break;
            case TP_WORLD_SPAWN:
                permission = CommonConfig.get().permission().across().permissionTpWorldSpawnAcrossDimension();
                break;
            case TP_HOME:
                permission = CommonConfig.get().permission().across().permissionTpHomeAcrossDimension();
                break;
            case TP_STAGE:
                permission = CommonConfig.get().permission().across().permissionTpStageAcrossDimension();
                break;
            case TP_BACK:
                permission = CommonConfig.get().permission().across().permissionTpBackAcrossDimension();
                break;
            case TP_GRAVE:
                permission = CommonConfig.get().permission().across().permissionTpGraveAcrossDimension();
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
        return getTeleportCoolDown(player, type, null);
    }

    public static int getTeleportCoolDown(ServerPlayer player, EnumTeleportType type, TeleportRequest excluded) {
        Instant current = Instant.now();
        int commandCoolDown = getCommandCoolDown(type);
        List<TeleportRecord> records = PlayerTeleportData.getData(player).peekTeleportRecords();
        Instant lastTpTime = records.stream().filter(record -> record.getTeleportType() == type)
                .map(TeleportRecord::getTeleportTime)
                .max(Comparator.comparing(Date::toInstant))
                .orElse(new Date(0)).toInstant();
        switch (CommonConfig.get().base().teleportRequest().teleportRequestCooldownType()) {
            case COMMON:
                return calculateCooldown(player.getUUID(), current, latestTeleportTime(records), CommonConfig.get().base().teleportRequest().teleportRequestCooldown(), null, excluded);
            case INDIVIDUAL:
                return calculateCooldown(player.getUUID(), current, lastTpTime, commandCoolDown, type, excluded);
            case MIXED:
                int globalCommandCoolDown = CommonConfig.get().base().teleportRequest().teleportRequestCooldown();
                int individualCooldown = calculateCooldown(player.getUUID(), current, lastTpTime, commandCoolDown, type, excluded);
                int globalCooldown = calculateCooldown(player.getUUID(), current, latestTeleportTime(records), globalCommandCoolDown, null, excluded);
                return Math.max(individualCooldown, globalCooldown);
            default:
                return 0;
        }
    }

    private static Instant latestTeleportTime(List<TeleportRecord> records) {
        return records.stream().map(TeleportRecord::getTeleportTime).max(Comparator.comparing(Date::toInstant))
                .orElse(new Date(0)).toInstant();
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

    private static int calculateCooldown(UUID uuid, Instant current, Instant lastTpTime, int cooldown, EnumTeleportType type, TeleportRequest excluded) {
        Optional<TeleportRequest> latestRequest = NarcissusFarewell.getTeleportRequest().values().stream()
                .filter(request -> request != excluded)
                .filter(request -> request.getRequester().getUUID().equals(uuid))
                .filter(request -> type == null || request.getTeleportType() == type)
                .max(Comparator.comparing(TeleportRequest::getRequestTime));

        Instant lastRequestTime = latestRequest.map(r -> r.getRequestTime().toInstant()).orElse(current.minusSeconds(cooldown));
        return Math.max(0, Math.max(cooldown - (int) Duration.between(lastRequestTime, current).getSeconds(), cooldown - (int) Duration.between(lastTpTime, current).getSeconds()));
    }

    // endregion 传送冷却



    // region 杂项

    public static final DamageSource damageSource = new DamageSource(NarcissusFarewell.MODID) {
        @Nonnull
        @Override
        public net.minecraft.network.chat.Component getLocalizedDeathMessage(@Nonnull LivingEntity entity) {
            return net.minecraft.network.chat.Component.empty();
        }
    }.bypassArmor().bypassMagic().bypassInvul();

    /**
     * 强行使玩家死亡
     */
    public static boolean killPlayer(@Nullable ServerPlayer source, ServerPlayer target) {
        try {
            if (target.isSleeping() && !target.level.isClientSide) {
                target.stopSleeping();
            }
            float lethal = target.getHealth() + target.getAbsorptionAmount();
            if (source != null) target.getCombatTracker().recordDamage(DamageSource.playerAttack(source), lethal, 0f);
            target.getCombatTracker().recordDamage(damageSource, lethal, 0f);
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
        return player.level.getEntitiesOfClass(Mob.class, player.getBoundingBox()
                        .inflate(CommonConfig.get().base().teleportTogether().tpWithFollowerRange()))
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
