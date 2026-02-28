package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumSafeMode;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpUpCommand {
    private TpUpCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_UP)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        Coordinate coordinate = NarcissusUtils.findUpCandidate(player.getLevel(), new Coordinate(player));
        if (coordinate == null) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "tp_up_not_found"));
            return 0;
        }
        coordinate.safe("safe".equalsIgnoreCase(CommandUtils.getStringDefault(context, "safe", "safe"))).safeMode(EnumSafeMode.Y_C_TO_T);
        if (CommandUtils.checkTeleportPost(player, coordinate, EnumTeleportType.TP_UP, true)) return 0;
        NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_UP);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.COMMAND_TP_UP.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_UP))
                .executes(TpUpCommand::execute)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(CommandUtils::safeSuggestion)
                        .executes(TpUpCommand::execute)
                );
    }
}
