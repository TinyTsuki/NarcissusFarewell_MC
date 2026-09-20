package xin.vanilla.narcissus.config;

import org.junit.Test;
import xin.vanilla.narcissus.config.preset.CommonConfigPresets;
import xin.vanilla.banira.common.config.ConfigHolder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

public class ConfigPresetBaselineTest {
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
