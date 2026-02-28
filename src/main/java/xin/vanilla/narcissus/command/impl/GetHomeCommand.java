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
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumMCColor;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.Component;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GetHomeCommand {
    private GetHomeCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.GET_HOME)) return 0;
        Component component;
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        String language = NarcissusUtils.getPlayerLanguage(player);
        if (data.getHomeCoordinate().isEmpty()) {
            component = Component.trans(language, EnumI18nType.FORMAT, "home_is_empty");
        } else {
            Component info = Component.empty();
            Map<String, List<KeyValue<String, Coordinate>>> map = data.getHomeCoordinate().entrySet().stream()
                    .collect(Collectors.groupingBy(
                            entry -> entry.getKey().key(),
                            Collectors.mapping(
                                    entry -> new KeyValue<>(entry.getKey().value(), entry.getValue()),
                                    Collectors.toList()
                            )
                    ));
            for (Map.Entry<String, List<KeyValue<String, Coordinate>>> entry : map.entrySet()) {
                Component dimension = Component.literal(entry.getKey()).color(EnumMCColor.DARK_GREEN.getColor());
                dimension.clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.getKey()));
                dimension.hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(entry.getKey()).toTextComponent()));
                dimension.append(Component.literal(": ").color(EnumMCColor.GRAY.getColor()));
                for (KeyValue<String, Coordinate> coordinates : entry.getValue()) {
                    Component defHome;
                    if (data.getDefaultHome().getOrDefault(entry.getKey(), "").equalsIgnoreCase(coordinates.key())) {
                        defHome = Component.trans(language, EnumI18nType.WORD, "default").color(EnumMCColor.GRAY.getColor());
                    } else {
                        defHome = Component.empty();
                    }
                    Component name = Component.trans(language, EnumI18nType.FORMAT, "home_info"
                            , coordinates.key()
                            , coordinates.value().toXString()
                            , coordinates.value().toYString()
                            , coordinates.value().toZString()
                            , defHome);
                    name.toChatComponent();
                    Component name_hover = Component.trans(language, EnumI18nType.FORMAT, "home_info_hover"
                            , coordinates.key()
                            , coordinates.value().toXString()
                            , coordinates.value().toYString()
                            , coordinates.value().toZString()
                            , defHome);
                    name.color(EnumMCColor.GREEN.getColor());
                    name.clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name_hover.toString(true)));
                    name.hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, name_hover.toChatComponent()));
                    dimension.append(name);
                    dimension.append(Component.literal(", ").color(EnumMCColor.GRAY.getColor()));
                }
                info.append(dimension).append("\n");
            }
            component = Component.trans(language, EnumI18nType.FORMAT, "home_is", info);
        }
        NarcissusUtils.sendMessage(player, component);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_GET_HOME.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.GET_HOME))
                .executes(GetHomeCommand::execute);
    }

}
