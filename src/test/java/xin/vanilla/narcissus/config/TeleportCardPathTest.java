package xin.vanilla.narcissus.config;

import org.junit.Test;
import static org.junit.Assert.*;

public class TeleportCardPathTest {
    @Test public void configuredCardValuesUseRegisteredPaths() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.bind(CommonConfig.class);
        fixture.holder.set("base.teleportCard.teleportCard", true);
        fixture.holder.set("base.teleportCard.teleportCardDaily", 17);
        assertTrue(CommonConfig.get().base().teleportCard().teleportCard());
        assertEquals(17, CommonConfig.get().base().teleportCard().teleportCardDaily());
    }
}
