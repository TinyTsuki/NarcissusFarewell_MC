package xin.vanilla.narcissus.config.migration;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.narcissus.data.cost.CostConfiguration;

import java.util.*;

@Getter
@Accessors(fluent = true)
public final class CostMigrationPlan {
    private final boolean migrationRequired;
    private final CostConfiguration configuration;
    private final Map<String, Object> configurationValues;
    private final Map<String, String> sources;
    private final Map<String, String> disabledExpressions;

    CostMigrationPlan(boolean migrationRequired, CostConfiguration configuration, Map<String, Object> configurationValues,
                      Map<String, String> sources, Map<String, String> disabledExpressions) {
        this.migrationRequired = migrationRequired; this.configuration = configuration;
        this.configurationValues = freeze(configurationValues);
        this.sources = Collections.unmodifiableMap(new LinkedHashMap<>(sources));
        this.disabledExpressions = Collections.unmodifiableMap(new LinkedHashMap<>(disabledExpressions));
    }

    @SuppressWarnings("unchecked") private static <T> T freeze(T value) {
        if (value instanceof Map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            ((Map<String, Object>) value).forEach((key, item) -> copy.put(key, freeze(item)));
            return (T) Collections.unmodifiableMap(copy);
        }
        if (value instanceof List) {
            List<Object> copy = new ArrayList<>(); for (Object item : (List<?>) value) copy.add(freeze(item));
            return (T) Collections.unmodifiableList(copy);
        }
        return value;
    }
}
