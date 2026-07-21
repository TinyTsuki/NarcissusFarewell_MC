package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.TeleportCountdownHelper;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.NarcissusUtils;

public final class ConfigCommand {
    private ConfigCommand() {
    }

    private static ConfigHolder commonConfig() {
        return BaniraConfigs.holder(CommonConfig.class);
    }

    private static int executeTeleportCard(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Component msg = NarcissusComponent.get().transAuto("server_config_status"
                , NarcissusLang.get().enabled(CommonConfig.get().base().teleportCard())
                , NarcissusComponent.get().transAuto("teleport_card"));
        MessageUtils.sendDefaultNotification(player, msg, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO);
        return 1;
    }

    private static int executeTeleportCardSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        boolean bool = BoolArgumentType.getBool(context, "bool");
        CommonConfig.get().base().teleportCard(bool);
        ServerPlayer player = context.getSource().getPlayerOrException();
        Component msg = NarcissusComponent.get().transAuto("server_config_status"
                , NarcissusLang.get().enabled(CommonConfig.get().base().teleportCard())
                , NarcissusComponent.get().transAuto("teleport_card"));
        MessageUtils.broadcastMessage(player, msg);
        return 1;
    }

    private static int executeMode(CommandContext<CommandSourceStack> context) {
        int mode = IntegerArgumentType.getInteger(context, "mode");
        CommandSourceStack source = context.getSource();
        switch (mode) {
            case 0:
                CommonConfig.resetConfig();
                break;
            case 1:
                CommonConfig.resetConfigWithMode1();
                break;
            case 2:
                CommonConfig.resetConfigWithMode2();
                break;
            case 3:
                CommonConfig.resetConfigWithMode3();
                break;
            default:
                throw new IllegalArgumentException("Mode " + mode + " does not exist");
        }
        Component component = NarcissusComponent.get().transAuto("server_config_mode", mode);
        MessageUtils.sendMessage(source, true, component);
        source.getServer().getPlayerList().getPlayers()
                .forEach(player -> source.getServer().getPlayerList().sendPlayerPermissionLevel(player));
        return 1;
    }

    private static int executeLanguage(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String code = StringArgumentType.getString(context, "language");
        CommonConfig.get().general().defaultLanguage(code);
        ServerPlayer player = context.getSource().getPlayerOrException();
        MessageUtils.broadcastMessage(player, NarcissusComponent.get().transAuto("server_default_language", CommonConfig.get().general().defaultLanguage()));
        return 1;
    }

    private static final SuggestionProvider<CommandSourceStack> MODE_SUGGESTION = (context, builder) -> {
        builder.suggest(0);
        builder.suggest(1);
        builder.suggest(2);
        builder.suggest(3);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> LANGUAGE_SUGGESTION = (context, builder) -> {
        NarcissusLang.get().getI18nFiles().forEach(builder::suggest);
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> TYPE_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();
        for (EnumTeleportType t : EnumTeleportType.countdownConfigurableTypes()) {
            String name = t.name().toLowerCase();
            if (name.startsWith(remaining)) {
                builder.suggest(t.name());
            }
        }
        return builder.buildFuture();
    };

    private static int executeTpCountdownQuery(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        xin.vanilla.narcissus.util.CommandUtils.notifyHelp(context);
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (xin.vanilla.narcissus.util.CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.CONFIG)) {
            return 0;
        }
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        for (EnumTeleportType t : EnumTeleportType.countdownConfigurableTypes()) {
            Component line = NarcissusComponent.get().transAuto("tp_countdown_query_line", t.name(), String.valueOf(data.getTeleportCountdownSeconds(t)));
            MessageUtils.sendMessage(context.getSource(), true, line);
        }
        return 1;
    }

    private static int executeTpCountdownSet(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        if (xin.vanilla.narcissus.util.CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.CONFIG)) {
            return 0;
        }
        String typeName = StringArgumentType.getString(context, "type");
        EnumTeleportType type = EnumTeleportType.valueOfEx(typeName);
        if (type == null || !EnumTeleportType.countdownConfigurableTypes().contains(type)) {
            MessageUtils.sendDefaultNotification(player, NarcissusComponent.get().transAuto("tp_countdown_invalid_type", typeName), EnumPosition.TOP_CENTER, EnumMoveType.AUTO);
            return 0;
        }
        int sec = IntegerArgumentType.getInteger(context, "seconds");
        sec = TeleportCountdownHelper.clampToPlayerAllowedRange(sec);
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        data.setTeleportCountdownSeconds(type, sec);
        PlayerTeleportData.syncPlayerData(player);
        MessageUtils.sendDefaultNotification(player, NarcissusComponent.get().transAuto("tp_countdown_set_ok", type.name(), String.valueOf(data.getTeleportCountdownSeconds(type))), EnumPosition.TOP_RIGHT, EnumMoveType.AUTO);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal("config")
                .then(Commands.literal("teleportCard")
                        .executes(ConfigCommand::executeTeleportCard)
                        .then(Commands.argument("bool", BoolArgumentType.bool())
                                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.VIRTUAL_OP))
                                .executes(ConfigCommand::executeTeleportCardSet)
                        )
                )
                .then(Commands.literal("mode")
                        .then(Commands.argument("mode", IntegerArgumentType.integer(0))
                                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.VIRTUAL_OP))
                                .suggests(MODE_SUGGESTION)
                                .executes(ConfigCommand::executeMode)
                        )
                )
                .then(Commands.literal("language")
                        .then(Commands.argument("language", StringArgumentType.word())
                                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.VIRTUAL_OP))
                                .suggests(LANGUAGE_SUGGESTION)
                                .executes(ConfigCommand::executeLanguage)
                        )
                )
                // 通用入口按配置描述符的点路径覆盖全部 COMMON 分类与字段。
                .then(Commands.literal("common")
                        .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.VIRTUAL_OP))
                        .then(Commands.argument("configKey", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    String input = CommandUtils.getStringEmpty(context, "configKey");
                                    CommandUtils.configKeySuggestion(commonConfig(), builder, input);
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("configValue", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            String configKey = StringArgumentType.getString(context, "configKey");
                                            CommandUtils.configValueSuggestion(commonConfig(), builder, configKey);
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> CommandUtils.executeModifyConfig(commonConfig(), context))
                                )
                        )
                )
                .then(Commands.literal("countdown")
                        .executes(ConfigCommand::executeTpCountdownQuery)
                        .then(Commands.literal("query")
                                .executes(ConfigCommand::executeTpCountdownQuery))
                        .then(Commands.literal("set")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests(TYPE_SUGGESTIONS)
                                        .then(Commands.argument("seconds", IntegerArgumentType.integer(0, TeleportCountdownHelper.ABSOLUTE_MAX_SEC))
                                                .executes(ConfigCommand::executeTpCountdownSet)))))
                .then(WhitelistCommand.create())
                .then(BlacklistCommand.create());
    }
}
