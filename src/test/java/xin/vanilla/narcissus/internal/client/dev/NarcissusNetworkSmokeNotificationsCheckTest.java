package xin.vanilla.narcissus.internal.client.dev;

import net.minecraft.util.text.ITextComponent;
import org.junit.Test;
import xin.vanilla.banira.client.data.NotificationLogEntry;
import xin.vanilla.banira.client.notification.NotificationTypeSettingsStore.TypeSettings;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeNotifications;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.NarcissusComponent;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class NarcissusNetworkSmokeNotificationsCheckTest {
    @Test
    public void actualCallerChecksRequireFreshNonFixtureNetworkEntries() {
        List<NotificationLogEntry> entries = entries("phase-one");
        assertNull(NarcissusNetworkSmokeNotificationsCheck.actualEntry(entries, NarcissusNotificationTypes.INTERACTIVE_HELP, 100));
        NotificationLogEntry help = new NotificationLogEntry().timestamp(99).source("network")
                .notificationType(NarcissusNotificationTypes.INTERACTIVE_HELP)
                .componentJson(NarcissusComponent.get().literal("Narcissus Farewell help").toJson().toString());
        entries.add(help);
        assertNull(NarcissusNetworkSmokeNotificationsCheck.actualEntry(entries, NarcissusNotificationTypes.INTERACTIVE_HELP, 100));
        help.timestamp(100).source("local");
        assertNull(NarcissusNetworkSmokeNotificationsCheck.actualEntry(entries, NarcissusNotificationTypes.INTERACTIVE_HELP, 100));
        help.source("network");
        assertSame(help, NarcissusNetworkSmokeNotificationsCheck.actualEntry(entries, NarcissusNotificationTypes.INTERACTIVE_HELP, 100));
    }

    @Test
    public void actualMissingRequestFeedbackCannotBeAnUnrelatedTeleportMessage() {
        List<NotificationLogEntry> entries = entries("phase-one");
        NotificationLogEntry feedback = new NotificationLogEntry().timestamp(100).source("network")
                .notificationType(NarcissusNotificationTypes.TELEPORT_REQUEST)
                .componentJson(NarcissusComponent.get().transAuto("tp_ask_invalid").toJson().toString());
        entries.add(feedback);
        assertNull(NarcissusNetworkSmokeNotificationsCheck.actualEntry(entries, NarcissusNotificationTypes.TELEPORT_REQUEST, 100));
        feedback.componentJson(NarcissusComponent.get().transAuto("tp_ask_not_found").toJson().toString());
        assertSame(feedback, NarcissusNetworkSmokeNotificationsCheck.actualEntry(entries, NarcissusNotificationTypes.TELEPORT_REQUEST, 100));
    }

    @Test
    public void waitsForAllTenCurrentPhaseNetworkEntries() {
        List<NotificationLogEntry> entries = entries("phase-one");
        entries.remove(9);
        assertEquals(9, NarcissusNetworkSmokeNotificationsCheck.received(entries, "phase-one").size());
        entries.addAll(entries("phase-two"));
        assertEquals(9, NarcissusNetworkSmokeNotificationsCheck.received(entries, "phase-one").size());
        entries.add(entries("phase-one").get(9));
        assertEquals(10, NarcissusNetworkSmokeNotificationsCheck.received(entries, "phase-one").size());
    }

    @Test(expected = IllegalStateException.class)
    public void localLogEntryCannotStandInForNetworkDelivery() {
        List<NotificationLogEntry> entries = entries("phase-one");
        entries.get(0).source("local");
        NarcissusNetworkSmokeNotificationsCheck.received(entries, "phase-one");
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsDuplicateNetworkDelivery() {
        List<NotificationLogEntry> entries = entries("phase-one");
        entries.add(entries.get(0));
        NarcissusNetworkSmokeNotificationsCheck.received(entries, "phase-one");
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsTokenDeliveredUnderWrongType() {
        List<NotificationLogEntry> entries = entries("phase-one");
        entries.get(0).notificationType(NarcissusNotificationTypes.WAYPOINT);
        NarcissusNetworkSmokeNotificationsCheck.received(entries, "phase-one");
    }

    @Test
    public void acceptsExactlyFiveActualChatPayloadsIncludingHelp() {
        NarcissusNetworkSmokeNotificationsCheck.verifyChat(chat(), "phase-one");
    }

    @Test(expected = IllegalStateException.class)
    public void missingHelpChatCannotPass() {
        List<ITextComponent> chat = chat();
        String token = NarcissusNetworkSmokeNotifications.token("phase-one", NarcissusNotificationTypes.INTERACTIVE_HELP);
        chat.removeIf(message -> message.getString().contains(token));
        NarcissusNetworkSmokeNotificationsCheck.verifyChat(chat, "phase-one");
    }

    @Test(expected = IllegalStateException.class)
    public void overlayTokenInChatCannotPass() {
        List<ITextComponent> chat = chat();
        chat.add(NarcissusNetworkSmokeNotifications.payload("phase-one", NarcissusNotificationTypes.WAYPOINT).toChat("en_us"));
        NarcissusNetworkSmokeNotificationsCheck.verifyChat(chat, "phase-one");
    }

    @Test(expected = IllegalStateException.class)
    public void chatCannotAcquireThemeRootTextColor() {
        List<ITextComponent> chat = chat();
        chat.set(0, NarcissusNetworkSmokeNotifications.payload("phase-one", NarcissusNotificationTypes.INTERACTIVE_TP_FLOW)
                .color(0xFFFF5555).toChat("en_us"));
        NarcissusNetworkSmokeNotificationsCheck.verifyChat(chat, "phase-one");
    }

    @Test
    public void restoresAllSettingsAfterSuccessfulAndFailedFactoryChecks() {
        TypeSettings settings = new TypeSettings().hidden(true).durationMs(1234).positionName("BOTTOM_LEFT")
                .animationName("AUTO").displayMode(EnumNotificationTypeDisplayMode.VANILLA_CHAT).displayModeCustomized(true);
        TypeSettings before = xin.vanilla.banira.client.notification.NotificationTypeSettingsStore.copyOf(settings);
        NarcissusNetworkSmokeNotificationsCheck.withDisplay(settings, EnumNotificationTypeDisplayMode.OVERLAY, () ->
                assertEquals(EnumNotificationTypeDisplayMode.OVERLAY, settings.displayMode()));
        assertEquals(before, settings);
        try {
            NarcissusNetworkSmokeNotificationsCheck.withDisplay(settings, EnumNotificationTypeDisplayMode.ACTION_BAR, () -> {
                settings.hidden(false).durationMs(1).positionName("").animationName("").displayModeCustomized(false);
                throw new IllegalStateException("factory failed");
            });
            fail("Expected failure");
        } catch (IllegalStateException expected) {
            assertEquals("factory failed", expected.getMessage());
        }
        assertEquals(before, settings);
    }

    private static List<NotificationLogEntry> entries(String phase) {
        List<NotificationLogEntry> result = new ArrayList<>();
        for (String type : NarcissusNotificationTypes.ALL_TYPE_IDS) {
            result.add(new NotificationLogEntry().source("network").notificationType(type)
                    .componentJson(NarcissusNetworkSmokeNotifications.payload(phase, type).toJson().toString()));
        }
        return result;
    }

    private static List<ITextComponent> chat() {
        List<ITextComponent> result = new ArrayList<>();
        for (String type : NarcissusNotificationTypes.ALL_TYPE_IDS) {
            if (NarcissusNetworkSmokeNotifications.expectedDisplay(type) == EnumNotificationTypeDisplayMode.VANILLA_CHAT) {
                result.add(NarcissusNetworkSmokeNotifications.payload("phase-one", type).toChat("en_us"));
            }
        }
        return result;
    }
}
