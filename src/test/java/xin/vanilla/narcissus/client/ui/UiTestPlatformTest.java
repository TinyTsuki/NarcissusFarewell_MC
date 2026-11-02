package xin.vanilla.narcissus.client.ui;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import xin.vanilla.banira.api.BaniraCommonSettings;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.platform.BaniraPlatform;
import xin.vanilla.banira.platform.BaniraPlatforms;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.TeleportCountdownHelper;

import java.lang.reflect.Field;

import static org.junit.Assert.*;

public class UiTestPlatformTest {
    private BaniraPlatform rawPlatform() throws Exception {
        Field field = BaniraPlatforms.class.getDeclaredField("platform");
        field.setAccessible(true);
        return (BaniraPlatform) field.get(null);
    }

    private void withPlatform(Statement body) throws Throwable {
        new UiTestPlatform().apply(body, Description.EMPTY).evaluate();
    }

    @Test
    public void unboundUiUsesRealDefaultsWithoutDiscoveringTheLoader() throws Throwable {
        BaniraPlatform previous = rawPlatform();
        withPlatform(new Statement() {
            @Override
            public void evaluate() {
                assertTrue(BaniraPlatforms.isInstalled());
                assertEquals("test", BaniraPlatforms.get().loaderType());
                assertNull(BaniraConfigs.handle(CommonConfig.class));
                assertEquals(0, TeleportCountdownHelper.playerRangeLo());
                assertEquals(300, TeleportCountdownHelper.playerRangeHi());
                assertEquals(300, TeleportCountdownHelper.clampToPlayerAllowedRange(86400));
                assertFalse(BaniraCommonSettings.defaultLanguage().isEmpty());
            }
        });
        assertSame(previous, rawPlatform());
    }

    @Test
    public void failedBodyRestoresTheExactPreviousPlatform() throws Throwable {
        BaniraPlatform previous = rawPlatform();
        AssertionError expected = new AssertionError("fixture failure");
        try {
            withPlatform(new Statement() {
                @Override
                public void evaluate() {
                    throw expected;
                }
            });
            fail("Expected the fixture body to fail");
        } catch (AssertionError actual) {
            assertSame(expected, actual);
        }
        assertSame(previous, rawPlatform());
    }

    @Test
    public void nestedFixtureRestoresTheOuterPlatform() throws Throwable {
        BaniraPlatform previous = rawPlatform();
        withPlatform(new Statement() {
            @Override
            public void evaluate() throws Throwable {
                BaniraPlatform outer = rawPlatform();
                withPlatform(new Statement() {
                    @Override
                    public void evaluate() {
                        assertNotSame(outer, BaniraPlatforms.get());
                    }
                });
                assertSame(outer, rawPlatform());
            }
        });
        assertSame(previous, rawPlatform());
    }
}
