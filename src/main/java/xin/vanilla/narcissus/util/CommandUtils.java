package xin.vanilla.narcissus.util;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
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

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 水仙辞指令工具。与 Brigadier 参数解析、补全等通用逻辑委托至 {@link xin.vanilla.banira.common.util.CommandUtils}。
 */
public final class CommandUtils {
    private CommandUtils() {
    }

    public static final String[] WHITE_LIST_MODES = {"none", "both", "auto_accept_tpa", "auto_accept_tph"};


    // region 指令参数相关（委托 BaniraCodex）

    public static String getLanguage(CommandSourceStack source) {
        return xin.vanilla.banira.common.util.CommandUtils.getLanguage(source);
    }

    public static void addSuggestion(SuggestionsBuilder suggestion, String input, String suggest) {
        xin.vanilla.banira.common.util.CommandUtils.addSuggestion(suggestion, input, suggest);
    }

    public static String getStringEmpty(CommandContext<?> context, String name) {
        return xin.vanilla.banira.common.util.CommandUtils.getStringEmpty(context, name);
    }

    public static String getStringDefault(CommandContext<?> context, String name, String defaultValue) {
        return xin.vanilla.banira.common.util.CommandUtils.getStringDefault(context, name, defaultValue);
    }

    public static String getStringEx(CommandContext<?> context, String name, String defaultValue) {
        return xin.vanilla.banira.common.util.CommandUtils.getStringEx(context, name, defaultValue);
    }

    public static String replaceResourcePath(String s) {
        return xin.vanilla.banira.common.util.CommandUtils.replaceResourcePath(s);
    }

    public static int getIntDefault(CommandContext<?> context, String name, int defaultValue) {
        return xin.vanilla.banira.common.util.CommandUtils.getIntDefault(context, name, defaultValue);
    }

    public static long getLongDefault(CommandContext<?> context, String name, long defaultValue) {
        return xin.vanilla.banira.common.util.CommandUtils.getLongDefault(context, name, defaultValue);
    }

    public static boolean getBooleanDefault(CommandContext<?> context, String name, boolean defaultValue) {
        return xin.vanilla.banira.common.util.CommandUtils.getBooleanDefault(context, name, defaultValue);
    }

    public static ServerLevel getDimensionDefault(CommandContext<CommandSourceStack> context, String name, ServerLevel defaultDimension) {
        return xin.vanilla.banira.common.util.CommandUtils.getDimensionDefault(context, name, defaultDimension);
    }

    // endregion 指令参数相关（委托 BaniraCodex）


