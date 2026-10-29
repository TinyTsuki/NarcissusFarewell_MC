package xin.vanilla.narcissus.client.ui;

import xin.vanilla.banira.api.client.ui.UiPageSession;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.client.gui.BaniraScreen;

public final class NarcissusUiPages {
    private NarcissusUiPages() {
    }

    public static UiPageSpec page(String name, BaniraScreen screen, UiPageSession session) {
        if (!name.equals("waypoints") && !name.equals("access") && !name.equals("player"))
            throw new IllegalArgumentException("Unknown Narcissus page");
        return new UiPageSpec("narcissus_farewell:" + name, "narcissus_farewell/" + name + ".html",
                session, previous -> screen, screen::getEffectiveTheme);
    }
}
