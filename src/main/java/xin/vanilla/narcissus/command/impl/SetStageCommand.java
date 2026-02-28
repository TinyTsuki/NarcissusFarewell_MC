package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
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
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.DimensionUtils;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.concurrent.CompletableFuture;

public final class SetStageCommand {
    private SetStageCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.SET_STAGE)) return 0;
        WorldStageData stageData = WorldStageData.get();
        String name = StringArgumentType.getString(context, "name");
        ResourceKey<Level> targetLevel;
        try {
            ResourceKey<Level> targetDimension = DimensionUtils.parse(StringArgumentType.getString(context, "dimension"));
            ServerLevel level = context.getSource().getServer().getLevel(targetDimension);
            if (level != null) {
                targetLevel = targetDimension;
            } else {
                targetLevel = player.level().dimension();
            }
        } catch (IllegalArgumentException ignored) {
            targetLevel = player.level().dimension();
        }
        String dimension = targetLevel.location().toString();
        KeyValue<String, String> key = new KeyValue<>(dimension, name);
        if (stageData.getStageCoordinate().containsKey(key)) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "stage_already_exists"), key.key(), key.value());
            return 0;
        }
        Coordinate coordinate = new Coordinate(player).dimension(targetLevel);
        try {
            coordinate.fromVec3(Vec3Argument.getCoordinates(context, "coordinate").getPosition(context.getSource()));
        } catch (IllegalArgumentException ignored) {
        }
        stageData.addCoordinate(key, coordinate);
        NarcissusUtils.broadcastPacket(new WaypointSyncToClient(WaypointSyncToClient.Action.ADD, WaypointSyncToClient.Type.STAGE, name, coordinate));
        NarcissusUtils.broadcastPacket(new StageDataSyncToClient(stageData.getStageCoordinate()));
        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "stage_set"), name, coordinate.toXyzString());
        return 1;
    }

    public static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        builder.suggest("stage");
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_SET_STAGE.get())
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
