package xin.vanilla.narcissus.internal.client.dev;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import xin.vanilla.banira.api.client.theme.BaniraThemes;
import xin.vanilla.banira.api.client.notification.BaniraClientNotificationTypes;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.NotificationLogEntry;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.notification.NotificationTypeRegistry;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore.TypeSettings;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.NotificationData;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationStyle;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeNotifications;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Checks received logs and native chat; themes are public-factory checks, not live queue inspection. */
public final class NarcissusNetworkSmokeNotificationsCheck {
    private boolean verified;
    private final long startedAt = System.currentTimeMillis();

    public boolean verifyWhenReceived(Minecraft client, String phase) {
        if (verified) return true;
        List<NotificationLogEntry> log = NotificationManager.get().getLog();
        Map<String, NotificationLogEntry> received = received(log, phase);
        if (received.size() != 10) return false;
        NotificationLogEntry help = actualEntry(log, NarcissusNotificationTypes.INTERACTIVE_HELP, startedAt);
        NotificationLogEntry missing = actualEntry(log, NarcissusNotificationTypes.TELEPORT_REQUEST, startedAt);
        if (help == null || missing == null) return false;
        List<net.minecraft.network.chat.Component> chat = chatMessages(client);
        verifyChat(chat, phase, client.level.registryAccess());
        verifyActualChat(chat, help, missing, client.level.registryAccess());
        BaniraColorConfig theme = BaniraColorConfig.forSeason(BaniraThemes.seasonFor(NarcissusFarewell.MODID));
        int factories = 0;
        for (String type : NarcissusNotificationTypes.ALL_TYPE_IDS) {
            verifyRegistration(type);
            NotificationLogEntry entry = received.get(type);
            Component component = entry.component();
            NarcissusNetworkSmokeNotifications.verifyNested(component, phase, type);
            EnumNotificationTypeDisplayMode mode = NarcissusNetworkSmokeNotifications.expectedDisplay(type);
            require(component.color().argb() == (mode == EnumNotificationTypeDisplayMode.OVERLAY
                    ? theme.notificationNormalText() : 0xFFFFFFFF), "Received root theme/text " + type);
            EnumPosition position = NarcissusNotificationTypes.TELEPORT_SEARCH.equals(type)
                    || NarcissusNotificationTypes.WAYPOINT.equals(type) ? EnumPosition.TOP_RIGHT : EnumPosition.TOP_CENTER;
            require(entry.position() == position && entry.animation() == EnumMoveType.AUTO
                    && entry.durationTime() == 5000L && entry.style() == EnumNotificationStyle.NORMAL,
                    "Received default layout/style " + type);

            Notification normal = Notification.fromData(data(entry, component, entry.style()), true);
            verifyFactory(normal, data(entry, component, entry.style()), mode, entry.style(), theme, phase, type);
            factories += verifyMatrix(entry, theme, phase);
            NarcissusNetworkSmokeStatus.append("PASS notification-type phase=" + phase + " type=" + type
                    + " source=network display=" + mode + " nested=preserved token="
                    + NarcissusNetworkSmokeNotifications.token(phase, type));
        }
        require(factories == 120, "Incomplete notification factory matrix " + factories);
        NarcissusNetworkSmokeNotifications.recordRuntime();
        NarcissusNetworkSmokeStatus.append("PASS notification-real-callers phase=" + phase
                + " help=network-chat missing-request=network-overlay teleports=0");
        NarcissusNetworkSmokeStatus.append("PASS notification-coverage phase=" + phase
                + " routes=synthetic-real-MessageUtils commands=configured-help,guarded-empty-tpaskyes"
                + " themes=factory-only live-queue=not-inspected other-gameplay-callers=not-executed");
        NarcissusNetworkSmokeStatus.append(NarcissusNetworkSmokeNotifications.verifiedMarker(phase));
        verified = true;
        return true;
    }

    public boolean verified() { return verified; }

