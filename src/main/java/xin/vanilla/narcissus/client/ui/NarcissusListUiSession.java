package xin.vanilla.narcissus.client.ui;

import xin.vanilla.banira.api.client.ui.*;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.internal.client.ui.NativeUiView;
import xin.vanilla.banira.internal.client.ui.UiOwnedResources;
import xin.vanilla.banira.internal.client.ui.UiRowPresentation;
import xin.vanilla.narcissus.NarcissusComponent;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

public abstract class NarcissusListUiSession implements UiPageSession {
    public enum Action { ADD, DELETE, CONFIRM, CANCEL, EDIT, TELEPORT, NAME, TIME, DISTANCE, UP, DOWN }
    public record Entry(String key, String name, String detail) { }
    public interface Port {
        long revision();
        List<Entry> entries();
        int tabs();
        int tab();
        void tab(int index);
        String query();
        void query(String value);
        void select(String key);
        String selected();
        String label(String id);
        String detail();
        String cost();
        boolean enabled(Action action);
        void action(Action action);
        void move(String key, int offset);
        void tick();
        void close();
        Set<Action> actions();
    }
    private final Port port;
    private final BooleanSupplier connection;
    private final LongSupplier clock;
    private UiView view;
    private UiOwnedResources bindings = new UiOwnedResources();
    private final Map<UiView, UiOwnedResources> rows = new IdentityHashMap<>();
    private final Map<String, String> texts = new HashMap<>();
    private final Map<String, Boolean> enabled = new HashMap<>();
    private List<Entry> entries = List.of();
    private long revision = Long.MIN_VALUE, dragTime, dragRevision;
    private String dragKey;
    private double dragY;
    private boolean closed, nativeView, rowsLocked;

