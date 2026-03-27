package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpCoordinateCommand {
    private TpCoordinateCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_COORDINATE)) return 0;
        SafeWorldCoordinate safeWorldCoordinate;
        try {
            Vector3d pos = Vec3Argument.getCoordinates(context, "coordinate").getPosition(context.getSource());
            RegistryKey<World> targetLevel;
            try {
                targetLevel = DimensionArgument.getDimension(context, "dimension").dimension();
            } catch (IllegalArgumentException ignored) {
                targetLevel = player.getLevel().dimension();
            }
            safeWorldCoordinate = new SafeWorldCoordinate(pos.x(), pos.y(), pos.z(), player.yRot, player.xRot, targetLevel);
        } catch (IllegalArgumentException ignored) {
            ServerPlayerEntity target = EntityArgument.getPlayer(context, "player");
            safeWorldCoordinate = new SafeWorldCoordinate(target.getX(), target.getY(), target.getZ(), target.yRot, target.xRot, target.getLevel().dimension());
        }
        safeWorldCoordinate.safe("safe".equalsIgnoreCase(xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, "safe")));
        if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_COORDINATE, true))
            return 0;
        NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_COORDINATE);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
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
