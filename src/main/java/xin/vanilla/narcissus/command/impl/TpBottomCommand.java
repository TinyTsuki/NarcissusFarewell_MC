package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
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

public final class TpBottomCommand {
    private TpBottomCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_BOTTOM)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        SafeWorldCoordinate safeWorldCoordinate = NarcissusUtils.findBottomCandidate(player.getLevel(), new SafeWorldCoordinate(player));
        if (safeWorldCoordinate == null) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("tp_bottom_not_found"), NarcissusNotificationTypes.TELEPORT_ERROR);
            return 0;
        }
        safeWorldCoordinate.safe("safe".equalsIgnoreCase(xin.vanilla.banira.common.util.CommandUtils.getStringDefault(context, "safe", "safe"))).safeMode(EnumSafeMode.Y_B_TO_C);
        if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_BOTTOM, true)) return 0;
        NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_BOTTOM);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandTpBottom())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_BOTTOM))
                .executes(TpBottomCommand::execute)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(CommandUtils::safeSuggestion)
                        .executes(TpBottomCommand::execute)
                );
    }
}
