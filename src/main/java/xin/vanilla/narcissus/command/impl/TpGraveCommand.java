package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumSafeMode;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.integration.GraveHelper;
import xin.vanilla.narcissus.util.*;

import java.util.concurrent.CompletableFuture;


public final class TpGraveCommand {
    private TpGraveCommand() {
    }

    private static int executeDefault(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_GRAVE)) return 0;

        Coordinate coord2 = GraveHelper.parseObituaryFromHeldItem(player);
        Coordinate coord1 = null;
        TeleportRecord record = null;

        if (coord2 == null) {
            record = GraveHelper.findLastDeathRecord(player, null);
            if (record != null) {
                coord1 = record.getBefore().clone();
                coord2 = GraveHelper.findCorpseGravestoneNearDeath(player, record);
            }
        } else {
            coord1 = coord2.clone();
        }

        Coordinate target = resolveTeleportTarget(coord1, coord2);
        if (target == null) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "death_not_found"));
            return 0;
        }

        if (CommandUtils.checkTeleportPost(player, target, EnumTeleportType.TP_GRAVE, true)) return 0;
        if (record != null) {
            NarcissusUtils.removeBackTeleportRecord(player, record);
        }
        NarcissusUtils.teleportTo(player, target, EnumTeleportType.TP_GRAVE);
        return 1;
    }

    private static int executeRange(CommandContext<CommandSourceStack> context, int range) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_GRAVE)) return 0;

        int limit = ServerConfig.GRAVE_SEARCH_RANGE_LIMIT.get();
        if (range > limit) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "grave_range_too_large"), limit);
            return 0;
        }

        Coordinate coord2 = GraveHelper.findCorpseGravestoneNearPlayer(player, range);
        if (coord2 == null) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "death_not_found"));
            return 0;
        }

        Coordinate target = resolveTeleportTarget(null, coord2);
        if (CommandUtils.checkTeleportPost(player, target, EnumTeleportType.TP_GRAVE, true)) return 0;
        NarcissusUtils.teleportTo(player, target, EnumTeleportType.TP_GRAVE);
        return 1;
    }

    private static int executeDim(CommandContext<CommandSourceStack> context, ResourceKey<Level> dim) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_GRAVE)) return 0;

        TeleportRecord record = GraveHelper.findLastDeathRecord(player, dim);
        if (record == null) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "death_not_found"));
            return 0;
        }

        Coordinate coord1 = record.getBefore().clone();
        Coordinate coord2 = GraveHelper.findCorpseGravestoneNearDeath(player, record);
        Coordinate target = resolveTeleportTarget(coord1, coord2);

        if (CommandUtils.checkTeleportPost(player, target, EnumTeleportType.TP_GRAVE, true)) return 0;
        NarcissusUtils.removeBackTeleportRecord(player, record);
        NarcissusUtils.teleportTo(player, target, EnumTeleportType.TP_GRAVE);
        return 1;
    }

    /**
     * 解析并执行传送目标
     */
    private static Coordinate resolveTeleportTarget(Coordinate coord1, Coordinate coord2) {
        Coordinate preferred = (coord2 != null) ? coord2 : coord1;
        if (preferred == null) return null;
        if (GraveHelper.isCoordinateSafe(preferred)) {
            return preferred.clone().safe(false);
        }
        Coordinate safe = NarcissusUtils.findSafeCoordinate(preferred.clone().safeMode(EnumSafeMode.Y_C_OFFSET_3), false);
        if (safe != null && GraveHelper.isCoordinateSafe(safe)) {
            return safe.safe(false);
        }
        return preferred.clone().safe(false);
    }

    public static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        builder.suggest("64");
        builder.suggest("128");
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerTeleportData.getData(player).getTeleportRecords().stream()
                .filter(r -> r.getTeleportType() == EnumTeleportType.DEATH)
                .map(r -> r.getBefore().dimension().location().toString())
                .filter(StringUtils::isNotNullOrEmpty)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_TP_GRAVE.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_GRAVE))
                .executes(TpGraveCommand::executeDefault)
                .then(Commands.argument("rangeOrDim", StringArgumentType.greedyString())
                        .suggests(TpGraveCommand::suggestion)
                        .executes(ctx -> {
                            String arg = StringArgumentType.getString(ctx, "rangeOrDim");
                            if (arg == null || arg.isEmpty()) return executeDefault(ctx);

                            try {
                                int range = Integer.parseInt(arg.trim());
                                if (range > 0) {
                                    return executeRange(ctx, range);
                                }
                            } catch (NumberFormatException ignored) {
                            }

                            ResourceKey<Level> dim = null;
                            try {
                                ResourceKey<Level> d = DimensionUtils.parse(arg);
                                if (ctx.getSource().getServer().getLevel(d) != null) dim = d;
                            } catch (IllegalArgumentException ignored) {
                            }
                            if (dim != null) {
                                return executeDim(ctx, dim);
                            }
                            return executeDefault(ctx);
                        })
                );
    }
}
