package xin.vanilla.narcissus.api.cost;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;
import xin.vanilla.narcissus.NarcissusComponent;

public enum CostPhase implements IEnumDescribable {
    PREVIEW, CHECK, COMMIT;

    @Override public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(NarcissusComponent.get(), this);
    }
}
