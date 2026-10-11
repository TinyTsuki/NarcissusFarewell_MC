package xin.vanilla.narcissus.service.cost;

import xin.vanilla.narcissus.api.cost.CostContext;
import xin.vanilla.narcissus.api.cost.CostPhase;
import xin.vanilla.narcissus.data.cost.*;
import xin.vanilla.narcissus.enums.EnumCostFailure;
import xin.vanilla.narcissus.internal.server.cost.CostEvaluation;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.LongSupplier;

public final class CostQuoteService {
    public interface QuoteAccess {
        boolean connected(UUID player);

        boolean permitted(UUID player, CostQuoteTarget target);

        /**
         * Null means the authoritative record or final destination is unknown.
         */
        CostEvaluation open(UUID player, CostQuoteRequest request);
    }

    private final Thread owner = Thread.currentThread();
    private final xin.vanilla.banira.api.cost.CostQuoteSessions sessions;
    private final LongSupplier clock, generation;
    private final QuoteAccess access;
    private final Function<CostContext, CostCalculation> calculate;
    private final CostPaymentService payment;
    private final CostPaymentService.PaymentAccess paymentAccess;

    public CostQuoteService(LongSupplier clock, LongSupplier generation, QuoteAccess access,
                            Function<CostContext, CostCalculation> calculate, CostPaymentService.PaymentAccess paymentAccess) {
        this.clock = Objects.requireNonNull(clock);
        this.generation = Objects.requireNonNull(generation);
        this.access = Objects.requireNonNull(access);
        this.calculate = Objects.requireNonNull(calculate);
        this.paymentAccess = Objects.requireNonNull(paymentAccess);
        this.payment = new CostPaymentService(calculate, paymentAccess);
        this.sessions = new xin.vanilla.banira.api.cost.CostQuoteSessions(clock, generation, access::connected);
    }

    public void openConnection(UUID player, UUID session) {
        sessions.open(player, session);
    }

    public void closeConnection(UUID player) {
        sessions.close(player);
    }

    public void clear() {
        sessions.clear();
    }

    public int connectionCount() {
        return sessions.connectionCount();
    }

    public CostQuote quote(UUID connectionPlayer, CostQuoteRequest request) {
        checkOwner();
        Objects.requireNonNull(request);
        xin.vanilla.banira.api.cost.CostFailure guard = sessions.accept(connectionPlayer, request.sessionId(),
                request.requestId(), request.generationId());
        if (guard.equals(xin.vanilla.banira.api.cost.CostFailure.RATE_LIMITED))
            return result(request, CostQuote.Status.RATE_LIMITED, EnumCostFailure.NONE);
        if (guard.isFailure())
            return result(request, guard.equals(xin.vanilla.banira.api.cost.CostFailure.CONFIGURATION_UNAVAILABLE)
                    ? CostQuote.Status.UNAVAILABLE : CostQuote.Status.REJECTED, NarcissusPaymentPolicy.failure(guard));
        CostQuoteTarget target = request.target();
        if ((target.kind() == CostQuoteTarget.Kind.HOME || target.kind() == CostQuoteTarget.Kind.HISTORY) && !target.owner().equals(connectionPlayer)
                || !access.permitted(connectionPlayer, target))
            return result(request, CostQuote.Status.REJECTED, EnumCostFailure.INVALID_OPERATION);
        if (target.kind() == CostQuoteTarget.Kind.UNKNOWN)
            return result(request, CostQuote.Status.UNKNOWN, EnumCostFailure.UNKNOWN_TARGET);
        try (CostEvaluation evaluation = access.open(connectionPlayer, request)) {
            if (evaluation == null) return result(request, CostQuote.Status.UNKNOWN, EnumCostFailure.UNKNOWN_TARGET);
            CostContext context = evaluation.context();
            if (context.phase() != CostPhase.PREVIEW || context.operationId() != request.requestId()
                    || context.generationId() != request.generationId() || context.teleportType() != target.teleportType()
                    || !connectionPlayer.equals(context.payer().uuid())) {
                return result(request, CostQuote.Status.REJECTED, EnumCostFailure.INVALID_OPERATION);
            }
            EnumCostFailure validity = paymentAccess.validate(context);
            if (validity != EnumCostFailure.NONE) return result(request, CostQuote.Status.UNAVAILABLE, validity);
            CostCalculation calculation = calculate.apply(context);
            CostPaymentPlan plan = payment.plan(calculation, context);
            if (request.generationId() != generation.getAsLong())
                return result(request, CostQuote.Status.UNAVAILABLE, EnumCostFailure.CONFIGURATION_UNAVAILABLE);
            return plan.isSuccess() ? CostQuote.ready(request, context.costType(), plan) : result(request, CostQuote.Status.UNAVAILABLE, plan.failure());
        } catch (SecurityException failure) {
            return result(request, CostQuote.Status.REJECTED, EnumCostFailure.INVALID_OPERATION);
        } catch (RuntimeException failure) {
            return result(request, CostQuote.Status.UNAVAILABLE, EnumCostFailure.FORMULA_FAILED);
        }
    }

    private static CostQuote result(CostQuoteRequest request, CostQuote.Status status, EnumCostFailure reason) {
        return CostQuote.unavailable(request, status, reason);
    }

    private void checkOwner() {
        if (Thread.currentThread() != owner)
            throw new IllegalStateException("Quote service requires server owner thread");
    }

}
