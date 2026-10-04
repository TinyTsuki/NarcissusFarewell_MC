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
    private static final CostCardSettings NO_CARDS = new CostCardSettings(false, 0, EnumCardType.WAIVE_COST);
    private final Map<EnumTeleportType, CostParameters> groups;
    private final CostCardSettings cards;
    private final int maxDistance;
    private final int crossDimensionDistance;

    public CostConfiguration(Map<EnumTeleportType, CostParameters> groups, CostCardSettings cards,
                             int maxDistance, int crossDimensionDistance) {
        this(groups, cards, maxDistance, crossDimensionDistance, true);
    }

    private CostConfiguration(Map<EnumTeleportType, CostParameters> groups, CostCardSettings cards,
                              int maxDistance, int crossDimensionDistance, boolean complete) {
        if (maxDistance < 0 || crossDimensionDistance < 0) throw new IllegalArgumentException("Invalid distance limits");
        EnumMap<EnumTeleportType, CostParameters> copy = new EnumMap<>(EnumTeleportType.class);
        for (EnumTeleportType type : complete ? EnumTeleportType.countdownConfigurableTypes() : groups.keySet()) {
            copy.put(type, Objects.requireNonNull(groups.get(type), "Missing cost group " + type));
        }
        this.groups = Collections.unmodifiableMap(copy);
        this.cards = Objects.requireNonNull(cards, "cards");
        this.maxDistance = maxDistance; this.crossDimensionDistance = crossDimensionDistance;
    }

    public static CostConfiguration selection(EnumTeleportType type, CostParameters parameters,
                                               CostCardSettings cards, int maxDistance, int crossDimensionDistance) {
        return new CostConfiguration(Collections.singletonMap(type, parameters), cards, maxDistance, crossDimensionDistance, false);
    }

    public static CostConfiguration freeSelection(EnumTeleportType type, CostParameters parameters) {
        return selection(type, parameters, NO_CARDS, 0, 0);
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
        return new CostConfiguration(groups, new CostCardSettings(false, 0, EnumCardType.WAIVE_COST), 10000, 10000);
    }
}
