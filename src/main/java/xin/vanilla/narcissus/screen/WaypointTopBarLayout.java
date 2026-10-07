package xin.vanilla.narcissus.screen;

/** Fixed-pixel waypoint header geometry, with measured label widths supplied by the screen. */
final class WaypointTopBarLayout {
    final Rect ticket, tab0, tab1, tab2, search, plus;
    final int height;

    private WaypointTopBarLayout(Rect ticket, Rect[] tabs, Rect search, Rect plus, int height) {
        this.ticket = ticket;
        this.tab0 = tabs[0];
        this.tab1 = tabs[1];
        this.tab2 = tabs[2];
        this.search = search;
        this.plus = plus;
        this.height = height;
    }

    static WaypointTopBarLayout layout(int width, int ticketWidth, int[] tabWidths) {
        long naturalWidth = ticketWidth + 100L;
        for (int tabWidth : tabWidths) naturalWidth += tabWidth;
        boolean twoRows = naturalWidth > width;
        int ticketW = Math.min(ticketWidth, Math.max(1, width - 84));
        int offset = twoRows ? 4 : 4 + ticketW + 4;
        Rect[] tabs = new Rect[3];
        long totalTabs = (long) tabWidths[0] + tabWidths[1] + tabWidths[2];
        int tabBudget = width - 16;
        int remaining = tabBudget;
        for (int i = 0; i < 3; i++) {
            int tabW = tabWidths[i];
            if (twoRows && totalTabs > tabBudget) {
                int extraBudget = tabBudget - 72;
                tabW = i == 2 ? remaining : 24 + (int) (extraBudget * (long) (tabW - 24) / (totalTabs - 72));
                remaining -= tabW;
            }
            tabs[i] = new Rect(offset, 8, tabW, 20);
            offset += tabW + (i < 2 ? 4 : 0);
        }
        int rowY = twoRows ? 32 : 6;
        int searchX = twoRows ? 4 + ticketW + 4 : offset + 8;
        return new WaypointTopBarLayout(new Rect(4, rowY, ticketW, 24), tabs,
                new Rect(searchX, rowY, width - 26 - searchX, 24),
                new Rect(width - 18, rowY + 5, 14, 14), twoRows ? 60 : 36);
    }

    Rect tab(int index) {
        return index == 0 ? tab0 : index == 1 ? tab1 : tab2;
    }

    static final class Rect {
        final int x, y, width, height;

        Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
