package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.event.ClickEvent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.Component;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;
import java.util.Date;

public final class TpAskCommand {
    private TpAskCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_ASK)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        ServerPlayerEntity target;
        try {
            target = EntityArgument.getPlayer(context, "player");
        } catch (IllegalArgumentException ignored) {
            target = NarcissusFarewell.getTeleportRequest().values().stream()
                    .filter(request -> request.getRequester().getUUID().equals(player.getUUID()))
                    .filter(request -> {
                        PlayerEntity entity = request.getTarget();
                        return NarcissusUtils.isTeleportTypeAcrossDimensionEnabled(player, EnumTeleportType.TP_ASK)
                                || entity != null && entity.level.dimension() == player.getLevel().dimension();
                    })
                    .max(Comparator.comparing(TeleportRequest::getRequestTime))
                    .orElse(new TeleportRequest().setTarget(NarcissusFarewell.getLastTeleportRequest()
                            .getOrDefault(player, NarcissusUtils.getRandomPlayer())))
                    .getTarget();
        }
        if (target == null) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "player_not_found"));
            return 0;
        }
        TeleportRequest request = new TeleportRequest()
                .setRequester(player)
                .setTarget(target)
                .setTeleportType(EnumTeleportType.TP_ASK)
                .setRequestTime(new Date());
        request.setSafe("safe".equalsIgnoreCase(CommandUtils.getStringEmpty(context, "safe")));
        if (CommandUtils.checkTeleportPost(request)) return 0;
        PlayerAccess targetAccess = PlayerTeleportData.getData(target).getAccess();
        String playerUUIDString = NarcissusUtils.getPlayerUUIDString(player);
        boolean ignore = targetAccess.getBlackList().contains(playerUUIDString)
                || (!targetAccess.getWhiteList().isEmpty() && !targetAccess.getWhiteList().contains(playerUUIDString));
        boolean autoAccept = !ignore && targetAccess.getAutoTpaList().contains(playerUUIDString);
        NarcissusFarewell.getTeleportRequest().put(request.getRequestId(), request.setIgnore(ignore));
        if (!ignore) {
            Component yesButton = Component.trans(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.FORMAT, "yes_button", NarcissusUtils.getPlayerLanguage(target))
                    .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), CommonConfig.COMMAND_TP_ASK_YES.get(), request.getRequestId())));
            Component noButton = Component.trans(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.FORMAT, "no_button", NarcissusUtils.getPlayerLanguage(target))
                    .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), CommonConfig.COMMAND_TP_ASK_NO.get(), request.getRequestId())));
            NarcissusUtils.sendMessage(target, Component.trans(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.FORMAT, "tp_ask_request_received", player.getDisplayName().getString(), yesButton, noButton));
        }
        Component cancelButton = Component.trans(NarcissusUtils.getPlayerLanguage(target), EnumI18nType.FORMAT, "cancel_button", NarcissusUtils.getPlayerLanguage(target))
                .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/%s %s %s", NarcissusUtils.getCommandPrefix(), CommonConfig.COMMAND_TP_ASK_CANCEL.get(), request.getRequestId())));
        NarcissusUtils.sendMessage(player, Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, "tp_ask_request_sent", target.getDisplayName().getString(), cancelButton));
        if (autoAccept) {
            ServerPlayerEntity finalTarget = target;
            new Thread(() -> NarcissusUtils.executeCommand(finalTarget, NarcissusUtils.getCommand(EnumCommandType.TP_ASK_YES) + " " + request.getRequestId())).start();
        }
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.COMMAND_TP_ASK.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_ASK))
                .executes(TpAskCommand::execute)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(TpAskCommand::execute)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(CommandUtils::safeSuggestion)
                                .executes(TpAskCommand::execute)
                        )
                );
    }
}
