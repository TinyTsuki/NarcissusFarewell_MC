package xin.vanilla.narcissus.config;

import org.junit.Test;
import xin.vanilla.narcissus.config.preset.CommonConfigPresets;
import xin.vanilla.banira.common.config.ConfigHolder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class ConfigPresetBaselineTest {
    @Test public void presetsKeepSearchExecutionOverridesAndExtensionValues() throws Exception {
        for (Consumer<ConfigHolder> preset : java.util.Arrays.<Consumer<ConfigHolder>>asList(
                CommonConfigPresets::resetConfig, CommonConfigPresets::resetConfigWithMode1,
                CommonConfigPresets::resetConfigWithMode2, CommonConfigPresets::resetConfigWithMode3)) {
            ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
            fixture.holder.set("base.safeTeleport.search.timeBudgetMs", .5D);
            fixture.holder.set("base.safeTeleport.search.maxConcurrentSearches", 4);
            fixture.holder.set("base.safeTeleport.search.timeoutSeconds", 5);
            fixture.values.put("extension.keep", "preserve,me");
            preset.accept(fixture.holder);
            org.junit.Assert.assertEquals(Double.valueOf(.5), fixture.holder.<Object>get("base.safeTeleport.search.timeBudgetMs"));
            org.junit.Assert.assertEquals(Integer.valueOf(4), fixture.holder.<Object>get("base.safeTeleport.search.maxConcurrentSearches"));
            org.junit.Assert.assertEquals(Integer.valueOf(5), fixture.holder.<Object>get("base.safeTeleport.search.timeoutSeconds"));
            org.junit.Assert.assertEquals("preserve,me", fixture.values.get("extension.keep"));
            org.junit.Assert.assertFalse(fixture.written.contains("extension.keep"));
        }
    }
    @Test public void presetDoesNotOverwriteAnUnrelatedExtensionValue() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.values.put("extension.keep", "preserve,me");
        CommonConfigPresets.resetConfig(fixture.holder);
        org.junit.Assert.assertEquals("preserve,me", fixture.values.get("extension.keep"));
        org.junit.Assert.assertFalse(fixture.written.contains("extension.keep"));
    }
    @Test public void defaultPresetKeepsItsOriginalCoverage() throws Exception {
        capture("preset-default", CommonConfigPresets::resetConfig);
    }
    @Test public void modeOneKeepsItsOriginalCoverage() throws Exception {
        capture("preset-one", CommonConfigPresets::resetConfigWithMode1);
    }
    @Test public void modeTwoKeepsItsOriginalCoverageAndSaves() throws Exception {
        capture("preset-two", CommonConfigPresets::resetConfigWithMode2);
    }
    @Test public void modeThreeKeepsItsOriginalCoverage() throws Exception {
        capture("preset-three", CommonConfigPresets::resetConfigWithMode3);
    }

    private static void capture(String name, Consumer<ConfigHolder> preset) throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.nonDefaultValues();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("before", new TreeMap<>(fixture.values));
        preset.accept(fixture.holder);
        result.put("after", new TreeMap<>(fixture.values));
        result.put("written", fixture.written);
        result.put("saves", fixture.saves);
        ConfigBaselineFixture.assertSnapshot(name, result);
    }
}
