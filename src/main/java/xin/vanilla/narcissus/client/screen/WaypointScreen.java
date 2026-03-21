package xin.vanilla.narcissus.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.FontDrawArgs;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.data.ShapeDrawArgs;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.common.util.NumberUtils;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.client.ClientStageData;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.network.ModNetworkHandler;
import xin.vanilla.narcissus.network.packet.WaypointDelToServer;
import xin.vanilla.narcissus.network.packet.WaypointTeleportToServer;
import xin.vanilla.narcissus.util.ClientCostCalculator;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

@Accessors(chain = true, fluent = true)
public class WaypointScreen extends BaniraScreen {

    // region Constants

    private static final int SCREEN_MARGIN = 8;
    private static final int GAP_H = 4;
    private static final int PANEL_PADDING = 6;
    private static final int TITLE_HEIGHT = 14;
    private static final int ITEM_HEIGHT = 24;
    private static final int MAX_VISIBLE_ITEMS = 6;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 2;
    private static final int LIST_PADDING_V = 1;
    private static final int FOOTER_HEIGHT = 48;
    private static final int FOOTER_PAD_H = 10;
    private static final int FOOTER_PAD_V = 8;
    private static final int GAP_V_FOOTER = 8;
    private static final int BUTTON_HEIGHT = 20;
    private static final int DIALOG_W = 280;
    private static final int DIALOG_H = 100;

    // endregion Constants

    // region Data

    private final SafeWorldCoordinate lastPlayerPos = new SafeWorldCoordinate();
    private long lastUpdateTime = 0;

    private final List<WaypointEntry> homeItems = new ArrayList<>();
    private final List<WaypointEntry> stageItems = new ArrayList<>();
    private final List<WaypointEntry> backItems = new ArrayList<>();

    private WaypointEntry selectedItem;
    private WaypointEntry lastSelectedItem;

    private int ticketCount;
    private int panelWidth;
    private int panelHeight;
    private int listHeight;
    private int contentW;
    private int startX;
    private int startY;
    private int footerY;
    private int buttonX;
    private int buttonY;
    private int buttonW;
    private int footerX;
    private int footerW;
    private int dlgX;
    private int dlgY;

    private WaypointEntry deleteConfirmItem;

    private WaypointEntry hoveredItem;

    private ScrollbarWidget homeScrollbar;
    private ScrollbarWidget stageScrollbar;
    private ScrollbarWidget backScrollbar;
    private ButtonWidget teleportButton;
    private ButtonWidget deleteCancelButton;
    private ButtonWidget deleteConfirmButton;

    // endregion Data

    public WaypointScreen() {
        super(Component.literal("WaypointScreen"));
        previousScreen(Minecraft.getInstance().screen);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    protected void onInit() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        loadData();
    }

