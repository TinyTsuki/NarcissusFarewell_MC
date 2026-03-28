package xin.vanilla.narcissus.enums;

import lombok.Getter;

/**
 * 传送卡应用方式
 */
@Getter
public enum EnumCardType {
    NONE("无效果"),
    LIKE_COST("与代价数量一致"),
    REFUND_COST("一比一抵消代价"),
    REFUND_ALL_COST("抵消全部代价"),
    REFUND_COOLDOWN("抵消冷却时间"),
    REFUND_COST_AND_COOLDOWN("抵消冷却时间并抵消一比一代价"),
    REFUND_ALL_COST_AND_COOLDOWN("抵消冷却时间并抵消全部代价");

    private final String desc;

    EnumCardType(String desc) {
        this.desc = desc;
    }

    public static EnumCardType valueOfEx(Object obj) {
        if (obj instanceof EnumCardType) return (EnumCardType) obj;
        if (obj instanceof String) {
            for (EnumCardType value : values()) {
                if (value.name().equalsIgnoreCase((String) obj)
                        || value.desc.equalsIgnoreCase((String) obj)
                ) {
                    return value;
                }
            }
        }
        return null;
    }

    public static EnumCardType valueOfDefault(Object obj) {
        EnumCardType value = valueOfEx(obj);
        return value == null ? NONE : value;
    }
}
