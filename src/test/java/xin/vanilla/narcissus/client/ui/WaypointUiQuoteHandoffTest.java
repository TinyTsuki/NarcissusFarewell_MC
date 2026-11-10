package xin.vanilla.narcissus.client.ui;

import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.narcissus.data.cost.CostPaymentPlan;
import xin.vanilla.narcissus.data.cost.CostQuote;
import xin.vanilla.narcissus.data.cost.CostQuoteRequest;
import xin.vanilla.narcissus.data.cost.CostQuoteTarget;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.internal.client.ClientCostQuotes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.Assert.*;

public class WaypointUiQuoteHandoffTest extends HeadlessUiEnvironment {


    @Test
    public void confirmedAndPendingQuotesSurviveEveryPresentationDetachWithoutPayment() {
        ClientCostQuotes quotes = new ClientCostQuotes();
        quotes.capabilities(UUID.randomUUID(), 1);
        CostQuoteTarget target = CostQuoteTarget.home(UUID.randomUUID(), "minecraft:overworld", "home");
        CostQuoteRequest request = quotes.request(target, 0).get();
        CostQuote confirmed = CostQuote.ready(request, EnumCostType.NONE, CostPaymentPlan.ready(0, 0, 0, false, false));
        assertTrue(quotes.accept(confirmed, 1));
        WaypointUiParityTest.Port port = new WaypointUiParityTest.Port() {
            @Override
            public void close() {
                super.close();
                quotes.closeView();
            }
        };
        WaypointUiSession session = new WaypointUiSession(port, () -> true);
        session.bind(new WaypointUiParityTest.View());
        for (UiDetachReason reason : new UiDetachReason[]{UiDetachReason.RELOAD, UiDetachReason.RESIZE, UiDetachReason.SWITCH, UiDetachReason.FALLBACK}) {
            session.detach(reason);
            session.bind(new WaypointUiParityTest.View());
            assertSame(confirmed, quotes.display(target, 100).get());
        }
        CostQuoteRequest refresh = quotes.request(target, 600_000_000).get();
        session.detach(UiDetachReason.SWITCH);
        session.bind(new WaypointUiParityTest.View());
        assertSame(confirmed, quotes.display(target, 600_000_001).get());
        assertTrue(quotes.accept(CostQuote.ready(refresh, EnumCostType.NONE,
                CostPaymentPlan.ready(0, 0, 0, false, false)), 600_000_002));
        assertEquals(0, port.actions);
        session.close(UiCloseReason.DISCONNECT);
        assertFalse(quotes.display(target, 600_000_003).isPresent());
    }


}
