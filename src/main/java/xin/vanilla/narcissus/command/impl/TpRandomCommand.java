package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpRandomCommand {
    private TpRandomCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_RANDOM)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        int range = CommandUtils.getIntDefault(context, "range", CommonConfig.get().general().teleportRandomDistanceLimit());
        range = NarcissusUtils.checkRange(player, EnumTeleportType.TP_RANDOM, range);
        RegistryKey<World> targetLevel = CommandUtils.getDimensionKeyDefault(context, "dimension", player.getLevel().dimension());
        SafeWorldCoordinate safeWorldCoordinate = SafeWorldCoordinate.random(player, range, targetLevel).safe(true);
        if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_RANDOM, true)) return 0;
        NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_RANDOM);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandTpRandom())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_RANDOM))
                .executes(TpRandomCommand::execute)
                .then(Commands.argument("range", IntegerArgumentType.integer(1))
                        .suggests(CommandUtils::rangeSuggestion)
                        .executes(TpRandomCommand::execute)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(CommandUtils::safeSuggestion)
                                .executes(TpRandomCommand::execute)
                                .then(Commands.argument("dimension", DimensionArgument.dimension())
                                        .executes(TpRandomCommand::execute)
                                )
                        )
                );
    }
}
