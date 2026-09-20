package xin.vanilla.narcissus.config;

import org.junit.Test;
import xin.vanilla.narcissus.config.access.ClientConfigAccess;
import xin.vanilla.narcissus.config.access.CommonConfigAccess;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;
import xin.vanilla.narcissus.enums.EnumCostType;
import static org.junit.Assert.*;

public class ConfigViewBaselineTest {
    @Test
    public void nestedAliasesAndCostConversionKeepTheirMeaning() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        CommonConfig.RootView view = CommonConfigAccess.root(fixture.holder);
        fixture.values.put("command.tpAsk.commandTpAsk", "");
        assertEquals("tpa", view.command().commandTpAsk());
        assertEquals("tpa", view.command().tpAsk().commandTpAsk());
        fixture.values.put("command.tpAsk.commandTpAsk", "ask,one");
        assertEquals("ask,one", view.command().commandTpAsk());
        fixture.values.put("cost.tpAsk.costTpAskType", "invalid-cost");
        assertEquals(EnumCostType.NONE, view.cost().tpAsk().type());
        view.cost().tpAsk().rate(0.002D);
        assertEquals(0.002D, view.cost().tpAsk().rate(), 0.0D);
        fixture.values.put("base.other.tpSound", "");
        assertEquals("minecraft:entity.enderman.teleport", view.base().other().tpSound());
        view.base().safeTeleport().unsafeBlocks(Arrays.asList("tag, clazz -> tag != null", "minecraft:lava"));
        assertEquals(2, view.base().safeTeleport().unsafeBlocks().size());
        assertEquals(0, fixture.saves);
        view.save();
        assertEquals(1, fixture.saves);
    }

    @Test
    public void clientNullFallsBackAndWriteRequiresExplicitSave() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(ClientConfig.class);
        ClientConfig.RootView view = ClientConfigAccess.root(fixture.holder);
        fixture.values.put("client.syncHomeMapWaypoint", null);
        assertTrue(view.client().syncHomeMapWaypoint());
        view.client().syncHomeMapWaypoint(false);
        assertFalse(view.client().syncHomeMapWaypoint());
        assertEquals(0, fixture.saves);
        view.save();
        assertEquals(1, fixture.saves);
    }

    @Test
    public void commonPathsDefaultsAndReadsRemainEquivalent() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        Map<String, Object> baseline = new LinkedHashMap<>();
        baseline.put("schema", fixture.schema());
        baseline.put("unbound", ConfigBaselineFixture.readView(CommonConfigAccess.root(null), CommonConfig.RootView.class));
        baseline.put("defaults", ConfigBaselineFixture.readView(CommonConfigAccess.root(fixture.holder), CommonConfig.RootView.class));
        fixture.nonDefaultValues();
        baseline.put("changed", ConfigBaselineFixture.readView(CommonConfigAccess.root(fixture.holder), CommonConfig.RootView.class));
        ConfigBaselineFixture.assertSnapshot("common", baseline);
    }

    @Test
    public void clientPathsDefaultsAndReadsRemainEquivalent() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(ClientConfig.class);
        Map<String, Object> baseline = new LinkedHashMap<>();
        baseline.put("schema", fixture.schema());
        baseline.put("unbound", ConfigBaselineFixture.readView(ClientConfigAccess.root(null), ClientConfig.RootView.class));
        baseline.put("defaults", ConfigBaselineFixture.readView(ClientConfigAccess.root(fixture.holder), ClientConfig.RootView.class));
        fixture.nonDefaultValues();
        baseline.put("changed", ConfigBaselineFixture.readView(ClientConfigAccess.root(fixture.holder), ClientConfig.RootView.class));
        ConfigBaselineFixture.assertSnapshot("client", baseline);
    }
}
