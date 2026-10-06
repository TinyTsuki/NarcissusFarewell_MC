package xin.vanilla.narcissus.config;

import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.search.SafeBlockPolicy;
import xin.vanilla.narcissus.search.SearchExecutionSettings;

import java.util.*;
import java.util.function.BooleanSupplier;

public final class CommonSearchConfiguration {
    private static final String SAFE = "base.safeTeleport.";
    private static final String EXECUTION = SAFE + "search.";
    private static final String RANDOM = "base.randomTeleport.";
    private static final String VIEW = "base.teleportLimit.teleportViewDistanceLimit";
    private final ConfigHolder holder;
    private final Map<EnumTeleportType, Snapshot[]> selections = new EnumMap<>(EnumTeleportType.class);
    private SearchExecutionSettings execution;
    private BooleanSupplier executionMatches;
    private SafeBlockPolicy policy;
    private Map<String, Object> policyValues;
    private BooleanSupplier policyMatches;

    public CommonSearchConfiguration(ConfigHolder holder) {
        this.holder = Objects.requireNonNull(holder, "holder");
    }

    public SearchExecutionSettings execution() {
        synchronized (holder) {
            requireBound();
            if (execution != null && executionMatches.getAsBoolean()) return execution;
            validate(EXECUTION + "timeBudgetMs");
            validate(EXECUTION + "maxConcurrentSearches");
            validate(EXECUTION + "timeoutSeconds");
            CommonConfigView.BaseView.SafeTeleportView.SearchView view = CommonConfig.get().base().safeTeleport().search();
            SearchExecutionSettings next = new SearchExecutionSettings(view.timeBudgetMs(), view.maxConcurrentSearches(), view.timeoutSeconds());
            Map<String, Object> values = new LinkedHashMap<>();
            values.put(EXECUTION + "timeBudgetMs", next.timeBudgetMs());
            values.put(EXECUTION + "maxConcurrentSearches", next.maxConcurrentSearches());
            values.put(EXECUTION + "timeoutSeconds", next.timeoutSeconds());
            BooleanSupplier matches = prepare(values);
            execution = next;
            executionMatches = matches;
            return next;
        }
    }

    public Snapshot capture(EnumTeleportType type, boolean usesView) {
        Objects.requireNonNull(type, "type");
        synchronized (holder) {
            requireBound();
            Snapshot[] pair = selections.computeIfAbsent(type, key -> new Snapshot[2]);
            int index = usesView ? 1 : 0;
            Snapshot previous = pair[index];
            if (previous != null && previous.matches()) return previous;
            for (String name : Arrays.asList("safeBlocks", "unsafeBlocks", "suffocatingBlocks", "safeChunkRange",
                    "setBlockWhenSafeNotFound", "getBlockFromInventory"))
                validate(SAFE + name);
            if (type == EnumTeleportType.TP_RANDOM) {
                validate(RANDOM + "teleportRandomDistanceLimit");
                validate(RANDOM + "tpRandomSafeNotFoundRetries");
            }
            if (usesView) validate(VIEW);
            CommonConfigView.BaseView base = CommonConfig.get().base();
            CommonConfigView.BaseView.SafeTeleportView safe = base.safeTeleport();
            if (policy == null || !policyMatches.getAsBoolean()) {
                List<String> support = safe.safeBlocks();
                List<String> unsafe = safe.unsafeBlocks();
                List<String> suffocating = safe.suffocatingBlocks();
                Map<String, Object> values = new LinkedHashMap<>();
                values.put(SAFE + "safeBlocks", support);
                values.put(SAFE + "unsafeBlocks", unsafe);
                values.put(SAFE + "suffocatingBlocks", suffocating);
                BooleanSupplier matches = prepare(values);
                SafeBlockPolicy next = SafeBlockPolicy.from(support, unsafe, suffocating);
                policy = next;
                policyValues = values;
                policyMatches = matches;
            }
            int range = safe.safeChunkRange();
            boolean place = safe.setBlockWhenSafeNotFound();
            boolean inventory = safe.getBlockFromInventory();
            int randomDistance = type == EnumTeleportType.TP_RANDOM ? base.randomTeleport().teleportRandomDistanceLimit() : 0;
            int retries = type == EnumTeleportType.TP_RANDOM ? base.randomTeleport().tpRandomSafeNotFoundRetries() : 0;
            int viewLimit = usesView ? base.teleportLimit().teleportViewDistanceLimit() : 0;
            Map<String, Object> expected = new LinkedHashMap<>(policyValues);
            expected.put(SAFE + "safeChunkRange", range);
            expected.put(SAFE + "setBlockWhenSafeNotFound", place);
            expected.put(SAFE + "getBlockFromInventory", inventory);
            if (type == EnumTeleportType.TP_RANDOM) {
                expected.put(RANDOM + "teleportRandomDistanceLimit", randomDistance);
                expected.put(RANDOM + "tpRandomSafeNotFoundRetries", retries);
            }
            if (usesView) expected.put(VIEW, viewLimit);
            Snapshot next = new Snapshot(policy, range, place, inventory, randomDistance, retries, viewLimit, prepare(expected));
            pair[index] = next;
            return next;
        }
    }

    private void requireBound() {
        if (BaniraConfigs.handle(CommonConfig.class) != holder) {
            throw new IllegalStateException("Search configuration holder changed; restart the search runtime");
        }
    }

    private void validate(String path) {
        Object value = holder.get(path);
        if (value == null || !holder.validate(path, value))
            throw new IllegalArgumentException("Invalid search configuration: " + path);
    }

    private BooleanSupplier prepare(Map<String, Object> expected) {
        BooleanSupplier stored = holder.prepareStoredMatch(expected, true);
        if (!stored.getAsBoolean())
            throw new IllegalArgumentException("Search configuration does not match its runtime values");
        return () -> {
            synchronized (holder) {
                return BaniraConfigs.handle(CommonConfig.class) == holder && stored.getAsBoolean();
            }
        };
    }

    public static final class Snapshot {
        private final SafeBlockPolicy policy;
        private final int safeChunkRange;
        private final boolean setBlockWhenSafeNotFound;
        private final boolean getBlockFromInventory;
        private final int randomDistanceLimit;
        private final int randomRetries;
        private final int viewDistanceLimit;
        private final BooleanSupplier matches;

        private Snapshot(SafeBlockPolicy policy, int safeChunkRange, boolean setBlockWhenSafeNotFound,
                         boolean getBlockFromInventory, int randomDistanceLimit, int randomRetries,
                         int viewDistanceLimit, BooleanSupplier matches) {
            this.policy = policy;
            this.safeChunkRange = safeChunkRange;
            this.setBlockWhenSafeNotFound = setBlockWhenSafeNotFound;
            this.getBlockFromInventory = getBlockFromInventory;
            this.randomDistanceLimit = randomDistanceLimit;
            this.randomRetries = randomRetries;
            this.viewDistanceLimit = viewDistanceLimit;
            this.matches = matches;
        }

        public SafeBlockPolicy policy() {
            return policy;
        }

        public int safeChunkRange() {
            return safeChunkRange;
        }

        public int randomDistanceLimit() {
            return randomDistanceLimit;
        }

        public int randomRetries() {
            return randomRetries;
        }

        public int viewDistanceLimit() {
            return viewDistanceLimit;
        }

        public boolean setBlockWhenSafeNotFound() {
            return setBlockWhenSafeNotFound;
        }

        public boolean getBlockFromInventory() {
            return getBlockFromInventory;
        }

        public boolean matches() {
            return matches.getAsBoolean();
        }
    }
}
