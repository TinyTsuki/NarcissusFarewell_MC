package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.BoolArgumentType;
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
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.DimensionUtils;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.concurrent.CompletableFuture;

public final class TpHomeCommand {
    private TpHomeCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_HOME)) return 0;
        RegistryKey<World> targetLevel = null;
        try {
            RegistryKey<World> targetDimension = DimensionUtils.parse(StringArgumentType.getString(context, "dimension"));
            ServerWorld level = context.getSource().getServer().getLevel(targetDimension);
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
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_not_found_in_dimension"), targetLevel.location().toString());
            } else if (targetLevel == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_not_found_with_name"), name);
            } else {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_not_found_with_name_in_dimension"), targetLevel.location().toString(), name);
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

    public static CompletableFuture<Suggestions> safeSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String name = CommandUtils.getStringDefault(context, "name", null);
        if ("true".equals(name) || "false".equals(name)) {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            for (KeyValue<String, String> keyValue : data.getHomeCoordinate().keySet()) {
                builder.suggest(keyValue.key());
            }
            if (data.getHomeCoordinate().keySet().stream()
                    .anyMatch(kv -> kv.value().equals("true") || kv.value().equals("false"))) {
                builder.suggest("true");
                builder.suggest("false");
            }
        } else {
            builder.suggest("true");
            builder.suggest("false");
        }
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
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
