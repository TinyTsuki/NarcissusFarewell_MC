package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.network.packet.StageDataSyncToClient;
import xin.vanilla.narcissus.network.packet.WaypointSyncToClient;
import xin.vanilla.narcissus.util.*;


public final class DelStageCommand {
    private DelStageCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandUtils.notifyHelp(context);
        CommandSourceStack source = context.getSource();
        ServerPlayer player = null;
        if (source.getEntity() instanceof ServerPlayer) {
            player = (ServerPlayer) source.getEntity();
        }
        if (CommandUtils.checkTeleportPre(source, EnumCommandType.DEL_STAGE)) return 0;
        String name = StringArgumentType.getString(context, "name");
        String dimension;
        try {
            ResourceKey<Level> targetLevel = DimensionUtils.parse(StringArgumentType.getString(context, "dimension"));
            dimension = targetLevel.location().toString();
        } catch (IllegalArgumentException ignored) {
            dimension = NarcissusUtils.getStageDimensionByName(player, name);
        }
        WorldStageData stageData = WorldStageData.get();
        Coordinate remove = stageData.getStageCoordinate().remove(new KeyValue<>(dimension, name));
        stageData.setDirty();
        if (remove == null) {
            if (player != null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "stage_not_found_with_name_in_dimension"), dimension, name);
            } else {
                NarcissusUtils.sendTranslatableMessage(source, false, I18nUtils.getKey(EnumI18nType.FORMAT, "stage_not_found_with_name_in_dimension"), dimension, name);
            }
            return 0;
        }
        NarcissusUtils.broadcastPacket(new WaypointSyncToClient(WaypointSyncToClient.Action.REMOVE, WaypointSyncToClient.Type.STAGE, name, remove));
        NarcissusUtils.broadcastPacket(new StageDataSyncToClient(stageData.getStageCoordinate()));
        Component dimensionComponent = Component.literal(dimension)
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(remove.toXyzString()).toTextComponent()));
        if (player != null) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "stage_del"), dimensionComponent, name);
        } else {
            NarcissusUtils.sendTranslatableMessage(source, true, I18nUtils.getKey(EnumI18nType.FORMAT, "stage_del"), dimensionComponent, name);
        }
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_DEL_STAGE.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.DEL_STAGE))
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(CommandUtils::stageSuggestion)
                        .executes(DelStageCommand::execute)
                        .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                .suggests(CommandUtils::stageDimSuggestion)
                                .executes(DelStageCommand::execute)
                        )
                );
    }
}
