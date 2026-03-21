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
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.concurrent.CompletableFuture;

public final class ConfigCommand {
    private ConfigCommand() {
    }

    private static int executeTeleportCard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Component msg = NarcissusComponent.get().transAuto("server_config_status"
                , NarcissusLang.get().enabled(CommonConfig.get().base().teleportCard())
                , NarcissusComponent.get().transAuto("teleport_card"));
        MessageUtils.sendMessage(player, msg);
        return 1;
    }

    private static int executeTeleportCardSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        boolean bool = BoolArgumentType.getBool(context, "bool");
        CommonConfig.get().base().teleportCard(bool);
        ServerPlayer player = context.getSource().getPlayerOrException();
        Component msg = NarcissusComponent.get().transAuto("server_config_status"
                , NarcissusLang.get().enabled(CommonConfig.get().base().teleportCard())
                , NarcissusComponent.get().transAuto("teleport_card"));
        MessageUtils.broadcastMessage(player, msg);
        return 1;
    }

    private static int executeMode(CommandContext<CommandSourceStack> context) {
        int mode = IntegerArgumentType.getInteger(context, "mode");
        CommandSourceStack source = context.getSource();
        String lang = CommandUtils.getLanguage(source);
        switch (mode) {
            case 0:
                CommonConfig.resetConfig();
                break;
            case 1:
                CommonConfig.resetConfigWithMode1();
                break;
            case 2:
                CommonConfig.resetConfigWithMode2();
                break;
            case 3:
                CommonConfig.resetConfigWithMode3();
                break;
            default:
                throw new IllegalArgumentException("Mode " + mode + " does not exist");
        }
        Component component = NarcissusComponent.get().transAuto("server_config_mode", mode);
        source.sendSuccess(component.toChat(lang), false);
        source.getServer().getPlayerList().getPlayers()
                .forEach(player -> source.getServer().getPlayerList().sendPlayerPermissionLevel(player));
        return 1;
    }

    private static int executeLanguage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String code = StringArgumentType.getString(context, "language");
        CommonConfig.get().general().defaultLanguage(code);
        ServerPlayer player = context.getSource().getPlayerOrException();
        MessageUtils.broadcastMessage(player, NarcissusComponent.get().transAuto("server_default_language", CommonConfig.get().general().defaultLanguage()));
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
        NarcissusLang.get().getI18nFiles().forEach(builder::suggest);
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
