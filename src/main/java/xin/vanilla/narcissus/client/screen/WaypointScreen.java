package xin.vanilla.narcissus.client.screen;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.screens.Screen;
import xin.vanilla.narcissus.client.component.Text;
import xin.vanilla.narcissus.client.data.FontDrawArgs;
import xin.vanilla.narcissus.client.enums.EnumAlignment;
import xin.vanilla.narcissus.data.Color;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.client.ClientStageData;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.network.ModNetworkHandler;
import xin.vanilla.narcissus.network.packet.WaypointDelToServer;
import xin.vanilla.narcissus.network.packet.WaypointTeleportToServer;
import xin.vanilla.narcissus.util.*;

import javax.annotation.Nonnull;
import java.util.*;


// AI真是太好用辣
public class WaypointScreen extends Screen {

    // region Constants

    private static final int SCREEN_MARGIN = 8;
    private static final int GAP_H = 4;
    private static final int PANEL_PADDING = 6;
    private static final int TITLE_HEIGHT = 14;
    private static final int ITEM_HEIGHT = 24;
    private static final int MAX_VISIBLE_ITEMS = 6;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final int SCROLLBAR_GAP = 2;
    private static final int LIST_PADDING_V = 1;       // 列表区域上下内边距
    private static final int FOOTER_HEIGHT = 48;      // 底部详情区增高，容纳多行与按钮
    private static final int FOOTER_PAD_H = 10;
    private static final int FOOTER_PAD_V = 8;
    private static final int GAP_V_FOOTER = 8;         // 面板与底部信息区的间距
    private static final int BUTTON_HEIGHT = 20;

    // 浅色系清新风格
    private static final int COLORS_BG_PANEL = 0xF2EEF2F6;           // 面板背景，略偏冷白
    private static final int COLORS_BG_ITEM = 0xF0E4E8F0;           // 列表项默认
    private static final int COLORS_BG_ITEM_SELECTED = 0xF0DCE8E8;  // 选中：极淡青绿底
    private static final int COLORS_BG_ITEM_DISABLED = 0xF0D8DCE4;
    private static final int COLORS_BG_ITEM_DISABLED_BACK = 0xF0D0D4DC;
    private static final int COLORS_TEXT_DISABLED = 0xFF9098A8;
    private static final int COLORS_BG_ITEM_HOVER = 0xF0E8ECF4;     // 悬停：略亮
    private static final int COLORS_BORDER = 0xFFB0BCC8;            // 面板主边框
    private static final int COLORS_BORDER_SOFT = 0xFFC0CCD8;        // 柔和边框
    private static final int COLORS_ACCENT = 0xFF7BA89C;            // 点缀色，用于选中/主按钮
    private static final int COLORS_ACCENT_SOFT = 0xFF9CC0B4;       // 点缀色浅版，悬停边框等
    private static final int COLORS_SELECTED_DISABLED = 0xFF9098A4; // 不可传送项选中时的左边条/边框，灰调
    private static final int COLORS_TEXT = 0xFF2C3848;
    private static final int COLORS_TEXT_DIM = 0xFF586878;
    private static final int COLORS_SCROLLBAR = 0xA0A0B8C8;
    private static final int COLORS_DELETE_HOVER = 0xFFD84858;
    private static final int COLORS_TOOLTIP_BG = 0xF8F4F8FC;
    private static final int COLORS_BTN = 0xF0B8D0C8;               // 主按钮
    private static final int COLORS_BTN_HOVER = 0xF0A0C4B8;         // 主按钮悬停
    private static final int COLORS_BTN_BORDER = 0xFF8CA89C;        // 主按钮边框
    private static final int COLORS_TEXT_ON_DARK = 0xFFF5FAFF;
    private static final int COLORS_FOOTER_BORDER = 0xFFA0B4C4;     // 底部详情区上边框，与面板区分

    // endregion Constants

    // region  Data

