package xin.vanilla.narcissus.data.cost;

import lombok.Value;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.enums.EnumCostFailure;
import xin.vanilla.narcissus.enums.EnumCostType;
import java.util.Locale;
import java.util.Objects;
import java.util.OptionalInt;

@Value
@Accessors(fluent = true)
public class CostQuote {
    CostQuoteRequest request;
    Status status;
    EnumCostType costType;
    int originalAmount, cardAmount, resourceAmount;
    boolean bypassCooldown, commandPending;
    EnumCostFailure failure;

    public enum Status implements IEnumDescribable {
        READY, UNKNOWN, UNAVAILABLE, REJECTED, RATE_LIMITED;
        @Override public Component enumDescription() {
            return NarcissusComponent.get().trans("word.narcissus_farewell.enum_cost_quote_status_" + name().toLowerCase(Locale.ROOT));
        }
    }
    public CostQuote(CostQuoteRequest request, Status status, EnumCostType type, int amount, int cards, int resource,
                     boolean bypass, boolean pending, EnumCostFailure failure) {
        this.request = Objects.requireNonNull(request, "request"); this.status = Objects.requireNonNull(status, "status");
        this.costType = Objects.requireNonNull(type, "type"); this.failure = Objects.requireNonNull(failure, "failure");
        if (amount < 0 || cards < 0 || resource < 0 || resource > amount || bypass && cards == 0
                || pending && (type != EnumCostType.COMMAND || resource == 0)
                || status == Status.READY && failure != EnumCostFailure.NONE
                || status != Status.READY && (amount != 0 || cards != 0 || resource != 0 || bypass || pending || type != EnumCostType.NONE)) {
            throw new IllegalArgumentException("Invalid quote result");
        }
        this.originalAmount = amount; this.cardAmount = cards; this.resourceAmount = resource;
        this.bypassCooldown = bypass; this.commandPending = pending;
    }
    public static CostQuote ready(CostQuoteRequest request, EnumCostType type, CostPaymentPlan plan) {
        if (!plan.isSuccess() || plan.isCommitted()) throw new IllegalArgumentException("Quote requires an unpaid plan");
        return new CostQuote(request, Status.READY, type, plan.originalAmount(), plan.cardAmount(), plan.resourceAmount(),
                plan.bypassCooldown(), plan.commandPending(), EnumCostFailure.NONE);
    }
    public static CostQuote unavailable(CostQuoteRequest request, Status status, EnumCostFailure failure) {
        if (status == Status.READY) throw new IllegalArgumentException("Unavailable quote cannot be ready");
        return new CostQuote(request, status, EnumCostType.NONE, 0, 0, 0, false, false, failure);
    }
    public OptionalInt amount() { return status == Status.READY ? OptionalInt.of(originalAmount) : OptionalInt.empty(); }
}
