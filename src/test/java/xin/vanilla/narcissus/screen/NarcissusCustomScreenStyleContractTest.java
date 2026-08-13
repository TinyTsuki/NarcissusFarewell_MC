package xin.vanilla.narcissus.screen;

import org.junit.Test;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.narcissus.config.ClientConfig;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 约束地标与访问名单界面的手帐色板、几何边界和配置表面。
 */
public class NarcissusCustomScreenStyleContractTest {

    @Test
    public void journalPaletteUsesTheEffectiveBaniraTheme() throws Exception {
        BaniraColorConfig theme = BaniraColorConfig.spring();
        Object palette = palette(theme);

        assertPalette(palette,
                theme.bgPrimary(), theme.bgSurface(), theme.bgSecondary(),
                theme.textPrimary(), theme.textSecondary(), theme.accent(),
                theme.accentFocused(), theme.borderFocused(), theme.border());
    }

    @Test
    public void journalLayoutKeepsEveryRegionInsideTheScreen() throws Exception {
        assertLayout(854, 480);
        assertLayout(520, 320);
        assertLayout(427, 240);
        Object wide = requiredMethod(NarcissusScreenChrome.class, "layout", int.class, int.class)
                .invoke(null, 1920, 1080);
        Object outer = requiredMethod(wide.getClass(), "outer").invoke(wide);
        assertEquals(340, intValue(outer, "width"));
        Object scaledWindow = requiredMethod(NarcissusScreenChrome.class, "layout", int.class, int.class)
                .invoke(null, 427, 240);
        Object scaledOuter = requiredMethod(scaledWindow.getClass(), "outer").invoke(scaledWindow);
        assertEquals(340, intValue(scaledOuter, "width"));
    }

    @Test
    public void pixelListViewportPreservesPartialRowsAndHitTesting() throws Exception {
        Object viewport = requiredMethod(NarcissusScreenChrome.class,
                "listViewport", int.class, int.class, int.class, int.class, double.class)
                .invoke(null, 10, 28, 100, 100, 13.5D);

        assertEquals(13.5D, doubleValue(viewport, "offset"), 0.001D);
        assertEquals(180.0D, doubleValue(viewport, "maxOffset"), 0.001D);
        assertEquals(0, intValue(viewport, "firstIndex"));
        assertEquals(5, intValue(viewport, "lastIndexExclusive"));
        assertEquals(86.5D, doubleValue(viewport, "rowY", 0), 0.001D);
        assertEquals(0, intValue(viewport, "itemIndexAt", 100.0D));
        assertEquals(1, intValue(viewport, "itemIndexAt", 115.0D));
        assertEquals(-1, intValue(viewport, "itemIndexAt", 99.0D));
    }

    @Test
    public void journalUsesFullWidthContentAndCompactHeaderTabs() throws Exception {
        Object layout = requiredMethod(NarcissusScreenChrome.class, "layout", int.class, int.class)
                .invoke(null, 854, 480);
        Object outer = requiredMethod(layout.getClass(), "outer").invoke(layout);
        Object content = requiredMethod(layout.getClass(), "content").invoke(layout);
        Object top = requiredMethod(layout.getClass(), "top").invoke(layout);
        Object list = requiredMethod(layout.getClass(), "list").invoke(layout);
        Object first = requiredMethod(NarcissusScreenChrome.class,
                "compactTabRect", top.getClass(), int.class, int.class).invoke(null, top, 100, 54);
        Object second = requiredMethod(NarcissusScreenChrome.class,
                "compactTabRect", top.getClass(), int.class, int.class).invoke(null, top, 158, 72);

        assertEquals(intValue(outer, "x") + 8, intValue(content, "x"));
        assertEquals(intValue(outer, "width") - 16, intValue(content, "width"));
        assertEquals(20, intValue(first, "height"));
        assertEquals(54, intValue(first, "width"));
        assertEquals(72, intValue(second, "width"));
        assertEquals(intValue(first, "y"), intValue(second, "y"));
        assertEquals(intValue(top, "x") + 100, intValue(first, "x"));
        assertEquals(bottom(top) + 6, intValue(list, "y"));
        assertTrue(!hasMethod(layout.getClass(), "tabs"));
        assertTrue(!hasMethod(NarcissusScreenChrome.class, "topTabRect"));
        assertTrue(!hasMethod(NarcissusScreenChrome.class, "drawTopTab"));
        assertTrue(!hasMethod(layout.getClass(), "sidebar"));
        assertTrue(!hasMethod(NarcissusScreenChrome.class, "sidebarItemRect"));
    }

