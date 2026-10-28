package xin.vanilla.narcissus.screen;

import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import xin.vanilla.banira.api.client.ui.UiPageSpec;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.widget.*;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.banira.internal.client.ui.page.PlayerConfigUiSession;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.client.ui.TeleportPreferencesUiSession;
import xin.vanilla.narcissus.config.TeleportCountdownHelper;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.network.packet.PlayerConfigSyncToServer;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

/**
 * 编辑玩家传送相关偏好。
 */
public class PlayerConfigScreen extends xin.vanilla.banira.client.gui.PlayerConfigScreen {
    private static final double LABEL_COLUMN_WIDTH_RATIO = 0.32;
    private static final double LABEL_COLUMN_MIN_WIDTH = 64;
    private static final int GAP_LABEL_TO_VALUE = 4;
    private static final double VALUE_AREA_MIN_WIDTH = 56;

    private final Map<EnumTeleportType, SliderWidget> sliders = new EnumMap<>(EnumTeleportType.class);
    private final Object uiConnection = Minecraft.getInstance().getConnection();
    private final TeleportPreferencesUiSession preferences;
    private UiPageSpec page;
    private xin.vanilla.banira.api.client.ui.UiPageState preferenceUiState;

    public PlayerConfigScreen(@Nullable Args args) {
        this(args != null ? args : new Args(), true);
    }

    private PlayerConfigScreen(Args args, boolean ignored) {
        super(NarcissusFarewell.MODID, NarcissusComponent.get().transClientAuto("tp_prefs_screen_title"),
                args.parentScreen(), args.theme(), args.season());
        preferences = new TeleportPreferencesUiSession(this, type -> {
            LocalPlayer player = Minecraft.getInstance().player;
            return player == null ? 0 : PlayerTeleportData.getData(player).getTeleportCountdownSeconds(type);
        }, this::uiConnectionCurrent, this::savePreferences);
    }

    private boolean uiConnectionCurrent() {
        return uiConnection != null && Minecraft.getInstance().getConnection() == uiConnection && Minecraft.getInstance().player != null;
    }

    @Override
    protected PlayerConfigUiSession playerUiSession() {
        return preferences.shared();
    }

    @Override
    public String uiSurfaceId() {
        return "narcissus_farewell:player";
    }

    @Override
    public UiPageSpec uiPage() {
        if (getClass() != PlayerConfigScreen.class) return null;
        if (page == null) page = preferences.shared().spec(this, uiSurfaceId());
        return page;
    }

    @Override
    public void captureUiState(xin.vanilla.banira.internal.client.ui.UiViewState state) {
        for (Map.Entry<EnumTeleportType, SliderWidget> entry : sliders.entrySet()) {
            preferences.draft(entry.getKey(), entry.getValue().editingText());
            NumericInputWidget input = entry.getValue().inlineInputWidget();
            if (state != null && input != null && input.focused())
                state.selection(countdownFocusId(entry.getKey()), input.cursorPosition(), input.highlightPos());
        }
        super.captureUiState(state);
    }

    @Override
    public void prepareUiState(xin.vanilla.banira.internal.client.ui.UiViewState state) {
        super.prepareUiState(state);
        preferenceUiState = state;
    }

    @Override
    protected void afterPlayerConfigBuilt() {
        super.afterPlayerConfigBuilt();
        if (preferenceUiState == null) return;
        for (Map.Entry<EnumTeleportType, SliderWidget> entry : sliders.entrySet()) {
            NumericInputWidget input = entry.getValue().inlineInputWidget();
            if (input != null && preferenceUiState.focusedField().equals(countdownFocusId(entry.getKey()))) {
                requestFocus(input);
                input.setSelectionRange(preferenceUiState.selectionStart(), preferenceUiState.selectionEnd());
            }
        }
    }

    static String countdownFocusId(EnumTeleportType type) {
        if (type == null) return "";
        int index = EnumTeleportType.countdownConfigurableTypes().indexOf(type);
        return index < 0 ? "" : "field:" + index + "/input";
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class Args {
        @Nullable
        private Screen parentScreen;
        @Nullable
        private BaniraColorConfig theme;
        @Nullable
        private EnumSeason season;
    }

    @Override
    protected void buildPlayerConfig(CollapsiblePanelWidget root) {
        sliders.clear();
        Component sectionTitle = NarcissusComponent.get().transClientAuto("tp_prefs_countdown_title");
        CollapsiblePanelWidget countdown = addPlayerSection(root, "teleport_countdown", sectionTitle, null);
        for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) {
            addCountdownRow(countdown, type);
        }
        countdown.refreshLayout();
    }

