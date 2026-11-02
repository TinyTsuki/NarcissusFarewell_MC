package xin.vanilla.narcissus.client.ui;

import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.narcissus.config.TeleportCountdownHelper;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class TeleportPreferencesUiSessionTest {
    @org.junit.ClassRule
    public static final HeadlessUiPlatformRule PLATFORM = new HeadlessUiPlatformRule();

    @BeforeClass
    public static void bootstrap() {
        net.minecraft.server.Bootstrap.bootStrap();
    }

    private net.minecraft.client.gui.screens.Screen owner() {
        return new net.minecraft.client.gui.screens.Screen(new net.minecraft.network.chat.TextComponent("")) {
        };
    }

    @Test
    public void rawDraftSurvivesRebindAndInvalidOrFractionalValuesCannotSubmit() {
        AtomicInteger sends = new AtomicInteger();
        TeleportPreferencesUiSession session = new TeleportPreferencesUiSession(owner(), type -> 0, () -> true, payload -> sends.incrementAndGet());
        EnumTeleportType type = EnumTeleportType.countdownConfigurableTypes().get(0);
        session.draft(type, "1.");
        session.shared().bind(new WaypointUiParityTest.View());
        session.shared().detach(xin.vanilla.banira.api.client.ui.UiDetachReason.SWITCH);
        session.shared().bind(new WaypointUiParityTest.View());
        assertEquals("1.", session.draft(type));
        assertFalse(session.submit());
        session.draft(type, "-");
        assertFalse(session.submit());
        session.draft(type, "86401");
        assertFalse(session.submit());
        assertEquals(0, sends.get());
    }

    @Test
    public void validFullPayloadSubmitsOnceAndOldConnectionCannotSubmit() {
        AtomicInteger sends = new AtomicInteger();
        AtomicBoolean connected = new AtomicBoolean(true);
        TeleportPreferencesUiSession session = new TeleportPreferencesUiSession(owner(), type -> 0, connected::get, payload -> {
            for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes())
                assertEquals(TeleportCountdownHelper.clampToPlayerAllowedRange(0), payload.getInt(type.name()));
            assertEquals(EnumTeleportType.countdownConfigurableTypes().size(), payload.getAllKeys().size());
            sends.incrementAndGet();
        });
        connected.set(false);
        assertFalse(session.submit());
        assertEquals(0, sends.get());
        connected.set(true);
        assertTrue(session.submit());
        assertFalse(session.submit());
        assertEquals(1, sends.get());
    }

    @Test
    public void nativeSaveCapturesRawInputsBeforeValidatingAndSubmitting() throws Exception {
        String source = UiBoundaryChecks.text(java.nio.file.Paths.get(
                "src/main/java/xin/vanilla/narcissus/screen/PlayerConfigScreen.java"));
        String save = source.substring(source.indexOf("protected void savePlayerConfig()"), source.indexOf("private void savePreferences"));
        assertTrue(save.contains("captureUiState(null)"));
        assertTrue(save.indexOf("captureUiState(null)") < save.indexOf("preferences.submit()"));
        assertTrue(save.contains("canSubmit()"));
    }
}
