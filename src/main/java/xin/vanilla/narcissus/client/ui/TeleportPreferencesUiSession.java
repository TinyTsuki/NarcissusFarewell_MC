package xin.vanilla.narcissus.client.ui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import xin.vanilla.banira.client.gui.InputFormScreen;
import xin.vanilla.banira.client.gui.component.Text;
import xin.vanilla.banira.internal.client.ui.page.PlayerConfigUiSession;
import xin.vanilla.banira.internal.client.ui.page.PlayerConfigUiPresentation;
import xin.vanilla.banira.BaniraComponent;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.TeleportCountdownHelper;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;

public final class TeleportPreferencesUiSession {
    private final PlayerConfigUiSession shared;
    private final BooleanSupplier connection;

    public TeleportPreferencesUiSession(Screen owner, ToIntFunction<EnumTeleportType> initial,
                                         BooleanSupplier connection, Consumer<CompoundTag> save) {
        this.connection = connection;
        InputFormScreen.Args args = new InputFormScreen.Args().setParentScreen(owner)
                .setTitle(Text.from(NarcissusComponent.get().transClientAuto("tp_prefs_screen_title")))
                .setCallback(result -> {
                    if (!connection.getAsBoolean()) throw new IllegalStateException(
                            NarcissusComponent.get().transClientAuto("tp_prefs_sync_not_connected").toString());
                    CompoundTag payload = new CompoundTag();
                    for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes())
                        payload.putInt(type.name(), TeleportCountdownHelper.clampToPlayerAllowedRange(Integer.parseInt(result.value(type.name()))));
                    save.accept(payload);
                });
        for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) {
            args.addWidget(new InputFormScreen.Widget().name(type.name()).type(InputFormScreen.WidgetType.NUMERIC)
                    .title(Text.from(type.enumDescription())).tooltip(Text.from(type.enumDescription()))
                    .defaultValue(Integer.toString(TeleportCountdownHelper.clampToPlayerAllowedRange(initial.applyAsInt(type))))
                    .validator(result -> valid(result.value(type.name())) ? "" : xin.vanilla.banira.BaniraComponent.get()
                            .transClientAuto("config_editor_validation_failed").toString()));
        }
        shared = new PlayerConfigUiSession(args).presentation(new PlayerConfigUiPresentation(
                NarcissusFarewell.MODID, NarcissusComponent.get().literal("NARCISSUS FAREWELL"),
                NarcissusComponent.get().transClientAuto("tp_prefs_countdown_title"),
                BaniraComponent.get().transClientAuto("player_config_seconds"),
                () -> BaniraComponent.get().transClientAuto("player_config_seconds_range",
                        TeleportCountdownHelper.playerRangeLo(), TeleportCountdownHelper.playerRangeHi())));
    }
    public static boolean valid(String value) {
        try {
            int integer = Integer.parseInt(value);
            return integer >= TeleportCountdownHelper.playerRangeLo() && integer <= TeleportCountdownHelper.playerRangeHi();
        } catch (NumberFormatException invalid) { return false; }
    }
    public PlayerConfigUiSession shared() { return shared; }
    public String draft(EnumTeleportType type) { return shared.fields().draft(type.name()); }
    public void draft(EnumTeleportType type, String value) { shared.fields().draft(type.name(), value); }
    public int seconds(EnumTeleportType type) {
        try { return TeleportCountdownHelper.clampToPlayerAllowedRange(Integer.parseInt(draft(type))); }
        catch (NumberFormatException invalid) { return 0; }
    }
    public void seconds(EnumTeleportType type, int value) { shared.fields().draft(type.name(), Integer.toString(value)); }
    public boolean submit() { return connection.getAsBoolean() && shared.fields().submit(); }
}
