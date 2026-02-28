package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.concurrent.CompletableFuture;

public final class FlyCommand {
    private FlyCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        CommandSource source = context.getSource();
        if (CommandUtils.checkTeleportPre(source, EnumCommandType.FLY)) return 0;
        ServerPlayerEntity target = CommandUtils.getPlayerOrSelf(context, "player");
        Boolean enable = CommandUtils.getBooleanOptional(context, "enable");
        Double speedDouble = CommandUtils.getDoubleOptional(context, "speed");
        Float speed = speedDouble != null ? speedDouble.floatValue() : null;
        NarcissusUtils.setPlayerFlightMode(target, enable, speed);
        return 1;
    }

    public static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        Double min = CommonConfig.FLY_SPEED_MIN.get();
        Double max = CommonConfig.FLY_SPEED_MAX.get();
        builder.suggest(String.valueOf(min));
        if (0.05F >= min && 0.05F <= max) {
            builder.suggest("0.05");
        }
        builder.suggest(String.valueOf(max));
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.COMMAND_FLY.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.FLY))
                .executes(FlyCommand::execute)
                .then(Commands.argument("enable", BoolArgumentType.bool())
                        .executes(FlyCommand::execute)
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(FlyCommand::execute)
                                .then(Commands.argument("speed", DoubleArgumentType.doubleArg(CommonConfig.FLY_SPEED_MIN.get(), CommonConfig.FLY_SPEED_MAX.get()))
                                        .suggests(FlyCommand::suggestion)
                                        .executes(FlyCommand::execute)
                                )
                        )
                );
    }
}
