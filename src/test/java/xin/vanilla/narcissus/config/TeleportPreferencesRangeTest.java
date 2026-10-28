package xin.vanilla.narcissus.config;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.platform.BaniraPlatform;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.narcissus.client.ui.TeleportPreferencesUiSession;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class TeleportPreferencesRangeTest {
    private BaniraPlatform previousPlatform;

    @Before
    public void rememberPlatform() throws Exception {
        previousPlatform = (BaniraPlatform) platformField().get(null);
    }

    @After
    public void restorePlatform() throws Exception {
        platformField().set(null, previousPlatform);
    }

    private java.lang.reflect.Field platformField() throws Exception {
        var field = BaniraPlatforms.class.getDeclaredField("platform");
        field.setAccessible(true);
        return field;
    }

    private void bind(ConfigBaselineFixture fixture) {
        fixture.bind(CommonConfig.class);
        BaniraPlatform configPlatform = BaniraPlatforms.get();
        BaniraPlatforms.install((BaniraPlatform) java.lang.reflect.Proxy.newProxyInstance(
                BaniraPlatform.class.getClassLoader(), new Class<?>[]{BaniraPlatform.class}, (proxy, method, args) -> {
                    if (method.getName().equals("isClient")) return false;
                    return method.invoke(configPlatform, args);
                }));
    }

    @BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    private net.minecraft.client.gui.screens.Screen owner() {
        return new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.empty()) {
        };
    }

    @Test
    public void realConfigRangesNormalizeAndValidateTheCompleteForm() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        bind(fixture);
        try {
            for (int[] sample : new int[][]{{3, 12, 3, 12}, {12, 3, 3, 12}, {5, 5, 5, 5}, {0, 86400, 0, 86400}, {90000, 100000, 86400, 86400}}) {
                fixture.values.put("teleportCountdown.playerCountdownRangeMin", sample[0]);
                fixture.values.put("teleportCountdown.playerCountdownRangeMax", sample[1]);
                assertEquals(sample[2], TeleportCountdownHelper.playerRangeLo());
                assertEquals(sample[3], TeleportCountdownHelper.playerRangeHi());
                AtomicInteger sends = new AtomicInteger();
                var session = new TeleportPreferencesUiSession(owner(), type -> 0, () -> true, payload -> {
                    assertEquals(16, payload.getAllKeys().size());
                    assertEquals(sample[3], payload.getInt("TP_COORDINATE"));
                    sends.incrementAndGet();
                });
                assertEquals(Integer.toString(sample[2]), session.draft(EnumTeleportType.TP_HOME));
                session.draft(EnumTeleportType.TP_COORDINATE, Integer.toString(sample[3] + 1));
                assertFalse(session.submit());
                session.draft(EnumTeleportType.TP_COORDINATE, Integer.toString(sample[3]));
                assertTrue(session.submit());
                assertEquals(1, sends.get());
            }
        } finally {
            restorePlatform();
        }
    }

    @Test
    public void tighteningRangeKeepsRawDraftAndForcedPolicyDoesNotBlockPersonalEditing() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        bind(fixture);
        try {
            fixture.values.put("teleportCountdown.playerCountdownRangeMin", 0);
            fixture.values.put("teleportCountdown.playerCountdownRangeMax", 300);
            fixture.values.put("teleportCountdown.forceServerCountdown", true);
            var session = new TeleportPreferencesUiSession(owner(), type -> 0, () -> true, payload -> {
            });
            session.draft(EnumTeleportType.TP_HOME, "20");
            assertTrue(session.shared().fields().canSubmit());
            fixture.values.put("teleportCountdown.playerCountdownRangeMax", 5);
            assertEquals("20", session.draft(EnumTeleportType.TP_HOME));
            assertFalse(session.shared().fields().canSubmit());
            assertFalse(session.submit());
            fixture.values.put("teleportCountdown.playerCountdownRangeMax", 300);
            assertTrue(session.submit());
        } finally {
            restorePlatform();
        }
    }
}
