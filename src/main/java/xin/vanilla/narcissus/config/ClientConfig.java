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

/**
 * 客户端配置：注解结构用于 ForgeConfigSpec；运行时通过 {@link #get()} 分层读取。
 * GUI 说明与项目根目录 {@code narcissus_farewell-client.toml} 中的注释一致。
 */
@Getter
@Setter
@Accessors(chain = true, fluent = true)
@Config(name = "narcissus_farewell-client", type = ModConfig.Type.CLIENT)
public class ClientConfig implements ConfigData {

    // region 配置结构

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @ConfigEntry.Gui.CollapsibleObject
    @ConfigEntry.Gui.Tooltip(zh_cn = "客户端设置", en_us = "Client settings")
    private ClientRootCategory client = new ClientRootCategory();

    private final ConfigHolder holder;
    private final ClientRoot clientApi;

    // endregion 配置结构

    private ClientConfig() {
        this(null);
    }

    ClientConfig(ConfigHolder holder) {
        this.holder = holder;
        this.clientApi = new ClientRoot(holder);
    }

    public static ClientConfig get() {
        return new ClientConfig(ForgeConfigAdapter.getHolder(ClientConfig.class));
    }

    public ClientRoot client() {
        return clientApi;
    }

    public ConfigHolder holder() {
        return holder;
    }

    public void save() {
        if (holder != null) {
            holder.save();
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

    public static final class Key {
        private Key() {
        }

        public static final String SYNC_HOME_MAP_WAYPOINT = "client.syncHomeMapWaypoint";
        public static final String SYNC_STAGE_MAP_WAYPOINT = "client.syncStageMapWaypoint";
    }

    public static final class ClientRoot {
        private final ConfigHolder holder;

        ClientRoot(ConfigHolder holder) {
            this.holder = holder;
        }

        public boolean syncHomeMapWaypoint() {
            if (holder == null) {
                return true;
            }
            Boolean v = holder.get(Key.SYNC_HOME_MAP_WAYPOINT);
            return v != null ? v : true;
        }

        public ClientRoot syncHomeMapWaypoint(boolean value) {
            if (holder != null) {
                holder.set(Key.SYNC_HOME_MAP_WAYPOINT, value);
            }
            return this;
        }

        public boolean syncStageMapWaypoint() {
            if (holder == null) {
                return true;
            }
            Boolean v = holder.get(Key.SYNC_STAGE_MAP_WAYPOINT);
            return v != null ? v : true;
        }

        public ClientRoot syncStageMapWaypoint(boolean value) {
            if (holder != null) {
                holder.set(Key.SYNC_STAGE_MAP_WAYPOINT, value);
            }
            return this;
        }
    }
}
