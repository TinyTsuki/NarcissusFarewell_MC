package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FeedCommand {
    private FeedCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        CommandSourceStack source = context.getSource();
        if (CommandUtils.checkTeleportPre(source, EnumCommandType.FEED)) return 0;
        if (source.getEntity() == null) {
            List<ServerPlayer> targetList = new ArrayList<>(xin.vanilla.banira.common.util.CommandUtils.getPlayersOptional(context, "player", new ArrayList<>()));
            if (targetList.isEmpty()) throw CommandSourceStack.ERROR_NOT_PLAYER.create();
            for (ServerPlayer target : targetList) {
                NarcissusUtils.killPlayer(null, target);
            }
        } else if (source.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = source.getPlayerOrException();
            if (!CommonConfig.get().featureSwitch().switchFeed()) {
                MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("command_disabled"));
                return 0;
            }
            List<ServerPlayer> targetList = new ArrayList<>(xin.vanilla.banira.common.util.CommandUtils.getPlayersOptional(context, "player", Collections.singletonList(player)));
            for (ServerPlayer target : targetList) {
                NarcissusUtils.killPlayer(player, target);
            }
        }
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandFeed())
                .executes(FeedCommand::execute)
                .then(Commands.argument("player", EntityArgument.players())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.FEED_OTHER))
                        .executes(FeedCommand::execute)
                );
    }
}
