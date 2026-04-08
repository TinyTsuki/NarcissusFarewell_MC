package xin.vanilla.narcissus.notification;

import xin.vanilla.banira.common.notification.ServerNotificationTypeRegistry;

/**
 * Narcissus 通知类型 id，供玩家在 Banira 通知设置里按类型定制显示方式。
 * <p>
 * 客户端在 {@link xin.vanilla.narcissus.event.ClientModEventHandler} 初始化时
 * 对 {@link #ALL_TYPE_IDS} 调用 {@code NotificationTypeRegistry.register}。
 */
public final class NarcissusNotificationTypes {

    private static final String P = "narcissus.";

    /**
     * /tpask、/tphere 等玩家互传请求（收到、拒绝、取消、过期、请求不存在等）
     */
    public static final String TELEPORT_REQUEST = P + "teleport_request";
    /**
     * 传送前限制：冷却、锁怪、倒计时因移动/受伤取消
     */
    public static final String TELEPORT_GUARD = P + "teleport_guard";
    /**
     * 安全点、结构、视角等搜索进行中
     */
    public static final String TELEPORT_SEARCH = P + "teleport_search";
    /**
     * 传送/导航失败：目标不存在、距离/跨维限制、无效参数等
     */
    public static final String TELEPORT_ERROR = P + "teleport_error";
    /**
     * 家、驿站、分享锚点（设置/冲突/删除提示等）
     */
    public static final String WAYPOINT = P + "waypoint";

    public static final String[] ALL_TYPE_IDS = {
            TELEPORT_REQUEST,
            TELEPORT_GUARD,
            TELEPORT_SEARCH,
            TELEPORT_ERROR,
            WAYPOINT,
    };

    public static void registerAllOnServer() {
        for (String id : ALL_TYPE_IDS) {
            ServerNotificationTypeRegistry.register(id);
        }
    }

    private NarcissusNotificationTypes() {
    }
}
