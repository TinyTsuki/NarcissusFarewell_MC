package xin.vanilla.narcissus.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumOperationType;
import xin.vanilla.narcissus.util.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class VirtualOpCommand {
    private VirtualOpCommand() {
    }

    private static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandUtils.notifyHelp(context);
        CommandSourceStack source = context.getSource();
        if (CommandUtils.checkTeleportPre(context.getSource(), EnumCommandType.VIRTUAL_OP)) return 0;
        if (source.getEntity() == null || source.getEntity() instanceof ServerPlayer) {
            EnumOperationType type = EnumOperationType.fromString(StringArgumentType.getString(context, "operation"));
            EnumCommandType[] rules;
            try {
                rules = Arrays.stream(CommandUtils.getStringDefault(context, "rules", "").split(","))
                        .filter(StringUtils::isNotNullOrEmpty)
                        .map(String::trim)
                        .map(String::toUpperCase)
                        .map(EnumCommandType::valueOf).toArray(EnumCommandType[]::new);
            } catch (IllegalArgumentException ignored) {
                rules = new EnumCommandType[]{};
            }
            List<ServerPlayer> targetList = new ArrayList<>();
            try {
                targetList.addAll(EntityArgument.getPlayers(context, "player"));
            } catch (IllegalArgumentException ignored) {
            }
            String language = ServerConfig.DEFAULT_LANGUAGE.get();
            if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
                language = NarcissusUtils.getPlayerLanguage(source.getPlayerOrException());
            }
            for (ServerPlayer target : targetList) {
                switch (type) {
                    case ADD:
                        VirtualPermissionManager.addVirtualPermission(target, rules);
                        break;
                    case SET:
                        VirtualPermissionManager.setVirtualPermission(target, rules);
                        break;
                    case DEL:
                    case REMOVE:
                        VirtualPermissionManager.delVirtualPermission(target, rules);
                        break;
                    case CLEAR:
                        VirtualPermissionManager.clearVirtualPermission(target);
                        break;
                }
                String permissions = VirtualPermissionManager.buildPermissionsString(VirtualPermissionManager.getVirtualPermission(target));
                NarcissusUtils.sendTranslatableMessage(target, I18nUtils.getKey(EnumI18nType.FORMAT, "player_virtual_op"), target.getDisplayName().getString(), permissions);
                if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
                    ServerPlayer player = source.getPlayerOrException();
                    if (!target.getStringUUID().equalsIgnoreCase(player.getStringUUID())) {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "player_virtual_op"), target.getDisplayName().getString(), permissions);
                    }
                } else {
                    source.sendSuccess(Component.trans(language, EnumI18nType.FORMAT, "player_virtual_op", target.getDisplayName().getString(), permissions).toChatComponent(), true);
                }
                source.getServer().getPlayerList().sendPlayerPermissionLevel(target);
            }
        }
        return 1;
    }

    public static CompletableFuture<Suggestions> operationSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        builder.suggest(EnumOperationType.ADD.name().toLowerCase());
        builder.suggest(EnumOperationType.SET.name().toLowerCase());
        builder.suggest(EnumOperationType.DEL.name().toLowerCase());
        builder.suggest(EnumOperationType.CLEAR.name().toLowerCase());
        builder.suggest(EnumOperationType.GET.name().toLowerCase());
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> rulesSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String operation = StringArgumentType.getString(context, "operation");
        if (operation.equalsIgnoreCase(EnumOperationType.GET.name().toLowerCase())
                || operation.equalsIgnoreCase(EnumOperationType.CLEAR.name().toLowerCase())
                || operation.equalsIgnoreCase(EnumOperationType.LIST.name().toLowerCase())) {
            return builder.buildFuture();
        }
        String input = CommandUtils.getStringEmpty(context, "rules").replace(" ", ",");
        String[] split = input.split(",");
        String current = input.endsWith(",") ? "" : split[split.length - 1];
        Arrays.stream(EnumCommandType.values())
                .filter(EnumCommandType::isOp)
                .filter(type -> Arrays.stream(split).noneMatch(in -> in.equalsIgnoreCase(type.name())))
                .filter(type -> StringUtils.isNullOrEmptyEx(current) || type.name().toLowerCase().contains(current.toLowerCase()))
                .forEach(type -> builder.suggest(type.name()));
        return builder.buildFuture();
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create() {
        return Commands.literal(CommonConfig.COMMAND_VIRTUAL_OP.get())
                .requires(source -> NarcissusUtils.hasCommandPermission(source, EnumCommandType.VIRTUAL_OP))
                .then(Commands.argument("operation", StringArgumentType.word())
                        .suggests(VirtualOpCommand::operationSuggestion)
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes(VirtualOpCommand::execute)
                                .then(Commands.argument("rules", StringArgumentType.greedyString())
                                        .suggests(VirtualOpCommand::rulesSuggestion)
                                        .executes(VirtualOpCommand::execute)
                                )
                        )
                );
    }
}
