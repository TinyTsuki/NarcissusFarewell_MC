package xin.vanilla.narcissus.command.impl;

import xin.vanilla.narcissus.internal.server.NarcissusCostService;
import xin.vanilla.narcissus.internal.server.NarcissusSearchService;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpViewCommand {
    private TpViewCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_VIEW)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        boolean safe = "safe".equalsIgnoreCase(xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, "safe"));
        int range = xin.vanilla.banira.common.util.CommandUtils.getIntDefault(context, "range", CommonConfig.get().base().teleportLimit().teleportViewDistanceLimit());
        range = NarcissusUtils.checkRange(player, EnumTeleportType.TP_VIEW, range);
        MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("tp_view_searching"), NarcissusNotificationTypes.TELEPORT_SEARCH);
        int finalRange = range;
        NarcissusCostService.Ticket ticket = NarcissusUtils.beginTeleport(player, EnumTeleportType.TP_VIEW);
        if (ticket == null) return 0;
        NarcissusSearchService search = NarcissusSearchService.get();
        if (search == null) {
            ticket.cancel();
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("search_failed"), NarcissusNotificationTypes.TELEPORT_ERROR);
            return 0;
        }
        return search.searchView(player, safe, finalRange, ticket, session -> {
            SafeWorldCoordinate safeWorldCoordinate = session.destination();
            if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_VIEW)) { session.cancel(); return; }
            NarcissusUtils.teleportSearched(player, session, EnumTeleportType.TP_VIEW, ticket);
        }) ? 1 : 0;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().command().commandTpView())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_VIEW))
                .executes(TpViewCommand::execute)
                .then(Commands.argument("safe", StringArgumentType.word())
                        .suggests(CommandUtils::safeSuggestion)
                        .executes(TpViewCommand::execute)
                        .then(Commands.argument("range", IntegerArgumentType.integer(1))
                                .suggests(CommandUtils::rangeSuggestion)
                                .executes(TpViewCommand::execute)
                        )
                );
    }
}
