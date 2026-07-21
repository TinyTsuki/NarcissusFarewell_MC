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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumMCColor;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.NumberUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.command.NarcissusCommand;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class HelpCommand {
    private HelpCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String lang = NarcissusLang.getPlayerLanguage(player);
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
            int pages = (int) Math.ceil((double) NarcissusCommand.HELP_MESSAGE.size() / CommonConfig.get().general().helpInfoNumPerPage());
            helpInfo = NarcissusComponent.get().literal(StringUtils.format(CommonConfig.get().general().helpHeader() + "\n", page, pages));
            for (int i = 0; (page - 1) * CommonConfig.get().general().helpInfoNumPerPage() + i < NarcissusCommand.HELP_MESSAGE.size() && i < CommonConfig.get().general().helpInfoNumPerPage(); i++) {
                KeyValue<String, EnumCommandType> keyValue = NarcissusCommand.HELP_MESSAGE.get((page - 1) * CommonConfig.get().general().helpInfoNumPerPage() + i);
                Component commandTips;
                if (keyValue.value().name().toLowerCase().contains("concise")) {
                    commandTips = NarcissusComponent.get().transAuto("concise", NarcissusUtils.getCommand(keyValue.value().replaceConcise()));
                } else {
                    commandTips = NarcissusComponent.get().transAuto(keyValue.value().name().toLowerCase());
                }
                commandTips.color(EnumMCColor.GRAY.getColor());
                String com = "/" + keyValue.key();
                helpInfo.append(NarcissusComponent.get().literal(com)
                                .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, com))
                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT
                                        , NarcissusComponent.get().transAuto("click_to_suggest").toVanilla(lang)))
                        )
                        .append(NarcissusComponent.get().literal(" -> ").color(EnumMCColor.YELLOW.getColor()))
                        .append(commandTips);
                if (i != NarcissusCommand.HELP_MESSAGE.size() - 1) {
                    helpInfo.append("\n");
                }
            }
            // 添加翻页按钮
            if (pages > 1) {
                helpInfo.append("\n");
                Component prevButton = NarcissusComponent.get().literal("<<< ");
                if (page > 1) {
                    prevButton.color(EnumMCColor.AQUA.getColor())
                            .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/%s %s %d", NarcissusUtils.getCommandPrefix(), "help", page - 1)))
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    NarcissusComponent.get().transAuto("previous_page").toVanilla(lang)));
                } else {
                    prevButton.color(EnumMCColor.DARK_AQUA.getColor());
                }
                helpInfo.append(prevButton);

                helpInfo.append(NarcissusComponent.get().literal(String.format(" %s/%s "
                                , StringUtils.padOptimizedLeft(page, String.valueOf(pages).length(), " ")
                                , pages))
                        .color(EnumMCColor.WHITE.getColor()));

                Component nextButton = NarcissusComponent.get().literal(" >>>");
                if (page < pages) {
                    nextButton.color(EnumMCColor.AQUA.getColor())
                            .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    String.format("/%s %s %d", NarcissusUtils.getCommandPrefix(), "help", page + 1)))
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    NarcissusComponent.get().transAuto("next_page").toVanilla(lang)));
                } else {
                    nextButton.color(EnumMCColor.DARK_AQUA.getColor());
                }
                helpInfo.append(nextButton);
            }
        } else {
            EnumCommandType type = EnumCommandType.valueOf(command);
            helpInfo = NarcissusComponent.get().empty();
            String com = "/" + NarcissusUtils.getCommand(type);
            helpInfo.append(NarcissusComponent.get().literal(com)
                            .clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, com))
                            .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT
                                    , NarcissusComponent.get().transAuto("click_to_suggest").toVanilla(lang)))
                    )
                    .append("\n")
                    .append(NarcissusComponent.get().transAuto(command.toLowerCase() + "_detail").color(EnumMCColor.GRAY.getColor()));
        }
        MessageUtils.sendNotification(player, helpInfo, NarcissusNotificationTypes.INTERACTIVE_HELP);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String input = CommandUtils.getStringEmpty(context, "command");
        boolean isInputEmpty = StringUtils.isNullOrEmpty(input);
        int totalPages = (int) Math.ceil((double) NarcissusCommand.HELP_MESSAGE.size() / CommonConfig.get().general().helpInfoNumPerPage());
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

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("help")
                .executes(HelpCommand::execute)
                .then(Commands.argument("command", StringArgumentType.word())
                        .suggests(HelpCommand::suggestion)
                        .executes(HelpCommand::execute)
                );
    }
}
