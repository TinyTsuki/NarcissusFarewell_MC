package xin.vanilla.narcissus.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;
import xin.vanilla.narcissus.NarcissusComponent;

/**
 * 代价类型
 */
public enum EnumCostType implements IEnumDescribable {
    NONE,
    EXP_POINT,
    EXP_LEVEL,
    HEALTH,
    HUNGER,
    ITEM,
    COMMAND,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(NarcissusComponent.get(), this);
    }
}
