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
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumMCColor;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GetHomeCommand {
    private GetHomeCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.GET_HOME)) return 0;
        Component component;
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        String language = NarcissusLang.getPlayerLanguage(player);
        if (data.getHomeCoordinate().isEmpty()) {
            component = NarcissusComponent.get().transAuto("home_is_empty");
        } else {
            Component info = NarcissusComponent.get().empty();
            Map<String, List<KeyValue<String, SafeWorldCoordinate>>> map = data.getHomeCoordinate().entrySet().stream()
                    .collect(Collectors.groupingBy(
                            entry -> entry.getKey().key(),
                            Collectors.mapping(
                                    entry -> new KeyValue<>(entry.getKey().value(), entry.getValue()),
                                    Collectors.toList()
                            )
                    ));
            for (Map.Entry<String, List<KeyValue<String, SafeWorldCoordinate>>> entry : map.entrySet()) {
                Component dimension = NarcissusComponent.get().literal(entry.getKey()).color(EnumMCColor.DARK_GREEN.getColor());
                dimension.clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.getKey()));
                dimension.hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, NarcissusComponent.get().literal(entry.getKey()).toVanilla()));
                dimension.append(NarcissusComponent.get().literal(": ").color(EnumMCColor.GRAY.getColor()));
                for (KeyValue<String, SafeWorldCoordinate> coordinates : entry.getValue()) {
                    Component defHome;
                    if (data.getDefaultHome().getOrDefault(entry.getKey(), "").equalsIgnoreCase(coordinates.key())) {
                        defHome = NarcissusComponent.get().transAuto("default").color(EnumMCColor.GRAY.getColor());
                    } else {
                        defHome = NarcissusComponent.get().empty();
                    }
                    Component name = NarcissusComponent.get().transAuto("home_info"
                            , coordinates.key()
                            , coordinates.value().xString()
                            , coordinates.value().yString()
                            , coordinates.value().zString()
                            , defHome);
                    Component name_hover = NarcissusComponent.get().transAuto("home_info_hover"
                            , coordinates.key()
                            , coordinates.value().xString()
                            , coordinates.value().yString()
                            , coordinates.value().zString()
                            , defHome);
                    name.color(EnumMCColor.GREEN.getColor());
                    name.clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name_hover.getString(language, true, true)));
                    name.hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, name_hover.toChat(language)));
                    dimension.append(name);
                    dimension.append(NarcissusComponent.get().literal(", ").color(EnumMCColor.GRAY.getColor()));
                }
                info.append(dimension).append("\n");
            }
            component = NarcissusComponent.get().transAuto("home_is", info);
        }
        MessageUtils.sendMessage(player, component);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandGetHome())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.GET_HOME))
                .executes(GetHomeCommand::execute);
    }

}
