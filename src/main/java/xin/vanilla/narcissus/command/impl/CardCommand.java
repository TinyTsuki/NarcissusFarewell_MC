package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class CardCommand {
    private CardCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.CARD)) return 0;
        CommandSourceStack source = context.getSource();
        String type = CommandUtils.getStringDefault(context, "type", "get");
        ServerPlayer target = CommandUtils.getPlayerOrSelf(context, "player");
        int num = CommandUtils.getIntDefault(context, "num", 0);
        String language = CommandUtils.getLanguage(source);
        PlayerTeleportData data = PlayerTeleportData.getData(target);
        switch (type) {
            case "set":
                data.setTeleportCard(num);
                break;
            case "add":
                data.plusTeleportCard(num);
                break;
            case "get":
                break;
            default:
                throw new IllegalArgumentException("Type " + type + " is not supported");
        }
        Component component = NarcissusComponent.get().transAuto("player_card"
                , target.getDisplayName().getString()
                , data.getTeleportCard());
        source.sendSuccess(component.toChat(language), false);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandCard())
                .executes(CardCommand::execute)
                .then(Commands.argument("type", StringArgumentType.word())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.SET_CARD))
                        .suggests((context, builder) -> {
                            String lang = CommandUtils.getLanguage(context.getSource());
                            builder.suggest("get", NarcissusComponent.get().transAuto("suggest_card_get").toVanilla(lang));
                            builder.suggest("add", NarcissusComponent.get().transAuto("suggest_card_add").toVanilla(lang));
                            builder.suggest("set", NarcissusComponent.get().transAuto("suggest_card_set").toVanilla(lang));
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(CardCommand::execute)
                                .then(Commands.argument("num", IntegerArgumentType.integer())
                                        .suggests((context, builder) -> {
                                            builder.suggest(-5);
                                            builder.suggest(-1);
                                            builder.suggest(1);
                                            builder.suggest(5);
                                            builder.suggest(10);
                                            builder.suggest(20);
                                            return builder.buildFuture();
                                        })
                                        .executes(CardCommand::execute)
                                )
                        )
                );
    }
}