    @Test
    public void waypointTooltipDoesNotContainOperationInstructions() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get(
                "src/main/java/xin/vanilla/narcissus/screen/WaypointScreen.java")), StandardCharsets.UTF_8);
        String zh = new String(Files.readAllBytes(Paths.get(
                "src/main/resources/assets/narcissus_farewell/lang/zh_cn.json")), StandardCharsets.UTF_8);
        String en = new String(Files.readAllBytes(Paths.get(
                "src/main/resources/assets/narcissus_farewell/lang/en_us.json")), StandardCharsets.UTF_8);

        assertTrue(!source.contains("waypoint_row_hint"));
        assertTrue(!zh.contains("waypoint_row_hint"));
        assertTrue(!en.contains("waypoint_row_hint"));
    }

    @Test
    public void waypointScreenUsesOneActiveListAndOneSelectedDetail() {
        assertTrue(hasField(WaypointScreen.class, "activeScrollbar"));
        assertFalseField(WaypointScreen.class, "panelMode");
        assertFalseField(WaypointScreen.class, "homeScrollbar");
        assertFalseField(WaypointScreen.class, "stageScrollbar");
        assertFalseField(WaypointScreen.class, "backScrollbar");
        assertFalseField(WaypointScreen.class, "tabListScrollbar");
        assertTrue(hasMethod(WaypointScreen.class, "drawWaypointTabs"));
        assertTrue(!hasMethod(WaypointScreen.class, "drawWaypointSidebar"));
        assertTrue(hasMethod(WaypointScreen.class, "drawActiveList"));
        assertTrue(hasMethod(WaypointScreen.class, "drawSelectedDetail"));
    }

    @Test
    public void accessListScreenUsesOneActiveListWithoutPersistentHelpFooter() {
        assertTrue(hasField(AccessListScreen.class, "activeScrollbar"));
        assertFalseField(AccessListScreen.class, "panelMode");
        assertFalseField(AccessListScreen.class, "blackScrollbar");
        assertFalseField(AccessListScreen.class, "whiteScrollbar");
        assertFalseField(AccessListScreen.class, "tabListScrollbar");
        assertTrue(hasMethod(AccessListScreen.class, "drawAccessTabs"));
        assertTrue(!hasMethod(AccessListScreen.class, "drawAccessSidebar"));
        assertTrue(hasMethod(AccessListScreen.class, "drawActiveList"));
        assertTrue(hasMethod(AccessListScreen.class, "drawAccessTooltip"));
        assertTrue(!hasMethod(AccessListScreen.class, "drawFooterHint"));
    }

    @Test
    public void obsoletePanelModesAreNotPartOfClientConfig() {
        assertTrue(!hasMethod(ClientConfig.ClientView.class, "waypointScreenPanelMode"));
        assertTrue(!hasMethod(ClientConfig.ClientView.class, "accessListScreenPanelMode"));
        assertTrue(Arrays.stream(ClientConfig.ClientRootCategory.class.getDeclaredFields())
                .noneMatch(field -> field.getName().endsWith("ScreenPanelMode")));
    }

    private static Object palette(BaniraColorConfig theme) throws Exception {
        Method method = requiredMethod(NarcissusScreenChrome.class,
                "palette", BaniraColorConfig.class);
        return method.invoke(null, theme);
    }

    private static void assertPalette(Object palette, int... expected) throws Exception {
        String[] names = {
                "paper", "content", "sidebar", "primary", "secondary",
                "accent", "selected", "strongLine", "softLine"
        };
        assertEquals(names.length, expected.length);
        for (int i = 0; i < names.length; i++) {
            Method getter = requiredMethod(palette.getClass(), names[i]);
            assertEquals(names[i], expected[i], ((Number) getter.invoke(palette)).intValue());
        }
    }

    private static void assertLayout(int width, int height) throws Exception {
        Object layout = requiredMethod(NarcissusScreenChrome.class, "layout", int.class, int.class)
                .invoke(null, width, height);
        Object outer = requiredMethod(layout.getClass(), "outer").invoke(layout);
        Object content = requiredMethod(layout.getClass(), "content").invoke(layout);
        Object top = requiredMethod(layout.getClass(), "top").invoke(layout);
        Object list = requiredMethod(layout.getClass(), "list").invoke(layout);
        Object detail = requiredMethod(layout.getClass(), "detail").invoke(layout);

        assertInside(outer, width, height);
        assertInside(content, width, height);
        assertInside(top, width, height);
        assertInside(list, width, height);
        assertInside(detail, width, height);
        assertTrue("list must have room for rows", intValue(list, "height") >= 72);
        assertTrue("detail must sit below list", intValue(detail, "y") >= bottom(list));
        assertTrue("header must sit above list", bottom(top) <= intValue(list, "y"));
    }

    private static void assertInside(Object rect, int width, int height) throws Exception {
        assertTrue(intValue(rect, "x") >= 0);
        assertTrue(intValue(rect, "y") >= 0);
        assertTrue(intValue(rect, "width") > 0);
        assertTrue(intValue(rect, "height") > 0);
        assertTrue(right(rect) <= width);
        assertTrue(bottom(rect) <= height);
    }

    private static int right(Object rect) throws Exception {
        return intValue(rect, "x") + intValue(rect, "width");
    }

    private static int bottom(Object rect) throws Exception {
        return intValue(rect, "y") + intValue(rect, "height");
    }

    private static int intValue(Object target, String method) throws Exception {
        return ((Number) requiredMethod(target.getClass(), method).invoke(target)).intValue();
    }

    private static int intValue(Object target, String method, double value) throws Exception {
        return ((Number) requiredMethod(target.getClass(), method, double.class).invoke(target, value)).intValue();
    }

    private static double doubleValue(Object target, String method) throws Exception {
        return ((Number) requiredMethod(target.getClass(), method).invoke(target)).doubleValue();
    }

    private static double doubleValue(Object target, String method, int value) throws Exception {
        return ((Number) requiredMethod(target.getClass(), method, int.class).invoke(target, value)).doubleValue();
    }

    private static boolean hasField(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredFields()).anyMatch(field -> field.getName().equals(name));
    }

    private static void assertFalseField(Class<?> type, String name) {
        assertTrue(type.getSimpleName() + " must not declare " + name, !hasField(type, name));
    }

    private static boolean hasMethod(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredMethods()).anyMatch(method -> method.getName().equals(name));
    }

    private static Method requiredMethod(Class<?> type, String name, Class<?>... parameterTypes) {
        try {
            Method method = type.getDeclaredMethod(name, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException error) {
            fail(type.getSimpleName() + " must declare " + name);
            throw new AssertionError(error);
        }
    }
}