    /**
     * 尝试获取布尔参数，若无则返回 null
     */
    @Nullable
    public static Boolean getBooleanOptional(CommandContext<?> context, String name) {
        try {
            return BoolArgumentType.getBool(context, name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * 尝试获取 Double 参数，若无则返回 null
     */
    @Nullable
    public static Double getDoubleOptional(CommandContext<?> context, String name) {
        try {
            return DoubleArgumentType.getDouble(context, name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * 尝试获取玩家参数，若无则返回执行者自身（若执行者为玩家），否则抛出 CommandSyntaxException
     */
    public static ServerPlayer getPlayerOrSelf(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        try {
            return EntityArgument.getPlayer(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            CommandSourceStack source = context.getSource();
            if (source.getEntity() instanceof ServerPlayer) {
                return source.getPlayerOrException();
            }
            throw CommandSourceStack.ERROR_NOT_PLAYER.create();
        }
    }

    /**
     * 尝试获取玩家参数，若无则返回 null
     */
    @Nullable
    public static ServerPlayer getPlayerOptional(CommandContext<CommandSourceStack> context, String name) {
        try {
            return EntityArgument.getPlayer(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return null;
        }
    }

    /**
     * 尝试获取玩家列表参数，若无则返回 fallback
     */
    public static Collection<ServerPlayer> getPlayersOptional(CommandContext<CommandSourceStack> context, String name, Collection<ServerPlayer> fallback) {
        try {
            return EntityArgument.getPlayers(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return fallback;
        }
    }

    /**
     * 尝试获取维度参数，若无则返回 defaultValue 对应的 ResourceKey
     */
    public static ResourceKey<Level> getDimensionKeyDefault(CommandContext<CommandSourceStack> context, String name, ResourceKey<Level> defaultValue) {
        try {
            return DimensionArgument.getDimension(context, name).dimension();
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            return defaultValue;
        }
    }

    /**
     * 若为第一次使用指令则进行提示（文案使用 BaniraCodex 的 format.notify_help，模组名来自水仙辞语言条目）
     */
    public static void notifyHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Entity entity = source.getEntity();
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        String cmd = "/" + NarcissusUtils.getCommandPrefix();
        Component modName = NarcissusComponent.get().trans("key.narcissus_farewell.categories");
        xin.vanilla.banira.common.util.CommandUtils.notifyHelp(context, data, modName, cmd);
    }

    public static boolean checkTeleportPre(CommandSourceStack source, EnumCommandType teleportType) {
        if (!NarcissusUtils.isCommandEnabled(teleportType)) {
            MessageUtils.sendMessage(source, false, NarcissusComponent.get().transAuto("command_disabled"));
            return true;
        }
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer player) {
            EnumTeleportType type = teleportType.toTeleportType();
            if (type != null) {
                int teleportCoolDown = NarcissusUtils.getTeleportCoolDown(player, type);
                if (teleportCoolDown > 0) {
                    MessageUtils.sendMessage(player, NarcissusComponent.get().transAuto("command_cooldown", teleportCoolDown));
                    return true;
                }
            }
            if (CommonConfig.get().general().tpWithEnemy() && NarcissusUtils.isTargetedByHostile(player)) {
                MessageUtils.sendMessage(player, NarcissusComponent.get().transAuto("locked_by_mob"));
                return true;
            }
        }
        return false;
    }

    public static boolean checkTeleportPost(TeleportRequest request) {
        return checkTeleportPost(request, false);
    }

    public static boolean checkTeleportPost(TeleportRequest request, boolean submit) {
        boolean result = NarcissusUtils.isTeleportAcrossDimensionEnabled(request.getRequester(), request.getTarget().level().dimension(), request.getTeleportType());
        result = result && NarcissusUtils.validTeleportCost(request, submit);
        if (CommonConfig.get().general().tpWithEnemy() && NarcissusUtils.isTargetedByHostile(request.getRequester())) {
            MessageUtils.sendMessage(request.getRequester(), NarcissusComponent.get().transAuto("locked_by_mob"));
            result = false;
        }
        return !result;
    }

    public static boolean checkTeleportPost(ServerPlayer player, SafeWorldCoordinate target, EnumTeleportType type) {
        return checkTeleportPost(player, target, type, false);
    }

    public static boolean checkTeleportPost(ServerPlayer player, SafeWorldCoordinate target, EnumTeleportType type, boolean submit) {
        boolean result = NarcissusUtils.isTeleportAcrossDimensionEnabled(player, target.dimension(), type);
        result = result && NarcissusUtils.validTeleportCost(player, target, type, submit);
        if (CommonConfig.get().general().tpWithEnemy() && NarcissusUtils.isTargetedByHostile(player)) {
            MessageUtils.sendMessage(player, NarcissusComponent.get().transAuto("locked_by_mob"));
            result = false;
        }
        return !result;
    }

    public static String getRequestId(CommandContext<CommandSourceStack> context, EnumTeleportType teleportType, boolean isTarget) {
        String result = null;
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            try {
                ServerPlayer requester = EntityArgument.getPlayer(context, "player");
                Map.Entry<String, TeleportRequest> entry1 = NarcissusFarewell.getTeleportRequest().entrySet().stream()
                        .filter(entry -> isTarget ? entry.getValue().getTarget().getUUID().equals(player.getUUID()) : entry.getValue().getRequester().getUUID().equals(player.getUUID()))
                        .filter(entry -> isTarget ? entry.getValue().getRequester().getUUID().equals(requester.getUUID()) : entry.getValue().getTarget().getUUID().equals(requester.getUUID()))
                        .filter(entry -> entry.getValue().getTeleportType() == teleportType)
                        .max(Comparator.comparing(entry -> entry.getValue().getRequestTime()))
                        .orElse(null);
                if (entry1 != null) result = entry1.getKey();
            } catch (IllegalArgumentException ignored) {
                try {
                    result = getStringEmpty(context, "requestId");
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
                                .toList();
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

    public static SuggestionProvider<CommandSourceStack> buildReqIndexSuggestions(EnumTeleportType teleportType, boolean isTarget) {
        return (context, builder) -> {
            ServerPlayer player = context.getSource().getPlayerOrException();
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

    public static Component getWhiteListMessage(ServerPlayer player, PlayerAccess access) {
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

    public static Component getBlacklistOrWhitelistHelp(Player player, boolean black) {
        return NarcissusComponent.get().transAuto(black ? "blacklist_tag" : "whitelist_tag")
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        NarcissusComponent.get().transAuto(black ? "blacklist_help" : "whitelist_help")
                                .toChat(NarcissusLang.getPlayerLanguage(player))));
    }

    // region suggestions

    public static CompletableFuture<Suggestions> dimSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String name = getStringEmpty(context, "dimension");
        String lang = getLanguage(context.getSource());
        Component dimTooltip = NarcissusComponent.get().transAuto("suggest_dimension");
        for (String dim : DimensionUtils.getAllIds()) {
            if (StringUtils.isNullOrEmpty(name) || dim.contains(name))
                builder.suggest(dim, dimTooltip.toVanilla(lang));
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> safeSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String lang = getLanguage(context.getSource());
        Component safeTooltip = NarcissusComponent.get().transAuto("suggest_safe");
        Component unsafeTooltip = NarcissusComponent.get().transAuto("suggest_unsafe");
        builder.suggest("safe", safeTooltip.toVanilla(lang));
        builder.suggest("unsafe", unsafeTooltip.toVanilla(lang));
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> rangeSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String lang = getLanguage(context.getSource());
        for (int i = 1; i <= 5; i++) {
            int index = (int) Math.pow(10, i);
            if (index <= CommonConfig.get().general().teleportRandomDistanceLimit()) {
                Component tooltip = NarcissusComponent.get().transAuto("suggest_range", index);
                builder.suggest(index, tooltip.toVanilla(lang));
            }
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> homeSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        String lang = getLanguage(context.getSource());
        Component tooltip = NarcissusComponent.get().transAuto("suggest_home");
        for (KeyValue<String, String> key : data.getHomeCoordinate().keySet()) {
            builder.suggest(StringUtils.formatString(key.value()), tooltip.toVanilla(lang));
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> stageSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String lang = getLanguage(context.getSource());
        Component tooltip = NarcissusComponent.get().transAuto("suggest_stage");
        for (KeyValue<String, String> key : WorldStageData.get().getStageCoordinate().keySet()) {
            builder.suggest(StringUtils.formatString(key.value()), tooltip.toVanilla(lang));
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> homeDimSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        String name = getStringEmpty(context, "name");
        String lang = getLanguage(context.getSource());
        Component tooltip = NarcissusComponent.get().transAuto("suggest_dimension");
        for (KeyValue<String, String> keyValue : data.getHomeCoordinate().keySet()) {
            if (keyValue.value().equals(name))
                builder.suggest(keyValue.key(), tooltip.toVanilla(lang));
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> stageDimSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        WorldStageData data = WorldStageData.get();
        String name = getStringEmpty(context, "name");
        String lang = getLanguage(context.getSource());
        Component tooltip = NarcissusComponent.get().transAuto("suggest_dimension");
        for (KeyValue<String, String> keyValue : data.getStageCoordinate().keySet()) {
            if (name == null || keyValue.value().contains(name))
                builder.suggest(keyValue.key(), tooltip.toVanilla(lang));
        }
        return builder.buildFuture();
    }

    // endregion suggestions

}
