package xin.vanilla.narcissus.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.UsernameCache;
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
import xin.vanilla.narcissus.network.ModNetworkHandler;

import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
            case VIRTUAL_OP -> prefix + " " + CommonConfig.COMMAND_VIRTUAL_OP.get();
            case VIRTUAL_OP_CONCISE -> isConciseEnabled(type) ? CommonConfig.COMMAND_VIRTUAL_OP.get() : "";
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
    public static boolean executeCommand(@NonNull ServerPlayer player, @NonNull String command) {
        AtomicBoolean result = new AtomicBoolean(false);
        try {
            player.level().getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack()
                            .withCallback((success, r) -> result.set(success && r > 0))
                    , command
            );
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: {}", command, e);
        }
        return result.get();
    }

    // endregion 指令相关

    // region 安全坐标

    public static ServerLevel getServerLevel() {
        return NarcissusFarewell.getServerInstance().getAllLevels().iterator().next();
    }

    public static int getWorldMinY(Level world) {
        return world.getMinY();
    }

    public static int getWorldMaxY(Level world) {
        return world.getMaxY();
    }

    public static Coordinate findTopCandidate(ServerLevel world, Coordinate start, Player player) {
        if (start.getY() >= NarcissusUtils.getWorldMaxY(world)) return null;
        SafeBlockChecker checker = new SafeBlockChecker(world, player);
        for (int y : IntStream.range(start.getYInt() + 1, NarcissusUtils.getWorldMaxY(world) + 1).boxed()
                .sorted(Comparator.comparingInt(Integer::intValue).reversed())
                .toList()) {
            if (checker.isSafeBlock(start.setY(y).toBlockPos(), false)) {
                return start.setY(y);
            }
        }
        return null;
    }

    public static Coordinate findBottomCandidate(ServerLevel world, Coordinate start, Player player) {
        if (start.getY() <= NarcissusUtils.getWorldMinY(world)) return null;
        SafeBlockChecker checker = new SafeBlockChecker(world, player);
        for (int y : IntStream.range(NarcissusUtils.getWorldMinY(world), start.getYInt()).boxed()
                .sorted(Comparator.comparingInt(Integer::intValue))
                .toList()) {
            if (checker.isSafeBlock(start.setY(y).toBlockPos(), false)) {
                return start.setY(y);
            }
        }
        return null;
    }

    public static Coordinate findUpCandidate(ServerLevel world, Coordinate start, Player player) {
        if (start.getY() >= NarcissusUtils.getWorldMaxY(world)) return null;
        SafeBlockChecker checker = new SafeBlockChecker(world, player);
        for (int y : IntStream.range(start.getYInt() + 1, NarcissusUtils.getWorldMaxY(world) + 1).boxed()
                .sorted(Comparator.comparingInt(Integer::intValue))
                .toList()) {
            if (checker.isSafeBlock(start.setY(y).toBlockPos(), false)) {
                return start.setY(y);
            }
        }
        return null;
    }

    public static Coordinate findDownCandidate(ServerLevel world, Coordinate start, Player player) {
        if (start.getY() <= NarcissusUtils.getWorldMinY(world)) return null;
        SafeBlockChecker checker = new SafeBlockChecker(world, player);
        for (int y : IntStream.range(NarcissusUtils.getWorldMinY(world), start.getYInt()).boxed()
                .sorted(Comparator.comparingInt(Integer::intValue).reversed())
                .toList()) {
            if (checker.isSafeBlock(start.setY(y).toBlockPos(), false)) {
                return start.setY(y);
            }
        }
        return null;
    }

    public static Coordinate findViewEndCandidate(ServerPlayer player, boolean safe, int range) {
        double stepScale = 0.75;
        Coordinate start = new Coordinate(player);
        Coordinate result = null;

        // 获取玩家的起始位置
        Vec3 startPosition = player.getEyePosition(1.0F);

        // 获取玩家的视线方向
        Vec3 direction = player.getViewVector(1.0F).normalize();
        // 步长
        Vec3 stepVector = direction.scale(stepScale);

        // 初始化变量
        Vec3 currentPosition = startPosition;
        Level world = player.level();

        // 从近到远寻找碰撞点
        for (int stepCount = 0; stepCount <= range; stepCount++) {
            // 更新当前检测位置
            currentPosition = startPosition.add(stepVector.scale(stepCount));
            BlockPos currentBlockPos = new BlockPos((int) currentPosition.x, (int) currentPosition.y, (int) currentPosition.z);

            // 获取当前方块状态
            BlockState blockState = world.getBlockState(currentBlockPos);

            // 检测方块是否不可穿过
            if (blockState.blocksMotion()) {
                // if (blockState.isCollisionShapeFullBlock(player.level(), currentBlockPos)) {
                result = start.clone().fromVec3(startPosition.add(stepVector.scale(stepCount - 1)));
                break;
            }
        }

        // 若未找到碰撞点，则使用射线的终点
        if (result == null) {
            result = start.clone().fromVec3(currentPosition);
        }

        // 若需寻找安全坐标，则从碰撞点反向查找安全位置
        if (safe) {
            SafeBlockChecker checker = new SafeBlockChecker(world, player);
            // 碰撞点的三维向量
            Vec3 collisionVector = result.toVec3();
            for (int stepCount = (int) Math.ceil(collisionVector.distanceTo(startPosition) / stepScale); stepCount >= 0; stepCount--) {
                currentPosition = startPosition.add(stepVector.scale(stepCount));
                BlockPos currentBlockPos = new BlockPos((int) currentPosition.x, (int) currentPosition.y, (int) currentPosition.z);
                for (int yOffset = -3; yOffset < 3; yOffset++) {
                    // 判断当前候选坐标是否安全
                    if (checker.isSafeBlock(currentBlockPos.above(yOffset), false)) {
                        result = start.clone().fromBlockPos(currentBlockPos).addY(yOffset).addX(0.5).addY(0.15).addZ(0.5);
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

    public static Coordinate findSafeCoordinate(Coordinate coordinate, Player player, boolean belowAllowAir) {
        Level world = getWorld(coordinate.getDimension());

        int chunkX = (int) coordinate.getX() >> 4;
        int chunkZ = (int) coordinate.getZ() >> 4;

        return searchForSafeCoordinateInChunk(world, coordinate, player, chunkX, chunkZ, belowAllowAir);
    }

    private static int deterministicHash(Coordinate c) {
        int prime = 31;
        int hash = 1;
        hash = prime * hash + Integer.hashCode(c.getXInt());
        hash = prime * hash + Integer.hashCode(c.getYInt());
        hash = prime * hash + Integer.hashCode(c.getZInt());
        return hash;
    }

    private static Coordinate searchForSafeCoordinateInChunk(Level world, Coordinate coordinate, Player player, int chunkX, int chunkZ, boolean belowAllowAir) {
        // 搜索安全位置，限制在目标范围区块内
        int offset = (ServerConfig.SAFE_CHUNK_RANGE.get() - 1) * 16;
        int chunkMinX = (chunkX << 4) - offset;
        int chunkMinZ = (chunkZ << 4) - offset;
        int chunkMaxX = chunkMinX + 15 + offset;
        int chunkMaxZ = chunkMinZ + 15 + offset;

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
        if (coordinate.getSafeMode() == EnumSafeMode.Y_C_TO_T) {
            IntStream.range(coordinate.getYInt(), NarcissusUtils.getWorldMaxY(world) + 1)
                    .forEach(y -> coordinates.add(new Coordinate(coordinate.getX(), y, coordinate.getZ())));
        } else if (coordinate.getSafeMode() == EnumSafeMode.Y_B_TO_C) {
            IntStream.range(NarcissusUtils.getWorldMinY(world), coordinate.getYInt() + 1)
                    .forEach(y -> coordinates.add(new Coordinate(coordinate.getX(), y, coordinate.getZ())));
        } else if (coordinate.getSafeMode() == EnumSafeMode.Y_C_TO_B) {
            IntStream.range(NarcissusUtils.getWorldMinY(world), coordinate.getYInt() + 1).boxed()
                    .sorted(Comparator.comparingInt(Integer::intValue).reversed())
                    .forEach(y -> coordinates.add(new Coordinate(coordinate.getX(), y, coordinate.getZ())));
        } else if (coordinate.getSafeMode() == EnumSafeMode.Y_T_TO_C) {
            IntStream.range(coordinate.getYInt(), NarcissusUtils.getWorldMaxY(world) + 1).boxed()
                    .sorted(Comparator.comparingInt(Integer::intValue).reversed())
                    .forEach(y -> coordinates.add(new Coordinate(coordinate.getX(), y, coordinate.getZ())));
        } else if (coordinate.getSafeMode() == EnumSafeMode.Y_C_OFFSET_3) {
            IntStream.range(coordinate.getYInt() - 3, coordinate.getYInt() + 3)
                    .forEach(y -> coordinates.add(new Coordinate(coordinate.getX(), y, coordinate.getZ())));
        } else {
            IntStream.range(chunkMinX, chunkMaxX)
                    .forEach(x -> IntStream.range(chunkMinZ, chunkMaxZ)
                            .forEach(z -> IntStream.range(NarcissusUtils.getWorldMinY(world), NarcissusUtils.getWorldMaxY(world) + 1)
                                    .forEach(y -> coordinates.add(new Coordinate(x, y, z)))
                            )
                    );
        }
        LOGGER.debug("TimeMillis before sorting: {}", System.currentTimeMillis());
        List<BlockPos> list = coordinates.stream().sorted(comparator).map(Coordinate::toBlockPos).collect(Collectors.toList());
        LOGGER.debug("TimeMillis before searching: {}", System.currentTimeMillis());
        Coordinate result = findSafeCoordinate(world, player, belowAllowAir, list);
        LOGGER.debug("TimeMillis after searching: {}", System.currentTimeMillis());
        LOGGER.debug("Target:{} | Safe:{}", coordinate.toXyzIntString(), result == null ? "null" : result.toXyzIntString());
        return result == null ? coordinate : result.addX(0.5).addY(0.15).addZ(0.5);
    }

    private static Coordinate findSafeCoordinate(Level world, Player player, boolean belowAllowAir, List<BlockPos> list) {
        if (list.isEmpty()) return new Coordinate();
        SafeBlockChecker checker = new SafeBlockChecker(world, player);
        for (BlockPos pos : list) {
            if (checker.isSafeBlock(pos, belowAllowAir)) {
                return new Coordinate().fromBlockPos(pos).setDimension(world.dimension());
            }
        }
        return null;
    }

    // endregion 安全坐标

    // region 坐标查找

    /**
     * 获取指定维度的世界实例
     */
    public static ServerLevel getWorld(ResourceKey<Level> dimension) {
        return NarcissusFarewell.getServerInstance().getLevel(dimension);
    }

    public static ResourceKey<Biome> getBiome(String id) {
        return getBiome(NarcissusFarewell.parseResource(id));
    }

    public static ResourceKey<Biome> getBiome(@NonNull ResourceLocation id) {
        ResourceKey<Biome> key = ResourceKey.create(ForgeRegistries.Keys.BIOMES, id);
        return ForgeRegistries.BIOMES.getKeys().stream().anyMatch(id::equals) ? key : null;
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
    public static Coordinate findNearestBiome(ServerLevel world, Coordinate start, ResourceKey<Biome> biome, int radius, int minDistance) {
        Pair<BlockPos, Holder<Biome>> nearestBiome = world.findClosestBiome3d(holder -> holder.is(biome), start.toBlockPos(), radius, minDistance, 64);
        if (nearestBiome != null) {
            BlockPos pos = nearestBiome.getFirst();
            if (pos != null) {
                return start.clone().setX(pos.getX()).setZ(pos.getZ()).setSafe(true);
            }
        }
        return null;
    }

    public static ResourceKey<Structure> getStructure(String id) {
        return getStructure(NarcissusFarewell.parseResource(id));
    }

    public static ResourceKey<Structure> getStructure(ResourceLocation id) {
        Map.Entry<ResourceKey<Structure>, Structure> mapEntry = NarcissusFarewell.getServerInstance().registryAccess()
                .lookupOrThrow(Registries.STRUCTURE).entrySet().stream()
                .filter(entry -> entry.getKey().location().equals(id))
                .findFirst().orElse(null);
        return mapEntry != null ? mapEntry.getKey() : null;
    }

    public static TagKey<Structure> getStructureTag(String id) {
        return getStructureTag(ResourceLocation.parse(id));
    }

    public static TagKey<Structure> getStructureTag(ResourceLocation id) {
        return NarcissusFarewell.getServerInstance().registryAccess()
                .lookupOrThrow(Registries.STRUCTURE).getTags()
                .filter(tag -> tag.key().location().equals(id))
                .map(HolderSet.Named::key)
                .findFirst().orElse(null);
    }

    /**
     * 获取指定范围内某个生物群系位置
     *
     * @param world  世界
     * @param start  开始位置
     * @param struct 目标结构
     * @param radius 搜索半径
     */
    public static Coordinate findNearestStruct(ServerLevel world, Coordinate start, ResourceKey<Structure> struct, int radius) {
        Registry<Structure> registry = world.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Either<ResourceKey<Structure>, TagKey<Structure>> left = Either.left(struct);
        HolderSet.ListBacked<Structure> holderSet = (HolderSet.ListBacked<Structure>) left.map((resourceKey) -> registry.get(resourceKey).map(HolderSet::direct).get(), registry::get);
        if (holderSet != null) {
            Pair<BlockPos, Holder<Structure>> pair = world.getChunkSource().getGenerator().findNearestMapStructure(world, holderSet, start.toBlockPos(), radius, true);
            if (pair != null) {
                BlockPos pos = pair.getFirst();
                if (pos != null) {
                    return start.clone().setX(pos.getX()).setZ(pos.getZ()).setSafe(true);
                }
            }
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
    public static Coordinate findNearestStruct(ServerLevel world, Coordinate start, TagKey<Structure> struct, int radius) {
        BlockPos pos = world.findNearestMapStructure(struct, start.toBlockPos(), radius, true);
        if (pos != null) {
            return start.clone().setX(pos.getX()).setZ(pos.getZ()).setSafe(true);
        }
        return null;
    }

    public static String getHomeDimensionByName(ServerPlayer player, String name) {
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        List<KeyValue<String, String>> list = data.getHomeCoordinate().keySet().stream()
                .filter(key -> key.getValue().equals(name))
                .toList();
        if (list.size() == 1) {
            return list.get(0).getKey();
        }
        return null;
    }

    public static KeyValue<String, String> getPlayerHomeKey(ServerPlayer player, ResourceKey<Level> dimension, String name) {
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        Map<String, String> defaultHome = data.getDefaultHome();
        if (defaultHome.isEmpty() && dimension == null && StringUtils.isNullOrEmpty(name) && data.getHomeCoordinate().size() != 1) {
            return null;
        }
        KeyValue<String, String> keyValue = null;
        if (dimension == null && StringUtils.isNotNullOrEmpty(name)) {
            if (defaultHome.isEmpty() || !defaultHome.containsValue(name)) {
                keyValue = data.getHomeCoordinate().keySet().stream()
                        .filter(key -> key.getValue().equals(name))
                        .filter(key -> key.getKey().equals(player.level().dimension().location().toString()))
                        .findFirst().orElse(null);
            } else if (defaultHome.containsValue(name)) {
                List<Map.Entry<String, String>> entryList = defaultHome.entrySet().stream().filter(entry -> entry.getValue().equals(name)).toList();
                if (entryList.size() == 1) {
                    keyValue = new KeyValue<>(entryList.get(0).getKey(), entryList.get(0).getValue());
                }
            }
        } else if (dimension != null && StringUtils.isNullOrEmpty(name)) {
            if (defaultHome.containsKey(dimension.location().toString())) {
                keyValue = new KeyValue<>(dimension.location().toString(), defaultHome.get(dimension.location().toString()));
            }
        } else if (dimension != null && StringUtils.isNotNullOrEmpty(name)) {
            keyValue = data.getHomeCoordinate().keySet().stream()
                    .filter(key -> key.getValue().equals(name))
                    .filter(key -> key.getKey().equals(dimension.location().toString()))
                    .findFirst().orElse(null);
        } else if (!defaultHome.isEmpty() && dimension == null && StringUtils.isNullOrEmpty(name)) {
            if (defaultHome.size() == 1) {
                keyValue = new KeyValue<>(defaultHome.keySet().iterator().next(), defaultHome.values().iterator().next());
            } else {
                String value = defaultHome.getOrDefault(player.level().dimension().location().toString(), null);
                if (value != null) {
                    keyValue = new KeyValue<>(player.level().dimension().location().toString(), value);
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
    public static Coordinate getPlayerHome(ServerPlayer player, ResourceKey<Level> dimension, String name) {
        return PlayerTeleportData.getData(player).getHomeCoordinate().getOrDefault(getPlayerHomeKey(player, dimension, name), null);
    }

    public static String getStageDimensionByName(String name) {
        WorldStageData stageData = WorldStageData.get();
        List<KeyValue<String, String>> list = stageData.getStageCoordinate().keySet().stream()
                .filter(key -> key.getValue().equals(name))
                .toList();
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
    public static KeyValue<String, String> findNearestStageKey(ServerPlayer player) {
        WorldStageData stageData = WorldStageData.get();
        Map.Entry<KeyValue<String, String>, Coordinate> stageEntry = stageData.getStageCoordinate().entrySet().stream()
                .filter(entry -> entry.getKey().getKey().equals(player.level().dimension().location().toString()))
                .min(Comparator.comparingInt(entry -> {
                    Coordinate value = entry.getValue();
                    double dx = value.getX() - player.getX();
                    double dy = value.getY() - player.getY();
                    double dz = value.getZ() - player.getZ();
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
                .filter(record -> dimension == null || record.getBefore().getDimension().equals(dimension))
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
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "range_too_large"), maxRange);
        } else if (range <= 0) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "range_too_small"), 1);
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
    public static void teleportTo(@NonNull ServerPlayer player, @NonNull Coordinate after, EnumTeleportType type) {
        Coordinate before = new Coordinate(player);
        Level world = player.level();
        MinecraftServer server = player.level().getServer();
        // 别听Idea的
        if (world != null && server != null) {
            ServerLevel level = server.getLevel(after.getDimension());
            if (level != null) {
                if (after.isSafe()) {
                    // 异步的代价就是粪吗
                    NarcissusUtils.sendActionBarMessage(player, Component.translatable(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.MESSAGE, "safe_searching"));
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
                        player.level().getServer().submit(() -> {
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
        ResourceLocation sound = NarcissusFarewell.parseResource(ServerConfig.TP_SOUND.get());
        NarcissusUtils.playSound(player, sound, 1.0f, 1.0f);
        after.setY(Math.floor(after.getY()) + 0.1);

        // 传送跟随者
        teleportFollowers(player, after, level);
        // 传送载体与乘客
        Entity vehicle = teleportPassengers(player, null, player.getRootVehicle(), after, level);
        // 传送玩家
        doTeleport(player, after, level);
        // 使玩家重新坐上载体
        if (vehicle != null) {
            player.startRiding(vehicle, true, false);
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
                entity.startRiding(passenger, true, false);
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
            if (entity.getOwner() != null && entity.getOwner().getUUID().equals(player.getUUID()) && !entity.isOrderedToSit()) {
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

            if (entity.goalSelector.getAvailableGoals().stream()
                    .anyMatch(goal -> goal.isRunning()
                            && (goal.getGoal() instanceof TemptGoal)
                            && FieldUtils.getPrivateFieldValue(TemptGoal.class, goal.getGoal(), FieldUtils.getTemptGoalPlayerFieldName()) == player
                    )) {
                doTeleport(entity, coordinate, level);
            }
        }
    }

    private static Entity doTeleport(@NonNull Entity entity, @NonNull Coordinate coordinate, ServerLevel level) {
        if (entity instanceof ServerPlayer player) {
            player.teleportTo(level, coordinate.getX(), coordinate.getY(), coordinate.getZ(), Set.of()
                    , coordinate.getYaw() == 0 ? player.getYRot() : (float) coordinate.getYaw()
                    , coordinate.getPitch() == 0 ? player.getXRot() : (float) coordinate.getPitch(), true);
        } else {
            if (level == entity.level()) {
                entity.teleportTo(coordinate.getX(), coordinate.getY(), coordinate.getZ());
            } else {
                entity = entity.teleport(
                        new TeleportTransition(level
                                , coordinate.toVec3()
                                , entity.getDeltaMovement()
                                , entity.getYRot()
                                , entity.getXRot()
                                , TeleportTransition.DO_NOTHING)
                );
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
            if (ItemStack.matches(stack, copy)) {
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
            result.addAll(player.getInventory().getNonEquipmentItems());
            for (EquipmentSlot slot : Inventory.EQUIPMENT_SLOT_MAPPING.values()) {
                result.add(player.getInventory().getEquipment().get((slot)));
            }
            result.add(player.getOffhandItem());
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
        for (ServerPlayer player : source.level().getServer().getPlayerList().getPlayers()) {
            sendMessage(player, Component.literal("[%s] %s")
                    .appendArg(getPlayerName(player))
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
        player.sendSystemMessage(Component.translatable(key, args).setLanguageCode(NarcissusUtils.getPlayerLanguage(player)).toChatComponent(), false);
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
            source.sendSuccess(() -> Component.translatable(key, args).setLanguageCode(ServerConfig.DEFAULT_LANGUAGE.get()).toChatComponent(), false);
        } else {
            source.sendFailure(Component.translatable(key, args).setLanguageCode(ServerConfig.DEFAULT_LANGUAGE.get()).toChatComponent());
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

    // endregion 消息相关

    // region 跨维度传送

    public static boolean isTeleportAcrossDimensionEnabled(ServerPlayer player, ResourceKey<Level> to, EnumTeleportType type) {
        boolean result = true;
        if (player.level().dimension() != to) {
            if (ServerConfig.TELEPORT_ACROSS_DIMENSION.get()) {
                if (!NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, type)) {
                    result = false;
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "across_dimension_not_enable_for"), getCommand(type));
                }
            } else {
                result = false;
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "across_dimension_not_enable"));
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

        Map<String, Double> vars = new HashMap<>();
        vars.put("distance", adjustedDistance);
        vars.put("num", (double) teleportCost.getNum());
        vars.put("rate", teleportCost.getRate());

        double need;
        try {
            need = new SafeExpressionEvaluator(teleportCost.getExp()).evaluate(vars);
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
                    , I18nUtils.getKey(EnumI18nType.MESSAGE, "cost_not_enough")
                    , Component.translatable(NarcissusUtils.getPlayerLanguage(player)
                            , EnumI18nType.WORD, "teleport_card")
                    , cardNeed
            );
        }

        switch (teleportCost.getType()) {
            case EXP_POINT:
                result = player.totalExperience >= costNeed;
                if (!result) {
                    NarcissusUtils.sendTranslatableMessage(player
                            , I18nUtils.getKey(EnumI18nType.MESSAGE, "cost_not_enough")
                            , Component.translatable(NarcissusUtils.getPlayerLanguage(player)
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
                            , I18nUtils.getKey(EnumI18nType.MESSAGE, "cost_not_enough")
                            , Component.translatable(NarcissusUtils.getPlayerLanguage(player)
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
                            , I18nUtils.getKey(EnumI18nType.MESSAGE, "cost_not_enough")
                            , Component.translatable(NarcissusUtils.getPlayerLanguage(player)
                                    , EnumI18nType.WORD, "health")
                            , costNeed
                    );
                } else if (submit) {
                    try {
                        EntityDataAccessor<? super Float> DATA_HEALTH_ID = (EntityDataAccessor<? super Float>) FieldUtils.getPrivateFieldValue(LivingEntity.class, null, FieldUtils.getEntityHealthFieldName());
                        player.getEntityData().set(DATA_HEALTH_ID, player.getHealth() - costNeed);
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
                            , I18nUtils.getKey(EnumI18nType.MESSAGE, "cost_not_enough")
                            , Component.translatable(NarcissusUtils.getPlayerLanguage(player)
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
                    ItemParser.ItemResult itemResult = new ItemParser(player.registryAccess()).parse(new StringReader(teleportCost.getConf()));
                    ItemStack itemStack = new ItemInput(itemResult.item(), itemResult.components()).createItemStack(1, false);
                    result = getItemCount(getPlayerItemList(player), itemStack) >= costNeed;
                    itemStack.setCount(costNeed);
                    if (!result) {
                        NarcissusUtils.sendMessage(player
                                , Component.translatable(EnumI18nType.MESSAGE, "cost_not_enough"
                                        , Component.literal(NarcissusUtils.getItemName(itemStack))
                                                .setHoverEvent(new HoverEvent.ShowItem(itemStack))
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
                                    , Component.translatable(EnumI18nType.MESSAGE, "cost_not_enough"
                                            , Component.literal(NarcissusUtils.getItemName(itemStack))
                                                    .setHoverEvent(new HoverEvent.ShowItem(itemStack))
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
        return player.getLanguage();
    }

    /**
     * 复制玩家语言设置
     *
     * @param originalPlayer 原始玩家
     * @param targetPlayer   目标玩家
     */
    public static void clonePlayerLanguage(ServerPlayer originalPlayer, ServerPlayer targetPlayer) {
        FieldUtils.setPrivateFieldValue(ServerPlayer.class, targetPlayer, FieldUtils.getPlayerLanguageFieldName(originalPlayer), getServerPlayerLanguage(originalPlayer));
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

    /**
     * 强行使玩家死亡
     */
    @SuppressWarnings("unchecked")
    public static boolean killPlayer(ServerPlayer player) {
        try {
            if (player.isSleeping() && !player.level().isClientSide()) {
                player.stopSleeping();
            }
            player.getEntityData().set((EntityDataAccessor<? super Float>) FieldUtils.getPrivateFieldValue(LivingEntity.class, null, FieldUtils.getEntityHealthFieldName()), 0f);
            player.connection.send(new ClientboundPlayerCombatKillPacket(player.getId(), CommonComponents.EMPTY));
            if (!player.isSpectator()) {
                if (!player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
                    player.getInventory().dropAll();
                }
            }
            player.level().broadcastEntityEvent(player, (byte) 3);
            player.awardStat(Stats.DEATHS);
            player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
            player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
            player.clearFire();
            player.getCombatTracker().recheckStatus();
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
        return ForgeRegistries.BLOCKS.getValue(NarcissusFarewell.parseResource(location));
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

    // endregion 杂项
}
