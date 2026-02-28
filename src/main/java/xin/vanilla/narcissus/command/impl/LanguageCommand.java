package xin.vanilla.narcissus.command.impl;

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
import xin.vanilla.narcissus.config.CustomConfig;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.Component;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.concurrent.CompletableFuture;

public final class LanguageCommand {
    private LanguageCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String language = StringArgumentType.getString(context, "language");
        if (I18nUtils.getI18nFiles().contains(language)) {
            CustomConfig.setPlayerLanguage(NarcissusUtils.getPlayerUUIDString(player), language);
            NarcissusUtils.sendMessage(player, Component.trans(player, EnumI18nType.FORMAT, "player_default_language", language));
        } else if ("server".equalsIgnoreCase(language) || "client".equalsIgnoreCase(language)) {
            CustomConfig.setPlayerLanguage(NarcissusUtils.getPlayerUUIDString(player), language);
            NarcissusUtils.sendMessage(player, Component.trans(player, EnumI18nType.FORMAT, "player_default_language", language));
        } else {
            NarcissusUtils.sendMessage(player, Component.trans(player, EnumI18nType.FORMAT, "language_not_exist").color(0xFFFF0000));
        }
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String lang = CommandUtils.getLanguage(context.getSource());
        Component clientTooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_language_client");
        Component serverTooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_language_server");
        builder.suggest("client", clientTooltip.toTextComponent());
        builder.suggest("server", serverTooltip.toTextComponent());
        I18nUtils.getI18nFiles().forEach(builder::suggest);
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_LANGUAGE.get())
                .then(Commands.argument("language", StringArgumentType.word())
                        .suggests(LanguageCommand::suggestion)
                        .executes(LanguageCommand::execute)
                );
    }
}
