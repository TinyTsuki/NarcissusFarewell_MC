package xin.vanilla.narcissus.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.gui.widget.BaseShapeWidget;
import xin.vanilla.banira.common.util.ColorUtils;

/**
 * 地标与访问名单界面共用的轻量面板外观
 */
final class NarcissusScreenChrome {

    private static final float OUTER_RADIUS = 7.0F;
    private static final float INNER_RADIUS = 3.5F;
    private static final int OUTER_MARGIN = 16;
    private static final int MIN_OUTER_WIDTH = 320;
    private static final int MAX_OUTER_WIDTH = 340;
    private static final int MAX_OUTER_HEIGHT = 438;

    private NarcissusScreenChrome() {
    }

    /**
     * 从当前 Banira 有效主题派生水仙辞界面色板。
     */
    static Palette palette(BaniraColorConfig semanticTheme) {
        return new Palette(
                semanticTheme.bgPrimary(), semanticTheme.bgSurface(), semanticTheme.bgSecondary(),
                semanticTheme.textPrimary(), semanticTheme.textSecondary(), semanticTheme.accent(),
                semanticTheme.accentFocused(), semanticTheme.borderFocused(), semanticTheme.border(),
                semanticTheme.error(), semanticTheme.textDisabled());
    }

    /**
     * 创建连续像素滚动视口；滚动偏移不会被取整为条目索引。
     */
    static ListViewport listViewport(int itemCount, int itemHeight, int viewportY,
                                     int viewportHeight, double requestedOffset) {
        return new ListViewport(itemCount, itemHeight, viewportY, viewportHeight, requestedOffset);
    }

    /**
     * 计算固定整数坐标，窄窗口下仍保证列表和详情区域可用。
     */
    static Layout layout(int screenWidth, int screenHeight) {
        int outerWidth = Math.min(MAX_OUTER_WIDTH, Math.max(MIN_OUTER_WIDTH, screenWidth - OUTER_MARGIN * 2));
        int outerHeight = Math.min(MAX_OUTER_HEIGHT, Math.max(260, screenHeight - OUTER_MARGIN * 2));
        outerWidth = Math.min(screenWidth, outerWidth);
        outerHeight = Math.min(screenHeight, outerHeight);
        int outerX = Math.max(0, (screenWidth - outerWidth) / 2);
        int outerY = Math.max(0, (screenHeight - outerHeight) / 2);

        Rect outer = new Rect(outerX, outerY, outerWidth, outerHeight);
        int inset = 8;
        Rect content = new Rect(outerX + inset, outerY + inset,
                outerWidth - inset * 2, outerHeight - inset * 2);
        Rect top = new Rect(content.x, content.y, content.width, 36);
        int detailHeight = Math.min(76, Math.max(62, content.height / 5));
        Rect detail = new Rect(content.x, content.y + content.height - detailHeight,
                content.width, detailHeight);
        Rect list = new Rect(content.x, top.y + top.height + 6,
                content.width, detail.y - (top.y + top.height + 6) - 6);
        return new Layout(outer, content, top, list, detail);
    }

    static void drawJournal(MatrixStack stack, Palette palette, Layout layout) {
        drawRounded(stack, layout.outer.x, layout.outer.y, layout.outer.width, layout.outer.height,
                palette.paper, OUTER_RADIUS, 0);
        drawRounded(stack, layout.outer.x, layout.outer.y, layout.outer.width, layout.outer.height,
                palette.strongLine, OUTER_RADIUS, 1);
        drawRounded(stack, layout.content.x, layout.content.y, layout.content.width, layout.content.height,
                palette.content, INNER_RADIUS, 0);
    }

    static void drawCompactTab(MatrixStack stack, Palette palette, Rect rect,
                               boolean hovered, boolean selected) {
        if (hovered) {
            int fill = ColorUtils.applyAlphaToArgb(palette.softLine, 0x30);
            drawRounded(stack, rect.x, rect.y, rect.width, rect.height, fill, INNER_RADIUS, 0);
        }
        if (selected) {
            drawRounded(stack, rect.x + 5, rect.y + rect.height - 2, rect.width - 10, 2,
                    palette.accent, 1.0F, 0);
        }
    }

    /**
     * 在顶栏内放置按内容定宽的紧凑分页。
     */
    static Rect compactTabRect(Rect top, int xOffset, int width) {
        int height = 20;
        return new Rect(top.x + xOffset, top.y + (top.height - height) / 2, width, height);
    }

    static void drawJournalListRow(MatrixStack stack, Palette palette, Rect rect,
                                   boolean enabled, boolean hovered, boolean selected) {
        int fill = palette.content;
        if (!enabled) {
            fill = ColorUtils.applyAlphaToArgb(palette.disabled, 0x28);
        } else if (selected) {
            fill = ColorUtils.applyAlphaToArgb(palette.selected, 0x38);
        } else if (hovered) {
            fill = ColorUtils.applyAlphaToArgb(palette.accent, 0x20);
        }
        drawRounded(stack, rect.x, rect.y, rect.width, rect.height, fill, INNER_RADIUS, 0);
        int border = selected ? palette.selected : hovered ? palette.accent : palette.softLine;
        drawRounded(stack, rect.x, rect.y, rect.width, rect.height, border, INNER_RADIUS, 1);
    }

