package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumMCColor;
import xin.vanilla.narcissus.util.*;

import java.util.ArrayList;
import java.util.List;

public final class ShareCommand {
    private ShareCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.SHARE)) return 0;
        CommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayerOrException();

        String name = CommandUtils.getStringDefault(context, "name", "Shared");

        List<ServerPlayerEntity> targetList = new ArrayList<>(CommandUtils.getPlayersOptional(context, "players",
                context.getSource().getServer().getPlayerList().getPlayers()));

        Component nameComponent;
        Component tpButton = Component.trans(EnumI18nType.FORMAT, "tp_button");
        Component copyButton = Component.trans(EnumI18nType.FORMAT, "copy_button");

        if (name.contains("->")) {
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            KeyValue<String, Coordinate> keyValue = data.getHomeCoordinate().entrySet().stream()
                    .map(entry -> new KeyValue<>(entry.getKey().value() + "->" + entry.getKey().key(), entry.getValue()))
                    .filter(kv -> name.equals(kv.key()))
                    .findFirst()
                    .orElse(new KeyValue<>(name, null));
            String[] split = keyValue.key().split("->");
            Coordinate coordinate = keyValue.value();
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_not_found_with_name_in_dimension")
                        , split[1], split[0]);
                return 0;
            }
            nameComponent = Component.literal(split[0]);
            String tpCommand = buildTpCommand(coordinate);
            tpButton.color(EnumMCColor.GREEN.getColor())
                    .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
            copyButton.color(EnumMCColor.GREEN.getColor())
                    .clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tpCommand))
                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
        } else if (name.contains(">>")) {
            KeyValue<String, Coordinate> keyValue = WorldStageData.get().getStageCoordinate().entrySet().stream()
                    .map(entry -> new KeyValue<>(entry.getKey().value() + ">>" + entry.getKey().key(), entry.getValue()))
                    .filter(kv -> name.equals(kv.key()))
                    .findFirst()
                    .orElse(new KeyValue<>(name, null));
            String[] split = keyValue.key().split(">>");
            Coordinate coordinate = keyValue.value();
            if (coordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "stage_not_found_with_name_in_dimension")
                        , split[1], split[0]);
                return 0;
            }
            nameComponent = Component.literal(split[0]);
            String tpCommand = buildTpCommand(coordinate);
            tpButton.color(EnumMCColor.GREEN.getColor())
                    .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
            copyButton.color(EnumMCColor.GREEN.getColor())
                    .clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tpCommand))
                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
        } else {
            nameComponent = Component.literal(name);
            Coordinate coordinate = new Coordinate(player);
            String tpCommand = buildTpCommand(coordinate);
            tpButton.color(EnumMCColor.GREEN.getColor())
                    .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
            copyButton.color(EnumMCColor.GREEN.getColor())
                    .clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tpCommand))
                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toTextComponent()));
        }

        String lang = ServerConfig.DEFAULT_LANGUAGE.get();
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
            lang = NarcissusUtils.getPlayerLanguage(source.getPlayerOrException());
        }
        Component component = Component.trans(lang, EnumI18nType.FORMAT, "shared_coordinates"
                , player.getDisplayName().getString()
                , nameComponent
                , tpButton
                , copyButton);
        for (ServerPlayerEntity target : targetList) {
            if (!target.getUUID().equals(player.getUUID())) {
                NarcissusUtils.sendMessage(target, component);
            }
        }
        source.sendSuccess(component.toChatComponent(lang), false);
        return 1;
    }

    private static String buildTpCommand(Coordinate coordinate) {
        return String.format("/%s %s %s %s unsafe %s"
                , NarcissusUtils.getCommand(EnumCommandType.TP_COORDINATE)
                , coordinate.toXString()
                , coordinate.toYString()
                , coordinate.toZString()
                , coordinate.getDimensionResourceId()
        );
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.COMMAND_SHARE.get())
                .executes(ShareCommand::execute)
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            String name = CommandUtils.getStringEmpty(context, "name");
                            CommandSource source = context.getSource();
                            ServerPlayerEntity player = source.getPlayerOrException();
                            PlayerTeleportData data = PlayerTeleportData.getData(player);
                            for (KeyValue<String, String> home : data.getHomeCoordinate().keySet()) {
                                String homeString = home.value() + "->" + home.key();
                                if (StringUtils.isNullOrEmptyEx(name) || homeString.toLowerCase().contains(name)) {
                                    builder.suggest(StringUtils.formatString(homeString));
                                }
                            }
                            for (KeyValue<String, String> stage : WorldStageData.get().getStageCoordinate().keySet()) {
                                String stageString = stage.value() + ">>" + stage.key();
                                if (StringUtils.isNullOrEmptyEx(name) || stageString.toLowerCase().contains(name)) {
                                    builder.suggest(StringUtils.formatString(stageString));
                                }
                            }
                            return builder.buildFuture();
                        })
                        .executes(ShareCommand::execute)
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(ShareCommand::execute)
                        )
                );
    }
}
