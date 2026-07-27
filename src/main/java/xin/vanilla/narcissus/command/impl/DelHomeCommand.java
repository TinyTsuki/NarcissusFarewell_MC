package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.util.DimensionUtils;
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

public final class DelHomeCommand {
    private DelHomeCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.DEL_HOME)) return 0;
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        String name = StringArgumentType.getString(context, "name");
        String dimension;
        try {
            ResourceKey<Level> targetLevel = DimensionUtils.parse(StringArgumentType.getString(context, "dimension"));
            dimension = targetLevel.location().toString();
        } catch (IllegalArgumentException ignored) {
            dimension = NarcissusUtils.getHomeDimensionByName(player, name);
        }
        SafeWorldCoordinate remove = data.getHomeCoordinate().remove(new KeyValue<>(dimension, name));
        data.setDirty();
        if (remove == null) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("home_not_found_with_name_in_dimension", dimension, name), NarcissusNotificationTypes.WAYPOINT);
            return 0;
        }
        if (data.getDefaultHome().containsKey(dimension)) {
            if (data.getDefaultHome().get(dimension).equals(name)) {
                data.getDefaultHome().remove(dimension);
                data.setDirty();
                MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("home_default_remove", name), NarcissusNotificationTypes.WAYPOINT);
            }
        }
        PacketUtils.sendPacketToPlayer(new WaypointSyncToClient(WaypointSyncToClient.Action.REMOVE, WaypointSyncToClient.Type.HOME, name, remove), player);
        PlayerTeleportData.syncPlayerData(player);
        Component dimensionComponent = NarcissusComponent.get().literal(dimension)
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, NarcissusComponent.get().literal(remove.xyzString()).toVanilla()));
        MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("home_del", dimensionComponent, name), NarcissusNotificationTypes.WAYPOINT);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().command().commandDelHome())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HOME))
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(CommandUtils::homeSuggestion)
                        .executes(DelHomeCommand::execute)
                        .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                .suggests(CommandUtils::homeDimSuggestion)
                                .executes(DelHomeCommand::execute)
                        )
                );
    }
}
