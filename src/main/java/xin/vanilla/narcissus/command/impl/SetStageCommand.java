package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
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

import java.util.concurrent.CompletableFuture;

public final class SetStageCommand {
    private SetStageCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.SET_STAGE)) return 0;
        WorldStageData stageData = WorldStageData.get();
        String name = StringArgumentType.getString(context, "name");
        RegistryKey<World> targetLevel;
        try {
            RegistryKey<World> targetDimension = DimensionUtils.parse(StringArgumentType.getString(context, "dimension"));
            ServerWorld level = context.getSource().getServer().getLevel(targetDimension);
            if (level != null) {
                targetLevel = targetDimension;
            } else {
                targetLevel = player.getLevel().dimension();
            }
        } catch (IllegalArgumentException ignored) {
            targetLevel = player.getLevel().dimension();
        }
        String dimension = targetLevel.location().toString();
        KeyValue<String, String> key = new KeyValue<>(dimension, name);
        if (stageData.getStageCoordinate().containsKey(key)) {
            MessageUtils.sendMessage(player, NarcissusComponent.get().transAuto("stage_already_exists", key.key(), key.value()));
            return 0;
        }
        SafeWorldCoordinate safeWorldCoordinate = new SafeWorldCoordinate(player);
        safeWorldCoordinate.dimension(targetLevel);
        try {
            safeWorldCoordinate.fromVector3d(Vec3Argument.getCoordinates(context, "coordinate").getPosition(context.getSource()));
        } catch (IllegalArgumentException ignored) {
        }
        stageData.addCoordinate(key, safeWorldCoordinate);
        PacketUtils.broadcastPacket(NetworkInit.INSTANCE, new WaypointSyncToClient(WaypointSyncToClient.Action.ADD, WaypointSyncToClient.Type.STAGE, name, safeWorldCoordinate));
        PacketUtils.broadcastPacket(NetworkInit.INSTANCE, new StageDataSyncToClient(stageData.getStageCoordinate()));
        MessageUtils.sendMessage(player, NarcissusComponent.get().transAuto("stage_set", name, safeWorldCoordinate.xyzString()));
        return 1;
    }

    public static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        builder.suggest("stage");
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandSetStage())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.SET_STAGE))
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(SetStageCommand::suggestion)
                        .executes(SetStageCommand::execute)
                        .then(Commands.argument("coordinate", Vec3Argument.vec3())
                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                        .suggests(CommandUtils::dimSuggestion)
                                        .executes(SetStageCommand::execute)
                                )
                        )
                );
    }
}
