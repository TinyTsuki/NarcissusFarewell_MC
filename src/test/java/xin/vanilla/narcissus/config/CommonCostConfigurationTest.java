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
}