    static NotificationLogEntry actualEntry(List<NotificationLogEntry> log, String type, long startedAt) {
        NotificationLogEntry found = null;
        for (NotificationLogEntry entry : log) {
            if (!type.equals(entry.notificationType()) || !"network".equals(entry.source()) || entry.timestamp() < startedAt) continue;
            Component component = entry.component();
            boolean matches = NarcissusNotificationTypes.INTERACTIVE_HELP.equals(type)
                    ? component.text().contains("Narcissus Farewell")
                    : "tp_ask_not_found".equals(component.text()) && NarcissusFarewell.MODID.equals(component.modId());
            if (!matches) continue;
            require(found == null, "Duplicate actual command feedback " + type);
            found = entry;
        }
        return found;
    }

    private static void verifyActualChat(List<net.minecraft.network.chat.Component> chat, NotificationLogEntry help,
                                         NotificationLogEntry missing, HolderLookup.Provider registries) {
        Component helpComponent = help.component();
        require(helpComponent.color().argb() == 0xFFFFFFFF, "Actual HELP acquired theme text");
        require(hasInteractiveChild(helpComponent), "Actual HELP lost click/hover controls");
        net.minecraft.network.chat.Component expected = helpComponent.toChat();
        int count = 0;
        String missingText = missing.component().toChat().getString();
        for (net.minecraft.network.chat.Component message : chat) {
            require(!missingText.equals(message.getString()), "Actual overlay feedback leaked into chat");
            if (!expected.getString().equals(message.getString())) continue;
            require(net.minecraft.network.chat.Component.Serializer.toJson(expected, registries)
                            .equals(net.minecraft.network.chat.Component.Serializer.toJson(message, registries)),
                    "Actual HELP native chat lost rich text");
            count++;
        }
        require(count == 1, "Actual HELP absent or duplicated in native chat: " + count);
    }

    private static boolean hasInteractiveChild(Component component) {
        if (component.clickEvent() != null && component.hoverEvent() != null) return true;
        for (Component child : component.getChildren()) if (hasInteractiveChild(child)) return true;
        return false;
    }

    static Map<String, NotificationLogEntry> received(List<NotificationLogEntry> log, String phase) {
        Map<String, NotificationLogEntry> result = new LinkedHashMap<>();
        for (NotificationLogEntry entry : log) {
            Component component = entry.component();
            for (String type : NarcissusNotificationTypes.ALL_TYPE_IDS) {
                String token = NarcissusNetworkSmokeNotifications.token(phase, type);
                if (!token.equals(component.text())) continue;
                require("network".equals(entry.source()), "Non-network notification token " + token);
                require(type.equals(entry.notificationType()), "Wrong notification route " + token);
                require(!result.containsKey(type), "Duplicate network notification " + token);
                NarcissusNetworkSmokeNotifications.verifyNested(component, phase, type);
                result.put(type, entry);
            }
        }
        return result;
    }

    private static List<net.minecraft.network.chat.Component> chatMessages(Minecraft client) {
        // Forge 52 uses official names at runtime; inspect full messages, not wrapped lines.
        List<GuiMessage> lines = ObfuscationReflectionHelper.getPrivateValue(
                ChatComponent.class, client.gui.getChat(), "allMessages");
        require(lines != null, "Native chat allMessages unavailable");
        List<net.minecraft.network.chat.Component> messages = new ArrayList<>();
        for (GuiMessage line : lines) messages.add(line.content());
        return messages;
    }

    static void verifyChat(List<net.minecraft.network.chat.Component> messages, String phase, HolderLookup.Provider registries) {
        for (String type : NarcissusNotificationTypes.ALL_TYPE_IDS) {
            String token = NarcissusNetworkSmokeNotifications.token(phase, type);
            boolean chat = NarcissusNetworkSmokeNotifications.expectedDisplay(type) == EnumNotificationTypeDisplayMode.VANILLA_CHAT;
            int found = 0;
            for (net.minecraft.network.chat.Component message : messages) {
                if (!message.getString().contains(token)) continue;
                found++;
                require(chat, "Overlay notification leaked into native chat " + token);
                net.minecraft.network.chat.Component expected = NarcissusNetworkSmokeNotifications.payload(phase, type).toChat("en_us");
                require(net.minecraft.network.chat.Component.Serializer.toJson(expected, registries)
                                .equals(net.minecraft.network.chat.Component.Serializer.toJson(message, registries)),
                        "Native chat text/color/click/hover changed " + token);
            }
            require(found == (chat ? 1 : 0), "Native chat destination count=" + found + " " + token);
        }
    }

