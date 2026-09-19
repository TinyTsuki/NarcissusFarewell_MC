package xin.vanilla.narcissus.internal.dev;

import org.junit.Test;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.notification.NotificationBudget;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class NarcissusNetworkSmokeNotificationsTest {
    @Test
    public void sendsAllTenRoutesOnceOnlyAfterRemoteBaniraHandshake() {
        NarcissusNetworkSmokeNotifications fixture = new NarcissusNetworkSmokeNotifications("phase-one");
        List<String> sent = new ArrayList<>();
        assertFalse(fixture.sendWhenReady(false, (type, payload) -> sent.add(type)));
        assertTrue(sent.isEmpty());
        assertTrue(fixture.sendWhenReady(true, (type, payload) -> {
            assertEquals(NarcissusNetworkSmokeNotifications.token("phase-one", type), payload.text());
            sent.add(type);
        }));
        assertTrue(fixture.sendWhenReady(true, (type, payload) -> fail("Must not resend")));
        assertEquals(Arrays.asList(NarcissusNotificationTypes.ALL_TYPE_IDS), sent);
        assertEquals(10, new HashSet<>(sent).size());
    }

    @Test
    public void everyPhaseAndTypeHasADistinctToken() {
        Set<String> tokens = new HashSet<>();
        for (String phase : Arrays.asList("phase-one", "phase-two")) {
            for (String type : NarcissusNotificationTypes.ALL_TYPE_IDS) {
                assertTrue(tokens.add(NarcissusNetworkSmokeNotifications.token(phase, type)));
            }
        }
        assertEquals(20, tokens.size());
    }

    @Test
    public void defaultRootAndExplicitNestedEventsSurviveSerialization() {
        for (String type : NarcissusNotificationTypes.ALL_TYPE_IDS) {
            Component original = NarcissusNetworkSmokeNotifications.payload("phase-one", type);
            assertEquals(0xFFFFFFFF, original.color().argb());
            Component copy = NarcissusComponent.get().deserialize(original.toJson());
            NarcissusNetworkSmokeNotifications.verifyNested(copy, "phase-one", type);
            assertEquals(original.toJson(), copy.toJson());
        }
    }

    @Test
    public void acceptsTheRealWirePreparationLanguageOnEveryNestedNode() {
        String type = NarcissusNotificationTypes.INTERACTIVE_HELP;
        NotificationBudget.Payload wire = NotificationBudget.prepare(
                NarcissusNetworkSmokeNotifications.payload("phase-two", type), "zh_cn");
        Component received = NarcissusComponent.get().deserialize(JsonUtils.parseObject(wire.componentJson()));
        NarcissusNetworkSmokeNotifications.verifyNested(received, "phase-two", type);
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsNestedClickLoss() {
        Component payload = NarcissusNetworkSmokeNotifications.payload("phase-one", NarcissusNotificationTypes.WAYPOINT);
        payload.getChildren().get(0).getChildren().get(0).clickEvent(null);
        NarcissusNetworkSmokeNotifications.verifyNested(payload, "phase-one", NarcissusNotificationTypes.WAYPOINT);
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsNestedHoverLoss() {
        Component payload = NarcissusNetworkSmokeNotifications.payload("phase-one", NarcissusNotificationTypes.WAYPOINT);
        payload.getChildren().get(0).getChildren().get(0).hoverEvent(null);
        NarcissusNetworkSmokeNotifications.verifyNested(payload, "phase-one", NarcissusNotificationTypes.WAYPOINT);
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsExplicitNestedColorLoss() {
        Component payload = NarcissusNetworkSmokeNotifications.payload("phase-one", NarcissusNotificationTypes.WAYPOINT);
        payload.getChildren().get(0).getChildren().get(0).color(0xFFFFFFFF);
        NarcissusNetworkSmokeNotifications.verifyNested(payload, "phase-one", NarcissusNotificationTypes.WAYPOINT);
    }

    @Test
    public void approvedDefaultsIncludeHelpInFiveChatRoutes() {
        int chat = 0;
        for (String type : NarcissusNotificationTypes.ALL_TYPE_IDS) {
            assertEquals(NarcissusNetworkSmokeNotifications.expectedDisplay(type),
                    NarcissusNotificationTypes.defaultDisplay(type));
            if (NarcissusNetworkSmokeNotifications.expectedDisplay(type) == EnumNotificationTypeDisplayMode.VANILLA_CHAT) chat++;
        }
        assertEquals(5, chat);
        assertEquals(EnumNotificationTypeDisplayMode.VANILLA_CHAT,
                NarcissusNetworkSmokeNotifications.expectedDisplay(NarcissusNotificationTypes.INTERACTIVE_HELP));
    }

    @Test
    public void completionRequiresExactCurrentPhaseReceiptMarker() {
        String marker = NarcissusNetworkSmokeNotifications.verifiedMarker("phase-one");
        assertFalse(NarcissusNetworkSmokeNotifications.clientVerified("phase-one", Collections.singletonList(marker + "-extra")));
        assertFalse(NarcissusNetworkSmokeNotifications.clientVerified("phase-two", Collections.singletonList(marker)));
        assertTrue(NarcissusNetworkSmokeNotifications.clientVerified("phase-one", Collections.singletonList(marker)));
    }

    @Test(expected = IllegalStateException.class)
    public void clientFailureOverridesAnEarlierReceiptMarker() {
        NarcissusNetworkSmokeNotifications.clientVerified("phase-one", Arrays.asList(
                NarcissusNetworkSmokeNotifications.verifiedMarker("phase-one"), "FAIL client failure"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownPhaseBeforeSending() {
        new NarcissusNetworkSmokeNotifications("phase-three");
    }
}