    private final Coordinate lastPlayerPos = new Coordinate();
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

    // 删除确认
    private WaypointEntry deleteConfirmItem;

    // 滚动偏移
    private int homeScroll, stageScroll, backScroll;

    // Tooltip 用
    private WaypointEntry hoveredItem;

    // 传送按钮区域
    private int buttonX, buttonY, buttonW, buttonH;

    // endregion Data


    public WaypointScreen() {
        super(Component.literal("WaypointScreen").toChatComponent());
    }

    @Override
    protected void init() {
        if (minecraft == null || minecraft.player == null) return;

        super.init();

        int availWidth = width - 2 * SCREEN_MARGIN - 2 * GAP_H;
        panelWidth = availWidth / 3;
        listHeight = ITEM_HEIGHT * MAX_VISIBLE_ITEMS;
        panelHeight = TITLE_HEIGHT + 2 * LIST_PADDING_V + listHeight;

        loadData();

        int footerY = 20 + panelHeight + GAP_V_FOOTER;
        buttonW = 100;
        buttonH = BUTTON_HEIGHT;
        buttonX = (width - 100) / 2;
        buttonY = footerY + (FOOTER_HEIGHT - buttonH) / 2;

        homeScroll = 0;
        stageScroll = 0;
        backScroll = 0;
    }

