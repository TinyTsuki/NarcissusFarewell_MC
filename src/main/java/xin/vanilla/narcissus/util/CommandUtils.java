package xin.vanilla.narcissus.util;

import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.ServerConfig;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumMCColor;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class CommandUtils {
    private static final Logger LOGGER = LogManager.getLogger();


    public static final String[] WHITE_LIST_MODES = {"none", "both", "auto_accept_tpa", "auto_accept_tph"};

    public static String getLanguage(CommandSourceStack source) {
        String lang = ServerConfig.DEFAULT_LANGUAGE.get();
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer) {
            try {
                lang = NarcissusUtils.getPlayerLanguage(source.getPlayerOrException());
            } catch (Exception ignored) {
            }
        }
        return lang;
    }


    // region 指令参数相关

    public static void addSuggestion(SuggestionsBuilder suggestion, String input, String suggest) {
        if (suggest.contains(input) || StringUtils.isNullOrEmpty(input)) {
            suggestion.suggest(suggest);
        }
    }

    public static String getStringEmpty(CommandContext<?> context, String name) {
        return getStringDefault(context, name, "");
    }

    public static String getStringDefault(CommandContext<?> context, String name, String defaultValue) {
        String result;
        try {
            result = StringArgumentType.getString(context, name);
        } catch (IllegalArgumentException ignored) {
            result = defaultValue;
        }
        return result;
    }

    public static String getStringEx(CommandContext<?> context, String name, String defaultValue) {
        String result;
        try {
            result = String.valueOf(context.getArgument(name, Object.class));
        } catch (IllegalArgumentException ignored) {
            result = defaultValue;
        }
        return result;
    }

    public static String replaceResourcePath(String s) {
        if (StringUtils.isNullOrEmpty(s)) return "";
        return s.substring(s.indexOf(":") + 1);
    }

    public static int getIntDefault(CommandContext<?> context, String name, int defaultValue) {
        int result;
        try {
            result = IntegerArgumentType.getInteger(context, name);
        } catch (IllegalArgumentException ignored) {
            result = defaultValue;
        }
        return result;
    }

    public static long getLongDefault(CommandContext<?> context, String name, long defaultValue) {
        long result;
        try {
            result = LongArgumentType.getLong(context, name);
        } catch (IllegalArgumentException ignored) {
            result = defaultValue;
        }
        return result;
    }

    public static boolean getBooleanDefault(CommandContext<?> context, String name, boolean defaultValue) {
        boolean result;
        try {
            result = BoolArgumentType.getBool(context, name);
        } catch (IllegalArgumentException ignored) {
            result = defaultValue;
        }
        return result;
    }

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

    public static ServerLevel getDimensionDefault(CommandContext<CommandSourceStack> context, String name, ServerLevel defaultDimension) {
        ServerLevel result;
        try {
            result = DimensionArgument.getDimension(context, name);
        } catch (IllegalArgumentException | CommandSyntaxException e) {
            result = defaultDimension;
        }
        return result;
    }

    /**
     * 若为第一次使用指令则进行提示
     */
    public static void notifyHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        Entity entity = source.getEntity();
        if (entity instanceof ServerPlayer player) {
            PlayerTeleportData data = PlayerTeleportData.getData(player);
            if (!data.isNotified()) {
                Component button = Component.literal("/" + NarcissusUtils.getCommandPrefix())
                        .color(EnumMCColor.AQUA.getColor())
                        .clickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + NarcissusUtils.getCommandPrefix()))
                        .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("/" + NarcissusUtils.getCommandPrefix())
                                .toTextComponent())
                        );
                NarcissusUtils.sendMessage(player, Component.trans(EnumI18nType.FORMAT, "notify_help", button));
                data.setNotified(true);
            }
        }
    }

    public static boolean checkTeleportPre(CommandSourceStack source, EnumCommandType teleportType) {
        if (!NarcissusUtils.isCommandEnabled(teleportType)) {
            NarcissusUtils.sendTranslatableMessage(source, false, I18nUtils.getKey(EnumI18nType.FORMAT, "command_disabled"));
            return true;
        }
        if (source.getEntity() != null && source.getEntity() instanceof ServerPlayer player) {
            EnumTeleportType type = teleportType.toTeleportType();
            if (type != null) {
                int teleportCoolDown = NarcissusUtils.getTeleportCoolDown(player, type);
                if (teleportCoolDown > 0) {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "command_cooldown"), teleportCoolDown);
                    return true;
                }
            }
            if (ServerConfig.TP_WITH_ENEMY.get() && NarcissusUtils.isTargetedByHostile(player)) {
                NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "locked_by_mob"));
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
        if (ServerConfig.TP_WITH_ENEMY.get() && NarcissusUtils.isTargetedByHostile(request.getRequester())) {
            NarcissusUtils.sendTranslatableMessage(request.getRequester(), I18nUtils.getKey(EnumI18nType.FORMAT, "locked_by_mob"));
            result = false;
        }
        return !result;
    }

    public static boolean checkTeleportPost(ServerPlayer player, Coordinate target, EnumTeleportType type) {
        return checkTeleportPost(player, target, type, false);
    }

    public static boolean checkTeleportPost(ServerPlayer player, Coordinate target, EnumTeleportType type, boolean submit) {
        boolean result = NarcissusUtils.isTeleportAcrossDimensionEnabled(player, target.dimension(), type);
        result = result && NarcissusUtils.validTeleportCost(player, target, type, submit);
        if (ServerConfig.TP_WITH_ENEMY.get() && NarcissusUtils.isTargetedByHostile(player)) {
            NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.FORMAT, "locked_by_mob"));
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
            return Component.trans(EnumI18nType.FORMAT, "list_is_empty", getBlacklistOrWhitelistHelp(player, false));
        }
        String language = NarcissusUtils.getPlayerLanguage(player);
        Component playerList = Component.empty();
        access.getWhiteList().stream()
                .map(s -> {
                    Component flag = Component.literal(NarcissusUtils.getPlayerNameByUUIDString(s));
                    if (access.getAutoTpaList().contains(s)) {
                        flag.append(Component.literal("A")
                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.trans(EnumI18nType.FORMAT, "auto_accept_tpa").toChatComponent(language))));
                    }
                    if (access.getAutoTphList().contains(s)) {
                        flag.append(Component.literal("H")
                                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                        Component.trans(EnumI18nType.FORMAT, "auto_accept_tph").toChatComponent(language))));
                    }
                    if (CollectionUtils.isNotNullOrEmpty(flag.getChildren())) {
                        flag.appendIndex(0, "[").append("]");
                    }
                    return flag;
                }).forEach(playerList::append);
        return Component.trans(EnumI18nType.FORMAT, "list_detail", getBlacklistOrWhitelistHelp(player, false), playerList);
    }

    public static Component getBlacklistOrWhitelistHelp(Player player, boolean black) {
        return Component.trans(EnumI18nType.WORD, black ? "blacklist" : "whitelist")
                .hoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Component.trans(EnumI18nType.FORMAT, black ? "blacklist_help" : "whitelist_help")
                                .toChatComponent(NarcissusUtils.getPlayerLanguage(player))));
    }

    // endregion 指令参数相关


    // region suggestions

    public static CompletableFuture<Suggestions> dimSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String name = getStringEmpty(context, "dimension");
        String lang = getLanguage(context.getSource());
        Component dimTooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_dimension");
        for (String dim : DimensionUtils.getAllIds()) {
            if (StringUtils.isNullOrEmpty(name) || dim.contains(name))
                builder.suggest(dim, dimTooltip.toTextComponent());
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> safeSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String lang = getLanguage(context.getSource());
        Component safeTooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_safe");
        Component unsafeTooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_unsafe");
        builder.suggest("safe", safeTooltip.toTextComponent());
        builder.suggest("unsafe", unsafeTooltip.toTextComponent());
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> rangeSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String lang = getLanguage(context.getSource());
        for (int i = 1; i <= 5; i++) {
            int index = (int) Math.pow(10, i);
            if (index <= ServerConfig.TELEPORT_RANDOM_DISTANCE_LIMIT.get()) {
                Component tooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_range", index);
                builder.suggest(index, tooltip.toTextComponent());
            }
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> homeSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        String lang = getLanguage(context.getSource());
        Component tooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_home");
        for (KeyValue<String, String> key : data.getHomeCoordinate().keySet()) {
            builder.suggest(StringUtils.formatString(key.value()), tooltip.toTextComponent());
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> stageSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String lang = getLanguage(context.getSource());
        Component tooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_stage");
        for (KeyValue<String, String> key : WorldStageData.get().getStageCoordinate().keySet()) {
            builder.suggest(StringUtils.formatString(key.value()), tooltip.toTextComponent());
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> homeDimSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        PlayerTeleportData data = PlayerTeleportData.getData(player);
        String name = getStringEmpty(context, "name");
        String lang = getLanguage(context.getSource());
        Component tooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_dimension");
        for (KeyValue<String, String> keyValue : data.getHomeCoordinate().keySet()) {
            if (keyValue.value().equals(name))
                builder.suggest(keyValue.key(), tooltip.toTextComponent());
        }
        return builder.buildFuture();
    }

    public static CompletableFuture<Suggestions> stageDimSuggestion(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        WorldStageData data = WorldStageData.get();
        String name = getStringEmpty(context, "name");
        String lang = getLanguage(context.getSource());
        Component tooltip = Component.trans(lang, EnumI18nType.FORMAT, "suggest_dimension");
        for (KeyValue<String, String> keyValue : data.getStageCoordinate().keySet()) {
            if (name == null || keyValue.value().contains(name))
                builder.suggest(keyValue.key(), tooltip.toTextComponent());
        }
        return builder.buildFuture();
    }

    // endregion suggestions

}
