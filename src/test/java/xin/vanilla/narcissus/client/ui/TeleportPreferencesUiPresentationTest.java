package xin.vanilla.narcissus.client.ui;

import net.minecraft.nbt.CompoundTag;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiRowProvider;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class TeleportPreferencesUiPresentationTest {
    private static final class EditorView extends WaypointUiParityTest.View {
        UiRowProvider categories() {
            return rowProviders.get("categories");
        }
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

    private TeleportPreferencesUiSession session(java.util.function.Consumer<CompoundTag> send) {
        return new TeleportPreferencesUiSession(owner(), t -> 0, () -> true, send);
    }

    @Test
    public void teleportPreferencesOptIntoOneSharedEditorSection() {
        var session = session(payload -> {
        });
        var view = new EditorView();
        session.shared().bind(view);
        assertNotNull("Real personal section is missing", view.categories());
        assertEquals(16, view.rows().size());
        assertEquals("16 / 16", view.text("entry_count"));
    }


    @Test
    public void actualFormRejectsInvalidRawIntegersEvenWhenFilteredOut() {
        for (String value : List.of("", " ", " 1", "1 ", "1.0", "1.5", "1e2", "2147483648", "-2147483649")) {
            AtomicInteger sends = new AtomicInteger();
            var session = session(payload -> sends.incrementAndGet());
            session.draft(EnumTeleportType.TP_COORDINATE, value);
            session.shared().query("TP_HOME");
            assertFalse(value, session.shared().fields().canSubmit());
            assertFalse(value, session.submit());
            assertEquals(value, session.draft(EnumTeleportType.TP_COORDINATE));
            assertEquals(0, sends.get());
        }
    }

    @Test
    public void validRawIntegersProduceCompletePayloadOnlyOnce() {
        for (String value : List.of("0", "+1", "01", "-0", "12", "\uff11\uff12", "\u0661\u0662")) {
            AtomicInteger sends = new AtomicInteger();
            var session = session(payload -> {
                assertEquals(16, payload.getAllKeys().size());
                assertFalse(payload.contains("DEATH"));
                assertFalse(payload.contains("OTHER"));
                assertEquals(Integer.parseInt(value), payload.getInt("TP_COORDINATE"));
                sends.incrementAndGet();
            });
            session.draft(EnumTeleportType.TP_COORDINATE, value);
            assertTrue(value, session.shared().fields().canSubmit());
            assertEquals(value, session.draft(EnumTeleportType.TP_COORDINATE));
            assertTrue(value, session.submit());
            assertFalse(session.submit());
            assertEquals(1, sends.get());
        }
    }

    @Test
    public void sendFailureRetainsDraftAndCanRetryWithoutReopening() {
        AtomicBoolean fail = new AtomicBoolean(true);
        AtomicInteger sends = new AtomicInteger();
        var session = session(payload -> {
            if (fail.get()) throw new IllegalStateException("send failed");
            sends.incrementAndGet();
        });
        session.draft(EnumTeleportType.TP_HOME, "09");
        assertFalse(session.submit());
        assertEquals("09", session.draft(EnumTeleportType.TP_HOME));
        assertFalse(session.shared().fields().finished());
        fail.set(false);
        assertTrue(session.submit());
        assertFalse(session.submit());
        assertEquals(1, sends.get());
    }

    @Test
    public void noSourceSupplierAndViewHandoffDoNotReplaceOpenDrafts() {
        var session = session(payload -> {
        });
        session.draft(EnumTeleportType.TP_HOME, "1.");
        session.shared().query("TP_HOME");
        var old = new WaypointUiParityTest.View();
        session.shared().bind(old);
        session.shared().tick();
        session.shared().detach(UiDetachReason.SWITCH);
        session.shared().bind(new WaypointUiParityTest.View());
        old.activate("submit");
        assertEquals("1.", session.draft(EnumTeleportType.TP_HOME));
        assertEquals("TP_HOME", session.shared().query());
        session.shared().close(UiCloseReason.DISCONNECT);
        assertFalse(session.submit());
    }
}
