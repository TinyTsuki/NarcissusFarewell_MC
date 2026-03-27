package xin.vanilla.narcissus.util;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.event.HoverEvent;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 水仙辞指令工具。与 Brigadier 参数解析、补全等通用逻辑委托至 {@link xin.vanilla.banira.common.util.CommandUtils}。
 */
public final class CommandUtils {
    private CommandUtils() {
    }

    public static final String[] WHITE_LIST_MODES = {"none", "both", "auto_accept_tpa", "auto_accept_tph"};

    /**
     * 若为第一次使用指令则进行提示
     */
    public static void notifyHelp(CommandContext<CommandSource> context) {
        CommandSource source = context.getSource();
        Entity entity = source.getEntity();
        if (!(entity instanceof ServerPlayerEntity)) return;
        ServerPlayerEntity player = (ServerPlayerEntity) entity;
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        String cmd = "/" + NarcissusUtils.getCommandPrefix();
        Component modName = NarcissusComponent.get().trans("key.narcissus_farewell.categories");
        xin.vanilla.banira.common.util.CommandUtils.notifyHelp(context, data, modName, cmd);
    }

    public static boolean checkTeleportPre(CommandSource source, EnumCommandType teleportType) {
        if (!NarcissusUtils.isCommandEnabled(teleportType)) {
            MessageUtils.sendMessage(source, false, NarcissusComponent.get().transAuto("command_disabled"));
            return true;
        }
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayerEntity) {
            ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
            EnumTeleportType type = teleportType.toTeleportType();
            if (type != null) {
                int teleportCoolDown = NarcissusUtils.getTeleportCoolDown(player, type);
                if (teleportCoolDown > 0) {
                    MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("command_cooldown", teleportCoolDown));
                    return true;
                }
            }
            if (CommonConfig.get().general().tpWithEnemy() && NarcissusUtils.isTargetedByHostile(player)) {
                MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("locked_by_mob"));
                return true;
            }
        }
        return false;
    }

    public static boolean checkTeleportPost(TeleportRequest request) {
        return checkTeleportPost(request, false);
    }

    public static boolean checkTeleportPost(TeleportRequest request, boolean submit) {
        boolean result = NarcissusUtils.isTeleportAcrossDimensionEnabled(request.getRequester(), request.getTarget().getLevel().dimension(), request.getTeleportType());
        result = result && NarcissusUtils.validTeleportCost(request, submit);
        if (CommonConfig.get().general().tpWithEnemy() && NarcissusUtils.isTargetedByHostile(request.getRequester())) {
            MessageUtils.sendNotification(request.getRequester(), NarcissusComponent.get().transAuto("locked_by_mob"));
            result = false;
        }
        return !result;
    }

    public static boolean checkTeleportPost(ServerPlayerEntity player, SafeWorldCoordinate target, EnumTeleportType type) {
        return checkTeleportPost(player, target, type, false);
    }

    public static boolean checkTeleportPost(ServerPlayerEntity player, SafeWorldCoordinate target, EnumTeleportType type, boolean submit) {
        boolean result = NarcissusUtils.isTeleportAcrossDimensionEnabled(player, target.dimension(), type);
        result = result && NarcissusUtils.validTeleportCost(player, target, type, submit);
        if (CommonConfig.get().general().tpWithEnemy() && NarcissusUtils.isTargetedByHostile(player)) {
            MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("locked_by_mob"));
            result = false;
        }
        return !result;
    }

    public static String getRequestId(CommandContext<CommandSource> context, EnumTeleportType teleportType, boolean isTarget) {
        String result = null;
        try {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            try {
                ServerPlayerEntity requester = EntityArgument.getPlayer(context, "player");
                Map.Entry<String, TeleportRequest> entry1 = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                        .filter(entry -> isTarget ? entry.getValue().getTarget().getUUID().equals(player.getUUID()) : entry.getValue().getRequester().getUUID().equals(player.getUUID()))
                        .filter(entry -> isTarget ? entry.getValue().getRequester().getUUID().equals(requester.getUUID()) : entry.getValue().getTarget().getUUID().equals(requester.getUUID()))
                        .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                        .max(Comparator.comparing(entry -> entry.getValue().getRequestTime()))
                        .orElse(null);
                if (entry1 != null) result = entry1.getKey();
            } catch (IllegalArgumentException ignored) {
                try {
                    result = xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, "requestId");
                    if (!NarcissusFarewell.getTeleportRequest().containsKey(result))
                        result = null;
                } catch (IllegalArgumentException ignored1) {
                    try {
                        int askIndex = IntegerArgumentType.getInteger(context, "requestIndex");
                        List<Map.Entry<String, TeleportRequest>> entryList = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                                .filter(entry -> !isTarget || !entry.getValue().isIgnore())
                                .filter(entry -> isTarget ? entry.getValue().getTarget().getUUID().equals(player.getUUID()) : entry.getValue().getRequester().getUUID().equals(player.getUUID()))
                                .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                                .sorted(Comparator.comparing(entry -> -entry.getValue().getRequestTime().getTime()))
                                .collect(Collectors.toList());
                        if (askIndex > 0 && askIndex <= entryList.size()) result = entryList.get(askIndex - 1).getKey();
                    } catch (IllegalArgumentException ignored2) {
                        Map.Entry<String, TeleportRequest> entry1 = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                                .filter(entry -> !isTarget || !entry.getValue().isIgnore())
                                .filter(entry -> isTarget ? entry.getValue().getTarget().getUUID().equals(player.getUUID()) : entry.getValue().getRequester().getUUID().equals(player.getUUID()))
                                .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                                .max(Comparator.comparing(entry -> entry.getValue().getRequestTime()))
                                .orElse(null);
                        if (entry1 != null) result = entry1.getKey();
                    }
                }
            }
        } catch (CommandSyntaxException ignored) {
        }
        return result;
    }

    public static SuggestionProvider<CommandSource> buildReqIndexSuggestions(EnumTeleportType teleportType, boolean isTarget) {
        return (context, builder) -> {
            ServerPlayerEntity player = context.getSource().getPlayerOrException();
            NarcissusFarewell.getTeleportRequest().entrySet().stream()
                    .filter(entry -> isTarget ? entry.getValue().getRequester().getUUID().equals(player.getUUID()) : entry.getValue().getTarget().getUUID().equals(player.getUUID()))
                    .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                    .forEach(entry -> builder.suggest(entry.getKey()));
            long count = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                    .filter(entry -> isTarget ? entry.getValue().getRequester().getUUID().equals(player.getUUID()) : entry.getValue().getTarget().getUUID().equals(player.getUUID()))
                    .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                    .count();
            for (int i = 0; i < count; i++) builder.suggest(i + 1);
            return builder.buildFuture();
        };
    }

    public static Component getWhiteListMessage(ServerPlayerEntity player, PlayerAccess access) {
        if (CollectionUtils.isNullOrEmpty(access.getWhiteList())) {
            return NarcissusComponent.get().transAuto("list_is_empty", getBlacklistOrWhitelistHelp(player, false));
        }
        String language = NarcissusLang.getPlayerLanguage(player);
        Component playerList = NarcissusComponent.get().empty();
        access.getWhiteList().stream()
                .map(s -> {
                    Component flag = NarcissusComponent.get().literal(PlayerUtils.getPlayerNameString(PlayerUtils.getPlayerByUUID(s)));
                    if (access.getAutoTpaList().contains(s)) {
                        flag.append(NarcissusComponent.get().literal("A")
                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        NarcissusComponent.get().transAuto("auto_accept_tpa").toChat(language))));
                    }
                    if (access.getAutoTphList().contains(s)) {
                        flag.append(NarcissusComponent.get().literal("H")
                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        NarcissusComponent.get().transAuto("auto_accept_tph").toChat(language))));
                    }
                    if (CollectionUtils.isNotNullOrEmpty(flag.getChildren())) {
                        flag.appendIndex(0, "[").append("]");
                    }
                    return flag;
                }).forEach(playerList::append);
        return NarcissusComponent.get().transAuto("list_detail", getBlacklistOrWhitelistHelp(player, false), playerList);
    }

    public static Component getBlacklistOrWhitelistHelp(PlayerEntity player, boolean black) {
        return NarcissusComponent.get().transAuto(black ? "blacklist_tag" : "whitelist_tag")
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        NarcissusComponent.get().transAuto(black ? "blacklist_help" : "whitelist_help")
                                .toChat(NarcissusLang.getPlayerLanguage(player))));
    }

    // region suggestions

    public static CompletableFuture<Suggestions> dimSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String name = xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, "dimension");
        String lang = xin.vanilla.banira.common.util.CommandUtils.getLanguage(context.getSource());
        Component dimTooltip = NarcissusComponent.get().transAuto("suggest_dimension");
        for (String dim : DimensionUtils.getAllIds()) {
            if (StringUtils.isNullOrEmpty(name) || dim.contains(name))
                builder.suggest(dim, dimTooltip.toVanilla(lang));
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> safeSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String lang = xin.vanilla.banira.common.util.CommandUtils.getLanguage(context.getSource());
        Component safeTooltip = NarcissusComponent.get().transAuto("suggest_safe");
        Component unsafeTooltip = NarcissusComponent.get().transAuto("suggest_unsafe");
        builder.suggest("safe", safeTooltip.toVanilla(lang));
        builder.suggest("unsafe", unsafeTooltip.toVanilla(lang));
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> rangeSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String lang = xin.vanilla.banira.common.util.CommandUtils.getLanguage(context.getSource());
        for (int i = 1; i <= 5; i++) {
            int index = (int) Math.pow(10, i);
            if (index <= CommonConfig.get().general().teleportRandomDistanceLimit()) {
                Component tooltip = NarcissusComponent.get().transAuto("suggest_range", index);
                builder.suggest(index, tooltip.toVanilla(lang));
            }
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> homeSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        String lang = xin.vanilla.banira.common.util.CommandUtils.getLanguage(context.getSource());
        Component tooltip = NarcissusComponent.get().transAuto("suggest_home");
        for (KeyValue<String, String> key : data.getHomeCoordinate().keySet()) {
            builder.suggest(StringUtils.formatString(key.value()), tooltip.toVanilla(lang));
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> stageSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        String lang = xin.vanilla.banira.common.util.CommandUtils.getLanguage(context.getSource());
        Component tooltip = NarcissusComponent.get().transAuto("suggest_stage");
        for (KeyValue<String, String> key : WorldStageData.get().getStageCoordinate().keySet()) {
            builder.suggest(StringUtils.formatString(key.value()), tooltip.toVanilla(lang));
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> homeDimSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        String name = xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, "name");
        String lang = xin.vanilla.banira.common.util.CommandUtils.getLanguage(context.getSource());
        Component tooltip = NarcissusComponent.get().transAuto("suggest_dimension");
        for (KeyValue<String, String> keyValue : data.getHomeCoordinate().keySet()) {
            if (keyValue.value().equals(name))
                builder.suggest(keyValue.key(), tooltip.toVanilla(lang));
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> stageDimSuggestion(CommandContext<CommandSource> context, SuggestionsBuilder builder) {
        WorldStageData data = WorldStageData.get();
        String name = xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, "name");
        String lang = xin.vanilla.banira.common.util.CommandUtils.getLanguage(context.getSource());
        Component tooltip = NarcissusComponent.get().transAuto("suggest_dimension");
        for (KeyValue<String, String> keyValue : data.getStageCoordinate().keySet()) {
            if (name == null || keyValue.value().contains(name))
                builder.suggest(keyValue.key(), tooltip.toVanilla(lang));
        }
        return builder.buildFuture();
    }

    // endregion suggestions

}
