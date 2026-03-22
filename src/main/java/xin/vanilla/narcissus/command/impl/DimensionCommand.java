package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMCColor;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.CommandUtils;

public final class DimensionCommand {
    private DimensionCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.DIMENSION)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        String dimString = player.getLevel().dimension().location().toString();
        Component dim = NarcissusComponent.get().literal(dimString);
        dim.color(EnumMCColor.GREEN.getColor())
                .clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, dimString))
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, NarcissusComponent.get().transAuto("chat_copy_click").toVanilla(NarcissusLang.getPlayerLanguage(player))));
        Component msg = NarcissusComponent.get().transAuto("dimension_info", dim);
        MessageUtils.sendMessage(player, msg);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandDimension())
                .executes(DimensionCommand::execute);
    }
}
