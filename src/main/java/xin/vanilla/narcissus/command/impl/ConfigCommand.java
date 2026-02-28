package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.Component;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.concurrent.CompletableFuture;

public final class ConfigCommand {
    private ConfigCommand() {
    }

    private static int executeTeleportCard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Component msg = Component.trans(EnumI18nType.FORMAT, "server_config_status"
                , I18nUtils.enabled(CommonConfig.TELEPORT_CARD.get())
                , Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.WORD, "teleport_card"));
        NarcissusUtils.sendMessage(player, msg);
        return 1;
    }

    private static int executeTeleportCardSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        boolean bool = BoolArgumentType.getBool(context, "bool");
        CommonConfig.TELEPORT_CARD.set(bool);
        ServerPlayer player = context.getSource().getPlayerOrException();
        Component msg = Component.trans(EnumI18nType.FORMAT, "server_config_status"
                , I18nUtils.enabled(CommonConfig.TELEPORT_CARD.get())
                , Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.WORD, "teleport_card"));
        NarcissusUtils.broadcastMessage(player, msg);
        return 1;
    }

    private static int executeMode(CommandContext<CommandSourceStack> context) {
        int mode = IntegerArgumentType.getInteger(context, "mode");
        CommandSourceStack source = context.getSource();
        String lang = CommandUtils.getLanguage(source);
        switch (mode) {
            case 0:
                ServerConfig.resetConfig();
                CommonConfig.resetConfig();
                break;
            case 1:
                ServerConfig.resetConfigWithMode1();
                CommonConfig.resetConfigWithMode1();
                break;
            case 2:
                ServerConfig.resetConfigWithMode2();
                CommonConfig.resetConfigWithMode2();
                break;
            case 3:
                ServerConfig.resetConfigWithMode3();
                CommonConfig.resetConfigWithMode3();
                break;
            default:
                throw new IllegalArgumentException("Mode " + mode + " does not exist");
        }
        Component component = Component.trans(lang, EnumI18nType.FORMAT, "server_config_mode", mode);
        source.sendSuccess(component.toChatComponent(lang), false);
        source.getServer().getPlayerList().getPlayers()
                .forEach(player -> source.getServer().getPlayerList().sendPlayerPermissionLevel(player));
        return 1;
    }

    private static int executeLanguage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String code = StringArgumentType.getString(context, "language");
        ServerConfig.DEFAULT_LANGUAGE.set(code);
        ServerPlayer player = context.getSource().getPlayerOrException();
        NarcissusUtils.broadcastMessage(player, Component.trans(player, EnumI18nType.FORMAT, "server_default_language", ServerConfig.DEFAULT_LANGUAGE.get()));
        return 1;
    }

    private static CompletableFuture<Suggestions> modeSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        builder.suggest(0);
        builder.suggest(1);
        builder.suggest(2);
        builder.suggest(3);
        return builder.buildFuture();
    }

    private static CompletableFuture<Suggestions> languageSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        I18nUtils.getI18nFiles().forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("config")
                .then(Commands.literal("teleportCard")
                        .executes(ConfigCommand::executeTeleportCard)
                        .then(Commands.argument("bool", BoolArgumentType.bool())
                                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.VIRTUAL_OP))
                                .executes(ConfigCommand::executeTeleportCardSet)
                        )
                )
                .then(Commands.literal("mode")
                        .then(Commands.argument("mode", IntegerArgumentType.integer(0))
                                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.VIRTUAL_OP))
                                .suggests(ConfigCommand::modeSuggestion)
                                .executes(ConfigCommand::executeMode)
                        )
                )
                .then(Commands.literal("language")
                        .then(Commands.argument("language", StringArgumentType.word())
                                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.VIRTUAL_OP))
                                .suggests(ConfigCommand::languageSuggestion)
                                .executes(ConfigCommand::executeLanguage)
                        )
                )
                .then(WhitelistCommand.create())
                .then(BlacklistCommand.create());
    }
}
