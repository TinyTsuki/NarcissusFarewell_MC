package xin.vanilla.narcissus.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.NonNull;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.BlockStateParser;
import net.minecraft.command.arguments.ItemInput;
import net.minecraft.command.arguments.ItemParser;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.brain.memory.MemoryModuleType;
import net.minecraft.entity.ai.goal.TemptGoal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SPlayerAbilitiesPacket;
import net.minecraft.network.play.server.SSetPassengersPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.*;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.CustomConfig;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.*;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.*;
import xin.vanilla.narcissus.mixin.LivingEntityInvoker;
import xin.vanilla.narcissus.mixin.ServerPlayerAccessor;
import xin.vanilla.narcissus.mixin.TemptGoalAccessor;
import xin.vanilla.narcissus.network.ModNetworkHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
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
        String commandPrefix = CommonConfig.COMMAND_PREFIX.get();
        if (StringUtils.isNullOrEmptyEx(commandPrefix) || !commandPrefix.matches("^(\\w ?)+$")) {
            CommonConfig.COMMAND_PREFIX.set(NarcissusFarewell.DEFAULT_COMMAND_PREFIX);
        }
        return CommonConfig.COMMAND_PREFIX.get().trim();
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
                return CommonConfig.TELEPORT_CARD.get();
            case SHARE:
            case SHARE_CONCISE:
                return CommonConfig.SWITCH_SHARE.get();
            case FEED:
            case FEED_OTHER:
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return CommonConfig.SWITCH_FEED.get();
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return CommonConfig.SWITCH_TP_COORDINATE.get();
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return CommonConfig.SWITCH_TP_STRUCTURE.get();
            case TP_ASK:
            case TP_ASK_YES:
            case TP_ASK_NO:
            case TP_ASK_CANCEL:
            case TP_ASK_CONCISE:
            case TP_ASK_YES_CONCISE:
            case TP_ASK_NO_CONCISE:
            case TP_ASK_CANCEL_CONCISE:
                return CommonConfig.SWITCH_TP_ASK.get();
            case TP_HERE:
            case TP_HERE_YES:
            case TP_HERE_NO:
            case TP_HERE_CANCEL:
            case TP_HERE_CONCISE:
            case TP_HERE_YES_CONCISE:
            case TP_HERE_NO_CONCISE:
            case TP_HERE_CANCEL_CONCISE:
                return CommonConfig.SWITCH_TP_HERE.get();
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return CommonConfig.SWITCH_TP_RANDOM.get();
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return CommonConfig.SWITCH_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return CommonConfig.SWITCH_TP_WORLD_SPAWN.get();
            case TP_TOP:
            case TP_TOP_CONCISE:
                return CommonConfig.SWITCH_TP_TOP.get();
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return CommonConfig.SWITCH_TP_BOTTOM.get();
            case TP_UP:
            case TP_UP_CONCISE:
                return CommonConfig.SWITCH_TP_UP.get();
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return CommonConfig.SWITCH_TP_DOWN.get();
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return CommonConfig.SWITCH_TP_VIEW.get();
            case TP_HOME:
            case SET_HOME:
            case DEL_HOME:
            case GET_HOME:
            case TP_HOME_CONCISE:
            case SET_HOME_CONCISE:
            case DEL_HOME_CONCISE:
            case GET_HOME_CONCISE:
                return CommonConfig.SWITCH_TP_HOME.get();
            case TP_STAGE:
            case SET_STAGE:
            case DEL_STAGE:
            case GET_STAGE:
            case TP_STAGE_CONCISE:
            case SET_STAGE_CONCISE:
            case DEL_STAGE_CONCISE:
            case GET_STAGE_CONCISE:
                return CommonConfig.SWITCH_TP_STAGE.get();
            case TP_BACK:
            case TP_BACK_CONCISE:
                return CommonConfig.SWITCH_TP_BACK.get();
            case TP_GRAVE:
            case TP_GRAVE_CONCISE:
                return CommonConfig.SWITCH_TP_GRAVE.get();
            case FLY:
            case FLY_CONCISE:
                return CommonConfig.SWITCH_FLY.get();
            default:
                return true;
        }
    }

    public static String getCommand(EnumTeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return CommonConfig.COMMAND_TP_COORDINATE.get();
            case TP_STRUCTURE:
                return CommonConfig.COMMAND_TP_STRUCTURE.get();
            case TP_ASK:
                return CommonConfig.COMMAND_TP_ASK.get();
            case TP_HERE:
                return CommonConfig.COMMAND_TP_HERE.get();
            case TP_RANDOM:
                return CommonConfig.COMMAND_TP_RANDOM.get();
            case TP_SPAWN:
                return CommonConfig.COMMAND_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
                return CommonConfig.COMMAND_TP_WORLD_SPAWN.get();
            case TP_TOP:
                return CommonConfig.COMMAND_TP_TOP.get();
            case TP_BOTTOM:
                return CommonConfig.COMMAND_TP_BOTTOM.get();
            case TP_UP:
                return CommonConfig.COMMAND_TP_UP.get();
            case TP_DOWN:
                return CommonConfig.COMMAND_TP_DOWN.get();
            case TP_VIEW:
                return CommonConfig.COMMAND_TP_VIEW.get();
            case TP_HOME:
                return CommonConfig.COMMAND_TP_HOME.get();
            case TP_STAGE:
                return CommonConfig.COMMAND_TP_STAGE.get();
            case TP_BACK:
                return CommonConfig.COMMAND_TP_BACK.get();
            case TP_GRAVE:
                return CommonConfig.COMMAND_TP_GRAVE.get();
            default:
                return "";
        }
    }

    public static String getCommand(EnumCommandType type) {
        String prefix = NarcissusUtils.getCommandPrefix();
        switch (type) {
            case HELP:
                return prefix + " help";
            case LANGUAGE:
                return prefix + " " + CommonConfig.COMMAND_LANGUAGE.get();
            case LANGUAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_LANGUAGE.get() : "";
            case DIMENSION:
                return prefix + " " + CommonConfig.COMMAND_DIMENSION.get();
            case DIMENSION_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_DIMENSION.get() : "";
            case UUID:
                return prefix + " " + CommonConfig.COMMAND_UUID.get();
            case UUID_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_UUID.get() : "";
            case CARD:
            case SET_CARD:
                return prefix + " " + CommonConfig.COMMAND_CARD.get();
            case CARD_CONCISE:
            case SET_CARD_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_CARD.get() : "";
            case SHARE:
                return prefix + " " + CommonConfig.COMMAND_SHARE.get();
            case SHARE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_SHARE.get() : "";
            case FEED:
            case FEED_OTHER:
                return prefix + " " + CommonConfig.COMMAND_FEED.get();
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_FEED.get() : "";
            case TP_COORDINATE:
                return prefix + " " + CommonConfig.COMMAND_TP_COORDINATE.get();
            case TP_COORDINATE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_COORDINATE.get() : "";
            case TP_STRUCTURE:
                return prefix + " " + CommonConfig.COMMAND_TP_STRUCTURE.get();
            case TP_STRUCTURE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_STRUCTURE.get() : "";
            case TP_ASK:
                return prefix + " " + CommonConfig.COMMAND_TP_ASK.get();
            case TP_ASK_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_ASK.get() : "";
            case TP_ASK_YES:
                return prefix + " " + CommonConfig.COMMAND_TP_ASK_YES.get();
            case TP_ASK_YES_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_ASK_YES.get() : "";
            case TP_ASK_NO:
                return prefix + " " + CommonConfig.COMMAND_TP_ASK_NO.get();
            case TP_ASK_NO_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_ASK_NO.get() : "";
            case TP_ASK_CANCEL:
                return prefix + " " + CommonConfig.COMMAND_TP_ASK_CANCEL.get();
            case TP_ASK_CANCEL_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_ASK_CANCEL.get() : "";
            case TP_HERE:
                return prefix + " " + CommonConfig.COMMAND_TP_HERE.get();
            case TP_HERE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_HERE.get() : "";
            case TP_HERE_YES:
                return prefix + " " + CommonConfig.COMMAND_TP_HERE_YES.get();
            case TP_HERE_YES_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_HERE_YES.get() : "";
            case TP_HERE_NO:
                return prefix + " " + CommonConfig.COMMAND_TP_HERE_NO.get();
            case TP_HERE_NO_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_HERE_NO.get() : "";
            case TP_HERE_CANCEL:
                return prefix + " " + CommonConfig.COMMAND_TP_HERE_CANCEL.get();
            case TP_HERE_CANCEL_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_HERE_CANCEL.get() : "";
            case TP_RANDOM:
                return prefix + " " + CommonConfig.COMMAND_TP_RANDOM.get();
            case TP_RANDOM_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_RANDOM.get() : "";
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
                return prefix + " " + CommonConfig.COMMAND_TP_SPAWN.get();
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_SPAWN.get() : "";
            case TP_WORLD_SPAWN:
                return prefix + " " + CommonConfig.COMMAND_TP_WORLD_SPAWN.get();
            case TP_WORLD_SPAWN_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_WORLD_SPAWN.get() : "";
            case TP_TOP:
                return prefix + " " + CommonConfig.COMMAND_TP_TOP.get();
            case TP_TOP_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_TOP.get() : "";
            case TP_BOTTOM:
                return prefix + " " + CommonConfig.COMMAND_TP_BOTTOM.get();
            case TP_BOTTOM_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_BOTTOM.get() : "";
            case TP_UP:
                return prefix + " " + CommonConfig.COMMAND_TP_UP.get();
            case TP_UP_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_UP.get() : "";
            case TP_DOWN:
                return prefix + " " + CommonConfig.COMMAND_TP_DOWN.get();
            case TP_DOWN_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_DOWN.get() : "";
            case TP_VIEW:
                return prefix + " " + CommonConfig.COMMAND_TP_VIEW.get();
            case TP_VIEW_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_VIEW.get() : "";
            case TP_HOME:
                return prefix + " " + CommonConfig.COMMAND_TP_HOME.get();
            case TP_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_HOME.get() : "";
            case SET_HOME:
                return prefix + " " + CommonConfig.COMMAND_SET_HOME.get();
            case SET_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_SET_HOME.get() : "";
            case DEL_HOME:
                return prefix + " " + CommonConfig.COMMAND_DEL_HOME.get();
            case DEL_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_DEL_HOME.get() : "";
            case GET_HOME:
                return prefix + " " + CommonConfig.COMMAND_GET_HOME.get();
            case GET_HOME_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_GET_HOME.get() : "";
            case TP_STAGE:
                return prefix + " " + CommonConfig.COMMAND_TP_STAGE.get();
            case TP_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_STAGE.get() : "";
            case SET_STAGE:
                return prefix + " " + CommonConfig.COMMAND_SET_STAGE.get();
            case SET_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_SET_STAGE.get() : "";
            case DEL_STAGE:
                return prefix + " " + CommonConfig.COMMAND_DEL_STAGE.get();
            case DEL_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_DEL_STAGE.get() : "";
            case GET_STAGE:
                return prefix + " " + CommonConfig.COMMAND_GET_STAGE.get();
            case GET_STAGE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_GET_STAGE.get() : "";
            case TP_BACK:
                return prefix + " " + CommonConfig.COMMAND_TP_BACK.get();
            case TP_BACK_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_BACK.get() : "";
            case TP_GRAVE:
                return prefix + " " + CommonConfig.COMMAND_TP_GRAVE.get();
            case TP_GRAVE_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_TP_GRAVE.get() : "";
            case FLY:
                return prefix + " " + CommonConfig.COMMAND_FLY.get();
            case FLY_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_FLY.get() : "";
            case VIRTUAL_OP:
                return prefix + " " + CommonConfig.COMMAND_VIRTUAL_OP.get();
            case VIRTUAL_OP_CONCISE:
                return isConciseEnabled(type) ? CommonConfig.COMMAND_VIRTUAL_OP.get() : "";
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
                return ServerConfig.PERMISSION_SET_CARD.get();
            case FEED_OTHER:
            case FEED_OTHER_CONCISE:
                return ServerConfig.PERMISSION_FEED_OTHER.get();
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return ServerConfig.PERMISSION_TP_COORDINATE.get();
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return ServerConfig.PERMISSION_TP_STRUCTURE.get();
            case TP_ASK:
            case TP_ASK_CANCEL:
                // case TP_ASK_YES:
                // case TP_ASK_NO:
            case TP_ASK_CONCISE:
            case TP_ASK_CANCEL_CONCISE:
                // case TP_ASK_YES_CONCISE:
                // case TP_ASK_NO_CONCISE:
                return ServerConfig.PERMISSION_TP_ASK.get();
            case TP_HERE:
            case TP_HERE_CANCEL:
                // case TP_HERE_YES:
                // case TP_HERE_NO:
            case TP_HERE_CONCISE:
            case TP_HERE_CANCEL_CONCISE:
                // case TP_HERE_YES_CONCISE:
                // case TP_HERE_NO_CONCISE:
                return ServerConfig.PERMISSION_TP_HERE.get();
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return ServerConfig.PERMISSION_TP_RANDOM.get();
            case TP_SPAWN:
            case TP_SPAWN_CONCISE:
                return ServerConfig.PERMISSION_TP_SPAWN.get();
            case TP_SPAWN_OTHER:
            case TP_SPAWN_OTHER_CONCISE:
                return ServerConfig.PERMISSION_TP_SPAWN_OTHER.get();
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return ServerConfig.PERMISSION_TP_WORLD_SPAWN.get();
            case TP_TOP:
            case TP_TOP_CONCISE:
                return ServerConfig.PERMISSION_TP_TOP.get();
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return ServerConfig.PERMISSION_TP_BOTTOM.get();
            case TP_UP:
            case TP_UP_CONCISE:
                return ServerConfig.PERMISSION_TP_UP.get();
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return ServerConfig.PERMISSION_TP_DOWN.get();
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return ServerConfig.PERMISSION_TP_VIEW.get();
            case TP_HOME:
            case SET_HOME:
            case DEL_HOME:
            case GET_HOME:
            case TP_HOME_CONCISE:
            case SET_HOME_CONCISE:
            case DEL_HOME_CONCISE:
            case GET_HOME_CONCISE:
                return ServerConfig.PERMISSION_TP_HOME.get();
            case TP_STAGE:
            case TP_STAGE_CONCISE:
                return ServerConfig.PERMISSION_TP_STAGE.get();
            case SET_STAGE:
            case SET_STAGE_CONCISE:
                return ServerConfig.PERMISSION_SET_STAGE.get();
            case DEL_STAGE:
            case DEL_STAGE_CONCISE:
                return ServerConfig.PERMISSION_DEL_STAGE.get();
            case GET_STAGE:
            case GET_STAGE_CONCISE:
                return ServerConfig.PERMISSION_GET_STAGE.get();
            case TP_BACK:
            case TP_BACK_CONCISE:
                return ServerConfig.PERMISSION_TP_BACK.get();
            case TP_GRAVE:
            case TP_GRAVE_CONCISE:
                return ServerConfig.PERMISSION_TP_GRAVE.get();
            case FLY:
            case FLY_CONCISE:
                return ServerConfig.PERMISSION_FLY.get();
            case VIRTUAL_OP:
            case VIRTUAL_OP_CONCISE:
                return ServerConfig.PERMISSION_VIRTUAL_OP.get();
            default:
                return 0;
        }
    }

    public static int getCommandPermissionLevel(EnumTeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return ServerConfig.PERMISSION_TP_COORDINATE.get();
            case TP_STRUCTURE:
                return ServerConfig.PERMISSION_TP_STRUCTURE.get();
            case TP_ASK:
                return ServerConfig.PERMISSION_TP_ASK.get();
            case TP_HERE:
                return ServerConfig.PERMISSION_TP_HERE.get();
            case TP_RANDOM:
                return ServerConfig.PERMISSION_TP_RANDOM.get();
            case TP_SPAWN:
                return ServerConfig.PERMISSION_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
                return ServerConfig.PERMISSION_TP_WORLD_SPAWN.get();
            case TP_TOP:
                return ServerConfig.PERMISSION_TP_TOP.get();
            case TP_BOTTOM:
                return ServerConfig.PERMISSION_TP_BOTTOM.get();
            case TP_UP:
                return ServerConfig.PERMISSION_TP_UP.get();
            case TP_DOWN:
                return ServerConfig.PERMISSION_TP_DOWN.get();
            case TP_VIEW:
                return ServerConfig.PERMISSION_TP_VIEW.get();
            case TP_HOME:
                return ServerConfig.PERMISSION_TP_HOME.get();
            case TP_STAGE:
                return ServerConfig.PERMISSION_TP_STAGE.get();
            case TP_BACK:
                return ServerConfig.PERMISSION_TP_BACK.get();
            default:
                return 0;
        }
    }

    public static boolean isConciseEnabled(EnumCommandType type) {
        switch (type) {
            case LANGUAGE:
            case LANGUAGE_CONCISE:
                return CommonConfig.CONCISE_LANGUAGE.get();
            case UUID:
            case UUID_CONCISE:
                return CommonConfig.CONCISE_UUID.get();
            case DIMENSION:
            case DIMENSION_CONCISE:
                return CommonConfig.CONCISE_DIMENSION.get();
            case CARD:
            case CARD_CONCISE:
            case SET_CARD:
            case SET_CARD_CONCISE:
                return CommonConfig.CONCISE_CARD.get();
            case SHARE:
            case SHARE_CONCISE:
                return CommonConfig.CONCISE_SHARE.get();
            case FEED:
            case FEED_OTHER:
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return CommonConfig.CONCISE_FEED.get();
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return CommonConfig.CONCISE_TP_COORDINATE.get();
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return CommonConfig.CONCISE_TP_STRUCTURE.get();
            case TP_ASK:
            case TP_ASK_CONCISE:
                return CommonConfig.CONCISE_TP_ASK.get();
            case TP_ASK_YES:
            case TP_ASK_YES_CONCISE:
                return CommonConfig.CONCISE_TP_ASK_YES.get();
            case TP_ASK_NO:
            case TP_ASK_NO_CONCISE:
                return CommonConfig.CONCISE_TP_ASK_NO.get();
            case TP_ASK_CANCEL:
            case TP_ASK_CANCEL_CONCISE:
                return CommonConfig.CONCISE_TP_ASK_CANCEL.get();
            case TP_HERE:
            case TP_HERE_CONCISE:
                return CommonConfig.CONCISE_TP_HERE.get();
            case TP_HERE_YES:
            case TP_HERE_YES_CONCISE:
                return CommonConfig.CONCISE_TP_HERE_YES.get();
            case TP_HERE_NO:
            case TP_HERE_NO_CONCISE:
                return CommonConfig.CONCISE_TP_HERE_NO.get();
            case TP_HERE_CANCEL:
            case TP_HERE_CANCEL_CONCISE:
                return CommonConfig.CONCISE_TP_HERE_CANCEL.get();
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return CommonConfig.CONCISE_TP_RANDOM.get();
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return CommonConfig.CONCISE_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return CommonConfig.CONCISE_TP_WORLD_SPAWN.get();
            case TP_TOP:
            case TP_TOP_CONCISE:
                return CommonConfig.CONCISE_TP_TOP.get();
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return CommonConfig.CONCISE_TP_BOTTOM.get();
            case TP_UP:
            case TP_UP_CONCISE:
                return CommonConfig.CONCISE_TP_UP.get();
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return CommonConfig.CONCISE_TP_DOWN.get();
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return CommonConfig.CONCISE_TP_VIEW.get();
            case TP_HOME:
            case TP_HOME_CONCISE:
                return CommonConfig.CONCISE_TP_HOME.get();
            case SET_HOME:
            case SET_HOME_CONCISE:
                return CommonConfig.CONCISE_SET_HOME.get();
            case DEL_HOME:
            case DEL_HOME_CONCISE:
                return CommonConfig.CONCISE_DEL_HOME.get();
            case GET_HOME:
            case GET_HOME_CONCISE:
                return CommonConfig.CONCISE_GET_HOME.get();
            case TP_STAGE:
            case TP_STAGE_CONCISE:
                return CommonConfig.CONCISE_TP_STAGE.get();
            case SET_STAGE:
            case SET_STAGE_CONCISE:
                return CommonConfig.CONCISE_SET_STAGE.get();
            case DEL_STAGE:
            case DEL_STAGE_CONCISE:
                return CommonConfig.CONCISE_DEL_STAGE.get();
            case GET_STAGE:
            case GET_STAGE_CONCISE:
                return CommonConfig.CONCISE_GET_STAGE.get();
            case TP_BACK:
            case TP_BACK_CONCISE:
                return CommonConfig.CONCISE_TP_BACK.get();
            case TP_GRAVE:
            case TP_GRAVE_CONCISE:
                return CommonConfig.CONCISE_TP_GRAVE.get();
            case FLY:
            case FLY_CONCISE:
                return CommonConfig.CONCISE_FLY.get();
            case VIRTUAL_OP:
            case VIRTUAL_OP_CONCISE:
                return CommonConfig.CONCISE_VIRTUAL_OP.get();
            default:
                return false;
        }
    }

    public static boolean hasCommandPermission(CommandSource source, EnumCommandType type) {
        return source.hasPermission(getCommandPermissionLevel(type)) || hasVirtualPermission(source.getEntity(), type);
    }

    public static boolean hasVirtualPermission(Entity source, EnumCommandType type) {
        // 若为玩家
        if (source instanceof PlayerEntity) {
            return VirtualPermissionManager.getVirtualPermission((PlayerEntity) source).stream()
                    .filter(Objects::nonNull)
                    .anyMatch(s -> s.replaceConcise() == type.replaceConcise());
        } else {
            return false;
        }
    }

    /**
     * 执行指令
     */
    public static boolean executeCommand(@NonNull ServerPlayerEntity player, @NonNull String command, int permission, boolean suppressedOutput) {
        boolean result = false;
        try {
            MinecraftServer server = player.getServer();
            CommandSource commandSourceStack = player.createCommandSourceStack();
            if (permission > 0) {
                commandSourceStack = commandSourceStack.withPermission(permission);
            }
            if (suppressedOutput) {
                commandSourceStack = commandSourceStack.withSuppressedOutput();
            }
            if (server != null) {
                result = server.getCommands().performCommand(commandSourceStack, command) > 0;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: {}", command, e);
        }
        return result;
    }

    /**
     * 执行指令
     */
    public static boolean executeCommand(@NonNull ServerPlayerEntity player, @NonNull String command) {
        return executeCommand(player, command, 0, false);
    }

    /**
     * 执行指令
     */
    public static boolean executeCommandNoOutput(@NonNull ServerPlayerEntity player, @NonNull String command) {
        return executeCommandNoOutput(player, command, 0);
    }

    /**
     * 执行指令
     */
    public static boolean executeCommandNoOutput(@NonNull ServerPlayerEntity player, @NonNull String command, int permission) {
        return executeCommand(player, command, permission, true);
    }

    public static void refreshPermission(@NonNull ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            server = NarcissusFarewell.getServerInstance();
        }
        server.getPlayerList().sendPlayerPermissionLevel(player);
    }

    // endregion 指令相关

    // region 安全坐标

    public static int getWorldMinY(World world) {
        return 0;
    }

    public static int getWorldMaxY(World world) {
        return world.getMaxBuildHeight();
    }

    public static Coordinate findTopCandidate(ServerWorld world, Coordinate start) {
        return new SafeCoordinateFinder(world).findTopCandidate(start);
    }

    public static Coordinate findBottomCandidate(ServerWorld world, Coordinate start) {
        return new SafeCoordinateFinder(world).findBottomCandidate(start);
    }

    public static Coordinate findUpCandidate(ServerWorld world, Coordinate start) {
        return new SafeCoordinateFinder(world).findUpCandidate(start);
    }

    public static Coordinate findDownCandidate(ServerWorld world, Coordinate start) {
        return new SafeCoordinateFinder(world).findDownCandidate(start);
    }

    public static Coordinate findViewEndCandidate(ServerPlayerEntity player, boolean safe, int range) {
        return new SafeCoordinateFinder(player.getLevel()).findViewEndCandidate(player, safe, range);
    }

    public static Coordinate findSafeCoordinate(Coordinate coordinate, boolean belowAllowAir) {
        World world = DimensionUtils.getLevel(coordinate.dimension());
        int chunkX = (int) coordinate.x() >> 4;
        int chunkZ = (int) coordinate.z() >> 4;
        Coordinate result = new SafeCoordinateFinder(world).searchInChunk(coordinate, chunkX, chunkZ, belowAllowAir);
        LOGGER.debug("Target:{} | Safe:{}", coordinate.toXyzIntString(), result == null ? "null" : result.toXyzIntString());
        return result == null ? coordinate : result;
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
                    .filter(key -> key.key().equals(player.level.dimension().location().toString()))
                    .findFirst()
                    .map(KeyValue::key)
                    .orElse(null);
        }
    }

    public static KeyValue<String, String> getPlayerHomeKey(ServerPlayerEntity player, RegistryKey<World> dimension, String name) {
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        Map<KeyValue<String, String>, Coordinate> homeCoordinate = data.getHomeCoordinate();
        Map<String, String> defaultHome = data.getDefaultHome();
        String currentDimStr = player.level.dimension().location().toString();
        String targetDimStr = dimension != null ? dimension.location().toString() : null;

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
                Coordinate c = homeCoordinate.get(kv);
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
    public static Coordinate getPlayerHome(ServerPlayerEntity player, RegistryKey<World> dimension, String name) {
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
                    .filter(key -> key.key().equals(player.level.dimension().location().toString()))
                    .findFirst()
                    .map(KeyValue::key)
                    .orElse(null);
        }
        return null;
    }

    public static KeyValue<String, String> getStageKey(ServerPlayerEntity player, RegistryKey<World> dimension, String name) {
        WorldStageData stageData = WorldStageData.get();
        Map<KeyValue<String, String>, Coordinate> stageCoordinate = stageData.getStageCoordinate();
        String currentDimStr = player.level.dimension().location().toString();
        String targetDimStr = dimension != null ? dimension.location().toString() : null;

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
    private static KeyValue<String, String> findNearestStageInDimension(Map<KeyValue<String, String>, Coordinate> stageCoordinate,
                                                                        ServerPlayerEntity player, String dimensionStr) {
        return stageCoordinate.entrySet().stream()
                .filter(entry -> entry.getKey().key().equals(dimensionStr))
                .min(Comparator.comparingDouble(entry -> {
                    Coordinate c = entry.getValue();
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
    public static Coordinate getStageCoordinate(ServerPlayerEntity player, RegistryKey<World> dimension, String name) {
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
        for (String s : ServerConfig.TELEPORT_BACK_SKIP_TYPE.get()) {
            EnumTeleportType value = EnumTeleportType.nullableValueOf(s);
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
        PlayerTeleportData.getData(player).getTeleportRecords().remove(record);
    }

    // endregion 坐标查找

    // region 传送相关

    /**
     * 检查传送范围
     */
    public static int checkRange(ServerPlayerEntity player, EnumTeleportType type, int range) {
        int maxRange;
        switch (type) {
            case TP_VIEW:
                maxRange = ServerConfig.TELEPORT_VIEW_DISTANCE_LIMIT.get();
                break;
            default:
                maxRange = ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get();
                break;
        }
        if (range > maxRange) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "range_too_large"), maxRange);
        } else if (range <= 0) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "range_too_small"), 1);
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
            teleportTo(to, new Coordinate(from).safe(safe), type);
        } else {
            teleportTo(from, new Coordinate(to).safe(safe), type);
        }
    }

    /**
     * 传送玩家到指定坐标
     *
     * @param player 玩家
     * @param after  坐标
     */
    public static void teleportTo(@NonNull ServerPlayerEntity player, @NonNull Coordinate after, EnumTeleportType type) {
        Coordinate before = new Coordinate(player);
        World world = player.level;
        if (world != null) {
            ServerWorld level = DimensionUtils.getLevel(after.dimension());
            if (level != null) {
                if (after.safe()) {
                    // 异步的代价就是粪吗
                    NarcissusUtils.sendActionBarMessage(player, Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, "safe_searching"));
                    new Thread(() -> {
                        Coordinate finalAfter = after.clone();
                        finalAfter = findSafeCoordinate(finalAfter, false);
                        Runnable runnable;
                        // 判断是否需要在脚下放置方块
                        SafeBlockChecker checker = new SafeBlockChecker(level);
                        if (ServerConfig.SETBLOCK_WHEN_SAFE_NOT_FOUND.get() && !checker.isSafeBlock(finalAfter.toBlockPos(), false)) {
                            BlockState blockState;
                            List<ItemStack> playerItemList = getPlayerItemList(player);
                            if (CollectionUtils.isNotNullOrEmpty(NarcissusFarewell.getSafeBlock().getSafeBlocksState())) {
                                if (ServerConfig.GETBLOCK_FROM_INVENTORY.get()) {
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
                                Coordinate airCoordinate = findSafeCoordinate(finalAfter, true);
                                if (!airCoordinate.toXyzString().equals(finalAfter.toXyzString())) {
                                    finalAfter = airCoordinate;
                                    runnable = () -> {
                                        Item blockItem = new ItemStack(blockState.getBlock()).getItem();
                                        Item remove = playerItemList.stream().map(ItemStack::getItem).filter(blockItem::equals).findFirst().orElse(null);
                                        if (remove != null) {
                                            ItemStack itemStack = new ItemStack(remove);
                                            itemStack.setCount(1);
                                            if (removeItemFromPlayerInventory(player, itemStack)) {
                                                level.setBlockAndUpdate(airCoordinate.toBlockPos().below(), blockState.getBlock().defaultBlockState());
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
                        Coordinate finalAfter1 = finalAfter;
                        player.server.submit(() -> {
                            if (runnable != null) runnable.run();
                            teleportPlayer(player, finalAfter1, type, before, level);
                        });
                    }).start();
                } else {
                    teleportPlayer(player, after, type, before, level);
                }
            }
        }
    }

    private static void teleportPlayer(@NonNull ServerPlayerEntity player, @NonNull Coordinate after, EnumTeleportType type, Coordinate before, ServerWorld level) {
        ResourceLocation sound = Identifier.parse(ServerConfig.TP_SOUND.get());
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
            broadcastPacket(new SSetPassengersPacket(vehicle));
        }

        NarcissusUtils.playSound(player, sound, 1.0f, 1.0f);
        TeleportRecord record = new TeleportRecord();
        record.setTeleportTime(new Date());
        record.setTeleportType(type);
        record.setBefore(before);
        record.setAfter(after);
        PlayerTeleportData.getData(player).addTeleportRecords(record);
    }

    /**
     * 传送载具及其所有乘客
     *
     * @param parent     载具
     * @param passenger  乘客
     * @param coordinate 目标坐标
     * @param level      目标世界
     * @return 玩家的坐骑
     */
    private static @Nullable Entity teleportPassengers(ServerPlayerEntity player, Entity parent, Entity passenger, @NonNull Coordinate coordinate, ServerWorld level) {
        if (!ServerConfig.TP_WITH_VEHICLE.get() || passenger == null) return null;

        Entity playerVehicle = null;
        List<Entity> passengers = new ArrayList<>(passenger.getPassengers());

        // 递归传送所有乘客
        for (Entity entity : passengers) {
            if (CollectionUtils.isNotNullOrEmpty(entity.getPassengers())) {
                Entity value = teleportPassengers(player, passenger, entity, coordinate, level);
                if (value != null) {
                    playerVehicle = value;
                }
            }
        }

        passengers.forEach(Entity::stopRiding);

        // 传送载具
        if (parent == null) {
            passenger = doTeleport(passenger, coordinate, level);
        }
        // 传送所有乘客
        for (Entity entity : passengers) {
            if (entity == player) {
                playerVehicle = passenger;
            } else if (entity.getVehicle() == null) {
                int oldId = entity.getId();
                entity = doTeleport(entity, coordinate, level);
                entity.startRiding(passenger, true);
                // 更新玩家乘坐的实体对象
                if (playerVehicle != null && oldId == playerVehicle.getId()) {
                    playerVehicle = entity;
                }
            }
        }
        // 同步客户端状态
        broadcastPacket(new SSetPassengersPacket(passenger));
        return playerVehicle;
    }

    /**
     * 传送跟随的实体
     */
    private static void teleportFollowers(@NonNull ServerPlayerEntity player, @NonNull Coordinate coordinate, ServerWorld level) {
        if (!ServerConfig.TP_WITH_FOLLOWER.get()) return;

        int followerRange = ServerConfig.TP_WITH_FOLLOWER_RANGE.get();

        // 传送主动跟随的实体
        for (TameableEntity entity : player.level.getEntitiesOfClass(TameableEntity.class, player.getBoundingBox().inflate(followerRange))) {
            if (entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(player.getUUID()) && !entity.isOrderedToSit()) {
                doTeleport(entity, coordinate, level);
            }
        }

        // 传送拴绳实体
        for (MobEntity entity : player.level.getEntitiesOfClass(MobEntity.class, player.getBoundingBox().inflate(followerRange))) {
            if (entity.getLeashHolder() == player) {
                doTeleport(entity, coordinate, level);
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
                doTeleport(entity, coordinate, level);
            }
        }
    }

    private static Entity doTeleport(@NonNull Entity entity, @NonNull Coordinate coordinate, ServerWorld level) {
        if (entity instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            player.teleportTo(level, coordinate.x(), coordinate.y(), coordinate.z()
                    , coordinate.yaw() == 0 ? player.yRot : (float) coordinate.yaw()
                    , coordinate.pitch() == 0 ? player.xRot : (float) coordinate.pitch());
        } else {
            if (level == entity.level) {
                entity.teleportToWithTicket(coordinate.x(), coordinate.y(), coordinate.z());
            } else {
                entity = entity.changeDimension(level, new ITeleporter() {
                    @Override
                    public Entity placeEntity(Entity entity, ServerWorld currentWorld, ServerWorld destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
                        // 计算目标区块坐标
                        int chunkX = coordinate.getXInt() >> 4;
                        int chunkZ = coordinate.getZInt() >> 4;
                        // 确保目标区块已加载
                        destWorld.getChunkSource().addRegionTicket(
                                TicketType.POST_TELEPORT,
                                new ChunkPos(chunkX, chunkZ),
                                4, // 加载等级
                                entity.getId()
                        );
                        // 复制实体，并且不生成传送门
                        Entity newEntity = repositionEntity.apply(false);
                        newEntity.moveTo(coordinate.x(), coordinate.y(), coordinate.z(), yaw, newEntity.xRot);
                        return newEntity;
                    }
                });
            }
        }
        return entity;
    }

    // endregion 传送相关

    // region 玩家与玩家背包

    /**
     * 获取随机玩家
     */
    public static ServerPlayerEntity getRandomPlayer() {
        try {
            List<ServerPlayerEntity> players = NarcissusFarewell.getServerInstance().getPlayerList().getPlayers();
            return players.get(new Random().nextInt(players.size()));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取随机玩家UUID
     */
    public static UUID getRandomPlayerUUID() {
        PlayerEntity randomPlayer = getRandomPlayer();
        return randomPlayer != null ? randomPlayer.getUUID() : null;
    }

    /**
     * 通过UUID获取对应的玩家
     *
     * @param uuid 玩家UUID
     */
    public static ServerPlayerEntity getPlayer(UUID uuid) {
        try {
            return Minecraft.getInstance().level.getServer().getPlayerList().getPlayer(uuid);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 移除玩家背包中的指定物品
     *
     * @param player       玩家
     * @param itemToRemove 要移除的物品
     * @return 是否全部移除成功
     */
    public static boolean removeItemFromPlayerInventory(ServerPlayerEntity player, ItemStack itemToRemove) {
        IInventory inventory = player.inventory;

        // 剩余要移除的数量
        int remainingAmount = itemToRemove.getCount();
        // 记录成功移除的物品数量，以便失败时进行回滚
        int successfullyRemoved = 0;

        // 遍历玩家背包的所有插槽
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            // 获取背包中的物品
            ItemStack stack = inventory.getItem(i);
            ItemStack copy = itemToRemove.copy();
            copy.setCount(stack.getCount());

            // 如果插槽中的物品是目标物品
            if (stack.equals(copy, false)) {
                // 获取当前物品堆叠的数量
                int stackSize = stack.getCount();

                // 如果堆叠数量大于或等于剩余需要移除的数量
                if (stackSize >= remainingAmount) {
                    // 移除指定数量的物品
                    stack.shrink(remainingAmount);
                    // 记录成功移除的数量
                    successfullyRemoved += remainingAmount;
                    // 移除完毕
                    remainingAmount = 0;
                    break;
                } else {
                    // 移除该堆所有物品
                    stack.setCount(0);
                    // 记录成功移除的数量
                    successfullyRemoved += stackSize;
                    // 减少剩余需要移除的数量
                    remainingAmount -= stackSize;
                }
            }
        }

        // 如果没有成功移除所有物品，撤销已移除的部分
        if (remainingAmount > 0) {
            // 创建副本并还回成功移除的物品
            ItemStack copy = itemToRemove.copy();
            copy.setCount(successfullyRemoved);
            // 将已移除的物品添加回背包
            player.inventory.add(copy);
        }

        // 是否成功移除所有物品
        return remainingAmount == 0;
    }

    public static List<ItemStack> getPlayerItemList(ServerPlayerEntity player) {
        List<ItemStack> result = new ArrayList<>();
        if (player != null) {
            result.addAll(player.inventory.items);
            result.addAll(player.inventory.armor);
            result.addAll(player.inventory.offhand);
            result = result.stream().filter(itemStack -> !itemStack.isEmpty() && itemStack.getItem() != Items.AIR).collect(Collectors.toList());
        }
        return result;
    }

    // endregion 玩家与玩家背包

    // region 消息相关

    /**
     * 广播消息
     *
     * @param source  发送者
     * @param message 消息
     */
    public static void broadcastMessage(ServerPlayerEntity source, Component message) {
        for (ServerPlayerEntity player : source.server.getPlayerList().getPlayers()) {
            sendMessage(player, Component.literal("[%s] %s")
                    .appendArg(getPlayerName(source))
                    .appendArg(message)
            );
        }
    }

    /**
     * 广播消息
     *
     * @param server  发送者
     * @param message 消息
     */
    public static void broadcastMessage(MinecraftServer server, Component message) {
        for (ServerPlayerEntity player : server.getPlayerList().getPlayers()) {
            sendMessage(player, Component.literal("[%s] %s")
                    .appendArg(server.getServerModName())
                    .appendArg(message)
            );
        }
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(ServerPlayerEntity player, Component message) {
        player.sendMessage(message.toChatComponent(NarcissusUtils.getPlayerLanguage(player)), player.getUUID());
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(ServerPlayerEntity player, String message) {
        player.sendMessage(Component.literal(message).toChatComponent(), player.getUUID());
    }

    /**
     * 发送翻译消息
     *
     * @param player 玩家
     * @param key    翻译键
     * @param args   参数
     */
    public static void sendTranslatableMessage(ServerPlayerEntity player, String key, Object... args) {
        player.sendMessage(Component.trans(key, args).languageCode(NarcissusUtils.getPlayerLanguage(player)).toChatComponent(), player.getUUID());
    }

    /**
     * 发送翻译消息
     *
     * @param source  指令来源
     * @param success 是否成功
     * @param key     翻译键
     * @param args    参数
     */
    public static void sendTranslatableMessage(CommandSource source, boolean success, String key, Object... args) {
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
            try {
                sendTranslatableMessage(source.getPlayerOrException(), key, args);
            } catch (CommandSyntaxException ignored) {
            }
        } else if (success) {
            source.sendSuccess(Component.trans(key, args).languageCode(ServerConfig.DEFAULT_LANGUAGE.get()).toChatComponent(), false);
        } else {
            source.sendFailure(Component.trans(key, args).languageCode(ServerConfig.DEFAULT_LANGUAGE.get()).toChatComponent());
        }
    }

    /**
     * 发送操作栏消息
     */
    public static void sendActionBarMessage(ServerPlayerEntity player, Component message) {
        player.connection.send(new SChatPacket(message.toChatComponent(NarcissusUtils.getPlayerLanguage(player)), ChatType.GAME_INFO, player.getUUID()));
    }

    /**
     * 广播数据包至所有玩家
     *
     * @param packet 数据包
     */
    public static void broadcastPacket(IPacket<?> packet) {
        NarcissusFarewell.getServerInstance().getPlayerList().getPlayers().forEach(player -> player.connection.send(packet));
    }

    /**
     * 发送数据包至服务器
     */
    public static <MSG> void sendPacketToServer(MSG msg) {
        ModNetworkHandler.INSTANCE.sendToServer(msg);
    }

    /**
     * 发送数据包至玩家
     */
    public static <MSG> void sendPacketToPlayer(MSG msg, ServerPlayerEntity player) {
        ModNetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    /**
     * 广播数据包至所有玩家
     */
    public static <MSG> void broadcastPacket(MSG msg) {
        NarcissusFarewell.getServerInstance().getPlayerList().getPlayers().forEach(player ->
                ModNetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg)
        );
    }

    // endregion 消息相关

    // region 跨维度传送

    public static boolean isTeleportAcrossDimensionEnabled(ServerPlayerEntity player, RegistryKey<World> to, EnumTeleportType type) {
        boolean result = true;
        if (player.level.dimension() != to) {
            if (ServerConfig.TELEPORT_ACROSS_DIMENSION.get()) {
                if (!NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, type)) {
                    result = false;
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "across_dimension_not_enable_for"), getCommand(type));
                }
            } else {
                result = false;
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "across_dimension_not_enable"));
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
                permission = ServerConfig.PERMISSION_TP_COORDINATE_ACROSS_DIMENSION.get();
                break;
            case TP_STRUCTURE:
                permission = ServerConfig.PERMISSION_TP_STRUCTURE_ACROSS_DIMENSION.get();
                break;
            case TP_ASK:
                permission = ServerConfig.PERMISSION_TP_ASK_ACROSS_DIMENSION.get();
                break;
            case TP_HERE:
                permission = ServerConfig.PERMISSION_TP_HERE_ACROSS_DIMENSION.get();
                break;
            case TP_RANDOM:
                permission = ServerConfig.PERMISSION_TP_RANDOM_ACROSS_DIMENSION.get();
                break;
            case TP_SPAWN:
                permission = ServerConfig.PERMISSION_TP_SPAWN_ACROSS_DIMENSION.get();
                break;
            case TP_WORLD_SPAWN:
                permission = ServerConfig.PERMISSION_TP_WORLD_SPAWN_ACROSS_DIMENSION.get();
                break;
            case TP_HOME:
                permission = ServerConfig.PERMISSION_TP_HOME_ACROSS_DIMENSION.get();
                break;
            case TP_STAGE:
                permission = ServerConfig.PERMISSION_TP_STAGE_ACROSS_DIMENSION.get();
                break;
            case TP_BACK:
                permission = ServerConfig.PERMISSION_TP_BACK_ACROSS_DIMENSION.get();
                break;
            case TP_GRAVE:
                permission = ServerConfig.PERMISSION_TP_GRAVE_ACROSS_DIMENSION.get();
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
        if (EnumCardType.REFUND_COOLDOWN.name().equalsIgnoreCase(CommonConfig.TELEPORT_CARD_TYPE.get())
                || EnumCardType.REFUND_ALL_COST_AND_COOLDOWN.name().equalsIgnoreCase(CommonConfig.TELEPORT_CARD_TYPE.get())
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
        switch (EnumCoolDownType.valueOf(ServerConfig.TELEPORT_REQUEST_COOLDOWN_TYPE.get())) {
            case COMMON:
                return calculateCooldown(player.getUUID(), current, lastTpTime, ServerConfig.TELEPORT_REQUEST_COOLDOWN.get(), null);
            case INDIVIDUAL:
                return calculateCooldown(player.getUUID(), current, lastTpTime, commandCoolDown, type);
            case MIXED:
                int globalCommandCoolDown = ServerConfig.TELEPORT_REQUEST_COOLDOWN.get();
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
                return ServerConfig.COOLDOWN_TP_COORDINATE.get();
            case TP_STRUCTURE:
                return ServerConfig.COOLDOWN_TP_STRUCTURE.get();
            case TP_ASK:
                return ServerConfig.COOLDOWN_TP_ASK.get();
            case TP_HERE:
                return ServerConfig.COOLDOWN_TP_HERE.get();
            case TP_RANDOM:
                return ServerConfig.COOLDOWN_TP_RANDOM.get();
            case TP_SPAWN:
                return ServerConfig.COOLDOWN_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
                return ServerConfig.COOLDOWN_TP_WORLD_SPAWN.get();
            case TP_TOP:
                return ServerConfig.COOLDOWN_TP_TOP.get();
            case TP_BOTTOM:
                return ServerConfig.COOLDOWN_TP_BOTTOM.get();
            case TP_UP:
                return ServerConfig.COOLDOWN_TP_UP.get();
            case TP_DOWN:
                return ServerConfig.COOLDOWN_TP_DOWN.get();
            case TP_VIEW:
                return ServerConfig.COOLDOWN_TP_VIEW.get();
            case TP_HOME:
                return ServerConfig.COOLDOWN_TP_HOME.get();
            case TP_STAGE:
                return ServerConfig.COOLDOWN_TP_STAGE.get();
            case TP_BACK:
                return ServerConfig.COOLDOWN_TP_BACK.get();
            case TP_GRAVE:
                return ServerConfig.COOLDOWN_TP_GRAVE.get();
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
    public static boolean validTeleportCost(ServerPlayerEntity player, Coordinate target, EnumTeleportType type, boolean submit) {
        return validateCost(player, target.dimension(), calculateDistance(new Coordinate(player), target), type, submit);
    }

    /**
     * 验证并收取传送代价
     *
     * @param request 传送请求
     * @param submit  是否收取代价
     * @return 是否验证通过
     */
    public static boolean validTeleportCost(TeleportRequest request, boolean submit) {
        Coordinate requesterCoordinate = new Coordinate(request.getRequester());
        Coordinate targetCoordinate = new Coordinate(request.getTarget());
        return validateCost(request.getRequester(), request.getTarget().getLevel().dimension(), calculateDistance(requesterCoordinate, targetCoordinate), request.getTeleportType(), submit);
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
            int limit = ServerConfig.TELEPORT_COST_DISTANCE_LIMIT.get();
            adjustedDistance = limit == 0 ? distance : Math.min(limit, distance);
        } else {
            adjustedDistance = ServerConfig.TELEPORT_COST_DISTANCE_ACROSS_DIMENSION.get();
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
            NarcissusUtils.sendTranslatableMessage(player
                    , I18nUtils.getKey(EnumI18nType.FORMAT, "cost_not_enough")
                    , Component.trans(NarcissusUtils.getPlayerLanguage(player)
                            , EnumI18nType.WORD, "teleport_card")
                    , cardNeed
            );
        }

        switch (teleportCost.getType()) {
            case EXP_POINT:
                result = player.totalExperience >= costNeed;
                if (!result) {
                    NarcissusUtils.sendTranslatableMessage(player
                            , I18nUtils.getKey(EnumI18nType.FORMAT, "cost_not_enough")
                            , Component.trans(NarcissusUtils.getPlayerLanguage(player)
                                    , EnumI18nType.WORD, "exp_point")
                            , costNeed
                    );
                } else if (submit) {
                    player.giveExperiencePoints(-costNeed);
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case EXP_LEVEL:
                result = player.experienceLevel >= costNeed;
                if (!result) {
                    NarcissusUtils.sendTranslatableMessage(player
                            , I18nUtils.getKey(EnumI18nType.FORMAT, "cost_not_enough")
                            , Component.trans(NarcissusUtils.getPlayerLanguage(player)
                                    , EnumI18nType.WORD, "exp_level")
                            , costNeed
                    );
                } else if (submit) {
                    player.giveExperienceLevels(-costNeed);
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case HEALTH:
                result = player.getHealth() > costNeed;
                if (!result) {
                    NarcissusUtils.sendTranslatableMessage(player
                            , I18nUtils.getKey(EnumI18nType.FORMAT, "cost_not_enough")
                            , Component.trans(NarcissusUtils.getPlayerLanguage(player)
                                    , EnumI18nType.WORD, "health")
                            , costNeed
                    );
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
                    NarcissusUtils.sendTranslatableMessage(player
                            , I18nUtils.getKey(EnumI18nType.FORMAT, "cost_not_enough")
                            , Component.trans(NarcissusUtils.getPlayerLanguage(player)
                                    , EnumI18nType.WORD, "hunger")
                            , costNeed
                    );
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
                        NarcissusUtils.sendMessage(player
                                , Component.trans(EnumI18nType.FORMAT, "cost_not_enough"
                                        , Component.literal(NarcissusUtils.getItemName(itemStack))
                                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemHover(itemStack)))
                                        , costNeed
                                )
                        );
                    } else if (submit) {
                        result = removeItemFromPlayerInventory(player, itemStack);
                        // 代价不足
                        if (result) {
                            data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                        } else {
                            NarcissusUtils.sendMessage(player
                                    , Component.trans(EnumI18nType.FORMAT, "cost_not_enough"
                                            , Component.literal(NarcissusUtils.getItemName(itemStack))
                                                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemHover(itemStack)))
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
                        result = NarcissusUtils.executeCommand(player, command);
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
        return result;
    }

    /**
     * 须支付多少传送卡
     */
    public static int getTeleportCardNeed(double need) {
        int ceil = (int) Math.ceil(need);
        if (!CommonConfig.TELEPORT_CARD.get()) return 0;
        switch (EnumCardType.valueOf(CommonConfig.TELEPORT_CARD_TYPE.get())) {
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
        if (!CommonConfig.TELEPORT_CARD.get()) return need;
        switch (EnumCardType.valueOf(CommonConfig.TELEPORT_CARD_TYPE.get())) {
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
                cost.setType(ServerConfig.COST_TP_COORDINATE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_COORDINATE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_COORDINATE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_COORDINATE_CONF.get());
                cost.setLower(ServerConfig.COST_TP_COORDINATE_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_COORDINATE_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_COORDINATE_EXP.get());
                break;
            case TP_STRUCTURE:
                cost.setType(ServerConfig.COST_TP_STRUCTURE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_STRUCTURE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_STRUCTURE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_STRUCTURE_CONF.get());
                cost.setLower(ServerConfig.COST_TP_STRUCTURE_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_STRUCTURE_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_STRUCTURE_EXP.get());
                break;
            case TP_ASK:
                cost.setType(ServerConfig.COST_TP_ASK_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_ASK_NUM.get());
                cost.setRate(ServerConfig.COST_TP_ASK_RATE.get());
                cost.setConf(ServerConfig.COST_TP_ASK_CONF.get());
                cost.setLower(ServerConfig.COST_TP_ASK_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_ASK_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_ASK_EXP.get());
                break;
            case TP_HERE:
                cost.setType(ServerConfig.COST_TP_HERE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_HERE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_HERE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_HERE_CONF.get());
                cost.setLower(ServerConfig.COST_TP_HERE_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_HERE_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_HERE_EXP.get());
                break;
            case TP_RANDOM:
                cost.setType(ServerConfig.COST_TP_RANDOM_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_RANDOM_NUM.get());
                cost.setRate(ServerConfig.COST_TP_RANDOM_RATE.get());
                cost.setConf(ServerConfig.COST_TP_RANDOM_CONF.get());
                cost.setLower(ServerConfig.COST_TP_RANDOM_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_RANDOM_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_RANDOM_EXP.get());
                break;
            case TP_SPAWN:
                cost.setType(ServerConfig.COST_TP_SPAWN_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_SPAWN_NUM.get());
                cost.setRate(ServerConfig.COST_TP_SPAWN_RATE.get());
                cost.setConf(ServerConfig.COST_TP_SPAWN_CONF.get());
                cost.setLower(ServerConfig.COST_TP_SPAWN_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_SPAWN_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_SPAWN_EXP.get());
                break;
            case TP_WORLD_SPAWN:
                cost.setType(ServerConfig.COST_TP_WORLD_SPAWN_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_WORLD_SPAWN_NUM.get());
                cost.setRate(ServerConfig.COST_TP_WORLD_SPAWN_RATE.get());
                cost.setConf(ServerConfig.COST_TP_WORLD_SPAWN_CONF.get());
                cost.setLower(ServerConfig.COST_TP_WORLD_SPAWN_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_WORLD_SPAWN_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_WORLD_SPAWN_EXP.get());
                break;
            case TP_TOP:
                cost.setType(ServerConfig.COST_TP_TOP_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_TOP_NUM.get());
                cost.setRate(ServerConfig.COST_TP_TOP_RATE.get());
                cost.setConf(ServerConfig.COST_TP_TOP_CONF.get());
                cost.setLower(ServerConfig.COST_TP_TOP_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_TOP_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_TOP_EXP.get());
                break;
            case TP_BOTTOM:
                cost.setType(ServerConfig.COST_TP_BOTTOM_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_BOTTOM_NUM.get());
                cost.setRate(ServerConfig.COST_TP_BOTTOM_RATE.get());
                cost.setConf(ServerConfig.COST_TP_BOTTOM_CONF.get());
                cost.setLower(ServerConfig.COST_TP_BOTTOM_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_BOTTOM_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_BOTTOM_EXP.get());
                break;
            case TP_UP:
                cost.setType(ServerConfig.COST_TP_UP_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_UP_NUM.get());
                cost.setRate(ServerConfig.COST_TP_UP_RATE.get());
                cost.setConf(ServerConfig.COST_TP_UP_CONF.get());
                cost.setLower(ServerConfig.COST_TP_UP_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_UP_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_UP_EXP.get());
                break;
            case TP_DOWN:
                cost.setType(ServerConfig.COST_TP_DOWN_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_DOWN_NUM.get());
                cost.setRate(ServerConfig.COST_TP_DOWN_RATE.get());
                cost.setConf(ServerConfig.COST_TP_DOWN_CONF.get());
                cost.setLower(ServerConfig.COST_TP_DOWN_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_DOWN_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_DOWN_EXP.get());
                break;
            case TP_VIEW:
                cost.setType(ServerConfig.COST_TP_VIEW_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_VIEW_NUM.get());
                cost.setRate(ServerConfig.COST_TP_VIEW_RATE.get());
                cost.setConf(ServerConfig.COST_TP_VIEW_CONF.get());
                cost.setLower(ServerConfig.COST_TP_VIEW_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_VIEW_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_VIEW_EXP.get());
                break;
            case TP_HOME:
                cost.setType(ServerConfig.COST_TP_HOME_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_HOME_NUM.get());
                cost.setRate(ServerConfig.COST_TP_HOME_RATE.get());
                cost.setConf(ServerConfig.COST_TP_HOME_CONF.get());
                cost.setLower(ServerConfig.COST_TP_HOME_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_HOME_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_HOME_EXP.get());
                break;
            case TP_STAGE:
                cost.setType(ServerConfig.COST_TP_STAGE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_STAGE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_STAGE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_STAGE_CONF.get());
                cost.setLower(ServerConfig.COST_TP_STAGE_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_STAGE_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_STAGE_EXP.get());
                break;
            case TP_BACK:
                cost.setType(ServerConfig.COST_TP_BACK_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_BACK_NUM.get());
                cost.setRate(ServerConfig.COST_TP_BACK_RATE.get());
                cost.setConf(ServerConfig.COST_TP_BACK_CONF.get());
                cost.setLower(ServerConfig.COST_TP_BACK_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_BACK_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_BACK_EXP.get());
                break;
            case TP_GRAVE:
                cost.setType(ServerConfig.COST_TP_GRAVE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_GRAVE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_GRAVE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_GRAVE_CONF.get());
                cost.setLower(ServerConfig.COST_TP_GRAVE_NUM_LOWER.get());
                cost.setUpper(ServerConfig.COST_TP_GRAVE_NUM_UPPER.get());
                cost.setExp(ServerConfig.COST_TP_GRAVE_EXP.get());
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

    public static double calculateDistance(Coordinate coordinate1, Coordinate coordinate2) {
        return coordinate1.distanceFrom(coordinate2);
    }

    // endregion 传送代价

    // region nbt文件读写

    public static CompoundNBT readCompressed(InputStream stream) {
        try {
            return CompressedStreamTools.readCompressed(stream);
        } catch (Exception e) {
            LOGGER.error("Failed to read compressed stream", e);
            return new CompoundNBT();
        }
    }

    public static CompoundNBT readCompressed(File file) {
        try {
            return CompressedStreamTools.readCompressed(file);
        } catch (Exception e) {
            LOGGER.error("Failed to read compressed file: {}", file.getAbsolutePath(), e);
            return new CompoundNBT();
        }
    }

    public static boolean writeCompressed(CompoundNBT tag, File file) {
        boolean result = false;
        try {
            CompressedStreamTools.writeCompressed(tag, file);
            result = true;
        } catch (Exception e) {
            LOGGER.error("Failed to write compressed file: {}", file.getAbsolutePath(), e);
        }
        return result;
    }

    public static boolean writeCompressed(CompoundNBT tag, OutputStream stream) {
        boolean result = false;
        try {
            CompressedStreamTools.writeCompressed(tag, stream);
            result = true;
        } catch (Exception e) {
            LOGGER.error("Failed to write compressed stream", e);
        }
        return result;
    }

    // endregion nbt文件读写

    // region 杂项

    public static String getPlayerLanguage(PlayerEntity player) {
        try {
            return NarcissusUtils.getValidLanguage(player, CustomConfig.getPlayerLanguage(getPlayerUUIDString(player)));
        } catch (IllegalArgumentException i) {
            return ServerConfig.DEFAULT_LANGUAGE.get();
        }
    }

    public static String getValidLanguage(@Nullable PlayerEntity player, @Nullable String language) {
        String result;
        if (StringUtils.isNullOrEmptyEx(language) || "client".equalsIgnoreCase(language)) {
            if (player instanceof ServerPlayerEntity) {
                result = NarcissusUtils.getServerPlayerLanguage((ServerPlayerEntity) player);
            } else {
                result = NarcissusUtils.getClientLanguage();
            }
        } else if ("server".equalsIgnoreCase(language)) {
            result = ServerConfig.DEFAULT_LANGUAGE.get();
        } else {
            result = language;
        }
        return result;
    }

    public static String getServerPlayerLanguage(ServerPlayerEntity player) {
        return PlayerLanguageManager.get(player);
    }

    /**
     * 复制玩家语言设置
     *
     * @param originalPlayer 原始玩家
     * @param targetPlayer   目标玩家
     */
    public static void clonePlayerLanguage(ServerPlayerEntity originalPlayer, ServerPlayerEntity targetPlayer) {
        ((ServerPlayerAccessor) targetPlayer).narcissus$language(((ServerPlayerAccessor) originalPlayer).narcissus$language());
    }

    public static String getClientLanguage() {
        return Minecraft.getInstance().getLanguageManager().getSelected().getCode();
    }

    public static String getPlayerName(@NonNull PlayerEntity player) {
        return player.getDisplayName().getString();
    }

    public static String getPlayerUUIDString(@NonNull PlayerEntity player) {
        return player.getUUID().toString();
    }

    public static String getPlayerNameByUUIDString(String uuid) {
        return UsernameCache.getMap().getOrDefault(UUID.fromString(uuid), "UnknownPlayer");
    }

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
     * 序列化方块默认状态
     */
    public static String serializeBlockState(Block block) {
        return serializeBlockState(block.defaultBlockState());
    }

    /**
     * 序列化方块状态
     */
    public static String serializeBlockState(BlockState blockState) {
        return BlockStateParser.serialize(blockState);
    }

    /**
     * 反序列化方块状态
     */
    public static BlockState deserializeBlockState(String block) {
        try {
            return new BlockStateParser(new StringReader(block), false).parse(true).getState();
        } catch (Exception e) {
            LOGGER.error("Invalid unsafe block: {}", block, e);
            return null;
        }
    }

    /**
     * 获取方块注册ID
     */
    @NonNull
    public static String getBlockRegistryName(BlockState blockState) {
        return getBlockRegistryName(blockState.getBlock());
    }

    /**
     * 获取方块注册ID
     */
    @NonNull
    public static String getBlockRegistryName(Block block) {
        ResourceLocation location = block.getRegistryName();
        return location == null ? "" : location.toString();
    }

    public static Block getBlockFromRegistryName(String location) {
        return ForgeRegistries.BLOCKS.getValue(Identifier.parse(location));
    }

    /**
     * 判断玩家是否被任何敌对生物锁定为攻击目标
     */
    public static boolean isTargetedByHostile(ServerPlayerEntity player) {
        return player.level.getEntitiesOfClass(MobEntity.class, player.getBoundingBox()
                        .inflate(ServerConfig.TP_WITH_FOLLOWER_RANGE.get()))
                .stream()
                .anyMatch(entity -> player.equals(entity.getTarget())
                        || (entity.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) && player.equals(entity.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null))
                );
    }

    public static String getItemName(ItemStack itemStack) {
        return itemStack.getDisplayName().getString();
    }

    public static String getItemName(Item item) {
        return getItemName(new ItemStack(item));
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
