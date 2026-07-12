package xin.vanilla.narcissus.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.Mth;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.PlayerSkinTextureUtils;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumPanelMode;
import xin.vanilla.narcissus.enums.EnumWhiteListMode;
import xin.vanilla.narcissus.network.packet.AccessListEditToServer;

import javax.annotation.Nonnull;
import java.util.*;

@Accessors(chain = true, fluent = true)
public class AccessListScreen extends BaniraScreen {

    // region Constants

    private static final int SCREEN_MARGIN = 8;
    private static final int GAP_H = 1;
    private static final int PANEL_PADDING = 6;
    private static final int HEADER_ROW_H = 22;
    private static final int TOP_BAR_H = 22;
    private static final int ITEM_HEIGHT = 24;
    private static final int MAX_VISIBLE_ITEMS = 7;
    private static final int LIST_ROWS_COMPACT = 5;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 2;
    private static final int LIST_PADDING_V = 2;
    private static final int FOOTER_HEIGHT = 40;
    /**
     * Tab 单栏时页脚仅一行提示
     */
    private static final int FOOTER_HEIGHT_TAB = 28;
    private static final int DIVIDER = 1;
    private static final int HEAD_SIZE = 16;
    private static final int HEAD_GAP_AFTER = 2;
    private static final float TAB_BTN_CORNER_R = 3f;
    private static final long TITLE_LONG_PRESS_MS = 550L;
    private static final int DIALOG_W = 280;
    private static final int DIALOG_H = 118;
    private static final int ADD_BTN_SIZE = 14;

    // endregion Constants

    // region Types

    private enum Column {
        BLACK,
        WHITE
    }

    private enum AccessListTab {
        BLACK,
        WHITE
    }

    // endregion Types

    // region Data

    private final List<String> blackItemsAll = new ArrayList<>();
    private final List<String> whiteItemsAll = new ArrayList<>();
    private final List<String> blackItems = new ArrayList<>();
    private final List<String> whiteItems = new ArrayList<>();

    private int panelWidth;
    private int visibleRowCount = MAX_VISIBLE_ITEMS;
    private int listHeight;
    private int listBodyW;
    private int startX;
    private int topBarY;
    private int headerRowY;
    private int listAreaY;
    private int footerY;
    private int footerW;
    private int footerPanelHeight = FOOTER_HEIGHT;
    private int dlgX;
    private int dlgY;

    private EnumPanelMode panelMode = EnumPanelMode.COLUMNS;
    private AccessListTab activeTab = AccessListTab.BLACK;

    private String deleteConfirmUuid;
    private Column deleteConfirmColumn;

    private InputWidget searchInput;
    private ButtonWidget titleLayoutToggleButton;
    private ButtonWidget addBlackButton;
    private ButtonWidget addWhiteButton;
    private ButtonWidget tabBlackButton;
    private ButtonWidget tabWhiteButton;
    private ButtonWidget tabAddButton;
    private ScrollbarWidget blackScrollbar;
    private ScrollbarWidget whiteScrollbar;
    private ScrollbarWidget tabListScrollbar;
    private ButtonWidget deleteCancelButton;
    private ButtonWidget deleteConfirmButton;

    private int lastAccessFingerprint = Integer.MIN_VALUE;

    // endregion Data

