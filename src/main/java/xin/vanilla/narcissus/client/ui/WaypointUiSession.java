package xin.vanilla.narcissus.client.ui;

import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.client.ui.UiGameElement;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.client.data.Texture;

import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;

public final class WaypointUiSession extends NarcissusListUiSession {
    public interface PresentationPort extends Port {
        WaypointUiPresentation presentation();

        default boolean confirming() {
            return presentation().confirming();
        }
    }

    public WaypointUiSession(Port port, BooleanSupplier connection) {
        this(port, connection, System::nanoTime);
    }

    public WaypointUiSession(Port port, BooleanSupplier connection, LongSupplier clock) {
        super(port, connection, clock);
    }

    private boolean typed() {
        return port() instanceof PresentationPort;
    }

    @Override
    protected boolean structuredPresentation() {
        return typed();
    }

    @Override
    protected boolean selectionLocked() {
        return typed() && ((PresentationPort) port()).confirming();
    }

    @Override
    protected int rowHeight() {
        return typed() ? 36 : super.rowHeight();
    }

    @Override
    protected boolean textTools() {
        return typed();
    }

    @Override
    protected boolean iconAction(String id) {
        return typed() && (id.equals("close") || id.equals("delete"));
    }

    @Override
    protected String rowName(Entry entry) {
        return typed() ? entry.name() : super.rowName(entry);
    }

    @Override
    protected String rowClass(Entry entry) {
        return typed() && entry.key().equals(port().selected()) ? "selected" : "";
    }

    @Override
    protected void bindPresentation(UiView view) {
        if (!typed()) return;
        for (String id : new String[]{"subtitle", "selection_label", "cost_label", "confirm_title"})
            text(id, port().label(id));
        text("search_hint", port().label("search"));
        view.gameElement("brand", UiGameElement.sprite(Texture.of(new ResourceLocation("narcissus_farewell", "gui/quick_icon.png"), 64, 64)));
        view.gameElement("close_icon", icon("close"));
        view.gameElement("delete_icon", icon("delete"));
        view.tooltip("close", xin.vanilla.narcissus.NarcissusComponent.get().literal(port().label("close")));
        view.tooltip("delete", xin.vanilla.narcissus.NarcissusComponent.get().literal(port().label("delete_tip")));
    }

    private static UiGameElement icon(String name) {
        return UiGameElement.sprite(Texture.of(new ResourceLocation("apricityui", "apricity/narcissus_farewell/icons/" + name + ".png"), 16, 16));
    }

    @Override
    public boolean key(int keyCode, int scanCode, int modifiers, boolean pressed) {
        if (pressed && keyCode == 256 && presentationCurrent() && selectionLocked() && port().enabled(Action.CANCEL)) {
            port().action(Action.CANCEL);
            refresh();
            return true;
        }
        return false;
    }

    @Override
    protected void refreshPresentation(UiView view, boolean connected) {
        if (!typed()) return;
        WaypointUiPresentation p = ((PresentationPort) port()).presentation();
        text("point_name", p.name());
        text("dimension", p.dimension());
        text("dimension_id", p.dimensionId());
        text("coordinate_x", p.x());
        text("coordinate_y", p.y());
        text("coordinate_z", p.z());
        text("point_distance", p.distance());
        text("point_date", p.created());
        text("cost", connected ? p.cost() : "");
        text("cost_note", connected ? p.note() : "");
        text("empty", p.empty());
        text("footer_status", p.footer());
        text("detail", p.confirming() ? p.name() + "\n" + p.dimension() + " [" + p.dimensionId() + "] "
                + p.x() + ", " + p.y() + ", " + p.z() : "");
        text("ordering_hint", p.history() ? port().label("history_order") : "");
        view.enabled("cost_note", connected && !p.note().isEmpty());
        view.enabled("empty", !p.empty().isEmpty());
        view.enabled("confirmation", p.confirming());
        lombok.var viewport = view.bounds("content");
        boolean roomy = viewport != null && viewport.height() >= 150;
        view.enabled("selection_label", roomy);
        view.enabled("dimension_id", roomy);
        view.enabled("point_metadata", roomy);
        view.enabled("search", connected && !p.confirming());
        view.enabled("close", connected && !p.confirming());
        if (connected && !p.confirming())
            view.tooltip("close", xin.vanilla.narcissus.NarcissusComponent.get().literal(port().label("close")));
        for (int index = 0; index < port().tabs(); index++)
            view.enabled("tab" + index, connected && !p.confirming() && index != port().tab());
    }
}
