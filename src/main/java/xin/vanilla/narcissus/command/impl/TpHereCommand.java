package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;
import java.util.Date;

public final class TpHereCommand {
    private TpHereCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_HERE)) return 0;
        ServerPlayer player = context.getSource().getPlayerOrException();
        ServerPlayer target;
        try {
            target = EntityArgument.getPlayer(context, "player");
        } catch (IllegalArgumentException ignored) {
            target = NarcissusFarewell.getTeleportRequest().values().stream()
                    .filter(request -> request.getRequester().getUUID().equals(player.getUUID()))
                    .filter(request -> {
                        Player entity = request.getTarget();
                        return NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, EnumTeleportType.TP_HERE)
                                || entity != null && entity.level().dimension() == player.level().dimension();
                    })
                    .max(Comparator.comparing(TeleportRequest::getRequestTime))
                    .orElse(new TeleportRequest().setTarget(NarcissusFarewell.getLastTeleportRequest()
                            .getOrDefault(player, NarcissusUtils.getRandomPlayer())))
                    .getTarget();
        }
        if (target == null) {
            MessageUtils.sendMessage(player, NarcissusComponent.get().transAuto("player_not_found"));
            return 0;
        }
        TeleportRequest request = new TeleportRequest()
                .setRequester(player)
                .setTarget(target)
                .setTeleportType(EnumTeleportType.TP_HERE)
                .setRequestTime(new Date());
        request.setSafe("safe".equalsIgnoreCase(CommandUtils.getStringEmpty(context, "safe")));
        if (CommandUtils.checkTeleportPost(request)) return 0;
        PlayerAccess targetAccess = PlayerTeleportData.getData(target).getAccess();
        String playerUUIDString = PlayerUtils.getPlayerUUIDString(player);
        boolean ignore = targetAccess.getBlackList().contains(playerUUIDString)
                || (!targetAccess.getWhiteList().isEmpty() && !targetAccess.getWhiteList().contains(playerUUIDString));
        boolean autoAccept = !ignore && targetAccess.getAutoTphList().contains(playerUUIDString);
        NarcissusFarewell.getTeleportRequest().put(request.getRequestId(), request.setIgnore(ignore));
        if (!ignore) {
            Component yesButton = NarcissusComponent.get().transAuto("yes_button")
                    .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), CommonConfig.get().commandNames().commandTpHereYes(), request.getRequestId())));
            Component noButton = NarcissusComponent.get().transAuto("no_button")
                    .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), CommonConfig.get().commandNames().commandTpHereNo(), request.getRequestId())));
            MessageUtils.sendMessage(target, NarcissusComponent.get().transAuto("tp_here_request_received",
                    player.getDisplayName().getString(), NarcissusComponent.get().transAuto(request.isSafe() ? "tp_here_safe" : "tp_here_unsafe"), yesButton, noButton));
        }
        Component cancelButton = NarcissusComponent.get().transAuto("cancel_button")
                .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), CommonConfig.get().commandNames().commandTpHereCancel(), request.getRequestId())));
        MessageUtils.sendMessage(player, NarcissusComponent.get().transAuto("tp_here_request_sent", target.getDisplayName().getString(), cancelButton));
        if (autoAccept) {
            ServerPlayer finalTarget = target;
            new Thread(() -> NarcissusUtils.executeCommand(finalTarget, NarcissusUtils.getCommand(EnumCommandType.TP_HERE_YES) + " " + request.getRequestId())).start();
        }
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandTpHere())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HERE))
                .executes(TpHereCommand::execute)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TpHereCommand::execute)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(CommandUtils::safeSuggestion)
                                .executes(TpHereCommand::execute)
                        )
                );
    }
}
