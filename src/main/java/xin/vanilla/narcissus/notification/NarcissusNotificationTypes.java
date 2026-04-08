package xin.vanilla.narcissus.notification;

import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumNotificationTypeDisplayMode;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.notification.ServerNotificationTypeRegistry;
import xin.vanilla.narcissus.NarcissusFarewell;

/**
 * Narcissus 通知类型 id，供玩家在 Banira 通知设置里按类型定制显示方式。
 * <p>
 * 客户端在 {@link xin.vanilla.narcissus.event.ClientModEventHandler} 初始化时
 * 对 {@link #ALL_TYPE_IDS} 调用 {@code NotificationTypeRegistry.register}。
 * <p>
 * 服务端在 {@link #registerAllOnServer()} 中登记默认 HUD 位置与动画，并对交互类类型附带
 * {@link EnumNotificationTypeDisplayMode#VANILLA_CHAT}，由 Banira 经登录包同步给客户端（JSON 无条目时生效）。
 */
public final class NarcissusNotificationTypes {

    private static final String P = NarcissusFarewell.MODID + ".";

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

    /**
     * /tpask、/tphere 等带 RUN_COMMAND 按钮的互传消息（多行、可点击）
     */
    public static final String INTERACTIVE_TP_FLOW = P + "interactive_tp_flow";
    /**
     * /share 坐标分享（传送/复制按钮等）
     */
    public static final String INTERACTIVE_SHARE = P + "interactive_share";
    /**
     * /help 多行可点击
     */
    public static final String INTERACTIVE_HELP = P + "interactive_help";
    /**
     * /gethome、/getstage 等列表与复制
     */
    public static final String INTERACTIVE_COORDINATE_LIST = P + "interactive_coordinate_list";
    /**
     * /dimension、/uuid、含悬停维度的反馈等
     */
    public static final String INTERACTIVE_QUERY = P + "interactive_query";

    public static final String[] ALL_TYPE_IDS = {
            TELEPORT_REQUEST,
            TELEPORT_GUARD,
            TELEPORT_SEARCH,
            TELEPORT_ERROR,
            WAYPOINT,
            INTERACTIVE_TP_FLOW,
            INTERACTIVE_SHARE,
            INTERACTIVE_HELP,
            INTERACTIVE_COORDINATE_LIST,
            INTERACTIVE_QUERY,
    };

    public static void registerAllOnServer() {
        ServerNotificationTypeRegistry.register(TELEPORT_SEARCH);
        ServerNotificationTypeRegistry.register(WAYPOINT);
        ServerNotificationTypeRegistry.register(TELEPORT_REQUEST, EnumPosition.TOP_CENTER, EnumMoveType.AUTO);
        ServerNotificationTypeRegistry.register(TELEPORT_GUARD, EnumPosition.TOP_CENTER, EnumMoveType.AUTO);
        ServerNotificationTypeRegistry.register(TELEPORT_ERROR, EnumPosition.TOP_CENTER, EnumMoveType.AUTO);
        EnumNotificationTypeDisplayMode vanillaChat = EnumNotificationTypeDisplayMode.VANILLA_CHAT;
        ServerNotificationTypeRegistry.register(INTERACTIVE_TP_FLOW, EnumPosition.TOP_CENTER, EnumMoveType.AUTO, vanillaChat);
        ServerNotificationTypeRegistry.register(INTERACTIVE_SHARE, EnumPosition.TOP_CENTER, EnumMoveType.AUTO, vanillaChat);
        ServerNotificationTypeRegistry.register(INTERACTIVE_HELP, EnumPosition.TOP_CENTER, EnumMoveType.AUTO, vanillaChat);
        ServerNotificationTypeRegistry.register(INTERACTIVE_COORDINATE_LIST, EnumPosition.TOP_CENTER, EnumMoveType.AUTO, vanillaChat);
        ServerNotificationTypeRegistry.register(INTERACTIVE_QUERY, EnumPosition.TOP_CENTER, EnumMoveType.AUTO, vanillaChat);
    }

    private NarcissusNotificationTypes() {
    }
}
