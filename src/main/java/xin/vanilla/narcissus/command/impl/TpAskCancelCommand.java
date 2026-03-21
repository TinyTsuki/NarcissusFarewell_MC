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
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpAskCancelCommand {
    private TpAskCancelCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_ASK_CANCEL)) return 0;
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = CommandUtils.getRequestId(context, EnumTeleportType.TP_ASK, false);
        if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("tp_ask_not_found"));
            return 0;
        }
        TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
        MessageUtils.sendNotification(request.getRequester(), NarcissusComponent.get().transAuto("tp_ask_cancelled", request.getRequester().getDisplayName().getString()));
        if (!request.isIgnore() && !request.getRequester().getUUID().equals(request.getTarget().getUUID())) {
            MessageUtils.sendNotification(request.getTarget(), NarcissusComponent.get().transAuto("tp_ask_cancelled", request.getRequester().getDisplayName().getString()));
        }
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandTpAskCancel())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_ASK_CANCEL))
                .executes(TpAskCancelCommand::execute)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(CommandUtils.buildReqIndexSuggestions(EnumTeleportType.TP_ASK, false))
                        .executes(TpAskCancelCommand::execute)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TpAskCancelCommand::execute)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .executes(TpAskCancelCommand::execute)
                );
    }
}
