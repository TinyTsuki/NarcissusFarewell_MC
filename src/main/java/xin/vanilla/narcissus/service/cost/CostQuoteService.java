package xin.vanilla.narcissus.service.cost;

import xin.vanilla.narcissus.api.cost.CostContext;
import xin.vanilla.narcissus.api.cost.CostPhase;
import xin.vanilla.narcissus.data.cost.*;
import xin.vanilla.narcissus.enums.EnumCostFailure;
import xin.vanilla.narcissus.internal.server.cost.CostEvaluation;

import java.util.HashMap;
import java.util.Map;
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
    private final Map<UUID, Connection> connections = new HashMap<>();
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
    }

    public void openConnection(UUID player, UUID session) {
        checkOwner();
        Objects.requireNonNull(session);
        Objects.requireNonNull(player);
        if (!access.connected(player)) throw new IllegalArgumentException("Quote connection is not live");
        Connection old = connections.get(player);
        if (old == null || !old.session.equals(session))
            connections.put(player, new Connection(session, clock.getAsLong()));
    }

    public void closeConnection(UUID player) {
        checkOwner();
        connections.remove(player);
    }

    public void clear() {
        checkOwner();
        connections.clear();
    }

    public int connectionCount() {
        checkOwner();
        return connections.size();
    }

    public CostQuote quote(UUID connectionPlayer, CostQuoteRequest request) {
        checkOwner();
        Objects.requireNonNull(request);
        Connection connection = connections.get(connectionPlayer);
        if (connection == null || !connection.session.equals(request.sessionId()))
            return result(request, CostQuote.Status.REJECTED, EnumCostFailure.ACTOR_UNAVAILABLE);
        if (!access.connected(connectionPlayer)) {
            connections.remove(connectionPlayer);
            return result(request, CostQuote.Status.REJECTED, EnumCostFailure.ACTOR_UNAVAILABLE);
        }
        if (!connection.consume(clock.getAsLong()))
            return result(request, CostQuote.Status.RATE_LIMITED, EnumCostFailure.NONE);
        if (request.requestId() <= connection.lastRequest)
            return result(request, CostQuote.Status.REJECTED, EnumCostFailure.INVALID_OPERATION);
        connection.lastRequest = request.requestId();
        if (request.generationId() != generation.getAsLong())
            return result(request, CostQuote.Status.UNAVAILABLE, EnumCostFailure.CONFIGURATION_UNAVAILABLE);
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

    private static final class Connection {
        final UUID session;
        long updated, lastRequest;
        double tokens = 8;

        Connection(UUID session, long now) {
            this.session = session;
            this.updated = now;
        }

        boolean consume(long now) {
            long delta = now - updated;
            if (delta > 0) {
                tokens = Math.min(8, tokens + delta / 250_000_000.0);
                updated = now;
            }
            if (tokens < 1) return false;
            tokens--;
            return true;
        }
    }
}
