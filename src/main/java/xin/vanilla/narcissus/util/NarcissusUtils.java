package xin.vanilla.narcissus.util;

import lombok.NonNull;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITempt;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.network.Packet;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.network.play.server.SPacketSetPassengers;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatList;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.*;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.IPlayerTeleportData;
import xin.vanilla.narcissus.data.player.PlayerTeleportDataCapability;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.*;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class NarcissusUtils {

    private static List<String> structureList = new ArrayList<>();

    private static final Logger LOGGER = LogManager.getLogger();

    // region 指令相关

    public static String getCommandPrefix() {
        String commandPrefix = ServerConfig.COMMAND_PREFIX;
        if (StringUtils.isNullOrEmptyEx(commandPrefix) || !commandPrefix.matches("^(\\w ?)+$")) {
            ServerConfig.COMMAND_PREFIX = NarcissusFarewell.DEFAULT_COMMAND_PREFIX;
        }
        return ServerConfig.COMMAND_PREFIX.trim();
    }

    /**
     * 判断指令类型是否开启
     *
     * @param type 指令类型
     */
    public static boolean isCommandEnabled(ECommandType type) {
        switch (type) {
            case CARD:
            case SET_CARD:
            case CARD_CONCISE:
            case SET_CARD_CONCISE:
                return ServerConfig.TELEPORT_CARD;
            case SHARE:
            case SHARE_CONCISE:
                return ServerConfig.SWITCH_SHARE;
            case FEED:
            case FEED_OTHER:
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return ServerConfig.SWITCH_FEED;
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return ServerConfig.SWITCH_TP_COORDINATE;
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return ServerConfig.SWITCH_TP_STRUCTURE;
            case TP_ASK:
            case TP_ASK_YES:
            case TP_ASK_NO:
            case TP_ASK_CANCEL:
            case TP_ASK_CONCISE:
            case TP_ASK_YES_CONCISE:
            case TP_ASK_NO_CONCISE:
            case TP_ASK_CANCEL_CONCISE:
                return ServerConfig.SWITCH_TP_ASK;
            case TP_HERE:
            case TP_HERE_YES:
            case TP_HERE_NO:
            case TP_HERE_CANCEL:
            case TP_HERE_CONCISE:
            case TP_HERE_YES_CONCISE:
            case TP_HERE_NO_CONCISE:
            case TP_HERE_CANCEL_CONCISE:
                return ServerConfig.SWITCH_TP_HERE;
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return ServerConfig.SWITCH_TP_RANDOM;
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return ServerConfig.SWITCH_TP_SPAWN;
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return ServerConfig.SWITCH_TP_WORLD_SPAWN;
            case TP_TOP:
            case TP_TOP_CONCISE:
                return ServerConfig.SWITCH_TP_TOP;
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return ServerConfig.SWITCH_TP_BOTTOM;
            case TP_UP:
            case TP_UP_CONCISE:
                return ServerConfig.SWITCH_TP_UP;
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return ServerConfig.SWITCH_TP_DOWN;
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return ServerConfig.SWITCH_TP_VIEW;
            case TP_HOME:
            case SET_HOME:
            case DEL_HOME:
            case GET_HOME:
            case TP_HOME_CONCISE:
            case SET_HOME_CONCISE:
            case DEL_HOME_CONCISE:
            case GET_HOME_CONCISE:
                return ServerConfig.SWITCH_TP_HOME;
            case TP_STAGE:
            case SET_STAGE:
            case DEL_STAGE:
            case GET_STAGE:
            case TP_STAGE_CONCISE:
            case SET_STAGE_CONCISE:
            case DEL_STAGE_CONCISE:
            case GET_STAGE_CONCISE:
                return ServerConfig.SWITCH_TP_STAGE;
            case TP_BACK:
            case TP_BACK_CONCISE:
                return ServerConfig.SWITCH_TP_BACK;
            default:
                return true;
        }
    }

    /**
     * 判断传送类型是否开启
     *
     * @param type 传送类型
     */
    public static boolean isTeleportEnabled(ECommandType type) {
        switch (type) {
            case FEED:
            case FEED_OTHER:
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return ServerConfig.SWITCH_FEED;
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return ServerConfig.SWITCH_TP_COORDINATE;
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return ServerConfig.SWITCH_TP_STRUCTURE;
            case TP_ASK:
            case TP_ASK_YES:
            case TP_ASK_NO:
            case TP_ASK_CONCISE:
            case TP_ASK_YES_CONCISE:
            case TP_ASK_NO_CONCISE:
                return ServerConfig.SWITCH_TP_ASK;
            case TP_HERE:
            case TP_HERE_YES:
            case TP_HERE_NO:
            case TP_HERE_CONCISE:
            case TP_HERE_YES_CONCISE:
            case TP_HERE_NO_CONCISE:
                return ServerConfig.SWITCH_TP_HERE;
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return ServerConfig.SWITCH_TP_RANDOM;
            case TP_SPAWN:
            case TP_SPAWN_CONCISE:
                return ServerConfig.SWITCH_TP_SPAWN;
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return ServerConfig.SWITCH_TP_WORLD_SPAWN;
            case TP_TOP:
            case TP_TOP_CONCISE:
                return ServerConfig.SWITCH_TP_TOP;
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return ServerConfig.SWITCH_TP_BOTTOM;
            case TP_UP:
            case TP_UP_CONCISE:
                return ServerConfig.SWITCH_TP_UP;
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return ServerConfig.SWITCH_TP_DOWN;
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return ServerConfig.SWITCH_TP_VIEW;
            case TP_HOME:
            case SET_HOME:
            case DEL_HOME:
            case GET_HOME:
            case TP_HOME_CONCISE:
            case SET_HOME_CONCISE:
            case DEL_HOME_CONCISE:
            case GET_HOME_CONCISE:
                return ServerConfig.SWITCH_TP_HOME;
            case TP_STAGE:
            case SET_STAGE:
            case DEL_STAGE:
            case GET_STAGE:
            case TP_STAGE_CONCISE:
            case SET_STAGE_CONCISE:
            case DEL_STAGE_CONCISE:
            case GET_STAGE_CONCISE:
                return ServerConfig.SWITCH_TP_STAGE;
            case TP_BACK:
            case TP_BACK_CONCISE:
                return ServerConfig.SWITCH_TP_BACK;
            default:
                return true;
        }
    }

    public static String getCommand(ETeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return ServerConfig.COMMAND_TP_COORDINATE;
            case TP_STRUCTURE:
                return ServerConfig.COMMAND_TP_STRUCTURE;
            case TP_ASK:
                return ServerConfig.COMMAND_TP_ASK;
            case TP_HERE:
                return ServerConfig.COMMAND_TP_HERE;
            case TP_RANDOM:
                return ServerConfig.COMMAND_TP_RANDOM;
            case TP_SPAWN:
                return ServerConfig.COMMAND_TP_SPAWN;
            case TP_WORLD_SPAWN:
                return ServerConfig.COMMAND_TP_WORLD_SPAWN;
            case TP_TOP:
                return ServerConfig.COMMAND_TP_TOP;
            case TP_BOTTOM:
                return ServerConfig.COMMAND_TP_BOTTOM;
            case TP_UP:
                return ServerConfig.COMMAND_TP_UP;
            case TP_DOWN:
                return ServerConfig.COMMAND_TP_DOWN;
            case TP_VIEW:
                return ServerConfig.COMMAND_TP_VIEW;
            case TP_HOME:
                return ServerConfig.COMMAND_TP_HOME;
            case TP_STAGE:
                return ServerConfig.COMMAND_TP_STAGE;
            case TP_BACK:
                return ServerConfig.COMMAND_TP_BACK;
            default:
                return "";
        }
    }

    public static String getCommand(ECommandType type) {
        return getCommand(type, true);
    }

    public static String getCommand(ECommandType type, boolean full) {
        String prefix = full ? NarcissusUtils.getCommandPrefix() + " " : "";
        switch (type) {
            case HELP:
                return prefix + "help";
            case LANGUAGE:
                return prefix + ServerConfig.COMMAND_LANGUAGE;
            case LANGUAGE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_LANGUAGE : "";
            case DIMENSION:
                return prefix + ServerConfig.COMMAND_DIMENSION;
            case UUID:
                return prefix + ServerConfig.COMMAND_UUID;
            case UUID_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_UUID : "";
            case CARD:
            case SET_CARD:
                return prefix + ServerConfig.COMMAND_CARD;
            case CARD_CONCISE:
            case SET_CARD_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_CARD : "";
            case SHARE:
                return prefix + ServerConfig.COMMAND_SHARE;
            case SHARE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_SHARE : "";
            case FEED:
            case FEED_OTHER:
                return prefix + ServerConfig.COMMAND_FEED;
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_FEED : "";
            case TP_COORDINATE:
                return prefix + ServerConfig.COMMAND_TP_COORDINATE;
            case TP_COORDINATE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_COORDINATE : "";
            case TP_STRUCTURE:
                return prefix + ServerConfig.COMMAND_TP_STRUCTURE;
            case TP_STRUCTURE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_STRUCTURE : "";
            case TP_ASK:
                return prefix + ServerConfig.COMMAND_TP_ASK;
            case TP_ASK_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK : "";
            case TP_ASK_YES:
                return prefix + ServerConfig.COMMAND_TP_ASK_YES;
            case TP_ASK_YES_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK_YES : "";
            case TP_ASK_NO:
                return prefix + ServerConfig.COMMAND_TP_ASK_NO;
            case TP_ASK_NO_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK_NO : "";
            case TP_ASK_CANCEL:
                return prefix + ServerConfig.COMMAND_TP_ASK_CANCEL;
            case TP_ASK_CANCEL_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK_CANCEL : "";
            case TP_HERE:
                return prefix + ServerConfig.COMMAND_TP_HERE;
            case TP_HERE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE : "";
            case TP_HERE_YES:
                return prefix + ServerConfig.COMMAND_TP_HERE_YES;
            case TP_HERE_YES_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE_YES : "";
            case TP_HERE_NO:
                return prefix + ServerConfig.COMMAND_TP_HERE_NO;
            case TP_HERE_NO_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE_NO : "";
            case TP_HERE_CANCEL:
                return prefix + ServerConfig.COMMAND_TP_HERE_CANCEL;
            case TP_HERE_CANCEL_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE_CANCEL : "";
            case TP_RANDOM:
                return prefix + ServerConfig.COMMAND_TP_RANDOM;
            case TP_RANDOM_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_RANDOM : "";
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
                return prefix + ServerConfig.COMMAND_TP_SPAWN;
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_SPAWN : "";
            case TP_WORLD_SPAWN:
                return prefix + ServerConfig.COMMAND_TP_WORLD_SPAWN;
            case TP_WORLD_SPAWN_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_WORLD_SPAWN : "";
            case TP_TOP:
                return prefix + ServerConfig.COMMAND_TP_TOP;
            case TP_TOP_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_TOP : "";
            case TP_BOTTOM:
                return prefix + ServerConfig.COMMAND_TP_BOTTOM;
            case TP_BOTTOM_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_BOTTOM : "";
            case TP_UP:
                return prefix + ServerConfig.COMMAND_TP_UP;
            case TP_UP_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_UP : "";
            case TP_DOWN:
                return prefix + ServerConfig.COMMAND_TP_DOWN;
            case TP_DOWN_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_DOWN : "";
            case TP_VIEW:
                return prefix + ServerConfig.COMMAND_TP_VIEW;
            case TP_VIEW_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_VIEW : "";
            case TP_HOME:
                return prefix + ServerConfig.COMMAND_TP_HOME;
            case TP_HOME_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HOME : "";
            case SET_HOME:
                return prefix + ServerConfig.COMMAND_SET_HOME;
            case SET_HOME_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_SET_HOME : "";
            case DEL_HOME:
                return prefix + ServerConfig.COMMAND_DEL_HOME;
            case DEL_HOME_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_DEL_HOME : "";
            case GET_HOME:
                return prefix + ServerConfig.COMMAND_GET_HOME;
            case GET_HOME_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_GET_HOME : "";
            case TP_STAGE:
                return prefix + ServerConfig.COMMAND_TP_STAGE;
            case TP_STAGE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_STAGE : "";
            case SET_STAGE:
                return prefix + ServerConfig.COMMAND_SET_STAGE;
            case SET_STAGE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_SET_STAGE : "";
            case DEL_STAGE:
                return prefix + ServerConfig.COMMAND_DEL_STAGE;
            case DEL_STAGE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_DEL_STAGE : "";
            case GET_STAGE:
                return prefix + ServerConfig.COMMAND_GET_STAGE;
            case GET_STAGE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_GET_STAGE : "";
            case TP_BACK:
                return prefix + ServerConfig.COMMAND_TP_BACK;
            case TP_BACK_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_BACK : "";
            case VIRTUAL_OP:
                return prefix + ServerConfig.COMMAND_VIRTUAL_OP;
            case VIRTUAL_OP_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_VIRTUAL_OP : "";
            default:
                return "";
        }
    }

    public static int getCommandPermissionLevel(ECommandType type) {
        switch (type) {
            case SET_CARD:
            case SET_CARD_CONCISE:
                return ServerConfig.PERMISSION_SET_CARD;
            case FEED_OTHER:
            case FEED_OTHER_CONCISE:
                return ServerConfig.PERMISSION_FEED_OTHER;
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return ServerConfig.PERMISSION_TP_COORDINATE;
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return ServerConfig.PERMISSION_TP_STRUCTURE;
            case TP_ASK:
            case TP_ASK_CANCEL:
                // case TP_ASK_YES:
                // case TP_ASK_NO:
            case TP_ASK_CONCISE:
            case TP_ASK_CANCEL_CONCISE:
                // case TP_ASK_YES_CONCISE:
                // case TP_ASK_NO_CONCISE:
                return ServerConfig.PERMISSION_TP_ASK;
            case TP_HERE:
            case TP_HERE_CANCEL:
                // case TP_HERE_YES:
                // case TP_HERE_NO:
            case TP_HERE_CONCISE:
            case TP_HERE_CANCEL_CONCISE:
                // case TP_HERE_YES_CONCISE:
                // case TP_HERE_NO_CONCISE:
                return ServerConfig.PERMISSION_TP_HERE;
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return ServerConfig.PERMISSION_TP_RANDOM;
            case TP_SPAWN:
            case TP_SPAWN_CONCISE:
                return ServerConfig.PERMISSION_TP_SPAWN;
            case TP_SPAWN_OTHER:
            case TP_SPAWN_OTHER_CONCISE:
                return ServerConfig.PERMISSION_TP_SPAWN_OTHER;
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return ServerConfig.PERMISSION_TP_WORLD_SPAWN;
            case TP_TOP:
            case TP_TOP_CONCISE:
                return ServerConfig.PERMISSION_TP_TOP;
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return ServerConfig.PERMISSION_TP_BOTTOM;
            case TP_UP:
            case TP_UP_CONCISE:
                return ServerConfig.PERMISSION_TP_UP;
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return ServerConfig.PERMISSION_TP_DOWN;
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return ServerConfig.PERMISSION_TP_VIEW;
            case TP_HOME:
            case SET_HOME:
            case DEL_HOME:
            case GET_HOME:
            case TP_HOME_CONCISE:
            case SET_HOME_CONCISE:
            case DEL_HOME_CONCISE:
            case GET_HOME_CONCISE:
                return ServerConfig.PERMISSION_TP_HOME;
            case TP_STAGE:
            case TP_STAGE_CONCISE:
                return ServerConfig.PERMISSION_TP_STAGE;
            case SET_STAGE:
            case SET_STAGE_CONCISE:
                return ServerConfig.PERMISSION_SET_STAGE;
            case DEL_STAGE:
            case DEL_STAGE_CONCISE:
                return ServerConfig.PERMISSION_DEL_STAGE;
            case GET_STAGE:
            case GET_STAGE_CONCISE:
                return ServerConfig.PERMISSION_GET_STAGE;
            case TP_BACK:
            case TP_BACK_CONCISE:
                return ServerConfig.PERMISSION_TP_BACK;
            case VIRTUAL_OP:
            case VIRTUAL_OP_CONCISE:
                return ServerConfig.PERMISSION_VIRTUAL_OP;
            default:
                return 0;
        }
    }

    public static int getCommandPermissionLevel(ETeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return ServerConfig.PERMISSION_TP_COORDINATE;
            case TP_STRUCTURE:
                return ServerConfig.PERMISSION_TP_STRUCTURE;
            case TP_ASK:
                return ServerConfig.PERMISSION_TP_ASK;
            case TP_HERE:
                return ServerConfig.PERMISSION_TP_HERE;
            case TP_RANDOM:
                return ServerConfig.PERMISSION_TP_RANDOM;
            case TP_SPAWN:
                return ServerConfig.PERMISSION_TP_SPAWN;
            case TP_WORLD_SPAWN:
                return ServerConfig.PERMISSION_TP_WORLD_SPAWN;
            case TP_TOP:
                return ServerConfig.PERMISSION_TP_TOP;
            case TP_BOTTOM:
                return ServerConfig.PERMISSION_TP_BOTTOM;
            case TP_UP:
                return ServerConfig.PERMISSION_TP_UP;
            case TP_DOWN:
                return ServerConfig.PERMISSION_TP_DOWN;
            case TP_VIEW:
                return ServerConfig.PERMISSION_TP_VIEW;
            case TP_HOME:
                return ServerConfig.PERMISSION_TP_HOME;
            case TP_STAGE:
                return ServerConfig.PERMISSION_TP_STAGE;
            case TP_BACK:
                return ServerConfig.PERMISSION_TP_BACK;
            default:
                return 0;
        }
    }

    public static boolean isConciseEnabled(ECommandType type) {
        switch (type) {
            case LANGUAGE:
            case LANGUAGE_CONCISE:
                return ServerConfig.CONCISE_LANGUAGE;
            case UUID:
            case UUID_CONCISE:
                return ServerConfig.CONCISE_UUID;
            case DIMENSION:
            case DIMENSION_CONCISE:
                return ServerConfig.CONCISE_DIMENSION;
            case CARD:
            case CARD_CONCISE:
            case SET_CARD:
            case SET_CARD_CONCISE:
                return ServerConfig.CONCISE_CARD;
            case SHARE:
            case SHARE_CONCISE:
                return ServerConfig.CONCISE_SHARE;
            case FEED:
            case FEED_OTHER:
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return ServerConfig.CONCISE_FEED;
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return ServerConfig.CONCISE_TP_COORDINATE;
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return ServerConfig.CONCISE_TP_STRUCTURE;
            case TP_ASK:
            case TP_ASK_CONCISE:
                return ServerConfig.CONCISE_TP_ASK;
            case TP_ASK_YES:
            case TP_ASK_YES_CONCISE:
                return ServerConfig.CONCISE_TP_ASK_YES;
            case TP_ASK_NO:
            case TP_ASK_NO_CONCISE:
                return ServerConfig.CONCISE_TP_ASK_NO;
            case TP_ASK_CANCEL:
            case TP_ASK_CANCEL_CONCISE:
                return ServerConfig.CONCISE_TP_ASK_CANCEL;
            case TP_HERE:
            case TP_HERE_CONCISE:
                return ServerConfig.CONCISE_TP_HERE;
            case TP_HERE_YES:
            case TP_HERE_YES_CONCISE:
                return ServerConfig.CONCISE_TP_HERE_YES;
            case TP_HERE_NO:
            case TP_HERE_NO_CONCISE:
                return ServerConfig.CONCISE_TP_HERE_NO;
            case TP_HERE_CANCEL:
            case TP_HERE_CANCEL_CONCISE:
                return ServerConfig.CONCISE_TP_HERE_CANCEL;
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return ServerConfig.CONCISE_TP_RANDOM;
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return ServerConfig.CONCISE_TP_SPAWN;
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return ServerConfig.CONCISE_TP_WORLD_SPAWN;
            case TP_TOP:
            case TP_TOP_CONCISE:
                return ServerConfig.CONCISE_TP_TOP;
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return ServerConfig.CONCISE_TP_BOTTOM;
            case TP_UP:
            case TP_UP_CONCISE:
                return ServerConfig.CONCISE_TP_UP;
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return ServerConfig.CONCISE_TP_DOWN;
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return ServerConfig.CONCISE_TP_VIEW;
            case TP_HOME:
            case TP_HOME_CONCISE:
                return ServerConfig.CONCISE_TP_HOME;
            case SET_HOME:
            case SET_HOME_CONCISE:
                return ServerConfig.CONCISE_SET_HOME;
            case DEL_HOME:
            case DEL_HOME_CONCISE:
                return ServerConfig.CONCISE_DEL_HOME;
            case GET_HOME:
            case GET_HOME_CONCISE:
                return ServerConfig.CONCISE_GET_HOME;
            case TP_STAGE:
            case TP_STAGE_CONCISE:
                return ServerConfig.CONCISE_TP_STAGE;
            case SET_STAGE:
            case SET_STAGE_CONCISE:
                return ServerConfig.CONCISE_SET_STAGE;
            case DEL_STAGE:
            case DEL_STAGE_CONCISE:
                return ServerConfig.CONCISE_DEL_STAGE;
            case GET_STAGE:
            case GET_STAGE_CONCISE:
                return ServerConfig.CONCISE_GET_STAGE;
            case TP_BACK:
            case TP_BACK_CONCISE:
                return ServerConfig.CONCISE_TP_BACK;
            case VIRTUAL_OP:
            case VIRTUAL_OP_CONCISE:
                return ServerConfig.CONCISE_VIRTUAL_OP;
            default:
                return false;
        }
    }

    public static boolean hasCommandPermission(ICommandSender player, ECommandType type) {
        int permissionLevel = getCommandPermissionLevel(type);
        return (permissionLevel > -1 && hasPermissions(player, permissionLevel)) || hasVirtualPermission(player, type);
    }

    public static boolean hasVirtualPermission(ICommandSender player, ECommandType type) {
        // 若为玩家
        if (player instanceof EntityPlayer) {
            return VirtualPermissionManager.getVirtualPermission((EntityPlayer) player).stream()
                    .filter(Objects::nonNull)
                    .anyMatch(s -> s.replaceConcise() == type.replaceConcise());
        } else {
            return false;
        }
    }

    /**
     * 判断玩家是否拥有指定的 OP 权限等级或更高权限
     *
     * @param player 目标玩家
     * @param level  要求的最低权限等级
     */
    public static boolean hasPermissions(ICommandSender player, int level) {
        // if (player == null || level < 0 || level > 4) {
        //     return false;
        // }
        // MinecraftServer server = NarcissusFarewell.getServerInstance();
        // if (server == null) {
        //     return false;
        // }
        // int permLevel = 0;
        // if (NarcissusFarewell.getServerInstance().getPlayerList().canSendCommands(player.getGameProfile())) {
        //     UserListOpsEntry opsEntry = NarcissusFarewell.getServerInstance().getPlayerList().getOppedPlayers().getEntry(player.getGameProfile());
        //     // idea又坑老实人，opsEntry是会为null的
        //     if (opsEntry != null) {
        //         permLevel = opsEntry.getPermissionLevel();
        //     }
        // }
        // return permLevel >= level;
        return level <= 0 || player.canUseCommand(level, "");
    }

    // endregion 指令相关

    // region 安全坐标

    /**
     * 不安全的方块
     */
    private static final List<Block> UNSAFE_BLOCKS = Arrays.stream(ServerConfig.UNSAFE_BLOCKS)
            .map(Block::getBlockFromName)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    private static final List<Block> SUFFOCATING_BLOCKS = Arrays.stream(ServerConfig.SUFFOCATING_BLOCKS)
            .map(Block::getBlockFromName)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    private static final List<Block> SAFE_BLOCKS = Arrays.stream(ServerConfig.SAFE_BLOCKS)
            .map(Block::getBlockFromName)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());

    public static int getWorldMinY(World world) {
        return 0;
    }

    public static int getWorldMaxY(World world) {
        return world.getHeight();
    }

    public static Coordinate findTopCandidate(WorldServer world, Coordinate start) {
        if (start.getY() >= NarcissusUtils.getWorldMaxY(world)) return null;
        for (int y : IntStream.range((int) start.getY() + 1, NarcissusUtils.getWorldMaxY(world)).boxed()
                .sorted(Comparator.comparingInt(Integer::intValue).reversed())
                .collect(Collectors.toList())) {
            Coordinate candidate = new Coordinate().setX(start.getX()).setY(y).setZ(start.getZ())
                    .setYaw(start.getYaw()).setPitch(start.getPitch())
                    .setDimension(start.getDimension())
                    .setSafe(start.isSafe()).setSafeMode(start.getSafeMode());
            if (isSafeCoordinate(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static Coordinate findBottomCandidate(WorldServer world, Coordinate start) {
        if (start.getY() <= NarcissusUtils.getWorldMinY(world)) return null;
        for (int y : IntStream.range(NarcissusUtils.getWorldMinY(world), (int) start.getY() - 1).boxed()
                .sorted(Comparator.comparingInt(Integer::intValue))
                .collect(Collectors.toList())) {
            Coordinate candidate = new Coordinate().setX(start.getX()).setY(y).setZ(start.getZ())
                    .setYaw(start.getYaw()).setPitch(start.getPitch())
                    .setDimension(start.getDimension())
                    .setSafe(start.isSafe()).setSafeMode(start.getSafeMode());
            if (isSafeCoordinate(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static Coordinate findUpCandidate(WorldServer world, Coordinate start) {
        if (start.getY() >= NarcissusUtils.getWorldMaxY(world)) return null;
        for (int y : IntStream.range((int) start.getY() + 1, NarcissusUtils.getWorldMaxY(world)).boxed()
                .sorted(Comparator.comparingInt(a -> a - (int) start.getY()))
                .collect(Collectors.toList())) {
            Coordinate candidate = new Coordinate().setX(start.getX()).setY(y).setZ(start.getZ())
                    .setYaw(start.getYaw()).setPitch(start.getPitch())
                    .setDimension(start.getDimension())
                    .setSafe(start.isSafe()).setSafeMode(start.getSafeMode());
            if (isSafeCoordinate(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static Coordinate findDownCandidate(WorldServer world, Coordinate start) {
        if (start.getY() <= NarcissusUtils.getWorldMinY(world)) return null;
        for (int y : IntStream.range(NarcissusUtils.getWorldMinY(world), (int) start.getY() - 1).boxed()
                .sorted(Comparator.comparingInt(a -> (int) start.getY() - a))
                .collect(Collectors.toList())) {
            Coordinate candidate = new Coordinate().setX(start.getX()).setY(y).setZ(start.getZ())
                    .setYaw(start.getYaw()).setPitch(start.getPitch())
                    .setDimension(start.getDimension())
                    .setSafe(start.isSafe()).setSafeMode(start.getSafeMode());
            if (isSafeCoordinate(world, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    public static Coordinate findViewEndCandidate(EntityPlayerMP player, boolean safe, int range) {
        double stepScale = 0.75;
        Coordinate start = new Coordinate(player);
        Coordinate result = null;

        // 获取玩家的起始位置
        Vec3d startPosition = player.getPositionEyes(1.0F);

        // 获取玩家的视线方向
        Vec3d direction = player.getLook(1.0F).normalize();
        // 步长
        Vec3d stepVector = direction.scale(stepScale);

        // 初始化变量
        Vec3d currentPosition = startPosition;
        World world = player.world;

        // 从近到远寻找碰撞点
        for (int stepCount = 0; stepCount <= range; stepCount++) {
            // 更新当前检测位置
            currentPosition = startPosition.add(stepVector.scale(stepCount));
            BlockPos currentBlockPos = new BlockPos(currentPosition.x, currentPosition.y, currentPosition.z);

            // 获取当前方块状态
            IBlockState blockState = world.getBlockState(currentBlockPos);

            // 检测方块是否不可穿过
            if (blockState.getMaterial().blocksMovement()) {
                result = start.clone().fromVector3d(startPosition.add(stepVector.scale(stepCount - 1)));
                break;
            }
        }

        // 若未找到碰撞点，则使用射线的终点
        if (result == null) {
            result = start.clone().fromVector3d(currentPosition);
        }

        // 若需寻找安全坐标，则从碰撞点反向查找安全位置
        if (safe) {
            // 碰撞点的三维向量
            Vec3d collisionVector = result.toVector3d();
            for (int stepCount = (int) Math.ceil(collisionVector.distanceTo(startPosition) / stepScale); stepCount >= 0; stepCount--) {
                currentPosition = startPosition.add(stepVector.scale(stepCount));
                BlockPos currentBlockPos = new BlockPos(currentPosition.x, currentPosition.y, currentPosition.z);
                for (int yOffset = -3; yOffset < 3; yOffset++) {
                    Coordinate candidate = start.clone().fromBlockPos(currentBlockPos).addY(yOffset);
                    // 判断当前候选坐标是否安全
                    if (isSafeCoordinate(world, candidate)) {
                        result = candidate.addX(0.5).addY(0.15).addZ(0.5);
                        stepCount = 0;
                        break;
                    }
                }
            }
        }
        // 若起点与结果相同则返回null
        if (start.equalsOfRange(result, 1)) {
            result = null;
        }
        return result;
    }

    public static Coordinate findSafeCoordinate(Coordinate coordinate, boolean belowAllowAir) {
        World world = DimensionManager.getWorld(coordinate.getDimension().getId(), true);
        DimensionManager.keepDimensionLoaded(coordinate.getDimension().getId(), true);

        int chunkX = (int) coordinate.getX() >> 4;
        int chunkZ = (int) coordinate.getZ() >> 4;

        return searchForSafeCoordinateInChunk(world, coordinate, chunkX, chunkZ, belowAllowAir);
    }

    private static int deterministicHash(Coordinate c) {
        int prime = 31;
        int hash = 1;
        hash = prime * hash + Integer.hashCode(c.getXInt());
        hash = prime * hash + Integer.hashCode(c.getYInt());
        hash = prime * hash + Integer.hashCode(c.getZInt());
        return hash;
    }

    private static Coordinate searchForSafeCoordinateInChunk(World world, Coordinate coordinate, int chunkX, int chunkZ, boolean belowAllowAir) {
        // 搜索安全位置，限制在目标范围区块内
        int offset = (ServerConfig.SAFE_CHUNK_RANGE - 1) * 16;
        int chunkMinX = (chunkX << 4) - offset;
        int chunkMinZ = (chunkZ << 4) - offset;
        int chunkMaxX = chunkMinX + 15 + offset;
        int chunkMaxZ = chunkMinZ + 15 + offset;

        Coordinate result = coordinate.clone();
        List<Coordinate> coordinates = new ArrayList<>();
        Comparator<Coordinate> comparator = (c1, c2) -> {
            // 计算各项距离
            double dist3D_1 = coordinate.distanceFrom(c1);
            double dist3D_2 = coordinate.distanceFrom(c2);
            double dist2D_1 = coordinate.distanceFrom2D(c1);
            double dist2D_2 = coordinate.distanceFrom2D(c2);
            double yDiff1 = Math.abs(coordinate.getY() - c1.getY());
            double yDiff2 = Math.abs(coordinate.getY() - c2.getY());

            // 分组
            int group1 = (dist3D_1 <= 16) ? 1 : (dist2D_1 <= 8 ? 2 : 3);
            int group2 = (dist3D_2 <= 16) ? 1 : (dist2D_2 <= 8 ? 2 : 3);

            // 先按组排序
            if (group1 != group2) {
                return group1 - group2;
            }

            // 同组内的排序规则：
            if (group1 == 1) {
                // 按三维距离排序
                return Double.compare(dist3D_1, dist3D_2);
            } else if (group1 == 2) {
                // 先按二维距离，再按 Y 轴偏差排序
                int cmp = Double.compare(dist2D_1, dist2D_2);
                if (cmp == 0) {
                    cmp = Double.compare(yDiff1, yDiff2);
                }
                return cmp;
            } else {
                // 使用确定性的伪随机排序
                int hash1 = deterministicHash(c1);
                int hash2 = deterministicHash(c2);
                return Integer.compare(hash1, hash2);
            }
        };

        LOGGER.debug("TimeMillis before generate: {}", System.currentTimeMillis());
        if (coordinate.getSafeMode() == ESafeMode.Y_DOWN) {
            IntStream.range((int) coordinate.getY(), NarcissusUtils.getWorldMinY(world))
                    .forEach(y -> coordinates.add(new Coordinate(coordinate.getX(), y, coordinate.getZ())));
        } else if (coordinate.getSafeMode() == ESafeMode.Y_UP) {
            IntStream.range((int) coordinate.getY(), NarcissusUtils.getWorldMaxY(world))
                    .forEach(y -> coordinates.add(new Coordinate(coordinate.getX(), y, coordinate.getZ())));
        } else if (coordinate.getSafeMode() == ESafeMode.Y_OFFSET_3) {
            IntStream.range((int) (coordinate.getY() - 3), (int) (coordinate.getY() + 3))
                    .forEach(y -> coordinates.add(new Coordinate(coordinate.getX(), y, coordinate.getZ())));
        } else {
            IntStream.range(chunkMinX, chunkMaxX)
                    .forEach(x -> IntStream.range(chunkMinZ, chunkMaxZ)
                            .forEach(z -> IntStream.range(NarcissusUtils.getWorldMinY(world), NarcissusUtils.getWorldMaxY(world))
                                    .forEach(y -> coordinates.add(new Coordinate(x, y, z)))
                            )
                    );
        }
        LOGGER.debug("TimeMillis before sorting: {}", System.currentTimeMillis());
        List<Coordinate> list = coordinates.stream().sorted(comparator).collect(Collectors.toList());
        LOGGER.debug("TimeMillis before searching: {}", System.currentTimeMillis());
        for (Coordinate c : list) {
            Coordinate candidate = new Coordinate().setX(c.getX()).setY(c.getY()).setZ(c.getZ())
                    .setYaw(coordinate.getYaw()).setPitch(coordinate.getPitch())
                    .setDimension(coordinate.getDimension())
                    .setSafe(coordinate.isSafe()).setSafeMode(coordinate.getSafeMode());
            if (belowAllowAir) {
                if (isAirCoordinate(world, candidate)) {
                    result = candidate.addX(0.5).addY(0.15).addZ(0.5);
                    break;
                }
            } else {
                if (isSafeCoordinate(world, candidate)) {
                    result = candidate.addX(0.5).addY(0.15).addZ(0.5);
                    break;
                }
            }
        }
        LOGGER.debug("TimeMillis after searching: {}", System.currentTimeMillis());
        LOGGER.debug("Target:{} | Safe:{}", coordinate.toXyzString(), result.toXyzString());
        return result;
    }

    private static boolean isAirCoordinate(World world, Coordinate coordinate) {
        IBlockState block = world.getBlockState(coordinate.toBlockPos());
        IBlockState blockAbove = world.getBlockState(coordinate.above().toBlockPos());
        IBlockState blockBelow = world.getBlockState(coordinate.below().toBlockPos());
        return isSafeBlock(world, coordinate, true, block, blockAbove, blockBelow);
    }

    private static boolean isSafeCoordinate(World world, Coordinate coordinate) {
        IBlockState block = world.getBlockState(coordinate.toBlockPos());
        IBlockState blockAbove = world.getBlockState(coordinate.toBlockPos().add(0, 1, 0));
        IBlockState blockBelow = world.getBlockState(coordinate.toBlockPos().add(0, -1, 0));
        return isSafeBlock(world, coordinate, false, block, blockAbove, blockBelow);
    }

    /**
     * 判断指定坐标是否安全
     *
     * @param block      方块
     * @param blockAbove 头部方块
     * @param blockBelow 脚下方块
     */
    private static boolean isSafeBlock(World world, Coordinate coordinate, boolean belowAllowAir, IBlockState block, IBlockState blockAbove, IBlockState blockBelow) {
        boolean isCurrentPassable = !block.getMaterial().blocksMovement()
                && !UNSAFE_BLOCKS.contains(block.getBlock());

        boolean isHeadSafe = !blockAbove.isFullCube()
                && !blockAbove.getMaterial().blocksMovement()
                && !UNSAFE_BLOCKS.contains(blockAbove.getBlock())
                && !SUFFOCATING_BLOCKS.contains(blockAbove.getBlock());

        boolean isBelowValid;
        if (blockBelow.getMaterial().isLiquid()) {
            isBelowValid = !UNSAFE_BLOCKS.contains(blockBelow.getBlock());
        } else {
            isBelowValid = blockBelow.getMaterial().isSolid()
                    && !UNSAFE_BLOCKS.contains(blockBelow.getBlock());
        }
        if (belowAllowAir) {
            isBelowValid = isBelowValid || blockBelow.getBlock() == Blocks.AIR;
        }

        return isCurrentPassable && isHeadSafe && isBelowValid;
    }

    // endregion 安全坐标

    // region 坐标查找

    /**
     * 获取指定维度的世界实例
     */
    public static WorldServer getWorld(DimensionType dimension) {
        // return DimensionManager.getWorld(NarcissusFarewell.getServerInstance(), dimension, true, true);
        return NarcissusFarewell.getServerInstance().getWorld(dimension.getId());
    }

    public static Biome getBiome(String id) {
        return getBiome(new ResourceLocation(id));
    }

    public static Biome getBiome(ResourceLocation id) {
        return ForgeRegistries.BIOMES.getValue(id);
    }

    /**
     * 获取指定范围内某个生物群系位置
     *
     * @param world       世界
     * @param start       开始位置
     * @param biome       目标生物群系
     * @param radius      搜索半径
     * @param minDistance 最小距离
     */
    public static Coordinate findNearestBiome(WorldServer world, Coordinate start, Biome biome, int radius, int minDistance) {
        BlockPos pos = world.getBiomeProvider().findBiomePosition((int) start.getX(), (int) start.getZ(), radius, new ArrayList<Biome>() {{
            add(biome);
        }}, world.rand);
        if (pos != null) {
            return start.clone().setX(pos.getX()).setZ(pos.getZ()).setSafe(true);
        }
        return null;
    }

    /**
     * 获取指定范围内某个生物群系位置
     *
     * @param world  世界
     * @param start  开始位置
     * @param struct 目标结构
     * @param radius 搜索半径
     */
    public static Coordinate findNearestStruct(WorldServer world, Coordinate start, String struct, int radius) {
        BlockPos pos = world.findNearestStructure(struct, start.toBlockPos(), true);
        if (pos != null) {
            return start.clone().setX(pos.getX()).setZ(pos.getZ()).setSafe(true);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getStructureList() {
        if (structureList.isEmpty()) {
            try {
                Field field = MapGenStructureIO.class.getDeclaredField(FieldUtils.getStartNameToClassMapFieldName());
                field.setAccessible(true);
                Map<String, Class<?>> map = (Map<String, Class<?>>) field.get(null);
                structureList = new ArrayList<>(map.keySet());
            } catch (Exception e) {
                LOGGER.error(e);
            }
        }
        return structureList;
    }

    public static String getHomeDimensionByName(EntityPlayerMP player, String name) {
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        List<KeyValue<String, String>> list = data.getHomeCoordinate().keySet().stream()
                .filter(key -> key.getValue().equals(name))
                .collect(Collectors.toList());
        if (list.size() == 1) {
            return list.get(0).getKey();
        }
        return null;
    }

    public static KeyValue<String, String> getPlayerHomeKey(EntityPlayerMP player, DimensionType dimension, String name) {
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        Map<String, String> defaultHome = data.getDefaultHome();
        if (defaultHome.isEmpty() && dimension == null && StringUtils.isNullOrEmpty(name) && data.getHomeCoordinate().size() != 1) {
            return null;
        }
        KeyValue<String, String> keyValue = null;
        if (dimension == null && StringUtils.isNotNullOrEmpty(name)) {
            if (defaultHome.isEmpty() || !defaultHome.containsValue(name)) {
                keyValue = data.getHomeCoordinate().keySet().stream()
                        .filter(key -> key.getValue().equals(name))
                        .filter(key -> key.getKey().equals(DimensionUtils.getStringIdFromInt(player.world.provider.getDimensionType().getId())))
                        .findFirst().orElse(null);
            } else if (defaultHome.containsValue(name)) {
                List<Map.Entry<String, String>> entryList = defaultHome.entrySet().stream().filter(entry -> entry.getValue().equals(name)).collect(Collectors.toList());
                if (entryList.size() == 1) {
                    keyValue = new KeyValue<>(entryList.get(0).getKey(), entryList.get(0).getValue());
                }
            }
        } else if (dimension != null && StringUtils.isNullOrEmpty(name)) {
            if (defaultHome.containsKey(DimensionUtils.getStringIdFromInt(dimension.getId()))) {
                keyValue = new KeyValue<>(DimensionUtils.getStringIdFromInt(dimension.getId()), defaultHome.get(DimensionUtils.getStringIdFromInt(dimension.getId())));
            }
        } else if (dimension != null && StringUtils.isNotNullOrEmpty(name)) {
            keyValue = data.getHomeCoordinate().keySet().stream()
                    .filter(key -> key.getValue().equals(name))
                    .filter(key -> key.getKey().equals(DimensionUtils.getStringIdFromInt(dimension.getId())))
                    .findFirst().orElse(null);
        } else if (!defaultHome.isEmpty() && dimension == null && StringUtils.isNullOrEmpty(name)) {
            if (defaultHome.size() == 1) {
                keyValue = new KeyValue<>(defaultHome.keySet().iterator().next(), defaultHome.values().iterator().next());
            } else {
                String value = defaultHome.getOrDefault(DimensionUtils.getStringIdFromInt(player.world.provider.getDimensionType().getId()), null);
                if (value != null) {
                    keyValue = new KeyValue<>(DimensionUtils.getStringIdFromInt(player.world.provider.getDimensionType().getId()), value);
                }
            }
        } else if (defaultHome.isEmpty() && dimension == null && StringUtils.isNullOrEmpty(name) && data.getHomeCoordinate().size() == 1) {
            keyValue = data.getHomeCoordinate().keySet().iterator().next();
        }
        return keyValue;
    }

    /**
     * 获取指定玩家的家坐标
     *
     * @param player    玩家
     * @param dimension 维度
     * @param name      名称
     */
    public static Coordinate getPlayerHome(EntityPlayerMP player, DimensionType dimension, String name) {
        return PlayerTeleportDataCapability.getData(player).getHomeCoordinate().getOrDefault(getPlayerHomeKey(player, dimension, name), null);
    }

    public static boolean isPlayerHome(EntityPlayerMP player, String name) {
        return PlayerTeleportDataCapability.getData(player).getHomeCoordinate().keySet().stream()
                .anyMatch(key -> key.getValue().equals(name));
    }

    public static String getStageDimensionByName(String name) {
        WorldStageData stageData = WorldStageData.get();
        List<KeyValue<String, String>> list = stageData.getStageCoordinate().keySet().stream()
                .filter(key -> key.getValue().equals(name))
                .collect(Collectors.toList());
        if (list.size() == 1) {
            return list.get(0).getKey();
        }
        return null;
    }

    /**
     * 获取距离玩家最近的驿站
     *
     * @param player 玩家
     * @return 驿站key
     */
    public static KeyValue<String, String> findNearestStageKey(EntityPlayerMP player) {
        WorldStageData stageData = WorldStageData.get();
        Map.Entry<KeyValue<String, String>, Coordinate> stageEntry = stageData.getStageCoordinate().entrySet().stream()
                .filter(entry -> entry.getKey().getKey().equals(DimensionUtils.getStringIdFromInt(player.world.provider.getDimensionType().getId())))
                .min(Comparator.comparingInt(entry -> {
                    Coordinate value = entry.getValue();
                    double dx = value.getX() - player.posX;
                    double dy = value.getY() - player.posY;
                    double dz = value.getZ() - player.posZ;
                    // 返回欧几里得距离的平方（避免开方操作，提高性能）
                    return (int) (dx * dx + dy * dy + dz * dz);
                })).orElse(null);
        return stageEntry != null ? stageEntry.getKey() : null;
    }

    /**
     * 获取玩家离开的坐标
     *
     * @param player    玩家
     * @param type      传送类型
     * @param dimension 维度
     * @return 查询到的离开坐标（如果未找到则返回 null）
     */
    public static TeleportRecord getBackTeleportRecord(EntityPlayerMP player, @Nullable ETeleportType type, @Nullable DimensionType dimension) {
        TeleportRecord result = null;
        // 获取玩家的传送数据
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        List<TeleportRecord> records = data.getTeleportRecords();
        Stream<TeleportRecord> stream = records.stream()
                .filter(record -> type == null || record.getTeleportType() == type);
        for (String s : ServerConfig.TELEPORT_BACK_SKIP_TYPE.split(",")) {
            ETeleportType value = ETeleportType.nullableValueOf(s);
            stream = stream
                    .filter(record -> type == value || record.getTeleportType() != value);
        }
        Optional<TeleportRecord> optionalRecord = stream
                .filter(record -> dimension == null || record.getBefore().getDimension().equals(dimension))
                .max(Comparator.comparing(TeleportRecord::getTeleportTime));
        if (optionalRecord.isPresent()) {
            result = optionalRecord.get();
        }
        return result;
    }

    public static void removeBackTeleportRecord(EntityPlayerMP player, TeleportRecord record) {
        PlayerTeleportDataCapability.getData(player).getTeleportRecords().remove(record);
    }

    // endregion 坐标查找

    // region 传送相关

    /**
     * 检查传送范围
     */
    public static int checkRange(EntityPlayerMP player, ETeleportType type, int range) {
        int maxRange;
        switch (type) {
            case TP_VIEW:
                maxRange = ServerConfig.TELEPORT_VIEW_DISTANCE_LIMIT;
                break;
            default:
                maxRange = ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT;
                break;
        }
        if (range > maxRange) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "range_too_large"), maxRange);
        } else if (range <= 0) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "range_too_small"), 1);
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
    public static void teleportTo(@NonNull EntityPlayerMP from, @NonNull EntityPlayerMP to, ETeleportType type, boolean safe) {
        if (ETeleportType.TP_HERE == type) {
            teleportTo(to, new Coordinate(from).setSafe(safe), type);
        } else {
            teleportTo(from, new Coordinate(to).setSafe(safe), type);
        }
    }

    /**
     * 传送玩家到指定坐标
     *
     * @param player 玩家
     * @param after  坐标
     */
    public static void teleportTo(@NonNull EntityPlayerMP player, @NonNull Coordinate after, ETeleportType type) {
        Coordinate before = new Coordinate(player);
        World world = player.world;
        MinecraftServer server = player.getServer();
        if (world != null && server != null) {
            WorldServer level = server.getWorld(after.getDimension().getId());
            if (level != null) {
                if (after.isSafe()) {
                    // 异步的代价就是粪吗
                    player.connection.sendPacket(new SPacketChat(Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "safe_searching").toTextComponent(), ChatType.GAME_INFO));
                    new Thread(() -> {
                        Coordinate finalAfter = after.clone();
                        finalAfter = findSafeCoordinate(finalAfter, false);
                        Runnable runnable;
                        // 判断是否需要在脚下放置方块
                        if (ServerConfig.SETBLOCK_WHEN_SAFE_NOT_FOUND && !isSafeCoordinate(level, finalAfter)) {
                            IBlockState blockState;
                            List<ItemStack> playerItemList = getPlayerItemList(player);
                            if (CollectionUtils.isNotNullOrEmpty(SAFE_BLOCKS)) {
                                if (ServerConfig.GETBLOCK_FROM_INVENTORY) {
                                    blockState = SAFE_BLOCKS.stream()
                                            .filter(block -> playerItemList.stream().map(ItemStack::getItem).anyMatch(item -> new ItemStack(block).getItem().equals(item)))
                                            .map(Block::getDefaultState)
                                            .findFirst().orElse(null);
                                } else {
                                    blockState = SAFE_BLOCKS.get(0).getDefaultState();
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
                                                getWorld(after.getDimension()).setBlockState(airCoordinate.toBlockPos().add(0, -1, 0), blockState.getBlock().getDefaultState());
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
                        server.addScheduledTask(() -> {
                            if (runnable != null) runnable.run();
                            teleportPlayer(player, finalAfter1, type, before, level);
                            DimensionManager.unloadWorld(finalAfter1.getDimension().getId());
                        });
                    }).start();
                } else {
                    teleportPlayer(player, after, type, before, level);
                }
            }
        }
    }

    private static void teleportPlayer(@NonNull EntityPlayerMP player, @NonNull Coordinate after, ETeleportType type, Coordinate before, WorldServer level) {
        ResourceLocation sound = new ResourceLocation(ServerConfig.TP_SOUND);
        NarcissusUtils.playSound(player, sound, 1.0f, 1.0f);
        after.setY(Math.floor(after.getY()) + 0.1);

        // 传送跟随者
        teleportFollowers(player, after, level);
        // 传送载体与乘客
        Entity vehicle = teleportPassengers(player, null, player.getLowestRidingEntity(), after, level);
        // 传送玩家
        doTeleport(player, after, level);
        // 使玩家重新坐上载体
        if (vehicle != null) {
            player.startRiding(vehicle, true);
            // 同步客户端状态
            broadcastPacket(new SPacketSetPassengers(vehicle));
        }
        NarcissusUtils.playSound(player, sound, 1.0f, 1.0f);
        TeleportRecord record = new TeleportRecord();
        record.setTeleportTime(new Date());
        record.setTeleportType(type);
        record.setBefore(before);
        record.setAfter(after);
        PlayerTeleportDataCapability.getData(player).addTeleportRecords(record);
    }

    public static List<EntityPlayerMP> getPlayer(EntityPlayerMP player, String name) {
        List<EntityPlayerMP> players = new ArrayList<>();
        try {
            if (StringUtils.isNotNullOrEmpty(name)) {
                if (name.startsWith("@")) {
                    switch (name.substring(1)) {
                        case "a":
                            players.addAll(NarcissusFarewell.getServerInstance().getPlayerList().getPlayers());
                            break;
                        case "r":
                            players.add(getRandomPlayer());
                            break;
                        case "p":
                            // 寻找距离玩家最近的其他玩家
                            if (player != null) {
                                players.add(NarcissusFarewell.getServerInstance().getPlayerList().getPlayers().stream()
                                        .filter(p -> !p.getUniqueID().equals(player.getUniqueID()))
                                        .min(Comparator.comparingInt(p -> {
                                            double dx = p.posX - player.posX;
                                            double dy = p.posY - player.posY;
                                            double dz = p.posZ - player.posZ;
                                            return (int) (dx * dx + dy * dy + dz * dz);
                                        })).orElse(null));
                            }
                            break;
                        case "s":
                            players.add(player);
                            break;
                    }
                } else {
                    players.add(NarcissusFarewell.getServerInstance().getPlayerList().getPlayerByUsername(name));
                }
            }
        } catch (Exception ignored) {
        }
        return players.stream().filter(Objects::nonNull).collect(Collectors.toList());
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
    private static @Nullable Entity teleportPassengers(EntityPlayerMP player, Entity parent, Entity passenger, @NonNull Coordinate coordinate, WorldServer level) {
        if (!ServerConfig.TP_WITH_VEHICLE || passenger == null) return null;

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

        passengers.forEach(Entity::dismountRidingEntity);

        // 传送载具
        if (parent == null) {
            passenger = doTeleport(passenger, coordinate, level);
        }
        // 传送所有乘客
        for (Entity entity : passengers) {
            if (entity == player) {
                playerVehicle = passenger;
            } else if (entity.getRidingEntity() == null) {
                int oldId = entity.getEntityId();
                entity = doTeleport(entity, coordinate, level);
                entity.startRiding(passenger, true);
                // 更新玩家乘坐的实体对象
                if (playerVehicle != null && oldId == playerVehicle.getEntityId()) {
                    playerVehicle = entity;
                }
            }
        }
        // 同步客户端状态
        broadcastPacket(new SPacketSetPassengers(passenger));
        return playerVehicle;
    }

    /**
     * 传送跟随的实体
     */
    private static void teleportFollowers(@NonNull EntityPlayerMP player, @NonNull Coordinate coordinate, WorldServer level) {
        if (!ServerConfig.TP_WITH_FOLLOWER) return;

        int followerRange = ServerConfig.TP_WITH_FOLLOWER_RANGE;

        // 传送主动跟随的实体
        for (EntityTameable entity : player.getServerWorld().getEntitiesWithinAABB(EntityTameable.class, player.getEntityBoundingBox().grow(followerRange))) {
            if (entity.getOwnerId() != null && entity.getOwnerId().equals(player.getUniqueID()) && !entity.isSitting()) {
                doTeleport(entity, coordinate, level);
            }
        }

        // 传送拴绳实体
        for (EntityLiving entity : player.getServerWorld().getEntitiesWithinAABB(EntityLiving.class, player.getEntityBoundingBox().grow(followerRange))) {
            if (entity.getLeashHolder() == player) {
                doTeleport(entity, coordinate, level);
            }
        }

        // 传送被吸引的非敌对实体
        for (EntityLiving entity : player.getServerWorld().getEntitiesWithinAABB(EntityLiving.class, player.getEntityBoundingBox().grow(followerRange))) {
            // 排除敌对生物（IMob 代表所有敌对生物）
            if (entity instanceof IMob) continue;
            // 检查实体是否被玩家吸引
            if (entity.getEntitySenses().canSee(player) &&
                    entity.tasks.taskEntries.stream()
                            .anyMatch(task -> task.using
                                    && (task.action instanceof EntityAITempt)
                                    && FieldUtils.getPrivateFieldValue(EntityAITempt.class, task.action, FieldUtils.getTemptGoalPlayerFieldName()) == player
                            )) {
                doTeleport(entity, coordinate, level);
            }
        }

    }

    private static Entity doTeleport(@NonNull Entity entity, @NonNull Coordinate coordinate, WorldServer level) {
        if (entity instanceof EntityPlayerMP) {
            EntityPlayerMP player = (EntityPlayerMP) entity;
            if (DimensionUtils.getDimensionType(player.dimension) == coordinate.getDimension()) {
                player.setPositionAndUpdate(coordinate.getX(), coordinate.getY(), coordinate.getZ());
                player.cameraYaw = coordinate.getYaw() == 0 ? player.cameraYaw : (float) coordinate.getYaw();
                player.cameraPitch = coordinate.getPitch() == 0 ? player.cameraPitch : (float) coordinate.getPitch();
            } else {
                player.getServer().getPlayerList().transferPlayerToDimension(player, coordinate.getDimension().getId(), new TeleporterCustom(level, coordinate));
            }
        } else {
            if (DimensionUtils.getDimensionType(entity.dimension) == coordinate.getDimension()) {
                entity.setPositionAndUpdate(coordinate.getX(), coordinate.getY(), coordinate.getZ());
            } else {
                entity = entity.changeDimension(coordinate.getDimension().getId(), new TeleporterCustom(level, coordinate));
            }
        }
        return entity;
    }

    // endregion 传送相关

    // region 玩家与玩家背包

    /**
     * 获取随机玩家
     */
    public static EntityPlayerMP getRandomPlayer() {
        try {
            List<EntityPlayerMP> players = NarcissusFarewell.getServerInstance().getPlayerList().getPlayers();
            return players.get(new Random().nextInt(players.size()));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取随机玩家UUID
     */
    public static UUID getRandomPlayerUUID() {
        EntityPlayer randomPlayer = getRandomPlayer();
        return randomPlayer != null ? randomPlayer.getUniqueID() : null;
    }

    /**
     * 通过UUID获取对应的玩家
     *
     * @param uuid 玩家UUID
     */
    public static EntityPlayerMP getPlayer(UUID uuid) {
        try {
            return Minecraft.getMinecraft().world.getMinecraftServer().getPlayerList().getPlayerByUUID(uuid);
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
    public static boolean removeItemFromPlayerInventory(EntityPlayerMP player, ItemStack itemToRemove) {
        IInventory inventory = player.inventory;

        // 剩余要移除的数量
        int remainingAmount = itemToRemove.getCount();
        // 记录成功移除的物品数量，以便失败时进行回滚
        int successfullyRemoved = 0;

        // 遍历玩家背包的所有插槽
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            // 获取背包中的物品
            ItemStack stack = inventory.getStackInSlot(i);
            ItemStack copy = itemToRemove.copy();
            copy.setCount(stack.getCount());

            // 如果插槽中的物品是目标物品
            if (ItemStack.areItemStacksEqual(stack, copy)) {
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
            player.inventory.addItemStackToInventory(copy);
        }

        // 是否成功移除所有物品
        return remainingAmount == 0;
    }

    public static List<ItemStack> getPlayerItemList(EntityPlayerMP player) {
        List<ItemStack> result = new ArrayList<>();
        if (player != null) {
            result.addAll(player.inventory.mainInventory);
            result.addAll(player.inventory.armorInventory);
            result.addAll(player.inventory.offHandInventory);
            result = result.stream().filter(itemStack -> !itemStack.isEmpty() && itemStack.getItem() != Items.AIR).collect(Collectors.toList());
        }
        return result;
    }

    public static NonNullList<ItemStack> getPlayerInventory(EntityPlayerMP player) {
        NonNullList<ItemStack> inventory = NonNullList.create();
        inventory.addAll(player.inventory.mainInventory);
        inventory.addAll(player.inventory.armorInventory);
        inventory.addAll(player.inventory.offHandInventory);
        return inventory;
    }

    // endregion 玩家与玩家背包

    // region 消息相关

    /**
     * 广播消息
     *
     * @param player  发送者
     * @param message 消息
     */
    public static void broadcastMessage(EntityPlayerMP player, Component message) {
        player.getServer().getPlayerList().sendMessage(new TextComponentTranslation("chat.type.announcement", player.getDisplayName().getFormattedText(), message.toChatComponent(NarcissusUtils.getPlayerLanguage(player))), true);
    }

    /**
     * 广播消息
     *
     * @param server  发送者
     * @param message 消息
     */
    public static void broadcastMessage(MinecraftServer server, Component message) {
        server.getPlayerList().sendMessage(new TextComponentTranslation("chat.type.announcement", "Server", message.toChatComponent()), true);
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(EntityPlayerMP player, Component message) {
        player.sendMessage(message.toChatComponent(NarcissusUtils.getPlayerLanguage(player)));
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(EntityPlayerMP player, String message) {
        player.sendMessage(Component.literal(message).toTextComponent());
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(ICommandSender player, String message) {
        player.sendMessage(Component.literal(message).toChatComponent());
    }

    /**
     * 发送翻译消息
     *
     * @param player 玩家
     * @param key    翻译键
     * @param args   参数
     */
    public static void sendTranslatableMessage(EntityPlayerMP player, String key, Object... args) {
        player.sendMessage(Component.translatable(key, args).setLanguageCode(NarcissusUtils.getPlayerLanguage(player)).toChatComponent());
    }

    /**
     * 发送翻译消息
     *
     * @param source  指令来源
     * @param success 是否成功
     * @param key     翻译键
     * @param args    参数
     */
    public static void sendTranslatableMessage(ICommandSender source, boolean success, String key, Object... args) {
        if (source instanceof EntityPlayerMP) {
            sendTranslatableMessage((EntityPlayerMP) source, key, args);
        } else {
            source.sendMessage(Component.translatable(key, args).setLanguageCode(ServerConfig.DEFAULT_LANGUAGE).toChatComponent());
        }
    }

    /**
     * 广播数据包至所有玩家
     *
     * @param packet 数据包
     */
    public static void broadcastPacket(Packet<?> packet) {
        NarcissusFarewell.getServerInstance().getPlayerList().getPlayers().forEach(player -> player.connection.sendPacket(packet));
    }

    // endregion 消息相关

    // region 跨维度传送

    public static boolean isTeleportAcrossDimensionEnabled(EntityPlayerMP player, DimensionType to, ETeleportType type) {
        boolean result = true;
        if (player.world.provider.getDimensionType() != to) {
            if (ServerConfig.TELEPORT_ACROSS_DIMENSION) {
                if (!NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, type)) {
                    result = false;
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "across_dimension_not_enable_for"), getCommand(type));
                }
            } else {
                result = false;
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "across_dimension_not_enable"));
            }
        }
        return result;
    }

    /**
     * 判断传送类型跨维度传送是否开启
     */
    public static boolean isTeleportTypeAcrossDimensionEnabled(EntityPlayerMP player, ETeleportType type) {
        int permission;
        switch (type) {
            case TP_COORDINATE:
                permission = ServerConfig.PERMISSION_TP_COORDINATE_ACROSS_DIMENSION;
                break;
            case TP_STRUCTURE:
                permission = ServerConfig.PERMISSION_TP_STRUCTURE_ACROSS_DIMENSION;
                break;
            case TP_ASK:
                permission = ServerConfig.PERMISSION_TP_ASK_ACROSS_DIMENSION;
                break;
            case TP_HERE:
                permission = ServerConfig.PERMISSION_TP_HERE_ACROSS_DIMENSION;
                break;
            case TP_RANDOM:
                permission = ServerConfig.PERMISSION_TP_RANDOM_ACROSS_DIMENSION;
                break;
            case TP_SPAWN:
                permission = ServerConfig.PERMISSION_TP_SPAWN_ACROSS_DIMENSION;
                break;
            case TP_WORLD_SPAWN:
                permission = ServerConfig.PERMISSION_TP_WORLD_SPAWN_ACROSS_DIMENSION;
                break;
            case TP_HOME:
                permission = ServerConfig.PERMISSION_TP_HOME_ACROSS_DIMENSION;
                break;
            case TP_STAGE:
                permission = ServerConfig.PERMISSION_TP_STAGE_ACROSS_DIMENSION;
                break;
            case TP_BACK:
                permission = ServerConfig.PERMISSION_TP_BACK_ACROSS_DIMENSION;
                break;
            default:
                permission = 0;
                break;
        }
        return permission > -1 && hasPermissions(player, permission);
    }

    // endregion 跨维度传送

    // region 传送冷却

    /**
     * 获取传送/传送请求冷却时间
     *
     * @param player 玩家
     * @param type   传送类型
     */
    public static int getTeleportCoolDown(EntityPlayerMP player, ETeleportType type) {
        // 如果传送卡类型为抵消冷却时间，则不计算冷却时间
        if (ServerConfig.TELEPORT_CARD_TYPE == ECardType.REFUND_COOLDOWN || ServerConfig.TELEPORT_CARD_TYPE == ECardType.REFUND_ALL_COST_AND_COOLDOWN) {
            if (PlayerTeleportDataCapability.getData(player).getTeleportCard() > 0) {
                return 0;
            }
        }
        Instant current = Instant.now();
        int commandCoolDown = getCommandCoolDown(type);
        Instant lastTpTime = PlayerTeleportDataCapability.getData(player).getTeleportRecords(type).stream()
                .map(TeleportRecord::getTeleportTime)
                .max(Comparator.comparing(Date::toInstant))
                .orElse(new Date(0)).toInstant();
        switch (ServerConfig.TELEPORT_REQUEST_COOLDOWN_TYPE) {
            case COMMON:
                return calculateCooldown(player.getUniqueID(), current, lastTpTime, ServerConfig.TELEPORT_REQUEST_COOLDOWN, null);
            case INDIVIDUAL:
                return calculateCooldown(player.getUniqueID(), current, lastTpTime, commandCoolDown, type);
            case MIXED:
                int globalCommandCoolDown = ServerConfig.TELEPORT_REQUEST_COOLDOWN;
                int individualCooldown = calculateCooldown(player.getUniqueID(), current, lastTpTime, commandCoolDown, type);
                int globalCooldown = calculateCooldown(player.getUniqueID(), current, lastTpTime, globalCommandCoolDown, null);
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
    public static int getCommandCoolDown(ETeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return ServerConfig.COOLDOWN_TP_COORDINATE;
            case TP_STRUCTURE:
                return ServerConfig.COOLDOWN_TP_STRUCTURE;
            case TP_ASK:
                return ServerConfig.COOLDOWN_TP_ASK;
            case TP_HERE:
                return ServerConfig.COOLDOWN_TP_HERE;
            case TP_RANDOM:
                return ServerConfig.COOLDOWN_TP_RANDOM;
            case TP_SPAWN:
                return ServerConfig.COOLDOWN_TP_SPAWN;
            case TP_WORLD_SPAWN:
                return ServerConfig.COOLDOWN_TP_WORLD_SPAWN;
            case TP_TOP:
                return ServerConfig.COOLDOWN_TP_TOP;
            case TP_BOTTOM:
                return ServerConfig.COOLDOWN_TP_BOTTOM;
            case TP_UP:
                return ServerConfig.COOLDOWN_TP_UP;
            case TP_DOWN:
                return ServerConfig.COOLDOWN_TP_DOWN;
            case TP_VIEW:
                return ServerConfig.COOLDOWN_TP_VIEW;
            case TP_HOME:
                return ServerConfig.COOLDOWN_TP_HOME;
            case TP_STAGE:
                return ServerConfig.COOLDOWN_TP_STAGE;
            case TP_BACK:
                return ServerConfig.COOLDOWN_TP_BACK;
            default:
                return 0;
        }
    }

    private static int calculateCooldown(UUID uuid, Instant current, Instant lastTpTime, int cooldown, ETeleportType type) {
        Optional<TeleportRequest> latestRequest = NarcissusFarewell.getTeleportRequest().values().stream()
                .filter(request -> request.getRequester().getUniqueID().equals(uuid))
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
    public static boolean validTeleportCost(EntityPlayerMP player, Coordinate target, ETeleportType type, boolean submit) {
        return validateCost(player, target.getDimension(), calculateDistance(new Coordinate(player), target), type, submit);
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
        return validateCost(request.getRequester(), request.getTarget().world.provider.getDimensionType(), calculateDistance(requesterCoordinate, targetCoordinate), request.getTeleportType(), submit);
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
    private static boolean validateCost(EntityPlayerMP player, DimensionType targetDim, double distance, ETeleportType teleportType, boolean submit) {
        TeleportCost teleportCost = NarcissusUtils.getCommandCost(teleportType);
        if (teleportCost.getType() == ECostType.NONE) return true;
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);

        double adjustedDistance;
        if (player.world.provider.getDimensionType() == targetDim) {
            int limit = ServerConfig.TELEPORT_COST_DISTANCE_LIMIT;
            adjustedDistance = limit == 0 ? distance : Math.min(limit, distance);
        } else {
            adjustedDistance = ServerConfig.TELEPORT_COST_DISTANCE_ACROSS_DIMENSION;
        }

        double need = teleportCost.getNum() * adjustedDistance * teleportCost.getRate();
        int cardNeed = getTeleportCardNeed(need);
        int costNeed = getTeleportCostNeed(data, cardNeed, (int) Math.ceil(need));
        boolean result = false;

        if (costNeed < 0) {
            NarcissusUtils.sendTranslatableMessage(player
                    , I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough")
                    , Component.translatable(NarcissusUtils.getPlayerLanguage(player)
                            , EI18nType.WORD, "teleport_card")
                    , cardNeed
            );
        }

        switch (teleportCost.getType()) {
            case EXP_POINT:
                result = player.experienceTotal >= costNeed;
                if (!result) {
                    NarcissusUtils.sendTranslatableMessage(player
                            , I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough")
                            , Component.translatable(NarcissusUtils.getPlayerLanguage(player)
                                    , EI18nType.WORD, "exp_point")
                            , costNeed
                    );
                } else if (submit) {
                    player.addExperience(-costNeed);
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case EXP_LEVEL:
                result = player.experienceLevel >= costNeed;
                if (!result) {
                    NarcissusUtils.sendTranslatableMessage(player
                            , I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough")
                            , Component.translatable(NarcissusUtils.getPlayerLanguage(player)
                                    , EI18nType.WORD, "exp_level")
                            , costNeed
                    );
                } else if (submit) {
                    player.addExperienceLevel(-costNeed);
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case HEALTH:
                result = player.getHealth() > costNeed;
                if (!result) {
                    NarcissusUtils.sendTranslatableMessage(player
                            , I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough")
                            , Component.translatable(NarcissusUtils.getPlayerLanguage(player)
                                    , EI18nType.WORD, "health")
                            , costNeed
                    );
                } else if (submit) {
                    try {
                        DataParameter<? super Float> DATA_HEALTH_ID = (DataParameter<? super Float>) FieldUtils.getPrivateFieldValue(EntityLivingBase.class, null, FieldUtils.getEntityHealthFieldName());
                        Float health = (Float) player.getDataManager().get(DATA_HEALTH_ID);
                        player.getDataManager().set(DATA_HEALTH_ID, health - costNeed);
                    } catch (Exception e) {
                        player.attackEntityFrom(DamageSource.MAGIC, costNeed);
                    }
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case HUNGER:
                result = player.getFoodStats().getFoodLevel() >= costNeed;
                if (!result) {
                    NarcissusUtils.sendTranslatableMessage(player
                            , I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough")
                            , Component.translatable(NarcissusUtils.getPlayerLanguage(player)
                                    , EI18nType.WORD, "hunger")
                            , costNeed
                    );
                } else if (submit) {
                    player.getFoodStats().setFoodLevel(player.getFoodStats().getFoodLevel() - costNeed);
                    data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                }
                break;
            case ITEM:
                try {
                    String itemId;
                    String nbt;
                    if (teleportCost.getConf().contains("{") && teleportCost.getConf().contains("}")) {
                        itemId = teleportCost.getConf().substring(0, teleportCost.getConf().indexOf("{"));
                        nbt = teleportCost.getConf().substring(teleportCost.getConf().indexOf("{"));
                    } else {
                        itemId = teleportCost.getConf();
                        nbt = null;
                    }
                    ItemStack itemStack = new ItemStack(CommandBase.getItemByText(player, itemId));
                    if (StringUtils.isNotNullOrEmpty(nbt)) {
                        try {
                            itemStack.setTagCompound(JsonToNBT.getTagFromJson(nbt));
                        } catch (NBTException ignored) {
                        }
                    }
                    result = getItemCount(getPlayerInventory(player), itemStack) >= costNeed;
                    if (!result) {
                        NarcissusUtils.sendTranslatableMessage(player
                                , I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough")
                                , NarcissusUtils.getItemName(itemStack)
                                , costNeed
                        );
                    } else if (submit) {
                        itemStack.setCount(costNeed);
                        result = removeItemFromPlayerInventory(player, itemStack);
                        // 代价不足
                        if (result) {
                            data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                        } else {
                            NarcissusUtils.sendTranslatableMessage(player
                                    , I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough")
                                    , NarcissusUtils.getItemName(itemStack)
                                    , costNeed
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
                        int commandResult = player.getServer().getCommandManager().executeCommand(player, command);
                        if (commandResult > 0) {
                            data.subTeleportCard(Math.min(data.getTeleportCard(), cardNeed));
                        }
                        result = commandResult > 0;
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
        if (!ServerConfig.TELEPORT_CARD) return 0;
        switch (ServerConfig.TELEPORT_CARD_TYPE) {
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
    public static int getTeleportCostNeed(IPlayerTeleportData data, int card, int need) {
        if (!ServerConfig.TELEPORT_CARD) return 0;
        switch (ServerConfig.TELEPORT_CARD_TYPE) {
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

    public static TeleportCost getCommandCost(ETeleportType type) {
        TeleportCost cost = new TeleportCost();
        switch (type) {
            case TP_COORDINATE:
                cost.setType(ServerConfig.COST_TP_COORDINATE_TYPE);
                cost.setNum(ServerConfig.COST_TP_COORDINATE_NUM);
                cost.setRate(ServerConfig.COST_TP_COORDINATE_RATE);
                cost.setConf(ServerConfig.COST_TP_COORDINATE_CONF);
                break;
            case TP_STRUCTURE:
                cost.setType(ServerConfig.COST_TP_STRUCTURE_TYPE);
                cost.setNum(ServerConfig.COST_TP_STRUCTURE_NUM);
                cost.setRate(ServerConfig.COST_TP_STRUCTURE_RATE);
                cost.setConf(ServerConfig.COST_TP_STRUCTURE_CONF);
                break;
            case TP_ASK:
                cost.setType(ServerConfig.COST_TP_ASK_TYPE);
                cost.setNum(ServerConfig.COST_TP_ASK_NUM);
                cost.setRate(ServerConfig.COST_TP_ASK_RATE);
                cost.setConf(ServerConfig.COST_TP_ASK_CONF);
                break;
            case TP_HERE:
                cost.setType(ServerConfig.COST_TP_HERE_TYPE);
                cost.setNum(ServerConfig.COST_TP_HERE_NUM);
                cost.setRate(ServerConfig.COST_TP_HERE_RATE);
                cost.setConf(ServerConfig.COST_TP_HERE_CONF);
                break;
            case TP_RANDOM:
                cost.setType(ServerConfig.COST_TP_RANDOM_TYPE);
                cost.setNum(ServerConfig.COST_TP_RANDOM_NUM);
                cost.setRate(ServerConfig.COST_TP_RANDOM_RATE);
                cost.setConf(ServerConfig.COST_TP_RANDOM_CONF);
                break;
            case TP_SPAWN:
                cost.setType(ServerConfig.COST_TP_SPAWN_TYPE);
                cost.setNum(ServerConfig.COST_TP_SPAWN_NUM);
                cost.setRate(ServerConfig.COST_TP_SPAWN_RATE);
                cost.setConf(ServerConfig.COST_TP_SPAWN_CONF);
                break;
            case TP_WORLD_SPAWN:
                cost.setType(ServerConfig.COST_TP_WORLD_SPAWN_TYPE);
                cost.setNum(ServerConfig.COST_TP_WORLD_SPAWN_NUM);
                cost.setRate(ServerConfig.COST_TP_WORLD_SPAWN_RATE);
                cost.setConf(ServerConfig.COST_TP_WORLD_SPAWN_CONF);
                break;
            case TP_TOP:
                cost.setType(ServerConfig.COST_TP_TOP_TYPE);
                cost.setNum(ServerConfig.COST_TP_TOP_NUM);
                cost.setRate(ServerConfig.COST_TP_TOP_RATE);
                cost.setConf(ServerConfig.COST_TP_TOP_CONF);
                break;
            case TP_BOTTOM:
                cost.setType(ServerConfig.COST_TP_BOTTOM_TYPE);
                cost.setNum(ServerConfig.COST_TP_BOTTOM_NUM);
                cost.setRate(ServerConfig.COST_TP_BOTTOM_RATE);
                cost.setConf(ServerConfig.COST_TP_BOTTOM_CONF);
                break;
            case TP_UP:
                cost.setType(ServerConfig.COST_TP_UP_TYPE);
                cost.setNum(ServerConfig.COST_TP_UP_NUM);
                cost.setRate(ServerConfig.COST_TP_UP_RATE);
                cost.setConf(ServerConfig.COST_TP_UP_CONF);
                break;
            case TP_DOWN:
                cost.setType(ServerConfig.COST_TP_DOWN_TYPE);
                cost.setNum(ServerConfig.COST_TP_DOWN_NUM);
                cost.setRate(ServerConfig.COST_TP_DOWN_RATE);
                cost.setConf(ServerConfig.COST_TP_DOWN_CONF);
                break;
            case TP_VIEW:
                cost.setType(ServerConfig.COST_TP_VIEW_TYPE);
                cost.setNum(ServerConfig.COST_TP_VIEW_NUM);
                cost.setRate(ServerConfig.COST_TP_VIEW_RATE);
                cost.setConf(ServerConfig.COST_TP_VIEW_CONF);
                break;
            case TP_HOME:
                cost.setType(ServerConfig.COST_TP_HOME_TYPE);
                cost.setNum(ServerConfig.COST_TP_HOME_NUM);
                cost.setRate(ServerConfig.COST_TP_HOME_RATE);
                cost.setConf(ServerConfig.COST_TP_HOME_CONF);
                break;
            case TP_STAGE:
                cost.setType(ServerConfig.COST_TP_STAGE_TYPE);
                cost.setNum(ServerConfig.COST_TP_STAGE_NUM);
                cost.setRate(ServerConfig.COST_TP_STAGE_RATE);
                cost.setConf(ServerConfig.COST_TP_STAGE_CONF);
                break;
            case TP_BACK:
                cost.setType(ServerConfig.COST_TP_BACK_TYPE);
                cost.setNum(ServerConfig.COST_TP_BACK_NUM);
                cost.setRate(ServerConfig.COST_TP_BACK_RATE);
                cost.setConf(ServerConfig.COST_TP_BACK_CONF);
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
            return ItemStack.areItemStacksEqual(item, copy);
        }).mapToInt(ItemStack::getCount).sum();
    }

    public static double calculateDistance(Coordinate coordinate1, Coordinate coordinate2) {
        return coordinate1.distanceFrom(coordinate2);
    }

    // endregion 传送代价

    // region 杂项
    public static String getPlayerLanguage(EntityPlayerMP player) {
        try {
            return PlayerTeleportDataCapability.getData(player).getValidLanguage(player);
        } catch (IllegalArgumentException i) {
            return ServerConfig.DEFAULT_LANGUAGE;
        }
    }

    public static String getValidLanguage(@Nullable EntityPlayer player, @Nullable String language) {
        String result;
        if (StringUtils.isNullOrEmptyEx(language) || "client".equalsIgnoreCase(language)) {
            if (player instanceof EntityPlayerMP) {
                result = NarcissusUtils.getServerPlayerLanguage((EntityPlayerMP) player);
            } else {
                result = NarcissusUtils.getClientLanguage();
            }
        } else if ("server".equalsIgnoreCase(language)) {
            result = ServerConfig.DEFAULT_LANGUAGE;
        } else {
            result = language;
        }
        return result;
    }

    public static String getServerPlayerLanguage(EntityPlayerMP player) {
        Object value = FieldUtils.getPrivateFieldValue(EntityPlayerMP.class, player, FieldUtils.getPlayerLanguageFieldName(player));
        if (value == null) {
            return "en_us";
        }
        return (String) value;
    }

    /**
     * 复制玩家语言设置
     *
     * @param originalPlayer 原始玩家
     * @param targetPlayer   目标玩家
     */
    public static void clonePlayerLanguage(EntityPlayerMP originalPlayer, EntityPlayerMP targetPlayer) {
        FieldUtils.setPrivateFieldValue(EntityPlayerMP.class, targetPlayer, FieldUtils.getPlayerLanguageFieldName(originalPlayer), getServerPlayerLanguage(originalPlayer));
    }

    public static final DamageSource DAMAGE_SOURCE = new DamageSource("narcissus");

    public static String getClientLanguage() {
        return Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();
    }

    /**
     * 强行使玩家死亡
     */
    @SuppressWarnings("unchecked")
    public static boolean killPlayer(EntityPlayerMP player) {
        try {
            DataParameter<? super Float> value = (DataParameter<? super Float>) FieldUtils.getPrivateFieldValue(EntityLivingBase.class, null, FieldUtils.getEntityHealthFieldName());
            if (value != null) {
                player.getDataManager().set(value, 0f);
                if (!player.world.getGameRules().getBoolean("keepInventory") && !player.isSpectator()) {
                    player.captureDrops = true;
                    player.capturedDrops.clear();
                    player.inventory.dropAllItems();
                    player.captureDrops = false;
                    PlayerDropsEvent event = new PlayerDropsEvent(player, DAMAGE_SOURCE, player.capturedDrops, false);
                    if (!MinecraftForge.EVENT_BUS.post(event)) {
                        for (net.minecraft.entity.item.EntityItem item : player.capturedDrops) {
                            player.world.spawnEntity(item);
                        }
                    }
                }
                player.addStat(StatList.DEATHS);
                player.takeStat(StatList.TIME_SINCE_DEATH);
                player.extinguish();
                player.getCombatTracker().reset();
            } else {
                player.onDeath(new DamageSource("narcissus"));
            }
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    /**
     * 播放音效
     *
     * @param player 玩家
     * @param sound  音效
     * @param volume 音量
     * @param pitch  音调
     */
    public static void playSound(EntityPlayerMP player, ResourceLocation sound, float volume, float pitch) {
        SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(sound);
        if (soundEvent != null) {
            player.connection.sendPacket(new SPacketSoundEffect(soundEvent, SoundCategory.PLAYERS,
                    player.posX, player.posY, player.posZ, volume, pitch));
        }
    }

    /**
     * 获取方块注册ID
     */
    @NonNull
    public static String getBlockRegistryName(Block block) {
        ResourceLocation location = block.getRegistryName();
        return location == null ? "" : location.toString();
    }

    public static String getItemName(ItemStack itemStack) {
        return itemStack.getDisplayName();
    }

    public static String getItemName(Item item) {
        return getItemName(new ItemStack(item));
    }

    // endregion 杂项
}
