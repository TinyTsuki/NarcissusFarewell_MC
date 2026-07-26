package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpCoordinateCommand {
    private TpCoordinateCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_COORDINATE)) return 0;
        SafeWorldCoordinate safeWorldCoordinate;
        try {
            Vec3 pos = Vec3Argument.getCoordinates(context, "coordinate").getPosition(context.getSource());
            ResourceKey<Level> targetLevel;
            try {
                targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
            } catch (IllegalArgumentException ignored) {
                targetLevel = player.getLevel().dimension();
            }
            safeWorldCoordinate = new SafeWorldCoordinate(pos.x(), pos.y(), pos.z(), player.yRot, player.xRot, targetLevel);
        } catch (IllegalArgumentException ignored) {
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            safeWorldCoordinate = new SafeWorldCoordinate(target.getX(), target.getY(), target.getZ(), target.yRot, target.xRot, target.getLevel().dimension());
        }
        safeWorldCoordinate.safe("safe".equalsIgnoreCase(xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, "safe")));
        if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_COORDINATE, true))
            return 0;
        NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_COORDINATE);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandTpCoordinate())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_COORDINATE))
                .then(Commands.argument("coordinate", Vec3Argument.vec3())
                        .executes(TpCoordinateCommand::execute)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(CommandUtils::safeSuggestion)
                                .executes(TpCoordinateCommand::execute)
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(TpCoordinateCommand::execute)
                                )
                        )
                )
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TpCoordinateCommand::execute)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(CommandUtils::safeSuggestion)
                                .executes(TpCoordinateCommand::execute)
                        )
                );
    }
}
