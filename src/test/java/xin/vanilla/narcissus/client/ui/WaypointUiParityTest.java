package xin.vanilla.narcissus.client.ui;

import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiDetachReason;
import xin.vanilla.banira.api.client.ui.UiPointer;
import xin.vanilla.banira.api.client.ui.UiRowProvider;
import xin.vanilla.banira.internal.client.ui.NativeUiView;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class WaypointUiParityTest {
    @BeforeClass
    public static void bootstrap() {
        net.neoforged.fml.loading.LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    static class View extends NativeUiView {
        View() {
            super(new Object());
        }

        UiRowProvider rows() {
            return rowProviders.get("fields");
        }

        int limit() {
            return rowLimits.get("fields");
        }

        String text(String id) {
            return texts.containsKey(id) ? texts.get(id).text() : "";
        }

        String draft(String id) {
            return drafts.get(id);
        }

        boolean enabled(String id) {
            return enabled.getOrDefault(id, true);
        }

        String tooltip(String id) {
            return tooltips.get(id).text();
        }

        @Override
        public xin.vanilla.banira.client.data.ScreenCoordinate bounds(String id) {
            return new xin.vanilla.banira.client.data.ScreenCoordinate(0, 1, 320, 240);
        }
    }

    static class Port implements NarcissusListUiSession.Port {
        long revision;
        int tab, actions, reads, ticks, closes, moves;
        String query = "", selected = "dimension:home", cost = "confirmed";
        boolean confirming;
        List<NarcissusListUiSession.Entry> entries = new ArrayList<>(List.of(
                new NarcissusListUiSession.Entry("dimension:home", "same name", "-1,64,-2"),
                new NarcissusListUiSession.Entry("other-dimension:home", "same name", "1,64,2")));

        public long revision() {
            return revision;
        }

        public List<NarcissusListUiSession.Entry> entries() {
            reads++;
            return entries;
        }

        public Set<NarcissusListUiSession.Action> actions() {
            return EnumSet.allOf(NarcissusListUiSession.Action.class);
        }

        public int tabs() {
            return 3;
        }

        public int tab() {
            return tab;
        }

        public void tab(int index) {
            tab = index;
            revision++;
        }

        public String query() {
            return query;
        }

        public void query(String value) {
            query = value;
            revision++;
        }

        public void select(String key) {
            selected = key;
            revision++;
        }

        public String selected() {
            return selected;
        }

        public String label(String id) {
            return id;
        }

        public String detail() {
            return selected;
        }

        public String cost() {
            return cost;
        }

        public boolean enabled(NarcissusListUiSession.Action action) {
            return action != NarcissusListUiSession.Action.CONFIRM || confirming;
        }

        public void action(NarcissusListUiSession.Action action) {
            actions++;
            if (action == NarcissusListUiSession.Action.DELETE) confirming = true;
            if (action == NarcissusListUiSession.Action.CONFIRM) confirming = false;
            revision++;
        }

        public void move(String key, int offset) {
            selected = key;
            moves += offset;
            revision++;
        }

        public void tick() {
            ticks++;
        }

        public void close() {
            closes++;
        }
    }

    @Test
    public void rowIdentityNeverComesFromTheDisplayName() {
        Port port = new Port();
        View view = new View();
        WaypointUiSession session = new WaypointUiSession(port, () -> true);
        session.bind(view);
        assertNotEquals(view.rows().key(0), view.rows().key(1));
        View row = new View();
        view.rows().bind(row, 1);
        row.activate("open");
        assertEquals("other-dimension:home", port.selected);
        assertEquals("same name\n1,64,2", row.tooltip("open"));
        assertEquals(0, port.actions);
    }

    @Test
    public void allTabsAndSearchSurviveRebindWithoutSendingOperations() {
        Port port = new Port();
        WaypointUiSession session = new WaypointUiSession(port, () -> true);
        View first = new View();
        session.bind(first);
        for (int index = 0; index < 3; index++) {
            first.activate("tab" + index);
            assertEquals(index, port.tab);
        }
        first.inputDraft("search", "long English / \u4e2d\u6587");
        session.detach(UiDetachReason.SWITCH);
        View next = new View();
        session.bind(next);
        assertEquals(port.query, next.draft("search"));
        assertEquals(0, port.actions);
        assertEquals(0, port.closes);
    }

    @Test
    public void detachedViewAndOldRowsCannotReplayActions() {
        Port port = new Port();
        WaypointUiSession session = new WaypointUiSession(port, () -> true);
        View first = new View();
        session.bind(first);
        View row = new View();
        first.rows().bind(row, 1);
        session.detach(UiDetachReason.RELOAD);
        session.bind(new View());
        first.activate("teleport");
        first.inputDraft("search", "stale");
        row.activate("open");
        assertEquals(0, port.actions);
        assertEquals("", port.query);
        assertEquals("dimension:home", port.selected);
    }

    @Test
    public void staleConnectionRejectsTabsSearchReorderAndMutation() {
        Port port = new Port();
        AtomicBoolean connected = new AtomicBoolean(true);
        WaypointUiSession session = new WaypointUiSession(port, connected::get);
        View view = new View();
        session.bind(view);
        connected.set(false);
        session.tick();
        view.activate("delete");
        view.activate("teleport");
        view.activate("tab1");
        view.inputDraft("search", "stale");
        assertEquals(0, port.actions);
        assertEquals(0, port.tab);
        assertEquals("", port.query);
        assertFalse(view.enabled("teleport"));
        assertEquals(0, port.ticks);
    }

    @Test
    public void deleteConfirmationCanOnlySubmitOnce() {
        Port port = new Port();
        WaypointUiSession session = new WaypointUiSession(port, () -> true);
        View view = new View();
        session.bind(view);
        view.activate("delete");
        view.activate("confirm");
        view.activate("confirm");
        assertEquals(2, port.actions);
        assertFalse(port.confirming);
    }

    @Test
    public void listRealizationIsBoundedAndUnchangedTicksDoNotRescanEntries() {
        Port port = new Port();
        for (int i = 0; i < 10000; i++) port.entries.add(new NarcissusListUiSession.Entry("id" + i, "item", "coords"));
        WaypointUiSession session = new WaypointUiSession(port, () -> true);
        View view = new View();
        session.bind(view);
        for (int i = 0; i < 100; i++) session.tick();
        assertEquals(10002, view.rows().size());
        assertEquals(120, view.limit());
        assertEquals(1, port.reads);
    }

    @Test
    public void heldDragMovesOriginalIdentityOnceAndDetachCancelsIt() {
        Port port = new Port();
        AtomicLong clock = new AtomicLong();
        WaypointUiSession session = new WaypointUiSession(port, () -> true, clock::get);
        View view = new View();
        session.bind(view);
        View row = new View();
        view.rows().bind(row, 0);
        row.pointer("open", new UiPointer(UiPointer.Kind.DOWN, 2, 10, 0));
        clock.set(300_000_000);
        row.pointer("open", new UiPointer(UiPointer.Kind.UP, 2, 58, 0));
        row.pointer("open", new UiPointer(UiPointer.Kind.UP, 2, 58, 0));
        assertEquals(1, port.moves);
        assertEquals("dimension:home", port.selected);
        session.detach(UiDetachReason.SWITCH);
        row.pointer("open", new UiPointer(UiPointer.Kind.UP, 2, 106, 0));
        assertEquals(1, port.moves);
    }

    @Test
    public void accessListUsesTheSameDraftAndSingleConfirmationContract() {
        Port port = new Port();
        AccessListUiSession session = new AccessListUiSession(port, () -> true);
        View first = new View();
        session.bind(first);
        first.inputDraft("search", "UUID");
        session.detach(UiDetachReason.RESIZE);
        View next = new View();
        session.bind(next);
        assertEquals("UUID", next.draft("search"));
        next.activate("delete");
        next.activate("confirm");
        next.activate("confirm");
        assertEquals(2, port.actions);
    }

    @Test
    public void releasingOutsideTheViewportNeverReorders() {
        Port port = new Port();
        AtomicLong clock = new AtomicLong();
        WaypointUiSession session = new WaypointUiSession(port, () -> true, clock::get);
        View view = new View();
        session.bind(view);
        View row = new View();
        view.rows().bind(row, 0);
        row.pointer("open", new UiPointer(UiPointer.Kind.DOWN, 2, 100, 0));
        clock.set(300_000_000);
        row.pointer("open", new UiPointer(UiPointer.Kind.UP, 0, 0, 0));
        assertEquals(0, port.moves);
    }

    @Test
    public void finalCloseReleasesOnceAndCannotBeRebound() {
        Port port = new Port();
        WaypointUiSession session = new WaypointUiSession(port, () -> true);
        View view = new View();
        session.bind(view);
        session.close(UiCloseReason.DISCONNECT);
        session.close(UiCloseReason.CANCEL);
        View next = new View();
        session.bind(next);
        next.activate("teleport");
        view.activate("teleport");
        assertEquals(1, port.closes);
        assertEquals(0, port.actions);
        assertNull(next.rows());
    }

    @Test
    public void emptyOrUnavailableListsNeverSubmitAnOperation() {
        Port port = new Port() {
            @Override
            public boolean enabled(NarcissusListUiSession.Action action) {
                return false;
            }
        };
        port.entries.clear();
        WaypointUiSession session = new WaypointUiSession(port, () -> true);
        View view = new View();
        session.bind(view);
        assertEquals(0, view.rows().size());
        for (NarcissusListUiSession.Action action : port.actions()) {
            String id = action.name().toLowerCase(Locale.ROOT);
            assertFalse(view.enabled(id));
            view.activate(id);
        }
        assertEquals(0, port.actions);
    }

    @Test
    public void closedVirtualWindowsCannotAccumulateOwnedRowBindings() throws Exception {
        Port port = new Port();
        for (int i = 0; i < 400; i++) port.entries.add(new NarcissusListUiSession.Entry("key" + i, "name", "detail"));
        WaypointUiSession session = new WaypointUiSession(port, () -> true);
        View view = new View();
        session.bind(view);
        for (int cycle = 0; cycle < 4; cycle++) {
            List<View> window = new ArrayList<>();
            for (int i = 0; i < 120; i++) {
                View row = new View();
                view.rows().bind(row, i);
                window.add(row);
            }
            for (View row : window) row.close();
        }
        java.lang.reflect.Field field = NarcissusListUiSession.class.getDeclaredField("rows");
        field.setAccessible(true);
        assertTrue(((Map<?, ?>) field.get(session)).size() <= 120);
    }
}
