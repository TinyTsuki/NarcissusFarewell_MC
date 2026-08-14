package xin.vanilla.narcissus.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.banira.client.data.*;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.enums.EnumOrientation;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.AbstractGuiUtils;
import xin.vanilla.banira.client.util.PlayerSkinTextureUtils;
import xin.vanilla.banira.common.data.Color;
import xin.vanilla.banira.common.util.ColorUtils;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.common.util.PlayerUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.data.PlayerAccess;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumWhiteListMode;
import xin.vanilla.narcissus.network.packet.AccessListEditToServer;

import javax.annotation.Nonnull;
import java.util.*;

@Accessors(chain = true, fluent = true)
public class AccessListScreen extends BaniraScreen {

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
        PoseStack stack = graphics.pose();

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

    private void drawAccessTabs(PoseStack stack, int mouseX, int mouseY) {
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

    private void drawActiveList(PoseStack stack, int mouseX, int mouseY) {
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

    private void drawAccessTooltip(PoseStack stack, BaniraColorConfig theme, String uuid,
                                   Column column, int mouseX, int mouseY) {
        List<String> lines = new ArrayList<>();
        lines.add(playerNameForListUuid(uuid));
        lines.add("UUID: " + uuid);
        if (column == Column.WHITE) {
            lines.add(NarcissusComponent.get().transClientAuto("access_list_white_mode").toString()
                    + ": " + whiteMode(uuid));
        }
        lines.add(NarcissusComponent.get().transClientAuto(
                column == Column.BLACK ? "blacklist_help" : "whitelist_help").toString());
        Text tooltipText = Text.literal(String.join("\n", lines)).stack(stack).font(font).color(Color.argb(theme.textPrimary()));
        FontDrawArgs args = FontDrawArgs.ofPopo(tooltipText)
                .x(mouseX)
                .y(mouseY);
        TooltipWidget.drawPopupMessage(stack, args, theme, season());
    }

    private String whiteMode(String uuid) {
        if (minecraft == null || minecraft.player == null) {
            return "NONE";
        }
        PlayerAccess access = PlayerTeleportData.getData(minecraft.player).getAccess();
        boolean tpa = access.getAutoTpaList().contains(uuid);
        boolean tph = access.getAutoTphList().contains(uuid);
        if (tpa && tph) return "TPA + TPH";
        if (tpa) return "TPA";
        if (tph) return "TPH";
        return "NONE";
    }

    private void drawDeleteConfirmOverlay(PoseStack stack, BaniraColorConfig theme) {
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
        if (checkPanelClick(mouseX, mouseY, accessListRect.x() + PANEL_PADDING,
                listAreaY + LIST_PADDING_V, activeTabItems(), activeScrollbar, activeTabColumn())) {
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

    private boolean checkPanelClick(double mouseX, double mouseY, int listX, int listY, List<String> items, ScrollbarWidget bar, Column column) {
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
        return false;
    }
}
