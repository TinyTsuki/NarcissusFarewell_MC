package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumMCColor;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.Component;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class DimensionCommand {
    private DimensionCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.DIMENSION)) return 0;
        ServerPlayer player = context.getSource().getPlayerOrException();
        String dimString = player.getLevel().dimension().location().toString();
        Component dim = Component.literal(dimString);
        dim.color(EnumMCColor.GREEN.getColor())
                .clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, dimString))
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, "chat_copy_click").toTextComponent()));
        Component msg = Component.trans(NarcissusUtils.getPlayerLanguage(player), EnumI18nType.FORMAT, "dimension_info", dim);
        NarcissusUtils.sendMessage(player, msg);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_DIMENSION.get())
                .executes(DimensionCommand::execute);
    }
}
