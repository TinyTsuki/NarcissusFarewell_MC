package xin.vanilla.narcissus.client.ui;

import xin.vanilla.narcissus.data.cost.CostQuote;
import xin.vanilla.narcissus.enums.EnumCostFailure;
import xin.vanilla.narcissus.enums.EnumCostType;

import java.util.ArrayList;
import java.util.Locale;
import java.util.function.Function;

@lombok.Value
@lombok.experimental.Accessors(fluent = true)
@lombok.AllArgsConstructor
public class WaypointCostPresentation {
    String amount;
    String note;

    public static WaypointCostPresentation from(CostQuote quote, Function<String, String> labels) {
        if (quote == null) return new WaypointCostPresentation(labels.apply("cost_unknown"), "");
        if (quote.status() != CostQuote.Status.READY) {
            String note = quote.failure() == EnumCostFailure.NONE ? ""
                    : labels.apply("enum_cost_failure_" + quote.failure().name().toLowerCase(Locale.ROOT));
            return new WaypointCostPresentation(labels.apply("enum_cost_quote_status_" + quote.status().name().toLowerCase(Locale.ROOT)), note);
        }
        lombok.var amounts = new ArrayList<String>();
        if (quote.costType() != EnumCostType.NONE && (quote.resourceAmount() > 0 || quote.cardAmount() == 0))
            amounts.add(quote.resourceAmount() + " " + labels.apply("enum_cost_type_" + quote.costType().name().toLowerCase(Locale.ROOT)));
        if (quote.cardAmount() > 0) amounts.add(quote.cardAmount() + " " + labels.apply("teleport_card"));
        lombok.var notes = new ArrayList<String>();
        if (quote.commandPending()) notes.add(labels.apply("cost_command_pending"));
        if (quote.bypassCooldown()) notes.add(labels.apply("cost_bypass_cooldown"));
        return new WaypointCostPresentation(amounts.isEmpty() ? labels.apply("cost_free") : String.join(" + ", amounts), String.join("\n", notes));
    }
}