    @Override
    protected void initWidgets() {
        int availWidth = width - 2 * SCREEN_MARGIN - 2 * GAP_H;
        panelWidth = availWidth / 3;
        listHeight = ITEM_HEIGHT * MAX_VISIBLE_ITEMS;
        panelHeight = TITLE_HEIGHT + 2 * LIST_PADDING_V + listHeight;
        contentW = panelWidth - 2 * PANEL_PADDING - SCROLLBAR_WIDTH - SCROLLBAR_GAP;

        startX = SCREEN_MARGIN;
        startY = 20;
        footerY = startY + panelHeight + GAP_V_FOOTER;
        footerX = SCREEN_MARGIN;
        footerW = width - 2 * SCREEN_MARGIN;

        buttonW = 100;
        buttonX = (width - buttonW) / 2;
        buttonY = footerY + (FOOTER_HEIGHT - BUTTON_HEIGHT) / 2;

        dlgX = (width - DIALOG_W) / 2;
        dlgY = (height - DIALOG_H) / 2;

        int cancelW = Math.max(72, font.width(Component.transClientAuto(NarcissusFarewell.MODID, "cancel").toString()) + 20);
        int deleteW = Math.max(72, font.width(Component.transClientAuto(NarcissusFarewell.MODID, "delete").toString()) + 20);
        int btnY = dlgY + DIALOG_H - 30;
        int cancelX = dlgX + (DIALOG_W - cancelW - deleteW - 10) / 2;
        int deleteX = cancelX + cancelW + 10;

        homeScrollbar = buildColumnScrollbar(0, homeItems);
        stageScrollbar = buildColumnScrollbar(1, stageItems);
        backScrollbar = buildColumnScrollbar(2, backItems);

        teleportButton = new ButtonWidget(this);
        teleportButton.id("teleport");
        teleportButton.bounds(new ScreenCoordinate(buttonX, buttonY, buttonW, BUTTON_HEIGHT));
        teleportButton.text(Component.transClientAuto(NarcissusFarewell.MODID, "teleport_btn"));
        teleportButton.radius(4);
        teleportButton.onClick(b -> {
            if (selectedItem != null && selectedItem.canTeleport) {
                onTeleportClick();
            }
        });
        addWidget(teleportButton);

        deleteCancelButton = new ButtonWidget(this);
        deleteCancelButton.id("delete_cancel");
        deleteCancelButton.bounds(new ScreenCoordinate(cancelX, btnY, cancelW, 20));
        deleteCancelButton.text(Component.transClientAuto(NarcissusFarewell.MODID, "cancel"));
        deleteCancelButton.radius(4);
        deleteCancelButton.visible(false);
        deleteCancelButton.onClick(b -> deleteConfirmItem = null);
        addWidget(deleteCancelButton);

        deleteConfirmButton = new ButtonWidget(this);
        deleteConfirmButton.id("delete_confirm");
        deleteConfirmButton.bounds(new ScreenCoordinate(deleteX, btnY, deleteW, 20));
        deleteConfirmButton.text(Component.transClientAuto(NarcissusFarewell.MODID, "delete"));
        deleteConfirmButton.radius(4);
        deleteConfirmButton.visible(false);
        deleteConfirmButton.onClick(b -> {
            if (deleteConfirmItem != null) {
                doActualDelete(deleteConfirmItem);
                deleteConfirmItem = null;
            }
        });
        addWidget(deleteConfirmButton);
    }

    private ScrollbarWidget buildColumnScrollbar(int columnIndex, List<WaypointEntry> items) {
        int px = startX + columnIndex * (panelWidth + GAP_H);
        int listX = px + PANEL_PADDING;
        int listY = startY + TITLE_HEIGHT + LIST_PADDING_V;
        int maxScroll = Math.max(0, items.size() - MAX_VISIBLE_ITEMS);

        ScrollbarWidget bar = new ScrollbarWidget(this);
        bar.id("scrollbar_" + columnIndex);
        bar.bounds(new ScreenCoordinate(listX + contentW + SCROLLBAR_GAP, listY, SCROLLBAR_WIDTH, listHeight));
        bar.orientation(EnumOrientation.VERTICAL);
        bar.minValue(0);
        bar.maxValue(maxScroll);
        bar.visibleSize(MAX_VISIBLE_ITEMS);
        bar.scrollStep(1.0);
        bar.addScrollHoverArea(new ScreenCoordinate(listX, listY, contentW, listHeight));
        addWidget(bar);
        return bar;
    }

    private void syncScrollbarLimits() {
        syncOneScrollbar(homeScrollbar, homeItems);
        syncOneScrollbar(stageScrollbar, stageItems);
        syncOneScrollbar(backScrollbar, backItems);
    }

    private void syncOneScrollbar(ScrollbarWidget bar, List<WaypointEntry> items) {
        if (bar == null) {
            return;
        }
        double v = bar.value();
        double maxScroll = Math.max(0, items.size() - MAX_VISIBLE_ITEMS);
        bar.maxValue(maxScroll);
        bar.value(Math.min(v, maxScroll));
    }

