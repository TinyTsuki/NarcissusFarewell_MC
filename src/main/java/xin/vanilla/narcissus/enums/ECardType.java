package xin.vanilla.narcissus.enums;

import lombok.Getter;

/**
 * 传送卡应用方式
 */
@Getter
public enum ECardType {
    NONE("无效果"),
    LIKE_COST("与代价数量一致"),
    REFUND_COST("一比一抵消代价"),
    REFUND_ALL_COST("抵消全部代价"),
    REFUND_COOLDOWN("抵消冷却时间"),
    REFUND_COST_AND_COOLDOWN("抵消冷却时间并抵消一比一代价"),
    REFUND_ALL_COST_AND_COOLDOWN("抵消冷却时间并抵消全部代价");

    private final String desc;

    ECardType(String desc) {
        this.desc = desc;
    }
}
