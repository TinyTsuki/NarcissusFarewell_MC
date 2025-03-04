package xin.vanilla.narcissus.util;

import lombok.NonNull;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatList;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Vec3;
import net.minecraft.world.ChunkPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.WorldChunkManager;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.*;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.IPlayerTeleportData;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.*;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
            case DIMENSION:
                return prefix + ServerConfig.COMMAND_DIMENSION;
            case UUID:
                return prefix + ServerConfig.COMMAND_UUID;
            case UUID_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_UUID : "";
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
                // case TP_ASK_YES:
                // case TP_ASK_NO:
            case TP_ASK_CONCISE:
                // case TP_ASK_YES_CONCISE:
                // case TP_ASK_NO_CONCISE:
                return ServerConfig.PERMISSION_TP_ASK;
            case TP_HERE:
                // case TP_HERE_YES:
                // case TP_HERE_NO:
            case TP_HERE_CONCISE:
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
            case UUID:
            case UUID_CONCISE:
                return ServerConfig.CONCISE_UUID;
            case DIMENSION:
            case DIMENSION_CONCISE:
                return ServerConfig.CONCISE_DIMENSION;
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
            case TP_HERE:
            case TP_HERE_CONCISE:
                return ServerConfig.CONCISE_TP_HERE;
            case TP_HERE_YES:
            case TP_HERE_YES_CONCISE:
                return ServerConfig.CONCISE_TP_HERE_YES;
            case TP_HERE_NO:
            case TP_HERE_NO_CONCISE:
                return ServerConfig.CONCISE_TP_HERE_NO;
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
        return level <= 0 || player.canCommandSenderUseCommand(level, "");
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

    public static int getWorldMinY(WorldServer world) {
        return 0;
    }

    public static int getWorldMaxY(WorldServer world) {
        return world.getHeight();
    }

    public static int ceil(double value) {
        if (value > 0) {
            return (int) Math.ceil(value);
        } else {
            return (int) Math.floor(value);
        }
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
        Vec3 startPosition = start.toVector3d().addVector(0, player.getEyeHeight(), 0);

        // 获取玩家的视线方向
        Vec3 direction = player.getLook(1.0F).normalize();
        // 步长
        Vec3 stepVector = Vec3.createVectorHelper(direction.xCoord * stepScale, direction.yCoord * stepScale, direction.zCoord * stepScale);

        // 初始化变量
        Vec3 currentPosition = startPosition;
        World world = player.getEntityWorld();

        // 从近到远寻找碰撞点
        for (int stepCount = 0; stepCount <= range; stepCount++) {
            // 更新当前检测位置
            currentPosition = startPosition.addVector(stepVector.xCoord * stepCount, stepVector.yCoord * stepCount, stepVector.zCoord * stepCount);

            // 获取当前方块状态
            Block blockState = world.getBlock(ceil(currentPosition.xCoord), (int) currentPosition.yCoord, ceil(currentPosition.zCoord));

            // 检测方块是否不可穿过
            if (blockState.getMaterial().blocksMovement()) {
                result = start.clone().fromVector3(startPosition.addVector(stepVector.xCoord * (stepCount - 1), stepVector.yCoord * (stepCount - 1), stepVector.zCoord * (stepCount - 1)));
                break;
            }
        }

        // 若未找到碰撞点，则使用射线的终点
        if (result == null) {
            result = start.clone().fromVector3(currentPosition);
        }

        // 若需寻找安全坐标，则从碰撞点反向查找安全位置
        if (safe) {
            // 碰撞点的三维向量
            Vec3 collisionVector = result.toVector3d();
            for (int stepCount = (int) Math.ceil(collisionVector.distanceTo(startPosition) / stepScale); stepCount >= 0; stepCount--) {
                currentPosition = startPosition.addVector(stepVector.xCoord * stepCount, stepVector.yCoord * stepCount, stepVector.zCoord * stepCount);
                for (int yOffset = -3; yOffset < 3; yOffset++) {
                    Coordinate candidate = start.clone().fromVector3(currentPosition).addY(yOffset);
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
        World world = DimensionManager.getWorld(DimensionUtils.getDimensionType(coordinate.getDimension()));
        // DimensionManager.keepDimensionLoaded(coordinate.getDimension(), true);

        int chunkX = (int) coordinate.getX() >> 4;
        int chunkZ = (int) coordinate.getZ() >> 4;

        return searchForSafeCoordinateInChunk(world, coordinate, chunkX, chunkZ, belowAllowAir);
    }

    private static Coordinate searchForSafeCoordinateInChunk(World world, Coordinate coordinate, int chunkX, int chunkZ, boolean belowAllowAir) {
        // 搜索安全位置，限制在目标范围区块内
        int offset = (ServerConfig.SAFE_CHUNK_RANGE - 1) * 16;
        int chunkMinX = (chunkX << 4) - offset;
        int chunkMinZ = (chunkZ << 4) - offset;
        int chunkMaxX = chunkMinX + 15 + offset;
        int chunkMaxZ = chunkMinZ + 15 + offset;

        List<Integer> yList;
        List<Integer> xList;
        List<Integer> zList;
        if (coordinate.getSafeMode() == ESafeMode.Y_DOWN) {
            xList = new ArrayList<Integer>() {{
                add((int) coordinate.getX());
            }};
            zList = new ArrayList<Integer>() {{
                add((int) coordinate.getZ());
            }};
            yList = IntStream.range((int) coordinate.getY(), 0).boxed()
                    .sorted(Comparator.comparingInt(a -> (int) coordinate.getY() - a))
                    .collect(Collectors.toList());
        } else if (coordinate.getSafeMode() == ESafeMode.Y_UP) {
            xList = new ArrayList<Integer>() {{
                add((int) coordinate.getX());
            }};
            zList = new ArrayList<Integer>() {{
                add((int) coordinate.getZ());
            }};
            yList = IntStream.range((int) coordinate.getY(), world.getHeight()).boxed()
                    .sorted(Comparator.comparingInt(a -> a - (int) coordinate.getY()))
                    .collect(Collectors.toList());
        } else if (coordinate.getSafeMode() == ESafeMode.Y_OFFSET_3) {
            xList = new ArrayList<Integer>() {{
                add((int) coordinate.getX());
            }};
            zList = new ArrayList<Integer>() {{
                add((int) coordinate.getZ());
            }};
            yList = IntStream.range((int) (coordinate.getY() - 3), (int) (coordinate.getY() + 3)).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getY())))
                    .collect(Collectors.toList());
        } else {
            xList = IntStream.range(chunkMinX, chunkMaxX).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getX())))
                    .collect(Collectors.toList());
            zList = IntStream.range(chunkMinZ, chunkMaxZ).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getZ())))
                    .collect(Collectors.toList());
            yList = IntStream.range(0, world.getHeight()).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getY())))
                    .collect(Collectors.toList());
        }
        for (int y : yList) {
            if (coordinate.getSafeMode() == ESafeMode.NONE && y <= 0 || (y <= 0 || y > world.getHeight())) continue;
            for (int x : xList) {
                for (int z : zList) {
                    double offsetX = x > 0 ? x + 0.5 : x - 0.5;
                    double offsetZ = z > 0 ? z + 0.5 : z - 0.5;
                    Coordinate candidate = new Coordinate().setX(offsetX).setY(y + 0.15).setZ(offsetZ)
                            .setYaw(coordinate.getYaw()).setPitch(coordinate.getPitch())
                            .setDimension(coordinate.getDimension())
                            .setSafe(coordinate.isSafe()).setSafeMode(coordinate.getSafeMode());
                    if (belowAllowAir) {
                        if (isAirCoordinate(world, candidate)) {
                            return candidate;
                        }
                    } else {
                        if (isSafeCoordinate(world, candidate)) {
                            return candidate;
                        }
                    }
                }
            }
        }
        return coordinate;
    }

    private static boolean isAirCoordinate(World world, Coordinate coordinate) {
        Block block = world.getBlock(ceil(coordinate.getX()), (int) coordinate.getY(), ceil(coordinate.getZ()));
        Block blockAbove = world.getBlock(ceil(coordinate.getX()), (int) coordinate.getY() + 1, ceil(coordinate.getZ()));
        Block blockBelow = world.getBlock(ceil(coordinate.getX()), (int) coordinate.getY() - 1, ceil(coordinate.getZ()));
        return (!block.getMaterial().blocksMovement() && !UNSAFE_BLOCKS.contains(block))
                && (!blockAbove.getMaterial().blocksMovement() && !UNSAFE_BLOCKS.contains(blockAbove) && !SUFFOCATING_BLOCKS.contains(blockAbove))
                && (blockBelow == Blocks.air);
    }

    private static boolean isSafeCoordinate(World world, Coordinate coordinate) {
        Block block = world.getBlock(ceil(coordinate.getX()), (int) coordinate.getY(), ceil(coordinate.getZ()));
        Block blockAbove = world.getBlock(ceil(coordinate.getX()), (int) coordinate.getY() + 1, ceil(coordinate.getZ()));
        Block blockBelow = world.getBlock(ceil(coordinate.getX()), (int) coordinate.getY() - 1, ceil(coordinate.getZ()));
        return isSafeBlock(block, blockAbove, blockBelow);
    }

    private static boolean isSafeBlock(Block block, Block blockAbove, Block blockBelow) {
        return (!block.getMaterial().blocksMovement() && !UNSAFE_BLOCKS.contains(block))
                && (!blockAbove.getMaterial().blocksMovement() && !UNSAFE_BLOCKS.contains(blockAbove) && !SUFFOCATING_BLOCKS.contains(blockAbove))
                && blockBelow.getMaterial().isSolid()
                && !UNSAFE_BLOCKS.contains(blockBelow);
    }

    // endregion 安全坐标

    // region 坐标查找

    /**
     * 获取指定维度的世界实例
     */
    public static WorldServer getWorld(int dimensionId) {
        // return DimensionManager.getWorld(NarcissusFarewell.getServerInstance(), dimension, true, true);
        return NarcissusFarewell.getServerInstance().worldServerForDimension(dimensionId);
    }

    public static BiomeGenBase getBiome(String id) {
        return BiomeUtils.getBiome(id);
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
    public static Coordinate findNearestBiome(WorldServer world, Coordinate start, BiomeGenBase biome, int radius, int minDistance) {
        WorldChunkManager chunkManager = world.getWorldChunkManager();
        ChunkPosition pos = chunkManager.findBiomePosition(
                (int) start.getX(), (int) start.getZ(),
                radius,
                Collections.singletonList(biome),
                world.rand
        );

        if (pos != null) {
            return start.clone()
                    .setX(pos.chunkPosX)
                    .setZ(pos.chunkPosZ)
                    .setSafe(true);
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
        ChunkPosition pos = world.findClosestStructure(struct, (int) start.getX(), (int) start.getY(), (int) start.getZ());
        if (pos != null) {
            return start.clone().setX(pos.chunkPosX).setZ(pos.chunkPosZ).setSafe(true);
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

    public static KeyValue<String, String> getPlayerHomeKey(EntityPlayerMP player, String dimension, String name) {
        IPlayerTeleportData data = PlayerTeleportData.get(player);
        Map<String, String> defaultHome = data.getDefaultHome();
        if (defaultHome.isEmpty() && StringUtils.isNullOrEmptyEx(dimension) && StringUtils.isNullOrEmpty(name) && data.getHomeCoordinate().size() != 1) {
            return null;
        }
        KeyValue<String, String> keyValue = null;
        if (StringUtils.isNullOrEmptyEx(dimension) && StringUtils.isNotNullOrEmpty(name)) {
            if (defaultHome.isEmpty() || !defaultHome.containsValue(name)) {
                keyValue = data.getHomeCoordinate().keySet().stream()
                        .filter(key -> key.getValue().equals(name))
                        .filter(key -> key.getKey().equals(DimensionUtils.getStringId(player.getEntityWorld().provider)))
                        .findFirst().orElse(null);
            } else if (defaultHome.containsValue(name)) {
                List<Map.Entry<String, String>> entryList = defaultHome.entrySet().stream().filter(entry -> entry.getValue().equals(name)).collect(Collectors.toList());
                if (entryList.size() == 1) {
                    keyValue = new KeyValue<>(entryList.get(0).getKey(), entryList.get(0).getValue());
                }
            }
        } else if (StringUtils.isNotNullOrEmpty(dimension) && StringUtils.isNullOrEmpty(name)) {
            if (defaultHome.containsKey(DimensionUtils.getStringId(dimension))) {
                keyValue = new KeyValue<>(DimensionUtils.getStringId(dimension), defaultHome.get(DimensionUtils.getStringId(dimension)));
            }
        } else if (StringUtils.isNotNullOrEmpty(dimension) && StringUtils.isNotNullOrEmpty(name)) {
            keyValue = data.getHomeCoordinate().keySet().stream()
                    .filter(key -> key.getValue().equals(name))
                    .filter(key -> key.getKey().equals(DimensionUtils.getStringId(dimension)))
                    .findFirst().orElse(null);
        } else if (!defaultHome.isEmpty() && StringUtils.isNullOrEmptyEx(dimension) && StringUtils.isNullOrEmpty(name)) {
            if (defaultHome.size() == 1) {
                keyValue = new KeyValue<>(defaultHome.keySet().iterator().next(), defaultHome.values().iterator().next());
            } else {
                String value = defaultHome.getOrDefault(DimensionUtils.getStringId(player.getEntityWorld().provider), null);
                if (value != null) {
                    keyValue = new KeyValue<>(DimensionUtils.getStringId(player.getEntityWorld().provider), value);
                }
            }
        } else if (defaultHome.isEmpty() && StringUtils.isNullOrEmptyEx(dimension) && StringUtils.isNullOrEmpty(name) && data.getHomeCoordinate().size() == 1) {
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
    public static Coordinate getPlayerHome(EntityPlayerMP player, String dimension, String name) {
        return PlayerTeleportData.get(player).getHomeCoordinate().getOrDefault(getPlayerHomeKey(player, dimension, name), null);
    }

    public static boolean isPlayerHome(EntityPlayerMP player, String name) {
        return PlayerTeleportData.get(player).getHomeCoordinate().keySet().stream()
                .anyMatch(key -> key.getValue().equals(name));
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
                .filter(entry -> entry.getKey().getKey().equals(DimensionUtils.getStringId(player.getEntityWorld().provider)))
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
     * 获取并移除玩家离开的坐标
     *
     * @param player    玩家
     * @param type      传送类型
     * @param dimension 维度
     * @return 查询到的离开坐标（如果未找到则返回 null）
     */
    public static TeleportRecord getBackTeleportRecord(EntityPlayerMP player, @Nullable ETeleportType type, @Nullable String dimension) {
        TeleportRecord result = null;
        // 获取玩家的传送数据
        IPlayerTeleportData data = PlayerTeleportData.get(player);
        List<TeleportRecord> records = data.getTeleportRecords();
        Optional<TeleportRecord> optionalRecord = records.stream()
                .filter(record -> type == null || record.getTeleportType() == type)
                .filter(record -> type == ETeleportType.TP_BACK || record.getTeleportType() != ETeleportType.TP_BACK)
                .filter(record -> StringUtils.isNullOrEmptyEx(dimension) || record.getBefore().getDimension().equals(dimension))
                .max(Comparator.comparing(TeleportRecord::getTeleportTime));
        if (optionalRecord.isPresent()) {
            result = optionalRecord.get();
        }
        return result;
    }

    public static void removeBackTeleportRecord(EntityPlayerMP player, TeleportRecord record) {
        PlayerTeleportData.get(player).getTeleportRecords().remove(record);
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
        World world = player.getEntityWorld();
        if (world != null) {
            WorldServer level = DimensionManager.getWorld(DimensionUtils.getDimensionType(after.getDimension()));
            if (level != null) {
                if (after.isSafe()) {
                    // 异步的代价就是粪吗
                    NarcissusUtils.sendMessage(player, Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "safe_searching"));
                    new Thread(() -> {
                        Coordinate finalAfter = after.clone();
                        finalAfter = findSafeCoordinate(finalAfter, false);
                        Runnable runnable;
                        // 判断是否需要在脚下放置方块
                        if (ServerConfig.SETBLOCK_WHEN_SAFE_NOT_FOUND && !isSafeCoordinate(level, finalAfter)) {
                            Block blockState;
                            List<ItemStack> playerItemList = getPlayerItemList(player);
                            if (CollectionUtils.isNotNullOrEmpty(SAFE_BLOCKS)) {
                                if (ServerConfig.GETBLOCK_FROM_INVENTORY) {
                                    blockState = SAFE_BLOCKS.stream()
                                            .filter(block -> playerItemList.stream().map(ItemStack::getItem).anyMatch(item -> new ItemStack(block).getItem().equals(item)))
                                            .findFirst().orElse(null);
                                } else {
                                    blockState = SAFE_BLOCKS.get(0);
                                }
                            } else {
                                blockState = null;
                            }
                            if (blockState != null) {
                                Coordinate airCoordinate = findSafeCoordinate(finalAfter, true);
                                if (!airCoordinate.toXyzString().equals(finalAfter.toXyzString())) {
                                    finalAfter = airCoordinate;
                                    runnable = () -> {
                                        Item blockItem = new ItemStack(blockState).getItem();
                                        Item remove = playerItemList.stream().map(ItemStack::getItem).filter(blockItem::equals).findFirst().orElse(null);
                                        if (remove != null) {
                                            ItemStack itemStack = new ItemStack(remove);
                                            itemStack.stackSize = 1;
                                            if (removeItemFromPlayerInventory(player, itemStack)) {
                                                getWorld(DimensionUtils.getDimensionType(after.getDimension())).setBlock((int) airCoordinate.getX(), (int) (airCoordinate.getY() - 1), (int) airCoordinate.getZ(), blockState);
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
                        ServerTaskExecutor.run(() -> {
                            if (runnable != null) runnable.run();
                            doTeleport(player, finalAfter1, type, before, level);
                            // DimensionManager.unloadWorld(DimensionUtils.getDimensionType(finalAfter1.getDimension()));
                        });
                    }).start();
                } else {
                    doTeleport(player, after, type, before, level);
                }
            }
        }
    }

    private static void doTeleport(@NonNull EntityPlayerMP player, @NonNull Coordinate after, ETeleportType type, Coordinate before, WorldServer level) {
        after.setY(Math.floor(after.getY()) + 0.1);
        if (before.getDimension().equalsIgnoreCase(after.getDimension())) {
            player.setPositionAndUpdate(after.getX(), after.getY(), after.getZ());
            player.cameraYaw = after.getYaw() == 0 ? player.cameraYaw : (float) after.getYaw();
            player.cameraPitch = after.getPitch() == 0 ? player.cameraPitch : (float) after.getPitch();
        } else {
            NarcissusFarewell.getServerInstance().getConfigurationManager()
                    .transferPlayerToDimension(player, DimensionUtils.getDimensionType(after.getDimension()), new TeleporterCustom(level, after));
        }
        TeleportRecord record = new TeleportRecord();
        record.setTeleportTime(new Date());
        record.setTeleportType(type);
        record.setBefore(before);
        record.setAfter(after);
        PlayerTeleportData.get(player).addTeleportRecords(record);
    }

    // endregion 传送相关

    // region 玩家与玩家背包

    /**
     * 获取随机玩家
     */
    @SuppressWarnings("unchecked")
    public static EntityPlayerMP getRandomPlayer() {
        try {
            List<EntityPlayerMP> players = NarcissusFarewell.getServerInstance().getConfigurationManager().playerEntityList;
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
    @SuppressWarnings("unchecked")
    public static EntityPlayerMP getPlayer(UUID uuid) {
        try {
            return (EntityPlayerMP) NarcissusFarewell.getServerInstance().getConfigurationManager().playerEntityList.stream()
                    .filter(player -> ((EntityPlayerMP) player).getUniqueID().equals(uuid))
                    .findFirst().orElse(null);
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
        int remainingAmount = itemToRemove.stackSize;
        // 记录成功移除的物品数量，以便失败时进行回滚
        int successfullyRemoved = 0;

        // 遍历玩家背包的所有插槽
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            // 获取背包中的物品
            ItemStack stack = inventory.getStackInSlot(i);
            ItemStack copy = itemToRemove.copy();
            copy.stackSize = stack.stackSize;

            // 如果插槽中的物品是目标物品
            if (ItemStack.areItemStacksEqual(stack, copy)) {
                // 获取当前物品堆叠的数量
                int stackSize = stack.stackSize;

                // 如果堆叠数量大于或等于剩余需要移除的数量
                if (stackSize >= remainingAmount) {
                    // 移除指定数量的物品
                    stack.stackSize = stackSize - remainingAmount;
                    // 记录成功移除的数量
                    successfullyRemoved += remainingAmount;
                    // 移除完毕
                    remainingAmount = 0;
                    break;
                } else {
                    // 移除该堆所有物品
                    stack.stackSize = 0;
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
            copy.stackSize = successfullyRemoved;
            // 将已移除的物品添加回背包
            player.inventory.addItemStackToInventory(copy);
        }

        // 是否成功移除所有物品
        return remainingAmount == 0;
    }

    public static List<ItemStack> getPlayerItemList(EntityPlayerMP player) {
        List<ItemStack> result = new ArrayList<>();
        if (player != null) {
            result.addAll(Arrays.asList(player.inventory.mainInventory));
            result.addAll(Arrays.asList(player.inventory.armorInventory));
            result = result.stream().filter(itemStack -> itemStack != null && itemStack.stackSize > 0).collect(Collectors.toList());
        }
        return result;
    }

    public static List<ItemStack> getPlayerInventory(EntityPlayerMP player) {
        List<ItemStack> inventory = new ArrayList<>();
        inventory.addAll(Arrays.asList(player.inventory.mainInventory));
        inventory.addAll(Arrays.asList(player.inventory.armorInventory));
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
        List<IChatComponent> list = message.toChatComponent(NarcissusUtils.getPlayerLanguage(player));
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                NarcissusFarewell.getServerInstance().getConfigurationManager().sendChatMsgImpl(new ChatComponentTranslation("chat.type.announcement", player.getDisplayName(), list.get(i)), false);
            } else {
                NarcissusFarewell.getServerInstance().getConfigurationManager().sendChatMsgImpl(list.get(i), false);
            }
        }
    }

    /**
     * 广播消息
     *
     * @param server  发送者
     * @param message 消息
     */
    public static void broadcastMessage(MinecraftServer server, Component message) {
        List<IChatComponent> list = message.toChatComponent();
        for (int i = 0; i < list.size(); i++) {
            server.getConfigurationManager().sendChatMsgImpl(list.get(i), i == 0);
        }
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(EntityPlayerMP player, Component message) {
        for (IChatComponent component : message.toChatComponent(NarcissusUtils.getPlayerLanguage(player))) {
            player.addChatMessage(component);
        }
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(EntityPlayerMP player, String message) {
        player.addChatMessage(Component.literal(message).toTextComponent());
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(ICommandSender player, String message) {
        for (IChatComponent component : Component.literal(message).toChatComponent()) {
            player.addChatMessage(component);
        }
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(ICommandSender player, Component message) {
        for (IChatComponent component : message.toChatComponent()) {
            player.addChatMessage(component);
        }
    }

    /**
     * 发送翻译消息
     *
     * @param player 玩家
     * @param key    翻译键
     * @param args   参数
     */
    public static void sendTranslatableMessage(EntityPlayerMP player, String key, Object... args) {
        for (IChatComponent component : Component.translatable(key, args).setLanguageCode(NarcissusUtils.getPlayerLanguage(player)).toChatComponent()) {
            player.addChatMessage(component);
        }
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
            for (IChatComponent component : Component.translatable(key, args).setLanguageCode(NarcissusFarewell.DEFAULT_LANGUAGE).toChatComponent()) {
                source.addChatMessage(component);
            }
        }
    }

    // endregion 消息相关

    // region 跨维度传送

    public static boolean isTeleportAcrossDimensionEnabled(EntityPlayerMP player, String to, ETeleportType type) {
        boolean result = true;
        if (!DimensionUtils.getStringId(player.getEntityWorld().provider.dimensionId).equalsIgnoreCase(to)) {
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
            if (PlayerTeleportData.get(player).getTeleportCard() > 0) {
                return 0;
            }
        }
        Instant current = Instant.now();
        int commandCoolDown = getCommandCoolDown(type);
        Instant lastTpTime = PlayerTeleportData.get(player).getTeleportRecords(type).stream()
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
        return validateCost(request.getRequester(), DimensionUtils.getStringId(request.getTarget().getEntityWorld().provider.dimensionId), calculateDistance(requesterCoordinate, targetCoordinate), request.getTeleportType(), submit);
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
    private static boolean validateCost(EntityPlayerMP player, String targetDim, double distance, ETeleportType teleportType, boolean submit) {
        TeleportCost cost = NarcissusUtils.getCommandCost(teleportType);
        if (cost.getType() == ECostType.NONE) return true;

        double adjustedDistance;
        if (DimensionUtils.getStringId(player.getEntityWorld().provider.dimensionId).equalsIgnoreCase(targetDim)) {
            adjustedDistance = Math.min(ServerConfig.TELEPORT_COST_DISTANCE_LIMIT, distance);
        } else {
            adjustedDistance = ServerConfig.TELEPORT_COST_DISTANCE_ACROSS_DIMENSION;
        }

        double need = cost.getNum() * adjustedDistance * cost.getRate();
        int cardNeedTotal = getTeleportCardNeedPre(need);
        int cardNeed = getTeleportCardNeedPost(player, need);
        int costNeed = getTeleportCostNeedPost(player, need);
        boolean result = false;

        switch (cost.getType()) {
            case EXP_POINT:
                result = player.experienceTotal >= costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.WORD, "exp_point"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.addExperience(-costNeed);
                    PlayerTeleportData.get(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case EXP_LEVEL:
                result = player.experienceLevel >= costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.WORD, "exp_level"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.addExperienceLevel(-costNeed);
                    PlayerTeleportData.get(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case HEALTH:
                result = player.getHealth() > costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.WORD, "health"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.attackEntityFrom(DamageSource.magic, costNeed);
                    PlayerTeleportData.get(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case HUNGER:
                result = player.getFoodStats().getFoodLevel() >= costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.WORD, "hunger"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.getFoodStats().addStats(-costNeed, 0);
                    PlayerTeleportData.get(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case ITEM:
                try {
                    ItemStack itemStack = ItemStack.loadItemStackFromNBT((NBTTagCompound) JsonToNBT.func_150315_a(cost.getConf()));
                    result = getItemCount(getPlayerInventory(player), itemStack) >= costNeed && cardNeed == 0;
                    if (!result && cardNeed == 0) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), itemStack.getDisplayName(), (int) Math.ceil(need));
                    } else if (result && submit) {
                        itemStack.stackSize = costNeed;
                        result = removeItemFromPlayerInventory(player, itemStack);
                        // 代价不足
                        if (result) {
                            PlayerTeleportData.get(player).subTeleportCard(cardNeedTotal);
                        } else {
                            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), itemStack.getDisplayName(), (int) Math.ceil(need));
                        }
                    }
                } catch (Exception ignored) {
                }
                break;
            case COMMAND:
                try {
                    result = cardNeed == 0;
                    if (result && submit) {
                        String command = cost.getConf().replaceAll("\\[num]", String.valueOf(costNeed));
                        int commandResult = NarcissusFarewell.getServerInstance().getCommandManager().executeCommand(player, command);
                        if (commandResult > 0) {
                            PlayerTeleportData.get(player).subTeleportCard(cardNeedTotal);
                        }
                        result = commandResult > 0;
                    }
                } catch (Exception ignored) {
                }
                break;
        }
        if (!result && cardNeed > 0) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.WORD, "teleport_card"), (int) Math.ceil(need));
        }
        return result;
    }

    /**
     * 使用传送卡后还须支付多少代价
     */
    public static int getTeleportCostNeedPost(EntityPlayerMP player, double need) {
        int ceil = (int) Math.ceil(need);
        if (!ServerConfig.TELEPORT_CARD) return ceil;
        IPlayerTeleportData data = PlayerTeleportData.get(player);
        switch (ServerConfig.TELEPORT_CARD_TYPE) {
            case NONE:
                return data.getTeleportCard() > 0 ? ceil : -1;
            case LIKE_COST:
                return data.getTeleportCard() >= ceil ? ceil : -1;
            case REFUND_COST:
            case REFUND_COST_AND_COOLDOWN:
                return Math.max(0, ceil - data.getTeleportCard());
            case REFUND_ALL_COST:
            case REFUND_ALL_COST_AND_COOLDOWN:
                return data.getTeleportCard() > 0 ? 0 : ceil;
            case REFUND_COOLDOWN:
            default:
                return ceil;
        }
    }

    /**
     * 须支付多少传送卡
     */
    public static int getTeleportCardNeedPre(double need) {
        int ceil = (int) Math.ceil(need);
        if (!ServerConfig.TELEPORT_CARD) return 0;
        switch (ServerConfig.TELEPORT_CARD_TYPE) {
            case LIKE_COST:
                return ceil;
            case NONE:
            case REFUND_COST:
            case REFUND_COST_AND_COOLDOWN:
            case REFUND_ALL_COST:
            case REFUND_ALL_COST_AND_COOLDOWN:
            case REFUND_COOLDOWN:
            default:
                return 1;
        }
    }

    /**
     * 使用传送卡后还须支付多少传送卡
     */
    public static int getTeleportCardNeedPost(EntityPlayerMP player, double need) {
        int ceil = (int) Math.ceil(need);
        if (!ServerConfig.TELEPORT_CARD) return 0;
        IPlayerTeleportData data = PlayerTeleportData.get(player);
        switch (ServerConfig.TELEPORT_CARD_TYPE) {
            case NONE:
                return data.getTeleportCard() > 0 ? 0 : 1;
            case LIKE_COST:
                return Math.max(0, ceil - data.getTeleportCard());
            case REFUND_COST:
            case REFUND_COST_AND_COOLDOWN:
            case REFUND_ALL_COST:
            case REFUND_ALL_COST_AND_COOLDOWN:
            case REFUND_COOLDOWN:
            default:
                return 0;
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
            copy.stackSize = item.stackSize;
            return ItemStack.areItemStacksEqual(item, copy);
        }).mapToInt(stack -> stack.stackSize).sum();
    }

    public static double calculateDistance(Coordinate coordinate1, Coordinate coordinate2) {
        double deltaX = coordinate1.getX() - coordinate2.getX();
        double deltaY = coordinate1.getY() - coordinate2.getY();
        double deltaZ = coordinate1.getZ() - coordinate2.getZ();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    // endregion 传送代价

    // region 杂项
    public static String getPlayerLanguage(EntityPlayerMP player) {
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
        FieldUtils.setPrivateFieldValue(EntityPlayerMP.class, targetPlayer, FieldUtils.getPlayerLanguageFieldName(originalPlayer), getPlayerLanguage(originalPlayer));
    }

    public static final DamageSource DAMAGE_SOURCE = new DamageSource("narcissus");

    public static String getClientLanguage() {
        return Minecraft.getMinecraft().getLanguageManager().getCurrentLanguage().getLanguageCode();
    }

    /**
     * 强行使玩家死亡
     */
    public static boolean killPlayer(EntityPlayerMP player) {
        try {
            player.getDataWatcher().updateObject(6, 0f);
            if (!player.getEntityWorld().getGameRules().getGameRuleBooleanValue("keepInventory")) {
                player.captureDrops = true;
                player.capturedDrops.clear();
                player.inventory.dropAllItems();
                player.captureDrops = false;
                PlayerDropsEvent event = new PlayerDropsEvent(player, DAMAGE_SOURCE, player.capturedDrops, false);
                if (!MinecraftForge.EVENT_BUS.post(event)) {
                    for (net.minecraft.entity.item.EntityItem item : player.capturedDrops) {
                        player.getEntityWorld().spawnEntityInWorld(item);
                    }
                }
            }
            player.addStat(StatList.deathsStat, 1);
            player.extinguish();
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    // endregion 杂项
}
