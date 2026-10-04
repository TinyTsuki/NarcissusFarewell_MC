package xin.vanilla.narcissus.service.cost;

import org.junit.Test;
import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.data.cost.*;
import xin.vanilla.narcissus.enums.*;
import xin.vanilla.narcissus.internal.server.cost.CostEvaluation;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.Assert.*;

public class CostQuoteServiceTest {
    private final UUID player = UUID.randomUUID();
    private final UUID session = UUID.randomUUID();
    private long now, generation = 1;
    private int opens, formulas, debits;
    private boolean connected = true, permitted = true, known = true;
    private CostContext escaped;
    private UUID contextPayer = player;
    private boolean changeGeneration, throwFormula;
    private final CostQuoteService service = new CostQuoteService(() -> now, () -> generation,
            new CostQuoteService.QuoteAccess() {
                public boolean connected(UUID id) { return connected && player.equals(id); }
                public boolean permitted(UUID id, CostQuoteTarget target) { return permitted; }
                public CostEvaluation open(UUID id, CostQuoteRequest request) {
                    opens++; if (!known) return null;
                    CostPlayerView view = proxy(CostPlayerView.class);
                    return CostEvaluation.open(CostContextInput.builder().operationId(request.requestId())
                            .generationId(request.generationId()).phase(CostPhase.PREVIEW)
                            .teleportType(request.target().teleportType()).parameters(CostParameters.free())
                            .cardSettings(new CostCardSettings(false, 0, EnumCardType.NONE))
                            .player(view).payer(view).source(new CostPosition("minecraft:overworld", 0, 0, 0, 0, 0, false, EnumSafeMode.NONE))
                            .sourceWorld(proxy(CostWorldView.class)).server(proxy(CostServerView.class)).build());
                }
            }, ctx -> {
                formulas++; escaped = ctx;
                if (changeGeneration) generation++;
                if (throwFormula) throw new IllegalStateException("Trusted script failure");
                return CostCalculation.success(5, 5);
            },
            new CostPaymentService.PaymentAccess() {
                public EnumCostFailure validate(CostContext ctx) { return EnumCostFailure.NONE; }
                public EnumCostFailure check(CostContext ctx, CostPaymentPlan plan) { return EnumCostFailure.NONE; }
                public EnumCostFailure pay(CostContext ctx, CostPaymentPlan plan) { debits++; return EnumCostFailure.NONE; }
            });
    private CostQuoteTarget target() { return CostQuoteTarget.home(player, "minecraft:overworld", "home"); }
    private CostQuoteRequest request(long id) { return new CostQuoteRequest(session, id, generation, target()); }
    private void enable() { service.openConnection(player, session); }

    @Test public void unregisteredWrongSessionDisconnectedAndStaleGenerationCannotInvokeFormula() {
        assertEquals(CostQuote.Status.REJECTED, service.quote(player, request(1)).status());
        enable(); assertEquals(CostQuote.Status.REJECTED, service.quote(player,
                new CostQuoteRequest(UUID.randomUUID(), 1, 1, target())).status());
        connected = false; assertEquals(CostQuote.Status.REJECTED, service.quote(player, request(2)).status());
        connected = true; enable(); generation = 2;
        assertEquals(CostQuote.Status.UNAVAILABLE, service.quote(player, new CostQuoteRequest(session, 3, 1, target())).status());
        assertEquals(0, opens); assertEquals(0, formulas);
    }

    @Test public void forgedHomeAndDeniedPermissionAreRejectedBeforeRecordResolution() {
        enable();
        CostQuoteTarget forged = CostQuoteTarget.home(UUID.randomUUID(), "minecraft:overworld", "home");
        assertEquals(CostQuote.Status.REJECTED, service.quote(player, new CostQuoteRequest(session, 1, 1, forged)).status());
        permitted = false; assertEquals(CostQuote.Status.REJECTED, service.quote(player, request(2)).status());
        assertEquals(0, opens); assertEquals(0, formulas);
    }

