package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class TpStageCommand {
    private TpStageCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_STAGE)) return 0;
        RegistryKey<World> targetLevel = null;
        try {
            RegistryKey<World> targetDimension = DimensionUtils.parse(StringArgumentType.getString(context, "dimension"));
            ServerWorld level = context.getSource().getServer().getLevel(targetDimension);
            if (level != null) {
                targetLevel = targetDimension;
            }
        } catch (IllegalArgumentException ignored) {
        }
        String name = CommandUtils.getStringDefault(context, "name", null);
        SafeWorldCoordinate safeWorldCoordinate = NarcissusUtils.getStageCoordinate(player, targetLevel, name);
        if (safeWorldCoordinate == null) {
            if (targetLevel == null && name == null) {
                NarcissusUtils.sendTranslatableMessage(player, "stage_nearest_not_found");
            } else if (targetLevel != null && name == null) {
                NarcissusUtils.sendTranslatableMessage(player, "stage_not_found_in_dimension", targetLevel.location().toString());
            } else if (targetLevel == null) {
                NarcissusUtils.sendTranslatableMessage(player, "stage_not_found", name);
            } else {
                NarcissusUtils.sendTranslatableMessage(player, "stage_not_found_with_name_in_dimension", targetLevel.location().toString(), name);
            }
            return 0;
        }
        safeWorldCoordinate.safe("safe".equalsIgnoreCase(CommandUtils.getStringEmpty(context, "safe")));
        if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_STAGE, true)) return 0;
        NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_STAGE);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
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