    private static void verifyRegistration(String type) {
        EnumNotificationTypeDisplayMode expected = NarcissusNetworkSmokeNotifications.expectedDisplay(type);
        require(NotificationTypeRegistry.knownTypesSorted().contains(type), "Unknown client type " + type);
        Component tooltip = BaniraClientNotificationTypes.tooltip(type);
        require(tooltip != null && !tooltip.isEmpty(), "Missing type tooltip " + type);
        require(NarcissusNotificationTypes.defaultDisplay(type) == expected
                && NotificationTypeRegistry.resolvedDisplayDefault(type) == expected, "Wrong registered default " + type);
        TypeSettings settings = NotificationTypeSettingsStore.get().getOrCreate(type);
        require(settings.displayMode() == expected && !settings.hidden() && !settings.displayModeCustomized(),
                "Wrong resolved settings " + type);
    }

    private static int verifyMatrix(NotificationLogEntry entry, BaniraColorConfig theme, String phase) {
        TypeSettings settings = NotificationTypeSettingsStore.get().getOrCreate(entry.notificationType());
        int count = 0;
        for (EnumNotificationTypeDisplayMode mode : EnumNotificationTypeDisplayMode.values()) {
            for (EnumNotificationStyle style : EnumNotificationStyle.values()) {
                // The log already contains the default overlay root color; reset only that root for each semantic style.
                Component component = entry.component().color(0xFFFFFFFF);
                NotificationData data = data(entry, component, style);
                withDisplay(settings, mode, () -> verifyFactory(Notification.fromData(data, true), data,
                        mode, style, theme, phase, entry.notificationType()));
                count++;
            }
        }
        return count;
    }

    private static NotificationData data(NotificationLogEntry entry, Component component, EnumNotificationStyle style) {
        return NotificationData.of(component, entry.position(), entry.animation(), entry.durationTime(), style, entry.notificationType());
    }

    private static void verifyFactory(Notification notification, NotificationData data, EnumNotificationTypeDisplayMode mode,
                                      EnumNotificationStyle style, BaniraColorConfig theme, String phase, String type) {
        NarcissusNetworkSmokeNotifications.verifyNested(notification.component(), phase, type);
        int bg = data.bgColor().argb();
        int border = data.borderColor().argb();
        int text = data.component().color().argb();
        if (mode == EnumNotificationTypeDisplayMode.OVERLAY) {
            switch (style) {
                case SUCCESS: bg = theme.notificationSuccessBg(); border = theme.notificationSuccessBorder(); text = theme.notificationSuccessText(); break;
                case WARNING: bg = theme.notificationWarningBg(); border = theme.notificationWarningBorder(); text = theme.notificationWarningText(); break;
                case ERROR: bg = theme.notificationErrorBg(); border = theme.notificationErrorBorder(); text = theme.notificationErrorText(); break;
                default: bg = theme.notificationNormalBg(); border = theme.notificationNormalBorder(); text = theme.notificationNormalText(); break;
            }
        }
        require(notification.bgColor().argb() == bg && notification.borderColor().argb() == border
                && notification.component().color().argb() == text, "Factory theme mismatch " + type + " " + style + " " + mode);
    }

    static void withDisplay(TypeSettings settings, EnumNotificationTypeDisplayMode mode, Runnable check) {
        TypeSettings before = NotificationTypeSettingsStore.copyOf(settings);
        try {
            settings.displayMode(mode);
            check.run();
        } finally {
            // Mutate only the existing in-memory object; put/replaceAllAndSave would persist test preferences.
            settings.hidden(before.hidden()).durationMs(before.durationMs()).positionName(before.positionName())
                    .animationName(before.animationName()).displayMode(before.displayMode())
                    .displayModeCustomized(before.displayModeCustomized());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
