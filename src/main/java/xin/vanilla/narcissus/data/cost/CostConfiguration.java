package xin.vanilla.narcissus.data.cost;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.narcissus.api.cost.CostCardSettings;
import xin.vanilla.narcissus.api.cost.CostParameters;
import xin.vanilla.narcissus.enums.EnumCardType;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.*;

@Getter
@Accessors(fluent = true)
public final class CostConfiguration {
    private final Map<EnumTeleportType, CostParameters> groups;
    private final CostCardSettings cards;
    private final int maxDistance;
    private final int crossDimensionDistance;

    public CostConfiguration(Map<EnumTeleportType, CostParameters> groups, CostCardSettings cards,
                             int maxDistance, int crossDimensionDistance) {
        if (maxDistance < 0 || crossDimensionDistance < 0) throw new IllegalArgumentException("Invalid distance limits");
        EnumMap<EnumTeleportType, CostParameters> copy = new EnumMap<>(EnumTeleportType.class);
        for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) {
            copy.put(type, Objects.requireNonNull(groups.get(type), "Missing cost group " + type));
        }
        this.groups = Collections.unmodifiableMap(copy);
        this.cards = Objects.requireNonNull(cards, "cards");
        this.maxDistance = maxDistance; this.crossDimensionDistance = crossDimensionDistance;
    }

    public CostParameters parameters(EnumTeleportType type) {
        Objects.requireNonNull(type, "type");
        return groups.getOrDefault(type, CostParameters.free());
    }

    public double distance(double raw, boolean crossDimension) {
        if (!Double.isFinite(raw) || raw < 0) throw new IllegalArgumentException("Invalid distance");
        return crossDimension ? crossDimensionDistance : maxDistance == 0 ? raw : Math.min(raw, maxDistance);
    }

    public static CostConfiguration defaults() {
        EnumMap<EnumTeleportType, CostParameters> groups = new EnumMap<>(EnumTeleportType.class);
        for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) groups.put(type, CostParameters.free());
        return new CostConfiguration(groups, new CostCardSettings(false, 0, EnumCardType.REFUND_ALL_COST), 10000, 10000);
    }
}
