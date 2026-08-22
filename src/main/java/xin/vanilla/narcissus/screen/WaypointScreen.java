package xin.vanilla.narcissus.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.enums.EnumI18nType;
import xin.vanilla.banira.common.util.*;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusLang;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.TeleportRecord;
import xin.vanilla.narcissus.data.client.ClientStageData;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.internal.client.NarcissusClientSyncState;
import xin.vanilla.narcissus.network.packet.WaypointAddHomeToServer;
import xin.vanilla.narcissus.network.packet.WaypointAddStageToServer;
import xin.vanilla.narcissus.network.packet.WaypointDelToServer;
import xin.vanilla.narcissus.network.packet.WaypointTeleportToServer;
import xin.vanilla.narcissus.util.ClientCostCalculator;
import xin.vanilla.narcissus.util.NarcissusUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

@Accessors(chain = true, fluent = true)
public class WaypointScreen extends BaniraScreen {

    // region Constants

    private static final int PANEL_PADDING = 6;
    private static final int ITEM_HEIGHT = 28;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 2;
    private static final int LIST_PADDING_V = 3;
    private static final int FOOTER_PAD_H = 10;
    private static final int FOOTER_PAD_V = 6;
    private static final int DIALOG_W = 280;
    private static final int DIALOG_H = 118;
    private static final int DOUBLE_CLICK_MS = 450;
    private static final int ADD_BTN_SIZE = 14;
    private static final int TELEPORT_BTN_W = 100;
    private static final int TELEPORT_BTN_H = 20;

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
    private long observedWaypointDataGeneration;

    private final List<WaypointEntry> homeItemsAll = new ArrayList<>();
    private final List<WaypointEntry> stageItemsAll = new ArrayList<>();
    private final List<WaypointEntry> backItemsAll = new ArrayList<>();

    private final List<WaypointEntry> homeItems = new ArrayList<>();
    private final List<WaypointEntry> stageItems = new ArrayList<>();
    private final List<WaypointEntry> backItems = new ArrayList<>();

    private WaypointEntry selectedItem;
    private WaypointEntry lastSelectedItem;

    private int ticketCount;
    private WaypointListTab activeTab = WaypointListTab.PRIVATE;
    private int listHeight;
    /**
     * 列表内容区宽度（= 面板外宽 - 2 * {@link #PANEL_PADDING}，随模式变化）
     */
    private int listBodyW;
    private int listAreaY;
    private int footerY;
    private int footerX;
    private int footerW;
    private int footerPanelHeight;
    private int dlgX;
    private int dlgY;

    private WaypointEntry deleteConfirmItem;
    private WaypointEntry hoveredItem;

    private WaypointEntry lastDoubleClickEntry;
    private long lastDoubleClickTime;