    private void addCountdownRow(CollapsiblePanelWidget section, EnumTeleportType type) {
        double rowWidth = section.getContentWidth();
        EntryRowWidget row = new EntryRowWidget(this);
        row.id("tp_prefs_" + type.name().toLowerCase(java.util.Locale.ROOT));
        row.bounds(new ScreenCoordinate(0, 0, rowWidth, ROW_HEIGHT));

        Component title = type.enumDescription();
        LabelWidget label = new LabelWidget(this);
        label.id("tp_prefs_lbl_" + type.name());
        label.bounds(new ScreenCoordinate(0, 0, labelTextWidth(rowWidth), ROW_HEIGHT));
        label.text(Text.from(title));
        label.textWrap(false);
        label.textVerticalAlign(EnumAlignment.CENTER);

        int current = preferences.seconds(type);
        SliderWidget slider = new SliderWidget(this);
        slider.id("tp_prefs_sl_" + type.name());
        slider.bounds(new ScreenCoordinate(valueStartX(rowWidth), 0, valueWidgetWidth(rowWidth), ROW_HEIGHT));
        slider.minValue(TeleportCountdownHelper.playerRangeLo())
                .maxValue(TeleportCountdownHelper.playerRangeHi())
                .step(1).decimalPlaces(0)
                .value(TeleportCountdownHelper.clampToPlayerAllowedRange(current));
        slider.onValueChanged(value -> preferences.seconds(type, (int) Math.round(value)));
        slider.editingText(preferences.draft(type));
        sliders.put(type, slider);

        row.addChild(label);
        row.addChild(slider);
        addPlayerRow(section, row, ROW_HEIGHT, label, null, title, null,
                type.name(), "countdown", "teleportCountdownSeconds");
    }

    private double labelColumnEndX(double rowWidth) {
        double maxEnd = rowWidth - VALUE_AREA_MIN_WIDTH;
        if (maxEnd < 1) return Math.max(1, rowWidth * 0.2);
        return Math.min(Math.max(LABEL_COLUMN_MIN_WIDTH,
                Math.min(rowWidth * LABEL_COLUMN_WIDTH_RATIO, maxEnd)), maxEnd);
    }

    private double labelTextWidth(double rowWidth) {
        return Math.max(1, labelColumnEndX(rowWidth) - GAP_LABEL_TO_VALUE);
    }

    private double valueStartX(double rowWidth) {
        return labelColumnEndX(rowWidth);
    }

    private double valueWidgetWidth(double rowWidth) {
        return Math.max(1, rowWidth - labelColumnEndX(rowWidth));
    }

    @Override
    protected void savePlayerConfig() {
        if (!uiConnectionCurrent()) {
            showNotification("tp_prefs_sync_not_connected", 3500, null);
            return;
        }
        captureUiState(null);
        if (!preferences.shared().fields().canSubmit()) {
            showNotification("tp_prefs_sync_failed", 4000,
                    xin.vanilla.banira.BaniraComponent.get().transClientAuto("config_editor_validation_failed").toString());
            return;
        }
        if (preferences.submit()) onClose();
    }

    private void savePreferences(CompoundTag payload) {
        if (!uiConnectionCurrent()) throw new IllegalStateException(
                NarcissusComponent.get().transClientAuto("tp_prefs_sync_not_connected").toString());
        LocalPlayer player = Minecraft.getInstance().player;
        try {
            PacketUtils.sendPacketToServer(new PlayerConfigSyncToServer(payload));
            PlayerTeleportData.getData(player).replaceAllTeleportCountdownsFromTag(payload);
            showNotification("tp_prefs_sync_ok", 2500, null);
        } catch (Exception exception) {
            showNotification("tp_prefs_sync_failed", 4000,
                    exception.getMessage() != null ? exception.getMessage() : "");
            throw new IllegalStateException(exception);
        }
    }

    private void showNotification(String key, long duration, @Nullable String detail) {
        Notification notification = Notification.ofComponent(detail == null
                ? NarcissusComponent.get().transClientAuto(key)
                : NarcissusComponent.get().transClientAuto(key, detail));
        notification.position(EnumPosition.TOP_RIGHT).durationTime(duration);
        NotificationManager.get().addNotification(notification);
    }

    private static final class EntryRowWidget extends BaseWidget {
        private EntryRowWidget(BaniraScreen screen) {
            super(screen);
        }

        @Override
        public double effectiveHeight() {
            double maxBottom = 0;
            for (IWidget child : children()) {
                if (child == null || !child.visible() || child.bounds() == null) continue;
                maxBottom = Math.max(maxBottom, child.bounds().y() + child.effectiveHeight());
            }
            return maxBottom > 0 ? maxBottom : (bounds() != null ? bounds().height() : 0);
        }

        @Override
        protected boolean onMouseClick(MouseEvent event) {
            return true;
        }

        @Override
        public void render(GuiGraphics graphics, float partialTicks) {
            if (visible()) renderChildren(graphics, partialTicks);
        }
    }
}
