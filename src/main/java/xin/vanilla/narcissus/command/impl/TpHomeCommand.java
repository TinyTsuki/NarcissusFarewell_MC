package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.util.DimensionUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.notification.NarcissusNotificationSend;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.concurrent.CompletableFuture;

public final class TpHomeCommand {
    private TpHomeCommand() {
    }

    private static int execute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.TP_HOME)) return 0;
        RegistryKey<World> targetLevel = null;
        try {
            RegistryKey<World> targetDimension = DimensionUtils.parse(StringArgumentType.getString(context, "dimension"));
            ServerWorld level = context.getSource().getServer().getLevel(targetDimension);
            if (level != null) {
                targetLevel = targetDimension;
            }
        } catch (IllegalArgumentException ignored) {
        }
        String name = xin.vanilla.banira.common.util.CommandUtils.getStringDefault(context, "name", null);
        SafeWorldCoordinate safeWorldCoordinate = NarcissusUtils.getPlayerHome(player, targetLevel, name);
        if (safeWorldCoordinate == null) {
            if (targetLevel == null && name == null) {
                NarcissusNotificationSend.send(player, NarcissusComponent.get().transAuto("home_not_found"), NarcissusNotificationTypes.TELEPORT_ERROR);
            } else if (targetLevel != null && name == null) {
                NarcissusNotificationSend.send(player, NarcissusComponent.get().transAuto("home_not_found_in_dimension", targetLevel.location().toString()), NarcissusNotificationTypes.TELEPORT_ERROR);
            } else if (targetLevel == null) {
                NarcissusNotificationSend.send(player, NarcissusComponent.get().transAuto("home_not_found_with_name", name), NarcissusNotificationTypes.TELEPORT_ERROR);
            } else {
                NarcissusNotificationSend.send(player, NarcissusComponent.get().transAuto("home_not_found_with_name_in_dimension", targetLevel.location().toString(), name), NarcissusNotificationTypes.TELEPORT_ERROR);
            }
            return 0;
        }
        try {
            safeWorldCoordinate.safe(BoolArgumentType.getBool(context, "safe"));
        } catch (IllegalArgumentException ignored) {
        }
        if (CommandUtils.checkTeleportPost(player, safeWorldCoordinate, EnumTeleportType.TP_HOME, true)) return 0;
        NarcissusUtils.teleportTo(player, safeWorldCoordinate, EnumTeleportType.TP_HOME);
        return 1;
    }

    public static CompletableFuture<Suggestions> safeSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        String name = xin.vanilla.banira.common.util.CommandUtils.getStringDefault(context, "name", null);
        String lang = xin.vanilla.banira.common.util.CommandUtils.getLanguage(context.getSource());
        Component trueTooltip = NarcissusComponent.get().transAuto("suggest_safe_true").languageCode(lang);
        Component falseTooltip = NarcissusComponent.get().transAuto("suggest_safe_false").languageCode(lang);
        Component dimTooltip = NarcissusComponent.get().transAuto("suggest_dimension").languageCode(lang);
        if ("true".equals(name) || "false".equals(name)) {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            for (KeyValue<String, String> keyValue : data.getHomeCoordinate().keySet()) {
                builder.suggest(keyValue.key(), dimTooltip.toVanilla());
            }
            if (data.getHomeCoordinate().keySet().stream()
                    .anyMatch(kv -> kv.value().equals("true") || kv.value().equals("false"))) {
                builder.suggest("true", trueTooltip.toVanilla());
                builder.suggest("false", falseTooltip.toVanilla());
            }
        } else {
            builder.suggest("true", trueTooltip.toVanilla());
            builder.suggest("false", falseTooltip.toVanilla());
        }
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal(CommonConfig.get().commandNames().commandTpHome())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.TP_HOME))
                .executes(TpHomeCommand::execute)
                .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(CommandUtils::homeSuggestion)
                        .executes(TpHomeCommand::execute)
                        .then(Commands.argument("safe", BoolArgumentType.bool())
                                .suggests(TpHomeCommand::safeSuggestion)
                                .executes(TpHomeCommand::execute)
                                .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                        .suggests(CommandUtils::homeDimSuggestion)
                                        .executes(TpHomeCommand::execute)
                                )
                        )
                )
                .then(Commands.argument("safe", BoolArgumentType.bool())
                        .executes(TpHomeCommand::execute)
                        .then(Commands.argument("dimension", StringArgumentType.greedyString())
                                .suggests(CommandUtils::homeDimSuggestion)
                                .executes(TpHomeCommand::execute)
                        )
                );
    }
}
