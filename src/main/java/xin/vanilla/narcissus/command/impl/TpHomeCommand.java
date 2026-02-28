package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.BoolArgumentType;
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
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.*;

import java.util.concurrent.CompletableFuture;

public final class TpHomeCommand {
    private TpHomeCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_HOME)) return 0;
        ResourceKey<Level> targetLevel = null;
        try {
            ResourceKey<Level> targetDimension = DimensionUtils.parse(StringArgumentType.getString(context, "dimension"));
            ServerLevel level = context.getSource().getServer().getLevel(targetDimension);
            if (level != null) {
                targetLevel = targetDimension;
            }
        } catch (IllegalArgumentException ignored) {
        }
        String name = CommandUtils.getStringDefault(context, "name", null);
        Coordinate coordinate = NarcissusUtils.getPlayerHome(player, targetLevel, name);
        if (coordinate == null) {
            if (targetLevel == null && name == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_not_found"));
            } else if (targetLevel != null && name == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_not_found_in_dimension"), targetLevel.identifier().toString());
            } else if (targetLevel == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_not_found_with_name"), name);
            } else {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_not_found_with_name_in_dimension"), targetLevel.identifier().toString(), name);
            }
            return 0;
        }
        try {
            coordinate.safe(BoolArgumentType.getBool(context, "safe"));
        } catch (IllegalArgumentException ignored) {
        }
        if (CommandUtils.checkTeleportPost(player, coordinate, EnumTeleportType.TP_HOME, true)) return 0;
        NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_HOME);
        return 1;
    }

    public static CompletableFuture<Suggestions> safeSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String name = CommandUtils.getStringDefault(context, "name", null);
        String lang = CommandUtils.getLanguage(context.getSource());
        Component trueTooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_safe_true");
        Component falseTooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_safe_false");
        Component dimTooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_dimension");
        if ("true".equals(name) || "false".equals(name)) {
            ServerPlayer player = context.getSource().getPlayerOrException();
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            for (KeyValue<String, String> keyValue : data.getHomeCoordinate().keySet()) {
                builder.suggest(keyValue.key(), dimTooltip.toTextComponent());
            }
            if (data.getHomeCoordinate().keySet().stream()
                    .anyMatch(kv -> kv.value().equals("true") || kv.value().equals("false"))) {
                builder.suggest("true", trueTooltip.toTextComponent());
                builder.suggest("false", falseTooltip.toTextComponent());
            }
        } else {
            builder.suggest("true", trueTooltip.toTextComponent());
            builder.suggest("false", falseTooltip.toTextComponent());
        }
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_TP_HOME.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HOME))
                .executes(TpHomeCommand::execute)
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(CommandUtils::homeSuggestion)
                        .executes(TpHomeCommand::execute)
                        .then(Commands.argument("safe", BoolArgumentType.bool())
                                .suggests(TpHomeCommand::safeSuggestion)
                                .executes(TpHomeCommand::execute)
                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                        .suggests(CommandUtils::homeDimSuggestion)
                                        .executes(TpHomeCommand::execute)
                                )
                        )
                )
                .then(Commands.argument("safe", BoolArgumentType.bool())
                        .executes(TpHomeCommand::execute)
                        .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                .suggests(CommandUtils::homeDimSuggestion)
                                .executes(TpHomeCommand::execute)
                        )
                );
    }
}
