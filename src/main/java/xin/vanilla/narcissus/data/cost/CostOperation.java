package xin.vanilla.narcissus.data.cost;

import xin.vanilla.narcissus.api.cost.CostContext;
import xin.vanilla.narcissus.api.cost.CostPhase;
import xin.vanilla.narcissus.enums.EnumCostFailure;
import xin.vanilla.narcissus.enums.EnumCostOperationState;
import xin.vanilla.narcissus.service.cost.NarcissusPaymentPolicy;
import java.util.Collections;
import java.util.Objects;

public final class CostOperation {
    private final xin.vanilla.banira.api.cost.CostOperation shared;
    private CostOperation(CostContext context) {
        shared = xin.vanilla.banira.api.cost.CostOperation.create(NarcissusPaymentPolicy.identity(context));
    }
    public static CostOperation create(CostContext context) { return new CostOperation(Objects.requireNonNull(context, "context")); }
    public long id() { return shared.identity().operationId(); }
    public EnumCostOperationState state() { return EnumCostOperationState.valueOf(shared.state().name()); }
    public boolean cancel() { return shared.cancel(); }
    public xin.vanilla.banira.api.cost.CostOperation sharedOperation() { return shared; }
    public EnumCostFailure begin(CostContext context) {
        shared.state();
        if (context.phase() != CostPhase.COMMIT) return EnumCostFailure.INVALID_OPERATION;
        return NarcissusPaymentPolicy.failure(shared.begin(NarcissusPaymentPolicy.identity(context)));
    }
    public void finish(CostPaymentPlan result) {
        xin.vanilla.banira.api.cost.CostPaymentPlan converted = result.isSuccess()
                ? xin.vanilla.banira.api.cost.CostPaymentPlan.ready(1, Collections.emptyList(), Collections.emptyList())
                : xin.vanilla.banira.api.cost.CostPaymentPlan.failed(NarcissusPaymentPolicy.sharedFailure(result.failure()));
        shared.finish(result.isCommitted() ? converted.paid() : converted);
    }
}