    @Test public void unknownRandomViewAndMissingTargetHaveNoFormulaOrFreeAmount() {
        enable();
        for (EnumTeleportType type : new EnumTeleportType[]{EnumTeleportType.TP_RANDOM, EnumTeleportType.TP_VIEW}) {
            CostQuote quote = service.quote(player, new CostQuoteRequest(session, type.ordinal() + 1, 1, CostQuoteTarget.unknown(type)));
            assertEquals(CostQuote.Status.UNKNOWN, quote.status()); assertFalse(quote.amount().isPresent());
        }
        known = false; assertEquals(CostQuote.Status.UNKNOWN, service.quote(player, request(30)).status());
        assertEquals(0, formulas); assertEquals(0, debits);
    }

    @Test public void eighthBurstPassesNinthRejectsAndMonotonicRefillIsFourPerSecond() {
        enable(); for (int i = 1; i <= 8; i++) assertEquals(CostQuote.Status.READY, service.quote(player, request(i)).status());
        assertEquals(CostQuote.Status.RATE_LIMITED, service.quote(player, request(9)).status());
        now = 249_999_999; assertEquals(CostQuote.Status.RATE_LIMITED, service.quote(player, request(10)).status());
        now = 250_000_000; assertEquals(CostQuote.Status.READY, service.quote(player, request(11)).status());
        now = 0; assertEquals(CostQuote.Status.RATE_LIMITED, service.quote(player, request(12)).status());
        now = 1_250_000_000; for (int i = 13; i <= 16; i++) assertEquals(CostQuote.Status.READY, service.quote(player, request(i)).status());
        assertEquals(CostQuote.Status.RATE_LIMITED, service.quote(player, request(17)).status());
        assertEquals(13, formulas);
    }

    @Test public void duplicatesContextExpiryAndConnectionClosureAreSafe() {
        enable(); CostQuote result = service.quote(player, request(1)); assertTrue(result.amount().isPresent());
        assertEquals(CostQuote.Status.REJECTED, service.quote(player, request(1)).status());
        try { escaped.teleportType(); fail("Borrowed context retained"); } catch (IllegalStateException expected) { }
        assertEquals(0, debits);
        service.closeConnection(player); assertEquals(0, service.connectionCount());
        assertEquals(CostQuote.Status.REJECTED, service.quote(player, request(2)).status());
    }

    @Test public void mismatchedContextPayerCannotBypassConnectionAuthority() {
        enable(); contextPayer = UUID.randomUUID();
        assertEquals(CostQuote.Status.REJECTED, service.quote(player, request(1)).status());
        assertEquals(0, formulas); assertEquals(0, debits);
    }

    @Test public void reentrantReloadAndThrowingFormulaReleaseContextWithoutPayment() {
        enable(); changeGeneration = true;
        assertEquals(CostQuote.Status.UNAVAILABLE, service.quote(player, request(1)).status());
        try { escaped.phase(); fail("Reload retained context"); } catch (IllegalStateException expected) { }
        changeGeneration = false; throwFormula = true;
        assertEquals(EnumCostFailure.FORMULA_FAILED, service.quote(player, request(2)).failure());
        try { escaped.phase(); fail("Exception retained context"); } catch (IllegalStateException expected) { }
        assertEquals(0, debits);
    }

    @Test public void repeatedCapabilitiesDoNotRefillBurstAndCloseClearsAllConnections() {
        enable(); for (int i = 1; i <= 8; i++) service.quote(player, request(i));
        enable(); assertEquals(CostQuote.Status.RATE_LIMITED, service.quote(player, request(9)).status());
        service.clear(); assertEquals(0, service.connectionCount());
    }

    private <T> T proxy(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, a) -> {
            if (m.getName().equals("uuid")) return contextPayer;
            if (m.getReturnType() == boolean.class) return true;
            if (m.getReturnType() == int.class) return 0;
            throw new UnsupportedOperationException(m.getName());
        }));
    }
}
