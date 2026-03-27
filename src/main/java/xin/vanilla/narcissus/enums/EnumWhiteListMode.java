package xin.vanilla.narcissus.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;
import xin.vanilla.narcissus.NarcissusComponent;

/**
 * 白名单子命令模式，与指令参数字符串一致（{@link #name()}）
 */
public enum EnumWhiteListMode implements IEnumDescribable {
    NONE,
    BOTH,
    AUTO_ACCEPT_TPA,
    AUTO_ACCEPT_TPH,
    ;

    public static EnumWhiteListMode valueOfEx(Object obj) {
        if (obj instanceof EnumWhiteListMode) {
            return (EnumWhiteListMode) obj;
        }
        if (obj instanceof String) {
            for (EnumWhiteListMode value : values()) {
                String str = (String) obj;
                if (value.name().equalsIgnoreCase(str)) {
                    return value;
                }
            }
        } else if (obj instanceof Number) {
            int i = ((Number) obj).intValue();
            if (i >= 0 && i < values().length) {
                return values()[i];
            }
        }
        return null;
    }

    public static EnumWhiteListMode valueOfDefault(Object obj) {
        EnumWhiteListMode v = valueOfEx(obj);
        return v == null ? NONE : v;
    }

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(NarcissusComponent.get(), this);
    }
}
