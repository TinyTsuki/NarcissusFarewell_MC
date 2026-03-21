package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class CardCommand {
    private CardCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.CARD)) return 0;
        CommandSource source = context.getSource();
        String type = CommandUtils.getStringDefault(context, "type", "get");
        ServerPlayerEntity target = CommandUtils.getPlayerOrSelf(context, "player");
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
        Component component = NarcissusLang.transLangAuto(language, "player_card"
                , target.getDisplayName().getString()
                , data.getTeleportCard());
        source.sendSuccess(component.toChat(language), false);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandCard())
                .executes(CardCommand::execute)
                .then(Commands.argument("type", StringArgumentType.word())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.SET_CARD))
                        .suggests((context, builder) -> {
                            String lang = CommandUtils.getLanguage(context.getSource());
                            builder.suggest("get", NarcissusLang.transLangAuto(lang, "suggest_card_get").toVanilla());
                            builder.suggest("add", NarcissusLang.transLangAuto(lang, "suggest_card_add").toVanilla());
                            builder.suggest("set", NarcissusLang.transLangAuto(lang, "suggest_card_set").toVanilla());
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
