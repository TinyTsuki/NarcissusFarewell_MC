package xin.vanilla.narcissus.util;

import com.mojang.brigadier.StringReader;
import lombok.NonNull;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.ItemInput;
import net.minecraft.command.arguments.ItemParser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.play.server.SPlayerAbilitiesPacket;
import net.minecraft.network.play.server.SSetPassengersPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.StringUtils;
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
import xin.vanilla.narcissus.internal.forge.network.ForgeNativePacketSender;
import xin.vanilla.narcissus.mixin.LivingEntityInvoker;
import xin.vanilla.narcissus.mixin.TemptGoalAccessor;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
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
        switch (type) {
            case CARD:
            case SET_CARD:
            case CARD_CONCISE:
            case SET_CARD_CONCISE:
                return CommonConfig.get().base().teleportCard().teleportCard();
            case SHARE:
            case SHARE_CONCISE:
                return CommonConfig.get().featureSwitch().switchShare();
            case FEED:
            case FEED_OTHER:
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return CommonConfig.get().featureSwitch().switchFeed();
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpCoordinate();
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpStructure();
            case TP_ASK:
            case TP_ASK_YES:
            case TP_ASK_NO:
            case TP_ASK_CANCEL:
            case TP_ASK_CONCISE:
            case TP_ASK_YES_CONCISE:
            case TP_ASK_NO_CONCISE:
            case TP_ASK_CANCEL_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpAsk();
            case TP_HERE:
            case TP_HERE_YES:
            case TP_HERE_NO:
            case TP_HERE_CANCEL:
            case TP_HERE_CONCISE:
            case TP_HERE_YES_CONCISE:
            case TP_HERE_NO_CONCISE:
            case TP_HERE_CANCEL_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpHere();
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpRandom();
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpSpawn();
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpWorldSpawn();
            case TP_TOP:
            case TP_TOP_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpTop();
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpBottom();
            case TP_UP:
            case TP_UP_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpUp();
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpDown();
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpView();
            case TP_HOME:
            case SET_HOME:
            case DEL_HOME:
            case GET_HOME:
            case TP_HOME_CONCISE:
            case SET_HOME_CONCISE:
            case DEL_HOME_CONCISE:
            case GET_HOME_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpHome();
            case TP_STAGE:
            case SET_STAGE:
            case DEL_STAGE:
            case GET_STAGE:
            case TP_STAGE_CONCISE:
            case SET_STAGE_CONCISE:
            case DEL_STAGE_CONCISE:
            case GET_STAGE_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpStage();
            case TP_BACK:
            case TP_BACK_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpBack();
            case TP_GRAVE:
            case TP_GRAVE_CONCISE:
                return CommonConfig.get().featureSwitch().switchTpGrave();
            case FLY:
            case FLY_CONCISE:
                return CommonConfig.get().featureSwitch().switchFly();
            default:
                return true;
        }
    }

    public static String getCommand(EnumTeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return CommonConfig.get().command().commandTpCoordinate();
            case TP_STRUCTURE:
                return CommonConfig.get().command().commandTpStructure();
            case TP_ASK:
                return CommonConfig.get().command().commandTpAsk();
            case TP_HERE:
                return CommonConfig.get().command().commandTpHere();
            case TP_RANDOM:
                return CommonConfig.get().command().commandTpRandom();
            case TP_SPAWN:
                return CommonConfig.get().command().commandTpSpawn();
            case TP_WORLD_SPAWN:
                return CommonConfig.get().command().commandTpWorldSpawn();
            case TP_TOP:
                return CommonConfig.get().command().commandTpTop();
            case TP_BOTTOM:
                return CommonConfig.get().command().commandTpBottom();
            case TP_UP:
                return CommonConfig.get().command().commandTpUp();
            case TP_DOWN:
                return CommonConfig.get().command().commandTpDown();
            case TP_VIEW:
                return CommonConfig.get().command().commandTpView();
            case TP_HOME:
                return CommonConfig.get().command().commandTpHome();
            case TP_STAGE:
                return CommonConfig.get().command().commandTpStage();
            case TP_BACK:
                return CommonConfig.get().command().commandTpBack();
            case TP_GRAVE:
                return CommonConfig.get().command().commandTpGrave();
            default:
                return "";
        }
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
                return prefix + " " + CommonConfig.get().command().commandTpAsk();
            case TP_ASK_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpAsk() : "";
            case TP_ASK_YES:
                return prefix + " " + CommonConfig.get().command().commandTpAskYes();
            case TP_ASK_YES_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpAskYes() : "";
            case TP_ASK_NO:
                return prefix + " " + CommonConfig.get().command().commandTpAskNo();
            case TP_ASK_NO_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpAskNo() : "";
            case TP_ASK_CANCEL:
                return prefix + " " + CommonConfig.get().command().commandTpAskCancel();
            case TP_ASK_CANCEL_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpAskCancel() : "";
            case TP_HERE:
                return prefix + " " + CommonConfig.get().command().commandTpHere();
            case TP_HERE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpHere() : "";
            case TP_HERE_YES:
                return prefix + " " + CommonConfig.get().command().commandTpHereYes();
            case TP_HERE_YES_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpHereYes() : "";
            case TP_HERE_NO:
                return prefix + " " + CommonConfig.get().command().commandTpHereNo();
            case TP_HERE_NO_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpHereNo() : "";
            case TP_HERE_CANCEL:
                return prefix + " " + CommonConfig.get().command().commandTpHereCancel();
            case TP_HERE_CANCEL_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpHereCancel() : "";
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
                return prefix + " " + CommonConfig.get().command().commandTpHome();
            case TP_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpHome() : "";
            case SET_HOME:
                return prefix + " " + CommonConfig.get().command().commandSetHome();
            case SET_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandSetHome() : "";
            case DEL_HOME:
                return prefix + " " + CommonConfig.get().command().commandDelHome();
            case DEL_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandDelHome() : "";
            case GET_HOME:
                return prefix + " " + CommonConfig.get().command().commandGetHome();
            case GET_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandGetHome() : "";
            case TP_STAGE:
                return prefix + " " + CommonConfig.get().command().commandTpStage();
            case TP_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandTpStage() : "";
            case SET_STAGE:
                return prefix + " " + CommonConfig.get().command().commandSetStage();
            case SET_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandSetStage() : "";
            case DEL_STAGE:
                return prefix + " " + CommonConfig.get().command().commandDelStage();
            case DEL_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandDelStage() : "";
            case GET_STAGE:
                return prefix + " " + CommonConfig.get().command().commandGetStage();
            case GET_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.get().command().commandGetStage() : "";
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
        switch (type) {
            case LANGUAGE:
            case LANGUAGE_CONCISE:
                return CommonConfig.get().concise().conciseLanguage();
            case UUID:
            case UUID_CONCISE:
                return CommonConfig.get().concise().conciseUuid();
            case DIMENSION:
            case DIMENSION_CONCISE:
                return CommonConfig.get().concise().conciseDimension();
            case CARD:
            case CARD_CONCISE:
            case SET_CARD:
            case SET_CARD_CONCISE:
                return CommonConfig.get().concise().conciseCard();
            case SHARE:
            case SHARE_CONCISE:
                return CommonConfig.get().concise().conciseShare();
            case FEED:
            case FEED_OTHER:
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return CommonConfig.get().concise().conciseFeed();
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return CommonConfig.get().concise().conciseTpCoordinate();
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return CommonConfig.get().concise().conciseTpStructure();
            case TP_ASK:
            case TP_ASK_CONCISE:
                return CommonConfig.get().concise().conciseTpAsk();
            case TP_ASK_YES:
            case TP_ASK_YES_CONCISE:
                return CommonConfig.get().concise().conciseTpAskYes();
            case TP_ASK_NO:
            case TP_ASK_NO_CONCISE:
                return CommonConfig.get().concise().conciseTpAskNo();
            case TP_ASK_CANCEL:
            case TP_ASK_CANCEL_CONCISE:
                return CommonConfig.get().concise().conciseTpAskCancel();
            case TP_HERE:
            case TP_HERE_CONCISE:
                return CommonConfig.get().concise().conciseTpHere();
            case TP_HERE_YES:
            case TP_HERE_YES_CONCISE:
                return CommonConfig.get().concise().conciseTpHereYes();
            case TP_HERE_NO:
            case TP_HERE_NO_CONCISE:
                return CommonConfig.get().concise().conciseTpHereNo();
            case TP_HERE_CANCEL:
            case TP_HERE_CANCEL_CONCISE:
                return CommonConfig.get().concise().conciseTpHereCancel();
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return CommonConfig.get().concise().conciseTpRandom();
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return CommonConfig.get().concise().conciseTpSpawn();
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return CommonConfig.get().concise().conciseTpWorldSpawn();
            case TP_TOP:
            case TP_TOP_CONCISE:
                return CommonConfig.get().concise().conciseTpTop();
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return CommonConfig.get().concise().conciseTpBottom();
            case TP_UP:
            case TP_UP_CONCISE:
                return CommonConfig.get().concise().conciseTpUp();
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return CommonConfig.get().concise().conciseTpDown();
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return CommonConfig.get().concise().conciseTpView();
            case TP_HOME:
            case TP_HOME_CONCISE:
                return CommonConfig.get().concise().conciseTpHome();
            case SET_HOME:
            case SET_HOME_CONCISE:
                return CommonConfig.get().concise().conciseSetHome();
            case DEL_HOME:
            case DEL_HOME_CONCISE:
                return CommonConfig.get().concise().conciseDelHome();
            case GET_HOME:
            case GET_HOME_CONCISE:
                return CommonConfig.get().concise().conciseGetHome();
            case TP_STAGE:
            case TP_STAGE_CONCISE:
                return CommonConfig.get().concise().conciseTpStage();
            case SET_STAGE:
            case SET_STAGE_CONCISE:
                return CommonConfig.get().concise().conciseSetStage();
            case DEL_STAGE:
            case DEL_STAGE_CONCISE:
                return CommonConfig.get().concise().conciseDelStage();
            case GET_STAGE:
            case GET_STAGE_CONCISE:
                return CommonConfig.get().concise().conciseGetStage();
            case TP_BACK:
            case TP_BACK_CONCISE:
                return CommonConfig.get().concise().conciseTpBack();
            case TP_GRAVE:
            case TP_GRAVE_CONCISE:
                return CommonConfig.get().concise().conciseTpGrave();
            case FLY:
            case FLY_CONCISE:
                return CommonConfig.get().concise().conciseFly();
            case VIRTUAL_OP:
            case VIRTUAL_OP_CONCISE:
                return CommonConfig.get().concise().conciseVirtualOp();
            default:
                return false;
        }
    }

    public static boolean hasCommandPermission(CommandSource source, EnumCommandType type) {
        return source.hasPermission(getCommandPermissionLevel(type)) || CommandUtils.hasVirtualPermission(source.getEntity(), type);
    }

    // endregion 指令相关

    // region 安全坐标

    public static SafeWorldCoordinate findTopCandidate(ServerWorld world, SafeWorldCoordinate start) {
        return new SafeCoordinateFinder(world).findTopCandidate(start);
    }

    public static SafeWorldCoordinate findBottomCandidate(ServerWorld world, SafeWorldCoordinate start) {
        return new SafeCoordinateFinder(world).findBottomCandidate(start);
    }

    public static SafeWorldCoordinate findUpCandidate(ServerWorld world, SafeWorldCoordinate start) {
        return new SafeCoordinateFinder(world).findUpCandidate(start);
    }

    public static SafeWorldCoordinate findDownCandidate(ServerWorld world, SafeWorldCoordinate start) {
        return new SafeCoordinateFinder(world).findDownCandidate(start);
    }

    public static SafeWorldCoordinate findViewEndCandidate(ServerPlayerEntity player, boolean safe, int range) {
        return new SafeCoordinateFinder(player.getLevel()).findViewEndCandidate(player, safe, range);
    }

    public static SafeWorldCoordinate findSafeCoordinate(SafeWorldCoordinate safeWorldCoordinate, boolean belowAllowAir) {
        World world = DimensionUtils.getLevel(safeWorldCoordinate.dimension());
        int chunkX = safeWorldCoordinate.chunkX();
        int chunkZ = safeWorldCoordinate.chunkZ();
        SafeWorldCoordinate result = new SafeCoordinateFinder(world).searchInChunk(safeWorldCoordinate, chunkX, chunkZ, belowAllowAir);
        LOGGER.debug("Target:{}, {}, {} | Safe:{}, {}, {}", safeWorldCoordinate.xInt(), safeWorldCoordinate.yInt(), safeWorldCoordinate.zInt(), result == null ? "null" : result.xInt(), result == null ? "null" : result.yInt(), result == null ? "null" : result.zInt());
        return result == null ? safeWorldCoordinate : result;
    }

    // endregion 安全坐标

    // region 坐标查找

    public static String getHomeDimensionByName(ServerPlayerEntity player, String name) {
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        List<KeyValue<String, String>> list = data.getHomeCoordinate().keySet().stream()
                .filter(key -> key.value().equals(name))
                .collect(Collectors.toList());
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

    public static KeyValue<String, String> getPlayerHomeKey(ServerPlayerEntity player, RegistryKey<World> dimension, String name) {
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
                .collect(Collectors.toList());

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
    public static SafeWorldCoordinate getPlayerHome(ServerPlayerEntity player, RegistryKey<World> dimension, String name) {
        return PlayerTeleportData.getData(player).getHomeCoordinate().getOrDefault(getPlayerHomeKey(player, dimension, name), null);
    }

    public static String getStageDimensionByName(@Nullable ServerPlayerEntity player, String name) {
        WorldStageData stageData = WorldStageData.get();
        List<KeyValue<String, String>> list = stageData.getStageCoordinate().keySet().stream()
                .filter(key -> key.value().equals(name))
                .collect(Collectors.toList());
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

    public static KeyValue<String, String> getStageKey(ServerPlayerEntity player, RegistryKey<World> dimension, String name) {
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
                    .collect(Collectors.toList());
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
                .collect(Collectors.toList());
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
                .collect(Collectors.toList());
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
                                                                        ServerPlayerEntity player, String dimensionStr) {
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
    public static SafeWorldCoordinate getStageCoordinate(ServerPlayerEntity player, RegistryKey<World> dimension, String name) {
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
    public static TeleportRecord getBackTeleportRecord(ServerPlayerEntity player, @Nullable EnumTeleportType type, @Nullable RegistryKey<World> dimension) {
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

    public static void removeBackTeleportRecord(ServerPlayerEntity player, TeleportRecord record) {
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
    private static void executeTeleportWithCountdown(ServerPlayerEntity player, EnumTeleportType type, Runnable teleportAction) {
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
                ServerPlayerEntity p = server.getPlayerList().getPlayer(uuid);
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
    public static int checkRange(ServerPlayerEntity player, EnumTeleportType type, int range) {
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
        teleportTo(request.getRequester(), request.getTarget(), request.getTeleportType(), request.isSafe());
    }

    /**
     * 传送玩家到指定玩家
     *
     * @param from 传送者
     * @param to   目标玩家
     */
    public static void teleportTo(@NonNull ServerPlayerEntity from, @NonNull ServerPlayerEntity to, EnumTeleportType type, boolean safe) {
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
    public static void teleportTo(@NonNull ServerPlayerEntity player, @NonNull SafeWorldCoordinate after, EnumTeleportType type) {
        teleportTo(player, after, type, -1);
    }

    /**
     * @param tprHorizontalRange 仅用于随机传送安全搜索失败时的重试
     */
    public static void teleportTo(@NonNull ServerPlayerEntity player, @NonNull SafeWorldCoordinate after, EnumTeleportType type, int tprHorizontalRange) {
        SafeWorldCoordinate before = new SafeWorldCoordinate(player);
        World world = player.level;
        if (world != null) {
            ServerWorld level = DimensionUtils.getLevel(after.dimension());
            if (level != null) {
                if (after.safe()) {
                    MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("safe_searching"), NarcissusNotificationTypes.TELEPORT_SEARCH);
                    final int tpRandomRangeArg = tprHorizontalRange;
                    new Thread(() -> {
                        SafeBlockChecker checker = new SafeBlockChecker(level);
                        SafeWorldCoordinate finalAfter;
                        if (type == EnumTeleportType.TP_RANDOM) {
                            int extra = CommonConfig.get().base().randomTeleport().tpRandomSafeNotFoundRetries();
                            int totalAttempts = 1 + Math.max(0, extra);
                            int useRange = tpRandomRangeArg > 0 ? tpRandomRangeArg : CommonConfig.get().base().randomTeleport().teleportRandomDistanceLimit();
                            SafeWorldCoordinate working = after.clone();
                            finalAfter = working;
                            for (int attempt = 0; attempt < totalAttempts; attempt++) {
                                SafeWorldCoordinate resolved = findSafeCoordinate(working.clone(), false);
                                finalAfter = resolved;
                                if (checker.isSafeBlock(resolved.toBlockPos(), false)) {
                                    break;
                                }
                                if (attempt < totalAttempts - 1) {
                                    working = SafeWorldCoordinate.random(player, useRange, after.dimension()).safe(true);
                                }
                            }
                        } else {
                            finalAfter = findSafeCoordinate(after.clone(), false);
                        }
                        Runnable runnable;
                        // 判断是否需要在脚下放置方块
                        if (CommonConfig.get().base().safeTeleport().setBlockWhenSafeNotFound() && !checker.isSafeBlock(finalAfter.toBlockPos(), false)) {
                            BlockState blockState;
                            List<ItemStack> playerItemList = ItemUtils.getAllPlayerItems(player);
                            if (CollectionUtils.isNotNullOrEmpty(NarcissusFarewell.getSafeBlock().getSafeBlocksState())) {
                                if (CommonConfig.get().base().safeTeleport().getBlockFromInventory()) {
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
                                SafeWorldCoordinate airSafeWorldCoordinate = findSafeCoordinate(finalAfter, true);
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
                            ServerPlayerEntity online = srv.getPlayerList().getPlayer(pid);
                            if (online == null) {
                                return;
                            }
                            executeTeleportWithCountdown(online, type, () -> {
                                ServerPlayerEntity pl = srv.getPlayerList().getPlayer(pid);
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
                        ServerPlayerEntity pl = srv != null ? srv.getPlayerList().getPlayer(pid) : null;
                        if (pl != null) {
                            teleportPlayer(pl, after, type, before, level);
                        }
                    });
                }
            }
        }
    }

    private static void teleportPlayer(@NonNull ServerPlayerEntity player, @NonNull SafeWorldCoordinate after, EnumTeleportType type, SafeWorldCoordinate before, ServerWorld level) {
        ResourceLocation sound = Identifier.id().parse(CommonConfig.get().base().other().tpSound());
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
            ForgeNativePacketSender.broadcast(new SSetPassengersPacket(vehicle));
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
    private static @Nullable Entity teleportPassengers(ServerPlayerEntity player, Entity parent, Entity passenger, @NonNull SafeWorldCoordinate safeWorldCoordinate, ServerWorld level) {
        if (!CommonConfig.get().base().teleportTogether().tpWithVehicle() || passenger == null) return null;

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
        ForgeNativePacketSender.broadcast(new SSetPassengersPacket(passenger));
        return playerVehicle;
    }

    /**
     * 传送跟随的实体
     */
    private static void teleportFollowers(@NonNull ServerPlayerEntity player, @NonNull SafeWorldCoordinate safeWorldCoordinate, ServerWorld level) {
        if (!CommonConfig.get().base().teleportTogether().tpWithFollower()) return;

        int followerRange = CommonConfig.get().base().teleportTogether().tpWithFollowerRange();

        // 传送主动跟随的实体
        for (TameableEntity entity : player.level.getEntitiesOfClass(TameableEntity.class, player.getBoundingBox().inflate(followerRange))) {
            if (entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(player.getUUID()) && !entity.isOrderedToSit()) {
                doTeleport(entity, safeWorldCoordinate, level);
            }
        }

        // 传送拴绳实体
        for (MobEntity entity : player.level.getEntitiesOfClass(MobEntity.class, player.getBoundingBox().inflate(followerRange))) {
            if (entity.getLeashHolder() == player) {
                doTeleport(entity, safeWorldCoordinate, level);
            }
        }

        // 传送被吸引的非敌对实体
        for (MobEntity entity : player.level.getEntitiesOfClass(MobEntity.class, player.getBoundingBox().inflate(followerRange))) {
            // 排除敌对生物
            if (entity instanceof MonsterEntity) continue;

            if (entity.goalSelector.getRunningGoals()
                    .anyMatch(goal -> goal.isRunning()
                            && (goal.getGoal() instanceof TemptGoal)
                            && ((TemptGoalAccessor) goal.getGoal()).narcissus$player() == player
                    )) {
                doTeleport(entity, safeWorldCoordinate, level);
            }
        }
    }

    private static Entity doTeleport(@NonNull Entity entity, @NonNull SafeWorldCoordinate safeWorldCoordinate, ServerWorld level) {
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            player.teleportTo(level, safeWorldCoordinate.x(), safeWorldCoordinate.y(), safeWorldCoordinate.z()
                    , safeWorldCoordinate.yaw() == 0 ? player.yRot : (float) safeWorldCoordinate.yaw()
                    , safeWorldCoordinate.pitch() == 0 ? player.xRot : (float) safeWorldCoordinate.pitch());
        } else {
            if (level == entity.level) {
                entity.teleportToWithTicket(safeWorldCoordinate.x(), safeWorldCoordinate.y(), safeWorldCoordinate.z());
            } else {
                entity = entity.changeDimension(level, new ITeleporter() {
                    @Override
                    public Entity placeEntity(Entity entity, ServerWorld currentWorld, ServerWorld destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
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
                        newEntity.moveTo(safeWorldCoordinate.x(), safeWorldCoordinate.y(), safeWorldCoordinate.z(), yaw, newEntity.xRot);
                        return newEntity;
                    }
                });
            }
        }
        return entity;
    }

    // endregion 传送相关


    // region 跨维度传送

    public static boolean isTeleportAcrossDimensionEnabled(ServerPlayerEntity player, RegistryKey<World> to, EnumTeleportType type) {
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
    public static boolean isTeleportTypeAcrossDimensionEnabled(ServerPlayerEntity player, EnumTeleportType type) {
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
    public static int getTeleportCoolDown(ServerPlayerEntity player, EnumTeleportType type) {
        // 如果传送卡类型为抵消冷却时间，则不计算冷却时间
        if (EnumCardType.REFUND_COOLDOWN.name().equalsIgnoreCase(CommonConfig.get().base().teleportCard().teleportCardType().name())
                || EnumCardType.REFUND_ALL_COST_AND_COOLDOWN.name().equalsIgnoreCase(CommonConfig.get().base().teleportCard().teleportCardType().name())
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
        switch (CommonConfig.get().base().teleportRequest().teleportRequestCooldownType()) {
            case COMMON:
                return calculateCooldown(player.getUUID(), current, lastTpTime, CommonConfig.get().base().teleportRequest().teleportRequestCooldown(), null);
            case INDIVIDUAL:
                return calculateCooldown(player.getUUID(), current, lastTpTime, commandCoolDown, type);
            case MIXED:
                int globalCommandCoolDown = CommonConfig.get().base().teleportRequest().teleportRequestCooldown();
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
    public static boolean validTeleportCost(ServerPlayerEntity player, SafeWorldCoordinate target, EnumTeleportType type, boolean submit) {
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
        return validateCost(request.getRequester(), request.getTarget().getLevel().dimension(), calculateDistance(requesterSafeWorldCoordinate, targetSafeWorldCoordinate), request.getTeleportType(), submit);
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
    private static boolean validateCost(ServerPlayerEntity player, RegistryKey<World> targetDim, double distance, EnumTeleportType teleportType, boolean submit) {
        TeleportCost teleportCost = NarcissusUtils.getCommandCost(teleportType);
        if (teleportCost.getType() == EnumCostType.NONE) return true;
        PlayerTeleportData data = PlayerTeleportData.getData(player);

        double adjustedDistance;
        if (player.getLevel().dimension() == targetDim) {
            int limit = CommonConfig.get().base().teleportLimit().teleportCostDistanceLimit();
            adjustedDistance = limit == 0 ? distance : Math.min(limit, distance);
        } else {
            adjustedDistance = CommonConfig.get().base().teleportLimit().teleportCostDistanceAcrossDimension();
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
            MessageUtils.sendNotification(player
                    , NarcissusComponent.get().transAuto("cost_not_enough"
                            , NarcissusComponent.get().transAuto("teleport_card")
                            , cardNeed
                    ), NarcissusNotificationTypes.TELEPORT_ERROR);
        }

        switch (teleportCost.getType()) {
            case EXP_POINT:
                result = player.totalExperience >= costNeed;
                if (!result) {
                    MessageUtils.sendNotification(player
                            , NarcissusComponent.get().transAuto("cost_not_enough"
                                    , NarcissusComponent.get().transAuto("exp_point")
                                    , costNeed
                            ), NarcissusNotificationTypes.TELEPORT_ERROR);
                } else if (submit) {
                    player.giveExperiencePoints(-costNeed);
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case EXP_LEVEL:
                result = player.experienceLevel >= costNeed;
                if (!result) {
                    MessageUtils.sendNotification(player
                            , NarcissusComponent.get().transAuto("cost_not_enough"
                                    , NarcissusComponent.get().transAuto("exp_level")
                                    , costNeed
                            ), NarcissusNotificationTypes.TELEPORT_ERROR);
                } else if (submit) {
                    player.giveExperienceLevels(-costNeed);
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case HEALTH:
                result = player.getHealth() > costNeed;
                if (!result) {
                    MessageUtils.sendNotification(player
                            , NarcissusComponent.get().transAuto("cost_not_enough"
                                    , NarcissusComponent.get().transAuto("health")
                                    , costNeed
                            ), NarcissusNotificationTypes.TELEPORT_ERROR);
                } else if (submit) {
                    try {
                        DataParameter<? super Float> DATA_HEALTH_ID = ((LivingEntityInvoker) player).narcissus$dataHealthId();
                        Float health = (Float) player.getEntityData().get(DATA_HEALTH_ID);
                        player.getEntityData().set(DATA_HEALTH_ID, health - costNeed);
                    } catch (Exception e) {
                        player.hurt(DamageSource.MAGIC, costNeed);
                    }
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case HUNGER:
                result = player.getFoodData().getFoodLevel() >= costNeed;
                if (!result) {
                    MessageUtils.sendNotification(player
                            , NarcissusComponent.get().transAuto("cost_not_enough"
                                    , NarcissusComponent.get().transAuto("hunger")
                                    , costNeed
                            ), NarcissusNotificationTypes.TELEPORT_ERROR);
                } else if (submit) {
                    player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - costNeed);
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case ITEM:
                try {
                    ItemParser parse = new ItemParser(new StringReader(teleportCost.getConf()), false).parse();
                    ItemStack itemStack = new ItemInput(parse.getItem(), parse.getNbt()).createItemStack(1, false);
                    result = getItemCount(player.inventory.items, itemStack) >= costNeed;
                    itemStack.setCount(costNeed);
                    if (!result) {
                        MessageUtils.sendNotification(player
                                , NarcissusComponent.get().transAuto("cost_not_enough"
                                        , NarcissusComponent.get().literal(ItemUtils.getItemHoverNameString(itemStack))
                                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemHover(itemStack)))
                                        , costNeed
                                ), NarcissusNotificationTypes.TELEPORT_ERROR);
                    } else if (submit) {
                        result = ItemUtils.removePlayerItem(player, itemStack);
                        // 代价不足
                        if (result) {
                            data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                        } else {
                            MessageUtils.sendNotification(player
                                    , NarcissusComponent.get().transAuto("cost_not_enough"
                                            , NarcissusComponent.get().literal(ItemUtils.getItemHoverNameString(itemStack))
                                                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemHover(itemStack)))
                                            , costNeed
                                    ), NarcissusNotificationTypes.TELEPORT_ERROR);
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
        if (!CommonConfig.get().base().teleportCard().teleportCard()) return 0;
        switch (CommonConfig.get().base().teleportCard().teleportCardType()) {
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
        if (!CommonConfig.get().base().teleportCard().teleportCard()) return need;
        switch (CommonConfig.get().base().teleportCard().teleportCardType()) {
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
            return item.equals(copy, false);
        }).mapToInt(ItemStack::getCount).sum();
    }

    public static double calculateDistance(SafeWorldCoordinate safeWorldCoordinate1, SafeWorldCoordinate safeWorldCoordinate2) {
        return safeWorldCoordinate1.distanceFrom(safeWorldCoordinate2);
    }

    // endregion 传送代价

    // region 杂项

    public static final DamageSource damageSource = new DamageSource(NarcissusFarewell.MODID) {
        @Nonnull
        @Override
        public ITextComponent getLocalizedDeathMessage(@Nonnull LivingEntity entity) {
            return StringTextComponent.EMPTY;
        }
    }.bypassArmor().bypassMagic().bypassInvul();

    /**
     * 强行使玩家死亡
     */
    public static boolean killPlayer(@Nullable ServerPlayerEntity source, ServerPlayerEntity target) {
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
    public static void playSound(ServerPlayerEntity player, ResourceLocation sound, float volume, float pitch) {
        SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(sound);
        if (soundEvent != null) {
            player.playNotifySound(soundEvent, SoundCategory.PLAYERS, volume, pitch);
        }
    }

    /**
     * 判断玩家是否被任何敌对生物锁定为攻击目标
     */
    public static boolean isTargetedByHostile(ServerPlayerEntity player) {
        return player.level.getEntitiesOfClass(MobEntity.class, player.getBoundingBox()
                        .inflate(CommonConfig.get().base().teleportTogether().tpWithFollowerRange()))
                .stream()
                .anyMatch(entity -> player.equals(entity.getTarget())
                        || (entity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) && player.equals(entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null))
                );
    }

    public static void setPlayerFlightMode(ServerPlayerEntity player) {
        setPlayerFlightMode(player, null);
    }

    public static void setPlayerFlightMode(ServerPlayerEntity player, Boolean enable) {
        setPlayerFlightMode(player, enable, null);
    }

    public static void setPlayerFlightMode(ServerPlayerEntity player, Boolean enable, Float speed) {
        CompoundNBT root = new CompoundNBT();
        player.abilities.addSaveData(root);
        CompoundNBT abilities = root.getCompound("abilities");
        if (enable == null) enable = !abilities.getBoolean("mayfly");
        abilities.putBoolean("mayfly", enable);
        if (!enable) abilities.putBoolean("flying", false);
        if (speed != null) abilities.putFloat("flySpeed", speed);
        player.abilities.loadSaveData(root);
        player.connection.send(new SPlayerAbilitiesPacket(player.abilities));
    }

    // endregion 杂项
}