    static void drawDetailSurface(MatrixStack stack, Palette palette, Rect rect) {
        drawRounded(stack, rect.x, rect.y, rect.width, rect.height,
                ColorUtils.applyAlphaToArgb(palette.sidebar, 0x58), INNER_RADIUS, 0);
        drawRounded(stack, rect.x, rect.y, rect.width, rect.height,
                palette.softLine, INNER_RADIUS, 1);
    }

    static void drawDialog(MatrixStack stack, Palette palette,
                           int x, int y, int width, int height) {
        drawRounded(stack, x, y, width, height, palette.paper, OUTER_RADIUS, 0);
        drawRounded(stack, x, y, width, height, palette.strongLine, OUTER_RADIUS, 1);
    }

    private static void drawRounded(MatrixStack stack, int x, int y, int width, int height,
                                    int color, float radius, int borderWidth) {
        if (width <= 0 || height <= 0) {
            return;
        }
        ShapeDrawArgs args = ShapeDrawArgs.rect(stack, x, y, width, height, color);
        args.rect().radius(radius).cornerMode(ShapeDrawArgs.RoundedCornerMode.FINE);
        if (borderWidth > 0) {
            args.rect().border(borderWidth);
        }
        BaseShapeWidget.drawShape(args);
    }

    static final class Palette {
        private final int paper;
        private final int content;
        private final int sidebar;
        private final int primary;
        private final int secondary;
        private final int accent;
        private final int selected;
        private final int strongLine;
        private final int softLine;
        private final int danger;
        private final int disabled;

        private Palette(int paper, int content, int sidebar, int primary, int secondary,
                        int accent, int selected, int strongLine, int softLine,
                        int danger, int disabled) {
            this.paper = paper;
            this.content = content;
            this.sidebar = sidebar;
            this.primary = primary;
            this.secondary = secondary;
            this.accent = accent;
            this.selected = selected;
            this.strongLine = strongLine;
            this.softLine = softLine;
            this.danger = danger;
            this.disabled = disabled;
        }

        int paper() { return paper; }
        int content() { return content; }
        int sidebar() { return sidebar; }
        int primary() { return primary; }
        int secondary() { return secondary; }
        int accent() { return accent; }
        int selected() { return selected; }
        int strongLine() { return strongLine; }
        int softLine() { return softLine; }
        int danger() { return danger; }
        int disabled() { return disabled; }
    }

    static final class Rect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        Rect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        int x() { return x; }
        int y() { return y; }
        int width() { return width; }
        int height() { return height; }
    }

    static final class Layout {
        private final Rect outer;
        private final Rect content;
        private final Rect top;
        private final Rect list;
        private final Rect detail;

        private Layout(Rect outer, Rect content, Rect top, Rect list, Rect detail) {
            this.outer = outer;
            this.content = content;
            this.top = top;
            this.list = list;
            this.detail = detail;
        }

        Rect outer() { return outer; }
        Rect content() { return content; }
        Rect top() { return top; }
        Rect list() { return list; }
        Rect detail() { return detail; }
    }

    static final class ListViewport {
        private final int itemCount;
        private final int itemHeight;
        private final int viewportY;
        private final int viewportHeight;
        private final double maxOffset;
        private final double offset;
        private final int firstIndex;
        private final int lastIndexExclusive;

        private ListViewport(int itemCount, int itemHeight, int viewportY,
                             int viewportHeight, double requestedOffset) {
            this.itemCount = Math.max(0, itemCount);
            this.itemHeight = Math.max(1, itemHeight);
            this.viewportY = viewportY;
            this.viewportHeight = Math.max(1, viewportHeight);
            this.maxOffset = Math.max(0.0D, this.itemCount * (double) this.itemHeight - this.viewportHeight);
            this.offset = Math.max(0.0D, Math.min(requestedOffset, this.maxOffset));
            this.firstIndex = Math.min(this.itemCount, (int) Math.floor(this.offset / this.itemHeight));
            this.lastIndexExclusive = Math.min(this.itemCount,
                    (int) Math.ceil((this.offset + this.viewportHeight) / this.itemHeight));
        }

        double offset() { return offset; }
        double maxOffset() { return maxOffset; }
        int firstIndex() { return firstIndex; }
        int lastIndexExclusive() { return lastIndexExclusive; }

        double rowY(int index) {
            return viewportY + index * (double) itemHeight - offset;
        }

        int itemIndexAt(double mouseY) {
            if (mouseY < viewportY || mouseY >= viewportY + viewportHeight) {
                return -1;
            }
            int index = (int) Math.floor((mouseY - viewportY + offset) / itemHeight);
            return index >= 0 && index < itemCount ? index : -1;
        }
    }
}
