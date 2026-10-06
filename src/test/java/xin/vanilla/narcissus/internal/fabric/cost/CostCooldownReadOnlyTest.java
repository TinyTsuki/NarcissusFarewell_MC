package xin.vanilla.narcissus.internal.fabric.cost;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.platform.BaniraConfigHandle;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.ConfigBaselineFixture;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCoolDownType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertTrue;

public class CostCooldownReadOnlyTest {
    private final FabricCostPlayerFixture fixture = new FabricCostPlayerFixture();

    @Before
    public void setup() throws Exception {
        fixture.setup();
        ConfigHolder holder = ConfigBaselineFixture.holderWithValues(CommonConfig.class, Collections.singletonMap(
                "base.teleportRequest.teleportRequestCooldownType", EnumCoolDownType.INDIVIDUAL));
        Method bind = ConfigBaselineFixture.class.getDeclaredMethod("bind", Class.class, BaniraConfigHandle.class);
        bind.setAccessible(true);
        bind.invoke(null, CommonConfig.class, holder);
        NarcissusFarewell.getTeleportRequest().clear();
    }

    @After
    public void cleanup() throws Exception {
        NarcissusFarewell.getTeleportRequest().clear();
        fixture.cleanup();
    }

    @Test
    public void queryingCooldownDoesNotFlushDirtyHistory() {
        PlayerTeleportData data = PlayerTeleportData.getData(fixture.player());
        data.setTeleportRecords(Collections.singletonList(new TeleportRecord().setTeleportType(EnumTeleportType.TP_HOME)
                .setTeleportTime(new Date())));
        data.setDirty();
        assertTrue(NarcissusUtils.getTeleportCoolDown(fixture.player(), EnumTeleportType.TP_HOME) >= 0);
        assertTrue("A quote must not save or clear pending player changes", data.isDirty());
    }
}
