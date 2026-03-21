package xin.vanilla.narcissus.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraftforge.fml.config.ModConfig;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ForgeConfigAdapter;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.narcissus.config.access.ClientConfigAccess;
import xin.vanilla.narcissus.enums.EnumWaypointPanelMode;

/**
 * 客户端配置：注解结构用于 ForgeConfigSpec；运行时通过 {@link #get()} 返回的 {@link RootView} 分层读取。
 * GUI 说明与项目根目录 {@code narcissus_farewell-client.toml} 中的注释一致。
 */
@Config(name = "narcissus_farewell-client", type = ModConfig.Type.CLIENT)
public class ClientConfig implements ConfigData {

    public ClientConfig() {
    }

    // region 配置结构

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "客户端设置", en_us = "Client settings")
    private ClientRootCategory client = new ClientRootCategory();

    // endregion 配置结构


    public static RootView get() {
        return ClientConfigAccess.root(ForgeConfigAdapter.getHolder(ClientConfig.class));
    }

    public static void save() {
        ConfigHolder h = ForgeConfigAdapter.getHolder(ClientConfig.class);
        if (h != null) {
            h.save();
        }
    }


    @Getter
    @Setter
    @Accessors(chain = true, fluent = true)
    public static class ClientRootCategory {
        @ConfigEntry.Gui.Tooltip(zh_cn = "创建家时同步地图路标", en_us = "Sync map waypoint when setting home")
        private boolean syncHomeMapWaypoint = true;

        @ConfigEntry.Gui.Tooltip(zh_cn = "创建驿站时同步地图路标", en_us = "Sync map waypoint when setting stage")
        private boolean syncStageMapWaypoint = true;

        @ConfigEntry.Gui.Tooltip(zh_cn = "传送路标界面布局：三列或 Tab 单栏；在界面内切换布局时会写入此选项。", en_us = "Waypoint screen layout: three columns or tabbed panel; toggling layout in the GUI updates this option.")
        private EnumWaypointPanelMode waypointScreenPanelMode = EnumWaypointPanelMode.THREE_COLUMNS;
    }

    // region 运行时视图接口

    public interface RootView {
        ClientView client();

        ConfigHolder holder();

        void save();
    }

    public interface ClientView {
        boolean syncHomeMapWaypoint();

        ClientView syncHomeMapWaypoint(boolean value);

        boolean syncStageMapWaypoint();

        ClientView syncStageMapWaypoint(boolean value);

        EnumWaypointPanelMode waypointScreenPanelMode();

        ClientView waypointScreenPanelMode(EnumWaypointPanelMode value);
    }

    // endregion 运行时视图接口
}
