package xin.vanilla.narcissus.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;
import xin.vanilla.narcissus.NarcissusComponent;

public enum EnumSafeMode implements IEnumDescribable {
    NONE,
    /**
     * 当前位置到顶部
     */
    Y_C_TO_T,
    /**
     * 底部到当前位置
     */
    Y_B_TO_C,
    /**
     * 当前位置到底部
     */
    Y_C_TO_B,
    /**
     * 顶部到当前位置
     */
    Y_T_TO_C,
    /**
     * 当前位置+-3
     */
    Y_C_OFFSET_3,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(NarcissusComponent.get(), this);
    }
}
