package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.util.CollectionUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.util.CommandUtils;

import java.util.Collection;
import java.util.stream.Collectors;

public final class BlacklistCommand {
    private BlacklistCommand() {
    }

    private static int executeGet(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        PlayerAccess access = data.getAccess();
        Component msg;
        if (CollectionUtils.isNullOrEmpty(access.getBlackList())) {
            msg = NarcissusComponent.get().transAuto("list_is_empty", CommandUtils.getBlacklistOrWhitelistHelp(player, true));
        } else {
            msg = NarcissusComponent.get().transAuto("list_detail"
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, true)
                    , access.getBlackList().stream()
                            .map(uuid -> PlayerUtils.getPlayerNameString(PlayerUtils.getPlayerByUUID(uuid)))
                            .collect(Collectors.joining(","))
            );
        }
        MessageUtils.sendDefaultNotification(player, msg, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO);
        return 1;
    }

    private static int executeAdd(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> players = EntityArgument.getPlayers(context, "players");
        if (CollectionUtils.isNullOrEmpty(players)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        PlayerAccess access = data.getAccess();
        String[] uuids = players.stream().map(PlayerUtils::getPlayerUUIDString).toArray(String[]::new);
        Component msg;
        if (access.addBlackList(uuids)) {
            data.setDirty();
            PlayerTeleportData.syncPlayerData(player);
            msg = NarcissusComponent.get().transAuto("list_add_success"
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, true)
                    , players.stream().map(PlayerUtils::getPlayerNameString).collect(Collectors.joining(","))
            );
        } else {
            msg = NarcissusComponent.get().transAuto("list_add_fail", CommandUtils.getBlacklistOrWhitelistHelp(player, true), CommandUtils.getBlacklistOrWhitelistHelp(player, false));
        }
        MessageUtils.sendDefaultNotification(player, msg, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO);
        return 1;
    }

    private static int executeDel(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> players = EntityArgument.getPlayers(context, "players");
        if (CollectionUtils.isNullOrEmpty(players)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        PlayerAccess access = data.getAccess();
        String[] uuids = players.stream().map(PlayerUtils::getPlayerUUIDString).toArray(String[]::new);
        access.removeBlackList(uuids);
        data.setDirty();
        PlayerTeleportData.syncPlayerData(player);
        Component msg = NarcissusComponent.get().transAuto("remove_success");
        if (CollectionUtils.isNullOrEmpty(access.getBlackList())) {
            msg.append(NarcissusComponent.get().transAuto("list_is_empty", CommandUtils.getBlacklistOrWhitelistHelp(player, true)));
        } else {
            msg.append(NarcissusComponent.get().transAuto("list_detail"
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, true)
                    , access.getBlackList().stream().map(uuid -> PlayerUtils.getPlayerNameString(PlayerUtils.getPlayerByUUID(uuid))).collect(Collectors.joining(","))
            ));
        }
        MessageUtils.sendDefaultNotification(player, msg, EnumPosition.TOP_RIGHT, EnumMoveType.AUTO);
        return 1;
    }

    public static LiteralArgumentBuilder<CommandSource> create() {
        return Commands.literal("black")
                .then(Commands.literal("get")
                        .executes(BlacklistCommand::executeGet)
                )
                .then(Commands.literal("add")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(BlacklistCommand::executeAdd)
                        )
                )
                .then(Commands.literal("del")
                        .then(Commands.argument("players", EntityArgument.players())
                                .executes(BlacklistCommand::executeDel)
                        )
                );
    }
}
