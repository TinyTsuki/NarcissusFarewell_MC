package xin.vanilla.narcissus.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;
import xin.vanilla.narcissus.config.access.ClientConfigAccess;
import xin.vanilla.narcissus.enums.EnumPanelMode;

/**
 * 客户端配置：注解结构用于 ForgeConfigSpec；运行时通过 {@link #get()} 返回的 {@link RootView} 分层读取。
 * GUI 说明与项目根目录 {@code narcissus_farewell-client.toml} 中的注释一致。
 */
@Config(name = "narcissus_farewell-client", type = ConfigScope.CLIENT)
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
        return ClientConfigAccess.root(BaniraConfigs.holder(ClientConfig.class));
    }

    public static void save() {
        ConfigHolder h = BaniraConfigs.holder(ClientConfig.class);
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
        private EnumPanelMode waypointScreenPanelMode = EnumPanelMode.COLUMNS;

        @ConfigEntry.Gui.Tooltip(zh_cn = "黑白名单界面布局：双列或 Tab 单栏；在界面内长按标题切换时会写入此选项。", en_us = "Access list screen layout: two columns or tabbed panel; long-press the title in the GUI to toggle and persist.")
        private EnumPanelMode accessListScreenPanelMode = EnumPanelMode.COLUMNS;
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

        EnumPanelMode waypointScreenPanelMode();

        ClientView waypointScreenPanelMode(EnumPanelMode value);

        EnumPanelMode accessListScreenPanelMode();

        ClientView accessListScreenPanelMode(EnumPanelMode value);
    }

    // endregion 运行时视图接口
}
