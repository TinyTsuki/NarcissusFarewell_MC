package xin.vanilla.narcissus.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 客户端配置
 */
public class ClientConfig {
    public static final ForgeConfigSpec CLIENT_CONFIG;

    /**
     * 同步创建home地图传送点
     */
    public static final ForgeConfigSpec.BooleanValue SYNC_HOME_MAP_WAYPOINT;

    /**
     * 同步创建stage地图传送点
     */
    public static final ForgeConfigSpec.BooleanValue SYNC_STAGE_MAP_WAYPOINT;

    static {
        ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();

        // 定义客户端配置项
        CLIENT_BUILDER.comment("Client Settings", "客户端设置").push("client");

        // 同步创建home地图传送点
        SYNC_HOME_MAP_WAYPOINT = CLIENT_BUILDER
                .comment("Create map waypoints when setting up personal waypoint."
                        , "创建私人传送点时同步创建地图传送点。")
                .define("syncHomeMapWaypoint", true);

        // 同步创建stage地图传送点
        SYNC_STAGE_MAP_WAYPOINT = CLIENT_BUILDER
                .comment("Create map waypoints when setting up public waypoint."
                        , "创建驿站传送点时同步创建地图传送点。")
                .define("syncStageMapWaypoint", true);

        CLIENT_BUILDER.pop();

        CLIENT_CONFIG = CLIENT_BUILDER.build();
    }

    public static void save() {
        CLIENT_CONFIG.save();
    }
}
