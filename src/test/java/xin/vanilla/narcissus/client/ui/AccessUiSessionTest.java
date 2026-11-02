package xin.vanilla.narcissus.client.ui;

import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiDetachReason;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class AccessUiSessionTest {
    @org.junit.ClassRule
    public static final HeadlessUiPlatformRule PLATFORM = new HeadlessUiPlatformRule();

    @BeforeClass
    public static void bootstrap() {
        WaypointUiParityTest.bootstrap();
    }

    static class Port extends WaypointUiParityTest.Port {
        boolean allowPresentation = true;

        Port() {
            selected = "";
        }

        public int tabs() {
            return 2;
        }

        public Set<NarcissusListUiSession.Action> actions() {
            return EnumSet.of(NarcissusListUiSession.Action.ADD, NarcissusListUiSession.Action.EDIT, NarcissusListUiSession.Action.DELETE, NarcissusListUiSession.Action.CONFIRM, NarcissusListUiSession.Action.CANCEL);
        }

        public boolean enabled(NarcissusListUiSession.Action action) {
            return confirming ? action == NarcissusListUiSession.Action.CONFIRM || action == NarcissusListUiSession.Action.CANCEL : action == NarcissusListUiSession.Action.ADD || !selected.isEmpty() && (action == NarcissusListUiSession.Action.EDIT || action == NarcissusListUiSession.Action.DELETE);
        }

        public void action(NarcissusListUiSession.Action action) {
            if (action == NarcissusListUiSession.Action.CANCEL) {
                actions++;
                confirming = false;
                return;
            }
            super.action(action);
        }
    }

    private NarcissusListUiSession.Port typed(Port port) throws Exception {
        Class<?> api, type;
        try {
            api = Class.forName("xin.vanilla.narcissus.client.ui.AccessListUiSession$PresentationPort");
            type = Class.forName("xin.vanilla.narcissus.client.ui.AccessUiPresentation");
        } catch (ClassNotFoundException failure) {
            fail("Missing typed access presentation port");
            return null;
        }
        return (NarcissusListUiSession.Port) Proxy.newProxyInstance(api.getClassLoader(), new Class<?>[]{api}, (proxy, method, args) -> {
            if (method.getName().equals("presentation")) {
                assertTrue("Detached player data must not be read", port.allowPresentation);
                return type.getConstructors()[0].newInstance("Mizuki", "11111111-1111-4111-8111-000000000004", "BOTH", "scope", "manual", "auto", "", "2 players", !port.selected.isEmpty(), port.confirming, port.tab == 1);
            }
            if (method.getName().equals("confirming")) return port.confirming;
            if (method.getName().equals("profileIcon")) return null;
            if (method.getName().equals("modeLabel")) return "TPA";
            Method delegate = port.getClass().getMethod(method.getName(), method.getParameterTypes());
            delegate.setAccessible(true);
            return delegate.invoke(port, args);
        });
    }

    @Test
    public void typedBindingShowsIndependentOutcomesWithoutMutations() throws Exception {
        Port p = new Port();
        p.selected = "dimension:home";
        lombok.var s = new AccessListUiSession(typed(p), () -> true);
        lombok.var v = new WaypointUiParityTest.View();
        s.bind(v);
        assertEquals("Mizuki", v.text("player_name"));
        assertEquals("manual", v.text("tpa_result"));
        assertEquals("auto", v.text("tph_result"));
        assertEquals(0, p.actions);
        assertEquals(120, v.limit());
    }

    @Test
    public void ownedConfirmationLocksRowsAndEscapeCancelsOnlyTheModal() throws Exception {
        Port p = new Port();
        p.selected = "dimension:home";
        lombok.var s = new AccessListUiSession(typed(p), () -> true);
        lombok.var v = new WaypointUiParityTest.View();
        s.bind(v);
        v.activate("delete");
        assertTrue(p.confirming);
        lombok.var row = new WaypointUiParityTest.View();
        v.rows().bind(row, 1);
        assertFalse(row.enabled("open"));
        v.inputDraft("search", "stale");
        v.activate("tab1");
        row.activate("open");
        assertEquals("", p.query);
        assertEquals(0, p.tab);
        assertEquals("dimension:home", p.selected);
        assertTrue(s.key(256, 0, 0, true));
        assertFalse(p.confirming);
        assertEquals(2, p.actions);
        assertFalse(s.key(256, 0, 0, true));
        lombok.var fresh = new WaypointUiParityTest.View();
        v.rows().bind(fresh, 1);
        assertTrue(fresh.enabled("open"));
    }

    @Test
    public void rebindPreservesQueryAndIdentityButOldRowsCannotReplay() throws Exception {
        Port p = new Port();
        p.selected = "dimension:home";
        lombok.var s = new AccessListUiSession(typed(p), () -> true);
        lombok.var first = new WaypointUiParityTest.View();
        s.bind(first);
        first.inputDraft("search", "Mizuki");
        lombok.var row = new WaypointUiParityTest.View();
        first.rows().bind(row, 1);
        s.detach(UiDetachReason.SWITCH);
        lombok.var next = new WaypointUiParityTest.View();
        s.bind(next);
        first.activate("edit");
        row.activate("open");
        assertEquals("Mizuki", next.draft("search"));
        assertEquals("dimension:home", p.selected);
        assertEquals(0, p.actions);
    }

    @Test
    public void disconnectedAndDetachedConfirmationsCannotSend() throws Exception {
        Port p = new Port();
        p.selected = "dimension:home";
        AtomicBoolean connected = new AtomicBoolean(true);
        lombok.var s = new AccessListUiSession(typed(p), connected::get);
        lombok.var v = new WaypointUiParityTest.View();
        s.bind(v);
        v.activate("delete");
        connected.set(false);
        s.tick();
        v.activate("confirm");
        assertFalse(s.key(256, 0, 0, true));
        assertEquals(1, p.actions);
        assertFalse(v.enabled("confirm"));
        s.detach(UiDetachReason.SWITCH);
        assertFalse(s.key(256, 0, 0, true));
    }

    @Test
    public void disconnectedRefreshDoesNotReadTheRemovedPlayer() throws Exception {
        Port p = new Port();
        AtomicBoolean connected = new AtomicBoolean(true);
        lombok.var s = new AccessListUiSession(typed(p), connected::get);
        lombok.var v = new WaypointUiParityTest.View();
        s.bind(v);
        p.allowPresentation = false;
        connected.set(false);
        s.tick();
        assertFalse(v.enabled("edit"));
        assertFalse(v.enabled("confirmation"));
    }
}
