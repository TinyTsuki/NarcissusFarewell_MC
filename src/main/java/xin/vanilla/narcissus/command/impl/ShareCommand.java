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
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumMCColor;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

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
        Component tpButton = Component.transAuto(NarcissusFarewell.MODID, "tp_button");
        Component copyButton = Component.transAuto(NarcissusFarewell.MODID, "copy_button");

        if (name.contains("->")) {
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            KeyValue<String, SafeWorldCoordinate> keyValue = data.getHomeCoordinate().entrySet().stream()
                    .map(entry -> new KeyValue<>(entry.getKey().value() + "->" + entry.getKey().key(), entry.getValue()))
                    .filter(kv -> name.equals(kv.key()))
                    .findFirst()
                    .orElse(new KeyValue<>(name, null));
            String[] split = keyValue.key().split("->");
            SafeWorldCoordinate safeWorldCoordinate = keyValue.value();
            if (safeWorldCoordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, "home_not_found_with_name_in_dimension"
                        , split[1], split[0]);
                return 0;
            }
            nameComponent = Component.literal(split[0]);
            String tpCommand = buildTpCommand(safeWorldCoordinate);
            tpButton.color(EnumMCColor.GREEN.getColor())
                    .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toVanilla()));
            copyButton.color(EnumMCColor.GREEN.getColor())
                    .clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tpCommand))
                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toVanilla()));
        } else if (name.contains(">>")) {
            KeyValue<String, SafeWorldCoordinate> keyValue = WorldStageData.get().getStageCoordinate().entrySet().stream()
                    .map(entry -> new KeyValue<>(entry.getKey().value() + ">>" + entry.getKey().key(), entry.getValue()))
                    .filter(kv -> name.equals(kv.key()))
                    .findFirst()
                    .orElse(new KeyValue<>(name, null));
            String[] split = keyValue.key().split(">>");
            SafeWorldCoordinate safeWorldCoordinate = keyValue.value();
            if (safeWorldCoordinate == null) {
                NarcissusUtils.sendTranslatableMessage(player, "stage_not_found_with_name_in_dimension"
                        , split[1], split[0]);
                return 0;
            }
            nameComponent = Component.literal(split[0]);
            String tpCommand = buildTpCommand(safeWorldCoordinate);
            tpButton.color(EnumMCColor.GREEN.getColor())
                    .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toVanilla()));
            copyButton.color(EnumMCColor.GREEN.getColor())
                    .clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tpCommand))
                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toVanilla()));
        } else {
            nameComponent = Component.literal(name);
            SafeWorldCoordinate safeWorldCoordinate = new SafeWorldCoordinate(player);
            String tpCommand = buildTpCommand(safeWorldCoordinate);
            tpButton.color(EnumMCColor.GREEN.getColor())
                    .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toVanilla()));
            copyButton.color(EnumMCColor.GREEN.getColor())
                    .clickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, tpCommand))
                    .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(tpCommand).toVanilla()));
        }

        String lang = CommonConfig.get().server().general().defaultLanguage();
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
            lang = NarcissusLang.getPlayerLanguage(source.getPlayerOrException());
        }
        Component component = NarcissusLang.transLangAuto(lang, "shared_coordinates"
                , player.getDisplayName().getString()
                , nameComponent
                , tpButton
                , copyButton);
        for (ServerPlayerEntity target : targetList) {
            if (!target.getUUID().equals(player.getUUID())) {
                NarcissusUtils.sendMessage(target, component);
            }
        }
        source.sendSuccess(component.toChat(lang), false);
        return 1;
    }

    private static String buildTpCommand(SafeWorldCoordinate safeWorldCoordinate) {
        return String.format("/%s %s %s %s unsafe %s"
                , NarcissusUtils.getCommand(EnumCommandType.TP_COORDINATE)
                , safeWorldCoordinate.xString()
                , safeWorldCoordinate.yString()
                , safeWorldCoordinate.zString()
                , safeWorldCoordinate.dimensionId()
        );
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandShare())
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
