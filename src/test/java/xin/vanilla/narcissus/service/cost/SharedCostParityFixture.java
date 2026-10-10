package xin.vanilla.narcissus.service.cost;

import com.google.gson.JsonObject;
import xin.vanilla.narcissus.api.cost.CostPhase;
import xin.vanilla.narcissus.data.cost.CostOperation;
import xin.vanilla.narcissus.data.cost.CostPaymentPlan;
import xin.vanilla.narcissus.enums.*;

import java.util.UUID;

final class SharedCostParityFixture {
    static JsonObject captureScenario(String scenario) {
        CostPaymentServiceTest.Fixture f = new CostPaymentServiceTest.Fixture();
        f.payerId = new UUID(0, 1);
        f.movingId = new UUID(0, 2);
        f.cards = 10;
        if (scenario.startsWith("mode:")) f.mode = EnumCardType.valueOf(scenario.substring(5));
        else if (scenario.startsWith("type:")) f.type = EnumCostType.valueOf(scenario.substring(5));
        else if (!scenario.equals("repeat") && !scenario.equals("cancel") && !scenario.equals("generation")
                && !scenario.equals("payer") && !scenario.equals("moving-target") && !scenario.equals("fixed-target")
                && !scenario.equals("command-failure") && !scenario.equals("notification-failure")
                && !scenario.equals("lost-resource")) throw new IllegalArgumentException(scenario);
        int notifications[] = {0};
        f.afterPayment = () -> {
            notifications[0]++;
            if (scenario.equals("notification-failure")) throw new IllegalStateException("Notification failed");
        };
        if (scenario.equals("moving-target")) f.targetId = new UUID(0, 3);
        if (scenario.equals("command-failure")) {
            f.type = EnumCostType.COMMAND;
            f.payFailure = EnumCostFailure.COMMAND_FAILED;
        }
        CostPaymentPlan check = f.service.plan(
                xin.vanilla.narcissus.data.cost.CostCalculation.success(f.amount, f.amount), f.context(CostPhase.CHECK));
        CostPaymentPlan preview = f.preview();
        int beforeCards = f.cards, beforeResource = f.resource;
        CostOperation operation = f.operation();
        switch (scenario) {
            case "cancel": operation.cancel(); break;
            case "generation": f.generation++; break;
            case "payer": f.payerId = new UUID(0, 4); break;
            case "moving-target":
                f.destination = CostPaymentServiceTest.Fixture.position("minecraft:the_nether", 60);
                f.amount = 8;
                break;
            case "fixed-target": f.destination = CostPaymentServiceTest.Fixture.position("minecraft:overworld", 20); break;
            case "lost-resource": f.resource = 4; break;
            default: break;
        }
        CostPaymentPlan result = f.service.commit(operation, f.context(CostPhase.COMMIT));
        CostPaymentPlan repeated = f.service.commit(operation, f.context(CostPhase.COMMIT));
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("check", check.failure().name());
        snapshot.addProperty("preview", preview.failure().name());
        snapshot.addProperty("previewCommand", preview.commandPending());
        snapshot.addProperty("failure", result.failure().name());
        snapshot.addProperty("amount", result.originalAmount());
        snapshot.addProperty("cards", result.cardAmount());
        snapshot.addProperty("resource", result.resourceAmount());
        snapshot.addProperty("bypass", result.bypassCooldown());
        snapshot.addProperty("committed", result.isCommitted());
        snapshot.addProperty("state", operation.state().name());
        snapshot.addProperty("beforeCards", beforeCards);
        snapshot.addProperty("beforeResource", beforeResource);
        snapshot.addProperty("afterCards", f.cards);
        snapshot.addProperty("afterResource", f.resource);
        snapshot.addProperty("payments", f.payments);
        snapshot.addProperty("notifications", notifications[0]);
        snapshot.addProperty("repeat", repeated.failure().name());
        return snapshot;
    }
}
