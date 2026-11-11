package xin.vanilla.narcissus.screen;

import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.joml.Matrix3x2fStack;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.api.client.ui.UiCloseReason;
import xin.vanilla.banira.api.client.ui.UiGameElement;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseClickTracker;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.PlayerSkinTextureUtils;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.client.ui.CommonUiPages;
import xin.vanilla.banira.internal.client.ui.UiNativePage;
import xin.vanilla.banira.internal.client.ui.UiViewState;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.client.ui.*;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumWhiteListMode;
import xin.vanilla.narcissus.network.packet.AccessListEditToServer;

import javax.annotation.Nonnull;
import java.util.*;

@Accessors(chain = true, fluent = true)
public class AccessListScreen extends BaniraScreen implements UiNativePage {
    private final Object uiConnection = Minecraft.getInstance().getConnection();
    private final AccessListUiSession uiSession = new AccessListUiSession(new UiPort(), this::uiConnectionCurrent);
    private UiPageSpec uiPage;
    private UiViewState uiState;
    private String uiQuery = "", uiSelected = "";
    private long uiRevision;

    private boolean uiConnectionCurrent() {
        return uiConnection != null && Minecraft.getInstance().getConnection() == uiConnection
                && Minecraft.getInstance().player != null;
    }

    @Override
    public String uiSurfaceId() {
        return "narcissus_farewell:access";
    }

    @Override
    public AccessListUiSession uiSession() {
        return uiSession;
    }

    @Override
    public UiPageSpec uiPage() {
        if (getClass() != AccessListScreen.class) return null;
        if (uiPage == null) uiPage = NarcissusUiPages.page("access", this, uiSession);
        return uiPage;
    }

    @Override
    public void captureUiState(UiViewState state) {
        if (searchInput != null) uiQuery = searchInput.value();
        if (state != null) {
            if (activeScrollbar != null) state.rowPosition(activeScrollbar.value() / ITEM_HEIGHT);
            if (searchInput != null && searchInput.focused())
                state.selection("search", searchInput.cursorPosition(), searchInput.highlightPos());
        }
    }

    @Override
    public void prepareUiState(UiViewState state) {
        uiState = state;
    }

    @Override
    protected void onRemoved() {
        CommonUiPages.removed(this);
        super.onRemoved();
    }

    @Override
    public void onClose() {
        if (CommonUiPages.close(this, UiCloseReason.CANCEL) || uiSession.requestClose()) return;
        uiSession.close(UiCloseReason.CANCEL);
        super.onClose();
    }

    private static Texture[] accessHeadTextures(UUID uuid) {
        return PlayerSkinTextureUtils.headFaceTextures(AccessProfileIcons.location(uuid, PlayerUtils.getPlayerSkin(uuid)));
    }

    private String uiKey(String uuid) {
        return activeTab.name() + ":" + uuid;
    }

    private String uiUuid(String key) {
        return activeTabItems().stream().filter(uuid -> uiKey(uuid).equals(key)).findFirst().orElse("");
    }

    private void deleteConfirmed() {
        if (!uiConnectionCurrent() || deleteConfirmUuid == null || deleteConfirmColumn == null) return;
        if (deleteConfirmColumn == Column.BLACK)
            PacketUtils.sendPacketToServer(new AccessListEditToServer(1, "", deleteConfirmUuid));
        else
            PacketUtils.sendPacketToServer(new AccessListEditToServer(3, EnumWhiteListMode.NONE.name(), deleteConfirmUuid));
        deleteConfirmUuid = null;
        deleteConfirmColumn = null;
        uiRevision++;
    }

    private final class UiPort implements AccessListUiSession.PresentationPort {
        public Set<NarcissusListUiSession.Action> actions() {
            return EnumSet.of(NarcissusListUiSession.Action.ADD, NarcissusListUiSession.Action.DELETE,
                    NarcissusListUiSession.Action.EDIT, NarcissusListUiSession.Action.CONFIRM, NarcissusListUiSession.Action.CANCEL);
        }

        public long revision() {
            return uiRevision;
        }