    @Override
    protected void onRender(@Nonnull MatrixStack stack, float partialTicks) {
        renderBackground(stack);

        if (minecraft != null && minecraft.player != null) {
            PlayerTeleportData data = PlayerTeleportData.getData(minecraft.player);
            ticketCount = data.getTeleportCard();
        }

        if (selectedItem != null) {
            long now = System.currentTimeMillis();
            boolean needUpdate = (now - lastUpdateTime > 500 && selectedItem != lastSelectedItem) || isPlayerMoved();
            if (needUpdate) {
                lastUpdateTime = now;
                lastSelectedItem = selectedItem;
            }
        }

        BaniraColorConfig theme = getEffectiveTheme();

        int mouseX = (int) inputState.mouseX();
        int mouseY = (int) inputState.mouseY();

        if (deleteConfirmItem == null) {
            updateHoveredItem(mouseX, mouseY);
        } else {
            hoveredItem = null;
        }

        boolean dialogOpen = deleteConfirmItem != null;
        if (homeScrollbar != null) {
            homeScrollbar.enabled(!dialogOpen);
            stageScrollbar.enabled(!dialogOpen);
            backScrollbar.enabled(!dialogOpen);
        }
        if (teleportButton != null) {
            teleportButton.visible(!dialogOpen);
            teleportButton.enabled(selectedItem != null && selectedItem.canTeleport);
        }
        if (deleteCancelButton != null) {
            deleteCancelButton.visible(dialogOpen);
            deleteConfirmButton.visible(dialogOpen);
        }

        drawPanel(stack, theme, 0, Component.transClientAuto(NarcissusFarewell.MODID, "private").toString(), homeItems, homeScrollbar, false, mouseX, mouseY);
        drawPanel(stack, theme, 1, Component.transClientAuto(NarcissusFarewell.MODID, "public").toString(), stageItems, stageScrollbar, false, mouseX, mouseY);
        drawPanel(stack, theme, 2, Component.transClientAuto(NarcissusFarewell.MODID, "footprints").toString(), backItems, backScrollbar, true, mouseX, mouseY);

        drawFooter(stack, theme);

        String ticketStr = Component.transClientAuto(NarcissusFarewell.MODID, "teleport_card").toString() + ": " + ticketCount;
        int tw = font.width(ticketStr);
        font.draw(stack, ticketStr, width - tw - 10, 10, theme.textPrimary());

        if (dialogOpen) {
            drawDeleteConfirmOverlay(stack, theme);
        }

        if (hoveredItem != null && !dialogOpen) {
            addDeferredTooltipRender(s -> drawCustomTooltip(s, theme, hoveredItem, mouseX, mouseY));
        }

        renderWidgets(stack, partialTicks);
    }

    @Override
    public void onMouseClicked(MouseClickedHandleArgs eventArgs) {
        if (deleteConfirmItem != null && eventArgs.button() == 0 && !eventArgs.consumed()) {
            eventArgs.consumed(true);
            return;
        }

        if (eventArgs.button() != 0 || eventArgs.consumed()) {
            super.onMouseClicked(eventArgs);
            return;
        }

        double mouseX = eventArgs.mouseX();
        double mouseY = eventArgs.mouseY();
        int listY = startY + TITLE_HEIGHT + LIST_PADDING_V;

        if (checkPanelClick(mouseX, mouseY, startX + PANEL_PADDING, listY, contentW, homeItems, homeScrollbar)) {
            eventArgs.consumed(true);
        } else if (checkPanelClick(mouseX, mouseY, startX + panelWidth + GAP_H + PANEL_PADDING, listY, contentW, stageItems, stageScrollbar)) {
            eventArgs.consumed(true);
        } else if (checkPanelClick(mouseX, mouseY, startX + (panelWidth + GAP_H) * 2 + PANEL_PADDING, listY, contentW, backItems, backScrollbar)) {
            eventArgs.consumed(true);
        }

        super.onMouseClicked(eventArgs);
    }

    private void loadData() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        PlayerTeleportData data = PlayerTeleportData.getData(minecraft.player);

        homeItems.clear();
        for (KeyValue<String, String> key : data.getHomeCoordinate().keySet()) {
            homeItems.add(new WaypointEntry(WaypointEntry.Type.HOME, key.value(),
                    data.getHomeCoordinate().get(key), true, null));
        }

        stageItems.clear();
        for (Map.Entry<KeyValue<String, String>, SafeWorldCoordinate> entry : ClientStageData.getStageCoordinate().entrySet()) {
            stageItems.add(new WaypointEntry(WaypointEntry.Type.STAGE, entry.getKey().value(),
                    entry.getValue(), true, null));
        }

        backItems.clear();
        List<TeleportRecord> records = data.getTeleportRecords().stream()
                .filter(r -> r.getBefore() != null)
                .collect(Collectors.toList());
        Set<String> seenRecordTypes = new HashSet<>();
        for (int i = records.size() - 1; i >= 0; i--) {
            TeleportRecord record = records.get(i);
            String recordTypeName = record.getTeleportType().name();
            boolean canTp = !seenRecordTypes.contains(recordTypeName);
            if (canTp) {
                seenRecordTypes.add(recordTypeName);
            }
            backItems.add(new WaypointEntry(WaypointEntry.Type.BACK, recordTypeName, record.getBefore(),
                    canTp, recordTypeName));
        }

