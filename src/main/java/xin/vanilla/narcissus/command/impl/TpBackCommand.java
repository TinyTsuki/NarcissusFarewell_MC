package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.concurrent.CompletableFuture;

public final class TpBackCommand {
    private TpBackCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_BACK)) return 0;
        EnumTeleportType type = EnumTeleportType.nullableValueOf(CommandUtils.getStringEmpty(context, "type"));
        RegistryKey<World> targetLevel = null;
        try {
            RegistryKey<World> targetDimension = DimensionUtils.parse(StringArgumentType.getString(context, "dimension"));
            ServerWorld level = context.getSource().getServer().getLevel(targetDimension);
            if (level != null) {
                targetLevel = targetDimension;
            }
        } catch (IllegalArgumentException ignored) {
        }
        TeleportRecord record = NarcissusUtils.getBackTeleportRecord(player, type, targetLevel);
        if (record == null) {
            NarcissusUtils.sendTranslatableMessage(player, "back_not_found");
            return 0;
        }
        SafeWorldCoordinate safeWorldCoordinate = record.getBefore().clone();
        safeWorldCoordinate.safe("safe".equalsIgnoreCase(CommandUtils.getStringEmpty(context, "safe")));
        if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_BACK, true)) return 0;
        NarcissusUtils.removeBackTeleportRecord(player, record);
        NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_BACK);
        return 1;
    }

    public static CompletableFuture<Suggestions> typeSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String type = CommandUtils.getStringEmpty(context, "type");
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

    public static CompletableFuture<Suggestions> dimSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        EnumTeleportType type = EnumTeleportType.nullableValueOf(CommandUtils.getStringEmpty(context, "type"));
        data.getTeleportRecords().stream()
                .filter(record -> type == null || record.getTeleportType().equals(type))
                .filter(java.util.Objects::nonNull)
                .map(record -> record.getBefore().dimension().location().toString())
                .filter(StringUtils::isNotNullOrEmpty)
                .forEach(builder::suggest);
        return builder.buildFuture();
    }


    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandTpBack())
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
