package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.util.CollectionUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.util.CommandUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class WhitelistCommand {
    private WhitelistCommand() {
    }

    private static int executeGet(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        PlayerAccess access = data.getAccess();
        Component msg = CommandUtils.getWhiteListMessage(player, access);
        MessageUtils.sendMessage(player, msg);
        return 1;
    }

    private static int executeAdd(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> players = EntityArgument.getPlayers(context, "players");
        if (CollectionUtils.isNullOrEmpty(players)) return 0;
        String mode = CommandUtils.getStringDefault(context, "mode", "none");
        if (!Arrays.asList(CommandUtils.WHITE_LIST_MODES).contains(mode)) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
        }
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        PlayerAccess access = data.getAccess();
        String[] uuids = players.stream().map(PlayerUtils::getPlayerUUIDString).toArray(String[]::new);
        Component msg;
        if (access.addWhiteList(uuids)) {
            switch (mode) {
                case "both":
                    access.addTpaList(uuids);
                    access.addTphList(uuids);
                    break;
                case "auto_accept_tpa":
                    access.addTpaList(uuids);
                    break;
                case "auto_accept_tph":
                    access.addTphList(uuids);
                    break;
                default:
                    break;
            }
            data.setDirty();
            msg = NarcissusComponent.get().transAuto("list_add_success"
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, false)
                    , players.stream().map(PlayerUtils::getPlayerNameString).collect(Collectors.joining(","))
            );
        } else {
            msg = NarcissusComponent.get().transAuto("list_add_fail", CommandUtils.getBlacklistOrWhitelistHelp(player, false), CommandUtils.getBlacklistOrWhitelistHelp(player, true));
        }
        MessageUtils.sendMessage(player, msg);
        return 1;
    }

    private static int executeDel(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> players = EntityArgument.getPlayers(context, "players");
        if (CollectionUtils.isNullOrEmpty(players)) return 0;
        String mode = CommandUtils.getStringDefault(context, "mode", "none");
        if (!Arrays.asList(CommandUtils.WHITE_LIST_MODES).contains(mode)) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
        }
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        PlayerAccess access = data.getAccess();
        String[] uuids = players.stream().map(PlayerUtils::getPlayerUUIDString).toArray(String[]::new);
        switch (mode) {
            case "both":
                access.removeAutoTpa(uuids);
                access.removeAutoTph(uuids);
                break;
            case "auto_accept_tpa":
                access.removeAutoTpa(uuids);
                break;
            case "auto_accept_tph":
                access.removeAutoTph(uuids);
                break;
            default:
                access.removeWhiteList(uuids);
                break;
        }
        data.setDirty();
        Component msg = NarcissusComponent.get().transAuto("remove_success")
                .append(CommandUtils.getWhiteListMessage(player, access));
        MessageUtils.sendMessage(player, msg);
        return 1;
    }

    private static CompletableFuture<Suggestions> suggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String lang = CommandUtils.getLanguage(context.getSource());
        String[] tooltipKeys = {"suggest_whitelist_none", "suggest_whitelist_both", "suggest_whitelist_auto_accept_tpa", "suggest_whitelist_auto_accept_tph"};
        for (int i = 0; i < CommandUtils.WHITE_LIST_MODES.length; i++) {
            Component tooltip = NarcissusComponent.get().transAuto(tooltipKeys[i]);
            builder.suggest(CommandUtils.WHITE_LIST_MODES[i], tooltip.toVanilla(lang));
        }
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal("white")
                .then(Commands.literal("get")
                        .executes(WhitelistCommand::executeGet)
                )
                .then(Commands.literal("add")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(WhitelistCommand::executeAdd)
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests(WhitelistCommand::suggestion)
                                        .executes(WhitelistCommand::executeAdd)
                                )
                        )
                )
                .then(Commands.literal("del")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(WhitelistCommand::executeDel)
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests(WhitelistCommand::suggestion)
                                        .executes(WhitelistCommand::executeDel)
                                )
                        )
                );
    }
}
