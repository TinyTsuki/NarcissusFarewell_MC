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
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;
import xin.vanilla.narcissus.util.StringUtils;

public final class TpHereCancelCommand {
    private TpHereCancelCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_HERE_CANCEL)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        String id = CommandUtils.getRequestId(context, EnumTeleportType.TP_HERE, false);
        if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "tp_here_not_found"));
            return 0;
        }
        TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
        NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EnumI18nType.FORMAT, "tp_here_cancelled"), request.getRequester().getDisplayName().getString());
        if (!request.isIgnore() && !request.getRequester().getUUID().equals(request.getTarget().getUUID())) {
            NarcissusUtils.sendTranslatableMessage(request.getTarget(), I18nUtils.getKey(EnumI18nType.FORMAT, "tp_here_cancelled"), request.getRequester().getDisplayName().getString());
        }
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.COMMAND_TP_HERE_CANCEL.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HERE_CANCEL))
                .executes(TpHereCancelCommand::execute)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(CommandUtils.buildReqIndexSuggestions(EnumTeleportType.TP_HERE, false))
                        .executes(TpHereCancelCommand::execute)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TpHereCancelCommand::execute)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .executes(TpHereCancelCommand::execute)
                );
    }
}
