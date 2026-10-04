package xin.vanilla.narcissus.service.cost;

import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.data.cost.*;
import xin.vanilla.narcissus.enums.*;

import java.util.Objects;
import java.util.function.Function;

public final class CostPaymentService {
    private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger();
    public interface PaymentAccess {
        EnumCostFailure validate(CostContext context);
        EnumCostFailure check(CostContext context, CostPaymentPlan plan);
        /** Recheck and debit on the owner thread; a failed command must not consume cards. */
        EnumCostFailure pay(CostContext context, CostPaymentPlan plan);
        default void afterPayment(CostContext context, CostPaymentPlan plan) { }
    }

    private final Function<CostContext, CostCalculation> calculator;
    private final PaymentAccess access;

    public CostPaymentService(Function<CostContext, CostCalculation> calculator, PaymentAccess access) {
        this.calculator = Objects.requireNonNull(calculator, "calculator");
        this.access = Objects.requireNonNull(access, "access");
    }

    public CostPaymentPlan plan(CostCalculation calculation, CostContext context) {
        EnumCostFailure validity = access.validate(context);
        if (validity != EnumCostFailure.NONE) return CostPaymentPlan.failed(validity);
        if (!calculation.isSuccess()) return CostPaymentPlan.failed(calculation.failure());
        boolean free = context.costType() == EnumCostType.NONE;
        int amount = free ? 0 : calculation.amount(), cards = 0, resources = amount;
        boolean bypass = false;
        if (!free && context.cardSettings().enabled()) {
            int balance = context.payer().teleportCards();
            if (balance < 0) return CostPaymentPlan.failed(EnumCostFailure.INSUFFICIENT_CARDS);
            switch (context.cardSettings().mode()) {
                case REQUIRE_ONE_WITH_COST:
                    if (balance < 1) return CostPaymentPlan.failed(EnumCostFailure.INSUFFICIENT_CARDS);
                    cards = 1; break;
                case REQUIRE_MATCHING_WITH_COST:
                    if (balance < amount) return CostPaymentPlan.failed(EnumCostFailure.INSUFFICIENT_CARDS);
                    cards = amount; break;
                case OFFSET_COST:
                case OFFSET_COST_AND_BYPASS_COOLDOWN:
                    cards = Math.min(balance, amount); resources = amount - cards;
                    bypass = context.cardSettings().mode() == EnumCardType.OFFSET_COST_AND_BYPASS_COOLDOWN && cards > 0;
                    break;
                case WAIVE_COST:
                case WAIVE_COST_AND_BYPASS_COOLDOWN:
                    if (balance > 0) { cards = 1; resources = 0; }
                    bypass = context.cardSettings().mode() == EnumCardType.WAIVE_COST_AND_BYPASS_COOLDOWN && cards > 0;
                    break;
                case BYPASS_COOLDOWN:
                    cards = balance > 0 ? 1 : 0; bypass = cards > 0; break;
                default: throw new IllegalStateException("Unknown card mode");
            }
        }
        if (context.cooldownSeconds() > 0 && !bypass) return CostPaymentPlan.failed(EnumCostFailure.COOLDOWN);
        CostPaymentPlan result = CostPaymentPlan.ready(amount, cards, resources, bypass,
                context.costType() == EnumCostType.COMMAND && resources > 0);
        EnumCostFailure failure = access.check(context, result);
        return failure == EnumCostFailure.NONE ? result : CostPaymentPlan.failed(failure);
    }

    public CostPaymentPlan commit(CostOperation operation, CostContext context) {
        EnumCostFailure initial = operation.begin(context);
        if (initial != EnumCostFailure.NONE) return CostPaymentPlan.failed(initial);
        CostPaymentPlan result;
        try {
            EnumCostFailure eligible = access.validate(context);
            result = eligible == EnumCostFailure.NONE ? plan(calculator.apply(context), context) : CostPaymentPlan.failed(eligible);
            if (result.isSuccess()) {
                EnumCostFailure failure = access.pay(context, result);
                result = failure == EnumCostFailure.NONE ? result.paid() : CostPaymentPlan.failed(failure);
            }
        } catch (RuntimeException error) {
            LOGGER.error("Cost payment failed for operation " + operation.id(), error);
            result = CostPaymentPlan.failed(EnumCostFailure.PAYMENT_FAILED);
        }
        operation.finish(result);
        if (result.isCommitted()) {
            try { access.afterPayment(context, result); }
            catch (RuntimeException error) {
                // Payment is already terminal; a transport/storage failure must never authorize another debit.
                LOGGER.error("Cost payment follow-up failed for operation " + operation.id(), error);
            }
        }
        return result;
    }
}
