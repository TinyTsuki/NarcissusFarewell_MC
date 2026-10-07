package xin.vanilla.narcissus.config.migration;

import org.junit.Test;
import xin.vanilla.narcissus.enums.EnumCardType;

import java.util.*;

import static org.junit.Assert.*;

public class LegacyFlatCostMigrationTest {
    @Test
    public void flatCardsAndGeneralDistanceRetainConfiguredValuesAndUnrelatedKeys() {
        Map<String, Object> base = map("teleportCard", true, "teleportCardDaily", 9,
                "teleportCardType", "REFUND_COST", "removeOriginalTp", true);
        Map<String, Object> general = map("teleportCostDistanceLimit", 8123,
                "teleportCostDistanceAcrossDimension", 456, "teleportHomeLimit", 7);
        Map<String, Object> input = map("base", base, "general", general, "unrelated", Arrays.asList("keep", "values"));
        CostMigrationPlan plan = CostConfigMigration.plan(input);
        assertTrue(plan.migrationRequired());
        assertTrue(plan.configuration().cards().enabled());
        assertEquals(9, plan.configuration().cards().dailyGrant());
        assertEquals(EnumCardType.OFFSET_COST, plan.configuration().cards().mode());
        assertEquals(8123, plan.configuration().maxDistance());
        assertEquals(456, plan.configuration().crossDimensionDistance());
        assertEquals(Collections.singletonMap("removeOriginalTp", true), plan.configurationValues().get("base"));
        assertEquals(Collections.singletonMap("teleportHomeLimit", 7), plan.configurationValues().get("general"));
        assertEquals(input.get("unrelated"), plan.configurationValues().get("unrelated"));
        assertEquals(Boolean.TRUE, base.get("teleportCard"));
        assertEquals(8123, general.get("teleportCostDistanceLimit"));
        assertFalse(CostConfigMigration.plan(plan.configurationValues()).migrationRequired());
    }

    @Test
    public void falseCardFlagIsStillRecognizedAsLegacyConfiguration() {
        CostMigrationPlan plan = CostConfigMigration.plan(map("base", map("teleportCard", false)));
        assertTrue(plan.migrationRequired());
        assertFalse(plan.configuration().cards().enabled());
        assertEquals(EnumCardType.WAIVE_COST, plan.configuration().cards().mode());
    }

    @Test
    public void dailyAndModeWithoutFlagUseTheLegacyDisabledDefault() {
        CostMigrationPlan plan = CostConfigMigration.plan(map("base",
                map("teleportCardDaily", 3, "teleportCardType", "REFUND_COOLDOWN")));
        assertTrue(plan.migrationRequired());
        assertFalse(plan.configuration().cards().enabled());
        assertEquals(3, plan.configuration().cards().dailyGrant());
        assertEquals(EnumCardType.BYPASS_COOLDOWN, plan.configuration().cards().mode());
    }

    @Test
    public void flatDistanceAloneTriggersMigration() {
        CostMigrationPlan plan = CostConfigMigration.plan(map("general",
                map("teleportCostDistanceLimit", 0, "teleportCostDistanceAcrossDimension", 73)));
        assertTrue(plan.migrationRequired());
        assertEquals(0, plan.configuration().maxDistance());
        assertEquals(73, plan.configuration().crossDimensionDistance());
    }

    @Test
    public void equalMixedDistanceValuesAreAcceptedButDifferentOnesAreBlocked() {
        Map<String, Object> limits = map("teleportCostDistanceLimit", 8123);
        Map<String, Object> general = map("teleportCostDistanceLimit", 8123L,
                "teleportCostDistanceAcrossDimension", 456);
        Map<String, Object> input = map("base", map("teleportLimit", limits), "general", general);
        assertEquals(456, CostConfigMigration.plan(input).configuration().crossDimensionDistance());
        general.put("teleportCostDistanceLimit", 123);
        try {
            CostConfigMigration.plan(input);
            fail("conflicting old locations must not select an arbitrary value");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("teleportCostDistanceLimit"));
        }
        assertEquals(123, general.get("teleportCostDistanceLimit"));
    }

    @Test
    public void mixedCardSiblingsMergeWithoutDiscardingNestedUnknownValues() {
        Map<String, Object> card = map("teleportCard", true, "teleportCardDaily", 9,
                "teleportCardType", "REFUND_COST", "customValue", "preserve");
        Map<String, Object> base = map("teleportCard", card, "teleportCardDaily", 9L,
                "teleportCardType", "REFUND_COST");
        CostMigrationPlan plan = CostConfigMigration.plan(map("base", base));
        assertEquals(9, plan.configuration().cards().dailyGrant());
        assertEquals(map("teleportCard", map("customValue", "preserve")), plan.configurationValues().get("base"));
        assertFalse(CostConfigMigration.plan(plan.configurationValues()).migrationRequired());
        base.put("teleportCardDaily", 1);
        try {
            CostConfigMigration.plan(map("base", base));
            fail("conflicting card daily grant must be blocked");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("teleportCardDaily"));
        }
    }

    @Test
    public void invalidFlatFlagOrDistanceDoesNotGetConvertedToDefaults() {
        for (Object flag : Arrays.asList("false", 0, Arrays.asList(false))) {
            try {
                CostConfigMigration.plan(map("base", map("teleportCard", flag)));
                fail("invalid card flag was accepted");
            } catch (IllegalArgumentException expected) {
                assertTrue(expected.getMessage().contains("teleportCard"));
            }
        }
        try {
            CostConfigMigration.plan(map("general", map("teleportCostDistanceLimit", 1.5)));
            fail("fractional distance must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("teleportCostDistanceLimit"));
        }
    }

    @Test
    public void oldAndNewCostSettingsCannotBeSilentlyCombined() {
        try {
            CostConfigMigration.plan(map("base", map("teleportCard", true),
                    "cost", map("cards", map("enabled", false))));
            fail("old/new collision must be rejected");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("Conflicting"));
        }
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) result.put((String) pairs[index], pairs[index + 1]);
        return result;
    }
}