    protected NarcissusListUiSession(Port port, BooleanSupplier connection, LongSupplier clock) {
        this.port = Objects.requireNonNull(port);
        this.connection = Objects.requireNonNull(connection);
        this.clock = Objects.requireNonNull(clock);
    }
    protected final Port port() { return port; }
    protected final boolean presentationCurrent() { return live(view) && !nativeView; }
    protected void bindPresentation(UiView view) { }
    protected void refreshPresentation(UiView view, boolean connected) { }
    protected boolean selectionLocked() { return false; }
    protected int rowHeight() { return 48; }
    protected boolean textTools() { return false; }
    protected boolean iconAction(String id) { return false; }
    protected boolean structuredPresentation() { return false; }
    protected String rowClass(Entry entry) { return ""; }
    protected String rowName(Entry entry) { return (entry.key().equals(port.selected()) ? "> " : "") + entry.name(); }
    protected void bindRow(UiView row, Entry entry) { }
    private boolean live(UiView owner) { return !closed && owner != null && view == owner && connection.getAsBoolean(); }
    public boolean requestClose() {
        if (view == null || closed) return false;
        view.requestClose(); return true;
    }
    @Override public void bind(UiView view) {
        detach(UiDetachReason.RELOAD);
        if (closed) return;
        this.view = Objects.requireNonNull(view);
        bindings = new UiOwnedResources();
        nativeView = view instanceof NativeUiView && ((NativeUiView) view).screen() instanceof BaniraScreen;
        if (nativeView) return;
        for (String id : List.of("title", "close")) if (!iconAction(id)) text(id, port.label(id));
        view.control("search", new UiControlSpec(UiControlSpec.Kind.TEXT, 32767, "", false));
        view.draft("search", port.query());
        view.tooltip("search", NarcissusComponent.get().literal(port.label("search")));
        bindings.own(view.onDraft("search", value -> {
            if (live(view) && !selectionLocked()) { port.query(value); refresh(); }
        }));
        bindings.own(view.onAction("close", () -> { if (this.view == view) view.requestClose(); }));
        for (int index = 0; index < port.tabs(); index++) {
            int tab = index;
            bindings.own(view.onAction("tab" + index, () -> {
                if (live(view) && !selectionLocked()) {
                    if (view instanceof NativeUiView) ((NativeUiView) view).state().rowPosition(0);
                    port.tab(tab); refresh();
                }
            }));
        }
        for (Action action : port.actions()) {
            String id = action.name().toLowerCase(Locale.ROOT);
            if (!iconAction(id)) text(id, port.label(id));
            view.tooltip(id, NarcissusComponent.get().literal(port.label(id + "_tip")));
            bindings.own(view.onAction(id, () -> {
                if (live(view) && port.enabled(action)) {
                    port.action(action); refresh();
                }
            }));
        }
        bindPresentation(view);
        refresh();
    }
    @Override public void tick() {
        if (view == null || nativeView || closed) return;
        if (connection.getAsBoolean()) port.tick();
        refresh();
    }
    protected final void refresh() {
        if (view == null || nativeView || closed) return;
        boolean locked = selectionLocked();
        if (port.revision() != revision || rowsLocked != locked) {
            rowsLocked = locked;
            revision = port.revision(); dragKey = null;
            entries = List.copyOf(port.entries());
            rebuild();
        }
        for (int index = 0; index < port.tabs(); index++) text("tab" + index, port.label("tab" + index));
        text("status", port.label("status"));
        if (!structuredPresentation()) {
            text("detail", port.detail());
            text("cost", port.cost());
        }
        for (Action action : port.actions()) {
            String id = action.name().toLowerCase(Locale.ROOT);
            boolean next = connection.getAsBoolean() && port.enabled(action);
            if (!Objects.equals(enabled.put(id, next), next)) {
                view.enabled(id, next);
                if (next) view.tooltip(id, NarcissusComponent.get().literal(port.label(id + "_tip")));
            }
        }
        refreshPresentation(view, connection.getAsBoolean());
    }
    protected final void text(String id, String value) {
        String next = Objects.toString(value, "");
        if (next.isEmpty() && !texts.containsKey(id)) { texts.put(id, next); return; }
        if (!Objects.equals(texts.put(id, next), next)) {
            view.text(id, NarcissusComponent.get().literal(next));
            view.tooltip(id, NarcissusComponent.get().literal(next));
        }
    }
    private void rebuild() {
        releaseRows();
        UiView owner = view;
        List<Entry> snapshot = entries;
        long token = revision;
        view.rows("fields", new UiRowPresentation() {
            public int rowHeight() { return NarcissusListUiSession.this.rowHeight(); }
            public boolean textTools() { return NarcissusListUiSession.this.textTools(); }
            public String rowClass(int index) { return NarcissusListUiSession.this.rowClass(snapshot.get(index)); }
            public int size() { return snapshot.size(); }
            public String key(int index) { return snapshot.get(index).key(); }
            public void bind(UiView row, int index) {
                UiOwnedResources previous = rows.remove(row);
                if (previous != null) previous.close();
                if (rows.size() >= 120) {
                    Iterator<Map.Entry<UiView, UiOwnedResources>> iterator = rows.entrySet().iterator();
                    while (iterator.hasNext()) {
                        Map.Entry<UiView, UiOwnedResources> entry = iterator.next();
                        if (entry.getKey() instanceof NativeUiView && ((NativeUiView) entry.getKey()).closed()) {
                            entry.getValue().close(); iterator.remove();
                        }
                    }
                    if (rows.size() >= 120) throw new IllegalStateException("Narcissus row ownership limit exceeded");
                }
                UiOwnedResources owned = new UiOwnedResources(); rows.put(row, owned);
                Entry entry = snapshot.get(index);
                row.text("open", NarcissusComponent.get().literal(rowName(entry)));
                row.text("label", NarcissusComponent.get().literal(entry.detail()));
                row.tooltip("open", NarcissusComponent.get().literal(entry.name() + "\n" + entry.detail()));
                row.tooltip("label", NarcissusComponent.get().literal(entry.detail()));
                row.enabled("open", !selectionLocked());
                bindRow(row, entry);
                owned.own(row.onAction("open", () -> {
                    if (live(owner) && token == revision && !selectionLocked()) { port.select(entry.key()); refresh(); }
                }));
                owned.own(row.onPointer("open", pointer -> {
                    if (!live(owner) || token != revision || !pointer.finite() || selectionLocked()) return;
                    if (pointer.kind() == UiPointer.Kind.DOWN && pointer.button() == 0 && port.enabled(Action.UP)) {
                        dragKey = entry.key(); dragY = pointer.y(); dragTime = clock.getAsLong(); dragRevision = revision;
                    } else if (pointer.kind() == UiPointer.Kind.UP && entry.key().equals(dragKey)) {
                        String key = dragKey; dragKey = null;
                        var content = owner.bounds("content");
                        if (content != null && (pointer.x() < content.x() || pointer.x() >= content.x() + content.width()
                                || pointer.y() < content.y() || pointer.y() >= content.y() + content.height())) return;
                        double delta = pointer.y() - dragY;
                        if (dragRevision == revision && clock.getAsLong() - dragTime >= 260_000_000L && Math.abs(delta) >= 24) {
                            int offset = (int) Math.max(-entries.size(), Math.min(entries.size(), Math.round(delta / rowHeight())));
                            port.move(key, offset); refresh();
                        }
                    }
                }));
            }
        }, 120);
    }
    private void releaseRows() { for (UiOwnedResources row : rows.values()) row.close(); rows.clear(); }
    @Override public void detach(UiDetachReason reason) {
        view = null; nativeView = false; dragKey = null;
        try { bindings.close(); } finally { releaseRows(); }
        texts.clear(); enabled.clear(); revision = Long.MIN_VALUE;
    }
    @Override public void close(UiCloseReason reason) {
        if (closed) return;
        closed = true;
        try { detach(UiDetachReason.DISCONNECT); } finally { port.close(); }
    }
}