    private InputWidget searchInput;
    private ButtonWidget addButton;
    private ScrollbarWidget activeScrollbar;
    private ButtonWidget teleportButton;
    private ButtonWidget deleteCancelButton;
    private ButtonWidget deleteConfirmButton;
    private NarcissusScreenChrome.Layout journalLayout;
    private NarcissusScreenChrome.Palette journalPalette;

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
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        observedWaypointDataGeneration = NarcissusClientSyncState.waypointDataGeneration();
        loadData();
        selectInitialNonEmptyTab();
    }

    @Override
    public void tick() {
        super.tick();
        long generation = NarcissusClientSyncState.waypointDataGeneration();
        if (generation == observedWaypointDataGeneration) return;
        observedWaypointDataGeneration = generation;
        refreshFromSynchronizedPlayerData();
    }

    @Override
    protected void initWidgets() {
        journalLayout = NarcissusScreenChrome.layout(width, height);
        journalPalette = NarcissusScreenChrome.palette(getEffectiveTheme());
        activeScrollbar = null;
        addButton = null;

        listAreaY = journalLayout.list().y();
        footerX = journalLayout.detail().x();
        footerY = journalLayout.detail().y();
        footerW = journalLayout.detail().width();
        footerPanelHeight = journalLayout.detail().height();
        listHeight = journalLayout.list().height();
        listBodyW = journalLayout.list().width() - PANEL_PADDING * 2;

        dlgX = (width - DIALOG_W) / 2;
        dlgY = (height - DIALOG_H) / 2;

        int cancelW = Math.max(72, font.width(NarcissusComponent.get().transClientAuto("cancel").toString()) + 20);
        int deleteW = Math.max(72, font.width(NarcissusComponent.get().transClientAuto("delete").toString()) + 20);
        int btnY = dlgY + DIALOG_H - 30;
        int cancelX = dlgX + (DIALOG_W - cancelW - deleteW - 10) / 2;
        int deleteX = cancelX + cancelW + 10;

        int addSpace = ADD_BTN_SIZE + 8;
        int searchX = journalLayout.top().x() + compactTabsEndOffset() + 8;
        int searchW = Math.max(50,
                journalLayout.top().x() + journalLayout.top().width() - addSpace - 4 - searchX);
        searchInput = new InputWidget(this);
        searchInput.id("search");
        searchInput.bounds(new ScreenCoordinate(searchX, journalLayout.top().y() + 6, searchW, 24));
        searchInput.text(Text.literal(NarcissusComponent.get().transClientAuto("waypoint_search_hint").toString()));
        searchInput.onTextChanged(t -> applySearchFilter());
        addWidget(searchInput);

        addButton = new ButtonWidget(this);
        addButton.id("add_waypoint");
        addButton.bounds(new ScreenCoordinate(journalLayout.top().x() + journalLayout.top().width() - ADD_BTN_SIZE - 4,
                journalLayout.top().y() + 11, ADD_BTN_SIZE, ADD_BTN_SIZE));
        addButton.presetStyle(ButtonWidget.PresetStyle.PLUS);
        addButton.radius(ADD_BTN_SIZE / 4f);
        addButton.padding(1);
        addButton.onClick(b -> onTabAddClick());
        addWidget(addButton);

        activeScrollbar = buildActiveScrollbar();

        int teleportBtnX = footerX + footerW - TELEPORT_BTN_W - FOOTER_PAD_H;
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

    private ScrollbarWidget buildActiveScrollbar() {
        int listX = journalLayout.list().x() + PANEL_PADDING;
        int viewportY = listAreaY + LIST_PADDING_V;
        int viewportHeight = listViewportHeight();
        double maxScroll = maxScrollOffset(activeTabItems());

        ScrollbarWidget bar = new ScrollbarWidget(this);
        bar.id("active_waypoint_scrollbar");
        bar.bounds(new ScreenCoordinate(listX + listBodyW - SCROLLBAR_WIDTH,
                viewportY, SCROLLBAR_WIDTH, viewportHeight));
        bar.orientation(EnumOrientation.VERTICAL);
        bar.minValue(0);
        bar.maxValue(maxScroll);
        bar.visibleSize(viewportHeight);
        bar.scrollStep(ITEM_HEIGHT * 0.45D);
        bar.addScrollHoverArea(new ScreenCoordinate(listX, viewportY, listBodyW, viewportHeight));
        addWidget(bar);
        return bar;
    }

    private int listViewportHeight() {
        return Math.max(1, listHeight - LIST_PADDING_V * 2);
    }

    private double maxScrollOffset(List<?> items) {
        return Math.max(0.0D, items.size() * (double) ITEM_HEIGHT - listViewportHeight());
    }

    private NarcissusScreenChrome.ListViewport activeViewport(List<?> items) {
        double offset = activeScrollbar != null ? activeScrollbar.value() : 0.0D;
        return NarcissusScreenChrome.listViewport(
                items.size(), ITEM_HEIGHT, listAreaY + LIST_PADDING_V, listViewportHeight(), offset);
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
        if (activeScrollbar != null) {
            activeScrollbar.value(0);
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

    private void syncScrollbarLimits() {
        syncOneScrollbar(activeScrollbar, activeTabItems());
    }

    private void syncOneScrollbar(ScrollbarWidget bar, List<WaypointEntry> items) {
        if (bar == null) {
            return;
        }
        double v = bar.value();
        double maxScroll = maxScrollOffset(items);
        bar.maxValue(maxScroll);
        bar.visibleSize(listViewportHeight());
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
        if (indexInList(activeTabItems(), selectedItem) >= 0) {
            return;
        }
        pickFirstSelection();
    }

    private void pickFirstSelection() {
        List<WaypointEntry> items = activeTabItems();
        selectedItem = items.isEmpty() ? null : items.get(0);
    }

    /** 初次打开优先展示有内容的分类，避免把已有地标藏在空页后面。 */
    private void selectInitialNonEmptyTab() {
        if (!homeItems.isEmpty()) {
            activeTab = WaypointListTab.PRIVATE;
        } else if (!stageItems.isEmpty()) {
            activeTab = WaypointListTab.PUBLIC;
        } else if (!backItems.isEmpty()) {
            activeTab = WaypointListTab.FOOTPRINTS;
        }
        pickFirstSelection();
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
        PlayerEntity p = minecraft.player;
        int need = NarcissusUtils.getCommandPermissionLevel(EnumCommandType.TP_HOME);
        if (!p.hasPermissions(need) && !CommandUtils.hasVirtualPermission(p, EnumCommandType.TP_HOME)) {
            return false;
        }
        return homeItemsAll.size() < CommonConfig.get().base().teleportLimit().teleportHomeLimit();
    }

    private boolean canAddStageClient() {
        if (minecraft == null || minecraft.player == null) {
            return false;
        }
        if (!CommonConfig.get().featureSwitch().switchTpStage()) {
            return false;
        }
        PlayerEntity p = minecraft.player;
        int need = NarcissusUtils.getCommandPermissionLevel(EnumCommandType.SET_STAGE);
        return p.hasPermissions(need) || CommandUtils.hasVirtualPermission(p, EnumCommandType.SET_STAGE);
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
                    PacketUtils.sendPacketToServer(new WaypointAddHomeToServer(n == null ? "" : n));
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
                    PacketUtils.sendPacketToServer(new WaypointAddStageToServer(name, dimension, x, y, z));
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
        journalPalette = NarcissusScreenChrome.palette(theme);
        int mouseX = (int) inputState.mouseX();
        int mouseY = (int) inputState.mouseY();

        if (deleteConfirmItem == null) {
            updateHoveredItem(mouseX, mouseY);
        } else {
            hoveredItem = null;
        }

        boolean dialogOpen = deleteConfirmItem != null;
        if (activeScrollbar != null) {
            boolean scroll = !dialogOpen && maxScrollOffset(activeTabItems()) > 0.0D;
            activeScrollbar.visible(scroll);
            activeScrollbar.enabled(scroll);
        }
        if (searchInput != null) {
            searchInput.enabled(!dialogOpen);
        }
        if (addButton != null) {
            boolean addOk = activeTab == WaypointListTab.PRIVATE && canAddHomeClient()
                    || activeTab == WaypointListTab.PUBLIC && canAddStageClient();
            addButton.visible(!dialogOpen && activeTab != WaypointListTab.FOOTPRINTS);
            addButton.enabled(!dialogOpen && addOk);
        }
        if (teleportButton != null) {
            teleportButton.visible(!dialogOpen);
            teleportButton.enabled(selectedItem != null && selectedItem.canTeleport);
        }
        if (deleteCancelButton != null) {
            deleteCancelButton.visible(dialogOpen);
            deleteConfirmButton.visible(dialogOpen);
        }

        NarcissusScreenChrome.drawJournal(stack, journalPalette, journalLayout);
        drawWaypointTabs(stack, mouseX, mouseY);
        drawWaypointTopBar(stack);
        drawActiveList(stack, mouseX, mouseY);
        drawSelectedDetail(stack);

        if (hoveredItem != null && !dialogOpen) {
            addDeferredTooltipRender(s -> drawCustomTooltip(s, theme, hoveredItem, mouseX, mouseY));
        }

        if (dialogOpen) {
            drawDeleteConfirmOverlay(stack, theme);
        }

        renderWidgets(stack, partialTicks);
    }

    private void drawLimitedTextLine(MatrixStack stack, String text, double x, double y, int maxWidth, int colorArgb) {
        LabelWidget.drawLimitedText(FontDrawArgs.of(Text.literal(text).stack(stack).font(font).color(Color.argb(colorArgb)))
                .x(x).y(y)
                .maxWidth(maxWidth)
                .align(EnumAlignment.START)
                .wrap(false)
                .inScreen(false));
    }

    private void drawLimitedTextCentered(MatrixStack stack, String text, double x, double y, int maxWidth, int colorArgb) {
        LabelWidget.drawLimitedText(FontDrawArgs.of(Text.literal(text).stack(stack).font(font).color(Color.argb(colorArgb)))
                .x(x).y(y)
                .maxWidth(maxWidth)
                .align(EnumAlignment.CENTER)
                .wrap(false)
                .inScreen(false));
    }

    private void drawWaypointTabs(MatrixStack stack, int mouseX, int mouseY) {
        WaypointListTab[] tabs = WaypointListTab.values();
        for (int i = 0; i < tabs.length; i++) {
            WaypointListTab tab = tabs[i];
            NarcissusScreenChrome.Rect rect = tabRect(i);
            boolean hovered = contains(rect, mouseX, mouseY);
            NarcissusScreenChrome.drawCompactTab(stack, journalPalette, rect, hovered, tab == activeTab);
            String label = waypointTabLabel(tab);
            int textWidth = Math.min(font.width(label), rect.width() - 10);
            drawLimitedTextLine(stack, label, rect.x() + (rect.width() - textWidth) / 2,
                    rect.y() + (rect.height() - font.lineHeight) / 2,
                    textWidth, tab == activeTab ? journalPalette.primary() : journalPalette.secondary());
        }
    }

    private void drawWaypointTopBar(MatrixStack stack) {
        String ticket = NarcissusComponent.get().transClientAuto("teleport_card").toString() + ": " + ticketCount;
        drawLimitedTextLine(stack, ticket, journalLayout.top().x() + 8,
                journalLayout.top().y() + (journalLayout.top().height() - font.lineHeight) / 2,
                Math.max(72, (int) searchInput.bounds().x() - journalLayout.top().x() - 14), journalPalette.primary());
    }

    private void drawActiveList(MatrixStack stack, int mouseX, int mouseY) {
        List<WaypointEntry> items = activeTabItems();
        int listX = journalLayout.list().x() + PANEL_PADDING;
        NarcissusScreenChrome.ListViewport viewport = activeViewport(items);
        boolean scrollNeeded = viewport.maxOffset() > 0.0D;
        int cw = listBodyW - (scrollNeeded ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);

        if (items.isEmpty()) {
            String emptyText = activeTab == WaypointListTab.PRIVATE
                    ? NarcissusComponent.get().transClientAuto("home_is_empty").toString()
                    : activeTab == WaypointListTab.PUBLIC
                    ? NarcissusComponent.get().transClientAuto("stage_is_empty").toString()
                    : NarcissusComponent.get().transClientAuto("list_is_empty",
                    NarcissusComponent.get().transClientAuto("footprints").toString()).toString();
            drawLimitedTextCentered(stack, emptyText, listX,
                    listAreaY + listHeight / 2 - font.lineHeight / 2, cw, journalPalette.secondary());
            return;
        }

        int viewportY = listAreaY + LIST_PADDING_V;
        AbstractGuiUtils.pushScissor(listX, viewportY, cw, listViewportHeight());
        try {
            for (int idx = viewport.firstIndex(); idx < viewport.lastIndexExclusive(); idx++) {
                WaypointEntry item = items.get(idx);
                int itemY = (int) Math.floor(viewport.rowY(idx));
                int rowH = ITEM_HEIGHT;

                int rowDrawH = Math.max(1, rowH - 1);
                boolean hover = mouseX >= listX && mouseX < listX + cw
                        && viewport.itemIndexAt(mouseY) == idx;
                boolean selected = item == selectedItem;

                int textColor = item.canTeleport ? journalPalette.primary() : journalPalette.disabled();
                int metaColor = item.canTeleport ? journalPalette.secondary() : journalPalette.disabled();

                NarcissusScreenChrome.drawJournalListRow(stack, journalPalette,
                        new NarcissusScreenChrome.Rect(listX, itemY, cw, rowDrawH),
                        item.canTeleport, hover, selected);

                int rowTextMaxW = Math.max(8, cw - 28);
                drawLimitedTextLine(stack, item.name, listX + 7, itemY + 3, rowTextMaxW, textColor);
                String meta = item.getDimensionName() + "  " + item.getCoordinateName()
                        + "  " + formatItemDistanceMeters(item);
                drawLimitedTextLine(stack, meta, listX + 7, itemY + 15, rowTextMaxW, metaColor);

                if ((item.type == WaypointEntry.Type.HOME || item.type == WaypointEntry.Type.STAGE) && item.canTeleport) {
                    int delX = listX + cw - 16;
                    int delY = itemY + (rowH - 10) / 2;
                    boolean delHover = hover && mouseX >= delX && mouseX <= delX + 14
                            && mouseY >= delY && mouseY < delY + 10;
                    drawLimitedTextLine(stack, "×", delX + 2, delY, 14,
                            delHover ? journalPalette.danger() : journalPalette.secondary());
                }
            }
        } finally {
            AbstractGuiUtils.popScissor();
        }
    }

    private NarcissusScreenChrome.Rect tabRect(int index) {
        WaypointListTab[] tabs = WaypointListTab.values();
        int offset = compactTabsStartOffset();
        for (int i = 0; i < index; i++) {
            offset += compactTabWidth(tabs[i]) + 4;
        }
        return NarcissusScreenChrome.compactTabRect(
                journalLayout.top(), offset, compactTabWidth(tabs[index]));
    }

    private int compactTabsStartOffset() {
        String ticket = NarcissusComponent.get().transClientAuto("teleport_card").toString() + ": " + ticketCount;
        return Math.max(64, font.width(ticket) + 14);
    }

    private int compactTabsEndOffset() {
        int offset = compactTabsStartOffset();
        WaypointListTab[] tabs = WaypointListTab.values();
        for (int i = 0; i < tabs.length; i++) {
            offset += compactTabWidth(tabs[i]);
            if (i + 1 < tabs.length) {
                offset += 4;
            }
        }
        return offset;
    }

    private int compactTabWidth(WaypointListTab tab) {
        String widestLabel = waypointTabName(tab) + "  999";
        return Math.max(44, font.width(widestLabel) + 12);
    }

    private String waypointTabLabel(WaypointListTab tab) {
        return waypointTabName(tab) + "  " + waypointTabCount(tab);
    }

    private static boolean contains(NarcissusScreenChrome.Rect rect, double x, double y) {
        return x >= rect.x() && x < rect.x() + rect.width()
                && y >= rect.y() && y < rect.y() + rect.height();
    }

    private String waypointTabName(WaypointListTab tab) {
        switch (tab) {
            case PUBLIC:
                return NarcissusComponent.get().transClientAuto("public").toString();
            case FOOTPRINTS:
                return NarcissusComponent.get().transClientAuto("footprints").toString();
            default:
                return NarcissusComponent.get().transClientAuto("private").toString();
        }
    }

    private int waypointTabCount(WaypointListTab tab) {
        switch (tab) {
            case PUBLIC:
                return stageItems.size();
            case FOOTPRINTS:
                return backItems.size();
            default:
                return homeItems.size();
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

        WaypointListTab[] tabs = WaypointListTab.values();
        for (int i = 0; i < tabs.length; i++) {
            if (contains(tabRect(i), mouseX, mouseY)) {
                onTabSelected(tabs[i]);
                eventArgs.consumed(true);
                super.onMouseClicked(eventArgs);
                return;
            }
        }

        if (checkPanelClick(mouseX, mouseY, journalLayout.list().x() + PANEL_PADDING,
                listAreaY + LIST_PADDING_V, activeTabItems(), activeScrollbar)) {
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
                .collect(Collectors.toList());
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

    /** 同步完成后重建列表，并尽量保留玩家当前正在查看的条目。 */
    private void refreshFromSynchronizedPlayerData() {
        WaypointEntry previousSelection = selectedItem;
        deleteConfirmItem = null;
        hoveredItem = null;
        lastDoubleClickEntry = null;
        lastDoubleClickTime = 0L;
        selectedItem = null;
        lastSelectedItem = null;
        loadData();
        selectedItem = findMatchingWaypoint(previousSelection, activeTabItems());
    }

    static WaypointEntry findMatchingWaypoint(WaypointEntry previousSelection, List<WaypointEntry> candidates) {
        return WaypointSelectionState.findMatching(
                selectionKey(previousSelection), candidates, WaypointScreen::selectionKey);
    }

    private static WaypointSelectionState.Key selectionKey(WaypointEntry entry) {
        if (entry == null) return null;
        String dimension = entry.safeWorldCoordinate == null
                ? null : entry.safeWorldCoordinate.getDimensionResourceId();
        return new WaypointSelectionState.Key(entry.type.name(), entry.name, dimension, entry.recordTime);
    }

    static WaypointEntry selectionAfterDelete(WaypointEntry selected, WaypointEntry deleted) {
        return WaypointSelectionState.afterDelete(selected, deleted);
    }

    private boolean checkPanelClick(double mouseX, double mouseY, int listX, int listY, List<WaypointEntry> items, ScrollbarWidget bar) {
        NarcissusScreenChrome.ListViewport viewport = NarcissusScreenChrome.listViewport(
                items.size(), ITEM_HEIGHT, listY, listViewportHeight(), bar != null ? bar.value() : 0.0D);
        boolean scrollNeeded = viewport.maxOffset() > 0.0D;
        int cw = listBodyW - (scrollNeeded ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);
        int idx = viewport.itemIndexAt(mouseY);
        if (idx < 0 || mouseX < listX || mouseX >= listX + cw) {
            return false;
        }
        WaypointEntry item = items.get(idx);
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
        int listX = journalLayout.list().x() + PANEL_PADDING;
        List<WaypointEntry> items = activeTabItems();
        NarcissusScreenChrome.ListViewport viewport = activeViewport(items);
        boolean scrollNeeded = viewport.maxOffset() > 0.0D;
        int cw = listBodyW - (scrollNeeded ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);
        int idx = viewport.itemIndexAt(mouseY);
        if (idx >= 0 && mouseX >= listX && mouseX < listX + cw) {
            hoveredItem = items.get(idx);
        }
    }

    private void drawCustomTooltip(MatrixStack stack, BaniraColorConfig theme, WaypointEntry item, int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        lines.add(item.name);
        lines.add(item.getDetailTypeName() + "  " + item.getDimensionName());
        lines.add(NarcissusComponent.get().transClientAuto("waypoint_detail_coord").toString()
                + item.getCoordinateName() + "  " + formatItemDistanceMeters(item));
        if (item.safeWorldCoordinate != null) {
            lines.add(NarcissusComponent.get().transClientAuto("waypoint_detail_dimension").toString()
                    + item.safeWorldCoordinate.getDimensionResourceId());
        }
        if (item.recordTime != null) {
            lines.add(NarcissusComponent.get().transClientAuto("waypoint_detail_time").toString()
                    + DateUtils.toString(item.recordTime, "yyyy-MM-dd HH:mm:ss"));
        }
        if (!item.canTeleport) {
            lines.add(NarcissusComponent.get().transClientAuto("back_record_used").toString());
        }
        String content = String.join("\n", lines);
        Text tooltipText = Text.literal(content).stack(stack).font(font).color(Color.argb(theme.textPrimary()));
        FontDrawArgs args = FontDrawArgs.ofPopo(tooltipText)
                .x(mouseX).y(mouseY)
                .inScreen(true);
        TooltipWidget.drawPopupMessage(stack, args, theme, season());
    }

    private void drawSelectedDetail(MatrixStack stack) {
        if (selectedItem == null) {
            teleportButton.visible(false);
            return;
        }
        NarcissusScreenChrome.drawDetailSurface(stack, journalPalette, journalLayout.detail());
        teleportButton.visible(deleteConfirmItem == null);

        String costStr = calculateCostDisplay(selectedItem);
        if (StringUtils.isNullOrEmptyEx(costStr)) {
            costStr = NarcissusComponent.get().transClientAuto("cost_free").toString();
        }
        String costFull = NarcissusComponent.get().transClientAuto("waypoint_detail_cost").toString() + costStr;
        int leftX = footerX + FOOTER_PAD_H;
        int leftZoneW = Math.max(40, footerW - TELEPORT_BTN_W - FOOTER_PAD_H * 3);
        int lineH = font.lineHeight + 1;
        int y0 = footerY + FOOTER_PAD_V;
        String sep = "  ";
        String line1 = selectedItem.getDetailTypeName() + sep + selectedItem.name;
        drawLimitedTextLine(stack, line1, leftX, y0, leftZoneW, journalPalette.primary());
        y0 += lineH;
        String line2 = selectedItem.getDimensionName() + sep + selectedItem.getCoordinateName()
                + sep + formatItemDistanceMeters(selectedItem);
        drawLimitedTextLine(stack, line2, leftX, y0, leftZoneW, journalPalette.secondary());
        y0 += lineH;
        drawLimitedTextLine(stack, costFull, leftX, y0, leftZoneW, journalPalette.selected());
    }

    private String formatItemDistanceMeters(WaypointEntry item) {
        if (minecraft == null || minecraft.player == null || item.safeWorldCoordinate == null
                || item.safeWorldCoordinate.dimension() != minecraft.player.level.dimension()) {
            return "∞m";
        }
        return NumberUtils.toFixedEx(item.safeWorldCoordinate.distanceFrom(new SafeWorldCoordinate(minecraft.player)), 1) + "m";
    }

    private void drawDeleteConfirmOverlay(MatrixStack stack, BaniraColorConfig theme) {
        ShapeDrawArgs dim = ShapeDrawArgs.rect(stack, 0, 0, width, height, ColorUtils.applyAlphaToArgb(theme.bgQuaternary(), 0x78));
        BaseShapeWidget.drawShape(dim);

        NarcissusScreenChrome.drawDialog(stack, journalPalette, dlgX, dlgY, DIALOG_W, DIALOG_H);

        String title = NarcissusComponent.get().transClientAuto("del_confirm_title").toString();
        String msg = NarcissusComponent.get().transClientAuto("del_confirm_msg").toString();

        int y = dlgY + 10;
        drawLimitedTextCentered(stack, title, dlgX, y, DIALOG_W, theme.textPrimary());
        y += font.lineHeight + 4;
        drawLimitedTextCentered(stack, msg, dlgX, y, DIALOG_W, theme.textSecondary());
        y += font.lineHeight + 6;

        if (deleteConfirmItem != null) {
            drawLimitedTextCentered(stack, deleteConfirmItem.name, dlgX, y, DIALOG_W, theme.textPrimary());
            y += font.lineHeight + 2;
            String meta = deleteConfirmItem.getDimensionName() + " " + deleteConfirmItem.getCoordinateName();
            drawLimitedTextCentered(stack, meta, dlgX, y, DIALOG_W, theme.textHint());
        }
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
        PacketUtils.sendPacketToServer(new WaypointTeleportToServer(type, name, dimension));
        super.previousScreen(null);
        onClose();
    }

    private void doActualDelete(WaypointEntry item) {
        if (item == null || item.safeWorldCoordinate == null || minecraft == null || minecraft.player == null) {
            return;
        }
        int typeOrdinal = item.type == WaypointEntry.Type.HOME ? 0 : 1;
        String dimension = item.safeWorldCoordinate.getDimensionResourceId();
        PacketUtils.sendPacketToServer(new WaypointDelToServer(typeOrdinal, item.name, dimension));
        if (item.type == WaypointEntry.Type.HOME) {
            homeItemsAll.removeIf(e -> e.name.equals(item.name) && e.safeWorldCoordinate != null && dimension.equals(e.safeWorldCoordinate.getDimensionResourceId()));
        } else if (item.type == WaypointEntry.Type.STAGE) {
            stageItemsAll.removeIf(e -> e.name.equals(item.name) && e.safeWorldCoordinate != null && dimension.equals(e.safeWorldCoordinate.getDimensionResourceId()));
        }
        applySearchFilter();
        selectedItem = selectionAfterDelete(selectedItem, item);
        if (selectedItem == null) lastSelectedItem = null;
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