    public AccessListScreen() {
        super(NarcissusComponent.get().literal("AccessListScreen"));
        previousScreen(Minecraft.getInstance().screen);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    protected void onInit() {
        panelMode = ClientConfig.get().client().accessListScreenPanelMode();
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        loadLists();
    }

    @Override
    protected void initWidgets() {
        blackScrollbar = null;
        whiteScrollbar = null;
        tabListScrollbar = null;
        addBlackButton = null;
        addWhiteButton = null;
        titleLayoutToggleButton = null;
        tabBlackButton = null;
        tabWhiteButton = null;
        tabAddButton = null;

        footerPanelHeight = panelMode == EnumPanelMode.TAB_SINGLE ? FOOTER_HEIGHT_TAB : FOOTER_HEIGHT;

        int usableOuter = width - 2 * SCREEN_MARGIN;
        if (panelMode == EnumPanelMode.TAB_SINGLE) {
            panelWidth = (usableOuter - 2 * GAP_H) / 3;
            footerW = 2 * panelWidth + GAP_H;
            listBodyW = footerW - 2 * PANEL_PADDING;
        } else {
            panelWidth = (usableOuter - GAP_H) / 2;
            footerW = 2 * panelWidth + GAP_H;
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
        int titleBtnW = Math.max(72, footerW - searchW - 2 * PANEL_PADDING - 8);
        titleLayoutToggleButton = new ButtonWidget(this);
        titleLayoutToggleButton.id("access_list_title_layout");
        titleLayoutToggleButton.bounds(new ScreenCoordinate(startX + PANEL_PADDING, topBarY, titleBtnW, TOP_BAR_H));
        titleLayoutToggleButton.text(NarcissusComponent.get().transClientAuto("access_list_title"));
        titleLayoutToggleButton.paddingLeft(6);
        titleLayoutToggleButton.paddingRight(6);
        titleLayoutToggleButton.paddingTop(Math.max(0, (TOP_BAR_H - 9) / 2));
        titleLayoutToggleButton.paddingBottom(Math.max(0, (TOP_BAR_H - 9) / 2));
        titleLayoutToggleButton.borderWidth(0);
        titleLayoutToggleButton.radius(4);
        titleLayoutToggleButton.onLongPress(TITLE_LONG_PRESS_MS, b -> togglePanelMode());
        addWidget(titleLayoutToggleButton);

        searchInput = new InputWidget(this);
        searchInput.id("search");
        searchInput.bounds(new ScreenCoordinate(startX + footerW - searchW - PANEL_PADDING, topBarY + 3, searchW, TOP_BAR_H - 6));
        searchInput.text(Text.literal(NarcissusComponent.get().transClientAuto("access_list_search_hint").toString()));
        searchInput.onTextChanged(t -> applySearchFilter());
        addWidget(searchInput);

        int addY = headerRowY + (HEADER_ROW_H - ADD_BTN_SIZE) / 2;
        if (panelMode == EnumPanelMode.COLUMNS) {
            addBlackButton = new ButtonWidget(this);
            addBlackButton.id("add_black");
            addBlackButton.bounds(new ScreenCoordinate(startX + panelWidth - PANEL_PADDING - ADD_BTN_SIZE, addY, ADD_BTN_SIZE, ADD_BTN_SIZE));
            addBlackButton.presetStyle(ButtonWidget.PresetStyle.PLUS);
            addBlackButton.radius(ADD_BTN_SIZE / 4f);
            addBlackButton.padding(1);
            addBlackButton.onClick(b -> openAddBlackDialog());
            addWidget(addBlackButton);

            addWhiteButton = new ButtonWidget(this);
            addWhiteButton.id("add_white");
            addWhiteButton.bounds(new ScreenCoordinate(startX + panelWidth + GAP_H + panelWidth - PANEL_PADDING - ADD_BTN_SIZE, addY, ADD_BTN_SIZE, ADD_BTN_SIZE));
            addWhiteButton.presetStyle(ButtonWidget.PresetStyle.PLUS);
            addWhiteButton.radius(ADD_BTN_SIZE / 4f);
            addWhiteButton.padding(1);
            addWhiteButton.onClick(b -> openAddWhiteDialog());
            addWidget(addWhiteButton);

            blackScrollbar = buildListScrollbarAt(startX, blackItems, "scrollbar_0");
            whiteScrollbar = buildListScrollbarAt(startX + panelWidth + GAP_H, whiteItems, "scrollbar_1");
        } else {
            int hdrPad = PANEL_PADDING;
            int tabBarInner = footerW - 2 * hdrPad;
            int gapTabs = 4;
            int tabW = (tabBarInner - ADD_BTN_SIZE - gapTabs) / 2 - 1;
            int tx = startX + hdrPad;
            int tabY = headerRowY + (HEADER_ROW_H - 18) / 2;
            tabBlackButton = new ButtonWidget(this);
            tabBlackButton.id("tab_black");
            tabBlackButton.bounds(new ScreenCoordinate(tx, tabY, tabW, 18));
            tabBlackButton.text(NarcissusComponent.get().transClientAuto("access_list_column_black"));
            tabBlackButton.radius(TAB_BTN_CORNER_R, 0f, TAB_BTN_CORNER_R, 0f);
            tabBlackButton.onClick(b -> onAccessTabSelected(AccessListTab.BLACK));
            addWidget(tabBlackButton);

            tabWhiteButton = new ButtonWidget(this);
            tabWhiteButton.id("tab_white");
            tabWhiteButton.bounds(new ScreenCoordinate(tx + (tabW + 1), tabY, tabW, 18));
            tabWhiteButton.text(NarcissusComponent.get().transClientAuto("access_list_column_white"));
            tabWhiteButton.radius(0f, TAB_BTN_CORNER_R, 0f, TAB_BTN_CORNER_R);
            tabWhiteButton.onClick(b -> onAccessTabSelected(AccessListTab.WHITE));
            addWidget(tabWhiteButton);

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

        deleteCancelButton = new ButtonWidget(this);
        deleteCancelButton.id("delete_cancel");
        deleteCancelButton.bounds(new ScreenCoordinate(cancelX, btnY, cancelW, 20));
        deleteCancelButton.text(NarcissusComponent.get().transClientAuto("cancel"));
        deleteCancelButton.radius(4);
        deleteCancelButton.visible(false);
        deleteCancelButton.onClick(b -> {
            deleteConfirmUuid = null;
            deleteConfirmColumn = null;
        });
        addWidget(deleteCancelButton);

        deleteConfirmButton = new ButtonWidget(this);
        deleteConfirmButton.id("delete_confirm");
        deleteConfirmButton.bounds(new ScreenCoordinate(deleteX, btnY, deleteW, 20));
        deleteConfirmButton.text(NarcissusComponent.get().transClientAuto("delete"));
        deleteConfirmButton.radius(4);
        deleteConfirmButton.visible(false);
        deleteConfirmButton.onClick(b -> {
            if (deleteConfirmUuid != null && deleteConfirmColumn != null) {
                if (deleteConfirmColumn == Column.BLACK) {
                    PacketUtils.sendPacketToServer(new AccessListEditToServer(1, "", deleteConfirmUuid));
                } else {
                    PacketUtils.sendPacketToServer(new AccessListEditToServer(3, EnumWhiteListMode.NONE.name(), deleteConfirmUuid));
                }
                deleteConfirmUuid = null;
                deleteConfirmColumn = null;
            }
        });
        addWidget(deleteConfirmButton);

        applySearchFilter();
    }

    private ScrollbarWidget buildListScrollbarAt(int panelOriginX, List<String> items, String barId) {
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

    private void togglePanelMode() {
        if (panelMode == EnumPanelMode.COLUMNS) {
            panelMode = EnumPanelMode.TAB_SINGLE;
        } else {
            panelMode = EnumPanelMode.COLUMNS;
        }
        ClientConfig.RootView cfg = ClientConfig.get();
        cfg.client().accessListScreenPanelMode(panelMode);
        cfg.save();
        refreshWidget();
    }

    private void onAccessTabSelected(AccessListTab tab) {
        activeTab = tab;
        if (tabListScrollbar != null) {
            tabListScrollbar.value(0);
        }
        syncScrollbarLimits();
    }

    private void onTabAddClick() {
        if (activeTab == AccessListTab.BLACK) {
            openAddBlackDialog();
        } else {
            openAddWhiteDialog();
        }
    }

    private List<String> activeTabItems() {
        return activeTab == AccessListTab.BLACK ? blackItems : whiteItems;
    }

    private Column activeTabColumn() {
        return activeTab == AccessListTab.BLACK ? Column.BLACK : Column.WHITE;
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
        if (panelMode == EnumPanelMode.COLUMNS) {
            syncOneScrollbar(blackScrollbar, blackItems);
            syncOneScrollbar(whiteScrollbar, whiteItems);
        } else {
            syncOneScrollbar(tabListScrollbar, activeTabItems());
        }
    }

    private void syncOneScrollbar(ScrollbarWidget bar, List<String> items) {
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
        filterUuidList(blackItemsAll, blackItems, q);
        filterUuidList(whiteItemsAll, whiteItems, q);
        syncScrollbarLimits();
    }

    private static void filterUuidList(List<String> src, List<String> dest, String qLower) {
        dest.clear();
        if (qLower.isEmpty()) {
            dest.addAll(src);
            return;
        }
        for (String u : src) {
            if (u.toLowerCase(Locale.ROOT).contains(qLower)) {
                dest.add(u);
                continue;
            }
            String display = playerNameForListUuid(u);
            if (display.toLowerCase(Locale.ROOT).contains(qLower)) {
                dest.add(u);
            }
        }
    }

    private static String playerNameForListUuid(String uuidStr) {
        try {
            return PlayerUtils.getPlayerNameString(UUID.fromString(uuidStr));
        } catch (IllegalArgumentException e) {
            return uuidStr;
        }
    }

    private int computeAccessFingerprint() {
        if (minecraft == null || minecraft.player == null) {
            return 0;
        }
        PlayerAccess access = PlayerTeleportData.getData(minecraft.player).getAccess();
        return Objects.hash(access.getBlackList(), access.getWhiteList(), access.getAutoTpaList(), access.getAutoTphList());
    }

    private void refreshListsIfNeeded() {
        int fp = computeAccessFingerprint();
        if (fp != lastAccessFingerprint) {
            loadLists();
        }
    }

    private void loadLists() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        PlayerAccess access = PlayerTeleportData.getData(minecraft.player).getAccess();
        blackItemsAll.clear();
        blackItemsAll.addAll(access.getBlackList());
        Collections.sort(blackItemsAll);
        whiteItemsAll.clear();
        whiteItemsAll.addAll(access.getWhiteList());
        Collections.sort(whiteItemsAll);
        applySearchFilter();
        lastAccessFingerprint = computeAccessFingerprint();
    }

    private void openAddBlackDialog() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        LinkedHashMap<String, String> nameToUuid = collectOnlinePlayerNameToUuid();
        if (nameToUuid.isEmpty()) {
            notifyNoOnlinePlayers();
            return;
        }
        List<String> labels = new ArrayList<>(nameToUuid.keySet());
        List<DropdownOption> options = new ArrayList<>();
        nameToUuid.forEach((label, uuid) -> {
            Texture[] textures = PlayerSkinTextureUtils.headFaceTextures(UUID.fromString(uuid));
            DropdownOption option = new DropdownOption(label, textures, NarcissusComponent.get().literal(uuid));
            options.add(option);
        });
        Text t = Text.literal(NarcissusComponent.get().transClientAuto("access_list_add_black_title").toString());
        InputFormScreen.Args args = new InputFormScreen.Args()
                .setParentScreen(this)
                .setTitle(t)
                .addWidget(new InputFormScreen.Widget()
                        .name("player_name")
                        .type(InputFormScreen.WidgetType.DROPDOWN)
                        .dropdownOptionEntries(options)
                        .defaultValue(labels.get(0))
                        .title(Text.literal(NarcissusComponent.get().transClientAuto("access_list_player_name").toString()))
                        .hint(Text.literal(BaniraComponent.get().transClientAuto("choose_option").toString())))
                .setCallback(results -> {
                    String label = results.value("player_name").trim();
                    String uuid = nameToUuid.get(label);
                    if (uuid != null) {
                        PacketUtils.sendPacketToServer(new AccessListEditToServer(0, "", uuid));
                    }
                });
        minecraft.setScreen(new InputFormScreen(args));
    }

    private void openAddWhiteDialog() {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        LinkedHashMap<String, String> nameToUuid = collectOnlinePlayerNameToUuid();
        if (nameToUuid.isEmpty()) {
            notifyNoOnlinePlayers();
            return;
        }
        List<String> labels = new ArrayList<>(nameToUuid.keySet());
        List<DropdownOption> options = new ArrayList<>();
        nameToUuid.forEach((label, uuid) -> {
            Texture[] textures = PlayerSkinTextureUtils.headFaceTextures(UUID.fromString(uuid));
            DropdownOption option = new DropdownOption(label, textures, NarcissusComponent.get().literal(uuid));
            options.add(option);
        });
        Text t = Text.literal(NarcissusComponent.get().transClientAuto("access_list_add_white_title").toString());
        InputFormScreen.Args args = new InputFormScreen.Args()
                .setParentScreen(this)
                .setTitle(t)
                .addWidget(new InputFormScreen.Widget()
                        .name("player_name")
                        .type(InputFormScreen.WidgetType.DROPDOWN)
                        .dropdownOptionEntries(options)
                        .defaultValue(labels.get(0))
                        .title(Text.literal(NarcissusComponent.get().transClientAuto("access_list_player_name").toString()))
                        .hint(Text.literal(BaniraComponent.get().transClientAuto("choose_option").toString())))
                .addWidget(new InputFormScreen.Widget()
                        .name("white_mode")
                        .type(InputFormScreen.WidgetType.DROPDOWN)
                        .dropdownOptions(EnumWhiteListMode.class)
                        .title(Text.literal(NarcissusComponent.get().transClientAuto("access_list_white_mode").toString()))
                        .defaultValue(EnumWhiteListMode.NONE)
                        .hint(Text.literal(BaniraComponent.get().transClientAuto("choose_option").toString())))
                .setCallback(results -> {
                    String label = results.value("player_name").trim();
                    String uuid = nameToUuid.get(label);
                    String mode = results.value("white_mode").trim();
                    if (uuid != null) {
                        PacketUtils.sendPacketToServer(new AccessListEditToServer(2, mode, uuid));
                    }
                });
        minecraft.setScreen(new InputFormScreen(args));
    }

    private LinkedHashMap<String, String> collectOnlinePlayerNameToUuid() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        if (minecraft == null || minecraft.player == null || minecraft.player.connection == null) {
            return map;
        }
        List<PlayerInfo> list = new ArrayList<>(minecraft.player.connection.getOnlinePlayers());
        list.sort(Comparator.comparing(a -> a.getProfile().getName(), String.CASE_INSENSITIVE_ORDER));
        for (PlayerInfo info : list) {
            if (info.getProfile() == null || info.getProfile().getId() == null) {
                continue;
            }
            String name = info.getProfile().getName();
            if (StringUtils.isNullOrEmptyEx(name)) {
                continue;
            }
            map.put(name, info.getProfile().getId().toString());
        }
        return map;
    }

    private void notifyNoOnlinePlayers() {
        if (minecraft == null || minecraft.gui == null) {
            return;
        }
        minecraft.gui.getChat().addMessage(NarcissusComponent.get().transClientAuto("access_list_no_online_players").toChat());
    }

    @Override
    protected void onRender(@Nonnull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics, mouseX, mouseY, partialTicks);

        refreshListsIfNeeded();

        BaniraColorConfig theme = getEffectiveTheme();

        boolean dialogOpen = deleteConfirmUuid != null;

        if (blackScrollbar != null) {
            boolean bScroll = !dialogOpen && panelMode == EnumPanelMode.COLUMNS && blackItems.size() > visibleRowCount;
            blackScrollbar.visible(bScroll);
            blackScrollbar.enabled(bScroll);
            boolean wScroll = !dialogOpen && panelMode == EnumPanelMode.COLUMNS && whiteItems.size() > visibleRowCount;
            whiteScrollbar.visible(wScroll);
            whiteScrollbar.enabled(wScroll);
        }
        if (tabListScrollbar != null) {
            boolean tScroll = !dialogOpen && panelMode == EnumPanelMode.TAB_SINGLE && activeTabItems().size() > visibleRowCount;
            tabListScrollbar.visible(tScroll);
            tabListScrollbar.enabled(tScroll);
        }
        if (searchInput != null) {
            searchInput.enabled(!dialogOpen);
        }
        if (titleLayoutToggleButton != null) {
            titleLayoutToggleButton.visible(!dialogOpen);
            titleLayoutToggleButton.enabled(!dialogOpen);
            titleLayoutToggleButton.text(NarcissusComponent.get().transClientAuto("access_list_title"));
            titleLayoutToggleButton.bgColor(theme.panelBg());
            titleLayoutToggleButton.hoverBgColor(theme.panelBg());
            titleLayoutToggleButton.focusedBgColor(theme.panelBg());
            titleLayoutToggleButton.pressedBgColor(ColorUtils.applyAlphaToArgb(theme.accent(), 0x62));
            titleLayoutToggleButton.borderWidth(0);
            titleLayoutToggleButton.textColor(theme.textPrimary());
            titleLayoutToggleButton.hoverTextColor(theme.textPrimary());
            titleLayoutToggleButton.focusedTextColor(theme.textPrimary());
            titleLayoutToggleButton.pressedTextColor(theme.textPrimary());
        }
        if (addBlackButton != null) {
            addBlackButton.enabled(!dialogOpen);
            addBlackButton.visible(!dialogOpen && panelMode == EnumPanelMode.COLUMNS);
        }
        if (addWhiteButton != null) {
            addWhiteButton.enabled(!dialogOpen);
            addWhiteButton.visible(!dialogOpen && panelMode == EnumPanelMode.COLUMNS);
        }
        if (tabBlackButton != null) {
            tabBlackButton.visible(!dialogOpen && panelMode == EnumPanelMode.TAB_SINGLE);
            tabBlackButton.enabled(!dialogOpen);
            applyTabButtonStyle(tabBlackButton, activeTab == AccessListTab.BLACK, theme);
        }
        if (tabWhiteButton != null) {
            tabWhiteButton.visible(!dialogOpen && panelMode == EnumPanelMode.TAB_SINGLE);
            tabWhiteButton.enabled(!dialogOpen);
            applyTabButtonStyle(tabWhiteButton, activeTab == AccessListTab.WHITE, theme);
        }
        if (tabAddButton != null) {
            tabAddButton.visible(!dialogOpen && panelMode == EnumPanelMode.TAB_SINGLE);
            tabAddButton.enabled(!dialogOpen);
        }
        if (deleteCancelButton != null) {
            deleteCancelButton.visible(dialogOpen);
            deleteConfirmButton.visible(dialogOpen);
        }

        PoseStack stack = graphics.pose();
        drawTopBarAndDividers(stack, theme);
        drawColumnHeaders(stack, theme);
        drawListColumns(stack, theme, mouseX, mouseY);
        drawFooterHint(stack, theme);

        if (!dialogOpen) {
            String h1 = NarcissusComponent.get().transClientAuto("blacklist_help").toString();
            String h2 = NarcissusComponent.get().transClientAuto("whitelist_help").toString();
            int y1 = footerY + 6;
            int lh = font.lineHeight;
            if (panelMode == EnumPanelMode.TAB_SINGLE) {
                String h = activeTab == AccessListTab.BLACK ? h1 : h2;
                boolean hoverLine = mouseX >= startX && mouseX < startX + footerW
                        && mouseY >= y1 && mouseY < y1 + lh;
                if (hoverLine) {
                    addDeferredTooltipRender(s -> drawFooterLineTooltip(s.pose(), theme, mouseX, mouseY, h));
                }
            } else {
                int lineGap = 2;
                int y2 = y1 + lh + lineGap;
                boolean hoverBlack = mouseX >= startX && mouseX < startX + footerW
                        && mouseY >= y1 && mouseY < y1 + lh;
                boolean hoverWhite = mouseX >= startX && mouseX < startX + footerW
                        && mouseY >= y2 && mouseY < y2 + lh;
                if (hoverBlack) {
                    addDeferredTooltipRender(s -> drawFooterLineTooltip(s.pose(), theme, mouseX, mouseY, h1));
                } else if (hoverWhite) {
                    addDeferredTooltipRender(s -> drawFooterLineTooltip(s.pose(), theme, mouseX, mouseY, h2));
                }
            }
        }

        if (dialogOpen) {
            drawDeleteConfirmOverlay(stack, theme);
        }

        renderWidgets(graphics, partialTicks);
    }

