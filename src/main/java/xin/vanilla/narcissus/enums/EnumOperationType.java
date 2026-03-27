package xin.vanilla.narcissus.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;
import xin.vanilla.narcissus.NarcissusComponent;

public enum EnumOperationType implements IEnumDescribable {
    ADD,
    SET,
    REMOVE,
    DEL,
    LIST,
    GET,
    CLEAR,
    ;

    public static EnumOperationType valueOfEx(Object obj) {
        if (obj instanceof EnumOperationType) return (EnumOperationType) obj;
        if (obj instanceof String) {
            for (EnumOperationType value : values()) {
                if (value.name().equalsIgnoreCase((String) obj)) {
                    return value;
                }
            }
        }
        return null;
    }

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(NarcissusComponent.get(), this);
    }
}
