package xin.vanilla.narcissus.client.ui;

import org.junit.Test;
import xin.vanilla.narcissus.data.cost.CostQuote;
import xin.vanilla.narcissus.data.cost.CostQuoteRequest;
import xin.vanilla.narcissus.data.cost.CostQuoteTarget;
import xin.vanilla.narcissus.enums.EnumCostFailure;
import xin.vanilla.narcissus.enums.EnumCostType;

import java.util.UUID;

import static org.junit.Assert.*;

public class WaypointCostPresentationTest {
    private final CostQuoteRequest request = new CostQuoteRequest(UUID.randomUUID(), 1, 1,
            CostQuoteTarget.home(UUID.randomUUID(), "minecraft:overworld", "name"));

    private WaypointCostPresentation show(CostQuote q) {
        return WaypointCostPresentation.from(q, k -> k);
    }

    @Test
    public void onlyExplicitReadyNoneCanDisplayFree() {
        assertEquals("cost_free", show(new CostQuote(request, CostQuote.Status.READY, EnumCostType.NONE, 0, 0, 0, false, false, EnumCostFailure.NONE)).amount());
        assertEquals("cost_unknown", show(null).amount());
        for (CostQuote.Status status : CostQuote.Status.values())
            if (status != CostQuote.Status.READY) {
                var result = show(CostQuote.unavailable(request, status, EnumCostFailure.REQUEST_UNAVAILABLE));
                assertNotEquals("cost_free", result.amount());
                assertFalse(result.amount().isEmpty());
                assertEquals("enum_cost_failure_request_unavailable", result.note());
            }
    }

    @Test
    public void allResourceTypesUseOnlyTheActualQuoteAmountsAndFlags() {
        for (EnumCostType type : EnumCostType.values())
            if (type != EnumCostType.NONE) {
                var result = show(new CostQuote(request, CostQuote.Status.READY, type, 9, 2, 7, true, type == EnumCostType.COMMAND, EnumCostFailure.NONE));
                assertTrue(result.amount().startsWith("7 enum_cost_type_"));
                assertTrue(result.amount().endsWith(" + 2 teleport_card"));
                assertTrue(result.note().contains("cost_bypass_cooldown"));
                assertEquals(type == EnumCostType.COMMAND, result.note().contains("cost_command_pending"));
            }
    }

    @Test
    public void cardOnlyPaymentDoesNotInventZeroResourceOrFree() {
        var result = show(new CostQuote(request, CostQuote.Status.READY, EnumCostType.EXP_POINT, 8, 2, 0, false, false, EnumCostFailure.NONE));
        assertEquals("2 teleport_card", result.amount());
        assertEquals("", result.note());
    }
}
