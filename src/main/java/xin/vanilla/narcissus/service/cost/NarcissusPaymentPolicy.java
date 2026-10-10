package xin.vanilla.narcissus.service.cost;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import xin.vanilla.banira.api.cost.CostFailure;
import xin.vanilla.banira.api.cost.CostIdentity;
import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.data.cost.CostCalculation;
import xin.vanilla.narcissus.data.cost.CostPaymentPlan;
import xin.vanilla.narcissus.enums.*;
import java.util.Locale;

public final class NarcissusPaymentPolicy {
    private static final Gson GSON = new Gson();
    private NarcissusPaymentPolicy() { }
    public static CostIdentity identity(CostContext context) {
        JsonObject business = new JsonObject();
        business.addProperty("moving", context.player().uuid().toString());
        business.addProperty("type", context.teleportType().name());
        business.addProperty("request", context.request().map(CostRequestInfo::id).orElse(null));
        business.addProperty("target", context.targetPlayer().map(p -> p.uuid().toString()).orElse(null));
        if (!context.targetPlayer().isPresent()) business.add("destination", GSON.toJsonTree(context.destination().orElse(null)));
        JsonObject configuration = new JsonObject();
        configuration.add("parameters", GSON.toJsonTree(context.parameters()));
        configuration.add("cards", GSON.toJsonTree(context.cardSettings()));
        return new CostIdentity("narcissus:teleport", context.payer().uuid(), context.operationId(), context.generationId(),
                business.toString(), configuration.toString());
    }
    public static CostFailure sharedFailure(EnumCostFailure failure) {
        return failure == EnumCostFailure.NONE ? CostFailure.NONE : CostFailure.of("narcissus:" + failure.name().toLowerCase(Locale.ROOT));
    }
    public static EnumCostFailure failure(CostFailure failure) {
        if (!failure.isFailure()) return EnumCostFailure.NONE;
        try { return EnumCostFailure.valueOf(failure.code().substring(failure.code().indexOf(':') + 1).toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException invalid) { return EnumCostFailure.PAYMENT_FAILED; }
    }
    public static CostPaymentPlan plan(CostCalculation calculation, CostContext context) {
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
                    cards = 1;
                    break;
                case REQUIRE_MATCHING_WITH_COST:
                    if (balance < amount) return CostPaymentPlan.failed(EnumCostFailure.INSUFFICIENT_CARDS);
                    cards = amount;
                    break;
                case OFFSET_COST:
                case OFFSET_COST_AND_BYPASS_COOLDOWN:
                    cards = Math.min(balance, amount);
                    resources = amount - cards;
                    bypass = context.cardSettings().mode() == EnumCardType.OFFSET_COST_AND_BYPASS_COOLDOWN && cards > 0;
                    break;
                case WAIVE_COST:
                case WAIVE_COST_AND_BYPASS_COOLDOWN:
                    if (balance > 0) {
                        cards = 1;
                        resources = 0;
                    }
                    bypass = context.cardSettings().mode() == EnumCardType.WAIVE_COST_AND_BYPASS_COOLDOWN && cards > 0;
                    break;
                case BYPASS_COOLDOWN:
                    cards = balance > 0 ? 1 : 0;
                    bypass = cards > 0;
                    break;
                default:
                    throw new IllegalStateException("Unknown card mode");
            }
        }
        if (context.cooldownSeconds() > 0 && !bypass) return CostPaymentPlan.failed(EnumCostFailure.COOLDOWN);
        return CostPaymentPlan.ready(amount, cards, resources, bypass,
                context.costType() == EnumCostType.COMMAND && resources > 0);
    }
}
