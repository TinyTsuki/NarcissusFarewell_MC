package xin.vanilla.narcissus.data.cost;

import lombok.Value;
import lombok.experimental.Accessors;
import xin.vanilla.narcissus.enums.EnumCostFailure;

import java.util.Objects;

@Value
@Accessors(fluent = true)
public class CostPaymentPlan {
    int originalAmount;
    int cardAmount;
    int resourceAmount;
    boolean bypassCooldown;
    boolean commandPending;
    boolean committed;
    EnumCostFailure failure;

    private CostPaymentPlan(int originalAmount, int cardAmount, int resourceAmount, boolean bypassCooldown,
                            boolean commandPending, boolean committed, EnumCostFailure failure) {
        this.originalAmount = originalAmount;
        this.cardAmount = cardAmount;
        this.resourceAmount = resourceAmount;
        this.bypassCooldown = bypassCooldown;
        this.commandPending = commandPending;
        this.committed = committed;
        this.failure = failure;
    }

    public static CostPaymentPlan ready(int amount, int cards, int resources, boolean bypass, boolean commandPending) {
        if (amount < 0 || cards < 0 || resources < 0) throw new IllegalArgumentException("Negative payment");
        return new CostPaymentPlan(amount, cards, resources, bypass && cards > 0, commandPending, false, EnumCostFailure.NONE);
    }

    public static CostPaymentPlan failed(EnumCostFailure failure) {
        if (Objects.requireNonNull(failure, "failure") == EnumCostFailure.NONE)
            throw new IllegalArgumentException("Failure required");
        return new CostPaymentPlan(0, 0, 0, false, false, false, failure);
    }

    public boolean isSuccess() {
        return failure == EnumCostFailure.NONE;
    }

    public boolean isCommitted() {
        return committed;
    }

    public CostPaymentPlan paid() {
        if (!isSuccess() || committed) throw new IllegalStateException("Payment is not pending");
        return new CostPaymentPlan(originalAmount, cardAmount, resourceAmount, bypassCooldown, false, true, failure);
    }
}
