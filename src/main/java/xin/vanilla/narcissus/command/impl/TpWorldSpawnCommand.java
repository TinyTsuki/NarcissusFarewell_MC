package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpWorldSpawnCommand {
    private TpWorldSpawnCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_WORLD_SPAWN)) return 0;
        ServerPlayer player = context.getSource().getPlayerOrException();
        Coordinate coordinate = new Coordinate(player);
        BlockPos respawnPosition = player.level().getRespawnData().pos();
        coordinate.dimension(player.level().dimension());
        if (respawnPosition == null) {
            respawnPosition = player.level().getServer().getLevel(Level.OVERWORLD).getRespawnData().pos();
            coordinate.dimension(Level.OVERWORLD);
        }
        coordinate.fromBlockPos(respawnPosition);
        coordinate.safe("safe".equalsIgnoreCase(CommandUtils.getStringDefault(context, "safe", "safe")));
        if (CommandUtils.checkTeleportPost(player, coordinate, EnumTeleportType.TP_WORLD_SPAWN, true)) return 0;
        NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_WORLD_SPAWN);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_TP_WORLD_SPAWN.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_WORLD_SPAWN))
                .executes(TpWorldSpawnCommand::execute)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(CommandUtils::safeSuggestion)
                        .executes(TpWorldSpawnCommand::execute)
                );
    }
}
