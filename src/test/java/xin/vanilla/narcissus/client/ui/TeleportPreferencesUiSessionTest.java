package xin.vanilla.narcissus.client.ui;

import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.narcissus.config.TeleportCountdownHelper;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class TeleportPreferencesUiSessionTest {
    @org.junit.Rule
    public final UiTestPlatform platform = new UiTestPlatform();

    @BeforeClass
    public static void bootstrap() {
        net.minecraft.util.registry.Bootstrap.bootStrap();
    }

    private net.minecraft.client.gui.screen.Screen owner() {
        return new net.minecraft.client.gui.screen.Screen(new net.minecraft.util.text.StringTextComponent("")) {
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


}
