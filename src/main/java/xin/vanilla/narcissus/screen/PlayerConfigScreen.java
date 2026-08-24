package xin.vanilla.narcissus.screen;

import lombok.Data;
import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import xin.vanilla.banira.client.data.BaniraColorConfig;
import xin.vanilla.banira.client.data.ScreenCoordinate;
import xin.vanilla.banira.client.enums.EnumAlignment;
import xin.vanilla.banira.client.gui.BaniraScreen;
import xin.vanilla.banira.client.gui.component.Notification;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.client.gui.event.MouseEvent;
import xin.vanilla.banira.client.gui.widget.BaseWidget;
import xin.vanilla.banira.client.gui.widget.CollapsiblePanelWidget;
import xin.vanilla.banira.client.gui.widget.IWidget;
import xin.vanilla.banira.client.gui.widget.LabelWidget;
import xin.vanilla.banira.client.gui.widget.SliderWidget;
import xin.vanilla.banira.client.util.NotificationManager;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.enums.EnumSeason;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
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

    public PlayerConfigScreen(@Nullable Args args) {
        this(args != null ? args : new Args(), true);
    }

    private PlayerConfigScreen(Args args, boolean ignored) {
        super(NarcissusFarewell.MODID, NarcissusComponent.get().transClientAuto("tp_prefs_screen_title"),
                args.parentScreen(), args.theme(), args.season());
    }

    @Data
    @Accessors(chain = true, fluent = true)
    public static class Args {
        @Nullable private Screen parentScreen;
        @Nullable private BaniraColorConfig theme;
        @Nullable private EnumSeason season;
    }

    @Override
    protected void buildPlayerConfig(CollapsiblePanelWidget root) {
        sliders.clear();
        Component sectionTitle = NarcissusComponent.get().transClientAuto("tp_prefs_countdown_title");
        CollapsiblePanelWidget countdown = addPlayerSection(root, "teleport_countdown", sectionTitle, null);
        LocalPlayer player = Minecraft.getInstance().player;
        PlayerTeleportData data = player != null ? PlayerTeleportData.getData(player) : null;
        for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) {
            addCountdownRow(countdown, type, data);
        }
        countdown.refreshLayout();
    }

    private void addCountdownRow(CollapsiblePanelWidget section, EnumTeleportType type,
                                 @Nullable PlayerTeleportData data) {
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

        int current = data != null ? data.getTeleportCountdownSeconds(type) : 0;
        SliderWidget slider = new SliderWidget(this);
        slider.id("tp_prefs_sl_" + type.name());
        slider.bounds(new ScreenCoordinate(valueStartX(rowWidth), 0, valueWidgetWidth(rowWidth), ROW_HEIGHT));
        slider.minValue(TeleportCountdownHelper.playerRangeLo())
                .maxValue(TeleportCountdownHelper.playerRangeHi())
                .step(1).decimalPlaces(0)
                .value(TeleportCountdownHelper.clampToPlayerAllowedRange(current));
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

    private CompoundTag buildFullCountdownTag() {
        CompoundTag tag = new CompoundTag();
        for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) {
            SliderWidget slider = sliders.get(type);
            int value = slider != null ? (int) Math.round(slider.value()) : 0;
            tag.putInt(type.name(), TeleportCountdownHelper.clampToPlayerAllowedRange(value));
        }
        return tag;
    }

    @Override
    protected void savePlayerConfig() {
        if (Minecraft.getInstance().getConnection() == null) {
            showNotification("tp_prefs_sync_not_connected", 3500, null);
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        try {
            CompoundTag payload = buildFullCountdownTag();
            PacketUtils.sendPacketToServer(new PlayerConfigSyncToServer(payload));
            PlayerTeleportData.getData(player).replaceAllTeleportCountdownsFromTag(payload);
            showNotification("tp_prefs_sync_ok", 2500, null);
            onClose();
        } catch (Exception exception) {
            showNotification("tp_prefs_sync_failed", 4000,
                    exception.getMessage() != null ? exception.getMessage() : "");
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
