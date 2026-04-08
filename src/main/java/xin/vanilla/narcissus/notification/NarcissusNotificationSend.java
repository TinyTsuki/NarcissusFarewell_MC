package xin.vanilla.narcissus.notification;

import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.EnumMoveType;
import xin.vanilla.banira.common.enums.EnumPosition;
import xin.vanilla.banira.common.notification.NotificationTypeKeys;
import xin.vanilla.banira.common.util.MessageUtils;

/**
 * 水仙辞服务端通知发送：按 {@link NarcissusNotificationTypes} 与重要程度选择 HUD 位置。
 * <ul>
 *     <li>顶部居中：传送失败、限制类、玩家互传请求相关（需立即注意）</li>
 *     <li>右上：搜索进度、家/驿站等偏信息类提示</li>
 *     <li>默认类型 {@code default}：由 {@link #sendDefault} 的 {@code topCenter} 指定</li>
 * </ul>
 */
public final class NarcissusNotificationSend {

    private static final long DURATION_MS = 5000L;
    private static final EnumMoveType MOVE = EnumMoveType.AUTO;

    private NarcissusNotificationSend() {
    }

    /**
     * 发送已注册的 {@code notificationType}（见 {@link NarcissusNotificationTypes#ALL_TYPE_IDS}）。
     */
    public static void send(ServerPlayerEntity player, Component message, String notificationType) {
        EnumPosition pos = positionForRegisteredType(notificationType);
        MessageUtils.sendNotification(player, message, pos, MOVE, DURATION_MS, notificationType);
    }

    /**
     * Banira 默认通知类型：用于未单独分类的提示；{@code topCenter} 为 true 时使用顶部居中（错误、阻断类）。
     */
    public static void sendDefault(ServerPlayerEntity player, Component message, boolean topCenter) {
        EnumPosition pos = topCenter ? EnumPosition.TOP_CENTER : EnumPosition.TOP_RIGHT;
        MessageUtils.sendNotification(player, message, pos, MOVE, DURATION_MS, NotificationTypeKeys.DEFAULT);
    }

    static EnumPosition positionForRegisteredType(String notificationType) {
        String t = NotificationTypeKeys.normalizeOrDefault(notificationType);
        if (NarcissusNotificationTypes.TELEPORT_SEARCH.equals(t) || NarcissusNotificationTypes.WAYPOINT.equals(t)) {
            return EnumPosition.TOP_RIGHT;
        }
        return EnumPosition.TOP_CENTER;
    }
}
