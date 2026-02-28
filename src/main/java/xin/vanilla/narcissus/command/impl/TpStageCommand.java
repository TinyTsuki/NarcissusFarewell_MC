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
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.DimensionUtils;
import xin.vanilla.narcissus.util.I18nUtils;
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
        String name = CommandUtils.getStringDefault(context, "name", null);
        Coordinate coordinate = NarcissusUtils.getStageCoordinate(player, targetLevel, name);
        if (coordinate == null) {
            if (targetLevel == null && name == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "stage_nearest_not_found"));
            } else if (targetLevel != null && name == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "stage_not_found_in_dimension"), targetLevel.identifier().toString());
            } else if (targetLevel == null) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "stage_not_found"), name);
            } else {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "stage_not_found_with_name_in_dimension"), targetLevel.identifier().toString(), name);
            }
            return 0;
        }
        coordinate.safe("safe".equalsIgnoreCase(CommandUtils.getStringEmpty(context, "safe")));
        if (CommandUtils.checkTeleportPost(player, coordinate, EnumTeleportType.TP_STAGE, true)) return 0;
        NarcissusUtils.teleportTo(player, coordinate, EnumTeleportType.TP_STAGE);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_TP_STAGE.get())
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
