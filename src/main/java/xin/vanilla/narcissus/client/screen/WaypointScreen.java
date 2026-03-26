package xin.vanilla.narcissus.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumEllipsisPosition;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.client.ClientStageData;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.enums.EnumWaypointPanelMode;
import xin.vanilla.narcissus.network.NetworkInit;
import xin.vanilla.narcissus.network.packet.WaypointAddHomeToServer;
import xin.vanilla.narcissus.network.packet.WaypointAddStageToServer;
import xin.vanilla.narcissus.network.packet.WaypointDelToServer;
import xin.vanilla.narcissus.network.packet.WaypointTeleportToServer;
import xin.vanilla.narcissus.util.ClientCostCalculator;
import xin.vanilla.narcissus.util.NarcissusUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Accessors(chain = true, fluent = true)
public class WaypointScreen extends BaniraScreen {

    // region Constants

    private static final int SCREEN_MARGIN = 8;
    private static final int GAP_H = 1;
    private static final int PANEL_PADDING = 6;
    private static final int HEADER_ROW_H = 22;
    private static final int TOP_BAR_H = 22;
    private static final int ITEM_HEIGHT = 24;
    private static final int MAX_VISIBLE_ITEMS = 6;
    /**
     * 窗口高度较低时列表固定显示行数
     */
    private static final int LIST_ROWS_COMPACT = 5;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 2;
    private static final int LIST_PADDING_V = 2;
    private static final int FOOTER_HEIGHT = 48;
    /**
     * Tab 模式详情区
     */
    private static final int FOOTER_HEIGHT_TAB = 48;
    private static final int FOOTER_PAD_H = 10;
    private static final int FOOTER_PAD_V = 6;
    private static final int DIVIDER = 1;
    private static final int DIALOG_W = 280;
    private static final int DIALOG_H = 100;
    private static final int DOUBLE_CLICK_MS = 450;
    private static final int ADD_BTN_SIZE = 14;
    /**
     * Tab 条圆角半径
     */
    private static final float TAB_BTN_CORNER_R = 3f;
    private static final int TELEPORT_BTN_W = 100;
    private static final int TELEPORT_BTN_H = 20;
    private static final long TICKET_LONG_PRESS_MS = 550;

    // endregion Constants

    // region Types

    private enum WaypointListTab {
        PRIVATE,
        PUBLIC,
        FOOTPRINTS
    }

    // endregion Types

    // region Data

    private final SafeWorldCoordinate lastPlayerPos = new SafeWorldCoordinate();
    private long lastUpdateTime = 0;

    private final List<WaypointEntry> homeItemsAll = new ArrayList<>();
    private final List<WaypointEntry> stageItemsAll = new ArrayList<>();
    private final List<WaypointEntry> backItemsAll = new ArrayList<>();

    private final List<WaypointEntry> homeItems = new ArrayList<>();
    private final List<WaypointEntry> stageItems = new ArrayList<>();
    private final List<WaypointEntry> backItems = new ArrayList<>();

    private WaypointEntry selectedItem;
    private WaypointEntry lastSelectedItem;

    private int ticketCount;
    private EnumWaypointPanelMode panelMode = EnumWaypointPanelMode.THREE_COLUMNS;
    private WaypointListTab activeTab = WaypointListTab.PRIVATE;
    private int panelWidth;
    /**
     * 当前窗口高度下列表可见行数：较高 {@link #MAX_VISIBLE_ITEMS} 行，较低 {@link #LIST_ROWS_COMPACT} 行；极端高度不足时再压缩。
     */
    private int visibleRowCount = MAX_VISIBLE_ITEMS;
    private int listHeight;
    /**
     * 列表内容区宽度（= 面板外宽 - 2 * {@link #PANEL_PADDING}，随模式变化）
     */
    private int listBodyW;
    private int startX;
    private int topBarY;
    private int headerRowY;
    private int listAreaY;
    private int footerY;
    private int footerX;
    private int footerW;
    /**
     * 当前布局下详情条高度（三列 {@link #FOOTER_HEIGHT}，Tab {@link #FOOTER_HEIGHT_TAB}）
     */
    private int footerPanelHeight = FOOTER_HEIGHT;
    private int dlgX;
    private int dlgY;

    private WaypointEntry deleteConfirmItem;
    private WaypointEntry hoveredItem;

    private WaypointEntry lastDoubleClickEntry;
    private long lastDoubleClickTime;

    private InputWidget searchInput;
    private ButtonWidget addHomeButton;
    private ButtonWidget addStageButton;
    private ButtonWidget tabPrivateButton;
    private ButtonWidget tabPublicButton;
    private ButtonWidget tabFootprintsButton;
    private ButtonWidget tabAddButton;
    private ScrollbarWidget homeScrollbar;
    private ScrollbarWidget stageScrollbar;
    private ScrollbarWidget backScrollbar;
    private ScrollbarWidget tabListScrollbar;
    private ButtonWidget teleportButton;
    private ButtonWidget deleteCancelButton;
    private ButtonWidget deleteConfirmButton;
    /**
     * 顶栏传送卡
     */
    private ButtonWidget ticketLayoutToggleButton;

    // endregion Data

