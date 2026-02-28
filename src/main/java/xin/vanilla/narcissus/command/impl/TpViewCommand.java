package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumSafeMode;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.Component;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpViewCommand {
    private TpViewCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_VIEW)) return 0;
        ServerPlayer player = context.getSource().getPlayerOrException();
        boolean safe = "safe".equalsIgnoreCase(CommandUtils.getStringEmpty(context, "safe"));
        int range = CommandUtils.getIntDefault(context, "range", ServerConfig.TELEPORT_VIEW_DISTANCE_LIMIT.get());
        range = NarcissusUtils.checkRange(player, EnumTeleportType.TP_VIEW, range);
        NarcissusUtils.sendActionBarMessage(player, Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, "tp_view_searching"));
        int finalRange = range;
        new Thread(() -> {
            Coordinate coordinate = NarcissusUtils.findViewEndCandidate(player, safe, finalRange);
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, safe ? "tp_view_safe_not_found" : "tp_view_not_found"));
                return;
            }
            coordinate.safeMode(EnumSafeMode.Y_C_OFFSET_3);
            if (CommandUtils.checkTeleportPost(player, coordinate, EnumTeleportType.TP_VIEW, true)) return;
            player.level().getServer().submit(() -> NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_VIEW));
        }).start();
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_TP_VIEW.get())
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
