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
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.network.packet.WaypointSyncToClient;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.Component;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.concurrent.CompletableFuture;

public final class SetHomeCommand {
    private SetHomeCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.SET_HOME)) return 0;
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        if (data.getHomeCoordinate().size() >= ServerConfig.TELEPORT_HOME_LIMIT.get()) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_limit"), ServerConfig.TELEPORT_HOME_LIMIT.get());
            return 0;
        }
        String name = CommandUtils.getStringDefault(context, "name", "home");
        boolean defaultHome = false;
        try {
            defaultHome = BoolArgumentType.getBool(context, "default");
        } catch (IllegalArgumentException ignored) {
        }
        Coordinate coordinate = new Coordinate(player);
        KeyValue<String, String> key = new KeyValue<>(player.level().dimension().location().toString(), name);
        if (data.getHomeCoordinate().containsKey(key)) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_already_exists"), key.key(), key.value());
            return 0;
        }
        data.addHomeCoordinate(key, coordinate);
        NarcissusUtils.sendPacketToPlayer(new WaypointSyncToClient(WaypointSyncToClient.Action.ADD, WaypointSyncToClient.Type.HOME, name, coordinate), player);
        if (defaultHome) {
            if (data.getDefaultHome().containsKey(player.level().dimension().location().toString())) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_default_remove"), data.getDefaultHome(player.level().dimension().location().toString()).value());
            }
            data.addDefaultHome(player.level().dimension().location().toString(), name);
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_set_default"), name, coordinate.toXyzString());
        } else {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_set"), name, coordinate.toXyzString());
        }
        return 1;
    }

    public static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String lang = CommandUtils.getLanguage(context.getSource());
        Component homeTooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_home_name");
        Component nameTooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_custom_name");
        builder.suggest("home", homeTooltip.toTextComponent());
        builder.suggest("name", nameTooltip.toTextComponent());
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> defaultSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String lang = CommandUtils.getLanguage(context.getSource());
        Component trueTooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_default_home_true");
        Component falseTooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_default_home_false");
        builder.suggest("true", trueTooltip.toTextComponent());
        builder.suggest("false", falseTooltip.toTextComponent());
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_SET_HOME.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HOME))
                .executes(SetHomeCommand::execute)
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(SetHomeCommand::suggestion)
                        .executes(SetHomeCommand::execute)
                        .then(Commands.argument("default", BoolArgumentType.bool())
                                .suggests(SetHomeCommand::defaultSuggestion)
                                .executes(SetHomeCommand::execute)
                        )
                );
    }
}