    public WaypointScreen() {
        super(NarcissusComponent.get().literal("WaypointScreen"));
        previousScreen(Minecraft.getInstance().screen);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    protected void onInit() {
        panelMode = ClientConfig.get().client().waypointScreenPanelMode();
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        loadData();
    }

    @Override
    protected void initWidgets() {
        homeScrollbar = null;
        stageScrollbar = null;
        backScrollbar = null;
        tabListScrollbar = null;
        addHomeButton = null;
        addStageButton = null;
        tabPrivateButton = null;
        tabPublicButton = null;
        tabFootprintsButton = null;
        tabAddButton = null;
        ticketLayoutToggleButton = null;

        footerPanelHeight = panelMode == EnumWaypointPanelMode.TAB_SINGLE ? FOOTER_HEIGHT_TAB : FOOTER_HEIGHT;

        int usableOuter = width - 2 * SCREEN_MARGIN;
        panelWidth = (usableOuter - 2 * GAP_H) / 3;
        int columnsTotalWidth = 3 * panelWidth + 2 * GAP_H;
        if (panelMode == EnumWaypointPanelMode.TAB_SINGLE) {
            footerW = 2 * panelWidth + GAP_H;
            listBodyW = footerW - 2 * PANEL_PADDING;
        } else {
            footerW = columnsTotalWidth;
            listBodyW = panelWidth - 2 * PANEL_PADDING;
        }

        int listOverhead = TOP_BAR_H + DIVIDER + HEADER_ROW_H + DIVIDER + DIVIDER;
        int minEdge = SCREEN_MARGIN + 4;
        int maxBlockH = height - 2 * minEdge;
        int maxListPixel = maxBlockH - footerPanelHeight - listOverhead;
        if (maxListPixel >= MAX_VISIBLE_ITEMS * ITEM_HEIGHT) {
            visibleRowCount = MAX_VISIBLE_ITEMS;
        } else if (maxListPixel >= LIST_ROWS_COMPACT * ITEM_HEIGHT) {
            visibleRowCount = LIST_ROWS_COMPACT;
        } else {
            visibleRowCount = Mth.clamp(maxListPixel / ITEM_HEIGHT, 1, LIST_ROWS_COMPACT);
        }
        listHeight = visibleRowCount * ITEM_HEIGHT;

        startX = SCREEN_MARGIN + (usableOuter - footerW) / 2;
        footerX = startX;

        int listBlockH = TOP_BAR_H + DIVIDER + HEADER_ROW_H + DIVIDER + listHeight + DIVIDER;
        int contentBlockHeight = listBlockH + footerPanelHeight;
        int blockTop = Math.max(SCREEN_MARGIN + 4, (height - contentBlockHeight) / 2);

        topBarY = blockTop;
        headerRowY = topBarY + TOP_BAR_H + DIVIDER;
        listAreaY = headerRowY + HEADER_ROW_H + DIVIDER;
        footerY = listAreaY + listHeight + DIVIDER;

        dlgX = (width - DIALOG_W) / 2;
        dlgY = (height - DIALOG_H) / 2;

        int cancelW = Math.max(72, font.width(NarcissusComponent.get().transClientAuto("cancel").toString()) + 20);
        int deleteW = Math.max(72, font.width(NarcissusComponent.get().transClientAuto("delete").toString()) + 20);
        int btnY = dlgY + DIALOG_H - 30;
        int cancelX = dlgX + (DIALOG_W - cancelW - deleteW - 10) / 2;
        int deleteX = cancelX + cancelW + 10;

        int searchW = Math.min(220, footerW / 2);
        searchInput = new InputWidget(this);
        searchInput.id("search");
        searchInput.bounds(new ScreenCoordinate(startX + footerW - searchW - PANEL_PADDING, topBarY + 3, searchW, TOP_BAR_H - 6));
        searchInput.text(Text.literal(NarcissusComponent.get().transClientAuto("waypoint_search_hint").toString()));
        searchInput.onTextChanged(t -> applySearchFilter());
        addWidget(searchInput);

        String ticketStr = NarcissusComponent.get().transClientAuto("teleport_card").toString() + ": " + ticketCount;
        int ticketChipW = Math.max(font.width(ticketStr) + 16, 100);
        ticketLayoutToggleButton = new ButtonWidget(this);
        ticketLayoutToggleButton.id("ticket_layout_toggle");
        ticketLayoutToggleButton.bounds(new ScreenCoordinate(startX + PANEL_PADDING, topBarY, ticketChipW, TOP_BAR_H));
        ticketLayoutToggleButton.text(Text.literal(ticketStr));
        ticketLayoutToggleButton.paddingLeft(6);
        ticketLayoutToggleButton.paddingRight(6);
        ticketLayoutToggleButton.paddingTop(Math.max(0, (TOP_BAR_H - 9) / 2));
        ticketLayoutToggleButton.paddingBottom(Math.max(0, (TOP_BAR_H - 9) / 2));
        ticketLayoutToggleButton.borderWidth(0);
        ticketLayoutToggleButton.radius(4);
        ticketLayoutToggleButton.onLongPress(TICKET_LONG_PRESS_MS, b -> togglePanelMode());
        addWidget(ticketLayoutToggleButton);

        int addY = headerRowY + (HEADER_ROW_H - ADD_BTN_SIZE) / 2;
        if (panelMode == EnumWaypointPanelMode.THREE_COLUMNS) {
            addHomeButton = new ButtonWidget(this);
            addHomeButton.id("add_home");
            addHomeButton.bounds(new ScreenCoordinate(startX + panelWidth - PANEL_PADDING - ADD_BTN_SIZE, addY, ADD_BTN_SIZE, ADD_BTN_SIZE));
            addHomeButton.presetStyle(ButtonWidget.PresetStyle.PLUS);
            addHomeButton.radius(ADD_BTN_SIZE / 4f);
            addHomeButton.padding(1);
            addHomeButton.onClick(b -> openAddHomeDialog());
            addWidget(addHomeButton);

            addStageButton = new ButtonWidget(this);
            addStageButton.id("add_stage");
            addStageButton.bounds(new ScreenCoordinate(startX + panelWidth + GAP_H + panelWidth - PANEL_PADDING - ADD_BTN_SIZE, addY, ADD_BTN_SIZE, ADD_BTN_SIZE));
            addStageButton.presetStyle(ButtonWidget.PresetStyle.PLUS);
            addStageButton.radius(ADD_BTN_SIZE / 4f);
            addStageButton.padding(1);
            addStageButton.onClick(b -> openAddStageDialog());
            addWidget(addStageButton);

            homeScrollbar = buildColumnScrollbar(0, homeItems);
            stageScrollbar = buildColumnScrollbar(1, stageItems);
            backScrollbar = buildColumnScrollbar(2, backItems);
        } else {
            int hdrPad = PANEL_PADDING;
            int tabBarInner = footerW - 2 * hdrPad;
            int gapTabs = 4;
            int tabW = (tabBarInner - ADD_BTN_SIZE - gapTabs) / 3 - 1;
            int tx = startX + hdrPad;
            int tabY = headerRowY + (HEADER_ROW_H - 18) / 2;
            tabPrivateButton = new ButtonWidget(this);
            tabPrivateButton.id("tab_private");
            tabPrivateButton.bounds(new ScreenCoordinate(tx, tabY, tabW, 18));
            tabPrivateButton.text(NarcissusComponent.get().transClientAuto("private"));
            tabPrivateButton.radius(TAB_BTN_CORNER_R, 0f, TAB_BTN_CORNER_R, 0f);
            tabPrivateButton.onClick(b -> onTabSelected(WaypointListTab.PRIVATE));
            addWidget(tabPrivateButton);

            tabPublicButton = new ButtonWidget(this);
            tabPublicButton.id("tab_public");
            tabPublicButton.bounds(new ScreenCoordinate(tx + (tabW + 1), tabY, tabW, 18));
            tabPublicButton.text(NarcissusComponent.get().transClientAuto("public"));
            tabPublicButton.radius(0);
            tabPublicButton.onClick(b -> onTabSelected(WaypointListTab.PUBLIC));
            addWidget(tabPublicButton);

            tabFootprintsButton = new ButtonWidget(this);
            tabFootprintsButton.id("tab_footprints");
            tabFootprintsButton.bounds(new ScreenCoordinate(tx + 2 * (tabW + 1), tabY, tabW, 18));
            tabFootprintsButton.text(NarcissusComponent.get().transClientAuto("footprints"));
            tabFootprintsButton.radius(0f, TAB_BTN_CORNER_R, 0f, TAB_BTN_CORNER_R);
            tabFootprintsButton.onClick(b -> onTabSelected(WaypointListTab.FOOTPRINTS));
            addWidget(tabFootprintsButton);

            tabAddButton = new ButtonWidget(this);
            tabAddButton.id("tab_add");
            tabAddButton.bounds(new ScreenCoordinate(startX + footerW - hdrPad - ADD_BTN_SIZE, addY, ADD_BTN_SIZE, ADD_BTN_SIZE));
            tabAddButton.presetStyle(ButtonWidget.PresetStyle.PLUS);
            tabAddButton.radius(ADD_BTN_SIZE / 4f);
            tabAddButton.padding(1);
            tabAddButton.onClick(b -> onTabAddClick());
            addWidget(tabAddButton);

            tabListScrollbar = buildListScrollbarAt(startX, activeTabItems(), "scrollbar_tab");
        }

        int footerCenterGap = 16;
        int teleportBtnX;
        if (panelMode == EnumWaypointPanelMode.TAB_SINGLE) {
            teleportBtnX = footerX + (footerW - TELEPORT_BTN_W) / 2;
        } else {
            int footerSideW = (footerW - 2 * FOOTER_PAD_H - TELEPORT_BTN_W - 2 * footerCenterGap) / 2;
            teleportBtnX = footerX + FOOTER_PAD_H + footerSideW + footerCenterGap;
        }
        int teleportBtnY = footerY + (footerPanelHeight - TELEPORT_BTN_H) / 2;
        teleportButton = new ButtonWidget(this);
        teleportButton.id("teleport");
        teleportButton.bounds(new ScreenCoordinate(teleportBtnX, teleportBtnY, TELEPORT_BTN_W, TELEPORT_BTN_H));
        teleportButton.text(NarcissusComponent.get().transClientAuto("teleport_btn"));
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
        deleteCancelButton.text(NarcissusComponent.get().transClientAuto("cancel"));
        deleteCancelButton.radius(4);
        deleteCancelButton.visible(false);
        deleteCancelButton.onClick(b -> deleteConfirmItem = null);
        addWidget(deleteCancelButton);

        deleteConfirmButton = new ButtonWidget(this);
        deleteConfirmButton.id("delete_confirm");
        deleteConfirmButton.bounds(new ScreenCoordinate(deleteX, btnY, deleteW, 20));
        deleteConfirmButton.text(NarcissusComponent.get().transClientAuto("delete"));
        deleteConfirmButton.radius(4);
        deleteConfirmButton.visible(false);
        deleteConfirmButton.onClick(b -> {
            if (deleteConfirmItem != null) {
                doActualDelete(deleteConfirmItem);
                deleteConfirmItem = null;
            }
        });
        addWidget(deleteConfirmButton);

        applySearchFilter();
    }

    private ScrollbarWidget buildColumnScrollbar(int columnIndex, List<WaypointEntry> items) {
        int px = startX + columnIndex * (panelWidth + GAP_H);
        return buildListScrollbarAt(px, items, "scrollbar_" + columnIndex);
    }

    private ScrollbarWidget buildListScrollbarAt(int panelOriginX, List<WaypointEntry> items, String barId) {
        int listX = panelOriginX + PANEL_PADDING;
        int maxScroll = Math.max(0, items.size() - visibleRowCount);

        ScrollbarWidget bar = new ScrollbarWidget(this);
        bar.id(barId);
        bar.bounds(new ScreenCoordinate(listX + listBodyW - SCROLLBAR_WIDTH, listAreaY, SCROLLBAR_WIDTH, listHeight));
        bar.orientation(EnumOrientation.VERTICAL);
        bar.minValue(0);
        bar.maxValue(maxScroll);
        bar.visibleSize(visibleRowCount);
        bar.scrollStep(1.0);
        bar.addScrollHoverArea(new ScreenCoordinate(listX, listAreaY, listBodyW, listHeight));
        addWidget(bar);
        return bar;
    }

    private List<WaypointEntry> activeTabItems() {
        switch (activeTab) {
            case PUBLIC:
                return stageItems;
            case FOOTPRINTS:
                return backItems;
            default:
                return homeItems;
        }
    }

    private void onTabSelected(WaypointListTab tab) {
        activeTab = tab;
        if (tabListScrollbar != null) {
            tabListScrollbar.value(0);
        }
        syncScrollbarLimits();
        ensureSelectionVisible();
    }

    private void onTabAddClick() {
        if (activeTab == WaypointListTab.PRIVATE) {
            openAddHomeDialog();
        } else if (activeTab == WaypointListTab.PUBLIC) {
            openAddStageDialog();
        }
    }

    private void togglePanelMode() {
        if (panelMode == EnumWaypointPanelMode.THREE_COLUMNS) {
            panelMode = EnumWaypointPanelMode.TAB_SINGLE;
            inferActiveTabFromSelection();
        } else {
            panelMode = EnumWaypointPanelMode.THREE_COLUMNS;
        }
        ClientConfig.RootView cfg = ClientConfig.get();
        cfg.client().waypointScreenPanelMode(panelMode);
        cfg.save();
        refreshWidget();
    }

    private void inferActiveTabFromSelection() {
        if (selectedItem == null) {
            activeTab = WaypointListTab.PRIVATE;
            return;
        }
        switch (selectedItem.type) {
            case STAGE:
                activeTab = WaypointListTab.PUBLIC;
                break;
            case BACK:
                activeTab = WaypointListTab.FOOTPRINTS;
                break;
            default:
                activeTab = WaypointListTab.PRIVATE;
        }
    }

    private void applyTabButtonStyle(ButtonWidget btn, boolean selected, BaniraColorConfig theme) {
        if (selected) {
            btn.bgColor(ColorUtils.applyAlphaToArgb(theme.accent(), 0x50));
            btn.hoverBgColor(ColorUtils.applyAlphaToArgb(theme.accent(), 0x68));
        } else {
            btn.bgColor(theme.bgTertiary());
            btn.hoverBgColor(ColorUtils.applyAlphaToArgb(theme.bgSecondary(), 0x90));
        }
    }

    private void syncScrollbarLimits() {
        if (panelMode == EnumWaypointPanelMode.THREE_COLUMNS) {
            syncOneScrollbar(homeScrollbar, homeItems);
            syncOneScrollbar(stageScrollbar, stageItems);
            syncOneScrollbar(backScrollbar, backItems);
        } else {
            syncOneScrollbar(tabListScrollbar, activeTabItems());
        }
    }

    private void syncOneScrollbar(ScrollbarWidget bar, List<WaypointEntry> items) {
        if (bar == null) {
            return;
        }
        double v = bar.value();
        double maxScroll = Math.max(0, items.size() - visibleRowCount);
        bar.maxValue(maxScroll);
        bar.value(Math.min(v, maxScroll));
    }

    private void applySearchFilter() {
        String raw = searchInput != null ? searchInput.value() : "";
        String q = raw.trim().toLowerCase(Locale.ROOT);
        filterInto(homeItemsAll, homeItems, q);
        filterInto(stageItemsAll, stageItems, q);
        filterInto(backItemsAll, backItems, q);
        syncScrollbarLimits();
        ensureSelectionVisible();
    }

    private static void filterInto(List<WaypointEntry> src, List<WaypointEntry> dest, String qLower) {
        dest.clear();
        if (qLower.isEmpty()) {
            dest.addAll(src);
            return;
        }
        for (WaypointEntry e : src) {
            if (matchesFilter(e, qLower)) {
                dest.add(e);
            }
        }
    }

    private static boolean matchesFilter(WaypointEntry e, String qLower) {
        if (e.name.toLowerCase(Locale.ROOT).contains(qLower)) {
            return true;
        }
        if (e.getDetailTypeName().toLowerCase(Locale.ROOT).contains(qLower)) {
            return true;
        }
        if (e.getCoordinateName().toLowerCase(Locale.ROOT).contains(qLower)) {
            return true;
        }
        if (e.getDimensionName().toLowerCase(Locale.ROOT).contains(qLower)) {
            return true;
        }
        if (e.safeWorldCoordinate != null) {
            String dimId = e.safeWorldCoordinate.dimension().location().toString().toLowerCase(Locale.ROOT);
            if (dimId.contains(qLower)) {
                return true;
            }
        }
        return false;
    }

    private void ensureSelectionVisible() {
        if (selectedItem == null) {
            pickFirstSelection();
            return;
        }
        if (panelMode == EnumWaypointPanelMode.TAB_SINGLE) {
            if (indexInList(activeTabItems(), selectedItem) >= 0) {
                return;
            }
            pickFirstSelection();
            return;
        }
        if (indexInList(homeItems, selectedItem) >= 0
                || indexInList(stageItems, selectedItem) >= 0
                || indexInList(backItems, selectedItem) >= 0) {
            return;
        }
        pickFirstSelection();
    }

    private void pickFirstSelection() {
        if (panelMode == EnumWaypointPanelMode.TAB_SINGLE) {
            List<WaypointEntry> cur = activeTabItems();
            selectedItem = cur.isEmpty() ? null : cur.get(0);
            return;
        }
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

    private static int indexInList(List<WaypointEntry> list, WaypointEntry e) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == e) {
                return i;
            }
        }
        return -1;
    }

    private boolean canAddHomeClient() {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        if (!CommonConfig.get().featureSwitch().switchTpHome()) {
            return false;
        }
        var p = minecraft.player;
        int need = NarcissusUtils.getCommandPermissionLevel(EnumCommandType.TP_HOME);
        if (!p.hasPermissions(need) && !NarcissusUtils.hasVirtualPermission(p, EnumCommandType.TP_HOME)) {
            return false;
        }
        return homeItemsAll.size() < CommonConfig.get().general().teleportHomeLimit();
    }

    private boolean canAddStageClient() {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        if (!CommonConfig.get().featureSwitch().switchTpStage()) {
            return false;
        }
        var p = minecraft.player;
        int need = NarcissusUtils.getCommandPermissionLevel(EnumCommandType.SET_STAGE);
        return p.hasPermissions(need) || NarcissusUtils.hasVirtualPermission(p, EnumCommandType.SET_STAGE);
    }

    private void openAddHomeDialog() {
        if (minecraft == null || minecraft.player == null || !canAddHomeClient()) {
            return;
        }
        Text t = Text.literal(NarcissusComponent.get().transClientAuto("waypoint_add_home_title").toString());
        InputFormScreen.Args args = new InputFormScreen.Args()
                .setParentScreen(this)
                .setTitle(t)
                .addWidget(new InputFormScreen.Widget()
                        .name("home_name")
                        .title(Text.literal(NarcissusComponent.get().transClientAuto("waypoint_field_name").toString()))
                        .hint(Text.literal(NarcissusComponent.get().transClientAuto("waypoint_name_hint").toString()))
                        .allowEmpty(false)
                        .validator(r -> {
                            String n = r.value("home_name");
                            if (StringUtils.isNullOrEmptyEx(n == null ? null : n.trim())) {
                                return NarcissusComponent.get().transClientAuto("waypoint_err_empty_name").toString();
                            }
                            return "";
                        }))
                .setCallback(results -> {
                    String n = results.value("home_name");
                    if (n != null) {
                        n = n.trim();
                    }
                    PacketUtils.sendPacketToServer(NetworkInit.INSTANCE, new WaypointAddHomeToServer(n == null ? "" : n));
                });
        minecraft.setScreen(new InputFormScreen(args));
    }

    private void openAddStageDialog() {
        if (minecraft == null || minecraft.player == null || !canAddStageClient()) {
            return;
        }
        String dim = minecraft.player.level.dimension().location().toString();
        String dx = NumberUtils.toFixedEx(minecraft.player.getX(), 1);
        String dy = NumberUtils.toFixedEx(minecraft.player.getY(), 1);
        String dz = NumberUtils.toFixedEx(minecraft.player.getZ(), 1);
        Text title = Text.literal(NarcissusComponent.get().transClientAuto("waypoint_add_stage_title").toString());
        InputFormScreen.Args args = new InputFormScreen.Args()
                .setParentScreen(this)
                .setTitle(title)
                .addWidget(new InputFormScreen.Widget()
                        .name("st_name")
                        .title(Text.literal(NarcissusComponent.get().transClientAuto("waypoint_field_name").toString()))
                        .allowEmpty(false)
                        .validator(r -> StringUtils.isNullOrEmptyEx(r.value("st_name") == null ? null : r.value("st_name").trim())
                                ? NarcissusComponent.get().transClientAuto("waypoint_err_empty_name").toString() : ""))
                .addWidget(new InputFormScreen.Widget()
                        .name("st_dim")
                        .title(Text.literal(NarcissusComponent.get().transClientAuto("waypoint_field_dimension").toString()))
                        .defaultValue(dim)
                        .allowEmpty(false)
                        .validator(r -> StringUtils.isNullOrEmptyEx(r.value("st_dim") == null ? null : r.value("st_dim").trim())
                                ? NarcissusComponent.get().transClientAuto("waypoint_err_empty_dimension").toString() : ""))
                .addWidget(new InputFormScreen.Widget()
                        .name("st_x")
                        .title(Text.literal(NarcissusComponent.get().transClientAuto("waypoint_field_x").toString()))
                        .defaultValue(dx)
                        .allowEmpty(false)
                        .validator(r -> parseCoordComponent(r.value("st_x"), "waypoint_err_coord")))
                .addWidget(new InputFormScreen.Widget()
                        .name("st_y")
                        .title(Text.literal(NarcissusComponent.get().transClientAuto("waypoint_field_y").toString()))
                        .defaultValue(dy)
                        .allowEmpty(false)
                        .validator(r -> parseCoordComponent(r.value("st_y"), "waypoint_err_coord")))
                .addWidget(new InputFormScreen.Widget()
                        .name("st_z")
                        .title(Text.literal(NarcissusComponent.get().transClientAuto("waypoint_field_z").toString()))
                        .defaultValue(dz)
                        .allowEmpty(false)
                        .validator(r -> parseCoordComponent(r.value("st_z"), "waypoint_err_coord")))
                .setCallback(results -> {
                    String name = results.value("st_name").trim();
                    String dimension = results.value("st_dim").trim();
                    double x = Double.parseDouble(results.value("st_x").trim().replace(',', '.'));
                    double y = Double.parseDouble(results.value("st_y").trim().replace(',', '.'));
                    double z = Double.parseDouble(results.value("st_z").trim().replace(',', '.'));
                    PacketUtils.sendPacketToServer(NetworkInit.INSTANCE, new WaypointAddStageToServer(name, dimension, x, y, z));
                });
        minecraft.setScreen(new InputFormScreen(args));
    }

    private static String parseCoordComponent(String s, String errKey) {
        if (StringUtils.isNullOrEmptyEx(s)) {
            return NarcissusComponent.get().transClientAuto(errKey).toString();
        }
        try {
            Double.parseDouble(s.trim().replace(',', '.'));
            return "";
        } catch (NumberFormatException e) {
            return NarcissusComponent.get().transClientAuto(errKey).toString();
        }
    }

    @Override
    protected void onRender(@Nonnull PoseStack stack, float partialTicks) {
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
            boolean homeScroll = !dialogOpen && panelMode == EnumWaypointPanelMode.THREE_COLUMNS && homeItems.size() > visibleRowCount;
            homeScrollbar.visible(homeScroll);
            homeScrollbar.enabled(homeScroll);
            boolean stageScroll = !dialogOpen && panelMode == EnumWaypointPanelMode.THREE_COLUMNS && stageItems.size() > visibleRowCount;
            stageScrollbar.visible(stageScroll);
            stageScrollbar.enabled(stageScroll);
            boolean backScroll = !dialogOpen && panelMode == EnumWaypointPanelMode.THREE_COLUMNS && backItems.size() > visibleRowCount;
            backScrollbar.visible(backScroll);
            backScrollbar.enabled(backScroll);
        }
        if (tabListScrollbar != null) {
            boolean tabScroll = !dialogOpen && panelMode == EnumWaypointPanelMode.TAB_SINGLE && activeTabItems().size() > visibleRowCount;
            tabListScrollbar.visible(tabScroll);
            tabListScrollbar.enabled(tabScroll);
        }
        if (searchInput != null) {
            searchInput.enabled(!dialogOpen);
        }
        if (addHomeButton != null) {
            addHomeButton.enabled(!dialogOpen && canAddHomeClient());
            addHomeButton.visible(!dialogOpen && panelMode == EnumWaypointPanelMode.THREE_COLUMNS);
        }
        if (addStageButton != null) {
            addStageButton.enabled(!dialogOpen && canAddStageClient());
            addStageButton.visible(!dialogOpen && panelMode == EnumWaypointPanelMode.THREE_COLUMNS);
        }
        if (tabPrivateButton != null) {
            tabPrivateButton.visible(!dialogOpen && panelMode == EnumWaypointPanelMode.TAB_SINGLE);
            tabPrivateButton.enabled(!dialogOpen);
            applyTabButtonStyle(tabPrivateButton, activeTab == WaypointListTab.PRIVATE, theme);
        }
        if (tabPublicButton != null) {
            tabPublicButton.visible(!dialogOpen && panelMode == EnumWaypointPanelMode.TAB_SINGLE);
            tabPublicButton.enabled(!dialogOpen);
            applyTabButtonStyle(tabPublicButton, activeTab == WaypointListTab.PUBLIC, theme);
        }
        if (tabFootprintsButton != null) {
            tabFootprintsButton.visible(!dialogOpen && panelMode == EnumWaypointPanelMode.TAB_SINGLE);
            tabFootprintsButton.enabled(!dialogOpen);
            applyTabButtonStyle(tabFootprintsButton, activeTab == WaypointListTab.FOOTPRINTS, theme);
        }
        if (tabAddButton != null) {
            tabAddButton.visible(!dialogOpen && panelMode == EnumWaypointPanelMode.TAB_SINGLE);
            boolean addOk = activeTab == WaypointListTab.PRIVATE && canAddHomeClient()
                    || activeTab == WaypointListTab.PUBLIC && canAddStageClient();
            tabAddButton.enabled(addOk);
        }
        if (ticketLayoutToggleButton != null) {
            ticketLayoutToggleButton.visible(!dialogOpen);
            ticketLayoutToggleButton.enabled(!dialogOpen);
            String ticketStr = NarcissusComponent.get().transClientAuto("teleport_card").toString() + ": " + ticketCount;
            ticketLayoutToggleButton.text(Text.literal(ticketStr));
            ticketLayoutToggleButton.bgColor(theme.panelBg());
            ticketLayoutToggleButton.hoverBgColor(theme.panelBg());
            ticketLayoutToggleButton.focusedBgColor(theme.panelBg());
            ticketLayoutToggleButton.pressedBgColor(ColorUtils.applyAlphaToArgb(theme.accent(), 0x62));
            ticketLayoutToggleButton.borderWidth(0);
            ticketLayoutToggleButton.textColor(theme.textPrimary());
            ticketLayoutToggleButton.hoverTextColor(theme.textPrimary());
            ticketLayoutToggleButton.focusedTextColor(theme.textPrimary());
            ticketLayoutToggleButton.pressedTextColor(theme.textPrimary());
        }
        if (teleportButton != null) {
            teleportButton.visible(!dialogOpen);
            teleportButton.enabled(selectedItem != null && selectedItem.canTeleport);
        }
        if (deleteCancelButton != null) {
            deleteCancelButton.visible(dialogOpen);
            deleteConfirmButton.visible(dialogOpen);
        }

        drawTopBarAndDividers(stack, theme);
        drawColumnHeaders(stack, theme);
        drawListColumns(stack, theme, mouseX, mouseY);
        drawFooter(stack, theme);

        if (hoveredItem != null && !dialogOpen) {
            addDeferredTooltipRender(s -> drawCustomTooltip(s, theme, hoveredItem, mouseX, mouseY));
        }

        if (dialogOpen) {
            drawDeleteConfirmOverlay(stack, theme);
        }

        renderWidgets(stack, partialTicks);
    }

    private void drawTopBarAndDividers(PoseStack stack, BaniraColorConfig theme) {
        ShapeDrawArgs topBg = ShapeDrawArgs.rect(stack, startX, topBarY, footerW, TOP_BAR_H, theme.panelBg());
        topBg.rect().radius(6f, 6f, 0f, 0f);
        BaseShapeWidget.drawShape(topBg);

        ShapeDrawArgs div0 = ShapeDrawArgs.rect(stack, startX, topBarY + TOP_BAR_H, footerW, DIVIDER, theme.border());
        BaseShapeWidget.drawShape(div0);

        ShapeDrawArgs div1 = ShapeDrawArgs.rect(stack, startX, headerRowY + HEADER_ROW_H, footerW, DIVIDER, theme.border());
        BaseShapeWidget.drawShape(div1);

        ShapeDrawArgs div2 = ShapeDrawArgs.rect(stack, startX, listAreaY + listHeight, footerW, DIVIDER, theme.border());
        BaseShapeWidget.drawShape(div2);
    }

    private void drawColumnHeaders(PoseStack stack, BaniraColorConfig theme) {
        if (panelMode == EnumWaypointPanelMode.THREE_COLUMNS) {
            for (int c = 0; c < 3; c++) {
                int x = startX + c * (panelWidth + GAP_H);
                ShapeDrawArgs h = ShapeDrawArgs.rect(stack, x, headerRowY, panelWidth, HEADER_ROW_H, theme.bgSecondary());
                h.rect().radius(0);
                BaseShapeWidget.drawShape(h);
                String title = c == 0
                        ? NarcissusComponent.get().transClientAuto("private").toString()
                        : c == 1
                        ? NarcissusComponent.get().transClientAuto("public").toString()
                        : NarcissusComponent.get().transClientAuto("footprints").toString();
                int titleY = headerRowY + (HEADER_ROW_H - font.lineHeight) / 2;
                font.draw(stack, title, x + PANEL_PADDING, titleY, theme.textPrimary());
            }
        } else {
            ShapeDrawArgs h = ShapeDrawArgs.rect(stack, startX, headerRowY, footerW, HEADER_ROW_H, theme.bgSecondary());
            h.rect().radius(0);
            BaseShapeWidget.drawShape(h);
        }
    }

    private void drawListColumns(PoseStack stack, BaniraColorConfig theme, int mouseX, int mouseY) {
        if (panelMode == EnumWaypointPanelMode.THREE_COLUMNS) {
            drawColumnList(stack, theme, startX, panelWidth, homeItems, homeScrollbar, false, false, mouseX, mouseY);
            drawColumnList(stack, theme, startX + panelWidth + GAP_H, panelWidth, stageItems, stageScrollbar, false, false, mouseX, mouseY);
            drawColumnList(stack, theme, startX + 2 * (panelWidth + GAP_H), panelWidth, backItems, backScrollbar, true, false, mouseX, mouseY);
        } else {
            drawColumnList(stack, theme, startX, footerW, activeTabItems(), tabListScrollbar, activeTab == WaypointListTab.FOOTPRINTS, true, mouseX, mouseY);
        }
    }

    private void drawColumnList(PoseStack stack, BaniraColorConfig theme, int listOriginX, int listPanelOuterW, List<WaypointEntry> items,
                                ScrollbarWidget scrollbar, boolean isBackPanel, boolean appendDistanceAfterCoords, int mouseX, int mouseY) {
        int x = listOriginX;
        ShapeDrawArgs panelShape = ShapeDrawArgs.rect(stack, x, listAreaY, listPanelOuterW, listHeight, theme.panelBg());
        panelShape.rect().radius(0);
        BaseShapeWidget.drawShape(panelShape);

        int listX = x + PANEL_PADDING;
        boolean scrollNeeded = items.size() > visibleRowCount;
        int cw = listBodyW - (scrollNeeded ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);
        int scroll = scrollbar != null
                ? (int) Math.round(Mth.clamp(scrollbar.value(), 0, Math.max(0, items.size() - visibleRowCount)))
                : 0;

        int innerTop = listAreaY + LIST_PADDING_V;
        int rowSlots = scrollNeeded ? visibleRowCount : items.size();

        for (int i = 0; i < rowSlots; i++) {
            int idx = i + scroll;
            if (idx >= items.size()) {
                break;
            }

            WaypointEntry item = items.get(idx);
            int itemY = innerTop + i * ITEM_HEIGHT;
            int rowH = ITEM_HEIGHT;

            int rowDrawH = Math.max(1, rowH - 1);
            boolean hover = mouseX >= listX && mouseX < listX + cw && mouseY >= itemY && mouseY < itemY + rowH;
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

            ShapeDrawArgs rowRect = ShapeDrawArgs.rect(stack, listX, itemY, cw, rowDrawH, rowBg);
            rowRect.rect().radius(4);
            BaseShapeWidget.drawShape(rowRect);

            if (item.canTeleport) {
                if (selected) {
                    ShapeDrawArgs accentBar = ShapeDrawArgs.rect(stack, listX, itemY, 3, rowDrawH, theme.accent());
                    BaseShapeWidget.drawShape(accentBar);
                } else if (hover) {
                    ShapeDrawArgs softBar = ShapeDrawArgs.rect(stack, listX, itemY, 2, rowDrawH, ColorUtils.applyAlphaToArgb(theme.accent(), 0x90));
                    BaseShapeWidget.drawShape(softBar);
                }
            } else if (selected) {
                ShapeDrawArgs disBar = ShapeDrawArgs.rect(stack, listX, itemY, 3, rowDrawH, theme.textDisabled());
                BaseShapeWidget.drawShape(disBar);
            }

            String titleLine = item.name;
            if (appendDistanceAfterCoords && item.recordTime != null) {
                titleLine = item.name + "  " + DateUtils.toString(item.recordTime, "yy-MM-dd HH:mm:ss");
            }
            font.draw(stack, font.plainSubstrByWidth(titleLine, cw - 18), listX + 3, itemY + 2, textColor);
            String meta = item.getDimensionName() + " " + item.getCoordinateName();
            if (appendDistanceAfterCoords) {
                meta = meta + "  " + formatItemDistanceMeters(item);
            }
            font.draw(stack, font.plainSubstrByWidth(meta, cw - 18), listX + 3, itemY + 12, metaColor);

            if ((item.type == WaypointEntry.Type.HOME || item.type == WaypointEntry.Type.STAGE) && item.canTeleport) {
                int delX = listX + cw - 16;
                int delY = itemY + (rowH - 10) / 2;
                boolean delHover = mouseX >= delX && mouseX <= delX + 14 && mouseY >= delY && mouseY < delY + 10;
                font.draw(stack, "×", delX + 2, delY, delHover ? theme.error() : theme.textHint());
            }
        }
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

        if (panelMode == EnumWaypointPanelMode.THREE_COLUMNS) {
            if (checkPanelClick(mouseX, mouseY, startX + PANEL_PADDING, listAreaY + LIST_PADDING_V, homeItems, homeScrollbar)) {
                eventArgs.consumed(true);
            } else if (checkPanelClick(mouseX, mouseY, startX + panelWidth + GAP_H + PANEL_PADDING, listAreaY + LIST_PADDING_V, stageItems, stageScrollbar)) {
                eventArgs.consumed(true);
            } else if (checkPanelClick(mouseX, mouseY, startX + (panelWidth + GAP_H) * 2 + PANEL_PADDING, listAreaY + LIST_PADDING_V, backItems, backScrollbar)) {
                eventArgs.consumed(true);
            }
        } else if (checkPanelClick(mouseX, mouseY, startX + PANEL_PADDING, listAreaY + LIST_PADDING_V, activeTabItems(), tabListScrollbar)) {
            eventArgs.consumed(true);
        }

        super.onMouseClicked(eventArgs);
    }

    @Override
    protected void onKeyPressed(KeyPressedHandleArgs eventArgs) {
        if (deleteConfirmItem != null && eventArgs.key() == GLFWKey.GLFW_KEY_ESCAPE) {
            deleteConfirmItem = null;
            eventArgs.consumed(true);
            return;
        }
        super.onKeyPressed(eventArgs);
    }

    private void loadData() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        PlayerTeleportData data = PlayerTeleportData.getData(minecraft.player);

        homeItemsAll.clear();
        for (KeyValue<String, String> key : data.getHomeCoordinate().keySet()) {
            homeItemsAll.add(new WaypointEntry(WaypointEntry.Type.HOME, key.value(),
                    data.getHomeCoordinate().get(key), true, null, null));
        }

        stageItemsAll.clear();
        for (Map.Entry<KeyValue<String, String>, SafeWorldCoordinate> entry : ClientStageData.getStageCoordinate().entrySet()) {
            stageItemsAll.add(new WaypointEntry(WaypointEntry.Type.STAGE, entry.getKey().value(),
                    entry.getValue(), true, null, null));
        }

        backItemsAll.clear();
        List<TeleportRecord> records = data.getTeleportRecords().stream()
                .filter(r -> r.getBefore() != null)
                .toList();
        Set<String> seenRecordTypes = new HashSet<>();
        for (int i = records.size() - 1; i >= 0; i--) {
            TeleportRecord record = records.get(i);
            String recordTypeName = record.getTeleportType().name();
            boolean canTp = !seenRecordTypes.contains(recordTypeName);
            if (canTp) {
                seenRecordTypes.add(recordTypeName);
            }
            backItemsAll.add(new WaypointEntry(WaypointEntry.Type.BACK, recordTypeName, record.getBefore(),
                    canTp, recordTypeName, record.getTeleportTime()));
        }

        ticketCount = data.getTeleportCard();
        applySearchFilter();
    }

    private boolean checkPanelClick(double mouseX, double mouseY, int listX, int listY, List<WaypointEntry> items, ScrollbarWidget bar) {
        boolean scrollNeeded = items.size() > visibleRowCount;
        int cw = listBodyW - (scrollNeeded ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);
        int scroll = bar != null ? (int) Math.round(Mth.clamp(bar.value(), 0, Math.max(0, items.size() - visibleRowCount))) : 0;

        int rowSlots = scrollNeeded ? visibleRowCount : items.size();

        for (int i = 0; i < rowSlots; i++) {
            int idx = i + scroll;
            if (idx >= items.size()) {
                break;
            }

            WaypointEntry item = items.get(idx);
            int itemY = listY + i * ITEM_HEIGHT;
            int rowH = ITEM_HEIGHT;

            if (mouseX >= listX && mouseX < listX + cw && mouseY >= itemY && mouseY < itemY + rowH - 1) {
                if ((item.type == WaypointEntry.Type.HOME || item.type == WaypointEntry.Type.STAGE) && item.canTeleport) {
                    int delX = listX + cw - 16;
                    if (mouseX >= delX) {
                        deleteConfirmItem = item;
                        lastDoubleClickEntry = null;
                        return true;
                    }
                }
                handleRowActivate(item);
                return true;
            }
        }
        return false;
    }

    private void handleRowActivate(WaypointEntry item) {
        selectedItem = item;
        long now = System.currentTimeMillis();
        if (lastDoubleClickEntry == item && now - lastDoubleClickTime < DOUBLE_CLICK_MS) {
            lastDoubleClickEntry = null;
            lastDoubleClickTime = 0;
            if (item.canTeleport) {
                selectedItem = item;
                onTeleportClick();
            }
        } else {
            lastDoubleClickEntry = item;
            lastDoubleClickTime = now;
        }
    }

    private void updateHoveredItem(int mouseX, int mouseY) {
        hoveredItem = null;
        int listY = listAreaY + LIST_PADDING_V;

        if (panelMode == EnumWaypointPanelMode.TAB_SINGLE) {
            int listX = startX + PANEL_PADDING;
            List<WaypointEntry> items = activeTabItems();
            ScrollbarWidget bar = tabListScrollbar;
            boolean scrollNeeded = items.size() > visibleRowCount;
            int cw = listBodyW - (scrollNeeded ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);
            int scroll = bar != null ? (int) Math.round(Mth.clamp(bar.value(), 0, Math.max(0, items.size() - visibleRowCount))) : 0;
            int rowSlots = scrollNeeded ? visibleRowCount : items.size();
            for (int i = 0; i < rowSlots; i++) {
                int idx = i + scroll;
                if (idx >= items.size()) {
                    break;
                }
                int itemY = listY + i * ITEM_HEIGHT;
                int rowH = ITEM_HEIGHT;
                if (mouseX >= listX && mouseX < listX + cw && mouseY >= itemY && mouseY < itemY + rowH) {
                    hoveredItem = items.get(idx);
                    return;
                }
            }
            return;
        }

        for (int p = 0; p < 3; p++) {
            int listX = startX + p * (panelWidth + GAP_H) + PANEL_PADDING;
            List<WaypointEntry> items = p == 0 ? homeItems : (p == 1 ? stageItems : backItems);
            ScrollbarWidget bar = p == 0 ? homeScrollbar : (p == 1 ? stageScrollbar : backScrollbar);
            boolean scrollNeeded = items.size() > visibleRowCount;
            int cw = listBodyW - (scrollNeeded ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);
            int scroll = bar != null ? (int) Math.round(Mth.clamp(bar.value(), 0, Math.max(0, items.size() - visibleRowCount))) : 0;
            int rowSlots = scrollNeeded ? visibleRowCount : items.size();

            for (int i = 0; i < rowSlots; i++) {
                int idx = i + scroll;
                if (idx >= items.size()) {
                    break;
                }
                int itemY = listY + i * ITEM_HEIGHT;
                int rowH = ITEM_HEIGHT;
                if (mouseX >= listX && mouseX < listX + cw && mouseY >= itemY && mouseY < itemY + rowH) {
                    hoveredItem = items.get(idx);
                    return;
                }
            }
        }
    }

    private void drawCustomTooltip(PoseStack stack, BaniraColorConfig theme, WaypointEntry item, int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        lines.add(item.name);
        lines.add(item.getDetailTypeName() + " | " + item.getDimensionName());
        lines.add(item.getCoordinateName());
        if (!item.canTeleport) {
            lines.add(NarcissusComponent.get().transClientAuto("back_record_used").toString());
        }
        String content = String.join("\n", lines);
        Text tooltipText = Text.literal(content).stack(stack).font(font).color(Color.argb(theme.textPrimary()));
        FontDrawArgs args = FontDrawArgs.ofPopo(tooltipText)
                .x(mouseX).y(mouseY)
                .inScreen(true);
        TooltipWidget.drawPopupMessage(stack, args);
    }

    private void drawFooter(PoseStack stack, BaniraColorConfig theme) {
        ShapeDrawArgs footerBg = ShapeDrawArgs.rect(stack, footerX, footerY, footerW, footerPanelHeight, theme.panelBg());
        footerBg.rect().radius(0f, 0f, 6f, 6f);
        BaseShapeWidget.drawShape(footerBg);

        if (selectedItem == null) {
            return;
        }

        String costStr = calculateCostDisplay(selectedItem);
        if (StringUtils.isNullOrEmptyEx(costStr)) {
            costStr = NarcissusComponent.get().transClientAuto("cost_free").toString();
        }
        String costFull = NarcissusComponent.get().transClientAuto("waypoint_detail_cost").toString() + costStr;

        if (panelMode == EnumWaypointPanelMode.TAB_SINGLE) {
            drawTabFooterCostAroundTeleport(stack, theme.textSecondary(), costFull);
            return;
        }

        Text costText = Text.literal(costFull).stack(stack).font(font).color(Color.argb(theme.textSecondary()));

        int centerGap = 16;
        int leftZoneW = (footerW - 2 * FOOTER_PAD_H - TELEPORT_BTN_W - 2 * centerGap) / 2;
        int rightZoneW = leftZoneW;
        int leftX = footerX + FOOTER_PAD_H;
        int rightX = footerX + footerW - FOOTER_PAD_H - rightZoneW;
        int lineH = font.lineHeight + 1;
        int y0 = footerY + FOOTER_PAD_V;

        String dimStr = selectedItem.getDimensionName();
        String coordStr = selectedItem.getCoordinateName();
        String distanceStr = formatItemDistanceMeters(selectedItem);

        String sep = "  ";
        String line1 = selectedItem.getDetailTypeName() + sep + selectedItem.name;
        font.draw(stack, font.plainSubstrByWidth(line1, leftZoneW), leftX, y0, theme.textPrimary());
        y0 += lineH;

        String line2 = dimStr + sep + coordStr;
        font.draw(stack, font.plainSubstrByWidth(line2, leftZoneW), leftX, y0, theme.textHint());
        y0 += lineH;

        String timeStr = selectedItem.recordTime != null ? DateUtils.toString(selectedItem.recordTime, "yy-MM-dd HH:mm:ss") : "-";
        String line3 = distanceStr + sep + timeStr;
        font.draw(stack, font.plainSubstrByWidth(line3, leftZoneW), leftX, y0, theme.textHint());

        LabelWidget.drawLimitedText(FontDrawArgs.of(costText)
                .x(rightX).y(footerY + FOOTER_PAD_V)
                .maxWidth(rightZoneW)
                .align(EnumAlignment.END)
                .wrap(true)
                .inScreen(false)
                .position(EnumEllipsisPosition.END)
                .bgArgb(0).bgBorderRadius(0).bgBorderThickness(0)
                .paddingLeft(0).paddingRight(0).paddingTop(0).paddingBottom(0));
    }

    // region Tab footer cost layout

    private void drawTabFooterCostAroundTeleport(PoseStack stack, int textArgb, String costFull) {
        if (StringUtils.isNullOrEmptyEx(costFull)) {
            return;
        }
        int gap = 6;
        int btnL = footerX + (footerW - TELEPORT_BTN_W) / 2;
        int btnR = btnL + TELEPORT_BTN_W;
        int xL = footerX + FOOTER_PAD_H;
        int xR = btnR + gap;
        int wL = Math.max(10, btnL - gap - xL);
        int wR = Math.max(10, footerX + footerW - FOOTER_PAD_H - xR);
        int lh = font.lineHeight;
        int maxLines = Math.max(1, (footerPanelHeight - 2 * FOOTER_PAD_V + lh - 1) / lh);

        String rem = costFull;
        int y0 = footerY + FOOTER_PAD_V;
        String ell = "...";
        for (int i = 0; i < maxLines && !rem.isEmpty(); i++) {
            boolean lastRow = i == maxLines - 1;
            String l = wL > 0 ? font.plainSubstrByWidth(rem, wL) : "";
            if (l.isEmpty() && wL > 0 && !rem.isEmpty()) {
                break;
            }
            rem = consumeDrawnPrefix(rem, l);
            String r = "";
            if (!rem.isEmpty() && wR > 0) {
                r = font.plainSubstrByWidth(rem, wR);
                if (r.isEmpty() && !rem.isEmpty()) {
                    break;
                }
                rem = consumeDrawnPrefix(rem, r);
            }
            if (lastRow && !rem.isEmpty()) {
                if (wR > 0) {
                    r = font.plainSubstrByWidth(r + rem + ell, wR);
                } else if (wL > 0) {
                    l = font.plainSubstrByWidth(l + rem + ell, wL);
                }
                rem = "";
            }
            if (!l.isEmpty()) {
                font.draw(stack, l, xL, y0 + i * lh, textArgb);
            }
            if (!r.isEmpty()) {
                font.draw(stack, r, xR, y0 + i * lh, textArgb);
            }
        }
    }

    private static String consumeDrawnPrefix(String rem, String drawn) {
        if (drawn.isEmpty()) {
            return rem;
        }
        if (rem.startsWith(drawn)) {
            return rem.substring(drawn.length()).replaceFirst("^\\s+", "");
        }
        int n = Math.min(drawn.length(), rem.length());
        return rem.substring(n).replaceFirst("^\\s+", "");
    }

    // endregion Tab footer cost layout

    private String formatItemDistanceMeters(WaypointEntry item) {
        if (minecraft == null || minecraft.player == null || item.safeWorldCoordinate == null
                || item.safeWorldCoordinate.dimension() != minecraft.player.level.dimension()) {
            return "∞m";
        }
        return NumberUtils.toFixedEx(item.safeWorldCoordinate.distanceFrom(new SafeWorldCoordinate(minecraft.player)), 1) + "m";
    }

    private void drawDeleteConfirmOverlay(PoseStack stack, BaniraColorConfig theme) {
        ShapeDrawArgs dim = ShapeDrawArgs.rect(stack, 0, 0, width, height, ColorUtils.applyAlphaToArgb(theme.bgQuaternary(), 0x78));
        BaseShapeWidget.drawShape(dim);

        ShapeDrawArgs dlg = ShapeDrawArgs.rect(stack, dlgX, dlgY, DIALOG_W, DIALOG_H, theme.panelBg());
        dlg.rect().radius(6);
        BaseShapeWidget.drawShape(dlg);

        ShapeDrawArgs dlgBorder = ShapeDrawArgs.rect(stack, dlgX, dlgY, DIALOG_W, DIALOG_H, theme.border());
        dlgBorder.rect().radius(6).border(1f);
        BaseShapeWidget.drawShape(dlgBorder);

        String title = NarcissusComponent.get().transClientAuto("del_confirm_title").toString();
        String msg = NarcissusComponent.get().transClientAuto("del_confirm_msg").toString();
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
        PacketUtils.sendPacketToServer(NetworkInit.INSTANCE, new WaypointTeleportToServer(type, name, dimension));
        super.previousScreen(null);
        onClose();
    }

    private void doActualDelete(WaypointEntry item) {
        if (item == null || item.safeWorldCoordinate == null || minecraft == null || minecraft.player == null) {
            return;
        }
        int typeOrdinal = item.type == WaypointEntry.Type.HOME ? 0 : 1;
        String dimension = item.safeWorldCoordinate.getDimensionResourceId();
        PacketUtils.sendPacketToServer(NetworkInit.INSTANCE, new WaypointDelToServer(typeOrdinal, item.name, dimension));
        if (item.type == WaypointEntry.Type.HOME) {
            homeItemsAll.removeIf(e -> e.name.equals(item.name) && e.safeWorldCoordinate != null && dimension.equals(e.safeWorldCoordinate.getDimensionResourceId()));
        } else if (item.type == WaypointEntry.Type.STAGE) {
            stageItemsAll.removeIf(e -> e.name.equals(item.name) && e.safeWorldCoordinate != null && dimension.equals(e.safeWorldCoordinate.getDimensionResourceId()));
        }
        applySearchFilter();
        if (selectedItem == item) {
            pickFirstSelection();
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
                lastPlayerPos.fromVec3(minecraft.player.position()).dimension(minecraft.player.level.dimension());
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
        @Nullable
        public final Date recordTime;

        public WaypointEntry(Type type, String name, SafeWorldCoordinate safeWorldCoordinate, boolean canTeleport, String recordType, @Nullable Date recordTime) {
            this.type = type;
            this.name = name;
            this.safeWorldCoordinate = safeWorldCoordinate;
            this.canTeleport = canTeleport;
            this.recordType = recordType;
            this.recordTime = recordTime;
        }

        public String getDetailTypeName() {
            switch (type) {
                case HOME:
                    return NarcissusComponent.get().transClientAuto("private").toString();
                case STAGE:
                    return NarcissusComponent.get().transClientAuto("public").toString();
                case BACK:
                    return NarcissusComponent.get().transClientAuto("footprints").toString();
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
                return NarcissusComponent.get().transClientAuto(key).toString();
            }
            return safeWorldCoordinate.dimension().location().toString();
        }

        public String getCoordinateName() {
            return safeWorldCoordinate != null ? String.format("(%s)", safeWorldCoordinate.toXyzIntString(",")) : "";
        }
    }
}
