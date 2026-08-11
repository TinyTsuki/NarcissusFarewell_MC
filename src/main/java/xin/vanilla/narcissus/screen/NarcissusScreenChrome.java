package xin.vanilla.narcissus.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.FontRenderer;
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

    private NarcissusScreenChrome() {
    }

    static void drawOuterSurface(MatrixStack stack, BaniraColorConfig theme,
                                 int x, int y, int width, int height) {
        drawRounded(stack, x + 2, y + 3, width, height,
                ColorUtils.applyAlphaToArgb(theme.bgQuaternary(), 0x55), OUTER_RADIUS, 0);
        drawRounded(stack, x, y, width, height, theme.panelBg(), OUTER_RADIUS, 0);
        drawRounded(stack, x, y, width, height,
                ColorUtils.applyAlphaToArgb(theme.border(), 0xB8), OUTER_RADIUS, 1);
    }

    static void drawTopBar(MatrixStack stack, BaniraColorConfig theme,
                           int x, int y, int width, int height) {
        drawRounded(stack, x + 1, y + 1, width - 2, height,
                ColorUtils.applyAlphaToArgb(theme.bgSecondary(), 0x72),
                OUTER_RADIUS - 1.0F, 0);
    }

    static void drawSegmentHeader(MatrixStack stack, BaniraColorConfig theme,
                                  int x, int y, int width, int height) {
        drawRounded(stack, x, y, width, height,
                ColorUtils.applyAlphaToArgb(theme.bgSecondary(), 0x48), INNER_RADIUS, 0);
    }

    static void drawListSurface(MatrixStack stack, BaniraColorConfig theme,
                                int x, int y, int width, int height) {
        drawRounded(stack, x, y, width, height,
                ColorUtils.applyAlphaToArgb(theme.bgSurface(), 0x42), INNER_RADIUS, 0);
    }

    static void drawListRow(MatrixStack stack, BaniraColorConfig theme,
                            int x, int y, int width, int height,
                            boolean enabled, boolean hovered, boolean selected) {
        int fill;
        if (!enabled) {
            fill = ColorUtils.applyAlphaToArgb(theme.bgDisabled(), selected ? 0x58 : 0x34);
        } else if (selected) {
            fill = ColorUtils.applyAlphaToArgb(theme.accent(), 0x32);
        } else if (hovered) {
            fill = ColorUtils.applyAlphaToArgb(theme.bgSecondary(), 0x5A);
        } else {
            fill = ColorUtils.applyAlphaToArgb(theme.bgSecondary(), 0x22);
        }
        drawRounded(stack, x, y, width, height, fill, INNER_RADIUS, 0);

        if (selected) {
            drawRounded(stack, x, y, width, height,
                    enabled ? theme.accentFocused() : theme.textDisabled(), INNER_RADIUS, 1);
        } else if (hovered) {
            drawRounded(stack, x, y, width, height,
                    ColorUtils.applyAlphaToArgb(theme.borderHover(), 0x82), INNER_RADIUS, 1);
        }
    }

    static void drawEmptyState(MatrixStack stack, FontRenderer font, BaniraColorConfig theme,
                               String text, int x, int y, int width, int height) {
        int lineY = y + height / 2 - font.lineHeight / 2;
        int textWidth = Math.min(width - 16, font.width(text));
        String shown = font.plainSubstrByWidth(text, Math.max(8, textWidth));
        int textX = x + (width - font.width(shown)) / 2;
        font.draw(stack, shown, textX, lineY, theme.textHint());
    }

    static void drawFooterSurface(MatrixStack stack, BaniraColorConfig theme,
                                  int x, int y, int width, int height) {
        drawRounded(stack, x + 1, y, width - 2, Math.max(1, height - 1),
                ColorUtils.applyAlphaToArgb(theme.bgSecondary(), 0x38),
                OUTER_RADIUS - 1.0F, 0);
    }

    static void drawDialog(MatrixStack stack, BaniraColorConfig theme,
                           int x, int y, int width, int height) {
        drawRounded(stack, x + 2, y + 3, width, height,
                ColorUtils.applyAlphaToArgb(theme.bgQuaternary(), 0x66), OUTER_RADIUS, 0);
        drawRounded(stack, x, y, width, height, theme.panelBg(), OUTER_RADIUS, 0);
        drawRounded(stack, x, y, width, height, theme.borderHover(), OUTER_RADIUS, 1);
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
}
