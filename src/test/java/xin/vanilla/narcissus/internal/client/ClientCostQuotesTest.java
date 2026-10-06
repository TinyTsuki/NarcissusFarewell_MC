package xin.vanilla.narcissus.internal.client;

import org.junit.Test;
import xin.vanilla.narcissus.data.cost.CostPaymentPlan;
import xin.vanilla.narcissus.data.cost.CostQuote;
import xin.vanilla.narcissus.data.cost.CostQuoteRequest;
import xin.vanilla.narcissus.data.cost.CostQuoteTarget;
import xin.vanilla.narcissus.enums.EnumCostType;

import java.util.UUID;

import static org.junit.Assert.*;

public class ClientCostQuotesTest {
    private final UUID session = UUID.randomUUID();
    private final UUID player = UUID.randomUUID();
    private final ClientCostQuotes cache = new ClientCostQuotes();

    private CostQuoteTarget target(String name) {
        return CostQuoteTarget.home(player, "minecraft:overworld", name);
    }

    private CostQuote answer(CostQuoteRequest r) {
        return CostQuote.ready(r, EnumCostType.EXP_POINT, CostPaymentPlan.ready(5, 1, 5, false, false));
    }

    private void enable() {
        cache.capabilities(session, 1);
    }

    @Test
    public void noCapabilityMeansNoRequestOrFreePlaceholder() {
        assertFalse(cache.request(target("a"), 0).isPresent());
        assertFalse(cache.cached(target("a"), 0).isPresent());
        assertFalse(cache.accept(answer(new CostQuoteRequest(session, 1, 1, target("a"))), 0));
    }

    @Test
    public void oneInflightAndFreshCacheCoalesceRepeatedHover() {
        enable();
        CostQuoteRequest first = cache.request(target("a"), 0).get();
        assertFalse(cache.request(target("a"), 499_000_000).isPresent());
        assertTrue(cache.accept(answer(first), 100_000_000));
        assertFalse(cache.request(target("a"), 599_000_000).isPresent());
        assertTrue(cache.cached(target("a"), 599_000_000).isPresent());
        assertTrue(cache.request(target("a"), 600_000_000).isPresent());
    }

    @Test
    public void selectionChangeRejectsOldResponseAndRespectsRefreshFloor() {
        enable();
        CostQuoteRequest a = cache.request(target("a"), 0).get();
        assertFalse(cache.request(target("b"), 1).isPresent());
        assertFalse(cache.accept(answer(a), 2));
        CostQuoteRequest b = cache.request(target("b"), 500_000_000).get();
        assertTrue(cache.accept(answer(b), 501_000_000));
        assertFalse(cache.accept(answer(b), 502_000_000));
        assertFalse(cache.cached(target("a"), 502_000_000).isPresent());
    }

    @Test
    public void timeoutRejectsLatePacketBeforeAnyRetry() {
        enable();
        CostQuoteRequest first = cache.request(target("a"), 0).get();
        assertFalse(cache.accept(answer(first), 3_000_000_000L));
        CostQuoteRequest retry = cache.request(target("a"), 3_000_000_001L).get();
        assertNotEquals(first.requestId(), retry.requestId());
        assertFalse(cache.accept(answer(first), 3_000_000_002L));
        assertTrue(cache.accept(answer(retry), 3_000_000_003L));
    }

    @Test
    public void closeDisconnectDeletionAndGenerationDiscardPendingAndValues() {
        enable();
        CostQuoteRequest r = cache.request(target("a"), 0).get();
        cache.invalidate(target("a"));
        assertFalse(cache.accept(answer(r), 1));
        r = cache.request(target("b"), 500_000_000).get();
        cache.capabilities(session, 2);
        assertFalse(cache.accept(answer(r), 501_000_000));
        r = cache.request(target("c"), 1_000_000_000).get();
        cache.closeView();
        assertFalse(cache.accept(answer(r), 1_000_000_001));
        assertTrue(cache.request(target("c"), 1_500_000_000).isPresent());
        cache.clear();
        assertFalse(cache.request(target("c"), 2_000_000_000).isPresent());
        cache.capabilities(UUID.randomUUID(), 1);
        assertFalse(cache.accept(answer(r), 2_000_000_001));
    }

    @Test
    public void cacheIsAccessOrderedAndBoundedToThirtyTwoTargets() {
        enable();
        for (int i = 0; i < 33; i++) {
            long now = i * 500_000_000L;
            CostQuoteRequest r = cache.request(target("home" + i), now).get();
            assertTrue(cache.accept(answer(r), now));
        }
        assertEquals(32, cache.size());
        assertFalse(cache.cached(target("home0"), 16_000_000_000L).isPresent());
        assertTrue(cache.cached(target("home32"), 16_000_000_000L).isPresent());
    }

    @Test
    public void capabilityRefreshDoesNotResetRateAndStaleGenerationDoesNotRollback() {
        enable();
        CostQuoteRequest r = cache.request(target("a"), 0).get();
        cache.capabilities(session, 1);
        assertTrue(cache.accept(answer(r), 1));
        cache.capabilities(session, 2);
        cache.capabilities(session, 1);
        assertEquals(2, cache.request(target("b"), 500_000_000).get().generationId());
    }

    @Test
    public void wrongConnectionTargetAndRequestCannotPoisonDisplayCache() {
        enable();
        CostQuoteRequest pending = cache.request(target("a"), 0).get();
        for (CostQuoteRequest wrong : new CostQuoteRequest[]{
                new CostQuoteRequest(UUID.randomUUID(), pending.requestId(), 1, target("a")),
                new CostQuoteRequest(session, pending.requestId(), 2, target("a")),
                new CostQuoteRequest(session, pending.requestId(), 1, target("b")),
                new CostQuoteRequest(session, pending.requestId() + 1, 1, target("a"))}) {
            assertFalse(cache.accept(answer(wrong), 1));
        }
        assertTrue(cache.accept(answer(pending), 2));
    }

    @Test
    public void closingAndReopeningSameViewCannotReuseOldRequestIdentity() {
        enable();
        CostQuoteRequest old = cache.request(target("a"), 0).get();
        cache.closeView();
        CostQuoteRequest fresh = cache.request(target("a"), 500_000_000).get();
        assertNotEquals(old.requestId(), fresh.requestId());
        assertFalse(cache.accept(answer(old), 500_000_001));
        assertTrue(cache.accept(answer(fresh), 500_000_002));
    }
}
