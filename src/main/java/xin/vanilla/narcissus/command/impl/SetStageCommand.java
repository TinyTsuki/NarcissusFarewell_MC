package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
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
import xin.vanilla.narcissus.network.packet.StageDataSyncToClient;
import xin.vanilla.narcissus.network.packet.WaypointSyncToClient;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.concurrent.CompletableFuture;

import static xin.vanilla.banira.common.util.CommandUtils.getStringDefault;

public final class SetStageCommand {
    private SetStageCommand() {
    }

    private static void sendStageMessage(CommandSource source, Component message, boolean success, String notificationType) {
        if (source.getEntity() instanceof ServerPlayerEntity) {
            MessageUtils.sendNotification((ServerPlayerEntity) source.getEntity(), message, notificationType);
        } else {
            MessageUtils.sendMessage(source, success, message);
        }
    }

    private static int addStage(CommandSource source, String name, RegistryKey<World> targetLevel, SafeWorldCoordinate safeWorldCoordinate) {
        WorldStageData stageData = WorldStageData.get();
        String dimension = targetLevel.location().toString();
        KeyValue<String, String> key = new KeyValue<>(dimension, name);
        if (stageData.getStageCoordinate().containsKey(key)) {
            sendStageMessage(source, NarcissusComponent.get().transAuto("stage_already_exists", key.key(), key.value()), false, NarcissusNotificationTypes.WAYPOINT);
            return 0;
        }
        stageData.addCoordinate(key, safeWorldCoordinate);
        PacketUtils.broadcastPacket(new WaypointSyncToClient(WaypointSyncToClient.Action.ADD, WaypointSyncToClient.Type.STAGE, name, safeWorldCoordinate));
        PacketUtils.broadcastPacket(new StageDataSyncToClient(stageData.getStageCoordinate()));
        sendStageMessage(source, NarcissusComponent.get().transAuto("stage_set", name, safeWorldCoordinate.xyzString()), true, NarcissusNotificationTypes.WAYPOINT);
        return 1;
    }

    private static int execute(CommandContext<CommandSource> context) {
        CommandUtils.notifyHelp(context);
        CommandSource source = context.getSource();
        ServerPlayerEntity player = source.getEntity() instanceof ServerPlayerEntity ? (ServerPlayerEntity) source.getEntity() : null;

        Vector3d pos = null;
        try {
            pos = Vec3Argument.getCoordinates(context, "coordinate").getPosition(source);
        } catch (IllegalArgumentException ignored) {
        }
        boolean hasCoord = pos != null;
        String dimArg = getStringDefault(context, "dimension", null);
        boolean hasDim = dimArg != null;

        if ((!hasCoord || !hasDim) && player == null) {
            sendStageMessage(source, NarcissusComponent.get().transAuto("set_stage_console_requires_pos_dim"), false, NarcissusNotificationTypes.TELEPORT_ERROR);
            return 0;
        }
        if (CommandUtils.checkTeleportPre(source, EnumCommandType.SET_STAGE)) return 0;

        String name = StringArgumentType.getString(context, "name");
        RegistryKey<World> targetLevel;
        SafeWorldCoordinate coord;

        if (!hasCoord) {
            targetLevel = player.getLevel().dimension();
            coord = new SafeWorldCoordinate(player);
        } else if (!hasDim) {
            targetLevel = player.getLevel().dimension();
            coord = new SafeWorldCoordinate(player);
            coord.dimension(targetLevel);
            coord.fromVector3d(pos);
        } else {
            boolean fromPlayer = player != null;
            try {
                RegistryKey<World> parsed = DimensionUtils.parse(dimArg);
                ServerWorld level = source.getServer().getLevel(parsed);
                if (level != null) {
                    targetLevel = parsed;
                } else if (fromPlayer) {
                    targetLevel = player.getLevel().dimension();
                } else {
                    sendStageMessage(source, NarcissusComponent.get().transAuto("set_stage_dimension_invalid"), false, NarcissusNotificationTypes.TELEPORT_ERROR);
                    return 0;
                }
            } catch (IllegalArgumentException e) {
                if (fromPlayer) {
                    targetLevel = player.getLevel().dimension();
                } else {
                    sendStageMessage(source, NarcissusComponent.get().transAuto("set_stage_dimension_invalid"), false, NarcissusNotificationTypes.TELEPORT_ERROR);
                    return 0;
                }
            }
            if (fromPlayer) {
                coord = new SafeWorldCoordinate(player);
                coord.dimension(targetLevel);
                coord.fromVector3d(pos);
            } else {
                coord = new SafeWorldCoordinate(pos.x, pos.y, pos.z, targetLevel);
            }
        }
        return addStage(source, name, targetLevel, coord);
    }

    public static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        builder.suggest("stage");
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().command().commandSetStage())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.SET_STAGE))
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(SetStageCommand::suggestion)
                        .executes(SetStageCommand::execute)
                        .then(Commands.argument("coordinate", Vec3Argument.vec3())
                                .executes(SetStageCommand::execute)
                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                        .suggests(CommandUtils::dimSuggestion)
                                        .executes(SetStageCommand::execute)
                                )
                        )
                );
    }
}