    private void drawLimitedTextLine(PoseStack stack, String text, double x, double y, int maxWidth, int colorArgb) {
        LabelWidget.drawLimitedText(FontDrawArgs.of(Text.literal(text).stack(stack).font(font).color(Color.argb(colorArgb)))
                .x(x).y(y)
                .maxWidth(maxWidth)
                .align(EnumAlignment.START)
                .wrap(false)
                .inScreen(false));
    }

    private void drawLimitedTextCentered(PoseStack stack, String text, double x, double y, int maxWidth, int colorArgb) {
        LabelWidget.drawLimitedText(FontDrawArgs.of(Text.literal(text).stack(stack).font(font).color(Color.argb(colorArgb)))
                .x(x).y(y)
                .maxWidth(maxWidth)
                .align(EnumAlignment.CENTER)
                .wrap(false)
                .inScreen(false));
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
        if (panelMode == EnumPanelMode.TAB_SINGLE) {
            ShapeDrawArgs h = ShapeDrawArgs.rect(stack, startX, headerRowY, footerW, HEADER_ROW_H, theme.bgSecondary());
            h.rect().radius(0);
            BaseShapeWidget.drawShape(h);
            return;
        }
        for (int c = 0; c < 2; c++) {
            int x = startX + c * (panelWidth + GAP_H);
            ShapeDrawArgs h = ShapeDrawArgs.rect(stack, x, headerRowY, panelWidth, HEADER_ROW_H, theme.bgSecondary());
            h.rect().radius(0);
            BaseShapeWidget.drawShape(h);
            String hdr = c == 0
                    ? NarcissusComponent.get().transClientAuto("access_list_column_black").toString()
                    : NarcissusComponent.get().transClientAuto("access_list_column_white").toString();
            int titleY = headerRowY + (HEADER_ROW_H - font.lineHeight) / 2;
            drawLimitedTextLine(stack, hdr, x + PANEL_PADDING, titleY, Math.max(8, panelWidth - 2 * PANEL_PADDING), theme.textPrimary());
        }
    }

