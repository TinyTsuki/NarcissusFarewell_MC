package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
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
        SafeWorldCoordinate safeWorldCoordinate = new SafeWorldCoordinate(player);
        BlockPos respawnPosition = player.level().getSharedSpawnPos();
        safeWorldCoordinate.dimension(player.level().dimension());
        if (respawnPosition == null) {
            respawnPosition = player.getServer().getLevel(Level.OVERWORLD).getSharedSpawnPos();
            safeWorldCoordinate.dimension(Level.OVERWORLD);
        }
        safeWorldCoordinate.fromBlockPos(respawnPosition);
        safeWorldCoordinate.safe("safe".equalsIgnoreCase(xin.vanilla.banira.common.util.CommandUtils.getStringDefault(context, "safe", "safe")));
        if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_WORLD_SPAWN, true)) return 0;
        NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_WORLD_SPAWN);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandTpWorldSpawn())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_WORLD_SPAWN))
                .executes(TpWorldSpawnCommand::execute)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(CommandUtils::safeSuggestion)
                        .executes(TpWorldSpawnCommand::execute)
                );
    }
}