        public List<NarcissusListUiSession.Entry> entries() {
            return activeTabItems().stream().map(uuid -> new NarcissusListUiSession.Entry(uiKey(uuid),
                    playerNameForListUuid(uuid), uuid)).toList();
        }

        public int tabs() {
            return 2;
        }

        public int tab() {
            return activeTab.ordinal();
        }

        public void tab(int index) {
            if (index < 0 || index >= 2) return;
            deleteConfirmUuid = null;
            deleteConfirmColumn = null;
            uiSelected = "";
            onAccessTabSelected(AccessListTab.values()[index]);
            uiRevision++;
        }

        public String query() {
            return uiQuery;
        }

        public void query(String value) {
            uiQuery = value;
            if (searchInput != null) searchInput.value(value);
            applySearchFilter();
        }

        public String selected() {
            return uiSelected;
        }

        public void select(String key) {
            if (!uiUuid(key).isEmpty()) {
                uiSelected = key;
                uiRevision++;
            }
        }

        public String label(String id) {
            if (id.equals("tab0") || id.equals("tab1")) {
                int index = id.equals("tab0") ? 0 : 1;
                return NarcissusComponent.get().transClientAuto(index == 0
                        ? "access_list_column_black" : "access_list_column_white") + " " + (index == 0 ? blackItemsAll.size() : whiteItemsAll.size());
            }
            return switch (id) {
                case "title" -> NarcissusComponent.get().transClientAuto("access_list_title").toString();
                case "subtitle" -> tr("access_personal");
                case "scope_label" -> tr("access_scope_label");
                case "player_label" -> tr("access_list_player_name");
                case "mode_label" -> tr("access_list_white_mode");
                case "confirm_title" -> tr("del_confirm_title");
                case "search" -> NarcissusComponent.get().transClientAuto("access_list_search_hint").toString();
                case "add" -> "+";
                case "delete" -> "x";
                case "edit" -> tr("access_edit");
                case "add_tip" ->
                        NarcissusComponent.get().transClientAuto(activeTab == AccessListTab.BLACK ? "access_list_add_black_title" : "access_list_add_white_title").toString();
                case "edit_tip" ->
                        NarcissusComponent.get().transClientAuto(activeTab == AccessListTab.BLACK ? "access_list_edit_black_title" : "access_list_edit_white_title").toString();
                case "delete_tip", "confirm", "confirm_tip" ->
                        NarcissusComponent.get().transClientAuto("delete").toString();
                case "cancel", "cancel_tip", "close" -> NarcissusComponent.get().transClientAuto("cancel").toString();
                default -> "";
            };
        }

        public String detail() {
            String uuid = deleteConfirmUuid == null ? uiUuid(uiSelected) : deleteConfirmUuid;
            if (uuid.isEmpty()) return activeTabItems().isEmpty() ? emptyText(activeTabColumn()) : "";
            return (deleteConfirmUuid == null ? "" : NarcissusComponent.get().transClientAuto("access_list_del_confirm_msg") + "\n")
                    + playerNameForListUuid(uuid) + "\n" + uuid + (activeTab == AccessListTab.WHITE ? "\n" + whiteModeValue(uuid).enumDescription() : "");
        }

        public String cost() {
            return "";
        }

        private String tr(String key) {
            return NarcissusComponent.get().transClientAuto(key).toString();
        }

        public boolean confirming() {
            return deleteConfirmUuid != null;
        }

        public UiGameElement profileIcon(String key) {
            String uuid = uiUuid(key);
            if (uuid.isEmpty()) return null;
            try {
                return UiGameElement.option(new DropdownOption("", accessHeadTextures(UUID.fromString(uuid)), null));
            } catch (IllegalArgumentException failure) {
                return null;
            }
        }

        public String modeLabel(String key) {
            String uuid = uiUuid(key);
            if (activeTab != AccessListTab.WHITE || uuid.isEmpty()) return "";
            return whiteModeValue(uuid) == EnumWhiteListMode.NONE ? tr("enum_white_list_mode_none") : whiteMode(uuid);
        }