    private void drawListColumns(PoseStack stack, BaniraColorConfig theme, int mouseX, int mouseY) {
        if (panelMode == EnumPanelMode.TAB_SINGLE) {
            drawColumnList(stack, theme, startX, footerW, activeTabItems(), tabListScrollbar, activeTabColumn(), mouseX, mouseY);
            return;
        }
        drawColumnList(stack, theme, startX, panelWidth, blackItems, blackScrollbar, Column.BLACK, mouseX, mouseY);
        drawColumnList(stack, theme, startX + panelWidth + GAP_H, panelWidth, whiteItems, whiteScrollbar, Column.WHITE, mouseX, mouseY);
    }

    private void drawColumnList(PoseStack stack, BaniraColorConfig theme, int listOriginX, int listPanelOuterW, List<String> items,
                                ScrollbarWidget scrollbar, Column column, int mouseX, int mouseY) {
        ShapeDrawArgs panelShape = ShapeDrawArgs.rect(stack, listOriginX, listAreaY, listPanelOuterW, listHeight, theme.panelBg());
        panelShape.rect().radius(0);
        BaseShapeWidget.drawShape(panelShape);

        int listX = listOriginX + PANEL_PADDING;
        boolean scrollNeeded = items.size() > visibleRowCount;
        int cw = listBodyW - (scrollNeeded ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);
        int scroll = scrollbar != null
                ? (int) Math.round(Mth.clamp(scrollbar.value(), 0, Math.max(0, items.size() - visibleRowCount)))
                : 0;

        int innerTop = listAreaY + LIST_PADDING_V;
        int rowSlots = scrollNeeded ? visibleRowCount : items.size();

        PlayerAccess access = minecraft != null && minecraft.player != null
                ? PlayerTeleportData.getData(minecraft.player).getAccess()
                : null;

        int headSlot = 3 + HEAD_SIZE + HEAD_GAP_AFTER;
        // 头像 + 文本 + 删除按钮
        int textMaxW = Math.max(8, cw - headSlot - 16);

        for (int i = 0; i < rowSlots; i++) {
            int idx = i + scroll;
            if (idx >= items.size()) {
                break;
            }

            String uuidStr = items.get(idx);
            int itemY = innerTop + i * ITEM_HEIGHT;
            int rowH = ITEM_HEIGHT;
            int rowDrawH = Math.max(1, rowH - 1);

            boolean hover = mouseX >= listX && mouseX < listX + cw && mouseY >= itemY && mouseY < itemY + rowH;

            int rowBg = hover
                    ? ColorUtils.applyAlphaToArgb(theme.bgSecondary(), 0x45)
                    : ColorUtils.applyAlphaToArgb(theme.bgSecondary(), 0x28);

            ShapeDrawArgs rowRect = ShapeDrawArgs.rect(stack, listX, itemY, cw, rowDrawH, rowBg);
            rowRect.rect().radius(4);
            BaseShapeWidget.drawShape(rowRect);

            if (hover) {
                ShapeDrawArgs softBar = ShapeDrawArgs.rect(stack, listX, itemY, 2, rowDrawH, ColorUtils.applyAlphaToArgb(theme.accent(), 0x90));
                BaseShapeWidget.drawShape(softBar);
            }

            int headX = listX + 3;
            int headY = itemY + (rowH - HEAD_SIZE) / 2;
            drawPlayerHeadFace(stack, uuidStr, headX, headY, HEAD_SIZE);

            int textX = listX + headSlot;
            String titleLine = playerNameForListUuid(uuidStr);
            drawLimitedTextLine(stack, titleLine, textX, itemY + 2, textMaxW, theme.textPrimary());

            String meta;
            if (column == Column.WHITE && access != null) {
                StringBuilder sb = new StringBuilder();
                if (access.getAutoTpaList().contains(uuidStr)) {
                    sb.append("TPA ");
                }
                if (access.getAutoTphList().contains(uuidStr)) {
                    sb.append("TPH");
                }
                meta = sb.toString().trim();
                if (meta.isEmpty()) {
                    meta = "NONE";
                }
            } else {
                meta = uuidStr;
            }
            drawLimitedTextLine(stack, meta, textX, itemY + 12, textMaxW, theme.textSecondary());

            int delX = listX + cw - 16;
            int delY = itemY + (rowH - 10) / 2;
            boolean delHover = mouseX >= delX && mouseX <= delX + 14 && mouseY >= delY && mouseY < delY + 10;
            drawLimitedTextLine(stack, "×", delX + 2, delY, 14, delHover ? theme.error() : theme.textHint());
        }
    }

