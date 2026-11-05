package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMCColor;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.CommandUtils;

public final class DimensionCommand {
    private DimensionCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.DIMENSION)) return 0;
        ServerPlayer player = context.getSource().getPlayerOrException();
        String dimString = player.level().dimension().identifier().toString();
        Component dim = NarcissusComponent.get().literal(dimString);
        dim.color(EnumMCColor.GREEN.getColor())
                .clickEvent(new ClickEvent.CopyToClipboard(dimString))
                .hoverEvent(new HoverEvent.ShowText(NarcissusComponent.get().transAuto("chat_copy_click").toVanilla(NarcissusLang.getPlayerLanguage(player))));
        Component msg = NarcissusComponent.get().transAuto("dimension_info", dim);
        MessageUtils.sendNotification(player, msg, NarcissusNotificationTypes.INTERACTIVE_QUERY);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().command().commandDimension())
                .executes(DimensionCommand::execute);
    }
}
