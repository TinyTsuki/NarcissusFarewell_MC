package xin.vanilla.narcissus.service.cost;

import com.google.gson.JsonObject;
import org.junit.Test;
import xin.vanilla.narcissus.data.cost.CostOperation;
import xin.vanilla.narcissus.enums.EnumCardType;
import xin.vanilla.narcissus.enums.EnumCostOperationState;
import xin.vanilla.narcissus.enums.EnumCostType;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class SharedCostMigrationContractTest {
    @Test
    public void operationHasOneSharedTerminalStateOwner() {
        boolean shared = false;
        for (Field field : CostOperation.class.getDeclaredFields()) {
            assertNotEquals("Duplicate payment state", EnumCostOperationState.class, field.getType());
            if (field.getType() == xin.vanilla.banira.api.cost.CostOperation.class) shared = true;
        }
        assertTrue("Shared operation not bound", shared);
    }

    @Test
    public void paymentServiceUsesTheSharedCommitEngine() {
        boolean shared = false;
        for (Field field : CostPaymentService.class.getDeclaredFields()) {
            if (field.getType() == xin.vanilla.banira.api.cost.CostPaymentService.class) shared = true;
        }
        assertTrue("Shared commit engine not bound", shared);
    }

    @Test
    public void captureOriginalPublicEntryPointResults() throws Exception {
        JsonObject snapshots = new JsonObject();
        for (EnumCardType mode : EnumCardType.values()) snapshots.add("mode:" + mode.name(), SharedCostParityFixture.captureScenario("mode:" + mode.name()));
        for (EnumCostType type : EnumCostType.values()) snapshots.add("type:" + type.name(), SharedCostParityFixture.captureScenario("type:" + type.name()));
        for (String name : new String[]{"repeat", "cancel", "generation", "payer", "moving-target", "fixed-target", "command-failure", "notification-failure", "lost-resource"})
            snapshots.add(name, SharedCostParityFixture.captureScenario(name));
        System.out.println("PARITY_SNAPSHOT=" + snapshots);
        try (java.io.InputStream input = getClass().getResourceAsStream("/xin/vanilla/narcissus/cost-parity-baseline.json")) {
            assertNotNull("Frozen original baseline missing", input);
            JsonObject expected = new com.google.gson.JsonParser().parse(new java.io.InputStreamReader(input,
                    java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(expected, snapshots);
        }
    }
}
