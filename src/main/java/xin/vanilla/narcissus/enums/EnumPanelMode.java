package xin.vanilla.narcissus.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;
import xin.vanilla.narcissus.NarcissusComponent;

/**
 * 传送路标界面布局模式
 */
public enum EnumPanelMode implements IEnumDescribable {
    /**
     * 列
     */
    COLUMNS,
    /**
     * Tab 单栏
     */
    TAB_SINGLE,
    ;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(NarcissusComponent.get(), this);
    }
}
