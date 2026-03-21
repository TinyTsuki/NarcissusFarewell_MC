package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMCColor;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.CommandUtils;

public final class UuidCommand {
    private UuidCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.UUID)) return 0;
        CommandSource source = context.getSource();
        ServerPlayerEntity target = CommandUtils.getPlayerOrSelf(context, "player");
        String language = CommandUtils.getLanguage(source);
        Component uuid = Component.literal(target.getStringUUID());
        uuid.color(EnumMCColor.GREEN.getColor())
                .clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, target.getStringUUID()))
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, NarcissusLang.transLangAuto(language, "chat_copy_click").toVanilla()));
        Component component = NarcissusLang.transLangAuto(language, "player_uuid", target.getDisplayName().getString(), uuid);
        source.sendSuccess(component.toChat(language), false);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandUuid())
                .executes(UuidCommand::execute)
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(UuidCommand::execute));
    }
}
