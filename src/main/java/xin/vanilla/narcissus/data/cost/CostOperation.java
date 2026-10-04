package xin.vanilla.narcissus.data.cost;

import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.enums.*;

import java.util.Objects;
import java.util.UUID;

/** One server-owned payment attempt. Contains identities and values, never borrowed/native objects. */
public final class CostOperation {
    private final Thread owner = Thread.currentThread();
    private final long id, generation;
    private final UUID payer, moving, target;
    private final EnumTeleportType type;
    private final String request;
    private final CostPosition destination;
    private final CostParameters parameters;
    private final CostCardSettings cards;
    private EnumCostOperationState state = EnumCostOperationState.PENDING;

    private CostOperation(CostContext context) {
        id = context.operationId(); generation = context.generationId();
        if (id <= 0 || generation < 0) throw new IllegalArgumentException("Invalid operation identity");
        payer = Objects.requireNonNull(context.payer().uuid(), "payer");
        moving = Objects.requireNonNull(context.player().uuid(), "moving");
        target = context.targetPlayer().map(CostPlayerView::uuid).orElse(null);
        type = Objects.requireNonNull(context.teleportType(), "type");
        request = context.request().map(CostRequestInfo::id).orElse(null);
        destination = context.destination().orElse(null);
        parameters = context.parameters(); cards = context.cardSettings();
    }

    public static CostOperation create(CostContext context) { return new CostOperation(Objects.requireNonNull(context, "context")); }
    public long id() { return id; }
    public EnumCostOperationState state() { requireOwner(); return state; }
    public boolean cancel() {
        requireOwner();
        if (state != EnumCostOperationState.PENDING) return false;
        state = EnumCostOperationState.CANCELLED; return true;
    }

    public EnumCostFailure begin(CostContext context) {
        requireOwner();
        if (state != EnumCostOperationState.PENDING || context.phase() != CostPhase.COMMIT) return EnumCostFailure.INVALID_OPERATION;
        EnumCostFailure failure = EnumCostFailure.NONE;
        if (generation != context.generationId() || !parameters.equals(context.parameters()) || !cards.equals(context.cardSettings())) {
            failure = EnumCostFailure.CONFIGURATION_UNAVAILABLE;
        } else if (id != context.operationId() || !payer.equals(context.payer().uuid())
                || !moving.equals(context.player().uuid()) || type != context.teleportType()
                || !Objects.equals(request, context.request().map(CostRequestInfo::id).orElse(null))
                || !Objects.equals(target, context.targetPlayer().map(CostPlayerView::uuid).orElse(null))
                || target == null && !Objects.equals(destination, context.destination().orElse(null))) {
            failure = EnumCostFailure.INVALID_OPERATION;
        }
        state = failure == EnumCostFailure.NONE ? EnumCostOperationState.COMMITTING : EnumCostOperationState.FAILED;
        return failure;
    }

    public void finish(CostPaymentPlan result) {
        requireOwner();
        if (state != EnumCostOperationState.COMMITTING) throw new IllegalStateException("Operation is not committing");
        state = result.isCommitted() ? EnumCostOperationState.PAID : EnumCostOperationState.FAILED;
    }

    private void requireOwner() {
        if (Thread.currentThread() != owner) throw new IllegalStateException("Payment requires its owning server thread");
    }
}