        public AccessUiPresentation presentation() {
            PlayerAccess access = PlayerTeleportData.getData(Minecraft.getInstance().player).getAccess();
            String uuid = uiUuid(uiSelected);
            boolean selected = !uuid.isEmpty();
            String scope = tr(access.getWhiteList().isEmpty() ? "access_scope_open" : "access_scope_white");
            AccessPolicyPresentation policy = AccessPolicyPresentation.of(uuid, access.getBlackList(), access.getWhiteList(), access.getAutoTpaList(), access.getAutoTphList());
            String mode;
            if (selected)
                mode = activeTab == AccessListTab.WHITE ? whiteModeValue(uuid).enumDescription().toString() : "";
            else {
                long tpa = access.getAutoTpaList().stream().filter(id -> !AccessPolicyPresentation.of(id, access.getBlackList(), access.getWhiteList(), access.getAutoTpaList(), access.getAutoTphList()).tpaKey().equals("access_request_ignored")).count();
                long tph = access.getAutoTphList().stream().filter(id -> !AccessPolicyPresentation.of(id, access.getBlackList(), access.getWhiteList(), access.getAutoTpaList(), access.getAutoTphList()).tphKey().equals("access_request_ignored")).count();
                mode = tr("access_auto_tpa") + " " + tpa + "\n" + tr("access_auto_tph") + " " + tph;
            }
            int total = activeTab == AccessListTab.BLACK ? blackItemsAll.size() : whiteItemsAll.size();
            return new AccessUiPresentation(selected ? playerNameForListUuid(uuid) : scope, uuid, mode,
                    tr("access_black_priority"), selected ? tr(policy.tpaKey()) : scope, selected ? tr(policy.tphKey()) : scope,
                    activeTabItems().isEmpty() ? uiQuery.trim().isEmpty() ? emptyText(activeTabColumn()) : tr("access_no_match") : "",
                    activeTabItems().size() + " / " + total, selected, confirming(), activeTab == AccessListTab.WHITE);
        }

        public boolean enabled(NarcissusListUiSession.Action action) {
            if (!uiConnectionCurrent()) return false;
            if (deleteConfirmUuid != null)
                return action == NarcissusListUiSession.Action.CONFIRM || action == NarcissusListUiSession.Action.CANCEL;
            return action == NarcissusListUiSession.Action.ADD
                    || (action == NarcissusListUiSession.Action.EDIT || action == NarcissusListUiSession.Action.DELETE) && !uiUuid(uiSelected).isEmpty();
        }

        public void action(NarcissusListUiSession.Action action) {
            if (!enabled(action)) return;
            switch (action) {
                case ADD -> onTabAddClick();
                case EDIT -> openEditDialog(uiUuid(uiSelected), activeTabColumn());
                case DELETE -> {
                    deleteConfirmUuid = uiUuid(uiSelected);
                    deleteConfirmColumn = activeTabColumn();
                    uiRevision++;
                }
                case CONFIRM -> deleteConfirmed();
                case CANCEL -> {
                    deleteConfirmUuid = null;
                    deleteConfirmColumn = null;
                    uiRevision++;
                }
                default -> {
                }
            }
        }

        public void move(String key, int offset) {
        }

        public void tick() {
            refreshListsIfNeeded();
        }

        public void close() {
        }
    }

    // region Constants

    private static final int PANEL_PADDING = 6;
    private static final int ITEM_HEIGHT = 28;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 2;
    private static final int LIST_PADDING_V = 3;
    private static final int HEAD_SIZE = 16;
    private static final int HEAD_GAP_AFTER = 2;
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
    private final MouseClickTracker clickTracker = new MouseClickTracker();

    private int listHeight;
    private int listBodyW;
    private int listAreaY;
    private int dlgX;
    private int dlgY;

    private AccessListTab activeTab = AccessListTab.BLACK;

    private String deleteConfirmUuid;
    private Column deleteConfirmColumn;
    private String hoveredUuid;
    private Column hoveredColumn;

    private InputWidget searchInput;
    private ButtonWidget addButton;
    private ScrollbarWidget activeScrollbar;
    private ButtonWidget deleteCancelButton;
    private ButtonWidget deleteConfirmButton;

