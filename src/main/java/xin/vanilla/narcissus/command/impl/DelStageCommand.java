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
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.network.NetworkInit;
import xin.vanilla.narcissus.network.packet.StageDataSyncToClient;
import xin.vanilla.narcissus.network.packet.WaypointSyncToClient;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;


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
        SafeWorldCoordinate remove = stageData.getStageCoordinate().remove(new KeyValue<>(dimension, name));
        stageData.setDirty();
        if (remove == null) {
            if (player != null) {
                MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("stage_not_found_with_name_in_dimension", dimension, name));
            } else {
                MessageUtils.sendMessage(source, false, NarcissusComponent.get().transAuto("stage_not_found_with_name_in_dimension", dimension, name));
            }
            return 0;
        }
        PacketUtils.broadcastPacket(NetworkInit.INSTANCE, new WaypointSyncToClient(WaypointSyncToClient.Action.REMOVE, WaypointSyncToClient.Type.STAGE, name, remove));
        PacketUtils.broadcastPacket(NetworkInit.INSTANCE, new StageDataSyncToClient(stageData.getStageCoordinate()));
        Component dimensionComponent = NarcissusComponent.get().literal(dimension)
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, NarcissusComponent.get().literal(remove.xyzString()).toVanilla()));
        if (player != null) {
            MessageUtils.sendMessage(player, NarcissusComponent.get().transAuto("stage_del", dimensionComponent, name));
        } else {
            MessageUtils.sendMessage(source, true, NarcissusComponent.get().transAuto("stage_del", dimensionComponent, name));
        }
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandDelStage())
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