    @Override
    public void render(@Nonnull PoseStack stack, int mouseX, int mouseY, float tick) {
        renderBackground(stack);

        if (minecraft != null && minecraft.player != null) {
            PlayerTeleportData data = PlayerTeleportData.getData(minecraft.player);
            ticketCount = data.getTeleportCard();
        }

        // 更新选中项详情
        if (selectedItem != null) {
            long now = System.currentTimeMillis();
            boolean needUpdate = (now - lastUpdateTime > 500 && selectedItem != lastSelectedItem) || isPlayerMoved();
            if (needUpdate) {
                lastUpdateTime = now;
                lastSelectedItem = selectedItem;
            }
        }

        int startX = SCREEN_MARGIN;
        int startY = 20;

        if (deleteConfirmItem == null) {
            updateHoveredItem(mouseX, mouseY);
        } else {
            hoveredItem = null;
        }

        // 绘制三个面板
        drawPanel(stack, startX, startY, Component.transClient(EnumI18nType.WORD, "private").toString(), homeItems, homeScroll, mouseX, mouseY, false);
        drawPanel(stack, startX + panelWidth + GAP_H, startY, Component.transClient(EnumI18nType.WORD, "public").toString(), stageItems, stageScroll, mouseX, mouseY, false);
        drawPanel(stack, startX + (panelWidth + GAP_H) * 2, startY, Component.transClient(EnumI18nType.WORD, "footprints").toString(), backItems, backScroll, mouseX, mouseY, true);

        // 底部详情区
        int footerY = startY + panelHeight + GAP_V_FOOTER;
        drawFooter(stack, footerY, mouseX, mouseY);

        // 传送卡数量
        String ticketStr = Component.transClient(EnumI18nType.WORD, "teleport_card").toString() + ": " + ticketCount;
        int tw = font.width(ticketStr);
        font.draw(stack, ticketStr, width - tw - 10, 10, COLORS_TEXT_ON_DARK);

        // 删除确认遮罩
        if (deleteConfirmItem != null) {
            drawDeleteConfirmOverlay(stack, mouseX, mouseY);
        }

        // Tooltip
        if (hoveredItem != null && deleteConfirmItem == null) {
            drawCustomTooltip(stack, hoveredItem, mouseX, mouseY);
        }

        // 传送按钮
        drawTeleportButton(stack, mouseX, mouseY);

        super.render(stack, mouseX, mouseY, tick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        // 传送按钮点击
        if (deleteConfirmItem == null && mouseX >= buttonX && mouseX < buttonX + buttonW && mouseY >= buttonY && mouseY < buttonY + buttonH) {
            if (selectedItem != null && selectedItem.canTeleport) {
                onTeleportClick();
                return true;
            }
        }

        if (deleteConfirmItem != null) {
            int dlgW = 280;
            int dlgH = 100;
            int dlgX = (width - dlgW) / 2;
            int dlgY = (height - dlgH) / 2;
            int cancelW = font.width(Component.transClient(EnumI18nType.WORD, "cancel").toString()) + 20;
            int deleteW = font.width(Component.transClient(EnumI18nType.WORD, "delete").toString()) + 20;
            int btnY = dlgY + dlgH - 30;
            int cancelX = dlgX + (dlgW - cancelW - deleteW - 10) / 2;
            int deleteX = cancelX + cancelW + 10;

            if (mouseX >= cancelX && mouseX <= cancelX + cancelW && mouseY >= btnY && mouseY < btnY + 20) {
                deleteConfirmItem = null;
                return true;
            }
            if (mouseX >= deleteX && mouseX <= deleteX + deleteW && mouseY >= btnY && mouseY < btnY + 20) {
                doActualDelete(deleteConfirmItem);
                deleteConfirmItem = null;
                return true;
            }
            return true;
        }

        int startX = SCREEN_MARGIN;
        int startY = 20;
        int listY = startY + TITLE_HEIGHT + LIST_PADDING_V;
        int contentW = panelWidth - 2 * PANEL_PADDING - SCROLLBAR_WIDTH - SCROLLBAR_GAP;

        // 检查三个面板的点击
        if (checkPanelClick(mouseX, mouseY, startX + PANEL_PADDING, listY, contentW, homeItems, homeScroll))
            return true;
        if (checkPanelClick(mouseX, mouseY, startX + panelWidth + GAP_H + PANEL_PADDING, listY, contentW, stageItems, stageScroll))
            return true;
        if (checkPanelClick(mouseX, mouseY, startX + (panelWidth + GAP_H) * 2 + PANEL_PADDING, listY, contentW, backItems, backScroll))
            return true;

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int startX = SCREEN_MARGIN;
        int startY = 20;
        int listY = startY + TITLE_HEIGHT + LIST_PADDING_V;
        int contentW = panelWidth - 2 * PANEL_PADDING - SCROLLBAR_WIDTH - SCROLLBAR_GAP;

        for (int p = 0; p < 3; p++) {
            int px = startX + p * (panelWidth + GAP_H) + PANEL_PADDING;
            if (mouseX >= px && mouseX < px + contentW && mouseY >= listY && mouseY < listY + listHeight) {
                if (p == 0) {
                    homeScroll = (int) clamp(homeScroll - delta, 0, Math.max(0, homeItems.size() - MAX_VISIBLE_ITEMS));
                } else if (p == 1) {
                    stageScroll = (int) clamp(stageScroll - delta, 0, Math.max(0, stageItems.size() - MAX_VISIBLE_ITEMS));
                } else {
                    backScroll = (int) clamp(backScroll - delta, 0, Math.max(0, backItems.size() - MAX_VISIBLE_ITEMS));
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }


    private void loadData() {
        if (minecraft == null || minecraft.player == null) return;
        PlayerTeleportData data = PlayerTeleportData.getData(minecraft.player);

        homeItems.clear();
        for (KeyValue<String, String> key : data.getHomeCoordinate().keySet()) {
            homeItems.add(new WaypointEntry(WaypointEntry.Type.HOME, key.value(),
                    data.getHomeCoordinate().get(key), true, null));
        }

        stageItems.clear();
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : ClientStageData.getStageCoordinate().entrySet()) {
            stageItems.add(new WaypointEntry(WaypointEntry.Type.STAGE, entry.getKey().value(),
                    entry.getValue(), true, null));
        }

        backItems.clear();
        List<TeleportRecord> records = data.getTeleportRecords().stream()
                .filter(r -> r.getBefore() != null)
                .toList();
        Set<String> seenRecordTypes = new HashSet<>();
        for (int i = records.size() - 1; i >= 0; i--) {
            TeleportRecord record = records.get(i);
            String recordTypeName = record.getTeleportType().name();
            boolean canTp = !seenRecordTypes.contains(recordTypeName);
            if (canTp) seenRecordTypes.add(recordTypeName);
            backItems.add(new WaypointEntry(WaypointEntry.Type.BACK, recordTypeName, record.getBefore(),
                    canTp, recordTypeName));
        }

        WaypointEntry first = null;
        if (!homeItems.isEmpty()) first = homeItems.get(0);
        else if (!stageItems.isEmpty()) first = stageItems.get(0);
        else if (!backItems.isEmpty()) first = backItems.get(0);
        if (first != null) selectedItem = first;

        ticketCount = data.getTeleportCard();
    }

    private boolean checkPanelClick(double mouseX, double mouseY, int listX, int listY, int contentW, List<WaypointEntry> items, int scroll) {
        for (int i = 0; i < MAX_VISIBLE_ITEMS; i++) {
            int idx = i + scroll;
            if (idx >= items.size()) break;

            WaypointEntry item = items.get(idx);
            int itemY = listY + i * ITEM_HEIGHT;

            if (mouseX >= listX && mouseX < listX + contentW && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT - 1) {
                // 删除按钮区域
                if ((item.type == WaypointEntry.Type.HOME || item.type == WaypointEntry.Type.STAGE) && item.canTeleport) {
                    int delX = listX + contentW - 16;
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
        int startX = SCREEN_MARGIN;
        int startY = 20;
        int listY = startY + TITLE_HEIGHT + LIST_PADDING_V;
        int contentW = panelWidth - 2 * PANEL_PADDING - SCROLLBAR_WIDTH - SCROLLBAR_GAP;

        for (int p = 0; p < 3; p++) {
            int listX = startX + p * (panelWidth + GAP_H) + PANEL_PADDING;
            List<WaypointEntry> items = p == 0 ? homeItems : (p == 1 ? stageItems : backItems);
            int scroll = p == 0 ? homeScroll : (p == 1 ? stageScroll : backScroll);

            for (int i = 0; i < MAX_VISIBLE_ITEMS; i++) {
                int idx = i + scroll;
                if (idx >= items.size()) break;
                int itemY = listY + i * ITEM_HEIGHT;
                if (mouseX >= listX && mouseX < listX + contentW && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
                    hoveredItem = items.get(idx);
                    return;
                }
            }
        }
    }

    private void drawTeleportButton(PoseStack stack, int mouseX, int mouseY) {
        // 若有确认弹窗，按钮仍绘制但不响应悬浮高亮
        boolean hover = deleteConfirmItem == null
                && mouseX >= buttonX && mouseX < buttonX + buttonW
                && mouseY >= buttonY && mouseY < buttonY + buttonH;
        boolean canTp = selectedItem != null && selectedItem.canTeleport;
        int bgColor = canTp ? (hover ? COLORS_BTN_HOVER : COLORS_BTN) : COLORS_BG_ITEM_DISABLED;
        int textColor = canTp ? COLORS_TEXT : COLORS_TEXT_DISABLED;
        int borderColor = canTp ? (hover ? COLORS_ACCENT : COLORS_BTN_BORDER) : COLORS_BORDER_SOFT;

        AbstractGuiUtils.drawRoundedRect(stack, buttonX, buttonY, buttonW, buttonH, bgColor, 4);
        AbstractGuiUtils.drawRoundedRectOutLineRough(stack, buttonX, buttonY, buttonW, buttonH, 1, borderColor, 4);
        String btnText = Component.transClient(EnumI18nType.WORD, "teleport_btn").toString();
        font.draw(stack, btnText, buttonX + (buttonW - font.width(btnText)) / 2f, buttonY + (buttonH - font.lineHeight) / 2f + 1, textColor);
    }

    private void drawCustomTooltip(PoseStack stack, WaypointEntry item, int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        lines.add(item.name);
        lines.add(item.getDetailTypeName() + " | " + item.getDimensionName());
        lines.add(item.getCoordinateName());
        if (!item.canTeleport) {
            lines.add(Component.transClient(EnumI18nType.WORD, "back_record_used").toString());
        }
        String content = String.join("\n", lines);
        Text tooltipText = Text.literal(content).stack(stack).font(font).color(Color.argb(COLORS_TEXT));
        FontDrawArgs args = FontDrawArgs.of(tooltipText)
                .x(mouseX).y(mouseY)
                .marginTop(2).marginBottom(2).marginLeft(2).marginRight(2)
                .paddingTop(6).paddingBottom(6).paddingLeft(6).paddingRight(6)
                .bgArgb(COLORS_TOOLTIP_BG).bgBorderRadius(4).bgBorderThickness(1)
                .inScreen(true);
        AbstractGuiUtils.drawPopupMessage(args);
    }

    private void drawPanel(PoseStack stack, int x, int y, String title, List<WaypointEntry> items, int scroll, int mouseX, int mouseY, boolean isBackPanel) {
        AbstractGuiUtils.drawRoundedRect(stack, x, y, panelWidth, panelHeight, COLORS_BG_PANEL, 3);
        AbstractGuiUtils.drawRoundedRectOutLineRough(stack, x, y, panelWidth, panelHeight, 1, COLORS_BORDER, 3);
        AbstractGuiUtils.fill(stack, x + 2, y + TITLE_HEIGHT - 1, panelWidth - 4, 1, COLORS_BORDER_SOFT);

        font.draw(stack, title, x + PANEL_PADDING, y + 3, COLORS_TEXT);

        int listY = y + TITLE_HEIGHT + LIST_PADDING_V;
        int listX = x + PANEL_PADDING;
        int contentW = panelWidth - 2 * PANEL_PADDING - SCROLLBAR_WIDTH - SCROLLBAR_GAP;
        int scrollbarX = listX + contentW + SCROLLBAR_GAP;

        int maxScroll = Math.max(0, items.size() - MAX_VISIBLE_ITEMS);
        scroll = clamp(scroll, 0, maxScroll);

        for (int i = 0; i < MAX_VISIBLE_ITEMS; i++) {
            int idx = i + scroll;
            if (idx >= items.size()) break;

            WaypointEntry item = items.get(idx);
            int itemY = listY + i * ITEM_HEIGHT;
            boolean hover = mouseX >= listX && mouseX < listX + contentW && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
            boolean selected = item == selectedItem;

            int bgColor;
            if (!item.canTeleport) {
                bgColor = isBackPanel ? COLORS_BG_ITEM_DISABLED_BACK : COLORS_BG_ITEM_DISABLED;
                if (selected) {
                    bgColor = 0xF0D4D8E0;
                }
            } else {
                bgColor = selected ? COLORS_BG_ITEM_SELECTED : (hover ? COLORS_BG_ITEM_HOVER : COLORS_BG_ITEM);
            }
            int textColor = !item.canTeleport && isBackPanel ? COLORS_TEXT_DISABLED : COLORS_TEXT;
            int metaColor = !item.canTeleport && isBackPanel ? COLORS_TEXT_DISABLED : COLORS_TEXT_DIM;

            AbstractGuiUtils.drawRoundedRect(stack, listX, itemY, contentW, ITEM_HEIGHT - 1, bgColor, 2);
            if (item.canTeleport) {
                if (selected) {
                    AbstractGuiUtils.fill(stack, listX, itemY, 3, ITEM_HEIGHT - 1, COLORS_ACCENT);
                    AbstractGuiUtils.drawRoundedRectOutLineRough(stack, listX, itemY, contentW, ITEM_HEIGHT - 1, 1, COLORS_ACCENT_SOFT, 2);
                } else if (hover) {
                    AbstractGuiUtils.fill(stack, listX, itemY, 2, ITEM_HEIGHT - 1, COLORS_ACCENT_SOFT);
                }
            } else if (selected) {
                AbstractGuiUtils.fill(stack, listX, itemY, 3, ITEM_HEIGHT - 1, COLORS_SELECTED_DISABLED);
                AbstractGuiUtils.drawRoundedRectOutLineRough(stack, listX, itemY, contentW, ITEM_HEIGHT - 1, 1, COLORS_SELECTED_DISABLED, 2);
            }

            font.draw(stack, font.plainSubstrByWidth(item.name, contentW - 18), listX + 3, itemY + 2, textColor);
            String meta = item.getDimensionName() + " " + item.getCoordinateName();
            font.draw(stack, font.plainSubstrByWidth(meta, contentW - 18), listX + 3, itemY + 12, metaColor);

            // 删除按钮 (home/stage)
            if ((item.type == WaypointEntry.Type.HOME || item.type == WaypointEntry.Type.STAGE) && item.canTeleport) {
                int delX = listX + contentW - 16;
                int delY = itemY + (ITEM_HEIGHT - 10) / 2;
                boolean delHover = mouseX >= delX && mouseX <= delX + 14 && mouseY >= delY && mouseY < delY + 10;
                font.draw(stack, "×", delX + 2, delY, delHover ? COLORS_DELETE_HOVER : COLORS_TEXT_DIM);
            }
        }

        // 滚动条
        if (items.size() > MAX_VISIBLE_ITEMS && maxScroll > 0) {
            int barH = listHeight;
            int thumbH = Math.max(8, barH * MAX_VISIBLE_ITEMS / items.size());
            int thumbY = listY + (int) ((barH - thumbH) * (double) scroll / maxScroll);
            AbstractGuiUtils.fill(stack, scrollbarX, thumbY, SCROLLBAR_WIDTH, thumbH, COLORS_SCROLLBAR);
        }
    }

    private void drawFooter(PoseStack stack, int footerY, int mouseX, int mouseY) {
        int footerX = SCREEN_MARGIN;
        int footerW = width - 2 * SCREEN_MARGIN;

        AbstractGuiUtils.drawRoundedRect(stack, footerX, footerY, footerW, FOOTER_HEIGHT, COLORS_BG_PANEL, 3);
        AbstractGuiUtils.drawRoundedRectOutLineRough(stack, footerX, footerY, footerW, FOOTER_HEIGHT, 1, COLORS_FOOTER_BORDER, 3);
        AbstractGuiUtils.fill(stack, footerX + 2, footerY, footerW - 4, 1, COLORS_ACCENT_SOFT);

        int centerGap = 16;
        int leftZoneW = (footerW - 2 * FOOTER_PAD_H - buttonW - 2 * centerGap) / 2;
        int rightZoneW = leftZoneW;
        int leftX = footerX + FOOTER_PAD_H;
        int rightX = footerX + footerW - FOOTER_PAD_H - rightZoneW;
        int lineH = font.lineHeight + 2;
        int y0 = footerY + FOOTER_PAD_V;

        if (selectedItem != null) {
            // 维度、坐标、距离
            String dimStr = selectedItem.getDimensionName();
            String coordStr = selectedItem.getCoordinateName();
            String distanceStr;
            if (minecraft != null && minecraft.player != null && selectedItem.coordinate != null
                    && selectedItem.coordinate.dimension() == minecraft.player.level.dimension()) {
                distanceStr = NumberUtils.toFixedEx(selectedItem.coordinate.distanceFrom(new Coordinate(minecraft.player)), 1) + "m";
            } else {
                distanceStr = "∞m";
            }
            font.draw(stack, font.plainSubstrByWidth(dimStr, leftZoneW), leftX, y0, COLORS_TEXT_DIM);
            font.draw(stack, font.plainSubstrByWidth(coordStr, leftZoneW), leftX, y0 + lineH, COLORS_TEXT_DIM);
            font.draw(stack, font.plainSubstrByWidth(distanceStr, leftZoneW), leftX, y0 + lineH * 2, COLORS_TEXT);

            // 传送消耗
            String costStr = calculateCostDisplay(selectedItem);
            if (costStr != null && !costStr.isEmpty()) {
                Text costText = Text.literal(costStr).stack(stack).font(font).color(Color.argb(COLORS_TEXT_DIM));
                AbstractGuiUtils.drawLimitedText(FontDrawArgs.of(costText)
                        .x(rightX).y(y0)
                        .maxWidth(rightZoneW)
                        .align(EnumAlignment.END)
                        .wrap(true)
                        .inScreen(false)
                        .paddingLeft(0).paddingRight(0).paddingTop(0).paddingBottom(0));
            }
        }
    }

    private void drawDeleteConfirmOverlay(PoseStack stack, int mouseX, int mouseY) {
        AbstractGuiUtils.fill(stack, 0, 0, width, height, 0x60000000);

        int dlgW = 280;
        int dlgH = 100;
        int dlgX = (width - dlgW) / 2;
        int dlgY = (height - dlgH) / 2;

        AbstractGuiUtils.drawRoundedRect(stack, dlgX, dlgY, dlgW, dlgH, COLORS_BG_PANEL, 6);
        AbstractGuiUtils.drawRoundedRectOutLineRough(stack, dlgX, dlgY, dlgW, dlgH, 1, COLORS_BORDER, 6);

        String title = Component.transClient(EnumI18nType.WORD, "del_confirm_title").toString();
        String msg = Component.transClient(EnumI18nType.WORD, "del_confirm_msg").toString();
        font.draw(stack, title, dlgX + (dlgW - font.width(title)) / 2f, dlgY + 15, COLORS_TEXT);
        font.draw(stack, msg, dlgX + (dlgW - font.width(msg)) / 2f, dlgY + 35, COLORS_TEXT_DIM);

        String cancelStr = Component.transClient(EnumI18nType.WORD, "cancel").toString();
        String deleteStr = Component.transClient(EnumI18nType.WORD, "delete").toString();
        int cancelW = font.width(cancelStr) + 20;
        int deleteW = font.width(deleteStr) + 20;
        int btnY = dlgY + dlgH - 30;
        int cancelX = dlgX + (dlgW - cancelW - deleteW - 10) / 2;
        int deleteX = cancelX + cancelW + 10;

        boolean cancelHover = mouseX >= cancelX && mouseX <= cancelX + cancelW && mouseY >= btnY && mouseY < btnY + 20;
        boolean deleteHover = mouseX >= deleteX && mouseX <= deleteX + deleteW && mouseY >= btnY && mouseY < btnY + 20;

        AbstractGuiUtils.drawRoundedRect(stack, cancelX, btnY, cancelW, 20, cancelHover ? 0xE8D0D8E0 : 0xE8C0C8D0, 4);
        AbstractGuiUtils.drawRoundedRect(stack, deleteX, btnY, deleteW, 20, deleteHover ? 0xE8F0A0A8 : 0xE8E09098, 4);
        font.draw(stack, cancelStr, cancelX + (cancelW - font.width(cancelStr)) / 2f, btnY + 6, COLORS_TEXT);
        font.draw(stack, deleteStr, deleteX + (deleteW - font.width(deleteStr)) / 2f, btnY + 6, COLORS_TEXT);
    }

    private void onTeleportClick() {
        if (selectedItem == null || !selectedItem.canTeleport) return;
        EnumTeleportType type = itemTypeToEnum(selectedItem.type);
        String name = selectedItem.name;
        String dimension = "";
        if (type == EnumTeleportType.TP_HOME || type == EnumTeleportType.TP_STAGE) {
            if (selectedItem.coordinate != null) dimension = selectedItem.coordinate.getDimensionResourceId();
        } else if (type == EnumTeleportType.TP_BACK) {
            name = selectedItem.recordType != null ? selectedItem.recordType : "";
        }
        ModNetworkHandler.INSTANCE.sendToServer(new WaypointTeleportToServer(type, name, dimension));
        onClose();
    }

    private void doActualDelete(WaypointEntry item) {
        if (item == null || item.coordinate == null || minecraft == null || minecraft.player == null) return;
        int typeOrdinal = item.type == WaypointEntry.Type.HOME ? 0 : 1;
        String dimension = item.coordinate.getDimensionResourceId();
        ModNetworkHandler.INSTANCE.sendToServer(new WaypointDelToServer(typeOrdinal, item.name, dimension));
        // 乐观更新
        if (item.type == WaypointEntry.Type.HOME) {
            homeItems.removeIf(e -> e.name.equals(item.name) && e.coordinate != null && dimension.equals(e.coordinate.getDimensionResourceId()));
            homeScroll = Math.min(homeScroll, Math.max(0, homeItems.size() - MAX_VISIBLE_ITEMS));
        } else if (item.type == WaypointEntry.Type.STAGE) {
            stageItems.removeIf(e -> e.name.equals(item.name) && e.coordinate != null && dimension.equals(e.coordinate.getDimensionResourceId()));
            stageScroll = Math.min(stageScroll, Math.max(0, stageItems.size() - MAX_VISIBLE_ITEMS));
        }
        if (selectedItem == item) {
            WaypointEntry first = null;
            if (!homeItems.isEmpty()) first = homeItems.get(0);
            else if (!stageItems.isEmpty()) first = stageItems.get(0);
            else if (!backItems.isEmpty()) first = backItems.get(0);
            selectedItem = first;
        }
    }

    private String calculateCostDisplay(WaypointEntry item) {
        if (item == null || item.coordinate == null || minecraft == null || minecraft.player == null) return "";
        return ClientCostCalculator.formatCostDisplay(minecraft.player, item.coordinate, itemTypeToEnum(item.type));
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
                lastPlayerPos.fromVec3(minecraft.player.position()).dimension(minecraft.player.level.dimension());
            }
            return changed;
        }
        return false;
    }

    public static int clamp(int a, int b, int c) {
        if (a < b) {
            return b;
        } else {
            return Math.min(a, c);
        }
    }

    public static double clamp(double a, double b, double c) {
        if (a < b) {
            return b;
        } else {
            return Math.min(a, c);
        }
    }


    public static class WaypointEntry {
        public enum Type {HOME, STAGE, BACK}

        public final Type type;
        public final String name;
        public final Coordinate coordinate;
        public final boolean canTeleport;
        public final String recordType;

        public WaypointEntry(Type type, String name, Coordinate coordinate, boolean canTeleport, String recordType) {
            this.type = type;
            this.name = name;
            this.coordinate = coordinate;
            this.canTeleport = canTeleport;
            this.recordType = recordType;
        }

        public String getDetailTypeName() {
            switch (type) {
                case HOME:
                    return Component.transClient(EnumI18nType.WORD, "private").toString();
                case STAGE:
                    return Component.transClient(EnumI18nType.WORD, "public").toString();
                case BACK:
                    return Component.transClient(EnumI18nType.WORD, "footprints").toString();
                default:
                    return "";
            }
        }

        public String getDimensionName() {
            if (coordinate == null) return "";
            String key = "dim." + coordinate.dimension().location().toString().replaceAll(":", ".");
            if (I18nUtils.hasTranslation(EnumI18nType.WORD, key)) {
                return Component.transClient(EnumI18nType.WORD, key).toString();
            }
            return coordinate.dimension().location().toString();
        }

        public String getCoordinateName() {
            return coordinate != null ? String.format("(%s)", coordinate.toXyzIntString(",")) : "";
        }
    }
}
