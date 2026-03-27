package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpViewCommand {
    private TpViewCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_VIEW)) return 0;
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean safe = "safe".equalsIgnoreCase(xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, "safe"));
        int range = xin.vanilla.banira.common.util.CommandUtils.getIntDefault(context, "range", CommonConfig.get().general().teleportViewDistanceLimit());
        range = NarcissusUtils.checkRange(player, EnumTeleportType.TP_VIEW, range);
        MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("tp_view_searching"));
        int finalRange = range;
        new Thread(() -> {
            SafeWorldCoordinate safeWorldCoordinate = NarcissusUtils.findViewEndCandidate(player, safe, finalRange);
            if (safeWorldCoordinate == null) {
                MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto(safe ? "tp_view_safe_not_found" : "tp_view_not_found"));
                return;
            }
            safeWorldCoordinate.safeMode(EnumSafeMode.Y_C_OFFSET_3);
            if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_VIEW, true)) return;
            player.server.submit(() -> NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_VIEW));
        }).start();
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandTpView())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_VIEW))
                .executes(TpViewCommand::execute)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(CommandUtils::safeSuggestion)
                        .executes(TpViewCommand::execute)
                        .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                .suggests(CommandUtils::rangeSuggestion)
                                .executes(TpViewCommand::execute)
                        )
                );
    }
}
