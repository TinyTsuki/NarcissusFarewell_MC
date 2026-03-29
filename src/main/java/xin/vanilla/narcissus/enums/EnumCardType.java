package xin.vanilla.narcissus.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;
import xin.vanilla.narcissus.NarcissusComponent;

/**
 * 传送卡应用方式
 */
public enum EnumCardType implements IEnumDescribable {
    NONE,
    LIKE_COST,
    REFUND_COST,
    REFUND_ALL_COST,
    REFUND_COOLDOWN,
    REFUND_COST_AND_COOLDOWN,
    REFUND_ALL_COST_AND_COOLDOWN,
    ;

    public static EnumCardType valueOfEx(Object obj) {
        if (obj instanceof EnumCardType) return (EnumCardType) obj;
        if (obj instanceof String) {
            for (EnumCardType value : values()) {
                if (value.name().equalsIgnoreCase((String) obj)) {
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

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(NarcissusComponent.get(), this);
    }
}