    private int lastAccessFingerprint = Integer.MIN_VALUE;
    private NarcissusScreenChrome.Layout journalLayout;
    private NarcissusScreenChrome.Rect accessListRect;
    private NarcissusScreenChrome.Palette journalPalette;

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
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        loadLists();
    }

    @Override
    protected void initWidgets() {
        journalLayout = NarcissusScreenChrome.layout(width, height);
        journalPalette = NarcissusScreenChrome.palette(getEffectiveTheme());
        int listY = journalLayout.list().y();
        accessListRect = new NarcissusScreenChrome.Rect(journalLayout.list().x(), listY,
                journalLayout.list().width(), journalLayout.content().y() + journalLayout.content().height() - listY);
        listAreaY = accessListRect.y();
        listHeight = accessListRect.height();
        listBodyW = accessListRect.width() - PANEL_PADDING * 2;
        activeScrollbar = null;
        addButton = null;

        dlgX = (width - DIALOG_W) / 2;
        dlgY = (height - DIALOG_H) / 2;

        int cancelW = Math.max(72, font.width(NarcissusComponent.get().transClientAuto("cancel").toString()) + 20);
        int deleteW = Math.max(72, font.width(NarcissusComponent.get().transClientAuto("delete").toString()) + 20);
        int btnY = dlgY + DIALOG_H - 30;
        int cancelX = dlgX + (DIALOG_W - cancelW - deleteW - 10) / 2;
        int deleteX = cancelX + cancelW + 10;

        int searchX = journalLayout.top().x() + compactTabsEndOffset() + 8;
        int searchW = Math.max(50,
                journalLayout.top().x() + journalLayout.top().width() - ADD_BTN_SIZE - 12 - searchX);
        searchInput = new InputWidget(this);
        searchInput.id("search");
        searchInput.value(uiQuery);
        searchInput.bounds(new ScreenCoordinate(searchX, journalLayout.top().y() + 6, searchW, 24));
        searchInput.text(Text.literal(NarcissusComponent.get().transClientAuto("access_list_search_hint").toString()));
        searchInput.onTextChanged(t -> applySearchFilter());
        addWidget(searchInput);

        addButton = new ButtonWidget(this);
        addButton.id("add_access_entry");
        addButton.bounds(new ScreenCoordinate(journalLayout.top().x() + journalLayout.top().width() - ADD_BTN_SIZE - 4,
                journalLayout.top().y() + 11, ADD_BTN_SIZE, ADD_BTN_SIZE));
        addButton.presetStyle(ButtonWidget.PresetStyle.PLUS);
        addButton.radius(ADD_BTN_SIZE / 4f);
        addButton.padding(1);
        addButton.onClick(b -> onTabAddClick());
        addWidget(addButton);

        activeScrollbar = buildActiveScrollbar();
        if (uiState != null) activeScrollbar.value(uiState.rowPosition() * ITEM_HEIGHT);

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
        deleteConfirmButton.onClick(b -> deleteConfirmed());
        addWidget(deleteConfirmButton);

        applySearchFilter();
    }

    private ScrollbarWidget buildActiveScrollbar() {
        int listX = accessListRect.x() + PANEL_PADDING;
        int viewportY = listAreaY + LIST_PADDING_V;
        int viewportHeight = listViewportHeight();
        double maxScroll = maxScrollOffset(activeTabItems());

        ScrollbarWidget bar = new ScrollbarWidget(this);
        bar.id("active_access_scrollbar");
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

    private void onAccessTabSelected(AccessListTab tab) {
        activeTab = tab;
        if (activeScrollbar != null) {
            activeScrollbar.value(0);
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

    private String emptyText(Column column) {
        String title = NarcissusComponent.get().transClientAuto(
                column == Column.BLACK ? "access_list_column_black" : "access_list_column_white").toString();
        return NarcissusComponent.get().transClientAuto("list_is_empty", title).toString();
    }

    private void syncScrollbarLimits() {
        syncOneScrollbar(activeScrollbar, activeTabItems());
    }

    private void syncOneScrollbar(ScrollbarWidget bar, List<String> items) {
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
        String raw = searchInput != null ? searchInput.value() : uiQuery;
        uiQuery = raw;
        uiRevision++;
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
            Texture[] textures = accessHeadTextures(UUID.fromString(uuid));
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
                    if (!uiConnectionCurrent()) return;
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
            Texture[] textures = accessHeadTextures(UUID.fromString(uuid));
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
                    if (!uiConnectionCurrent()) return;
                    String uuid = nameToUuid.get(label);
                    String mode = results.value("white_mode").trim();
                    if (uuid != null) {
                        PacketUtils.sendPacketToServer(new AccessListEditToServer(2, mode, uuid));
                    }
                });
        minecraft.setScreen(new InputFormScreen(args));
    }

    private void openEditDialog(String currentUuid, Column column) {
        if (minecraft == null || minecraft.player == null) return;
        uiSelected = column.name() + ":" + currentUuid;
        uiRevision++;
        LinkedHashMap<String, String> nameToUuid = collectOnlinePlayerNameToUuid();
        String currentName = playerNameForListUuid(currentUuid);
        nameToUuid.put(currentName, currentUuid);

        List<DropdownOption> options = new ArrayList<>();
        nameToUuid.forEach((label, uuid) -> options.add(new DropdownOption(label,
                accessHeadTextures(UUID.fromString(uuid)),
                NarcissusComponent.get().literal(uuid))));

        InputFormScreen.Args args = new InputFormScreen.Args()
                .setParentScreen(this)
                .setTitle(Text.literal(NarcissusComponent.get().transClientAuto(
                        column == Column.BLACK ? "access_list_edit_black_title"
                                : "access_list_edit_white_title").toString()))
                .addWidget(new InputFormScreen.Widget()
                        .name("player_name")
                        .type(InputFormScreen.WidgetType.DROPDOWN)
                        .dropdownOptionEntries(options)
                        .defaultValue(currentName)
                        .title(Text.literal(NarcissusComponent.get()
                                .transClientAuto("access_list_player_name").toString()))
                        .hint(Text.literal(BaniraComponent.get().transClientAuto("choose_option").toString())));
        if (column == Column.WHITE) {
            args.addWidget(new InputFormScreen.Widget()
                    .name("white_mode")
                    .type(InputFormScreen.WidgetType.DROPDOWN)
                    .dropdownOptions(EnumWhiteListMode.class)
                    .title(Text.literal(NarcissusComponent.get()
                            .transClientAuto("access_list_white_mode").toString()))
                    .defaultValue(whiteModeValue(currentUuid))
                    .hint(Text.literal(BaniraComponent.get().transClientAuto("choose_option").toString())));
        }
        args.setCallback(results -> {
            if (!uiConnectionCurrent()) return;
            String selectedUuid = nameToUuid.get(results.value("player_name").trim());
            if (selectedUuid == null) return;
            if (column == Column.BLACK) {
                if (!currentUuid.equals(selectedUuid)) {
                    PacketUtils.sendPacketToServer(new AccessListEditToServer(1, "", currentUuid));
                    PacketUtils.sendPacketToServer(new AccessListEditToServer(0, "", selectedUuid));
                }
            } else {
                String mode = results.value("white_mode").trim();
                PacketUtils.sendPacketToServer(new AccessListEditToServer(
                        3, EnumWhiteListMode.NONE.name(), currentUuid));
                PacketUtils.sendPacketToServer(new AccessListEditToServer(2, mode, selectedUuid));
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
        list.sort(Comparator.comparing(a -> a.getProfile().name(), String.CASE_INSENSITIVE_ORDER));
        for (PlayerInfo info : list) {
            if (info.getProfile() == null || info.getProfile().id() == null) {
                continue;
            }
            String name = info.getProfile().name();
            if (StringUtils.isNullOrEmptyEx(name)) {
                continue;
            }
            map.put(name, info.getProfile().id().toString());
        }
        return map;
    }

    private void notifyNoOnlinePlayers() {
        if (minecraft == null || minecraft.gui == null) {
            return;
        }
        minecraft.gui.getChat().addClientSystemMessage(NarcissusComponent.get().transClientAuto("access_list_no_online_players").toChat());
    }

    @Override
    protected void onRender(@Nonnull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        Matrix3x2fStack stack = graphics.pose();

        refreshListsIfNeeded();

        BaniraColorConfig theme = getEffectiveTheme();
        journalPalette = NarcissusScreenChrome.palette(theme);
        boolean dialogOpen = deleteConfirmUuid != null;
        if (activeScrollbar != null) {
            boolean scroll = !dialogOpen && maxScrollOffset(activeTabItems()) > 0.0D;
            activeScrollbar.visible(scroll);
            activeScrollbar.enabled(scroll);
        }
        if (searchInput != null) {
            searchInput.enabled(!dialogOpen);
        }
        if (addButton != null) {
            addButton.visible(!dialogOpen);
            addButton.enabled(!dialogOpen);
        }
        if (deleteCancelButton != null) {
            deleteCancelButton.visible(dialogOpen);
            deleteConfirmButton.visible(dialogOpen);
        }

        NarcissusScreenChrome.drawJournal(stack, journalPalette, journalLayout);
        drawAccessTabs(stack, mouseX, mouseY);
        drawActiveList(stack, mouseX, mouseY);

        if (hoveredUuid != null && !dialogOpen) {
            addDeferredTooltipRender(s -> drawAccessTooltip(s.pose(), theme, hoveredUuid, hoveredColumn, mouseX, mouseY));
        }

        if (dialogOpen) {
            drawDeleteConfirmOverlay(stack, theme);
        }

        renderWidgets(graphics, partialTicks);
    }

    private void drawLimitedTextLine(Matrix3x2fStack stack, String text, double x, double y, int maxWidth, int colorArgb) {
        LabelWidget.drawLimitedText(FontDrawArgs.of(Text.literal(text).stack(stack).font(font).color(Color.argb(colorArgb)))
                .x(x).y(y)
                .maxWidth(maxWidth)
                .align(EnumAlignment.START)
                .wrap(false)
                .inScreen(false));
    }

    private void drawLimitedTextCentered(Matrix3x2fStack stack, String text, double x, double y, int maxWidth, int colorArgb) {
        LabelWidget.drawLimitedText(FontDrawArgs.of(Text.literal(text).stack(stack).font(font).color(Color.argb(colorArgb)))
                .x(x).y(y)
                .maxWidth(maxWidth)
                .align(EnumAlignment.CENTER)
                .wrap(false)
                .inScreen(false));
    }

    private void drawAccessTabs(Matrix3x2fStack stack, int mouseX, int mouseY) {
        AccessListTab[] tabs = AccessListTab.values();
        for (int i = 0; i < tabs.length; i++) {
            AccessListTab tab = tabs[i];
            NarcissusScreenChrome.Rect rect = tabRect(i);
            boolean hovered = contains(rect, mouseX, mouseY);
            NarcissusScreenChrome.drawCompactTab(stack, journalPalette, rect, hovered, tab == activeTab);
            String name = NarcissusComponent.get().transClientAuto(
                    tab == AccessListTab.BLACK ? "access_list_column_black" : "access_list_column_white").toString();
            int count = tab == AccessListTab.BLACK ? blackItems.size() : whiteItems.size();
            String label = name + "  " + count;
            int textWidth = Math.min(font.width(label), rect.width() - 16);
            drawLimitedTextLine(stack, label, rect.x() + (rect.width() - textWidth) / 2,
                    rect.y() + (rect.height() - font.lineHeight) / 2,
                    textWidth, tab == activeTab ? journalPalette.primary() : journalPalette.secondary());
        }
    }

    private void drawActiveList(Matrix3x2fStack stack, int mouseX, int mouseY) {
        List<String> items = activeTabItems();
        Column column = activeTabColumn();
        int listX = accessListRect.x() + PANEL_PADDING;
        NarcissusScreenChrome.ListViewport viewport = activeViewport(items);
        boolean scrollNeeded = viewport.maxOffset() > 0.0D;
        int cw = listBodyW - (scrollNeeded ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);

        if (items.isEmpty()) {
            drawLimitedTextCentered(stack, emptyText(column), listX,
                    listAreaY + listHeight / 2 - font.lineHeight / 2,
                    cw, journalPalette.secondary());
            hoveredUuid = null;
            hoveredColumn = null;
            return;
        }

        PlayerAccess access = minecraft != null && minecraft.player != null
                ? PlayerTeleportData.getData(minecraft.player).getAccess()
                : null;

        int headSlot = 6 + HEAD_SIZE + HEAD_GAP_AFTER;
        int textMaxW = Math.max(8, cw - headSlot - 16);
        hoveredUuid = null;
        hoveredColumn = null;

        int viewportY = listAreaY + LIST_PADDING_V;
        AbstractGuiUtils.pushScissor(listX, viewportY, cw, listViewportHeight());
        try {
            for (int idx = viewport.firstIndex(); idx < viewport.lastIndexExclusive(); idx++) {
                String uuidStr = items.get(idx);
                int itemY = (int) Math.floor(viewport.rowY(idx));
                int rowH = ITEM_HEIGHT;
                int rowDrawH = Math.max(1, rowH - 1);

                boolean hover = mouseX >= listX && mouseX < listX + cw
                        && viewport.itemIndexAt(mouseY) == idx;
                if (hover) {
                    hoveredUuid = uuidStr;
                    hoveredColumn = column;
                }

                NarcissusScreenChrome.drawJournalListRow(stack, journalPalette,
                        new NarcissusScreenChrome.Rect(listX, itemY, cw, rowDrawH),
                        true, hover, false);

                int headX = listX + 6;
                int headY = itemY + (rowH - HEAD_SIZE) / 2;
                drawPlayerHeadFace(stack, uuidStr, headX, headY, HEAD_SIZE);

                int textX = listX + headSlot;
                String titleLine = playerNameForListUuid(uuidStr);
                drawLimitedTextLine(stack, titleLine, textX, itemY + 2, textMaxW, journalPalette.primary());

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
                drawLimitedTextLine(stack, meta, textX, itemY + 12, textMaxW, journalPalette.secondary());

                int delX = listX + cw - 16;
                int delY = itemY + (rowH - 10) / 2;
                boolean delHover = mouseX >= delX && mouseX <= delX + 14 && mouseY >= delY && mouseY < delY + 10;
                drawLimitedTextLine(stack, "×", delX + 2, delY, 14,
                        delHover ? journalPalette.danger() : journalPalette.secondary());
            }
        } finally {
            AbstractGuiUtils.popScissor();
        }
    }

    private NarcissusScreenChrome.Rect tabRect(int index) {
        AccessListTab[] tabs = AccessListTab.values();
        int offset = 4;
        for (int i = 0; i < index; i++) {
            offset += compactTabWidth(tabs[i]) + 4;
        }
        return NarcissusScreenChrome.compactTabRect(
                journalLayout.top(), offset, compactTabWidth(tabs[index]));
    }

    private int compactTabsEndOffset() {
        int offset = 4;
        AccessListTab[] tabs = AccessListTab.values();
        for (int i = 0; i < tabs.length; i++) {
            offset += compactTabWidth(tabs[i]);
            if (i + 1 < tabs.length) {
                offset += 4;
            }
        }
        return offset;
    }

    private int compactTabWidth(AccessListTab tab) {
        String name = NarcissusComponent.get().transClientAuto(
                tab == AccessListTab.BLACK ? "access_list_column_black" : "access_list_column_white").toString();
        return Math.max(52, font.width(name + "  999") + 12);
    }

    private static boolean contains(NarcissusScreenChrome.Rect rect, double x, double y) {
        return x >= rect.x() && x < rect.x() + rect.width()
                && y >= rect.y() && y < rect.y() + rect.height();
    }

    private static void drawPlayerHeadFace(Matrix3x2fStack stack, String uuidStr, int x, int y, int size) {
        UUID uuid;
        try {
            uuid = UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return;
        }
        Texture[] tex = accessHeadTextures(uuid);
        if (tex == null || tex.length < 2 || tex[0] == null) {
            return;
        }
        ImageWidget.blit(stack, tex[0], x, y, size, size);
        if (tex[1] != null) {
            ImageWidget.blitBlend(stack, tex[1], x, y, size, size);
        }
    }

    private void drawAccessTooltip(Matrix3x2fStack stack, BaniraColorConfig theme, String uuid,
                                   Column column, int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        lines.add(playerNameForListUuid(uuid));
        lines.add("UUID: " + uuid);
        if (column == Column.WHITE) {
            EnumWhiteListMode mode = whiteModeValue(uuid);
            if (mode != EnumWhiteListMode.NONE) {
                lines.add(mode.enumDescription().toString());
            }
        }
        Text tooltipText = Text.literal(String.join("\n", lines)).stack(stack).font(font).color(Color.argb(theme.textPrimary()));
        FontDrawArgs args = FontDrawArgs.ofPopo(tooltipText)
                .x(mouseX)
                .y(mouseY);
        TooltipWidget.drawPopupMessage(stack, args, theme, season());
    }

    private String whiteMode(String uuid) {
        EnumWhiteListMode mode = whiteModeValue(uuid);
        if (mode == EnumWhiteListMode.BOTH) return "TPA + TPH";
        if (mode == EnumWhiteListMode.AUTO_ACCEPT_TPA) return "TPA";
        if (mode == EnumWhiteListMode.AUTO_ACCEPT_TPH) return "TPH";
        return "NONE";
    }

    private EnumWhiteListMode whiteModeValue(String uuid) {
        if (minecraft == null || minecraft.player == null) {
            return EnumWhiteListMode.NONE;
        }
        PlayerAccess access = PlayerTeleportData.getData(minecraft.player).getAccess();
        boolean tpa = access.getAutoTpaList().contains(uuid);
        boolean tph = access.getAutoTphList().contains(uuid);
        if (tpa && tph) return EnumWhiteListMode.BOTH;
        if (tpa) return EnumWhiteListMode.AUTO_ACCEPT_TPA;
        if (tph) return EnumWhiteListMode.AUTO_ACCEPT_TPH;
        return EnumWhiteListMode.NONE;
    }

    private void drawDeleteConfirmOverlay(Matrix3x2fStack stack, BaniraColorConfig theme) {
        ShapeDrawArgs dim = ShapeDrawArgs.rect(stack, 0, 0, width, height, ColorUtils.applyAlphaToArgb(theme.bgQuaternary(), 0x78));
        BaseShapeWidget.drawShape(dim);

        NarcissusScreenChrome.drawDialog(stack, journalPalette, dlgX, dlgY, DIALOG_W, DIALOG_H);

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

        AccessListTab[] tabs = AccessListTab.values();
        for (int i = 0; i < tabs.length; i++) {
            if (contains(tabRect(i), mouseX, mouseY)) {
                onAccessTabSelected(tabs[i]);
                eventArgs.consumed(true);
                super.onMouseClicked(eventArgs);
                return;
            }
        }
        boolean doubleClick = clickTracker.record(mouseX, mouseY, eventArgs.button()).doubleClick();
        if (checkPanelClick(mouseX, mouseY, accessListRect.x() + PANEL_PADDING,
                listAreaY + LIST_PADDING_V, activeTabItems(), activeScrollbar, activeTabColumn(),
                doubleClick)) {
            eventArgs.consumed(true);
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

    private boolean checkPanelClick(double mouseX, double mouseY, int listX, int listY,
                                    List<String> items, ScrollbarWidget bar, Column column,
                                    boolean doubleClick) {
        NarcissusScreenChrome.ListViewport viewport = NarcissusScreenChrome.listViewport(
                items.size(), ITEM_HEIGHT, listY, listViewportHeight(), bar != null ? bar.value() : 0.0D);
        boolean scrollNeeded = viewport.maxOffset() > 0.0D;
        int cw = listBodyW - (scrollNeeded ? SCROLLBAR_WIDTH + SCROLLBAR_GAP : 0);
        int idx = viewport.itemIndexAt(mouseY);
        if (idx < 0 || mouseX < listX || mouseX >= listX + cw) {
            return false;
        }
        int delX = listX + cw - 16;
        if (mouseX >= delX) {
            deleteConfirmUuid = items.get(idx);
            deleteConfirmColumn = column;
            return true;
        }
        if (doubleClick) {
            openEditDialog(items.get(idx), column);
            return true;
        }
        return false;
    }
}
