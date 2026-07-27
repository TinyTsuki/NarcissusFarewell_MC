package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpSpawnCommand {
    private TpSpawnCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_SPAWN)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        ServerPlayerEntity target = xin.vanilla.banira.common.util.CommandUtils.getPlayerOptional(context, "player");
        if (target == null) target = player;
        SafeWorldCoordinate safeWorldCoordinate = new SafeWorldCoordinate(target);
        BlockPos respawnPosition = target.getRespawnPosition();
        safeWorldCoordinate.dimension(target.getRespawnDimension());
        if (respawnPosition == null) {
            String raw = CommonConfig.get().base().teleportLimit().tpSpawnNoBedWorldDimension();
            String s = raw == null ? "" : raw.trim();
            ServerWorld level;
            if (s.isEmpty() || "CURRENT".equalsIgnoreCase(s) || "AUTO".equalsIgnoreCase(s)) {
                level = target.getLevel();
            } else {
                level = DimensionUtils.getLevel(s);
            }
            if (level == null) {
                level = target.getLevel();
            }
            respawnPosition = level.getSharedSpawnPos();
            safeWorldCoordinate.dimension(level.dimension());
        }
        safeWorldCoordinate.fromBlockPos(respawnPosition);
        safeWorldCoordinate.safe("safe".equalsIgnoreCase(xin.vanilla.banira.common.util.CommandUtils.getStringDefault(context, "safe", "safe")));
        if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_SPAWN, true)) return 0;
        NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_SPAWN);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().command().commandTpSpawn())
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
