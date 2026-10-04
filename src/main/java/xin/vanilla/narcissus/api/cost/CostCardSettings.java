package xin.vanilla.narcissus.api.cost;

import lombok.Value;
import lombok.experimental.Accessors;
import xin.vanilla.narcissus.enums.EnumCardType;

import java.util.Objects;

@Value
@Accessors(fluent = true)
public class CostCardSettings {
    boolean enabled;
    int dailyGrant;
    EnumCardType mode;

    public CostCardSettings(boolean enabled, int dailyGrant, EnumCardType mode) {
        if (dailyGrant < 0) throw new IllegalArgumentException("Negative daily card grant");
        this.enabled = enabled; this.dailyGrant = dailyGrant;
        this.mode = Objects.requireNonNull(mode, "mode");
    }
}
