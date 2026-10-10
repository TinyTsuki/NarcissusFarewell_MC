package xin.vanilla.narcissus.service.cost;

import xin.vanilla.narcissus.api.cost.CostContext;
import xin.vanilla.narcissus.api.cost.CostPhase;
import xin.vanilla.narcissus.data.cost.CostCalculation;
import xin.vanilla.narcissus.data.cost.CostOperation;
import xin.vanilla.narcissus.data.cost.CostPaymentPlan;
import xin.vanilla.narcissus.enums.EnumCostFailure;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public final class CostPaymentService {
    public interface PreparedPayment {
        xin.vanilla.banira.api.cost.CostPaymentPlan plan();
        EnumCostFailure pay();
        default List<xin.vanilla.banira.api.cost.CostPaymentPlan.ResourceSnapshot> receipt() { return plan().snapshots(); }
        default void afterPayment() { }
    }
    public interface PaymentAccess {
        EnumCostFailure validate(CostContext context);
        EnumCostFailure check(CostContext context, CostPaymentPlan plan);
        EnumCostFailure pay(CostContext context, CostPaymentPlan plan);
        default void afterPayment(CostContext context, CostPaymentPlan plan) { }
        default PreparedPayment prepare(CostContext context, CostPaymentPlan plan) {
            EnumCostFailure failure = check(context, plan);
            xin.vanilla.banira.api.cost.CostPaymentPlan shared = failure == EnumCostFailure.NONE
                    ? xin.vanilla.banira.api.cost.CostPaymentPlan.ready(1, Collections.emptyList(), Collections.emptyList())
                    : xin.vanilla.banira.api.cost.CostPaymentPlan.failed(NarcissusPaymentPolicy.sharedFailure(failure));
            PaymentAccess access = this;
            return new PreparedPayment() {
                public xin.vanilla.banira.api.cost.CostPaymentPlan plan() { return shared; }
                public EnumCostFailure pay() { return access.pay(context, plan); }
                public void afterPayment() { access.afterPayment(context, plan.paid()); }
            };
        }
    }
    private final PaymentAccess access;
    private final xin.vanilla.banira.api.cost.CostPaymentService<Attempt> shared;
    public CostPaymentService(Function<CostContext, CostCalculation> calculator, PaymentAccess access) {
        Objects.requireNonNull(calculator, "calculator");
        this.access = Objects.requireNonNull(access, "access");
        shared = new xin.vanilla.banira.api.cost.CostPaymentService<>(attempt -> {
            attempt.selected = plan(calculator.apply(attempt.context), attempt.context);
            if (!attempt.selected.isSuccess()) return xin.vanilla.banira.api.cost.CostPaymentPlan.failed(
                    NarcissusPaymentPolicy.sharedFailure(attempt.selected.failure()));
            attempt.prepared = access.prepare(attempt.context, attempt.selected);
            return attempt.prepared.plan();
        }, attempt -> NarcissusPaymentPolicy.identity(attempt.context), new xin.vanilla.banira.api.cost.CostPaymentService.PaymentAccess<Attempt>() {
            public xin.vanilla.banira.api.cost.CostFailure validate(Attempt attempt) { return NarcissusPaymentPolicy.sharedFailure(access.validate(attempt.context)); }
            public xin.vanilla.banira.api.cost.CostFailure pay(Attempt attempt, xin.vanilla.banira.api.cost.CostPaymentPlan plan) {
                return NarcissusPaymentPolicy.sharedFailure(attempt.prepared.pay());
            }
            public List<xin.vanilla.banira.api.cost.CostPaymentPlan.ResourceSnapshot> receipt(Attempt attempt, xin.vanilla.banira.api.cost.CostPaymentPlan plan) {
                return attempt.prepared.receipt();
            }
            public void afterPayment(Attempt attempt, xin.vanilla.banira.api.cost.CostPaymentPlan plan) { attempt.prepared.afterPayment(); }
        });
    }
    public CostPaymentPlan plan(CostCalculation calculation, CostContext context) {
        EnumCostFailure validity = access.validate(context);
        if (validity != EnumCostFailure.NONE) return CostPaymentPlan.failed(validity);
        CostPaymentPlan selected = NarcissusPaymentPolicy.plan(calculation, context);
        if (!selected.isSuccess()) return selected;
        EnumCostFailure failure = access.check(context, selected);
        return failure == EnumCostFailure.NONE ? selected : CostPaymentPlan.failed(failure);
    }
    public CostPaymentPlan commit(CostOperation operation, CostContext context) {
        operation.state();
        if (context.phase() != CostPhase.COMMIT) return CostPaymentPlan.failed(EnumCostFailure.INVALID_OPERATION);
        Attempt attempt = new Attempt(context);
        xin.vanilla.banira.api.cost.CostPaymentPlan result = shared.commit(operation.sharedOperation(), attempt);
        return result.isCommitted() ? attempt.selected.paid() : CostPaymentPlan.failed(NarcissusPaymentPolicy.failure(result.failure()));
    }
    private static final class Attempt {
        final CostContext context;
        CostPaymentPlan selected;
        PreparedPayment prepared;
        Attempt(CostContext context) { this.context = context; }
    }
}