    private void drawFooterHint(PoseStack stack, BaniraColorConfig theme) {
        ShapeDrawArgs footerBg = ShapeDrawArgs.rect(stack, startX, footerY, footerW, footerPanelHeight, theme.panelBg());
        footerBg.rect().radius(0f, 0f, 6f, 6f);
        BaseShapeWidget.drawShape(footerBg);

        String h1 = NarcissusComponent.get().transClientAuto("blacklist_help").toString();
        String h2 = NarcissusComponent.get().transClientAuto("whitelist_help").toString();
        int maxW = Math.max(8, footerW - 2 * PANEL_PADDING);
        int y = footerY + 6;
        if (panelMode == EnumPanelMode.TAB_SINGLE) {
            String h = activeTab == AccessListTab.BLACK ? h1 : h2;
            drawLimitedTextLine(stack, h, startX + PANEL_PADDING, y, maxW, theme.textHint());
        } else {
            drawLimitedTextLine(stack, h1, startX + PANEL_PADDING, y, maxW, theme.textHint());
            y += font.lineHeight + 2;
            drawLimitedTextLine(stack, h2, startX + PANEL_PADDING, y, maxW, theme.textHint());
        }
    }

    private static void drawPlayerHeadFace(PoseStack stack, String uuidStr, int x, int y, int size) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return;
        }
        Texture[] tex = PlayerSkinTextureUtils.headFaceTextures(uuid);
        if (tex == null || tex.length < 2 || tex[0] == null) {
            return;
        }
        ImageWidget.blit(stack, tex[0], x, y, size, size);
        if (tex[1] != null) {
            ImageWidget.blitBlend(stack, tex[1], x, y, size, size);
        }
    }

    private void drawFooterLineTooltip(PoseStack stack, BaniraColorConfig theme, int mouseX, int mouseY, String fullText) {
        Text tooltipText = Text.literal(fullText).stack(stack).font(font).color(Color.argb(theme.textPrimary()));
        FontDrawArgs args = FontDrawArgs.ofPopo(tooltipText)
                .x(mouseX)
                .y(mouseY);
        TooltipWidget.drawPopupMessage(stack, args);
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
        String msg = NarcissusComponent.get().transClientAuto("access_list_del_confirm_msg").toString();

        int y = dlgY + 10;
        drawLimitedTextCentered(stack, title, dlgX, y, DIALOG_W, theme.textPrimary());
        y += font.lineHeight + 4;
        drawLimitedTextCentered(stack, msg, dlgX, y, DIALOG_W, theme.textSecondary());
        y += font.lineHeight + 6;

        if (deleteConfirmUuid != null) {
            String pname = playerNameForListUuid(deleteConfirmUuid);
            drawLimitedTextCentered(stack, pname, dlgX, y, DIALOG_W, theme.textPrimary());
            y += font.lineHeight + 2;
            drawLimitedTextCentered(stack, deleteConfirmUuid, dlgX, y, DIALOG_W, theme.textHint());
        }
    }

    @Override
    public void onMouseClicked(MouseClickedHandleArgs eventArgs) {
        if (deleteConfirmUuid != null && eventArgs.button() == 0 && !eventArgs.consumed()) {
            eventArgs.consumed(true);
            return;
        }

        if (eventArgs.button() != 0 || eventArgs.consumed()) {
            super.onMouseClicked(eventArgs);
            return;
        }

        double mouseX = eventArgs.mouseX();
        double mouseY = eventArgs.mouseY();

        if (panelMode == EnumPanelMode.TAB_SINGLE) {
            if (checkPanelClick(mouseX, mouseY, startX + PANEL_PADDING, listAreaY + LIST_PADDING_V,
                    activeTabItems(), tabListScrollbar, activeTabColumn())) {
                eventArgs.consumed(true);
            }
        } else {
            if (checkPanelClick(mouseX, mouseY, startX + PANEL_PADDING, listAreaY + LIST_PADDING_V, blackItems, blackScrollbar, Column.BLACK)) {
                eventArgs.consumed(true);
            } else if (checkPanelClick(mouseX, mouseY, startX + panelWidth + GAP_H + PANEL_PADDING, listAreaY + LIST_PADDING_V, whiteItems, whiteScrollbar, Column.WHITE)) {
                eventArgs.consumed(true);
            }
        }

        super.onMouseClicked(eventArgs);
    }

    @Override
    protected void onKeyPressed(KeyPressedHandleArgs eventArgs) {
        if (deleteConfirmUuid != null && eventArgs.key() == GLFWKey.GLFW_KEY_ESCAPE) {
            deleteConfirmUuid = null;
            deleteConfirmColumn = null;
            eventArgs.consumed(true);
            return;
        }
        super.onKeyPressed(eventArgs);
    }

    private boolean checkPanelClick(double mouseX, double mouseY, int listX, int listY, List<String> items, ScrollbarWidget bar, Column column) {
        boolean scrollNeeded = items.size() > visibleRowCount;
        int cw = listBodyW - (scrollNeeded ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);
        int scroll = bar != null ? (int) Math.round(Mth.clamp(bar.value(), 0, Math.max(0, items.size() - visibleRowCount))) : 0;

        int rowSlots = scrollNeeded ? visibleRowCount : items.size();

        for (int i = 0; i < rowSlots; i++) {
            int idx = i + scroll;
            if (idx >= items.size()) {
                break;
            }

            String uuidStr = items.get(idx);
            int itemY = listY + i * ITEM_HEIGHT;
            int rowH = ITEM_HEIGHT;

            if (mouseX >= listX && mouseX < listX + cw && mouseY >= itemY && mouseY < itemY + rowH - 1) {
                int delX = listX + cw - 16;
                if (mouseX >= delX) {
                    deleteConfirmUuid = uuidStr;
                    deleteConfirmColumn = column;
                    return true;
                }
            }
        }
        return false;
    }
}
