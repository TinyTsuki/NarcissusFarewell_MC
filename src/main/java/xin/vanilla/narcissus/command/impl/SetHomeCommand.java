package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.network.packet.WaypointSyncToClient;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.concurrent.CompletableFuture;

public final class SetHomeCommand {
    private SetHomeCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.SET_HOME)) return 0;
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        if (data.getHomeCoordinate().size() >= CommonConfig.get().general().teleportHomeLimit()) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("home_limit", CommonConfig.get().general().teleportHomeLimit()), NarcissusNotificationTypes.WAYPOINT);
            return 0;
        }
        String name = xin.vanilla.banira.common.util.CommandUtils.getStringDefault(context, "name", "home");
        boolean defaultHome = false;
        try {
            defaultHome = BoolArgumentType.getBool(context, "default");
        } catch (IllegalArgumentException ignored) {
        }
        SafeWorldCoordinate safeWorldCoordinate = new SafeWorldCoordinate(player);
        KeyValue<String, String> key = new KeyValue<>(player.level().dimension().location().toString(), name);
        if (data.getHomeCoordinate().containsKey(key)) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("home_already_exists", key.key(), key.value()), NarcissusNotificationTypes.WAYPOINT);
            return 0;
        }
        data.addHomeCoordinate(key, safeWorldCoordinate);
        PacketUtils.sendPacketToPlayer(new WaypointSyncToClient(WaypointSyncToClient.Action.ADD, WaypointSyncToClient.Kind.HOME, name, safeWorldCoordinate), player);
        if (defaultHome) {
            if (data.getDefaultHome().containsKey(player.level().dimension().location().toString())) {
                MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("home_default_remove", data.getDefaultHome(player.level().dimension().location().toString()).value()), NarcissusNotificationTypes.WAYPOINT);
            }
            data.addDefaultHome(player.level().dimension().location().toString(), name);
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("home_set_default", name, safeWorldCoordinate.xyzString()), NarcissusNotificationTypes.WAYPOINT);
        } else {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("home_set", name, safeWorldCoordinate.xyzString()), NarcissusNotificationTypes.WAYPOINT);
        }
        PlayerTeleportData.syncPlayerData(player);
        return 1;
    }

    public static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String lang = xin.vanilla.banira.common.util.CommandUtils.getLanguage(context.getSource());
        Component homeTooltip = NarcissusComponent.get().transAuto("suggest_home_name");
        Component nameTooltip = NarcissusComponent.get().transAuto("suggest_custom_name");
        builder.suggest("home", homeTooltip.toVanilla(lang));
        builder.suggest("name", nameTooltip.toVanilla(lang));
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> defaultSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String lang = xin.vanilla.banira.common.util.CommandUtils.getLanguage(context.getSource());
        Component trueTooltip = NarcissusComponent.get().transAuto("suggest_default_home_true");
        Component falseTooltip = NarcissusComponent.get().transAuto("suggest_default_home_false");
        builder.suggest("true", trueTooltip.toVanilla(lang));
        builder.suggest("false", falseTooltip.toVanilla(lang));
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandSetHome())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HOME))
                .executes(SetHomeCommand::execute)
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(SetHomeCommand::suggestion)
                        .executes(SetHomeCommand::execute)
                        .then(Commands.argument("default", BoolArgumentType.bool())
                                .suggests(SetHomeCommand::defaultSuggestion)
                                .executes(SetHomeCommand::execute)
                        )
                );
    }
}
