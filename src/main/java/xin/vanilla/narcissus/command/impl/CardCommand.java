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
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
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
        String type = xin.vanilla.banira.common.util.CommandUtils.getStringDefault(context, "type", "get");
        ServerPlayerEntity target = xin.vanilla.banira.common.util.CommandUtils.getPlayerOrSelf(context, "player");
        int num = xin.vanilla.banira.common.util.CommandUtils.getIntDefault(context, "num", 0);
        PlayerTeleportData data = PlayerTeleportData.getData(target);
        switch (type) {
            case "set":
                data.setTeleportCard(num);
                PlayerTeleportData.syncPlayerData(target);
                break;
            case "add":
                data.plusTeleportCard(num);
                PlayerTeleportData.syncPlayerData(target);
                break;
            case "get":
                break;
            default:
                throw new IllegalArgumentException("Type " + type + " is not supported");
        }
        Component component = NarcissusComponent.get().transAuto("player_card"
                , target.getDisplayName().getString()
                , data.getTeleportCard());
        MessageUtils.sendMessage(source, true, component);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandCard())
                .executes(CardCommand::execute)
                .then(Commands.argument("type", StringArgumentType.word())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.SET_CARD))
                        .suggests((context, builder) -> {
                            String lang = xin.vanilla.banira.common.util.CommandUtils.getLanguage(context.getSource());
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
