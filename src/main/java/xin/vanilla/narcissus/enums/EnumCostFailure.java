package xin.vanilla.narcissus.enums;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.banira.common.util.EnumDescriptionHelper;
import xin.vanilla.narcissus.NarcissusComponent;

public enum EnumCostFailure implements IEnumDescribable {
    NONE, INVALID_DISTANCE, INVALID_RESULT, AMOUNT_OVERFLOW, FORMULA_FAILED, CONFIGURATION_UNAVAILABLE,
    INSUFFICIENT_CARDS, INSUFFICIENT_RESOURCE, COOLDOWN, INVALID_OPERATION, ACTOR_UNAVAILABLE,
    REQUEST_UNAVAILABLE, UNKNOWN_TARGET, INVALID_PAYMENT, COMMAND_FAILED, PAYMENT_FAILED;

    @Override
    public Component enumDescription() {
        return EnumDescriptionHelper.describeEnum(NarcissusComponent.get(), this);
    }
}
