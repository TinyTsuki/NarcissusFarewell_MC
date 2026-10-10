package xin.vanilla.narcissus.service.cost;

import com.google.gson.JsonObject;
import org.junit.Test;
import xin.vanilla.narcissus.enums.EnumCostType;

import static org.junit.Assert.*;

public class SharedCostParityTest {
    @Test
    public void sevenCardModesKeepExactDebitAndCooldownContracts() {
        String[] modes = {"REQUIRE_ONE_WITH_COST", "REQUIRE_MATCHING_WITH_COST", "OFFSET_COST", "WAIVE_COST",
                "BYPASS_COOLDOWN", "OFFSET_COST_AND_BYPASS_COOLDOWN", "WAIVE_COST_AND_BYPASS_COOLDOWN"};
        int[][] values = {{1, 5, 0}, {5, 5, 0}, {5, 0, 0}, {1, 0, 0}, {1, 5, 1}, {5, 0, 1}, {1, 0, 1}};
        for (int i = 0; i < modes.length; i++) {
            JsonObject actual = SharedCostParityFixture.captureScenario("mode:" + modes[i]);
            assertPaid(actual, 5, values[i][0], values[i][1]);
            assertEquals(values[i][2] == 1, actual.get("bypass").getAsBoolean());
        }
    }

    @Test
    public void everyResourceTypeKeepsReadOnlyCheckAndPreview() {
        for (EnumCostType type : EnumCostType.values()) {
            JsonObject actual = SharedCostParityFixture.captureScenario("type:" + type.name());
            boolean free = type == EnumCostType.NONE;
            assertPaid(actual, free ? 0 : 5, free ? 0 : 1, free ? 0 : 5);
            assertEquals(type == EnumCostType.COMMAND, actual.get("previewCommand").getAsBoolean());
        }
    }

    @Test
    public void unavailableIdentityAndResourceNeverDebit() {
        String[][] cases = {{"cancel", "INVALID_OPERATION", "CANCELLED"},
                {"generation", "CONFIGURATION_UNAVAILABLE", "FAILED"},
                {"payer", "INVALID_OPERATION", "FAILED"}, {"fixed-target", "INVALID_OPERATION", "FAILED"},
                {"lost-resource", "INSUFFICIENT_RESOURCE", "FAILED"}};
        for (String[] scenario : cases) {
            JsonObject actual = SharedCostParityFixture.captureScenario(scenario[0]);
            assertEquals(scenario[0], scenario[1], actual.get("failure").getAsString());
            assertEquals(scenario[0], scenario[2], actual.get("state").getAsString());
            assertEquals(0, actual.get("payments").getAsInt());
            assertEquals(0, actual.get("notifications").getAsInt());
            assertEquals(10, actual.get("afterCards").getAsInt());
            assertEquals(scenario[0].equals("lost-resource") ? 4 : 10, actual.get("afterResource").getAsInt());
        }
    }

    @Test
    public void movingPlayerTargetRepricesWithoutChangingPayer() {
        assertPaid(SharedCostParityFixture.captureScenario("moving-target"), 8, 1, 8);
    }

    @Test
    public void failedCommandKeepsCardsAndCannotRetry() {
        JsonObject actual = SharedCostParityFixture.captureScenario("command-failure");
        assertEquals("COMMAND_FAILED", actual.get("failure").getAsString());
        assertEquals("FAILED", actual.get("state").getAsString());
        assertEquals(10, actual.get("afterCards").getAsInt());
        assertEquals(10, actual.get("afterResource").getAsInt());
        assertEquals(1, actual.get("payments").getAsInt());
        assertEquals(0, actual.get("notifications").getAsInt());
        assertEquals("INVALID_OPERATION", actual.get("repeat").getAsString());
    }

    @Test
    public void notificationFailureCannotReopenPaidOperation() {
        assertPaid(SharedCostParityFixture.captureScenario("notification-failure"), 5, 1, 5);
        assertPaid(SharedCostParityFixture.captureScenario("repeat"), 5, 1, 5);
    }

    private static void assertPaid(JsonObject actual, int amount, int cards, int resource) {
        assertEquals("NONE", actual.get("check").getAsString());
        assertEquals("NONE", actual.get("preview").getAsString());
        assertEquals(10, actual.get("beforeCards").getAsInt());
        assertEquals(10, actual.get("beforeResource").getAsInt());
        assertEquals("NONE", actual.get("failure").getAsString());
        assertEquals(amount, actual.get("amount").getAsInt());
        assertEquals(cards, actual.get("cards").getAsInt());
        assertEquals(resource, actual.get("resource").getAsInt());
        assertTrue(actual.get("committed").getAsBoolean());
        assertEquals("PAID", actual.get("state").getAsString());
        assertEquals(10 - cards, actual.get("afterCards").getAsInt());
        assertEquals(10 - resource, actual.get("afterResource").getAsInt());
        assertEquals(1, actual.get("payments").getAsInt());
        assertEquals(1, actual.get("notifications").getAsInt());
        assertEquals("INVALID_OPERATION", actual.get("repeat").getAsString());
    }
}
