package xin.vanilla.narcissus.config;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class GeneratedConfigSchemaTest {
    @BeforeClass
    public static void bootstrap() {
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    public void commonSchemaMatchesRegistration() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.bind(CommonConfig.class);
        assertEquals(fixture.defaults, ConfigBaselineFixture.readView(CommonConfigView.get(), CommonConfigView.class));
    }

    @Test
    public void clientSchemaMatchesRegistration() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(ClientConfig.class);
        fixture.bind(ClientConfig.class);
        assertEquals(fixture.defaults, ConfigBaselineFixture.readView(ClientConfigView.get(), ClientConfigView.class));
    }

    @Test
    public void generatedSearchSettingsEnforceTheirRegisteredBounds() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.bind(CommonConfig.class);
        String prefix = "base.safeTeleport.search.";
        for (String key : Arrays.asList("timeBudgetMs", "maxConcurrentSearches", "timeoutSeconds")) {
            assertTrue("Missing search setting: " + key, fixture.defaults.containsKey(prefix + key));
        }
        assertEquals(Double.valueOf(2), fixture.holder.<Object>get(prefix + "timeBudgetMs"));
        assertTrue(fixture.holder.validate(prefix + "timeBudgetMs", .1D));
        assertTrue(fixture.holder.validate(prefix + "timeBudgetMs", 10D));
        assertFalse(fixture.holder.validate(prefix + "timeBudgetMs", .09D));
        assertFalse(fixture.holder.validate(prefix + "timeBudgetMs", 10.1D));
        assertFalse(fixture.holder.validate(prefix + "maxConcurrentSearches", 0));
        assertFalse(fixture.holder.validate(prefix + "maxConcurrentSearches", 257));
        assertFalse(fixture.holder.validate(prefix + "timeoutSeconds", 0));
        assertFalse(fixture.holder.validate(prefix + "timeoutSeconds", 301));
        xin.vanilla.banira.common.config.ConfigEntryDescriptor budget = fixture.descriptors.stream()
                .filter(d -> d.getPath().equals(prefix + "timeBudgetMs")).findFirst().get();
        assertEquals(1, budget.getDecimalPlaces());
        assertEquals(fixture.defaults, ConfigBaselineFixture.readView(CommonConfigView.get(), CommonConfigView.class));
    }

    @Test
    public void ruleListsAreIndependentAndKeepCommas() throws Exception {
        ConfigBaselineFixture.bind(CommonConfig.class, null);
        List<String> original = new ArrayList<>(CommonConfigView.get().base().safeTeleport().unsafeBlocks());
        CommonConfigView.get().base().safeTeleport().unsafeBlocks().clear();
        assertEquals(original, CommonConfigView.get().base().safeTeleport().unsafeBlocks());
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.bind(CommonConfig.class);
        List<String> rules = Arrays.asList("tick, clazz, itemClazz -> tick >= 5", "minecraft:arrow");
        CommonConfigView.get().base().safeTeleport().unsafeBlocks(rules);
        assertEquals(rules, CommonConfigView.get().base().safeTeleport().unsafeBlocks());
        CommonConfigView.get().base().safeTeleport().unsafeBlocks().clear();
        assertEquals(rules, fixture.holder.get("base.safeTeleport.unsafeBlocks"));
        assertEquals(0, fixture.saves);
        CommonConfigView.get().handle().save();
        assertEquals(1, fixture.saves);
    }
}
