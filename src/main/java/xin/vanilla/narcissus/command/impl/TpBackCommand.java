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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.concurrent.CompletableFuture;

public final class TpBackCommand {
    private TpBackCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_BACK)) return 0;
        EnumTeleportType type = EnumTeleportType.valueOfEx(xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, "type"));
        ResourceKey<Level> targetLevel = null;
        try {
            ResourceKey<Level> targetDimension = DimensionUtils.parse(StringArgumentType.getString(context, "dimension"));
            ServerLevel level = context.getSource().getServer().getLevel(targetDimension);
            if (level != null) {
                targetLevel = targetDimension;
            }
        } catch (IllegalArgumentException ignored) {
        }
        TeleportRecord record = NarcissusUtils.getBackTeleportRecord(player, type, targetLevel);
        if (record == null) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("back_not_found"), NarcissusNotificationTypes.TELEPORT_ERROR);
            return 0;
        }
        SafeWorldCoordinate safeWorldCoordinate = record.getBefore().clone();
        safeWorldCoordinate.safe("safe".equalsIgnoreCase(xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, "safe")));
        if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_BACK, true)) return 0;
        NarcissusUtils.removeBackTeleportRecord(player, record);
        NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_BACK);
        return 1;
    }

    public static CompletableFuture<Suggestions> typeSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String type = xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, "type");
        if (StringUtils.isNullOrEmptyEx(type)) {
            builder.suggest("ALL");
        }
        for (EnumTeleportType value : EnumTeleportType.values()) {
            if (StringUtils.isNullOrEmptyEx(type) || value.name().toLowerCase().contains(type.toLowerCase())) {
                builder.suggest(value.name());
            }
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> dimSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        EnumTeleportType type = EnumTeleportType.valueOfEx(xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, "type"));
        data.getTeleportRecords().stream()
                .filter(record -> type == null || record.getTeleportType().equals(type))
                .filter(java.util.Objects::nonNull)
                .map(record -> record.getBefore().dimension().location().toString())
                .filter(StringUtils::isNotNullOrEmpty)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }


    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().command().commandTpBack())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_BACK))
                .executes(TpBackCommand::execute)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(CommandUtils::safeSuggestion)
                        .executes(TpBackCommand::execute)
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(TpBackCommand::typeSuggestion)
                                .executes(TpBackCommand::execute)
                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                        .suggests(TpBackCommand::dimSuggestion)
                                        .executes(TpBackCommand::execute)
                                )
                        )
                );
    }
}
