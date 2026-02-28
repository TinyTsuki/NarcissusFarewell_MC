package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.util.CollectionUtils;
import xin.vanilla.narcissus.util.CommandUtils;
import xin.vanilla.narcissus.util.Component;
import xin.vanilla.narcissus.util.NarcissusUtils;

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
            msg = Component.trans(EnumI18nType.FORMAT, "list_is_empty", CommandUtils.getBlacklistOrWhitelistHelp(player, true));
        } else {
            msg = Component.trans(EnumI18nType.FORMAT, "list_detail"
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, true)
                    , access.getBlackList().stream()
                            .map(NarcissusUtils::getPlayerNameByUUIDString)
                            .collect(Collectors.joining(","))
            );
        }
        NarcissusUtils.sendMessage(player, msg);
        return 1;
    }

    private static int executeAdd(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> players = EntityArgument.getPlayers(context, "players");
        if (CollectionUtils.isNullOrEmpty(players)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        PlayerAccess access = data.getAccess();
        String[] uuids = players.stream().map(NarcissusUtils::getPlayerUUIDString).toArray(String[]::new);
        Component msg;
        if (access.addBlackList(uuids)) {
            data.setDirty();
            msg = Component.trans(EnumI18nType.FORMAT, "list_add_success"
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, true)
                    , players.stream().map(NarcissusUtils::getPlayerName).collect(Collectors.joining(","))
            );
        } else {
            msg = Component.trans(EnumI18nType.FORMAT, "list_add_fail", CommandUtils.getBlacklistOrWhitelistHelp(player, true), CommandUtils.getBlacklistOrWhitelistHelp(player, false));
        }
        NarcissusUtils.sendMessage(player, msg);
        return 1;
    }

    private static int executeDel(CommandContext<CommandSource> context) throws CommandSyntaxException {
        Collection<ServerPlayerEntity> players = EntityArgument.getPlayers(context, "players");
        if (CollectionUtils.isNullOrEmpty(players)) return 0;
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        PlayerAccess access = data.getAccess();
        String[] uuids = players.stream().map(NarcissusUtils::getPlayerUUIDString).toArray(String[]::new);
        access.removeBlackList(uuids);
        data.setDirty();
        Component msg = Component.trans(EnumI18nType.FORMAT, "remove_success");
        if (CollectionUtils.isNullOrEmpty(access.getBlackList())) {
            msg.append(Component.trans(EnumI18nType.FORMAT, "list_is_empty", CommandUtils.getBlacklistOrWhitelistHelp(player, true)));
        } else {
            msg.append(Component.trans(EnumI18nType.FORMAT, "list_detail"
                    , CommandUtils.getBlacklistOrWhitelistHelp(player, true)
                    , access.getBlackList().stream().map(NarcissusUtils::getPlayerNameByUUIDString).collect(Collectors.joining(","))
            ));
        }
        NarcissusUtils.sendMessage(player, msg);
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
