package xin.vanilla.narcissus.config;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.common.config.ConfigData;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.annotation.Config;
import xin.vanilla.banira.common.config.annotation.ConfigEntry;

/**
 * 客户端配置：注解结构用于 ForgeConfigSpec；运行时通过 {@link #get()} 返回的 {@link ClientConfigView} 分层读取。
 * GUI 说明与项目根目录 {@code narcissus_farewell-client.toml} 中的注释一致。
 */
@Config(name = "narcissus_farewell-client", type = ConfigScope.CLIENT,
        generateView = true, viewUnbound = Config.UnboundAccess.DEFAULTS)
public class ClientConfig implements ConfigData {

    public ClientConfig() {
    }

    // region 配置结构

    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "客户端设置", en_us = "Client settings")
    private ClientRootCategory client = new ClientRootCategory();

    // endregion 配置结构


    public static ClientConfigView get() {
        return ClientConfigView.get();
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

    }
}
