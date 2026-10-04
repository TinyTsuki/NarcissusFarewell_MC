package xin.vanilla.narcissus.config.migration;

import org.junit.Test;
import xin.vanilla.banira.api.script.*;
import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class CostConfigMigrationTest {
    @Test public void standardAndConstantCostsDoNotAcquireAnExtraDistanceFee() {
        Map<String, Object> legacy = legacy("num * distance * rate");
        CostMigrationPlan plan = CostConfigMigration.plan(legacy);
        CostParameters home = plan.configuration().parameters(EnumTeleportType.TP_HOME);
        assertEquals(0, home.fixedAmount(), 0);
        assertEquals(.006, home.perBlockAmount(), 0);
        assertEquals(16, plan.configuration().groups().size());
        assertTrue(plan.sources().isEmpty());
        assertEquals("unchanged", plan.configurationValues().get("unrelated"));
        assertTrue(((Map<?, ?>) legacy.get("cost")).containsKey("tpHome"));
        CostParameters constant = CostConfigMigration.plan(legacy("5")).configuration().parameters(EnumTeleportType.TP_HOME);
        assertEquals(5, constant.fixedAmount(), 0);
        assertEquals(0, constant.perBlockAmount(), 0);
    }

    @Test public void migrationMovesCardsAndDistanceAndRemovesOnlyTheirLegacyKeys() {
        Map<String, Object> legacy = legacy("num");
        Map<String, Object> limits = new LinkedHashMap<>();
        limits.put("teleportHomeLimit", 8);
        limits.put("teleportCostDistanceLimit", 7000);
        limits.put("teleportCostDistanceAcrossDimension", 500);
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("teleportCard", true); card.put("teleportCardDaily", 3); card.put("teleportCardType", "REFUND_COST");
        Map<String, Object> base = new LinkedHashMap<>();
        base.put("teleportLimit", limits); base.put("teleportCard", card); legacy.put("base", base);
        CostMigrationPlan plan = CostConfigMigration.plan(legacy);
        assertEquals(7000, plan.configuration().maxDistance());
        assertEquals(500, plan.configuration().crossDimensionDistance());
        assertTrue(plan.configuration().cards().enabled());
        assertEquals(3, plan.configuration().cards().dailyGrant());
        Map<?, ?> migratedBase = (Map<?, ?>) plan.configurationValues().get("base");
        Map<?, ?> migratedLimits = (Map<?, ?>) migratedBase.get("teleportLimit");
        assertEquals(Collections.singletonMap("teleportHomeLimit", 8), migratedLimits);
        assertFalse(migratedBase.containsKey("teleportCard"));
        Map<?, ?> migratedCost = (Map<?, ?>) plan.configurationValues().get("cost");
        assertEquals("OFFSET_COST", ((Map<?, ?>) migratedCost.get("cards")).get("mode"));
    }

    @Test public void commandPlaceholderMigrationPreservesCommasAndOtherPayload() {
        Map<String, Object> legacy = legacy("num");
        Map<String, Object> home = home(legacy);
        home.put("costTpHomeType", "COMMAND");
        home.put("costTpHomeConf", "data merge entity @s {Tags:[\"a,b\",\"[num]\"]}");
        assertEquals("data merge entity @s {Tags:[\"a,b\",\"{amount}\"]}",
                CostConfigMigration.plan(legacy).configuration().parameters(EnumTeleportType.TP_HOME).command());
        home.put("costTpHomeType", "ITEM");
        home.put("costTpHomeConf", "minecraft:paper{display:{Name:'[num],a,b'}}");
        assertEquals("minecraft:paper{display:{Name:'[num],a,b'}}",
                CostConfigMigration.plan(legacy).configuration().parameters(EnumTeleportType.TP_HOME).item());
    }

    @Test public void disabledInvalidFormulaIsArchivedWithoutBlockingEnabledGroups() {
        Map<String, Object> legacy = legacy("unknown + '05'");
        home(legacy).put("costTpHomeType", "NONE");
        CostMigrationPlan plan = CostConfigMigration.plan(legacy);
        assertEquals("NONE", plan.configuration().parameters(EnumTeleportType.TP_HOME).type().name());
        assertEquals(1, plan.disabledExpressions().size());
        home(legacy).put("costTpHomeType", "EXP_POINT");
        try { CostConfigMigration.plan(legacy); fail("Unknown active variable"); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("tpHome")); }
    }

    @Test public void generatedMathAndBooleanExpressionsRunWithFrozenParameters() throws Exception {
        String integerProduct = "100000 * 100000 / 1000000000";
        assertEquals(10d, new xin.vanilla.banira.common.util.SafeExpressionEvaluator(integerProduct)
                .evaluateDouble(Collections.emptyMap()), 0);
        // Migration intentionally uses Java int arithmetic; decimal literals opt into double arithmetic.
        assertEquals(1, 100000 * 100000 / 1000000000);
        ExecutorService owner = Executors.newSingleThreadExecutor();
        ScriptSession<ScriptFactory<CostFormula>> session = BaniraScripts.openFactorySession(
                "cost-migration-test", CostFormula.class, "1", ScriptLimits.defaults(), owner);
        try {
            String[] expressions = {"num * distance * rate", "max(num, sqrt(distance) * rate)", "-2^2 + 10",
                    "distance > 100 && num == 2", "distance > 100 ? num : rate", "1e2 + .5", "5 / 2", "pow(2, 3^2)",
                    "true == distance > 100", "random(5, 5)", "Math.ceil(1.2)", integerProduct,
                    "100000.0 * 100000 / 1000000000"};
            double[] expected = {1.2, 2, 6, 1, 2, 100.5, 2, 512, 1, 5, 2, 1, 10};
            List<ScriptSource> sources = new ArrayList<>();
            for (int i = 0; i < expressions.length; i++) {
                String name = "Formula" + i;
                String body = LegacyCostExpression.toJava(expressions[i], 2, .003);
                String source = "package xin.vanilla.banira.generated.cost; public final class " + name
                        + " implements xin.vanilla.narcissus.api.cost.CostFormula { "
                        + "public double calculate(xin.vanilla.narcissus.api.cost.CostContext ctx) { return value(ctx.distance()); } "
                        + "public double value(double distance) { " + body + " } " + LegacyCostExpression.helpers() + " }";
                sources.add(new ScriptSource(name, "xin.vanilla.banira.generated.cost." + name, name + ".java", source));
            }
            PreparedScripts<ScriptFactory<CostFormula>> prepared = session.prepare(sources).get(15, TimeUnit.SECONDS);
            owner.submit(() -> {
                for (int i = 0; i < expected.length; i++) {
                    CostFormula formula = prepared.scripts().get("Formula" + i).create();
                    assertEquals(expected[i], (double) formula.getClass().getMethod("value", double.class).invoke(formula, 200d), 1e-9);
                }
                return null;
            }).get(15, TimeUnit.SECONDS);
        } finally { session.close(); owner.shutdownNow(); assertTrue(owner.awaitTermination(15, TimeUnit.SECONDS)); }
    }

    @Test public void rejectsUnknownInputsStringCoercionAndUnboundedSyntax() {
        for (String expression : Arrays.asList("'05' == 5", "health + 1", "sqrt()", "distance.class", "null", "1; System.exit(0)", "distance && true")) {
            try { LegacyCostExpression.toJava(expression, 2, .003); fail(expression); }
            catch (IllegalArgumentException expected) { }
        }
        char[] deep = new char[130]; Arrays.fill(deep, '(');
        try { LegacyCostExpression.toJava(new String(deep) + "num", 2, .003); fail("Unbounded nesting"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void secondPlanningRetainsMigratedSettingsAndNeedsNoNewMigration() {
        Map<String, Object> legacy = legacy("max(num, sqrt(distance) * rate)");
        CostMigrationPlan first = CostConfigMigration.plan(legacy);
        CostMigrationPlan second = CostConfigMigration.plan(first.configurationValues());
        assertFalse(second.migrationRequired());
        assertEquals(first.configuration().groups(), second.configuration().groups());
        assertTrue(second.sources().isEmpty());
    }

    @Test public void unrelatedCostTablesDoNotTriggerAnotherMigration() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("cost", Collections.singletonMap("tpsIntegration", Collections.singletonMap("enabled", true)));
        assertFalse(CostConfigMigration.plan(values).migrationRequired());
    }

    @Test public void allSixteenLegacyGroupsRetainTheirOwnParameters() {
        String[] names = {"Coordinate", "Structure", "Ask", "Here", "Random", "Spawn", "WorldSpawn", "Top",
                "Bottom", "Up", "Down", "View", "Home", "Stage", "Back", "Grave"};
        Map<String, Object> cost = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            Map<String, Object> group = new LinkedHashMap<>(); String prefix = "costTp" + names[i];
            group.put(prefix + "Type", "EXP_POINT"); group.put(prefix + "Num", i + 1); group.put(prefix + "Exp", "num");
            cost.put("tp" + names[i], group);
        }
        CostMigrationPlan plan = CostConfigMigration.plan(Collections.singletonMap("cost", cost));
        for (int i = 0; i < names.length; i++) {
            CostParameters group = plan.configuration().parameters(EnumTeleportType.countdownConfigurableTypes().get(i));
            assertEquals(i + 1, group.fixedAmount(), 0); assertEquals(0, group.perBlockAmount(), 0);
        }
    }

    @Test public void generatedFilesCompileAndAnUnconvertedDisabledFormulaCannotBeEnabled() throws Exception {
        Map<String, Object> values = legacy("max(num, sqrt(distance) * rate)");
        @SuppressWarnings("unchecked") Map<String, Object> cost = (Map<String, Object>) values.get("cost");
        Map<String, Object> disabled = new LinkedHashMap<>();
        disabled.put("costTpGraveType", "NONE"); disabled.put("costTpGraveExp", "unknown + '05'"); cost.put("tpGrave", disabled);
        CostMigrationPlan plan = CostConfigMigration.plan(values);
        ExecutorService owner = Executors.newSingleThreadExecutor();
        ScriptSession<ScriptFactory<CostFormula>> session = BaniraScripts.openFactorySession(
                "generated-cost-migration", CostFormula.class, "1", ScriptLimits.defaults(), owner);
        try {
            List<ScriptSource> sources = new ArrayList<>();
            plan.sources().forEach((file, source) -> sources.add(new ScriptSource(file.substring(0, file.length() - 5),
                    "xin.vanilla.banira.generated.cost." + file.substring(0, file.length() - 5), file, source)));
            PreparedScripts<ScriptFactory<CostFormula>> prepared = session.prepare(sources).get(15, TimeUnit.SECONDS);
            owner.submit(() -> {
                assertNotNull(prepared.scripts().get("LegacyHomeCost").create());
                try { prepared.scripts().get("LegacyGraveCost").create(); fail("Unconverted formula enabled"); }
                catch (RuntimeException expected) { }
                return null;
            }).get(15, TimeUnit.SECONDS);
        } finally { session.close(); owner.shutdownNow(); assertTrue(owner.awaitTermination(15, TimeUnit.SECONDS)); }
    }

    public static Map<String, Object> legacy(String expression) {
        Map<String, Object> group = new LinkedHashMap<>();
        group.put("costTpHomeType", "EXP_POINT"); group.put("costTpHomeNum", 2);
        group.put("costTpHomeRate", .003); group.put("costTpHomeNumUpper", 30);
        group.put("costTpHomeNumLower", 1); group.put("costTpHomeExp", expression); group.put("costTpHomeConf", "");
        Map<String, Object> cost = new LinkedHashMap<>(); cost.put("tpHome", group);
        Map<String, Object> root = new LinkedHashMap<>(); root.put("unrelated", "unchanged"); root.put("cost", cost);
        return root;
    }

    @SuppressWarnings("unchecked") private static Map<String, Object> home(Map<String, Object> legacy) {
        return (Map<String, Object>) ((Map<?, ?>) legacy.get("cost")).get("tpHome");
    }
}
