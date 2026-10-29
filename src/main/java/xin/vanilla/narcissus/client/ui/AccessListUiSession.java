package xin.vanilla.narcissus.client.ui;

import net.minecraft.resources.ResourceLocation;
import xin.vanilla.banira.api.client.ui.UiGameElement;
import xin.vanilla.banira.api.client.ui.UiView;
import xin.vanilla.banira.client.data.Texture;
import xin.vanilla.narcissus.NarcissusComponent;

import java.util.function.BooleanSupplier;

public final class AccessListUiSession extends NarcissusListUiSession {
    public interface PresentationPort extends Port {
        AccessUiPresentation presentation();

        default boolean confirming() {
            return presentation().confirming();
        }

        UiGameElement profileIcon(String key);

        String modeLabel(String key);
    }

    public AccessListUiSession(Port port, BooleanSupplier connection) {
        super(port, connection, System::nanoTime);
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
    protected void bindRow(UiView row, Entry entry) {
        if (!typed()) return;
        PresentationPort p = (PresentationPort) port();
        row.gameElement("icon", p.profileIcon(entry.key()));
        row.text("mode", NarcissusComponent.get().literal(p.modeLabel(entry.key())));
    }

    @Override
    protected void bindPresentation(UiView view) {
        if (!typed()) return;
        for (String id : new String[]{"subtitle", "scope_label", "mode_label", "confirm_title"})
            text(id, port().label(id));
        text("search_hint", port().label("search"));
        view.gameElement("brand", UiGameElement.sprite(Texture.of(ResourceLocation.fromNamespaceAndPath("narcissus_farewell", "gui/quick_icon.png"), 64, 64)));
        view.gameElement("delete_icon", UiGameElement.sprite(Texture.of(ResourceLocation.fromNamespaceAndPath("apricityui", "apricity/narcissus_farewell/icons/delete.png"), 16, 16)));
        view.tooltip("close", NarcissusComponent.get().literal(port().label("close")));
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
        if (!connected) {
            view.enabled("confirmation", false);
            view.enabled("search", false);
            view.enabled("close", false);
            for (int i = 0; i < port().tabs(); i++) view.enabled("tab" + i, false);
            return;
        }
        AccessUiPresentation p = ((PresentationPort) port()).presentation();
        text("scope_label", port().label(p.selected() ? "player_label" : "scope_label"));
        text("player_name", p.name());
        text("player_uuid", p.uuid());
        text("mode", p.mode());
        text("scope", p.scope());
        text("tpa_result", p.tpa());
        text("tph_result", p.tph());
        text("empty", p.empty());
        text("footer_status", p.footer());
        text("detail", p.confirming() ? port().detail() : "");
        view.enabled("identity", p.selected());
        view.enabled("mode_label", p.selected() && p.whitelist());
        view.enabled("mode", !p.mode().isEmpty());
        view.enabled("empty", !p.empty().isEmpty());
        view.enabled("confirmation", p.confirming());
        view.enabled("search", connected && !p.confirming());
        view.enabled("close", connected && !p.confirming());
        if (connected && !p.confirming())
            view.tooltip("close", NarcissusComponent.get().literal(port().label("close")));
        for (int i = 0; i < port().tabs(); i++)
            view.enabled("tab" + i, connected && !p.confirming() && i != port().tab());
    }
}