        WaypointEntry first = null;
        if (!homeItems.isEmpty()) {
            first = homeItems.get(0);
        } else if (!stageItems.isEmpty()) {
            first = stageItems.get(0);
        } else if (!backItems.isEmpty()) {
            first = backItems.get(0);
        }
        if (first != null) {
            selectedItem = first;
        }

        ticketCount = data.getTeleportCard();
    }

    private boolean checkPanelClick(double mouseX, double mouseY, int listX, int listY, int cw, List<WaypointEntry> items, ScrollbarWidget bar) {
        int scroll = bar != null ? (int) Math.round(MathHelper.clamp(bar.value(), 0, Math.max(0, items.size() - MAX_VISIBLE_ITEMS))) : 0;

        for (int i = 0; i < MAX_VISIBLE_ITEMS; i++) {
            int idx = i + scroll;
            if (idx >= items.size()) {
                break;
            }

            WaypointEntry item = items.get(idx);
            int itemY = listY + i * ITEM_HEIGHT;

            if (mouseX >= listX && mouseX < listX + cw && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT - 1) {
                if ((item.type == WaypointEntry.Type.HOME || item.type == WaypointEntry.Type.STAGE) && item.canTeleport) {
                    int delX = listX + cw - 16;
                    if (mouseX >= delX) {
                        deleteConfirmItem = item;
                        return true;
                    }
                }
                selectedItem = item;
                return true;
            }
        }
        return false;
    }

    private void updateHoveredItem(int mouseX, int mouseY) {
        hoveredItem = null;
        int listY = startY + TITLE_HEIGHT + LIST_PADDING_V;

        for (int p = 0; p < 3; p++) {
            int listX = startX + p * (panelWidth + GAP_H) + PANEL_PADDING;
            List<WaypointEntry> items = p == 0 ? homeItems : (p == 1 ? stageItems : backItems);
            ScrollbarWidget bar = p == 0 ? homeScrollbar : (p == 1 ? stageScrollbar : backScrollbar);
            int scroll = bar != null ? (int) Math.round(MathHelper.clamp(bar.value(), 0, Math.max(0, items.size() - MAX_VISIBLE_ITEMS))) : 0;

            for (int i = 0; i < MAX_VISIBLE_ITEMS; i++) {
                int idx = i + scroll;
                if (idx >= items.size()) {
                    break;
                }
                int itemY = listY + i * ITEM_HEIGHT;
                if (mouseX >= listX && mouseX < listX + contentW && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
                    hoveredItem = items.get(idx);
                    return;
                }
            }
        }
    }

    private void drawCustomTooltip(MatrixStack stack, BaniraColorConfig theme, WaypointEntry item, int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        lines.add(item.name);
        lines.add(item.getDetailTypeName() + " | " + item.getDimensionName());
        lines.add(item.getCoordinateName());
        if (!item.canTeleport) {
            lines.add(Component.transClientAuto(NarcissusFarewell.MODID, "back_record_used").toString());
        }
        String content = String.join("\n", lines);
        Text tooltipText = Text.literal(content).stack(stack).font(font).color(Color.argb(theme.textPrimary()));
        FontDrawArgs args = FontDrawArgs.of(tooltipText)
                .x(mouseX).y(mouseY)
                .marginTop(2).marginBottom(2).marginLeft(2).marginRight(2)
                .bgArgb(ColorUtils.applyAlphaToArgb(theme.bgSurface(), 0xF0))
                .bgBorderRadius(4).bgBorderThickness(1)
                .inScreen(true);
        TooltipWidget.drawPopupMessage(stack, args);
    }

    private void drawPanel(MatrixStack stack, BaniraColorConfig theme, int columnIndex, String title, List<WaypointEntry> items,
                           ScrollbarWidget scrollbar, boolean isBackPanel, int mouseX, int mouseY) {
        int x = startX + columnIndex * (panelWidth + GAP_H);
        int y = startY;

        ShapeDrawArgs panelShape = ShapeDrawArgs.rect(stack, x, y, panelWidth, panelHeight, theme.panelBg());
        panelShape.rect().radius(6);
        BaseShapeWidget.drawShape(panelShape);

        ShapeDrawArgs titleLine = ShapeDrawArgs.rect(stack, x + 2, y + TITLE_HEIGHT - 1, panelWidth - 4, 1, theme.border());
        BaseShapeWidget.drawShape(titleLine);

        font.draw(stack, title, x + PANEL_PADDING, y + 3, theme.textPrimary());

        int listY = y + TITLE_HEIGHT + LIST_PADDING_V;
        int listX = x + PANEL_PADDING;
        int scroll = scrollbar != null
                ? (int) Math.round(MathHelper.clamp(scrollbar.value(), 0, Math.max(0, items.size() - MAX_VISIBLE_ITEMS)))
                : 0;

        for (int i = 0; i < MAX_VISIBLE_ITEMS; i++) {
            int idx = i + scroll;
            if (idx >= items.size()) {
                break;
            }

            WaypointEntry item = items.get(idx);
            int itemY = listY + i * ITEM_HEIGHT;
            boolean hover = mouseX >= listX && mouseX < listX + contentW && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
            boolean selected = item == selectedItem;

            int rowBg;
            if (!item.canTeleport) {
                rowBg = isBackPanel
                        ? ColorUtils.applyAlphaToArgb(theme.bgDisabled(), 0x55)
                        : ColorUtils.applyAlphaToArgb(theme.bgDisabled(), 0x40);
                if (selected) {
                    rowBg = ColorUtils.applyAlphaToArgb(theme.bgTertiary(), 0x50);
                }
            } else {
                rowBg = selected
                        ? ColorUtils.applyAlphaToArgb(theme.accent(), 0x38)
                        : hover
                        ? ColorUtils.applyAlphaToArgb(theme.bgSecondary(), 0x45)
                        : ColorUtils.applyAlphaToArgb(theme.bgSecondary(), 0x28);
            }

            int textColor = !item.canTeleport && isBackPanel ? theme.textDisabled() : theme.textPrimary();
            int metaColor = !item.canTeleport && isBackPanel ? theme.textDisabled() : theme.textSecondary();

            ShapeDrawArgs rowRect = ShapeDrawArgs.rect(stack, listX, itemY, contentW, ITEM_HEIGHT - 1, rowBg);
            rowRect.rect().radius(4);
            BaseShapeWidget.drawShape(rowRect);

            if (item.canTeleport) {
                if (selected) {
                    ShapeDrawArgs accentBar = ShapeDrawArgs.rect(stack, listX, itemY, 3, ITEM_HEIGHT - 1, theme.accent());
                    BaseShapeWidget.drawShape(accentBar);
                } else if (hover) {
                    ShapeDrawArgs softBar = ShapeDrawArgs.rect(stack, listX, itemY, 2, ITEM_HEIGHT - 1, ColorUtils.applyAlphaToArgb(theme.accent(), 0x90));
                    BaseShapeWidget.drawShape(softBar);
                }
            } else if (selected) {
                ShapeDrawArgs disBar = ShapeDrawArgs.rect(stack, listX, itemY, 3, ITEM_HEIGHT - 1, theme.textDisabled());
                BaseShapeWidget.drawShape(disBar);
            }

            font.draw(stack, font.plainSubstrByWidth(item.name, contentW - 18), listX + 3, itemY + 2, textColor);
            String meta = item.getDimensionName() + " " + item.getCoordinateName();
            font.draw(stack, font.plainSubstrByWidth(meta, contentW - 18), listX + 3, itemY + 12, metaColor);

            if ((item.type == WaypointEntry.Type.HOME || item.type == WaypointEntry.Type.STAGE) && item.canTeleport) {
                int delX = listX + contentW - 16;
                int delY = itemY + (ITEM_HEIGHT - 10) / 2;
                boolean delHover = mouseX >= delX && mouseX <= delX + 14 && mouseY >= delY && mouseY < delY + 10;
                font.draw(stack, "×", delX + 2, delY, delHover ? theme.error() : theme.textHint());
            }
        }
    }

    private void drawFooter(MatrixStack stack, BaniraColorConfig theme) {
        ShapeDrawArgs footerBg = ShapeDrawArgs.rect(stack, footerX, footerY, footerW, FOOTER_HEIGHT, theme.panelBg());
        footerBg.rect().radius(6);
        BaseShapeWidget.drawShape(footerBg);

        ShapeDrawArgs footerTop = ShapeDrawArgs.rect(stack, footerX + 2, footerY, footerW - 4, 1, ColorUtils.applyAlphaToArgb(theme.accent(), 0x80));
        BaseShapeWidget.drawShape(footerTop);

        int centerGap = 16;
        int leftZoneW = (footerW - 2 * FOOTER_PAD_H - buttonW - 2 * centerGap) / 2;
        int rightZoneW = leftZoneW;
        int leftX = footerX + FOOTER_PAD_H;
        int rightX = footerX + footerW - FOOTER_PAD_H - rightZoneW;
        int lineH = font.lineHeight + 2;
        int y0 = footerY + FOOTER_PAD_V;

        if (selectedItem != null) {
            String dimStr = selectedItem.getDimensionName();
            String coordStr = selectedItem.getCoordinateName();
            String distanceStr;
            if (minecraft != null && minecraft.player != null && selectedItem.safeWorldCoordinate != null
                    && selectedItem.safeWorldCoordinate.dimension() == minecraft.player.level.dimension()) {
                distanceStr = NumberUtils.toFixedEx(selectedItem.safeWorldCoordinate.distanceFrom(new SafeWorldCoordinate(minecraft.player)), 1) + "m";
            } else {
                distanceStr = "∞m";
            }
            font.draw(stack, font.plainSubstrByWidth(dimStr, leftZoneW), leftX, y0, theme.textHint());
            font.draw(stack, font.plainSubstrByWidth(coordStr, leftZoneW), leftX, y0 + lineH, theme.textHint());
            font.draw(stack, font.plainSubstrByWidth(distanceStr, leftZoneW), leftX, y0 + lineH * 2, theme.textPrimary());

            String costStr = calculateCostDisplay(selectedItem);
            if (costStr != null && !costStr.isEmpty()) {
                Text costText = Text.literal(costStr).stack(stack).font(font).color(Color.argb(theme.textSecondary()));
                LabelWidget.drawLimitedText(FontDrawArgs.of(costText)
                        .x(rightX).y(y0)
                        .maxWidth(rightZoneW)
                        .align(EnumAlignment.END)
                        .wrap(true)
                        .inScreen(false)
                        .position(EnumEllipsisPosition.END)
                        .bgArgb(0).bgBorderRadius(0).bgBorderThickness(0)
                        .paddingLeft(0).paddingRight(0).paddingTop(0).paddingBottom(0));
            }
        }
    }

    private void drawDeleteConfirmOverlay(MatrixStack stack, BaniraColorConfig theme) {
        ShapeDrawArgs dim = ShapeDrawArgs.rect(stack, 0, 0, width, height, ColorUtils.applyAlphaToArgb(theme.bgQuaternary(), 0x78));
        BaseShapeWidget.drawShape(dim);

        ShapeDrawArgs dlg = ShapeDrawArgs.rect(stack, dlgX, dlgY, DIALOG_W, DIALOG_H, theme.panelBg());
        dlg.rect().radius(6);
        BaseShapeWidget.drawShape(dlg);

        ShapeDrawArgs dlgBorder = ShapeDrawArgs.rect(stack, dlgX, dlgY, DIALOG_W, DIALOG_H, theme.border());
        dlgBorder.rect().radius(6).border(1f);
        BaseShapeWidget.drawShape(dlgBorder);

        String title = Component.transClientAuto(NarcissusFarewell.MODID, "del_confirm_title").toString();
        String msg = Component.transClientAuto(NarcissusFarewell.MODID, "del_confirm_msg").toString();
        font.draw(stack, title, dlgX + (DIALOG_W - font.width(title)) / 2f, dlgY + 15, theme.textPrimary());
        font.draw(stack, msg, dlgX + (DIALOG_W - font.width(msg)) / 2f, dlgY + 35, theme.textSecondary());
    }

    private void onTeleportClick() {
        if (selectedItem == null || !selectedItem.canTeleport) {
            return;
        }
        EnumTeleportType type = itemTypeToEnum(selectedItem.type);
        String name = selectedItem.name;
        String dimension = "";
        if (type == EnumTeleportType.TP_HOME || type == EnumTeleportType.TP_STAGE) {
            if (selectedItem.safeWorldCoordinate != null) {
                dimension = selectedItem.safeWorldCoordinate.getDimensionResourceId();
            }
        } else if (type == EnumTeleportType.TP_BACK) {
            name = selectedItem.recordType != null ? selectedItem.recordType : "";
        }
        ModNetworkHandler.INSTANCE.sendToServer(new WaypointTeleportToServer(type, name, dimension));
        onClose();
    }

    private void doActualDelete(WaypointEntry item) {
        if (item == null || item.safeWorldCoordinate == null || minecraft == null || minecraft.player == null) {
            return;
        }
        int typeOrdinal = item.type == WaypointEntry.Type.HOME ? 0 : 1;
        String dimension = item.safeWorldCoordinate.getDimensionResourceId();
        ModNetworkHandler.INSTANCE.sendToServer(new WaypointDelToServer(typeOrdinal, item.name, dimension));
        if (item.type == WaypointEntry.Type.HOME) {
            homeItems.removeIf(e -> e.name.equals(item.name) && e.safeWorldCoordinate != null && dimension.equals(e.safeWorldCoordinate.getDimensionResourceId()));
        } else if (item.type == WaypointEntry.Type.STAGE) {
            stageItems.removeIf(e -> e.name.equals(item.name) && e.safeWorldCoordinate != null && dimension.equals(e.safeWorldCoordinate.getDimensionResourceId()));
        }
        syncScrollbarLimits();
        if (selectedItem == item) {
            WaypointEntry first = null;
            if (!homeItems.isEmpty()) {
                first = homeItems.get(0);
            } else if (!stageItems.isEmpty()) {
                first = stageItems.get(0);
            } else if (!backItems.isEmpty()) {
                first = backItems.get(0);
            }
            selectedItem = first;
        }
    }

    private String calculateCostDisplay(WaypointEntry item) {
        if (item == null || item.safeWorldCoordinate == null || minecraft == null || minecraft.player == null) {
            return "";
        }
        return ClientCostCalculator.formatCostDisplay(minecraft.player, item.safeWorldCoordinate, itemTypeToEnum(item.type));
    }

    private static EnumTeleportType itemTypeToEnum(WaypointEntry.Type type) {
        switch (type) {
            case STAGE:
                return EnumTeleportType.TP_STAGE;
            case BACK:
                return EnumTeleportType.TP_BACK;
            default:
                return EnumTeleportType.TP_HOME;
        }
    }

    private boolean isPlayerMoved() {
        if (minecraft != null && minecraft.player != null) {
            boolean changed = lastPlayerPos.x() != minecraft.player.getX()
                    || lastPlayerPos.y() != minecraft.player.getY()
                    || lastPlayerPos.z() != minecraft.player.getZ()
                    || lastPlayerPos.dimension() != minecraft.player.level.dimension();
            if (changed) {
                lastPlayerPos.fromVector3d(minecraft.player.position()).dimension(minecraft.player.level.dimension());
            }
            return changed;
        }
        return false;
    }

    @Accessors(chain = true, fluent = true)
    public static class WaypointEntry {
        public enum Type {HOME, STAGE, BACK}

        public final Type type;
        public final String name;
        public final SafeWorldCoordinate safeWorldCoordinate;
        public final boolean canTeleport;
        public final String recordType;

        public WaypointEntry(Type type, String name, SafeWorldCoordinate safeWorldCoordinate, boolean canTeleport, String recordType) {
            this.type = type;
            this.name = name;
            this.safeWorldCoordinate = safeWorldCoordinate;
            this.canTeleport = canTeleport;
            this.recordType = recordType;
        }

        public String getDetailTypeName() {
            switch (type) {
                case HOME:
                    return Component.transClientAuto(NarcissusFarewell.MODID, "private").toString();
                case STAGE:
                    return Component.transClientAuto(NarcissusFarewell.MODID, "public").toString();
                case BACK:
                    return Component.transClientAuto(NarcissusFarewell.MODID, "footprints").toString();
                default:
                    return "";
            }
        }

        public String getDimensionName() {
            if (safeWorldCoordinate == null) {
                return "";
            }
            String key = "dim." + safeWorldCoordinate.dimension().location().toString().replaceAll(":", ".");
            if (NarcissusLang.hasTranslation(EnumI18nType.WORD, key)) {
                return Component.transClientAuto(NarcissusFarewell.MODID, key).toString();
            }
            return safeWorldCoordinate.dimension().location().toString();
        }

        public String getCoordinateName() {
            return safeWorldCoordinate != null ? String.format("(%s)", safeWorldCoordinate.toXyzIntString(",")) : "";
        }
    }
}
