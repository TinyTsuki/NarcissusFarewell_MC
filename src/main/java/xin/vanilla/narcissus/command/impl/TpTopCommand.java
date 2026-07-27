package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumSafeMode;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpTopCommand {
    private TpTopCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_TOP)) return 0;
        ServerPlayer player = context.getSource().getPlayerOrException();
        SafeWorldCoordinate safeWorldCoordinate = NarcissusUtils.findTopCandidate(player, new SafeWorldCoordinate(player));
        if (safeWorldCoordinate == null) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("tp_top_not_found"), NarcissusNotificationTypes.TELEPORT_ERROR);
            return 0;
        }
        safeWorldCoordinate.safe("safe".equalsIgnoreCase(xin.vanilla.banira.common.util.CommandUtils.getStringDefault(context, "safe", "safe"))).safeMode(EnumSafeMode.Y_T_TO_C);
        if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_TOP, true)) return 0;
        NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_TOP);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().command().commandTpTop())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_TOP))
                .executes(TpTopCommand::execute)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(CommandUtils::safeSuggestion)
                        .executes(TpTopCommand::execute)
                );
    }
}
