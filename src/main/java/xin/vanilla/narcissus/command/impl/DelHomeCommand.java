package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.network.packet.WaypointSyncToClient;
import xin.vanilla.narcissus.util.*;

public final class DelHomeCommand {
    private DelHomeCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.DEL_HOME)) return 0;
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        String name = StringArgumentType.getString(context, "name");
        String dimension;
        try {
            RegistryKey<World> targetLevel = DimensionUtils.parse(StringArgumentType.getString(context, "dimension"));
            dimension = targetLevel.location().toString();
        } catch (IllegalArgumentException ignored) {
            dimension = NarcissusUtils.getHomeDimensionByName(player, name);
        }
        Coordinate remove = data.getHomeCoordinate().remove(new KeyValue<>(dimension, name));
        data.setDirty();
        if (remove == null) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_not_found_with_name_in_dimension"), dimension, name);
            return 0;
        }
        if (data.getDefaultHome().containsKey(dimension)) {
            if (data.getDefaultHome().get(dimension).equals(name)) {
                data.getDefaultHome().remove(dimension);
                data.setDirty();
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_default_remove"), name);
            }
        }
        NarcissusUtils.sendPacketToPlayer(new WaypointSyncToClient(WaypointSyncToClient.Action.REMOVE, WaypointSyncToClient.Type.HOME, name, remove), player);
        PlayerTeleportData.syncPlayerData(player);
        Component dimensionComponent = Component.literal(dimension)
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(remove.toXyzString()).toTextComponent()));
        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "home_del"), dimensionComponent, name);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.COMMAND_DEL_HOME.get())
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
