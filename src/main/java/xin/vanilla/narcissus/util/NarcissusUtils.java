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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
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
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.*;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.IPlayerTeleportData;
import xin.vanilla.narcissus.data.player.PlayerTeleportDataCapability;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.*;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
        switch (type) {
            case CARD:
            case SET_CARD:
            case CARD_CONCISE:
            case SET_CARD_CONCISE:
                return ServerConfig.TELEPORT_CARD.get();
            case SHARE:
            case SHARE_CONCISE:
                return ServerConfig.SWITCH_SHARE.get();
            case FEED:
            case FEED_OTHER:
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return ServerConfig.SWITCH_FEED.get();
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return ServerConfig.SWITCH_TP_COORDINATE.get();
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return ServerConfig.SWITCH_TP_STRUCTURE.get();
            case TP_ASK:
            case TP_ASK_YES:
            case TP_ASK_NO:
            case TP_ASK_CANCEL:
            case TP_ASK_CONCISE:
            case TP_ASK_YES_CONCISE:
            case TP_ASK_NO_CONCISE:
            case TP_ASK_CANCEL_CONCISE:
                return ServerConfig.SWITCH_TP_ASK.get();
            case TP_HERE:
            case TP_HERE_YES:
            case TP_HERE_NO:
            case TP_HERE_CANCEL:
            case TP_HERE_CONCISE:
            case TP_HERE_YES_CONCISE:
            case TP_HERE_NO_CONCISE:
            case TP_HERE_CANCEL_CONCISE:
                return ServerConfig.SWITCH_TP_HERE.get();
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return ServerConfig.SWITCH_TP_RANDOM.get();
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return ServerConfig.SWITCH_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return ServerConfig.SWITCH_TP_WORLD_SPAWN.get();
            case TP_TOP:
            case TP_TOP_CONCISE:
                return ServerConfig.SWITCH_TP_TOP.get();
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return ServerConfig.SWITCH_TP_BOTTOM.get();
            case TP_UP:
            case TP_UP_CONCISE:
                return ServerConfig.SWITCH_TP_UP.get();
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return ServerConfig.SWITCH_TP_DOWN.get();
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return ServerConfig.SWITCH_TP_VIEW.get();
            case TP_HOME:
            case SET_HOME:
            case DEL_HOME:
            case GET_HOME:
            case TP_HOME_CONCISE:
            case SET_HOME_CONCISE:
            case DEL_HOME_CONCISE:
            case GET_HOME_CONCISE:
                return ServerConfig.SWITCH_TP_HOME.get();
            case TP_STAGE:
            case SET_STAGE:
            case DEL_STAGE:
            case GET_STAGE:
            case TP_STAGE_CONCISE:
            case SET_STAGE_CONCISE:
            case DEL_STAGE_CONCISE:
            case GET_STAGE_CONCISE:
                return ServerConfig.SWITCH_TP_STAGE.get();
            case TP_BACK:
            case TP_BACK_CONCISE:
                return ServerConfig.SWITCH_TP_BACK.get();
            default:
                return true;
        }
    }

    public static String getCommand(ETeleportType type) {
        switch (type) {
            case TP_COORDINATE:
                return ServerConfig.COMMAND_TP_COORDINATE.get();
            case TP_STRUCTURE:
                return ServerConfig.COMMAND_TP_STRUCTURE.get();
            case TP_ASK:
                return ServerConfig.COMMAND_TP_ASK.get();
            case TP_HERE:
                return ServerConfig.COMMAND_TP_HERE.get();
            case TP_RANDOM:
                return ServerConfig.COMMAND_TP_RANDOM.get();
            case TP_SPAWN:
                return ServerConfig.COMMAND_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
                return ServerConfig.COMMAND_TP_WORLD_SPAWN.get();
            case TP_TOP:
                return ServerConfig.COMMAND_TP_TOP.get();
            case TP_BOTTOM:
                return ServerConfig.COMMAND_TP_BOTTOM.get();
            case TP_UP:
                return ServerConfig.COMMAND_TP_UP.get();
            case TP_DOWN:
                return ServerConfig.COMMAND_TP_DOWN.get();
            case TP_VIEW:
                return ServerConfig.COMMAND_TP_VIEW.get();
            case TP_HOME:
                return ServerConfig.COMMAND_TP_HOME.get();
            case TP_STAGE:
                return ServerConfig.COMMAND_TP_STAGE.get();
            case TP_BACK:
                return ServerConfig.COMMAND_TP_BACK.get();
            default:
                return "";
        }
    }

    public static String getCommand(ECommandType type) {
        String prefix = NarcissusUtils.getCommandPrefix();
        switch (type) {
            case HELP:
                return prefix + " help";
            case LANGUAGE:
                return prefix + " " + ServerConfig.COMMAND_LANGUAGE.get();
            case LANGUAGE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_LANGUAGE.get() : "";
            case DIMENSION:
                return prefix + " " + ServerConfig.COMMAND_DIMENSION.get();
            case DIMENSION_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_DIMENSION.get() : "";
            case UUID:
                return prefix + " " + ServerConfig.COMMAND_UUID.get();
            case UUID_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_UUID.get() : "";
            case CARD:
            case SET_CARD:
                return prefix + " " + ServerConfig.COMMAND_CARD.get();
            case CARD_CONCISE:
            case SET_CARD_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_CARD.get() : "";
            case SHARE:
                return prefix + " " + ServerConfig.COMMAND_SHARE.get();
            case SHARE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_SHARE.get() : "";
            case FEED:
            case FEED_OTHER:
                return prefix + " " + ServerConfig.COMMAND_FEED.get();
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_FEED.get() : "";
            case TP_COORDINATE:
                return prefix + " " + ServerConfig.COMMAND_TP_COORDINATE.get();
            case TP_COORDINATE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_COORDINATE.get() : "";
            case TP_STRUCTURE:
                return prefix + " " + ServerConfig.COMMAND_TP_STRUCTURE.get();
            case TP_STRUCTURE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_STRUCTURE.get() : "";
            case TP_ASK:
                return prefix + " " + ServerConfig.COMMAND_TP_ASK.get();
            case TP_ASK_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK.get() : "";
            case TP_ASK_YES:
                return prefix + " " + ServerConfig.COMMAND_TP_ASK_YES.get();
            case TP_ASK_YES_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK_YES.get() : "";
            case TP_ASK_NO:
                return prefix + " " + ServerConfig.COMMAND_TP_ASK_NO.get();
            case TP_ASK_NO_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK_NO.get() : "";
            case TP_ASK_CANCEL:
                return prefix + " " + ServerConfig.COMMAND_TP_ASK_CANCEL.get();
            case TP_ASK_CANCEL_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_ASK_CANCEL.get() : "";
            case TP_HERE:
                return prefix + " " + ServerConfig.COMMAND_TP_HERE.get();
            case TP_HERE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE.get() : "";
            case TP_HERE_YES:
                return prefix + " " + ServerConfig.COMMAND_TP_HERE_YES.get();
            case TP_HERE_YES_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE_YES.get() : "";
            case TP_HERE_NO:
                return prefix + " " + ServerConfig.COMMAND_TP_HERE_NO.get();
            case TP_HERE_NO_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE_NO.get() : "";
            case TP_HERE_CANCEL:
                return prefix + " " + ServerConfig.COMMAND_TP_HERE_CANCEL.get();
            case TP_HERE_CANCEL_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HERE_CANCEL.get() : "";
            case TP_RANDOM:
                return prefix + " " + ServerConfig.COMMAND_TP_RANDOM.get();
            case TP_RANDOM_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_RANDOM.get() : "";
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
                return prefix + " " + ServerConfig.COMMAND_TP_SPAWN.get();
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_SPAWN.get() : "";
            case TP_WORLD_SPAWN:
                return prefix + " " + ServerConfig.COMMAND_TP_WORLD_SPAWN.get();
            case TP_WORLD_SPAWN_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_WORLD_SPAWN.get() : "";
            case TP_TOP:
                return prefix + " " + ServerConfig.COMMAND_TP_TOP.get();
            case TP_TOP_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_TOP.get() : "";
            case TP_BOTTOM:
                return prefix + " " + ServerConfig.COMMAND_TP_BOTTOM.get();
            case TP_BOTTOM_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_BOTTOM.get() : "";
            case TP_UP:
                return prefix + " " + ServerConfig.COMMAND_TP_UP.get();
            case TP_UP_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_UP.get() : "";
            case TP_DOWN:
                return prefix + " " + ServerConfig.COMMAND_TP_DOWN.get();
            case TP_DOWN_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_DOWN.get() : "";
            case TP_VIEW:
                return prefix + " " + ServerConfig.COMMAND_TP_VIEW.get();
            case TP_VIEW_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_VIEW.get() : "";
            case TP_HOME:
                return prefix + " " + ServerConfig.COMMAND_TP_HOME.get();
            case TP_HOME_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_HOME.get() : "";
            case SET_HOME:
                return prefix + " " + ServerConfig.COMMAND_SET_HOME.get();
            case SET_HOME_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_SET_HOME.get() : "";
            case DEL_HOME:
                return prefix + " " + ServerConfig.COMMAND_DEL_HOME.get();
            case DEL_HOME_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_DEL_HOME.get() : "";
            case GET_HOME:
                return prefix + " " + ServerConfig.COMMAND_GET_HOME.get();
            case GET_HOME_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_GET_HOME.get() : "";
            case TP_STAGE:
                return prefix + " " + ServerConfig.COMMAND_TP_STAGE.get();
            case TP_STAGE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_STAGE.get() : "";
            case SET_STAGE:
                return prefix + " " + ServerConfig.COMMAND_SET_STAGE.get();
            case SET_STAGE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_SET_STAGE.get() : "";
            case DEL_STAGE:
                return prefix + " " + ServerConfig.COMMAND_DEL_STAGE.get();
            case DEL_STAGE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_DEL_STAGE.get() : "";
            case GET_STAGE:
                return prefix + " " + ServerConfig.COMMAND_GET_STAGE.get();
            case GET_STAGE_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_GET_STAGE.get() : "";
            case TP_BACK:
                return prefix + " " + ServerConfig.COMMAND_TP_BACK.get();
            case TP_BACK_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_TP_BACK.get() : "";
            case VIRTUAL_OP:
                return prefix + " " + ServerConfig.COMMAND_VIRTUAL_OP.get();
            case VIRTUAL_OP_CONCISE:
                return isConciseEnabled(type) ? ServerConfig.COMMAND_VIRTUAL_OP.get() : "";
            default:
                return "";
        }
    }

    public static int getCommandPermissionLevel(ECommandType type) {
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
            case VIRTUAL_OP:
            case VIRTUAL_OP_CONCISE:
                return ServerConfig.PERMISSION_VIRTUAL_OP.get();
            default:
                return 0;
        }
    }

    public static int getCommandPermissionLevel(ETeleportType type) {
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

    public static boolean isConciseEnabled(ECommandType type) {
        switch (type) {
            case LANGUAGE:
            case LANGUAGE_CONCISE:
                return ServerConfig.CONCISE_LANGUAGE.get();
            case UUID:
            case UUID_CONCISE:
                return ServerConfig.CONCISE_UUID.get();
            case DIMENSION:
            case DIMENSION_CONCISE:
                return ServerConfig.CONCISE_DIMENSION.get();
            case CARD:
            case CARD_CONCISE:
            case SET_CARD:
            case SET_CARD_CONCISE:
                return ServerConfig.CONCISE_CARD.get();
            case SHARE:
            case SHARE_CONCISE:
                return ServerConfig.CONCISE_SHARE.get();
            case FEED:
            case FEED_OTHER:
            case FEED_CONCISE:
            case FEED_OTHER_CONCISE:
                return ServerConfig.CONCISE_FEED.get();
            case TP_COORDINATE:
            case TP_COORDINATE_CONCISE:
                return ServerConfig.CONCISE_TP_COORDINATE.get();
            case TP_STRUCTURE:
            case TP_STRUCTURE_CONCISE:
                return ServerConfig.CONCISE_TP_STRUCTURE.get();
            case TP_ASK:
            case TP_ASK_CONCISE:
                return ServerConfig.CONCISE_TP_ASK.get();
            case TP_ASK_YES:
            case TP_ASK_YES_CONCISE:
                return ServerConfig.CONCISE_TP_ASK_YES.get();
            case TP_ASK_NO:
            case TP_ASK_NO_CONCISE:
                return ServerConfig.CONCISE_TP_ASK_NO.get();
            case TP_ASK_CANCEL:
            case TP_ASK_CANCEL_CONCISE:
                return ServerConfig.CONCISE_TP_ASK_CANCEL.get();
            case TP_HERE:
            case TP_HERE_CONCISE:
                return ServerConfig.CONCISE_TP_HERE.get();
            case TP_HERE_YES:
            case TP_HERE_YES_CONCISE:
                return ServerConfig.CONCISE_TP_HERE_YES.get();
            case TP_HERE_NO:
            case TP_HERE_NO_CONCISE:
                return ServerConfig.CONCISE_TP_HERE_NO.get();
            case TP_HERE_CANCEL:
            case TP_HERE_CANCEL_CONCISE:
                return ServerConfig.CONCISE_TP_HERE_CANCEL.get();
            case TP_RANDOM:
            case TP_RANDOM_CONCISE:
                return ServerConfig.CONCISE_TP_RANDOM.get();
            case TP_SPAWN:
            case TP_SPAWN_OTHER:
            case TP_SPAWN_CONCISE:
            case TP_SPAWN_OTHER_CONCISE:
                return ServerConfig.CONCISE_TP_SPAWN.get();
            case TP_WORLD_SPAWN:
            case TP_WORLD_SPAWN_CONCISE:
                return ServerConfig.CONCISE_TP_WORLD_SPAWN.get();
            case TP_TOP:
            case TP_TOP_CONCISE:
                return ServerConfig.CONCISE_TP_TOP.get();
            case TP_BOTTOM:
            case TP_BOTTOM_CONCISE:
                return ServerConfig.CONCISE_TP_BOTTOM.get();
            case TP_UP:
            case TP_UP_CONCISE:
                return ServerConfig.CONCISE_TP_UP.get();
            case TP_DOWN:
            case TP_DOWN_CONCISE:
                return ServerConfig.CONCISE_TP_DOWN.get();
            case TP_VIEW:
            case TP_VIEW_CONCISE:
                return ServerConfig.CONCISE_TP_VIEW.get();
            case TP_HOME:
            case TP_HOME_CONCISE:
                return ServerConfig.CONCISE_TP_HOME.get();
            case SET_HOME:
            case SET_HOME_CONCISE:
                return ServerConfig.CONCISE_SET_HOME.get();
            case DEL_HOME:
            case DEL_HOME_CONCISE:
                return ServerConfig.CONCISE_DEL_HOME.get();
            case GET_HOME:
            case GET_HOME_CONCISE:
                return ServerConfig.CONCISE_GET_HOME.get();
            case TP_STAGE:
            case TP_STAGE_CONCISE:
                return ServerConfig.CONCISE_TP_STAGE.get();
            case SET_STAGE:
            case SET_STAGE_CONCISE:
                return ServerConfig.CONCISE_SET_STAGE.get();
            case DEL_STAGE:
            case DEL_STAGE_CONCISE:
                return ServerConfig.CONCISE_DEL_STAGE.get();
            case GET_STAGE:
            case GET_STAGE_CONCISE:
                return ServerConfig.CONCISE_GET_STAGE.get();
            case TP_BACK:
            case TP_BACK_CONCISE:
                return ServerConfig.CONCISE_TP_BACK.get();
            case VIRTUAL_OP:
            case VIRTUAL_OP_CONCISE:
                return ServerConfig.CONCISE_VIRTUAL_OP.get();
            default:
                return false;
        }
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
    private static final List<BlockState> SAFE_BLOCKS_STATE = ServerConfig.SAFE_BLOCKS.get().stream()
            .map(NarcissusUtils::deserializeBlockState)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    /**
     * 安全的方块
     */
    private static final List<String> SAFE_BLOCKS = ServerConfig.SAFE_BLOCKS.get().stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    /**
     * 不安全的方块
     */
    private static final List<BlockState> UNSAFE_BLOCKS_STATE = ServerConfig.UNSAFE_BLOCKS.get().stream()
            .map(NarcissusUtils::deserializeBlockState)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    /**
     * 不安全的方块
     */
    private static final List<String> UNSAFE_BLOCKS = ServerConfig.UNSAFE_BLOCKS.get().stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    private static final List<BlockState> SUFFOCATING_BLOCKS_STATE = ServerConfig.SUFFOCATING_BLOCKS.get().stream()
            .map(NarcissusUtils::deserializeBlockState)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    private static final List<String> SUFFOCATING_BLOCKS = ServerConfig.SUFFOCATING_BLOCKS.get().stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    public static ServerLevel getServerLevel() {
        return NarcissusFarewell.getServerInstance().getAllLevels().iterator().next();
    }

    public static int getWorldMinY(Level world) {
        return world.getMinBuildHeight();
    }

    public static int getWorldMaxY(Level world) {
        return world.getMaxBuildHeight();
    }

    public static Coordinate findTopCandidate(ServerLevel world, Coordinate start) {
        if (start.getY() >= NarcissusUtils.getWorldMaxY(world)) return null;
        for (int y : IntStream.range((int) start.getY() + 1, NarcissusUtils.getWorldMaxY(world)).boxed()
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
        if (start.getY() <= NarcissusUtils.getWorldMinY(world)) return null;
        for (int y : IntStream.range(NarcissusUtils.getWorldMinY(world), (int) start.getY() - 1).boxed()
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
        if (start.getY() >= NarcissusUtils.getWorldMaxY(world)) return null;
        for (int y : IntStream.range((int) start.getY() + 1, NarcissusUtils.getWorldMaxY(world)).boxed()
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
        if (start.getY() <= NarcissusUtils.getWorldMinY(world)) return null;
        for (int y : IntStream.range(NarcissusUtils.getWorldMinY(world), (int) start.getY() - 1).boxed()
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

        // 若未找到碰撞点，则使用射线的终点
        if (result == null) {
            result = start.clone().fromVec3(currentPosition);
        }

        // 若需寻找安全坐标，则从碰撞点反向查找安全位置
        if (safe) {
            // 碰撞点的三维向量
            Vec3 collisionVector = result.toVec3();
            for (int stepCount = (int) Math.ceil(collisionVector.distanceTo(startPosition) / stepScale); stepCount >= 0; stepCount--) {
                currentPosition = startPosition.add(stepVector.scale(stepCount));
                BlockPos currentBlockPos = new BlockPos((int) currentPosition.x, (int) currentPosition.y, (int) currentPosition.z);
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
        Level world = getWorld(coordinate.getDimension());

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

    private static Coordinate searchForSafeCoordinateInChunk(Level world, Coordinate coordinate, int chunkX, int chunkZ, boolean belowAllowAir) {
        // 搜索安全位置，限制在目标范围区块内
        int offset = (ServerConfig.SAFE_CHUNK_RANGE.get() - 1) * 16;
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
        List<Coordinate> list = coordinates.stream().sorted(comparator).toList();
        LOGGER.debug("TimeMillis before searching: {}", System.currentTimeMillis());
        for (Coordinate c : list) {
            double offsetX = c.getX() >= 0 ? c.getX() + 0.5 : c.getX() - 0.5;
            double offsetZ = c.getZ() >= 0 ? c.getZ() + 0.5 : c.getZ() - 0.5;
            Coordinate candidate = new Coordinate().setX(offsetX).setY(c.getY() + 0.15).setZ(offsetZ)
                    .setYaw(coordinate.getYaw()).setPitch(coordinate.getPitch())
                    .setDimension(coordinate.getDimension())
                    .setSafe(coordinate.isSafe()).setSafeMode(coordinate.getSafeMode());
            if (belowAllowAir) {
                if (isAirCoordinate(world, candidate)) {
                    result = candidate;
                    break;
                }
            } else {
                if (isSafeCoordinate(world, candidate)) {
                    result = candidate;
                    break;
                }
            }
        }
        LOGGER.debug("TimeMillis after searching: {}", System.currentTimeMillis());
        LOGGER.debug("Target:{} | Safe:{}", coordinate.toXyzString(), result.toXyzString());
        return result;
    }

    private static boolean isAirCoordinate(Level world, Coordinate coordinate) {
        BlockState block = world.getBlockState(coordinate.toBlockPos());
        BlockState blockAbove = world.getBlockState(coordinate.toBlockPos().above());
        BlockState blockBelow = world.getBlockState(coordinate.toBlockPos().below());
        return isSafeBlock(world, coordinate, true, block, blockAbove, blockBelow);
    }

    private static boolean isSafeCoordinate(Level world, Coordinate coordinate) {
        BlockState block = world.getBlockState(coordinate.toBlockPos());
        BlockState blockAbove = world.getBlockState(coordinate.toBlockPos().above());
        BlockState blockBelow = world.getBlockState(coordinate.toBlockPos().below());
        return isSafeBlock(world, coordinate, false, block, blockAbove, blockBelow);
    }

    /**
     * 判断指定坐标是否安全
     *
     * @param block      方块
     * @param blockAbove 头部方块
     * @param blockBelow 脚下方块
     */
    private static boolean isSafeBlock(Level world, Coordinate coordinate, boolean belowAllowAir, BlockState block, BlockState blockAbove, BlockState blockBelow) {
        boolean isCurrentPassable = !block.isCollisionShapeFullBlock(world, coordinate.toBlockPos())
                && !UNSAFE_BLOCKS_STATE.contains(block)
                && !UNSAFE_BLOCKS.contains(NarcissusUtils.getBlockRegistryName(block));

        boolean isHeadSafe = !blockAbove.isSuffocating(world, coordinate.above().toBlockPos())
                && !blockAbove.isCollisionShapeFullBlock(world, coordinate.above().toBlockPos())
                && !UNSAFE_BLOCKS_STATE.contains(blockAbove)
                && !UNSAFE_BLOCKS.contains(NarcissusUtils.getBlockRegistryName(blockAbove))
                && !SUFFOCATING_BLOCKS_STATE.contains(blockAbove)
                && !SUFFOCATING_BLOCKS.contains(NarcissusUtils.getBlockRegistryName(blockAbove));

        boolean isBelowValid;
        if (!blockBelow.getFluidState().isEmpty()) {
            isBelowValid = !UNSAFE_BLOCKS_STATE.contains(blockBelow)
                    && !UNSAFE_BLOCKS.contains(NarcissusUtils.getBlockRegistryName(blockBelow));
        } else {
            isBelowValid = blockBelow.isSolidRender(world, coordinate.below().toBlockPos())
                    && !UNSAFE_BLOCKS_STATE.contains(blockBelow)
                    && !UNSAFE_BLOCKS.contains(NarcissusUtils.getBlockRegistryName(blockBelow));
        }
        if (belowAllowAir) {
            isBelowValid = isBelowValid || blockBelow.is(Blocks.AIR) || blockBelow.is(Blocks.CAVE_AIR);
        }

        return isCurrentPassable && isHeadSafe && isBelowValid;
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
        return getBiome(new ResourceLocation(id));
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
        return getStructure(new ResourceLocation(id));
    }

    public static ResourceKey<Structure> getStructure(ResourceLocation id) {
        Map.Entry<ResourceKey<Structure>, Structure> mapEntry = NarcissusFarewell.getServerInstance().registryAccess()
                .registryOrThrow(Registries.STRUCTURE).entrySet().stream()
                .filter(entry -> entry.getKey().location().equals(id))
                .findFirst().orElse(null);
        return mapEntry != null ? mapEntry.getKey() : null;
    }

    public static TagKey<Structure> getStructureTag(String id) {
        return getStructureTag(new ResourceLocation(id));
    }

    public static TagKey<Structure> getStructureTag(ResourceLocation id) {
        return NarcissusFarewell.getServerInstance().registryAccess()
                .registryOrThrow(Registries.STRUCTURE).getTagNames()
                .filter(tag -> tag.location().equals(id))
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
        Registry<Structure> registry = world.registryAccess().registryOrThrow(Registries.STRUCTURE);
        Either<ResourceKey<Structure>, TagKey<Structure>> left = Either.left(struct);
        HolderSet.ListBacked<Structure> holderSet = left.map((resourceKey) -> registry.getHolder(resourceKey).map(HolderSet::direct), registry::getTag).orElse(null);
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
     * 获取玩家离开的坐标
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
        Stream<TeleportRecord> stream = records.stream()
                .filter(record -> type == null || record.getTeleportType() == type);
        for (String s : ServerConfig.TELEPORT_BACK_SKIP_TYPE.get()) {
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
                            if (CollectionUtils.isNotNullOrEmpty(SAFE_BLOCKS_STATE)) {
                                if (ServerConfig.GETBLOCK_FROM_INVENTORY.get()) {
                                    blockState = SAFE_BLOCKS_STATE.stream()
                                            .filter(block -> playerItemList.stream().map(ItemStack::getItem).anyMatch(item -> new ItemStack(block.getBlock()).getItem().equals(item)))
                                            .findFirst().orElse(null);
                                } else {
                                    blockState = SAFE_BLOCKS_STATE.get(0);
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

    private static void teleportPlayer(@NonNull ServerPlayer player, @NonNull Coordinate after, ETeleportType type, Coordinate before, ServerLevel level) {
        ResourceLocation sound = new ResourceLocation(ServerConfig.TP_SOUND.get());
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
        PlayerTeleportDataCapability.getData(player).addTeleportRecords(record);
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
            player.teleportTo(level, coordinate.getX(), coordinate.getY(), coordinate.getZ()
                    , coordinate.getYaw() == 0 ? player.getYRot() : (float) coordinate.getYaw()
                    , coordinate.getPitch() == 0 ? player.getXRot() : (float) coordinate.getPitch());
        } else {
            if (level == entity.level()) {
                entity.teleportToWithTicket(coordinate.getX(), coordinate.getY(), coordinate.getZ());
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
                        newEntity.moveTo(coordinate.getX(), coordinate.getY(), coordinate.getZ(), yaw, newEntity.getXRot());
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
            source.sendSuccess(() -> Component.translatable(key, args).setLanguageCode(ServerConfig.DEFAULT_LANGUAGE.get()).toChatComponent(), false);
        } else {
            source.sendFailure(Component.translatable(key, args).setLanguageCode(ServerConfig.DEFAULT_LANGUAGE.get()).toChatComponent());
        }
    }

    /**
     * 广播数据包至所有玩家
     *
     * @param packet 数据包
     */
    public static void broadcastPacket(Packet<?> packet) {
        NarcissusFarewell.getServerInstance().getPlayerList().getPlayers().forEach(player -> player.connection.send(packet));
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
        return coordinate1.distanceFrom(coordinate2);
    }

    // endregion 传送代价

    // region 杂项

    public static String getPlayerLanguage(ServerPlayer player) {
        return PlayerTeleportDataCapability.getData(player).getValidLanguage(player);
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

    // endregion 杂项
}
