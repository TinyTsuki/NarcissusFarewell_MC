package xin.vanilla.narcissus.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import lombok.NonNull;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.capability.TeleportRecord;
import xin.vanilla.narcissus.capability.player.IPlayerTeleportData;
import xin.vanilla.narcissus.capability.player.PlayerTeleportDataCapability;
import xin.vanilla.narcissus.capability.world.WorldStageData;
import xin.vanilla.narcissus.config.*;
import xin.vanilla.narcissus.enums.*;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class NarcissusUtils {

    private static final Logger LOGGER = LogManager.getLogger();

    // region 指令相关

    public static String getCommandPrefix() {
        String commandPrefix = ServerConfig.COMMAND_PREFIX.get();
        if (StringUtils.isNullOrEmptyEx(commandPrefix) || !commandPrefix.matches("^(\\w ?)+$")) {
            ServerConfig.COMMAND_PREFIX.set(NarcissusFarewell.DEFAULT_COMMAND_PREFIX);
        }
        return ServerConfig.COMMAND_PREFIX.get().trim();
    }

    /**
     * 判断指令类型是否开启
     *
     * @param type 指令类型
     */
    public static boolean isCommandEnabled(ECommandType type) {
        return switch (type) {
            case FEED, FEED_OTHER, FEED_CONCISE, FEED_OTHER_CONCISE -> ServerConfig.SWITCH_FEED.get();
            case TP_COORDINATE, TP_COORDINATE_CONCISE -> ServerConfig.SWITCH_TP_COORDINATE.get();
            case TP_STRUCTURE, TP_STRUCTURE_CONCISE -> ServerConfig.SWITCH_TP_STRUCTURE.get();
            case TP_ASK, TP_ASK_YES, TP_ASK_NO, TP_ASK_CONCISE, TP_ASK_YES_CONCISE, TP_ASK_NO_CONCISE ->
                    ServerConfig.SWITCH_TP_ASK.get();
            case TP_HERE, TP_HERE_YES, TP_HERE_NO, TP_HERE_CONCISE, TP_HERE_YES_CONCISE, TP_HERE_NO_CONCISE ->
                    ServerConfig.SWITCH_TP_HERE.get();
            case TP_RANDOM, TP_RANDOM_CONCISE -> ServerConfig.SWITCH_TP_RANDOM.get();
            case TP_SPAWN, TP_SPAWN_OTHER, TP_SPAWN_CONCISE, TP_SPAWN_OTHER_CONCISE ->
                    ServerConfig.SWITCH_TP_SPAWN.get();
            case TP_WORLD_SPAWN, TP_WORLD_SPAWN_CONCISE -> ServerConfig.SWITCH_TP_WORLD_SPAWN.get();
            case TP_TOP, TP_TOP_CONCISE -> ServerConfig.SWITCH_TP_TOP.get();
            case TP_BOTTOM, TP_BOTTOM_CONCISE -> ServerConfig.SWITCH_TP_BOTTOM.get();
            case TP_UP, TP_UP_CONCISE -> ServerConfig.SWITCH_TP_UP.get();
            case TP_DOWN, TP_DOWN_CONCISE -> ServerConfig.SWITCH_TP_DOWN.get();
            case TP_VIEW, TP_VIEW_CONCISE -> ServerConfig.SWITCH_TP_VIEW.get();
            case TP_HOME, SET_HOME, DEL_HOME, GET_HOME, TP_HOME_CONCISE, SET_HOME_CONCISE, DEL_HOME_CONCISE,
                 GET_HOME_CONCISE -> ServerConfig.SWITCH_TP_HOME.get();
            case TP_STAGE, SET_STAGE, DEL_STAGE, GET_STAGE, TP_STAGE_CONCISE, SET_STAGE_CONCISE, DEL_STAGE_CONCISE,
                 GET_STAGE_CONCISE -> ServerConfig.SWITCH_TP_STAGE.get();
            case TP_BACK, TP_BACK_CONCISE -> ServerConfig.SWITCH_TP_BACK.get();
            default -> true;
        };
    }

    public static String getCommand(ETeleportType type) {
        return switch (type) {
            case TP_COORDINATE -> ServerConfig.COMMAND_TP_COORDINATE.get();
            case TP_STRUCTURE -> ServerConfig.COMMAND_TP_STRUCTURE.get();
            case TP_ASK -> ServerConfig.COMMAND_TP_ASK.get();
            case TP_HERE -> ServerConfig.COMMAND_TP_HERE.get();
            case TP_RANDOM -> ServerConfig.COMMAND_TP_RANDOM.get();
            case TP_SPAWN -> ServerConfig.COMMAND_TP_SPAWN.get();
            case TP_WORLD_SPAWN -> ServerConfig.COMMAND_TP_WORLD_SPAWN.get();
            case TP_TOP -> ServerConfig.COMMAND_TP_TOP.get();
            case TP_BOTTOM -> ServerConfig.COMMAND_TP_BOTTOM.get();
            case TP_UP -> ServerConfig.COMMAND_TP_UP.get();
            case TP_DOWN -> ServerConfig.COMMAND_TP_DOWN.get();
            case TP_VIEW -> ServerConfig.COMMAND_TP_VIEW.get();
            case TP_HOME -> ServerConfig.COMMAND_TP_HOME.get();
            case TP_STAGE -> ServerConfig.COMMAND_TP_STAGE.get();
            case TP_BACK -> ServerConfig.COMMAND_TP_BACK.get();
            default -> "";
        };
    }

    public static String getCommand(ECommandType type) {
        String prefix = NarcissusUtils.getCommandPrefix();
        return switch (type) {
            case HELP -> prefix + " help";
            case DIMENSION -> prefix + " " + ServerConfig.COMMAND_DIMENSION.get();
            case DIMENSION_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_DIMENSION.get() : "";
            case UUID -> prefix + " " + ServerConfig.COMMAND_UUID.get();
            case UUID_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_UUID.get() : "";
            case FEED, FEED_OTHER -> prefix + " " + ServerConfig.COMMAND_FEED.get();
            case FEED_CONCISE, FEED_OTHER_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_FEED.get() : "";
            case TP_COORDINATE -> prefix + " " + ServerConfig.COMMAND_TP_COORDINATE.get();
            case TP_COORDINATE_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_COORDINATE.get() : "";
            case TP_STRUCTURE -> prefix + " " + ServerConfig.COMMAND_TP_STRUCTURE.get();
            case TP_STRUCTURE_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_STRUCTURE.get() : "";
            case TP_ASK -> prefix + " " + ServerConfig.COMMAND_TP_ASK.get();
            case TP_ASK_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK.get() : "";
            case TP_ASK_YES -> prefix + " " + ServerConfig.COMMAND_TP_ASK_YES.get();
            case TP_ASK_YES_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK_YES.get() : "";
            case TP_ASK_NO -> prefix + " " + ServerConfig.COMMAND_TP_ASK_NO.get();
            case TP_ASK_NO_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK_NO.get() : "";
            case TP_HERE -> prefix + " " + ServerConfig.COMMAND_TP_HERE.get();
            case TP_HERE_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE.get() : "";
            case TP_HERE_YES -> prefix + " " + ServerConfig.COMMAND_TP_HERE_YES.get();
            case TP_HERE_YES_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE_YES.get() : "";
            case TP_HERE_NO -> prefix + " " + ServerConfig.COMMAND_TP_HERE_NO.get();
            case TP_HERE_NO_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE_NO.get() : "";
            case TP_RANDOM -> prefix + " " + ServerConfig.COMMAND_TP_RANDOM.get();
            case TP_RANDOM_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_RANDOM.get() : "";
            case TP_SPAWN, TP_SPAWN_OTHER -> prefix + " " + ServerConfig.COMMAND_TP_SPAWN.get();
            case TP_SPAWN_CONCISE, TP_SPAWN_OTHER_CONCISE ->
                    isConciseEnabled(type) ? ServerConfig.COMMAND_TP_SPAWN.get() : "";
            case TP_WORLD_SPAWN -> prefix + " " + ServerConfig.COMMAND_TP_WORLD_SPAWN.get();
            case TP_WORLD_SPAWN_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_WORLD_SPAWN.get() : "";
            case TP_TOP -> prefix + " " + ServerConfig.COMMAND_TP_TOP.get();
            case TP_TOP_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_TOP.get() : "";
            case TP_BOTTOM -> prefix + " " + ServerConfig.COMMAND_TP_BOTTOM.get();
            case TP_BOTTOM_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_BOTTOM.get() : "";
            case TP_UP -> prefix + " " + ServerConfig.COMMAND_TP_UP.get();
            case TP_UP_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_UP.get() : "";
            case TP_DOWN -> prefix + " " + ServerConfig.COMMAND_TP_DOWN.get();
            case TP_DOWN_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_DOWN.get() : "";
            case TP_VIEW -> prefix + " " + ServerConfig.COMMAND_TP_VIEW.get();
            case TP_VIEW_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_VIEW.get() : "";
            case TP_HOME -> prefix + " " + ServerConfig.COMMAND_TP_HOME.get();
            case TP_HOME_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HOME.get() : "";
            case SET_HOME -> prefix + " " + ServerConfig.COMMAND_SET_HOME.get();
            case SET_HOME_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_SET_HOME.get() : "";
            case DEL_HOME -> prefix + " " + ServerConfig.COMMAND_DEL_HOME.get();
            case DEL_HOME_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_DEL_HOME.get() : "";
            case GET_HOME -> prefix + " " + ServerConfig.COMMAND_GET_HOME.get();
            case GET_HOME_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_GET_HOME.get() : "";
            case TP_STAGE -> prefix + " " + ServerConfig.COMMAND_TP_STAGE.get();
            case TP_STAGE_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_STAGE.get() : "";
            case SET_STAGE -> prefix + " " + ServerConfig.COMMAND_SET_STAGE.get();
            case SET_STAGE_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_SET_STAGE.get() : "";
            case DEL_STAGE -> prefix + " " + ServerConfig.COMMAND_DEL_STAGE.get();
            case DEL_STAGE_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_DEL_STAGE.get() : "";
            case GET_STAGE -> prefix + " " + ServerConfig.COMMAND_GET_STAGE.get();
            case GET_STAGE_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_GET_STAGE.get() : "";
            case TP_BACK -> prefix + " " + ServerConfig.COMMAND_TP_BACK.get();
            case TP_BACK_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_TP_BACK.get() : "";
            case VIRTUAL_OP -> prefix + " " + ServerConfig.COMMAND_VIRTUAL_OP.get();
            case VIRTUAL_OP_CONCISE -> isConciseEnabled(type) ? ServerConfig.COMMAND_VIRTUAL_OP.get() : "";
            default -> "";
        };
    }

    public static int getCommandPermissionLevel(ECommandType type) {
        return switch (type) {
            case FEED_OTHER, FEED_OTHER_CONCISE -> ServerConfig.PERMISSION_FEED_OTHER.get();
            case TP_COORDINATE, TP_COORDINATE_CONCISE -> ServerConfig.PERMISSION_TP_COORDINATE.get();
            case TP_STRUCTURE, TP_STRUCTURE_CONCISE -> ServerConfig.PERMISSION_TP_STRUCTURE.get();
            // case TP_ASK_YES:
            // case TP_ASK_NO:
            case TP_ASK, TP_ASK_CONCISE ->
                // case TP_ASK_YES_CONCISE:
                // case TP_ASK_NO_CONCISE:
                    ServerConfig.PERMISSION_TP_ASK.get();
            // case TP_HERE_YES:
            // case TP_HERE_NO:
            case TP_HERE, TP_HERE_CONCISE ->
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

    public static int getCommandPermissionLevel(ETeleportType type) {
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

    public static boolean isConciseEnabled(ECommandType type) {
        return switch (type) {
            case UUID, UUID_CONCISE -> ServerConfig.CONCISE_UUID.get();
            case DIMENSION, DIMENSION_CONCISE -> ServerConfig.CONCISE_DIMENSION.get();
            case FEED, FEED_OTHER, FEED_CONCISE, FEED_OTHER_CONCISE -> ServerConfig.CONCISE_FEED.get();
            case TP_COORDINATE, TP_COORDINATE_CONCISE -> ServerConfig.CONCISE_TP_COORDINATE.get();
            case TP_STRUCTURE, TP_STRUCTURE_CONCISE -> ServerConfig.CONCISE_TP_STRUCTURE.get();
            case TP_ASK, TP_ASK_CONCISE -> ServerConfig.CONCISE_TP_ASK.get();
            case TP_ASK_YES, TP_ASK_YES_CONCISE -> ServerConfig.CONCISE_TP_ASK_YES.get();
            case TP_ASK_NO, TP_ASK_NO_CONCISE -> ServerConfig.CONCISE_TP_ASK_NO.get();
            case TP_HERE, TP_HERE_CONCISE -> ServerConfig.CONCISE_TP_HERE.get();
            case TP_HERE_YES, TP_HERE_YES_CONCISE -> ServerConfig.CONCISE_TP_HERE_YES.get();
            case TP_HERE_NO, TP_HERE_NO_CONCISE -> ServerConfig.CONCISE_TP_HERE_NO.get();
            case TP_RANDOM, TP_RANDOM_CONCISE -> ServerConfig.CONCISE_TP_RANDOM.get();
            case TP_SPAWN, TP_SPAWN_OTHER, TP_SPAWN_CONCISE, TP_SPAWN_OTHER_CONCISE ->
                    ServerConfig.CONCISE_TP_SPAWN.get();
            case TP_WORLD_SPAWN, TP_WORLD_SPAWN_CONCISE -> ServerConfig.CONCISE_TP_WORLD_SPAWN.get();
            case TP_TOP, TP_TOP_CONCISE -> ServerConfig.CONCISE_TP_TOP.get();
            case TP_BOTTOM, TP_BOTTOM_CONCISE -> ServerConfig.CONCISE_TP_BOTTOM.get();
            case TP_UP, TP_UP_CONCISE -> ServerConfig.CONCISE_TP_UP.get();
            case TP_DOWN, TP_DOWN_CONCISE -> ServerConfig.CONCISE_TP_DOWN.get();
            case TP_VIEW, TP_VIEW_CONCISE -> ServerConfig.CONCISE_TP_VIEW.get();
            case TP_HOME, TP_HOME_CONCISE -> ServerConfig.CONCISE_TP_HOME.get();
            case SET_HOME, SET_HOME_CONCISE -> ServerConfig.CONCISE_SET_HOME.get();
            case DEL_HOME, DEL_HOME_CONCISE -> ServerConfig.CONCISE_DEL_HOME.get();
            case GET_HOME, GET_HOME_CONCISE -> ServerConfig.CONCISE_GET_HOME.get();
            case TP_STAGE, TP_STAGE_CONCISE -> ServerConfig.CONCISE_TP_STAGE.get();
            case SET_STAGE, SET_STAGE_CONCISE -> ServerConfig.CONCISE_SET_STAGE.get();
            case DEL_STAGE, DEL_STAGE_CONCISE -> ServerConfig.CONCISE_DEL_STAGE.get();
            case GET_STAGE, GET_STAGE_CONCISE -> ServerConfig.CONCISE_GET_STAGE.get();
            case TP_BACK, TP_BACK_CONCISE -> ServerConfig.CONCISE_TP_BACK.get();
            case VIRTUAL_OP, VIRTUAL_OP_CONCISE -> ServerConfig.CONCISE_VIRTUAL_OP.get();
            default -> false;
        };
    }

    public static boolean hasCommandPermission(CommandSourceStack source, ECommandType type) {
        return source.hasPermission(getCommandPermissionLevel(type)) || hasVirtualPermission(source.getEntity(), type);
    }

    public static boolean hasVirtualPermission(Entity source, ECommandType type) {
        // 若为玩家
        if (source instanceof Player) {
            return VirtualPermissionManager.getVirtualPermission((Player) source).stream()
                    .filter(Objects::nonNull)
                    .anyMatch(s -> s.replaceConcise() == type.replaceConcise());
        } else {
            return false;
        }
    }

    // endregion 指令相关

    // region 安全坐标

    /**
     * 安全的方块
     */
    private static final List<BlockState> SAFE_BLOCKS = ServerConfig.SAFE_BLOCKS.get().stream()
            .map(block -> {
                try {
                    return BlockStateParser.parseForBlock(getServerLevel().holderLookup(Registries.BLOCK), new StringReader(block), false).blockState();
                } catch (CommandSyntaxException e) {
                    LOGGER.error("Invalid unsafe block: {}", block, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    /**
     * 不安全的方块
     */
    private static final List<BlockState> UNSAFE_BLOCKS = ServerConfig.UNSAFE_BLOCKS.get().stream()
            .map(block -> {
                try {
                    return BlockStateParser.parseForBlock(getServerLevel().holderLookup(Registries.BLOCK), new StringReader(block), false).blockState();
                } catch (CommandSyntaxException e) {
                    LOGGER.error("Invalid unsafe block: {}", block, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    private static final List<BlockState> SUFFOCATING_BLOCKS = ServerConfig.SUFFOCATING_BLOCKS.get().stream()
            .map(block -> {
                try {
                    return BlockStateParser.parseForBlock(getServerLevel().holderLookup(Registries.BLOCK), new StringReader(block), false).blockState();
                } catch (CommandSyntaxException e) {
                    LOGGER.error("Invalid unsafe block: {}", block, e);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    public static ServerLevel getServerLevel() {
        return NarcissusFarewell.getServerInstance().getAllLevels().iterator().next();
    }

    public static Coordinate findTopCandidate(ServerLevel world, Coordinate start) {
        if (start.getY() >= world.getMaxY()) return null;
        for (int y : IntStream.range((int) start.getY() + 1, world.getMaxY()).boxed()
                .sorted(Comparator.comparingInt(Integer::intValue).reversed())
                .toList()) {
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

    public static Coordinate findBottomCandidate(ServerLevel world, Coordinate start) {
        if (start.getY() <= 0) return null;
        for (int y : IntStream.range(world.getMaxY(), (int) start.getY() - 1).boxed()
                .sorted(Comparator.comparingInt(Integer::intValue))
                .toList()) {
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

    public static Coordinate findUpCandidate(ServerLevel world, Coordinate start) {
        if (start.getY() >= world.getMaxY()) return null;
        for (int y : IntStream.range((int) start.getY() + 1, world.getMaxY()).boxed()
                .sorted(Comparator.comparingInt(a -> a - (int) start.getY()))
                .toList()) {
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

    public static Coordinate findDownCandidate(ServerLevel world, Coordinate start) {
        if (start.getY() <= 0) return null;
        for (int y : IntStream.range(world.getMaxY(), (int) start.getY() - 1).boxed()
                .sorted(Comparator.comparingInt(a -> (int) start.getY() - a))
                .toList()) {
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

        // 如果未找到碰撞点，则使用射线的终点
        if (result == null) {
            result = start.clone().fromVec3(currentPosition);
        }

        // 如果 safe 为 true，从碰撞点反向查找安全位置
        if (safe) {
            Vec3 collisionVector = result.toVec3(); // 碰撞点的三维向量
            for (int stepCount = (int) Math.ceil(collisionVector.distanceTo(startPosition) / stepScale); stepCount >= 0; stepCount--) {
                currentPosition = startPosition.add(stepVector.scale(stepCount));
                BlockPos currentBlockPos = new BlockPos((int) currentPosition.x, (int) currentPosition.y, (int) currentPosition.z);
                for (int yOffset = -3; yOffset < 3; yOffset++) {
                    Coordinate candidate = start.clone().fromBlockPos(currentBlockPos).addY(yOffset);
                    // 判断当前候选坐标是否安全
                    if (isSafeCoordinate(world, candidate)) {
                        result = candidate.addX(0.5).addY(0.15).addZ(0.5); // 找到安全位置
                        stepCount = 0; // 跳出循环
                        break;
                    }
                }
            }
        }
        // 如果起点与结果相同则返回null
        if (start.equalsOfRange(result, 1)) {
            result = null;
        }
        return result;
    }

    public static Coordinate findSafeCoordinate(Coordinate coordinate, boolean belowAllowAir) {
        Level world = getWorld(coordinate.getDimension());

        int chunkX = (int) coordinate.getX() >> 4;
        int chunkZ = (int) coordinate.getZ() >> 4;

        return searchForSafeCoordinateInChunk(world, coordinate, chunkX, chunkZ, belowAllowAir);
    }

    private static Coordinate searchForSafeCoordinateInChunk(Level world, Coordinate coordinate, int chunkX, int chunkZ, boolean belowAllowAir) {
        // 搜索安全位置，限制在目标范围区块内
        int offset = (ServerConfig.SAFE_CHUNK_RANGE.get() - 1) * 16;
        int chunkMinX = (chunkX << 4) - offset;
        int chunkMinZ = (chunkZ << 4) - offset;
        int chunkMaxX = chunkMinX + 15 + offset;
        int chunkMaxZ = chunkMinZ + 15 + offset;
        int minY = world.getMinY();
        int maxY = world.getMaxY();

        List<Integer> yList;
        List<Integer> xList;
        List<Integer> zList;
        if (coordinate.getSafeMode() == ESafeMode.Y_DOWN) {
            xList = new ArrayList<>() {{
                add((int) coordinate.getX());
            }};
            zList = new ArrayList<>() {{
                add((int) coordinate.getZ());
            }};
            yList = IntStream.range((int) coordinate.getY(), 0).boxed()
                    .sorted(Comparator.comparingInt(a -> (int) coordinate.getY() - a))
                    .collect(Collectors.toList());
        } else if (coordinate.getSafeMode() == ESafeMode.Y_UP) {
            xList = new ArrayList<>() {{
                add((int) coordinate.getX());
            }};
            zList = new ArrayList<>() {{
                add((int) coordinate.getZ());
            }};
            yList = IntStream.range((int) coordinate.getY(), maxY).boxed()
                    .sorted(Comparator.comparingInt(a -> a - (int) coordinate.getY()))
                    .collect(Collectors.toList());
        } else if (coordinate.getSafeMode() == ESafeMode.Y_OFFSET_3) {
            xList = new ArrayList<>() {{
                add((int) coordinate.getX());
            }};
            zList = new ArrayList<>() {{
                add((int) coordinate.getZ());
            }};
            yList = IntStream.range((int) (coordinate.getY() - 3), (int) (coordinate.getY() + 3)).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getY())))
                    .collect(Collectors.toList());
        } else {
            xList = IntStream.range(chunkMinX, chunkMaxX).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getX())))
                    .toList();
            zList = IntStream.range(chunkMinZ, chunkMaxZ).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getZ())))
                    .toList();
            yList = IntStream.range(minY, maxY).boxed()
                    .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) coordinate.getY())))
                    .collect(Collectors.toList());
        }
        for (int y : yList) {
            if (coordinate.getSafeMode() == ESafeMode.NONE && y <= minY || (y <= minY || y > maxY)) continue;
            for (int x : xList) {
                for (int z : zList) {
                    Coordinate candidate = new Coordinate().setX(x + 0.5).setY(y + 0.15).setZ(z + 0.5)
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

    private static boolean isAirCoordinate(Level world, Coordinate coordinate) {
        BlockState block = world.getBlockState(coordinate.toBlockPos());
        BlockState blockAbove = world.getBlockState(coordinate.toBlockPos().above());
        BlockState blockBelow = world.getBlockState(coordinate.toBlockPos().below());
        return (!block.blocksMotion() && !UNSAFE_BLOCKS.contains(block) && !UNSAFE_BLOCKS.contains(block.getBlock().defaultBlockState()))
                && (!blockAbove.blocksMotion() && !UNSAFE_BLOCKS.contains(blockAbove) && !UNSAFE_BLOCKS.contains(blockAbove.getBlock().defaultBlockState()) && !SUFFOCATING_BLOCKS.contains(blockAbove) && !SUFFOCATING_BLOCKS.contains(blockAbove.getBlock().defaultBlockState()))
                && (blockBelow.is(Blocks.AIR) || blockBelow.is(Blocks.CAVE_AIR));
    }

    private static boolean isSafeCoordinate(Level world, Coordinate coordinate) {
        BlockState block = world.getBlockState(coordinate.toBlockPos());
        BlockState blockAbove = world.getBlockState(coordinate.toBlockPos().above());
        BlockState blockBelow = world.getBlockState(coordinate.toBlockPos().below());
        return (!block.blocksMotion() && !UNSAFE_BLOCKS.contains(block) && !UNSAFE_BLOCKS.contains(block.getBlock().defaultBlockState()))
                && (!blockAbove.blocksMotion() && !UNSAFE_BLOCKS.contains(blockAbove) && !UNSAFE_BLOCKS.contains(blockAbove.getBlock().defaultBlockState()) && !SUFFOCATING_BLOCKS.contains(blockAbove) && !SUFFOCATING_BLOCKS.contains(blockAbove.getBlock().defaultBlockState()))
                && (blockBelow.isSolid() && !UNSAFE_BLOCKS.contains(blockBelow) && !UNSAFE_BLOCKS.contains(blockBelow.getBlock().defaultBlockState()));
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
        return getBiome(ResourceLocation.parse(id));
    }

    public static ResourceKey<Biome> getBiome(@NonNull ResourceLocation id) {
        // FIXME 应该有更好的判断方法
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
        // for (int x : IntStream.range((int) (start.getX() - radius), (int) (start.getX() + radius)).boxed()
        //         .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) start.getX())))
        //         .collect(Collectors.toList())) {
        //     for (int z : IntStream.range((int) (start.getZ() - radius), (int) (start.getZ() + radius)).boxed()
        //             .sorted(Comparator.comparingInt(a -> Math.abs(a - (int) start.getZ())))
        //             .collect(Collectors.toList())) {
        //         Coordinate clone = start.clone();
        //         BlockPos pos = clone.setX(x).setZ(z).toBlockPos();
        //         Biome b = world.getBiome(pos);
        //         if (b == biome) {
        //             return clone;
        //         }
        //     }
        // }
        // // 未找到目标生物群系
        // return null;
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
        return getStructure(ResourceLocation.parse(id));
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

    public static KeyValue<String, String> getPlayerHomeKey(ServerPlayer player, ResourceKey<Level> dimension, String name) {
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
        return PlayerTeleportDataCapability.getData(player).getHomeCoordinate().getOrDefault(getPlayerHomeKey(player, dimension, name), null);
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
     * 获取并移除玩家离开的坐标
     *
     * @param player    玩家
     * @param type      传送类型
     * @param dimension 维度
     * @return 查询到的离开坐标（如果未找到则返回 null）
     */
    public static TeleportRecord getBackTeleportRecord(ServerPlayer player, @Nullable ETeleportType type, @Nullable ResourceKey<Level> dimension) {
        TeleportRecord result = null;
        // 获取玩家的传送数据
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        List<TeleportRecord> records = data.getTeleportRecords();
        Optional<TeleportRecord> optionalRecord = records.stream()
                .filter(record -> type == null || record.getTeleportType() == type)
                .filter(record -> type == ETeleportType.TP_BACK || record.getTeleportType() != ETeleportType.TP_BACK)
                .filter(record -> dimension == null || record.getBefore().getDimension().equals(dimension))
                .max(Comparator.comparing(TeleportRecord::getTeleportTime));
        if (optionalRecord.isPresent()) {
            result = optionalRecord.get();
        }
        return result;
    }

    public static void removeBackTeleportRecord(ServerPlayer player, TeleportRecord record) {
        PlayerTeleportDataCapability.getData(player).getTeleportRecords().remove(record);
    }

    // endregion 坐标查找

    // region 传送相关

    /**
     * 检查传送范围
     */
    public static int checkRange(ServerPlayer player, ETeleportType type, int range) {
        int maxRange = switch (type) {
            case TP_VIEW -> ServerConfig.TELEPORT_VIEW_DISTANCE_LIMIT.get();
            default -> ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get();
        };
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
    public static void teleportTo(@NonNull ServerPlayer from, @NonNull ServerPlayer to, ETeleportType type, boolean safe) {
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
    public static void teleportTo(@NonNull ServerPlayer player, @NonNull Coordinate after, ETeleportType type) {
        Coordinate before = new Coordinate(player);
        Level world = player.level();
        MinecraftServer server = player.getServer();
        // 别听Idea的
        if (world != null && server != null) {
            ServerLevel level = server.getLevel(after.getDimension());
            if (level != null) {
                if (after.isSafe()) {
                    // 异步的代价就是粪吗
                    player.displayClientMessage(Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.MESSAGE, "safe_searching").toTextComponent(), true);
                    new Thread(() -> {
                        Coordinate finalAfter = after.clone();
                        finalAfter = findSafeCoordinate(finalAfter, false);
                        Runnable runnable;
                        // 判断是否需要在脚下放置方块
                        if (ServerConfig.SETBLOCK_WHEN_SAFE_NOT_FOUND.get() && !isSafeCoordinate(level, finalAfter)) {
                            BlockState blockState;
                            List<ItemStack> playerItemList = getPlayerItemList(player);
                            if (CollectionUtils.isNotNullOrEmpty(SAFE_BLOCKS)) {
                                if (ServerConfig.GETBLOCK_FROM_INVENTORY.get()) {
                                    blockState = SAFE_BLOCKS.stream()
                                            .filter(block -> playerItemList.stream().map(ItemStack::getItem).anyMatch(item -> new ItemStack(block.getBlock()).getItem().equals(item)))
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
                            doTeleport(player, finalAfter1, type, before, level);
                        });
                    }).start();
                } else {
                    doTeleport(player, after, type, before, level);
                }
            }
        }
    }

    private static void doTeleport(@NonNull ServerPlayer player, @NonNull Coordinate after, ETeleportType type, Coordinate before, ServerLevel level) {
        after.setY(Math.floor(after.getY()) + 0.1);
        player.teleportTo(level, after.getX(), after.getY(), after.getZ(), Set.of()
                , after.getYaw() == 0 ? player.getYRot() : (float) after.getYaw()
                , after.getPitch() == 0 ? player.getXRot() : (float) after.getPitch(), true);
        TeleportRecord record = new TeleportRecord();
        record.setTeleportTime(new Date());
        record.setTeleportType(type);
        record.setBefore(before);
        record.setAfter(after);
        PlayerTeleportDataCapability.getData(player).addTeleportRecords(record);
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
     * @param player  发送者
     * @param message 消息
     */
    public static void broadcastMessage(ServerPlayer player, Component message) {
        player.server.getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.translatable("chat.type.announcement", player.getDisplayName(), message.toChatComponent()), false);
    }

    /**
     * 广播消息
     *
     * @param server  发送者
     * @param message 消息
     */
    public static void broadcastMessage(MinecraftServer server, Component message) {
        server.getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.translatable("chat.type.announcement", "Server", message.toChatComponent()), false);
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
            source.sendSuccess(() -> Component.translatable(key, args).setLanguageCode(NarcissusFarewell.DEFAULT_LANGUAGE).toChatComponent(), false);
        } else {
            source.sendFailure(Component.translatable(key, args).setLanguageCode(NarcissusFarewell.DEFAULT_LANGUAGE).toChatComponent());
        }
    }

    // endregion 消息相关

    // region 跨维度传送

    public static boolean isTeleportAcrossDimensionEnabled(ServerPlayer player, ResourceKey<Level> to, ETeleportType type) {
        boolean result = true;
        if (player.level().dimension() != to) {
            if (ServerConfig.TELEPORT_ACROSS_DIMENSION.get()) {
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
    public static boolean isTeleportTypeAcrossDimensionEnabled(ServerPlayer player, ETeleportType type) {
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
    public static int getTeleportCoolDown(ServerPlayer player, ETeleportType type) {
        // 如果传送卡类型为抵消冷却时间，则不计算冷却时间
        if (ServerConfig.TELEPORT_CARD_TYPE.get() == ECardType.REFUND_COOLDOWN || ServerConfig.TELEPORT_CARD_TYPE.get() == ECardType.REFUND_ALL_COST_AND_COOLDOWN) {
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
        switch (ServerConfig.TELEPORT_REQUEST_COOLDOWN_TYPE.get()) {
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
    public static int getCommandCoolDown(ETeleportType type) {
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

    private static int calculateCooldown(UUID uuid, Instant current, Instant lastTpTime, int cooldown, ETeleportType type) {
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
    public static boolean validTeleportCost(ServerPlayer player, Coordinate target, ETeleportType type, boolean submit) {
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
    private static boolean validateCost(ServerPlayer player, ResourceKey<Level> targetDim, double distance, ETeleportType teleportType, boolean submit) {
        TeleportCost cost = NarcissusUtils.getCommandCost(teleportType);
        if (cost.getType() == ECostType.NONE) return true;

        double adjustedDistance;
        if (player.level().dimension() == targetDim) {
            adjustedDistance = Math.min(ServerConfig.TELEPORT_COST_DISTANCE_LIMIT.get(), distance);
        } else {
            adjustedDistance = ServerConfig.TELEPORT_COST_DISTANCE_ACROSS_DIMENSION.get();
        }

        double need = cost.getNum() * adjustedDistance * cost.getRate();
        int cardNeedTotal = getTeleportCardNeedPre(need);
        int cardNeed = getTeleportCardNeedPost(player, need);
        int costNeed = getTeleportCostNeedPost(player, need);
        boolean result = false;

        switch (cost.getType()) {
            case EXP_POINT:
                result = player.totalExperience >= costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.WORD, "exp_point"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.giveExperiencePoints(-costNeed);
                    PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case EXP_LEVEL:
                result = player.experienceLevel >= costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.WORD, "exp_level"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.giveExperienceLevels(-costNeed);
                    PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case HEALTH:
                result = player.getHealth() > costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.WORD, "health"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.hurt(player.level().damageSources().magic(), costNeed);
                    PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case HUNGER:
                result = player.getFoodData().getFoodLevel() >= costNeed && cardNeed == 0;
                if (!result && cardNeed == 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), Component.translatable(NarcissusUtils.getPlayerLanguage(player), EI18nType.WORD, "hunger"), (int) Math.ceil(need));
                } else if (result && submit) {
                    player.getFoodData().setFoodLevel(player.getFoodData().getFoodLevel() - costNeed);
                    PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                }
                break;
            case ITEM:
                try {
                    ItemStack itemStack = ItemStack.parseOptional(player.registryAccess(), TagParser.parseTag(cost.getConf()));
                    result = getItemCount(player.getInventory().items, itemStack) >= costNeed && cardNeed == 0;
                    if (!result && cardNeed == 0) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "cost_not_enough"), itemStack.getDisplayName(), (int) Math.ceil(need));
                    } else if (result && submit) {
                        itemStack.setCount(costNeed);
                        result = removeItemFromPlayerInventory(player, itemStack);
                        // 代价不足
                        if (result) {
                            PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
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
                        // 指令执行的返回结果怎么没了???
                        NarcissusFarewell.getServerInstance().getCommands().performPrefixedCommand(player.createCommandSourceStack(), command);
                        PlayerTeleportDataCapability.getData(player).subTeleportCard(cardNeedTotal);
                        result = true;
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
    public static int getTeleportCostNeedPost(ServerPlayer player, double need) {
        int ceil = (int) Math.ceil(need);
        if (!ServerConfig.TELEPORT_CARD.get()) return ceil;
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        return switch (ServerConfig.TELEPORT_CARD_TYPE.get()) {
            case NONE -> data.getTeleportCard() > 0 ? ceil : -1;
            case LIKE_COST -> data.getTeleportCard() >= ceil ? ceil : -1;
            case REFUND_COST, REFUND_COST_AND_COOLDOWN -> Math.max(0, ceil - data.getTeleportCard());
            case REFUND_ALL_COST, REFUND_ALL_COST_AND_COOLDOWN -> data.getTeleportCard() > 0 ? 0 : ceil;
            default -> ceil;
        };
    }

    /**
     * 须支付多少传送卡
     */
    public static int getTeleportCardNeedPre(double need) {
        int ceil = (int) Math.ceil(need);
        if (!ServerConfig.TELEPORT_CARD.get()) return 0;
        return switch (ServerConfig.TELEPORT_CARD_TYPE.get()) {
            case LIKE_COST -> ceil;
            default -> 1;
        };
    }

    /**
     * 使用传送卡后还须支付多少传送卡
     */
    public static int getTeleportCardNeedPost(ServerPlayer player, double need) {
        int ceil = (int) Math.ceil(need);
        if (!ServerConfig.TELEPORT_CARD.get()) return 0;
        IPlayerTeleportData data = PlayerTeleportDataCapability.getData(player);
        return switch (ServerConfig.TELEPORT_CARD_TYPE.get()) {
            case NONE -> data.getTeleportCard() > 0 ? 0 : 1;
            case LIKE_COST -> Math.max(0, ceil - data.getTeleportCard());
            default -> 0;
        };
    }

    public static TeleportCost getCommandCost(ETeleportType type) {
        TeleportCost cost = new TeleportCost();
        switch (type) {
            case TP_COORDINATE:
                cost.setType(ServerConfig.COST_TP_COORDINATE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_COORDINATE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_COORDINATE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_COORDINATE_CONF.get());
                break;
            case TP_STRUCTURE:
                cost.setType(ServerConfig.COST_TP_STRUCTURE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_STRUCTURE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_STRUCTURE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_STRUCTURE_CONF.get());
                break;
            case TP_ASK:
                cost.setType(ServerConfig.COST_TP_ASK_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_ASK_NUM.get());
                cost.setRate(ServerConfig.COST_TP_ASK_RATE.get());
                cost.setConf(ServerConfig.COST_TP_ASK_CONF.get());
                break;
            case TP_HERE:
                cost.setType(ServerConfig.COST_TP_HERE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_HERE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_HERE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_HERE_CONF.get());
                break;
            case TP_RANDOM:
                cost.setType(ServerConfig.COST_TP_RANDOM_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_RANDOM_NUM.get());
                cost.setRate(ServerConfig.COST_TP_RANDOM_RATE.get());
                cost.setConf(ServerConfig.COST_TP_RANDOM_CONF.get());
                break;
            case TP_SPAWN:
                cost.setType(ServerConfig.COST_TP_SPAWN_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_SPAWN_NUM.get());
                cost.setRate(ServerConfig.COST_TP_SPAWN_RATE.get());
                cost.setConf(ServerConfig.COST_TP_SPAWN_CONF.get());
                break;
            case TP_WORLD_SPAWN:
                cost.setType(ServerConfig.COST_TP_WORLD_SPAWN_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_WORLD_SPAWN_NUM.get());
                cost.setRate(ServerConfig.COST_TP_WORLD_SPAWN_RATE.get());
                cost.setConf(ServerConfig.COST_TP_WORLD_SPAWN_CONF.get());
                break;
            case TP_TOP:
                cost.setType(ServerConfig.COST_TP_TOP_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_TOP_NUM.get());
                cost.setRate(ServerConfig.COST_TP_TOP_RATE.get());
                cost.setConf(ServerConfig.COST_TP_TOP_CONF.get());
                break;
            case TP_BOTTOM:
                cost.setType(ServerConfig.COST_TP_BOTTOM_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_BOTTOM_NUM.get());
                cost.setRate(ServerConfig.COST_TP_BOTTOM_RATE.get());
                cost.setConf(ServerConfig.COST_TP_BOTTOM_CONF.get());
                break;
            case TP_UP:
                cost.setType(ServerConfig.COST_TP_UP_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_UP_NUM.get());
                cost.setRate(ServerConfig.COST_TP_UP_RATE.get());
                cost.setConf(ServerConfig.COST_TP_UP_CONF.get());
                break;
            case TP_DOWN:
                cost.setType(ServerConfig.COST_TP_DOWN_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_DOWN_NUM.get());
                cost.setRate(ServerConfig.COST_TP_DOWN_RATE.get());
                cost.setConf(ServerConfig.COST_TP_DOWN_CONF.get());
                break;
            case TP_VIEW:
                cost.setType(ServerConfig.COST_TP_VIEW_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_VIEW_NUM.get());
                cost.setRate(ServerConfig.COST_TP_VIEW_RATE.get());
                cost.setConf(ServerConfig.COST_TP_VIEW_CONF.get());
                break;
            case TP_HOME:
                cost.setType(ServerConfig.COST_TP_HOME_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_HOME_NUM.get());
                cost.setRate(ServerConfig.COST_TP_HOME_RATE.get());
                cost.setConf(ServerConfig.COST_TP_HOME_CONF.get());
                break;
            case TP_STAGE:
                cost.setType(ServerConfig.COST_TP_STAGE_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_STAGE_NUM.get());
                cost.setRate(ServerConfig.COST_TP_STAGE_RATE.get());
                cost.setConf(ServerConfig.COST_TP_STAGE_CONF.get());
                break;
            case TP_BACK:
                cost.setType(ServerConfig.COST_TP_BACK_TYPE.get());
                cost.setNum(ServerConfig.COST_TP_BACK_NUM.get());
                cost.setRate(ServerConfig.COST_TP_BACK_RATE.get());
                cost.setConf(ServerConfig.COST_TP_BACK_CONF.get());
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
        double deltaX = coordinate1.getX() - coordinate2.getX();
        double deltaY = coordinate1.getY() - coordinate2.getY();
        double deltaZ = coordinate1.getZ() - coordinate2.getZ();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    // endregion 传送代价

    // region 杂项

    public static String getPlayerLanguage(ServerPlayer player) {
        return player.getLanguage();
    }

    /**
     * 复制玩家语言设置
     *
     * @param originalPlayer 原始玩家
     * @param targetPlayer   目标玩家
     */
    public static void clonePlayerLanguage(ServerPlayer originalPlayer, ServerPlayer targetPlayer) {
        FieldUtils.setPrivateFieldValue(ServerPlayer.class, targetPlayer, FieldUtils.getPlayerLanguageFieldName(originalPlayer), getPlayerLanguage(originalPlayer));
    }

    public static String getClientLanguage() {
        return Minecraft.getInstance().getLanguageManager().getSelected();
    }

    /**
     * 强行使玩家死亡
     */
    @SuppressWarnings("unchecked")
    public static boolean killPlayer(ServerPlayer player) {
        try {
            if (player.isSleeping() && !player.level().isClientSide) {
                player.stopSleeping();
            }
            player.getEntityData().set((EntityDataAccessor<? super Float>) FieldUtils.getPrivateFieldValue(LivingEntity.class, null, FieldUtils.getEntityHealthFieldName()), 0f);
            player.connection.send(new ClientboundPlayerCombatKillPacket(player.getId(), CommonComponents.EMPTY));
            if (!player.isSpectator()) {
                if (!player.serverLevel().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) {
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
     * 获取当前mod支持的mc版本
     *
     * @return 主版本*1000000+次版本*1000+修订版本， 如 1.16.5 -> 1 * 1000000 + 16 * 1000 + 5 = 10016005
     */
    public static int getMcVersion() {
        int version = 0;
        ModContainer container = ModList.get().getModContainerById(NarcissusFarewell.MODID).orElse(null);
        if (container != null) {
            IModInfo.ModVersion minecraftVersion = container.getModInfo().getDependencies().stream()
                    .filter(dependency -> dependency.getModId().equalsIgnoreCase("minecraft"))
                    .findFirst()
                    .orElse(null);
            if (minecraftVersion != null) {
                ArtifactVersion lowerBound = minecraftVersion.getVersionRange().getRestrictions().get(0).getLowerBound();
                int majorVersion = lowerBound.getMajorVersion();
                int minorVersion = lowerBound.getMinorVersion();
                int incrementalVersion = lowerBound.getIncrementalVersion();
                version = majorVersion * 1000000 + minorVersion * 1000 + incrementalVersion;
            }
        }
        return version;
    }

    // endregion 杂项
}
