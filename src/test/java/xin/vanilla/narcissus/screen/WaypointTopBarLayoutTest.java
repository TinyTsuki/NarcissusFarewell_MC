package xin.vanilla.narcissus.screen;

import org.junit.Test;
import static org.junit.Assert.*;

public class WaypointTopBarLayoutTest {
    @Test public void shortGuiAllocatesPositiveScrollViewportAndContainedFooter() {
        for (int height : new int[]{128, 144, 150, 160, 180, 200}) {
            NarcissusScreenChrome.Layout chrome = NarcissusScreenChrome.layout(320, height, 60);
            assertEquals(60, chrome.top().height());
            assertTrue(chrome.list().height() > 6);
            assertTrue(chrome.list().y() >= chrome.top().y() + chrome.top().height());
            assertTrue(chrome.list().y() + chrome.list().height() <= chrome.detail().y());
            assertTrue(chrome.detail().height() >= 44);
            assertTrue(chrome.detail().y() + chrome.detail().height()
                    <= chrome.content().y() + chrome.content().height());
            assertTrue(chrome.detail().y() + chrome.detail().height() <= height);
        }
    }

    @org.junit.BeforeClass
    public static void bootstrap() {
        net.minecraft.util.registry.Bootstrap.bootStrap();
    }

    private xin.vanilla.banira.client.gui.widget.ScrollbarWidget headlessScrollbar() throws Exception {
        Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
        java.lang.reflect.Field field = unsafeType.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        // This API-only fixture avoids the widget constructor's game-launcher theme lookup.
        xin.vanilla.banira.client.gui.widget.ScrollbarWidget bar =
                (xin.vanilla.banira.client.gui.widget.ScrollbarWidget) unsafeType
                        .getMethod("allocateInstance", Class.class).invoke(field.get(null),
                                xin.vanilla.banira.client.gui.widget.ScrollbarWidget.class);
        bar.scrollingCoordinates(new java.util.ArrayList<>());
        return bar;
    }

    @Test public void legacyScrollingCoordinatesCanReplaceTheHoverRegionAfterReflow() throws Exception {
        xin.vanilla.banira.client.gui.widget.ScrollbarWidget bar = headlessScrollbar();
        xin.vanilla.banira.client.data.ScreenCoordinate oldRegion =
                new xin.vanilla.banira.client.data.ScreenCoordinate(8, 42, 280, 120);
        xin.vanilla.banira.client.data.ScreenCoordinate newRegion =
                new xin.vanilla.banira.client.data.ScreenCoordinate(8, 66, 280, 96);
        bar.addScrollHoverArea(oldRegion);
        java.util.List<xin.vanilla.banira.client.data.ScreenCoordinate> oldRegions = bar.scrollingCoordinates();
        bar.scrollingCoordinates(new java.util.ArrayList<>());
        bar.addScrollHoverArea(newRegion);
        assertNotSame(oldRegions, bar.scrollingCoordinates());
        assertEquals(1, bar.scrollingCoordinates().size());
        assertSame(newRegion, bar.scrollingCoordinates().get(0));
        assertFalse(bar.scrollingCoordinates().contains(oldRegion));
    }

    private WaypointTopBarLayout layout(int width, int ticket, int... tabs) {
        return WaypointTopBarLayout.layout(width, ticket, tabs);
    }

    private boolean overlaps(WaypointTopBarLayout.Rect a, WaypointTopBarLayout.Rect b) {
        return a.x < b.x + b.width && b.x < a.x + a.width
                && a.y < b.y + b.height && b.y < a.y + a.height;
    }

    private void fits(int width, int ticket, int... tabs) {
        WaypointTopBarLayout model = layout(width, ticket, tabs);
        WaypointTopBarLayout.Rect[] rects = {model.ticket, model.tab0, model.tab1,
                model.tab2, model.search, model.plus};
        for (WaypointTopBarLayout.Rect r : rects) {
            assertTrue(r.x >= 0);
            assertTrue(r.y >= 0);
            assertTrue(r.width > 0);
            assertTrue(r.x + r.width <= width);
            assertTrue(r.y + r.height <= model.height);
        }
        for (int a = 0; a < rects.length; a++)
            for (int b = a + 1; b < rects.length; b++) assertFalse(overlaps(rects[a], rects[b]));
        assertTrue(model.search.width >= 50);
        assertEquals(14, model.plus.width);
        assertEquals(24, model.search.height);
    }

    @Test public void english854By480UsesTwoRowsWithDedicatedSearchAndPlus() throws Exception {
        fits(324, 88, 70, 66, 85);
        assertEquals(60, layout(324, 88, 70, 66, 85).height);
    }

    @Test public void shortLabelsKeepCompactSingleRow() throws Exception {
        fits(324, 38, 44, 44, 50);
        assertEquals(36, layout(324, 38, 44, 44, 50).height);
    }

    @Test public void longTranslationsAndLargeCountsFitScaledViewports() throws Exception {
        for (int width : new int[]{144, 197, 240, 304, 324, 420, 838})
            fits(width, 400, 250, 350, 500);
    }

    @Test public void measuredLabelsDetermineWidthsWhenSpaceIsAvailable() throws Exception {
        WaypointTopBarLayout model = layout(838, 90, 60, 70, 80);
        assertEquals(90, model.ticket.width);
        assertEquals(60, model.tab0.width);
        assertEquals(70, model.tab1.width);
        assertEquals(80, model.tab2.width);
    }

    @Test public void oneVeryLongTranslationDoesNotEraseOtherTabs() {
        WaypointTopBarLayout model = layout(144, 400, 44, 44, 10000);
        assertTrue(model.tab0.width >= 24);
        assertTrue(model.tab1.width >= 24);
        assertTrue(model.tab2.width >= 24);
        fits(144, 400, 44, 44, 10000);
    }

    @Test public void headerLeavesListAndDetailSeparateAtSupportedGuiSizes() {
        for (int[] size : new int[][]{{320, 240}, {427, 240}, {854, 480}, {960, 540}, {1920, 1080}}) {
            NarcissusScreenChrome.Layout chrome = NarcissusScreenChrome.layout(size[0], size[1], 60);
            assertTrue(chrome.outer().x() >= 0);
            assertTrue(chrome.outer().y() >= 0);
            assertTrue(chrome.outer().x() + chrome.outer().width() <= size[0]);
            assertTrue(chrome.outer().y() + chrome.outer().height() <= size[1]);
            assertTrue(chrome.top().y() + chrome.top().height() < chrome.list().y());
            assertTrue(chrome.list().height() > 0);
            assertTrue(chrome.list().y() + chrome.list().height() < chrome.detail().y());
            assertTrue(chrome.detail().y() + chrome.detail().height() <= size[1]);
        }
    }
}
