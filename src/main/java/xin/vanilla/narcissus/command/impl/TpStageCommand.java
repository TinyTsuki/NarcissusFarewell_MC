package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpStageCommand {
    private TpStageCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_STAGE)) return 0;
        ResourceKey<Level> targetLevel = null;
        try {
            ResourceKey<Level> targetDimension = DimensionUtils.parse(StringArgumentType.getString(context, "dimension"));
            ServerLevel level = context.getSource().getServer().getLevel(targetDimension);
            if (level != null) {
                targetLevel = targetDimension;
            }
        } catch (IllegalArgumentException ignored) {
        }
        String name = xin.vanilla.banira.common.util.CommandUtils.getStringDefault(context, "name", null);
        SafeWorldCoordinate safeWorldCoordinate = NarcissusUtils.getStageCoordinate(player, targetLevel, name);
        if (safeWorldCoordinate == null) {
            if (targetLevel == null && name == null) {
                MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("stage_nearest_not_found"), NarcissusNotificationTypes.TELEPORT_ERROR);
            } else if (targetLevel != null && name == null) {
                MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("stage_not_found_in_dimension", targetLevel.location().toString()), NarcissusNotificationTypes.TELEPORT_ERROR);
            } else if (targetLevel == null) {
                MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("stage_not_found", name), NarcissusNotificationTypes.TELEPORT_ERROR);
            } else {
                MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("stage_not_found_with_name_in_dimension", targetLevel.location().toString(), name), NarcissusNotificationTypes.TELEPORT_ERROR);
            }
            return 0;
        }
        safeWorldCoordinate.safe("safe".equalsIgnoreCase(xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, "safe")));
        if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_STAGE, true)) return 0;
        NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_STAGE);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandTpStage())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_STAGE))
                .executes(TpStageCommand::execute)
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(CommandUtils::stageSuggestion)
                        .executes(TpStageCommand::execute)
                        .then(Commands.argument("safe", StringArgumentType.word())
                                .suggests(CommandUtils::safeSuggestion)
                                .executes(TpStageCommand::execute)
                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                        .suggests(CommandUtils::stageDimSuggestion)
                                        .executes(TpStageCommand::execute)
                                )
                        )
                );
    }
}
