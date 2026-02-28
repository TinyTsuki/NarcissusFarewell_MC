package xin.vanilla.narcissus.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.Packet;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.UsernameCache;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.network.PacketDistributor;
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

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("resource")
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
        return switch (type) {
            case CARD, SET_CARD, CARD_CONCISE, SET_CARD_CONCISE -> CommonConfig.TELEPORT_CARD.get();
            case SHARE, SHARE_CONCISE -> CommonConfig.SWITCH_SHARE.get();
            case FEED, FEED_OTHER, FEED_CONCISE, FEED_OTHER_CONCISE -> CommonConfig.SWITCH_FEED.get();
            case TP_COORDINATE, TP_COORDINATE_CONCISE -> CommonConfig.SWITCH_TP_COORDINATE.get();
            case TP_STRUCTURE, TP_STRUCTURE_CONCISE -> CommonConfig.SWITCH_TP_STRUCTURE.get();
            case TP_ASK, TP_ASK_YES, TP_ASK_NO, TP_ASK_CANCEL, TP_ASK_CONCISE, TP_ASK_YES_CONCISE, TP_ASK_NO_CONCISE,
                 TP_ASK_CANCEL_CONCISE -> CommonConfig.SWITCH_TP_ASK.get();
            case TP_HERE, TP_HERE_YES, TP_HERE_NO, TP_HERE_CANCEL, TP_HERE_CONCISE, TP_HERE_YES_CONCISE,
                 TP_HERE_NO_CONCISE, TP_HERE_CANCEL_CONCISE -> CommonConfig.SWITCH_TP_HERE.get();
            case TP_RANDOM, TP_RANDOM_CONCISE -> CommonConfig.SWITCH_TP_RANDOM.get();
            case TP_SPAWN, TP_SPAWN_OTHER, TP_SPAWN_CONCISE, TP_SPAWN_OTHER_CONCISE ->
                    CommonConfig.SWITCH_TP_SPAWN.get();
            case TP_WORLD_SPAWN, TP_WORLD_SPAWN_CONCISE -> CommonConfig.SWITCH_TP_WORLD_SPAWN.get();
            case TP_TOP, TP_TOP_CONCISE -> CommonConfig.SWITCH_TP_TOP.get();
            case TP_BOTTOM, TP_BOTTOM_CONCISE -> CommonConfig.SWITCH_TP_BOTTOM.get();
            case TP_UP, TP_UP_CONCISE -> CommonConfig.SWITCH_TP_UP.get();
            case TP_DOWN, TP_DOWN_CONCISE -> CommonConfig.SWITCH_TP_DOWN.get();
            case TP_VIEW, TP_VIEW_CONCISE -> CommonConfig.SWITCH_TP_VIEW.get();
            case TP_HOME, SET_HOME, DEL_HOME, GET_HOME, TP_HOME_CONCISE, SET_HOME_CONCISE, DEL_HOME_CONCISE,
                 GET_HOME_CONCISE -> CommonConfig.SWITCH_TP_HOME.get();
            case TP_STAGE, SET_STAGE, DEL_STAGE, GET_STAGE, TP_STAGE_CONCISE, SET_STAGE_CONCISE, DEL_STAGE_CONCISE,
                 GET_STAGE_CONCISE -> CommonConfig.SWITCH_TP_STAGE.get();
            case TP_BACK, TP_BACK_CONCISE -> CommonConfig.SWITCH_TP_BACK.get();
            case TP_GRAVE, TP_GRAVE_CONCISE -> CommonConfig.SWITCH_TP_GRAVE.get();
            case FLY, FLY_CONCISE -> CommonConfig.SWITCH_FLY.get();
            default -> true;
        };
    }

    public static String getCommand(EnumTeleportType type) {
        return switch (type) {
            case TP_COORDINATE -> CommonConfig.COMMAND_TP_COORDINATE.get();
            case TP_STRUCTURE -> CommonConfig.COMMAND_TP_STRUCTURE.get();
            case TP_ASK -> CommonConfig.COMMAND_TP_ASK.get();
            case TP_HERE -> CommonConfig.COMMAND_TP_HERE.get();
            case TP_RANDOM -> CommonConfig.COMMAND_TP_RANDOM.get();
            case TP_SPAWN -> CommonConfig.COMMAND_TP_SPAWN.get();
            case TP_WORLD_SPAWN -> CommonConfig.COMMAND_TP_WORLD_SPAWN.get();
            case TP_TOP -> CommonConfig.COMMAND_TP_TOP.get();
            case TP_BOTTOM -> CommonConfig.COMMAND_TP_BOTTOM.get();
            case TP_UP -> CommonConfig.COMMAND_TP_UP.get();
            case TP_DOWN -> CommonConfig.COMMAND_TP_DOWN.get();
            case TP_VIEW -> CommonConfig.COMMAND_TP_VIEW.get();
            case TP_HOME -> CommonConfig.COMMAND_TP_HOME.get();
            case TP_STAGE -> CommonConfig.COMMAND_TP_STAGE.get();
            case TP_BACK -> CommonConfig.COMMAND_TP_BACK.get();
            case TP_GRAVE -> CommonConfig.COMMAND_TP_GRAVE.get();
            default -> "";
        };
    }

    public static String getCommand(EnumCommandType type) {
        String prefix = NarcissusUtils.getCommandPrefix();
        return switch (type) {
            case HELP -> prefix + " help";
            case LANGUAGE -> prefix + " " + CommonConfig.COMMAND_LANGUAGE.get();
            case LANGUAGE_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_LANGUAGE.get() : "";
            case DIMENSION -> prefix + " " + CommonConfig.COMMAND_DIMENSION.get();
            case DIMENSION_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_DIMENSION.get() : "";
            case UUID -> prefix + " " + CommonConfig.COMMAND_UUID.get();
            case UUID_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_UUID.get() : "";
            case CARD, SET_CARD -> prefix + " " + CommonConfig.COMMAND_CARD.get();
            case CARD_CONCISE, SET_CARD_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_CARD.get() : "";
            case SHARE -> prefix + " " + CommonConfig.COMMAND_SHARE.get();
            case SHARE_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_SHARE.get() : "";
            case FEED, FEED_OTHER -> prefix + " " + CommonConfig.COMMAND_FEED.get();
            case FEED_CONCISE, FEED_OTHER_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_FEED.get() : "";
            case TP_COORDINATE -> prefix + " " + CommonConfig.COMMAND_TP_COORDINATE.get();
            case TP_COORDINATE_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_COORDINATE.get() : "";
            case TP_STRUCTURE -> prefix + " " + CommonConfig.COMMAND_TP_STRUCTURE.get();
            case TP_STRUCTURE_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_STRUCTURE.get() : "";
            case TP_ASK -> prefix + " " + CommonConfig.COMMAND_TP_ASK.get();
            case TP_ASK_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_ASK.get() : "";
            case TP_ASK_YES -> prefix + " " + CommonConfig.COMMAND_TP_ASK_YES.get();
            case TP_ASK_YES_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_ASK_YES.get() : "";
            case TP_ASK_NO -> prefix + " " + CommonConfig.COMMAND_TP_ASK_NO.get();
            case TP_ASK_NO_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_ASK_NO.get() : "";
            case TP_ASK_CANCEL -> prefix + " " + CommonConfig.COMMAND_TP_ASK_CANCEL.get();
            case TP_ASK_CANCEL_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_ASK_CANCEL.get() : "";
            case TP_HERE -> prefix + " " + CommonConfig.COMMAND_TP_HERE.get();
            case TP_HERE_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_HERE.get() : "";
            case TP_HERE_YES -> prefix + " " + CommonConfig.COMMAND_TP_HERE_YES.get();
            case TP_HERE_YES_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_HERE_YES.get() : "";
            case TP_HERE_NO -> prefix + " " + CommonConfig.COMMAND_TP_HERE_NO.get();
            case TP_HERE_NO_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_HERE_NO.get() : "";
            case TP_HERE_CANCEL -> prefix + " " + CommonConfig.COMMAND_TP_HERE_CANCEL.get();
            case TP_HERE_CANCEL_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_HERE_CANCEL.get() : "";
            case TP_RANDOM -> prefix + " " + CommonConfig.COMMAND_TP_RANDOM.get();
            case TP_RANDOM_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_RANDOM.get() : "";
            case TP_SPAWN, TP_SPAWN_OTHER -> prefix + " " + CommonConfig.COMMAND_TP_SPAWN.get();
            case TP_SPAWN_CONCISE, TP_SPAWN_OTHER_CONCISE ->
                    isConciseEnabled(type) ? CommonConfig.COMMAND_TP_SPAWN.get() : "";
            case TP_WORLD_SPAWN -> prefix + " " + CommonConfig.COMMAND_TP_WORLD_SPAWN.get();
            case TP_WORLD_SPAWN_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_WORLD_SPAWN.get() : "";
            case TP_TOP -> prefix + " " + CommonConfig.COMMAND_TP_TOP.get();
            case TP_TOP_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_TOP.get() : "";
            case TP_BOTTOM -> prefix + " " + CommonConfig.COMMAND_TP_BOTTOM.get();
            case TP_BOTTOM_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_BOTTOM.get() : "";
            case TP_UP -> prefix + " " + CommonConfig.COMMAND_TP_UP.get();
            case TP_UP_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_UP.get() : "";
            case TP_DOWN -> prefix + " " + CommonConfig.COMMAND_TP_DOWN.get();
            case TP_DOWN_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_DOWN.get() : "";
            case TP_VIEW -> prefix + " " + CommonConfig.COMMAND_TP_VIEW.get();
            case TP_VIEW_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_VIEW.get() : "";
            case TP_HOME -> prefix + " " + CommonConfig.COMMAND_TP_HOME.get();
            case TP_HOME_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_HOME.get() : "";
            case SET_HOME -> prefix + " " + CommonConfig.COMMAND_SET_HOME.get();
            case SET_HOME_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_SET_HOME.get() : "";
            case DEL_HOME -> prefix + " " + CommonConfig.COMMAND_DEL_HOME.get();
            case DEL_HOME_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_DEL_HOME.get() : "";
            case GET_HOME -> prefix + " " + CommonConfig.COMMAND_GET_HOME.get();
            case GET_HOME_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_GET_HOME.get() : "";
            case TP_STAGE -> prefix + " " + CommonConfig.COMMAND_TP_STAGE.get();
            case TP_STAGE_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_STAGE.get() : "";
            case SET_STAGE -> prefix + " " + CommonConfig.COMMAND_SET_STAGE.get();
            case SET_STAGE_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_SET_STAGE.get() : "";
            case DEL_STAGE -> prefix + " " + CommonConfig.COMMAND_DEL_STAGE.get();
            case DEL_STAGE_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_DEL_STAGE.get() : "";
            case GET_STAGE -> prefix + " " + CommonConfig.COMMAND_GET_STAGE.get();
            case GET_STAGE_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_GET_STAGE.get() : "";
            case TP_BACK -> prefix + " " + CommonConfig.COMMAND_TP_BACK.get();
            case TP_BACK_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_BACK.get() : "";
            case TP_GRAVE -> prefix + " " + CommonConfig.COMMAND_TP_GRAVE.get();
            case TP_GRAVE_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_TP_GRAVE.get() : "";
            case FLY -> prefix + " " + CommonConfig.COMMAND_FLY.get();
            case FLY_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_FLY.get() : "";
            case VIRTUAL_OP -> prefix + " " + CommonConfig.COMMAND_VIRTUAL_OP.get();
            case VIRTUAL_OP_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_VIRTUAL_OP.get() : "";
            case CONFIG -> prefix + " config";
            case BLACKLIST -> prefix + " config black";
            case WHITELIST -> prefix + " config white";
            default -> "";
        };
    }

    public static int getCommandPermissionLevel(EnumCommandType type) {
        return switch (type) {
            case SET_CARD, SET_CARD_CONCISE -> ServerConfig.PERMISSION_SET_CARD.get();
            case FEED_OTHER, FEED_OTHER_CONCISE -> ServerConfig.PERMISSION_FEED_OTHER.get();
            case TP_COORDINATE, TP_COORDINATE_CONCISE -> ServerConfig.PERMISSION_TP_COORDINATE.get();
            case TP_STRUCTURE, TP_STRUCTURE_CONCISE -> ServerConfig.PERMISSION_TP_STRUCTURE.get();
            // case TP_ASK_YES:
            // case TP_ASK_NO:
            case TP_ASK, TP_ASK_CANCEL, TP_ASK_CONCISE, TP_ASK_CANCEL_CONCISE ->
                // case TP_ASK_YES_CONCISE:
                // case TP_ASK_NO_CONCISE:
                    ServerConfig.PERMISSION_TP_ASK.get();
            // case TP_HERE_YES:
            // case TP_HERE_NO:
            case TP_HERE, TP_HERE_CANCEL, TP_HERE_CONCISE, TP_HERE_CANCEL_CONCISE ->
                // case TP_HERE_YES_CONCISE:
                // case TP_HERE_NO_CONCISE:
                    ServerConfig.PERMISSION_TP_HERE.get();
            case TP_RANDOM, TP_RANDOM_CONCISE -> ServerConfig.PERMISSION_TP_RANDOM.get();
            case TP_SPAWN, TP_SPAWN_CONCISE -> ServerConfig.PERMISSION_TP_SPAWN.get();
            case TP_SPAWN_OTHER, TP_SPAWN_OTHER_CONCISE -> ServerConfig.PERMISSION_TP_SPAWN_OTHER.get();
            case TP_WORLD_SPAWN, TP_WORLD_SPAWN_CONCISE -> ServerConfig.PERMISSION_TP_WORLD_SPAWN.get();
            case TP_TOP, TP_TOP_CONCISE -> ServerConfig.PERMISSION_TP_TOP.get();
            case TP_BOTTOM, TP_BOTTOM_CONCISE -> ServerConfig.PERMISSION_TP_BOTTOM.get();
            case TP_UP, TP_UP_CONCISE -> ServerConfig.PERMISSION_TP_UP.get();
            case TP_DOWN, TP_DOWN_CONCISE -> ServerConfig.PERMISSION_TP_DOWN.get();
            case TP_VIEW, TP_VIEW_CONCISE -> ServerConfig.PERMISSION_TP_VIEW.get();
            case TP_HOME, SET_HOME, DEL_HOME, GET_HOME, TP_HOME_CONCISE, SET_HOME_CONCISE, DEL_HOME_CONCISE,
                 GET_HOME_CONCISE -> ServerConfig.PERMISSION_TP_HOME.get();
            case TP_STAGE, TP_STAGE_CONCISE -> ServerConfig.PERMISSION_TP_STAGE.get();
            case SET_STAGE, SET_STAGE_CONCISE -> ServerConfig.PERMISSION_SET_STAGE.get();
            case DEL_STAGE, DEL_STAGE_CONCISE -> ServerConfig.PERMISSION_DEL_STAGE.get();
            case GET_STAGE, GET_STAGE_CONCISE -> ServerConfig.PERMISSION_GET_STAGE.get();
            case TP_BACK, TP_BACK_CONCISE -> ServerConfig.PERMISSION_TP_BACK.get();
            case TP_GRAVE, TP_GRAVE_CONCISE -> ServerConfig.PERMISSION_TP_GRAVE.get();
            case FLY, FLY_CONCISE -> ServerConfig.PERMISSION_FLY.get();
            case VIRTUAL_OP, VIRTUAL_OP_CONCISE -> ServerConfig.PERMISSION_VIRTUAL_OP.get();
            default -> 0;
        };
    }

    public static int getCommandPermissionLevel(EnumTeleportType type) {
        return switch (type) {
            case TP_COORDINATE -> ServerConfig.PERMISSION_TP_COORDINATE.get();
            case TP_STRUCTURE -> ServerConfig.PERMISSION_TP_STRUCTURE.get();
            case TP_ASK -> ServerConfig.PERMISSION_TP_ASK.get();
            case TP_HERE -> ServerConfig.PERMISSION_TP_HERE.get();
            case TP_RANDOM -> ServerConfig.PERMISSION_TP_RANDOM.get();
            case TP_SPAWN -> ServerConfig.PERMISSION_TP_SPAWN.get();
            case TP_WORLD_SPAWN -> ServerConfig.PERMISSION_TP_WORLD_SPAWN.get();
            case TP_TOP -> ServerConfig.PERMISSION_TP_TOP.get();
            case TP_BOTTOM -> ServerConfig.PERMISSION_TP_BOTTOM.get();
            case TP_UP -> ServerConfig.PERMISSION_TP_UP.get();
            case TP_DOWN -> ServerConfig.PERMISSION_TP_DOWN.get();
            case TP_VIEW -> ServerConfig.PERMISSION_TP_VIEW.get();
            case TP_HOME -> ServerConfig.PERMISSION_TP_HOME.get();
            case TP_STAGE -> ServerConfig.PERMISSION_TP_STAGE.get();
            case TP_BACK -> ServerConfig.PERMISSION_TP_BACK.get();
            default -> 0;
        };
    }

    public static boolean isConciseEnabled(EnumCommandType type) {
        return switch (type) {
            case LANGUAGE, LANGUAGE_CONCISE -> CommonConfig.CONCISE_LANGUAGE.get();
            case UUID, UUID_CONCISE -> CommonConfig.CONCISE_UUID.get();
            case DIMENSION, DIMENSION_CONCISE -> CommonConfig.CONCISE_DIMENSION.get();
            case CARD, CARD_CONCISE, SET_CARD, SET_CARD_CONCISE -> CommonConfig.CONCISE_CARD.get();
            case SHARE, SHARE_CONCISE -> CommonConfig.CONCISE_SHARE.get();
            case FEED, FEED_OTHER, FEED_CONCISE, FEED_OTHER_CONCISE -> CommonConfig.CONCISE_FEED.get();
            case TP_COORDINATE, TP_COORDINATE_CONCISE -> CommonConfig.CONCISE_TP_COORDINATE.get();
            case TP_STRUCTURE, TP_STRUCTURE_CONCISE -> CommonConfig.CONCISE_TP_STRUCTURE.get();
            case TP_ASK, TP_ASK_CONCISE -> CommonConfig.CONCISE_TP_ASK.get();
            case TP_ASK_YES, TP_ASK_YES_CONCISE -> CommonConfig.CONCISE_TP_ASK_YES.get();
            case TP_ASK_NO, TP_ASK_NO_CONCISE -> CommonConfig.CONCISE_TP_ASK_NO.get();
            case TP_ASK_CANCEL, TP_ASK_CANCEL_CONCISE -> CommonConfig.CONCISE_TP_ASK_CANCEL.get();
            case TP_HERE, TP_HERE_CONCISE -> CommonConfig.CONCISE_TP_HERE.get();
            case TP_HERE_YES, TP_HERE_YES_CONCISE -> CommonConfig.CONCISE_TP_HERE_YES.get();
            case TP_HERE_NO, TP_HERE_NO_CONCISE -> CommonConfig.CONCISE_TP_HERE_NO.get();
            case TP_HERE_CANCEL, TP_HERE_CANCEL_CONCISE -> CommonConfig.CONCISE_TP_HERE_CANCEL.get();
            case TP_RANDOM, TP_RANDOM_CONCISE -> CommonConfig.CONCISE_TP_RANDOM.get();
            case TP_SPAWN, TP_SPAWN_OTHER, TP_SPAWN_CONCISE, TP_SPAWN_OTHER_CONCISE ->
                    CommonConfig.CONCISE_TP_SPAWN.get();
            case TP_WORLD_SPAWN, TP_WORLD_SPAWN_CONCISE -> CommonConfig.CONCISE_TP_WORLD_SPAWN.get();
            case TP_TOP, TP_TOP_CONCISE -> CommonConfig.CONCISE_TP_TOP.get();
            case TP_BOTTOM, TP_BOTTOM_CONCISE -> CommonConfig.CONCISE_TP_BOTTOM.get();
            case TP_UP, TP_UP_CONCISE -> CommonConfig.CONCISE_TP_UP.get();
            case TP_DOWN, TP_DOWN_CONCISE -> CommonConfig.CONCISE_TP_DOWN.get();
            case TP_VIEW, TP_VIEW_CONCISE -> CommonConfig.CONCISE_TP_VIEW.get();
            case TP_HOME, TP_HOME_CONCISE -> CommonConfig.CONCISE_TP_HOME.get();
            case SET_HOME, SET_HOME_CONCISE -> CommonConfig.CONCISE_SET_HOME.get();
            case DEL_HOME, DEL_HOME_CONCISE -> CommonConfig.CONCISE_DEL_HOME.get();
            case GET_HOME, GET_HOME_CONCISE -> CommonConfig.CONCISE_GET_HOME.get();
            case TP_STAGE, TP_STAGE_CONCISE -> CommonConfig.CONCISE_TP_STAGE.get();
            case SET_STAGE, SET_STAGE_CONCISE -> CommonConfig.CONCISE_SET_STAGE.get();
            case DEL_STAGE, DEL_STAGE_CONCISE -> CommonConfig.CONCISE_DEL_STAGE.get();
            case GET_STAGE, GET_STAGE_CONCISE -> CommonConfig.CONCISE_GET_STAGE.get();
            case TP_BACK, TP_BACK_CONCISE -> CommonConfig.CONCISE_TP_BACK.get();
            case TP_GRAVE, TP_GRAVE_CONCISE -> CommonConfig.CONCISE_TP_GRAVE.get();
            case FLY, FLY_CONCISE -> CommonConfig.CONCISE_FLY.get();
            case VIRTUAL_OP, VIRTUAL_OP_CONCISE -> CommonConfig.CONCISE_VIRTUAL_OP.get();
            default -> false;
        };
    }

    public static boolean hasCommandPermission(CommandSourceStack source, EnumCommandType type) {
        return source.hasPermission(getCommandPermissionLevel(type)) || hasVirtualPermission(source.getEntity(), type);
    }

    public static boolean hasVirtualPermission(Entity source, EnumCommandType type) {
        // 若为玩家
        if (source instanceof Player) {
            return VirtualPermissionManager.getVirtualPermission((Player) source).stream()
                    .filter(Objects::nonNull)
                    .anyMatch(s -> s.replaceConcise() == type.replaceConcise());
        } else {
            return false;
        }
    }

    /**
     * 执行指令
     */
    public static boolean executeCommand(@NonNull ServerPlayer player, @NonNull String command, int permission, boolean suppressedOutput) {
        AtomicBoolean result = new AtomicBoolean(false);
        try {
            MinecraftServer server = player.getServer();
            CommandSourceStack commandSourceStack = player.createCommandSourceStack();
            if (permission > 0) {
                commandSourceStack = commandSourceStack.withPermission(permission);
            }
            if (suppressedOutput) {
                commandSourceStack = commandSourceStack.withSuppressedOutput();
            }
            if (server != null) {
                server.getCommands().performPrefixedCommand(commandSourceStack.withCallback((success, r) -> result.set(success && r > 0)), command);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: {}", command, e);
        }
        return result.get();
    }

    /**
     * 执行指令
     */
    public static boolean executeCommand(@NonNull ServerPlayer player, @NonNull String command) {
        return executeCommand(player, command, 0, false);
    }

    /**
     * 执行指令
     */
    public static boolean executeCommandNoOutput(@NonNull ServerPlayer player, @NonNull String command) {
        return executeCommandNoOutput(player, command, 0);
    }

    /**
     * 执行指令
     */
    public static boolean executeCommandNoOutput(@NonNull ServerPlayer player, @NonNull String command, int permission) {
        return executeCommand(player, command, permission, true);
    }

    public static void refreshPermission(@NonNull ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            server = NarcissusFarewell.getServerInstance();
        }
        server.getPlayerList().sendPlayerPermissionLevel(player);
    }

    // endregion 指令相关

    // region 安全坐标

    public static ServerLevel getServerLevel() {
        return NarcissusFarewell.getServerInstance().getAllLevels().iterator().next();
    }

    public static int getWorldMinY(Level world) {
        return world.getMinBuildHeight();
    }

    public static int getWorldMaxY(Level world) {
        return world.getMaxBuildHeight();
    }

    public static Coordinate findTopCandidate(ServerPlayer player, Coordinate start) {
        return new SafeCoordinateFinder(player.level(), player).findTopCandidate(start);
    }

    public static Coordinate findBottomCandidate(ServerPlayer player, Coordinate start) {
        return new SafeCoordinateFinder(player.level(), player).findBottomCandidate(start);
    }

    public static Coordinate findUpCandidate(ServerPlayer player, Coordinate start) {
        return new SafeCoordinateFinder(player.level(), player).findUpCandidate(start);
    }

    public static Coordinate findDownCandidate(ServerPlayer player, Coordinate start) {
        return new SafeCoordinateFinder(player.level(), player).findDownCandidate(start);
    }

    public static Coordinate findViewEndCandidate(ServerPlayer player, boolean safe, int range) {
        return new SafeCoordinateFinder(player.level(), player).findViewEndCandidate(player, safe, range);
    }

    public static Coordinate findSafeCoordinate(Coordinate coordinate, ServerPlayer player, boolean belowAllowAir) {
        Level world = DimensionUtils.getLevel(coordinate.dimension());
        int chunkX = (int) coordinate.x() >> 4;
        int chunkZ = (int) coordinate.z() >> 4;
        Coordinate result = new SafeCoordinateFinder(player.level(), player).searchInChunk(coordinate, chunkX, chunkZ, belowAllowAir);
        LOGGER.debug("Target:{} | Safe:{}", coordinate.toXyzIntString(), result == null ? "null" : result.toXyzIntString());
        return result == null ? coordinate : result;
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
                    .filter(key -> key.key().equals(player.level().dimension().location().toString()))
                    .findFirst()
                    .map(KeyValue::key)
                    .orElse(null);
        }
    }

    public static KeyValue<String, String> getPlayerHomeKey(ServerPlayer player, ResourceKey<Level> dimension, String name) {
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        Map<KeyValue<String, String>, Coordinate> homeCoordinate = data.getHomeCoordinate();
        Map<String, String> defaultHome = data.getDefaultHome();
        String currentDimStr = player.level().dimension().location().toString();
        String targetDimStr = dimension != null ? dimension.location().toString() : null;

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
    public static Coordinate getPlayerHome(ServerPlayer player, ResourceKey<Level> dimension, String name) {
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
                    .filter(key -> key.key().equals(player.level().dimension().location().toString()))
                    .findFirst()
                    .map(KeyValue::key)
                    .orElse(null);
        }
        return null;
    }

    public static KeyValue<String, String> getStageKey(ServerPlayer player, ResourceKey<Level> dimension, String name) {
        WorldStageData stageData = WorldStageData.get();
        Map<KeyValue<String, String>, Coordinate> stageCoordinate = stageData.getStageCoordinate();
        String currentDimStr = player.level().dimension().location().toString();
        String targetDimStr = dimension != null ? dimension.location().toString() : null;

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
    private static KeyValue<String, String> findNearestStageInDimension(Map<KeyValue<String, String>, Coordinate> stageCoordinate,
                                                                        ServerPlayer player, String dimensionStr) {
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
    public static Coordinate getStageCoordinate(ServerPlayer player, ResourceKey<Level> dimension, String name) {
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

    public static void removeBackTeleportRecord(ServerPlayer player, TeleportRecord record) {
        PlayerTeleportData.getData(player).getTeleportRecords().remove(record);
    }

    // endregion 坐标查找

    // region 传送相关

    /**
     * 检查传送范围
     */
    public static int checkRange(ServerPlayer player, EnumTeleportType type, int range) {
        int maxRange = switch (type) {
            case TP_VIEW -> ServerConfig.TELEPORT_VIEW_DISTANCE_LIMIT.get();
            default -> ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get();
        };
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
    public static void teleportTo(@NonNull ServerPlayer from, @NonNull ServerPlayer to, EnumTeleportType type, boolean safe) {
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
    public static void teleportTo(@NonNull ServerPlayer player, @NonNull Coordinate after, EnumTeleportType type) {
        Coordinate before = new Coordinate(player);
        Level world = player.level();
        if (world != null) {
            ServerLevel level = DimensionUtils.getLevel(after.dimension());
            if (level != null) {
                if (after.safe()) {
                    // 异步的代价就是粪吗
                    NarcissusUtils.sendActionBarMessage(player, Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, "safe_searching"));
                    new Thread(() -> {
                        Coordinate finalAfter = after.clone();
                        finalAfter = findSafeCoordinate(finalAfter, player, false);
                        Runnable runnable;
                        // 判断是否需要在脚下放置方块
                        SafeBlockChecker checker = new SafeBlockChecker(level, player);
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
                                Coordinate airCoordinate = findSafeCoordinate(finalAfter, player, true);
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

    private static void teleportPlayer(@NonNull ServerPlayer player, @NonNull Coordinate after, EnumTeleportType type, Coordinate before, ServerLevel level) {
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
            broadcastPacket(new ClientboundSetPassengersPacket(vehicle));
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
    private static @Nullable Entity teleportPassengers(ServerPlayer player, Entity parent, Entity passenger, @NonNull Coordinate coordinate, ServerLevel level) {
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
        broadcastPacket(new ClientboundSetPassengersPacket(passenger));
        return playerVehicle;
    }

    /**
     * 传送跟随的实体
     */
    private static void teleportFollowers(@NonNull ServerPlayer player, @NonNull Coordinate coordinate, ServerLevel level) {
        if (!ServerConfig.TP_WITH_FOLLOWER.get()) return;

        int followerRange = ServerConfig.TP_WITH_FOLLOWER_RANGE.get();

        // 传送主动跟随的实体
        for (TamableAnimal entity : player.level().getEntitiesOfClass(TamableAnimal.class, player.getBoundingBox().inflate(followerRange))) {
            if (entity.getOwnerUUID() != null && entity.getOwnerUUID().equals(player.getUUID()) && !entity.isOrderedToSit()) {
                doTeleport(entity, coordinate, level);
            }
        }

        // 传送拴绳实体
        for (Mob entity : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(followerRange))) {
            if (entity.getLeashHolder() == player) {
                doTeleport(entity, coordinate, level);
            }
        }

        // 传送被吸引的非敌对实体
        for (Mob entity : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(followerRange))) {
            // 排除敌对生物
            if (entity instanceof Monster) continue;

            if (entity.goalSelector.getRunningGoals()
                    .anyMatch(goal -> goal.isRunning()
                            && (goal.getGoal() instanceof TemptGoal)
                            && ((TemptGoalAccessor) goal.getGoal()).narcissus$player() == player
                    )) {
                doTeleport(entity, coordinate, level);
            }
        }
    }

    private static Entity doTeleport(@NonNull Entity entity, @NonNull Coordinate coordinate, ServerLevel level) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(level, coordinate.x(), coordinate.y(), coordinate.z()
                    , coordinate.yaw() == 0 ? player.getYRot() : (float) coordinate.yaw()
                    , coordinate.pitch() == 0 ? player.getXRot() : (float) coordinate.pitch());
        } else {
            if (level == entity.level()) {
                entity.teleportToWithTicket(coordinate.x(), coordinate.y(), coordinate.z());
            } else {
                entity = entity.changeDimension(level, new ITeleporter() {
                    @Override
                    public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
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
                        newEntity.moveTo(coordinate.x(), coordinate.y(), coordinate.z(), yaw, newEntity.getXRot());
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
    public static ServerPlayer getRandomPlayer() {
        try {
            List<ServerPlayer> players = NarcissusFarewell.getServerInstance().getPlayerList().getPlayers();
            return players.get(new Random().nextInt(players.size()));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 获取随机玩家UUID
     */
    public static UUID getRandomPlayerUUID() {
        Player randomPlayer = getRandomPlayer();
        return randomPlayer != null ? randomPlayer.getUUID() : null;
    }

    /**
     * 通过UUID获取对应的玩家
     *
     * @param uuid 玩家UUID
     */
    public static ServerPlayer getPlayer(UUID uuid) {
        try {
            return NarcissusFarewell.getServerInstance().getPlayerList().getPlayer(uuid);
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
    public static boolean removeItemFromPlayerInventory(ServerPlayer player, ItemStack itemToRemove) {
        Inventory inventory = player.getInventory();

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
            player.getInventory().add(copy);
        }

        // 是否成功移除所有物品
        return remainingAmount == 0;
    }

    public static List<ItemStack> getPlayerItemList(ServerPlayer player) {
        List<ItemStack> result = new ArrayList<>();
        if (player != null) {
            result.addAll(player.getInventory().items);
            result.addAll(player.getInventory().armor);
            result.addAll(player.getInventory().offhand);
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
    public static void broadcastMessage(ServerPlayer source, Component message) {
        for (ServerPlayer player : source.server.getPlayerList().getPlayers()) {
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
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
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
    public static void sendMessage(ServerPlayer player, Component message) {
        player.sendSystemMessage(message.toChatComponent(NarcissusUtils.getPlayerLanguage(player)), false);
    }

    /**
     * 发送消息
     *
     * @param player  玩家
     * @param message 消息
     */
    public static void sendMessage(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal(message).toChatComponent(), false);
    }

    /**
     * 发送翻译消息
     *
     * @param player 玩家
     * @param key    翻译键
     * @param args   参数
     */
    public static void sendTranslatableMessage(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(Component.trans(key, args).languageCode(NarcissusUtils.getPlayerLanguage(player)).toChatComponent(), false);
    }

    /**
     * 发送翻译消息
     *
     * @param source  指令来源
     * @param success 是否成功
     * @param key     翻译键
     * @param args    参数
     */
    public static void sendTranslatableMessage(CommandSourceStack source, boolean success, String key, Object... args) {
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
            try {
                sendTranslatableMessage(source.getPlayerOrException(), key, args);
            } catch (CommandSyntaxException ignored) {
            }
        } else if (success) {
            source.sendSuccess(() -> Component.trans(key, args).languageCode(ServerConfig.DEFAULT_LANGUAGE.get()).toChatComponent(), false);
        } else {
            source.sendFailure(Component.trans(key, args).languageCode(ServerConfig.DEFAULT_LANGUAGE.get()).toChatComponent());
        }
    }

    /**
     * 发送操作栏消息
     */
    public static void sendActionBarMessage(ServerPlayer player, Component message) {
        player.displayClientMessage(message.toTextComponent(NarcissusUtils.getPlayerLanguage(player)), true);
    }

    /**
     * 广播数据包至所有玩家
     *
     * @param packet 数据包
     */
    public static void broadcastPacket(Packet<?> packet) {
        NarcissusFarewell.getServerInstance().getPlayerList().getPlayers().forEach(player -> player.connection.send(packet));
    }

    /**
     * 发送数据包至服务器
     */
    public static <MSG> void sendPacketToServer(MSG msg) {
        ModNetworkHandler.INSTANCE.send(msg, PacketDistributor.SERVER.noArg());
    }

    /**
     * 发送数据包至玩家
     */
    public static <MSG> void sendPacketToPlayer(MSG msg, ServerPlayer player) {
        ModNetworkHandler.INSTANCE.send(msg, PacketDistributor.PLAYER.with(player));
    }

    /**
     * 广播数据包至所有玩家
     */
    public static <MSG> void broadcastPacket(MSG msg) {
        NarcissusFarewell.getServerInstance().getPlayerList().getPlayers().forEach(player ->
                ModNetworkHandler.INSTANCE.send(msg, PacketDistributor.PLAYER.with(player))
        );
    }

    // endregion 消息相关

    // region 跨维度传送

    public static boolean isTeleportAcrossDimensionEnabled(ServerPlayer player, ResourceKey<Level> to, EnumTeleportType type) {
        boolean result = true;
        if (player.level().dimension() != to) {
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
    public static boolean isTeleportTypeAcrossDimensionEnabled(ServerPlayer player, EnumTeleportType type) {
        int permission = switch (type) {
            case TP_COORDINATE -> ServerConfig.PERMISSION_TP_COORDINATE_ACROSS_DIMENSION.get();
            case TP_STRUCTURE -> ServerConfig.PERMISSION_TP_STRUCTURE_ACROSS_DIMENSION.get();
            case TP_ASK -> ServerConfig.PERMISSION_TP_ASK_ACROSS_DIMENSION.get();
            case TP_HERE -> ServerConfig.PERMISSION_TP_HERE_ACROSS_DIMENSION.get();
            case TP_RANDOM -> ServerConfig.PERMISSION_TP_RANDOM_ACROSS_DIMENSION.get();
            case TP_SPAWN -> ServerConfig.PERMISSION_TP_SPAWN_ACROSS_DIMENSION.get();
            case TP_WORLD_SPAWN -> ServerConfig.PERMISSION_TP_WORLD_SPAWN_ACROSS_DIMENSION.get();
            case TP_HOME -> ServerConfig.PERMISSION_TP_HOME_ACROSS_DIMENSION.get();
            case TP_STAGE -> ServerConfig.PERMISSION_TP_STAGE_ACROSS_DIMENSION.get();
            case TP_BACK -> ServerConfig.PERMISSION_TP_BACK_ACROSS_DIMENSION.get();
            case TP_GRAVE -> ServerConfig.PERMISSION_TP_GRAVE_ACROSS_DIMENSION.get();
            default -> 0;
        };
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
        return switch (type) {
            case TP_COORDINATE -> ServerConfig.COOLDOWN_TP_COORDINATE.get();
            case TP_STRUCTURE -> ServerConfig.COOLDOWN_TP_STRUCTURE.get();
            case TP_ASK -> ServerConfig.COOLDOWN_TP_ASK.get();
            case TP_HERE -> ServerConfig.COOLDOWN_TP_HERE.get();
            case TP_RANDOM -> ServerConfig.COOLDOWN_TP_RANDOM.get();
            case TP_SPAWN -> ServerConfig.COOLDOWN_TP_SPAWN.get();
            case TP_WORLD_SPAWN -> ServerConfig.COOLDOWN_TP_WORLD_SPAWN.get();
            case TP_TOP -> ServerConfig.COOLDOWN_TP_TOP.get();
            case TP_BOTTOM -> ServerConfig.COOLDOWN_TP_BOTTOM.get();
            case TP_UP -> ServerConfig.COOLDOWN_TP_UP.get();
            case TP_DOWN -> ServerConfig.COOLDOWN_TP_DOWN.get();
            case TP_VIEW -> ServerConfig.COOLDOWN_TP_VIEW.get();
            case TP_HOME -> ServerConfig.COOLDOWN_TP_HOME.get();
            case TP_STAGE -> ServerConfig.COOLDOWN_TP_STAGE.get();
            case TP_BACK -> ServerConfig.COOLDOWN_TP_BACK.get();
            case TP_GRAVE -> ServerConfig.COOLDOWN_TP_GRAVE.get();
            default -> 0;
        };
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
    public static boolean validTeleportCost(ServerPlayer player, Coordinate target, EnumTeleportType type, boolean submit) {
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
        return validateCost(request.getRequester(), request.getTarget().level().dimension(), calculateDistance(requesterCoordinate, targetCoordinate), request.getTeleportType(), submit);
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
                    ItemParser.ItemResult itemResult = ItemParser.parseForItem(BuiltInRegistries.ITEM.asLookup(), new StringReader(teleportCost.getConf()));
                    ItemStack itemStack = new ItemInput(itemResult.item(), itemResult.nbt()).createItemStack(1, false);
                    result = getItemCount(player.getInventory().items, itemStack) >= costNeed;
                    itemStack.setCount(costNeed);
                    if (!result) {
                        NarcissusUtils.sendMessage(player
                                , Component.trans(EnumI18nType.FORMAT, "cost_not_enough"
                                        , Component.literal(NarcissusUtils.getItemName(itemStack))
                                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(itemStack)))
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

    public static CompoundTag readCompressed(InputStream stream) {
        try {
            return NbtIo.readCompressed(stream, NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            LOGGER.error("Failed to read compressed stream", e);
            return new CompoundTag();
        }
    }

    public static CompoundTag readCompressed(File file) {
        try {
            return NbtIo.readCompressed(file.toPath(), NbtAccounter.unlimitedHeap());
        } catch (Exception e) {
            LOGGER.error("Failed to read compressed file: {}", file.getAbsolutePath(), e);
            return new CompoundTag();
        }
    }

    public static boolean writeCompressed(CompoundTag tag, File file) {
        boolean result = false;
        try {
            NbtIo.writeCompressed(tag, file.toPath());
            result = true;
        } catch (Exception e) {
            LOGGER.error("Failed to write compressed file: {}", file.getAbsolutePath(), e);
        }
        return result;
    }

    public static boolean writeCompressed(CompoundTag tag, OutputStream stream) {
        boolean result = false;
        try {
            NbtIo.writeCompressed(tag, stream);
            result = true;
        } catch (Exception e) {
            LOGGER.error("Failed to write compressed stream", e);
        }
        return result;
    }

    // endregion nbt文件读写

    // region 杂项

    public static String getPlayerLanguage(Player player) {
        try {
            return NarcissusUtils.getValidLanguage(player, CustomConfig.getPlayerLanguage(getPlayerUUIDString(player)));
        } catch (IllegalArgumentException i) {
            return ServerConfig.DEFAULT_LANGUAGE.get();
        }
    }

    public static String getValidLanguage(@Nullable Player player, @Nullable String language) {
        String result;
        if (StringUtils.isNullOrEmptyEx(language) || "client".equalsIgnoreCase(language)) {
            if (player instanceof ServerPlayer) {
                result = NarcissusUtils.getServerPlayerLanguage((ServerPlayer) player);
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

    public static String getServerPlayerLanguage(ServerPlayer player) {
        return PlayerLanguageManager.get(player);
    }

    /**
     * 复制玩家语言设置
     *
     * @param originalPlayer 原始玩家
     * @param targetPlayer   目标玩家
     */
    public static void clonePlayerLanguage(ServerPlayer originalPlayer, ServerPlayer targetPlayer) {
        ((ServerPlayerAccessor) targetPlayer).narcissus$language(((ServerPlayerAccessor) originalPlayer).narcissus$language());
    }

    public static String getClientLanguage() {
        return Minecraft.getInstance().getLanguageManager().getSelected();
    }

    public static String getPlayerName(@NonNull Player player) {
        return player.getDisplayName().getString();
    }

    public static String getPlayerUUIDString(@NonNull Player player) {
        return player.getUUID().toString();
    }

    public static String getPlayerNameByUUIDString(String uuid) {
        return UsernameCache.getMap().getOrDefault(UUID.fromString(uuid), "UnknownPlayer");
    }

    public static final ResourceKey<DamageType> MOD_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.create("mod"));

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
            return BlockStateParser.parseForBlock(getServerLevel().holderLookup(Registries.BLOCK), new StringReader(block), false).blockState();
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
        Optional<ResourceKey<Block>> key = block.defaultBlockState().getBlockHolder().unwrapKey();
        return key.map(blockResourceKey -> blockResourceKey.location().toString()).orElse("");
    }

    public static Block getBlockFromRegistryName(String location) {
        return ForgeRegistries.BLOCKS.getValue(Identifier.parse(location));
    }

    /**
     * 判断玩家是否被任何敌对生物锁定为攻击目标
     */
    public static boolean isTargetedByHostile(ServerPlayer player) {
        return player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox()
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
