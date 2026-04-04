package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMCColor;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.CommandUtils;

public final class UuidCommand {
    private UuidCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.UUID)) return 0;
        CommandSourceStack source = context.getSource();
        ServerPlayer target = xin.vanilla.banira.common.util.CommandUtils.getPlayerOrSelf(context, "player");
        String language = xin.vanilla.banira.common.util.CommandUtils.getLanguage(source);
        Component uuid = NarcissusComponent.get().literal(target.getStringUUID());
        uuid.color(EnumMCColor.GREEN.getColor())
                .clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, target.getStringUUID()))
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, NarcissusComponent.get().transAuto("chat_copy_click").toVanilla(language)));
        Component component = NarcissusComponent.get().transAuto("player_uuid", target.getDisplayName().getString(), uuid);
        if (source.getEntity() instanceof ServerPlayer player) {
            MessageUtils.sendNotification(player, component, NarcissusNotificationTypes.INTERACTIVE_QUERY);
        } else {
            MessageUtils.sendMessage(source, true, component);
        }
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandUuid())
                .executes(UuidCommand::execute)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(UuidCommand::execute));
    }
}
