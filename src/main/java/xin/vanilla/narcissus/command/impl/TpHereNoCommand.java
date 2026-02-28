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

public final class TpHereNoCommand {
    private TpHereNoCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_HERE_YES)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        String id = CommandUtils.getRequestId(context, EnumTeleportType.TP_HERE, true);
        if (StringUtils.isNullOrEmpty(id) || !NarcissusFarewell.getTeleportRequest().containsKey(id)) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "tp_here_not_found"));
            return 0;
        }
        TeleportRequest request = NarcissusFarewell.getTeleportRequest().remove(id);
        NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EnumI18nType.FORMAT, "tp_here_rejected"), request.getTarget().getDisplayName().getString());
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.COMMAND_TP_HERE_NO.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HERE_NO))
                .executes(TpHereNoCommand::execute)
                .then(Commands.argument("requestIndex", IntegerArgumentType.integer(1))
                        .suggests(CommandUtils.buildReqIndexSuggestions(EnumTeleportType.TP_HERE, true))
                        .executes(TpHereNoCommand::execute)
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TpHereNoCommand::execute)
                )
                .then(Commands.argument("requestId", StringArgumentType.word())
                        .executes(TpHereNoCommand::execute)
                );
    }
}
