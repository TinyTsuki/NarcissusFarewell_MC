package xin.vanilla.narcissus.service.cost;

import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.data.cost.CostCalculation;
import xin.vanilla.narcissus.enums.EnumCostFailure;
import xin.vanilla.narcissus.enums.EnumCostType;

import java.util.Objects;

public final class CostCalculator {
    public CostCalculation calculate(CostParameters parameters, double distance) {
        Objects.requireNonNull(parameters, "parameters");
        if (parameters.type() == EnumCostType.NONE) return CostCalculation.FREE;
        if (!Double.isFinite(distance) || distance < 0) return CostCalculation.failed(EnumCostFailure.INVALID_DISTANCE);
        return finish(parameters, parameters.fixedAmount() + distance * parameters.perBlockAmount());
    }

    public CostCalculation calculate(CostParameters parameters, CostContext context, CostFormula formula) {
        Objects.requireNonNull(parameters, "parameters");
        if (parameters.type() == EnumCostType.NONE) return CostCalculation.FREE;
        Objects.requireNonNull(context, "context");
        if (formula == null) return calculate(parameters, context.distance());
        try {
            return finish(parameters, formula.calculate(context));
        } catch (RuntimeException error) {
            return CostCalculation.failed(EnumCostFailure.FORMULA_FAILED);
        }
    }

    private CostCalculation finish(CostParameters parameters, double raw) {
        if (!Double.isFinite(raw) || raw < 0) return CostCalculation.failed(EnumCostFailure.INVALID_RESULT);
        double bounded = Math.max(parameters.minAmount(), raw);
        if (parameters.maxAmount() >= 0) bounded = Math.min(bounded, parameters.maxAmount());
        double rounded = Math.ceil(bounded);
        if (rounded > Integer.MAX_VALUE) return CostCalculation.failed(EnumCostFailure.AMOUNT_OVERFLOW);
        return CostCalculation.success(raw, (int) rounded);
    }
}
