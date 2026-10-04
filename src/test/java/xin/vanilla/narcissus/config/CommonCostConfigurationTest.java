package xin.vanilla.narcissus.config;

import org.junit.*;
import xin.vanilla.narcissus.api.cost.CostParameters;
import xin.vanilla.narcissus.config.migration.CostConfigMigration;
import xin.vanilla.narcissus.data.cost.CostConfiguration;
import xin.vanilla.narcissus.enums.*;

import static org.junit.Assert.*;

public class CommonCostConfigurationTest {
    private Object previousPlatform;
    @Before public void rememberPlatform() throws Exception {
        java.lang.reflect.Field field = xin.vanilla.banira.platform.BaniraPlatforms.class.getDeclaredField("platform");
        field.setAccessible(true);
        previousPlatform = field.get(null);
    }
    @After public void restorePlatform() throws Exception {
        java.lang.reflect.Field field = xin.vanilla.banira.platform.BaniraPlatforms.class.getDeclaredField("platform");
        field.setAccessible(true);
        field.set(null, previousPlatform);
    }
    @Test public void allSixteenGroupsRemainIndependentInGeneratedViews() throws Exception {
        net.minecraft.util.registry.Bootstrap.bootStrap();
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.bind(CommonConfig.class);
        CommonCostConfiguration live = new CommonCostConfiguration(fixture.holder);
        int amount = 0;
        for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) {
            String prefix = "cost." + CostConfigMigration.groupName(type) + ".";
            fixture.holder.set(prefix + "type", EnumCostType.EXP_POINT);
            fixture.holder.set(prefix + "fixedAmount", (double) ++amount);
        }
        amount = 0;
        CostConfiguration snapshot = live.snapshot();
        for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) {
            CostParameters actual = live.selection(type).parameters(type);
            assertEquals(++amount, actual.fixedAmount(), 0);
            assertEquals(actual, snapshot.parameters(type));
        }
        assertEquals(0, fixture.saves);
    }

    @Test public void selectedGroupDoesNotReadAnInvalidUnrelatedGroup() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.bind(CommonConfig.class);
        fixture.holder.set("cost.home.fixedAmount", 3D);
        fixture.values.put("cost.stage.fixedAmount", Double.NaN);
        CommonCostConfiguration live = new CommonCostConfiguration(fixture.holder);
        assertEquals(3, live.selection(EnumTeleportType.TP_HOME).parameters(EnumTeleportType.TP_HOME).fixedAmount(), 0);
        try { live.snapshot(); fail("Invalid cold snapshot accepted"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void unchangedSelectionReusesItsImmutableSnapshot() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.bind(CommonConfig.class);
        fixture.holder.set("cost.home.type", EnumCostType.EXP_POINT);
        CommonCostConfiguration live = new CommonCostConfiguration(fixture.holder);
        CostConfiguration first = live.selection(EnumTeleportType.TP_HOME);
        assertSame(first, live.selection(EnumTeleportType.TP_HOME));
        assertEquals(0, fixture.saves);
    }

    @Test public void everyUnsavedDependencyInvalidatesTheCachedSelection() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.bind(CommonConfig.class);
        fixture.holder.set("cost.home.type", EnumCostType.EXP_POINT);
        CommonCostConfiguration live = new CommonCostConfiguration(fixture.holder);
        java.util.Map<String, Object> edits = new java.util.LinkedHashMap<>();
        edits.put("cost.home.type", EnumCostType.EXP_LEVEL);
        edits.put("cost.home.fixedAmount", 3D);
        edits.put("cost.home.perBlockAmount", .003D);
        edits.put("cost.home.minAmount", 1);
        edits.put("cost.home.maxAmount", 21);
        edits.put("cost.home.item", "minecraft:stone");
        edits.put("cost.home.command", "say {amount}");
        edits.put("cost.home.custom.file", "Fee.java");
        edits.put("cost.cards.enabled", true);
        edits.put("cost.cards.dailyGrant", 1);
        edits.put("cost.cards.mode", EnumCardType.OFFSET_COST);
        edits.put("cost.distance.maxDistance", 9000);
        edits.put("cost.distance.crossDimensionDistance", 8000);
        for (java.util.Map.Entry<String, Object> edit : edits.entrySet()) {
            CostConfiguration before = live.selection(EnumTeleportType.TP_HOME);
            fixture.holder.set(edit.getKey(), edit.getValue());
            CostConfiguration after = live.selection(EnumTeleportType.TP_HOME);
            assertNotSame(edit.getKey(), before, after);
            assertSame(edit.getKey(), after, live.selection(EnumTeleportType.TP_HOME));
            assertEquals(live.parameters(EnumTeleportType.TP_HOME), after.parameters(EnumTeleportType.TP_HOME));
            assertEquals(live.cards(), after.cards());
        }
        assertEquals(0, fixture.saves);
    }

    @Test public void freeSnapshotIgnoresBrokenUnrelatedCardsAndDistance() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.bind(CommonConfig.class);
        CommonCostConfiguration live = new CommonCostConfiguration(fixture.holder);
        CostConfiguration free = live.selection(EnumTeleportType.TP_HOME);
        fixture.values.put("cost.cards.dailyGrant", -1);
        fixture.values.put("cost.distance.maxDistance", -1);
        fixture.values.put("cost.stage.fixedAmount", Double.NaN);
        assertSame(free, live.selection(EnumTeleportType.TP_HOME));
        fixture.holder.set("cost.home.type", EnumCostType.EXP_POINT);
        try { live.selection(EnumTeleportType.TP_HOME); fail("Charged selection accepted invalid dependencies"); }
        catch (IllegalArgumentException expected) { }
    }

    @Test public void reboundHolderCannotUseAnOldCachedSnapshotOrLock() throws Exception {
        ConfigBaselineFixture first = new ConfigBaselineFixture(CommonConfig.class);
        first.bind(CommonConfig.class);
        CommonCostConfiguration live = new CommonCostConfiguration(first.holder);
        live.selection(EnumTeleportType.TP_HOME);
        ConfigBaselineFixture second = new ConfigBaselineFixture(CommonConfig.class);
        second.bind(CommonConfig.class);
        try { live.selection(EnumTeleportType.TP_HOME); fail("Rebound holder was accepted by old runtime"); }
        catch (IllegalStateException expected) { }
        try { live.snapshot(); fail("Rebound holder was accepted by old publication"); }
        catch (IllegalStateException expected) { }
        assertEquals(0, new CommonCostConfiguration(second.holder).selection(EnumTeleportType.TP_HOME)
                .parameters(EnumTeleportType.TP_HOME).fixedAmount(), 0);
    }

    @Test public void enumNamesInTheBackendMatchWithoutRebuildingAndBadNumbersFailClosed() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.bind(CommonConfig.class);
        fixture.holder.set("cost.home.type", EnumCostType.EXP_POINT);
        CommonCostConfiguration live = new CommonCostConfiguration(fixture.holder);
        CostConfiguration first = live.selection(EnumTeleportType.TP_HOME);
        fixture.values.put("cost.home.type", "EXP_POINT");
        fixture.values.put("cost.cards.mode", "WAIVE_COST");
        assertSame(first, live.selection(EnumTeleportType.TP_HOME));
        fixture.values.put("cost.home.fixedAmount", Double.NaN);
        try { live.selection(EnumTeleportType.TP_HOME); fail("Cached selection hid invalid live amount"); }
        catch (IllegalArgumentException expected) { }
        fixture.values.put("cost.home.fixedAmount", 0D);
        assertSame(first, live.selection(EnumTeleportType.TP_HOME));
    }
}
