package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.concurrent.CompletableFuture;

public final class FlyCommand {
    private FlyCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        CommandSourceStack source = context.getSource();
        if (CommandUtils.checkTeleportPre(source, EnumCommandType.FLY)) return 0;
        ServerPlayer target = CommandUtils.getPlayerOrSelf(context, "player");
        Boolean enable = CommandUtils.getBooleanOptional(context, "enable");
        Double speedDouble = CommandUtils.getDoubleOptional(context, "speed");
        Float speed = speedDouble != null ? speedDouble.floatValue() : null;
        NarcissusUtils.setPlayerFlightMode(target, enable, speed);
        return 1;
    }

    public static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        Double min = CommonConfig.get().base().flySpeedMin();
        Double max = CommonConfig.get().base().flySpeedMax();
        builder.suggest(String.valueOf(min));
        if (0.05F >= min && 0.05F <= max) {
            builder.suggest("0.05");
        }
        builder.suggest(String.valueOf(max));
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandFly())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.FLY))
                .executes(FlyCommand::execute)
                .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(FlyCommand::execute)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(FlyCommand::execute)
                                .then(Commands.argument("speed", DoubleArgumentType.doubleArg(CommonConfig.get().base().flySpeedMin(), CommonConfig.get().base().flySpeedMax()))
                                        .suggests(FlyCommand::suggestion)
                                        .executes(FlyCommand::execute)
                                )
                        )
                );
    }
}
