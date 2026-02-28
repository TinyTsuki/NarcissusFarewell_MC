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
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import xin.vanilla.narcissus.command.NarcissusCommand;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumMCColor;
import xin.vanilla.narcissus.util.*;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class HelpCommand {
    private HelpCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        String command;
        int page;
        try {
            command = StringArgumentType.getString(context, "command");
            page = NumberUtils.toInt(command);
        } catch (IllegalArgumentException ignored) {
            command = "";
            page = 1;
        }
        Component helpInfo;
        if (page > 0) {
            int pages = (int) Math.ceil((double) NarcissusCommand.HELP_MESSAGE.size() / ServerConfig.HELP_INFO_NUM_PER_PAGE.get());
            helpInfo = Component.literal(StringUtils.format(ServerConfig.HELP_HEADER.get() + "\n", page, pages));
            for (int i = 0; (page - 1) * ServerConfig.HELP_INFO_NUM_PER_PAGE.get() + i < NarcissusCommand.HELP_MESSAGE.size() && i < ServerConfig.HELP_INFO_NUM_PER_PAGE.get(); i++) {
                KeyValue<String, EnumCommandType> keyValue = NarcissusCommand.HELP_MESSAGE.get((page - 1) * ServerConfig.HELP_INFO_NUM_PER_PAGE.get() + i);
                Component commandTips;
                if (keyValue.value().name().toLowerCase().contains("concise")) {
                    commandTips = Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, "concise", NarcissusUtils.getCommand(keyValue.value().replaceConcise()));
                } else {
                    commandTips = Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, keyValue.value().name().toLowerCase());
                }
                commandTips.color(EnumMCColor.GRAY.getColor());
                String com = "/" + keyValue.key();
                helpInfo.append(Component.literal(com)
                                .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, com))
                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT
                                        , Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, "click_to_suggest").toTextComponent()))
                        )
                        .append(new Component(" -> ").color(EnumMCColor.YELLOW.getColor()))
                        .append(commandTips);
                if (i != NarcissusCommand.HELP_MESSAGE.size() - 1) {
                    helpInfo.append("\n");
                }
            }
            // 添加翻页按钮
            if (pages > 1) {
                helpInfo.append("\n");
                Component prevButton = Component.literal("<<< ");
                if (page > 1) {
                    prevButton.color(EnumMCColor.AQUA.getColor())
                            .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/%s %s %d", NarcissusUtils.getCommandPrefix(), "help", page - 1)))
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, "previous_page").toTextComponent()));
                } else {
                    prevButton.color(EnumMCColor.DARK_AQUA.getColor());
                }
                helpInfo.append(prevButton);

                helpInfo.append(Component.literal(String.format(" %s/%s "
                                , StringUtils.padOptimizedLeft(page, String.valueOf(pages).length(), " ")
                                , pages))
                        .color(EnumMCColor.WHITE.getColor()));

                Component nextButton = Component.literal(" >>>");
                if (page < pages) {
                    nextButton.color(EnumMCColor.AQUA.getColor())
                            .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/%s %s %d", NarcissusUtils.getCommandPrefix(), "help", page + 1)))
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, "next_page").toTextComponent()));
                } else {
                    nextButton.color(EnumMCColor.DARK_AQUA.getColor());
                }
                helpInfo.append(nextButton);
            }
        } else {
            EnumCommandType type = EnumCommandType.valueOf(command);
            helpInfo = Component.empty();
            String com = "/" + NarcissusUtils.getCommand(type);
            helpInfo.append(Component.literal(com)
                            .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, com))
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT
                                    , Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, "click_to_suggest").toTextComponent()))
                    )
                    .append("\n")
                    .append(Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, command.toLowerCase() + "_detail").color(EnumMCColor.GRAY.getColor()));
        }
        NarcissusUtils.sendMessage(player, helpInfo);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String input = CommandUtils.getStringEmpty(context, "command");
        boolean isInputEmpty = StringUtils.isNullOrEmpty(input);
        int totalPages = (int) Math.ceil((double) NarcissusCommand.HELP_MESSAGE.size() / ServerConfig.HELP_INFO_NUM_PER_PAGE.get());
        for (int i = 0; i < totalPages && isInputEmpty; i++) {
            builder.suggest(i + 1);
        }
        for (EnumCommandType type : Arrays.stream(EnumCommandType.values())
                .filter(type -> type != EnumCommandType.HELP)
                .filter(type -> !type.isIgnore())
                .filter(type -> !type.name().toLowerCase().contains("concise"))
                .filter(type -> isInputEmpty || type.name().toLowerCase().contains(input.toLowerCase()))
                .sorted(Comparator.comparing(EnumCommandType::getSort))
                .collect(Collectors.toList())) {
            builder.suggest(type.name());
        }
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal("help")
                .executes(HelpCommand::execute)
                .then(Commands.argument("command", StringArgumentType.word())
                        .suggests(HelpCommand::suggestion)
                        .executes(HelpCommand::execute)
                );
    }
}
