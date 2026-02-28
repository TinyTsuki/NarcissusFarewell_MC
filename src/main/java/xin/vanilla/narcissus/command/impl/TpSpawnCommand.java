package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpSpawnCommand {
    private TpSpawnCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_SPAWN)) return 0;
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer target = CommandUtils.getPlayerOptional(context, "player");
        if (target == null) target = player;
        Coordinate coordinate = new Coordinate(target);
        BlockPos respawnPosition = target.getRespawnPosition();
        coordinate.dimension(target.getRespawnDimension());
        if (respawnPosition == null) {
            respawnPosition = target.getLevel().getSharedSpawnPos();
            coordinate.dimension(target.getLevel().dimension());
        }
        if (respawnPosition == null) {
            respawnPosition = target.getServer().getLevel(Level.OVERWORLD).getSharedSpawnPos();
            coordinate.dimension(Level.OVERWORLD);
        }
        coordinate.fromBlockPos(respawnPosition);
        coordinate.safe("safe".equalsIgnoreCase(CommandUtils.getStringDefault(context, "safe", "safe")));
        if (CommandUtils.checkTeleportPost(player, coordinate, EnumTeleportType.TP_SPAWN, true)) return 0;
        NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_SPAWN);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_TP_SPAWN.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_SPAWN))
                .executes(TpSpawnCommand::execute)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(CommandUtils::safeSuggestion)
                        .executes(TpSpawnCommand::execute)
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_SPAWN_OTHER))
                                .executes(TpSpawnCommand::execute)
                        )
                );
    }
}
