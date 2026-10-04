package xin.vanilla.narcissus.data.cost;

import xin.vanilla.narcissus.enums.EnumCostFailure;

import java.util.Objects;

public final class CostCalculation {
    public static final CostCalculation FREE = new CostCalculation(0, 0, EnumCostFailure.NONE);
    private final double rawAmount;
    private final int amount;
    private final EnumCostFailure failure;

    private CostCalculation(double rawAmount, int amount, EnumCostFailure failure) {
        this.rawAmount = rawAmount; this.amount = amount; this.failure = failure;
    }

    public static CostCalculation success(double rawAmount, int amount) {
        if (!Double.isFinite(rawAmount) || rawAmount < 0 || amount < 0) throw new IllegalArgumentException("Invalid payable amount");
        return new CostCalculation(rawAmount, amount, EnumCostFailure.NONE);
    }

    public static CostCalculation failed(EnumCostFailure failure) {
        if (Objects.requireNonNull(failure, "failure") == EnumCostFailure.NONE) throw new IllegalArgumentException("Failure required");
        return new CostCalculation(0, 0, failure);
    }

    public boolean isSuccess() { return failure == EnumCostFailure.NONE; }
    public EnumCostFailure failure() { return failure; }
    public double rawAmount() { requireSuccess(); return rawAmount; }
    public int amount() { requireSuccess(); return amount; }
    private void requireSuccess() { if (!isSuccess()) throw new IllegalStateException("Cost calculation failed: " + failure); }
}
