package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpWorldSpawnCommand {
    private TpWorldSpawnCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_WORLD_SPAWN)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        SafeWorldCoordinate safeWorldCoordinate = new SafeWorldCoordinate(player);
        BlockPos respawnPosition = player.getLevel().getSharedSpawnPos();
        safeWorldCoordinate.dimension(player.getLevel().dimension());
        if (respawnPosition == null) {
            respawnPosition = player.getServer().getLevel(World.OVERWORLD).getSharedSpawnPos();
            safeWorldCoordinate.dimension(World.OVERWORLD);
        }
        safeWorldCoordinate.fromBlockPos(respawnPosition);
        safeWorldCoordinate.safe("safe".equalsIgnoreCase(CommandUtils.getStringDefault(context, "safe", "safe")));
        if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_WORLD_SPAWN, true)) return 0;
        NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_WORLD_SPAWN);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandTpWorldSpawn())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_WORLD_SPAWN))
                .executes(TpWorldSpawnCommand::execute)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(CommandUtils::safeSuggestion)
                        .executes(TpWorldSpawnCommand::execute)
                );
    }
}
