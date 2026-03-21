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
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GetStageCommand {
    private GetStageCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.GET_STAGE)) return 0;
        Component component;
        WorldStageData data = WorldStageData.get();
        String language = NarcissusLang.getPlayerLanguage(player);
        if (data.getStageCoordinate().isEmpty()) {
            component = NarcissusLang.transLangAuto(language, "stage_is_empty");
        } else {
            Component info = Component.empty();
            Map<String, List<KeyValue<String, SafeWorldCoordinate>>> map = data.getStageCoordinate().entrySet().stream()
                    .collect(Collectors.groupingBy(
                            entry -> entry.getKey().key(),
                            Collectors.mapping(
                                    entry -> new KeyValue<>(entry.getKey().value(), entry.getValue()),
                                    Collectors.toList()
                            )
                    ));
            for (Map.Entry<String, List<KeyValue<String, SafeWorldCoordinate>>> entry : map.entrySet()) {
                Component dimension = Component.literal(entry.getKey()).color(EnumMCColor.DARK_GREEN.getColor());
                dimension.clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, entry.getKey()));
                dimension.hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(entry.getKey()).toVanilla()));
                dimension.append(Component.literal(": ").color(EnumMCColor.GRAY.getColor()));
                for (KeyValue<String, SafeWorldCoordinate> coordinates : entry.getValue()) {
                    Component name = NarcissusLang.transLangAuto(language, "stage_info"
                            , coordinates.key()
                            , coordinates.value().xString()
                            , coordinates.value().yString()
                            , coordinates.value().zString());
                    Component name_hover = NarcissusLang.transLangAuto(language, "stage_info_hover"
                            , coordinates.key()
                            , coordinates.value().xString()
                            , coordinates.value().yString()
                            , coordinates.value().zString());
                    name.color(EnumMCColor.GREEN.getColor());
                    name.clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, name_hover.toString(true)));
                    name.hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, name_hover.toChat()));
                    dimension.append(name);
                    dimension.append(Component.literal(", ").color(EnumMCColor.GRAY.getColor()));
                }
                info.append(dimension).append("\n");
            }
            component = NarcissusLang.transLangAuto(language, "stage_is", info);
        }
        NarcissusUtils.sendMessage(player, component);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandGetStage())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.GET_STAGE))
                .executes(GetStageCommand::execute);
    }
}
