package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
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

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        CommandSource source = context.getSource();
        if (CommandUtils.checkTeleportPre(source, EnumCommandType.FEED)) return 0;
        if (source.getEntity() == null) {
            List<ServerPlayerEntity> targetList = new ArrayList<>(xin.vanilla.banira.common.util.CommandUtils.getPlayersOptional(context, "player", new ArrayList<>()));
            if (targetList.isEmpty()) throw CommandSource.ERROR_NOT_PLAYER.create();
            for (ServerPlayerEntity target : targetList) {
                NarcissusUtils.killPlayer(null, target);
            }
        } else if (source.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = source.getPlayerOrException();
            if (!CommonConfig.get().featureSwitch().switchFeed()) {
                MessageUtils.sendDefaultNotification(player, NarcissusComponent.get().transAuto("command_disabled"), EnumPosition.TOP_CENTER, EnumMoveType.AUTO);
                return 0;
            }
            List<ServerPlayerEntity> targetList = new ArrayList<>(xin.vanilla.banira.common.util.CommandUtils.getPlayersOptional(context, "player", Collections.singletonList(player)));
            for (ServerPlayerEntity target : targetList) {
                NarcissusUtils.killPlayer(player, target);
            }
        }
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandFeed())
                .executes(FeedCommand::execute)
                .then(Commands.argument("player", EntityArgument.players())
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.FEED_OTHER))
                        .executes(FeedCommand::execute)
                );
    }
}
