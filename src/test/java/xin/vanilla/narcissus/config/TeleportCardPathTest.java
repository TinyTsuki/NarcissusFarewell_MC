package xin.vanilla.narcissus.config;

import org.junit.Test;
import static org.junit.Assert.*;

public class TeleportCardPathTest {
    @Test public void configuredCardValuesUseRegisteredPaths() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.bind(CommonConfig.class);
        fixture.holder.set("cost.cards.enabled", true);
        fixture.holder.set("cost.cards.dailyGrant", 17);
        assertTrue(CommonConfig.get().cost().cards().enabled());
        assertEquals(17, CommonConfig.get().cost().cards().dailyGrant());
    }
}
