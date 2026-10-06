package xin.vanilla.narcissus.service.cost;

import org.junit.Test;
import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.data.cost.CostCalculation;
import xin.vanilla.narcissus.data.cost.CostOperation;
import xin.vanilla.narcissus.data.cost.CostPaymentPlan;
import xin.vanilla.narcissus.enums.*;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class CostPaymentServiceTest {
    @Test
    public void allSevenModesHaveExactDebitsAndCooldownMeaning() {
        int[][] expected = {{1, 5, 0}, {5, 5, 0}, {5, 0, 0}, {1, 0, 0},
                {1, 5, 1}, {5, 0, 1}, {1, 0, 1}};
        EnumCardType[] modes = {EnumCardType.REQUIRE_ONE_WITH_COST, EnumCardType.REQUIRE_MATCHING_WITH_COST, EnumCardType.OFFSET_COST,
                EnumCardType.WAIVE_COST, EnumCardType.BYPASS_COOLDOWN,
                EnumCardType.OFFSET_COST_AND_BYPASS_COOLDOWN, EnumCardType.WAIVE_COST_AND_BYPASS_COOLDOWN};
        for (int i = 0; i < modes.length; i++) {
            Fixture f = new Fixture();
            f.mode = modes[i];
            f.cards = 10;
            CostPaymentPlan plan = f.preview();
            assertTrue(modes[i].name(), plan.isSuccess());
            assertEquals(expected[i][0], plan.cardAmount());
            assertEquals(expected[i][1], plan.resourceAmount());
            assertEquals(expected[i][2] == 1, plan.bypassCooldown());
            assertEquals(10, f.cards);
            assertEquals(10, f.resource);
            assertEquals(0, f.payments);
            assertTrue(f.commit().isCommitted());
            assertEquals(10 - expected[i][0], f.cards);
            assertEquals(10 - expected[i][1], f.resource);
        }
    }

    @Test
    public void insufficientRequiredCardsNeverCreditAnyResource() {
        for (EnumCostType type : EnumCostType.values()) {
            if (type == EnumCostType.NONE) continue;
            for (EnumCardType mode : Arrays.asList(EnumCardType.REQUIRE_ONE_WITH_COST, EnumCardType.REQUIRE_MATCHING_WITH_COST)) {
                Fixture f = new Fixture();
                f.type = type;
                f.cards = 0;
                f.mode = mode;
                assertEquals(EnumCostFailure.INSUFFICIENT_CARDS, f.commit().failure());
                assertEquals(10, f.resource);
                assertEquals(0, f.cards);
                assertEquals(0, f.payments);
            }
        }
    }

    @Test
    public void partialOffsetCannotMakeAmountsNegative() {
        Fixture f = new Fixture();
        f.cards = 2;
        f.mode = EnumCardType.OFFSET_COST_AND_BYPASS_COOLDOWN;
        f.cooldown = 30;
        CostPaymentPlan result = f.commit();
        assertTrue(result.isCommitted());
        assertEquals(2, result.cardAmount());
        assertEquals(3, result.resourceAmount());
        assertEquals(7, f.resource);
        assertEquals(0, f.cards);
    }

    @Test
    public void cooldownBypassNeedsEnabledCardsAndActualConsumption() {
        for (boolean enabled : new boolean[]{false, true}) {
            for (int cards : new int[]{0, 2}) {
                Fixture f = new Fixture();
                f.enabled = enabled;
                f.cards = cards;
                f.mode = EnumCardType.OFFSET_COST_AND_BYPASS_COOLDOWN;
                f.cooldown = 30;
                assertEquals(enabled && cards > 0, f.preview().isSuccess());
                assertEquals(0, f.payments);
            }
        }
    }

    @Test
    public void noneAndZeroAreDifferentAndZeroCannotBypassWithQuantityOffset() {
        Fixture f = new Fixture();
        f.type = EnumCostType.NONE;
        f.cooldown = 0;
        assertTrue(f.commit().isCommitted());
        assertEquals(2, f.cards);
        assertEquals(10, f.resource);
        f = new Fixture();
        f.amount = 0;
        f.mode = EnumCardType.OFFSET_COST_AND_BYPASS_COOLDOWN;
        f.cooldown = 30;
        assertEquals(EnumCostFailure.COOLDOWN, f.commit().failure());
        assertEquals(2, f.cards);
        f = new Fixture();
        f.amount = 0;
        f.mode = EnumCardType.REQUIRE_ONE_WITH_COST;
        assertTrue(f.commit().isCommitted());
        assertEquals(1, f.cards);
    }

    @Test
    public void positiveCommandIsPendingUntilOneSuccessfulCommit() {
        Fixture f = new Fixture();
        f.type = EnumCostType.COMMAND;
        assertTrue(f.preview().commandPending());
        assertEquals(0, f.payments);
        CostOperation op = f.operation();
        CostPaymentPlan paid = f.service.commit(op, f.context(CostPhase.COMMIT));
        assertTrue(paid.isCommitted());
        assertFalse(paid.commandPending());
        assertEquals(EnumCostFailure.INVALID_OPERATION, f.service.commit(op, f.context(CostPhase.COMMIT)).failure());
        assertEquals(1, f.payments);
    }

    @Test
    public void failedOrUncertainCommandIsTerminalWithoutCardDebitOrRetry() {
        Fixture f = new Fixture();
        f.type = EnumCostType.COMMAND;
        f.payFailure = EnumCostFailure.COMMAND_FAILED;
        CostOperation op = f.operation();
        assertEquals(EnumCostFailure.COMMAND_FAILED, f.service.commit(op, f.context(CostPhase.COMMIT)).failure());
        assertEquals(2, f.cards);
        f.payFailure = EnumCostFailure.NONE;
        assertFalse(f.service.commit(op, f.context(CostPhase.COMMIT)).isSuccess());
        assertEquals(1, f.payments);
    }

    @Test
    public void commitRecalculatesResourcesAndRejectsLostCards() {
        Fixture f = new Fixture();
        assertTrue(f.preview().isSuccess());
        CostOperation op = f.operation();
        f.resource = 4;
        assertEquals(EnumCostFailure.INSUFFICIENT_RESOURCE, f.service.commit(op, f.context(CostPhase.COMMIT)).failure());
        assertEquals(2, f.cards);
        assertEquals(0, f.payments);
        f = new Fixture();
        op = f.operation();
        f.cards = 0;
        assertEquals(EnumCostFailure.INSUFFICIENT_CARDS, f.service.commit(op, f.context(CostPhase.COMMIT)).failure());
    }

    @Test
    public void commitUsesUpdatedDistanceButRejectsNewConfigurationOrPoint() {
        Fixture f = new Fixture();
        CostOperation op = f.operation();
        f.amount = 7;
        assertEquals(7, f.service.commit(op, f.context(CostPhase.COMMIT)).resourceAmount());
        f = new Fixture();
        op = f.operation();
        f.generation++;
        assertEquals(EnumCostFailure.CONFIGURATION_UNAVAILABLE, f.service.commit(op, f.context(CostPhase.COMMIT)).failure());
        f = new Fixture();
        op = f.operation();
        f.destination = Fixture.position("minecraft:overworld", 20);
        assertEquals(EnumCostFailure.INVALID_OPERATION, f.service.commit(op, f.context(CostPhase.COMMIT)).failure());
    }

    @Test
    public void playerDestinationCanMoveAcrossDimensionsAndPayerStaysBound() {
        Fixture f = new Fixture();
        f.targetId = UUID.randomUUID();
        CostOperation op = f.operation();
        f.destination = Fixture.position("minecraft:the_nether", 60);
        f.amount = 8;
        assertEquals(8, f.service.commit(op, f.context(CostPhase.COMMIT)).resourceAmount());
        f = new Fixture();
        op = f.operation();
        f.payerId = UUID.randomUUID();
        assertEquals(EnumCostFailure.INVALID_OPERATION, f.service.commit(op, f.context(CostPhase.COMMIT)).failure());
    }

    @Test
    public void cancelledExpiredDeletedDeadOrDisconnectedOperationsCannotPay() {
        Fixture f = new Fixture();
        CostOperation op = f.operation();
        op.cancel();
        assertFalse(f.service.commit(op, f.context(CostPhase.COMMIT)).isSuccess());
        assertEquals(0, f.payments);
        for (EnumCostFailure reason : Arrays.asList(EnumCostFailure.REQUEST_UNAVAILABLE, EnumCostFailure.ACTOR_UNAVAILABLE)) {
            f = new Fixture();
            op = f.operation();
            f.validity = reason;
            assertEquals(reason, f.service.commit(op, f.context(CostPhase.COMMIT)).failure());
            assertEquals(0, f.payments);
        }
    }

    @Test
    public void requestIdentityCannotBeSwappedBetweenPreviewAndPayment() {
        Fixture f = new Fixture();
        f.requestId = "first";
        CostOperation op = f.operation();
        f.requestId = "second";
        assertEquals(EnumCostFailure.INVALID_OPERATION, f.service.commit(op, f.context(CostPhase.COMMIT)).failure());
    }

    @Test
    public void crossThreadAndNonCommitPhaseCannotConsumeOperation() throws Exception {
        Fixture f = new Fixture();
        CostOperation op = f.operation();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try {
                f.service.commit(op, f.context(CostPhase.COMMIT));
            } catch (Throwable e) {
                error.set(e);
            }
        });
        thread.start();
        thread.join();
        assertTrue(error.get() instanceof IllegalStateException);
        assertFalse(f.service.commit(op, f.context(CostPhase.PREVIEW)).isSuccess());
        assertTrue(f.service.commit(op, f.context(CostPhase.COMMIT)).isCommitted());
    }

    @Test
    public void failedCalculationAndReentrantPaymentCannotExecuteTwice() {
        Fixture f = new Fixture();
        f.calcFailure = EnumCostFailure.INVALID_RESULT;
        assertEquals(EnumCostFailure.INVALID_RESULT, f.commit().failure());
        assertEquals(0, f.payments);
        f = new Fixture();
        CostOperation op = f.operation();
        Fixture current = f;
        f.onPay = () -> assertFalse(current.service.commit(op, current.context(CostPhase.COMMIT)).isSuccess());
        assertTrue(f.service.commit(op, f.context(CostPhase.COMMIT)).isCommitted());
        assertEquals(1, f.payments);
    }

    @Test
    public void unavailableActorsAreRejectedBeforeCallingCustomFormula() {
        Fixture f = new Fixture();
        f.validity = EnumCostFailure.ACTOR_UNAVAILABLE;
        f.service = new CostPaymentService(ctx -> {
            fail("Formula called for an unavailable actor");
            return CostCalculation.FREE;
        }, f);
        assertEquals(EnumCostFailure.ACTOR_UNAVAILABLE, f.commit().failure());
    }

    @Test
    public void saveOrSyncFailureCannotTurnPaidOperationIntoASecondDebit() {
        Fixture f = new Fixture();
        CostOperation op = f.operation();
        f.afterPayment = () -> {
            assertEquals(EnumCostOperationState.PAID, op.state());
            throw new IllegalStateException("Transport gone");
        };
        assertTrue(f.service.commit(op, f.context(CostPhase.COMMIT)).isCommitted());
        assertFalse(f.service.commit(op, f.context(CostPhase.COMMIT)).isSuccess());
        assertEquals(1, f.payments);
    }

    static final class Fixture implements CostPaymentService.PaymentAccess {
        UUID payerId = UUID.randomUUID(), movingId = UUID.randomUUID(), targetId;
        long generation = 1;
        String requestId;
        int cards = 2, resource = 10, amount = 5, cooldown, payments;
        boolean enabled = true;
        EnumCostType type = EnumCostType.EXP_POINT;
        EnumCardType mode = EnumCardType.REQUIRE_ONE_WITH_COST;
        EnumCostFailure validity = EnumCostFailure.NONE, payFailure = EnumCostFailure.NONE, calcFailure = EnumCostFailure.NONE;
        CostPosition destination = position("minecraft:overworld", 10);
        Runnable onPay, afterPayment;
        CostPaymentService service = new CostPaymentService(ctx -> calcFailure == EnumCostFailure.NONE
                ? CostCalculation.success(amount, amount) : CostCalculation.failed(calcFailure), this);

        CostOperation operation() {
            return CostOperation.create(context(CostPhase.CHECK));
        }

        CostPaymentPlan preview() {
            return service.plan(CostCalculation.success(amount, amount), context(CostPhase.PREVIEW));
        }

        CostPaymentPlan commit() {
            return service.commit(operation(), context(CostPhase.COMMIT));
        }

        public EnumCostFailure validate(CostContext ctx) {
            return validity;
        }

        public EnumCostFailure check(CostContext ctx, CostPaymentPlan plan) {
            return type == EnumCostType.COMMAND || resource >= plan.resourceAmount() ? EnumCostFailure.NONE : EnumCostFailure.INSUFFICIENT_RESOURCE;
        }

        public EnumCostFailure pay(CostContext ctx, CostPaymentPlan plan) {
            payments++;
            if (onPay != null) onPay.run();
            if (payFailure != EnumCostFailure.NONE) return payFailure;
            resource -= plan.resourceAmount();
            cards -= plan.cardAmount();
            return EnumCostFailure.NONE;
        }

        public void afterPayment(CostContext ctx, CostPaymentPlan plan) {
            if (afterPayment != null) afterPayment.run();
        }

        CostContext context(CostPhase phase) {
            return (CostContext) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{CostContext.class}, (p, m, a) -> {
                switch (m.getName()) {
                    case "operationId":
                        return 1L;
                    case "generationId":
                        return generation;
                    case "phase":
                        return phase;
                    case "teleportType":
                        return EnumTeleportType.TP_HOME;
                    case "costType":
                        return type;
                    case "parameters":
                        return new CostParameters(type, 5, 0, 0, -1, "minecraft:stone", "test {amount}", "");
                    case "cardSettings":
                        return new CostCardSettings(enabled, 0, mode);
                    case "cooldownSeconds":
                        return cooldown;
                    case "payer":
                        return player(payerId);
                    case "player":
                        return player(movingId);
                    case "targetPlayer":
                        return Optional.ofNullable(targetId).map(this::player);
                    case "requester":
                        return Optional.empty();
                    case "request":
                        return Optional.ofNullable(requestId).map(id -> new CostRequestInfo(id, 0, Long.MAX_VALUE, false, false));
                    case "destination":
                        return Optional.ofNullable(destination);
                    default:
                        throw new UnsupportedOperationException(m.getName());
                }
            });
        }

        CostPlayerView player(UUID id) {
            return (CostPlayerView) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{CostPlayerView.class}, (p, m, a) -> {
                switch (m.getName()) {
                    case "uuid":
                        return id;
                    case "teleportCards":
                        return cards;
                    case "alive":
                        return true;
                    case "removed":
                        return false;
                    default:
                        throw new UnsupportedOperationException(m.getName());
                }
            });
        }

        static CostPosition position(String dimension, double x) {
            return new CostPosition(dimension, x, 64, 0, 0, 0, false, EnumSafeMode.NONE);
        }
    }
}
