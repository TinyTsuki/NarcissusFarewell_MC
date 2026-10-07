package xin.vanilla.narcissus.config.migration;

import xin.vanilla.narcissus.enums.EnumCoolDownType;

import java.math.BigDecimal;
import java.util.*;

final class LegacyConfigLayoutMigration {
    private static final Map<String, String> PATHS = new LinkedHashMap<>();
    private static final Map<String, Kind> TYPES = new LinkedHashMap<>();

    static {
        aliases("commandNames", "command", Kind.TEXT,
                "commandPrefix", "commandUuid", "commandDimension", "commandCard", "commandShare", "commandFeed",
                "commandTpCoordinate", "commandTpStructure", "commandTpRandom", "commandTpSpawn", "commandTpWorldSpawn",
                "commandTpTop", "commandTpBottom", "commandTpUp", "commandTpDown", "commandTpView", "commandTpBack", "commandTpGrave", "commandFly");
        aliases("conciseCommands", "concise", Kind.BOOLEAN,
                "conciseLanguage", "conciseUuid", "conciseDimension", "conciseCard", "conciseShare", "conciseFeed",
                "conciseTpCoordinate", "conciseTpStructure", "conciseTpRandom", "conciseTpSpawn", "conciseTpWorldSpawn",
                "conciseTpTop", "conciseTpBottom", "conciseTpUp", "conciseTpDown", "conciseTpView", "conciseTpBack", "conciseTpGrave", "conciseFly", "conciseVirtualOp");
        for (String prefix : Arrays.asList("command", "concise")) {
            String oldRoot = prefix.equals("command") ? "commandNames" : "conciseCommands";
            Kind kind = prefix.equals("command") ? Kind.TEXT : Kind.BOOLEAN;
            for (String group : Arrays.asList("Ask", "Here"))
                aliases(oldRoot + ".tp" + group, prefix + ".tp" + group, kind,
                        prefix + "Tp" + group, prefix + "Tp" + group + "Yes", prefix + "Tp" + group + "No", prefix + "Tp" + group + "Cancel");
            for (String group : Arrays.asList("Home", "Stage"))
                aliases(oldRoot + ".tp" + group, prefix + ".tp" + group, kind,
                        prefix + "Tp" + group, prefix + "Set" + group, prefix + "Del" + group, prefix + "Get" + group);
        }
        aliases("base", "base.other", Kind.BOOLEAN, "removeOriginalTp");
        aliases("base", "base.creativeFlight", Kind.NUMBER, "flySpeedMin", "flySpeedMax");
        aliases("general", "base.other", Kind.TEXT, "tpSound");
        aliases("general", "base.teleportLimit", Kind.INTEGER,
                "teleportRecordLimit", "teleportViewDistanceLimit", "graveSearchRangeLimit", "teleportHomeLimit");
        aliases("general", "base.teleportLimit", Kind.STRINGS, "teleportBackSkipType");
        aliases("general", "base.teleportLimit", Kind.BOOLEAN, "teleportAcrossDimension");
        aliases("general", "base.teleportLimit", Kind.TEXT, "tpSpawnNoBedWorldDimension");
        aliases("general", "base.teleportTogether", Kind.BOOLEAN, "tpWithVehicle", "tpWithFollower", "tpWithEnemy");
        aliases("general", "base.teleportTogether", Kind.INTEGER, "tpWithFollowerRange");
        aliases("general", "base.teleportRequest", Kind.INTEGER, "teleportRequestExpireTime", "teleportRequestCooldown");
        aliases("general", "base.teleportRequest", Kind.COOLDOWN, "teleportRequestCooldownType");
        aliases("general", "base.randomTeleport", Kind.INTEGER, "teleportRandomDistanceLimit", "tpRandomSafeNotFoundRetries");
        aliases("general.safeTeleport", "base.safeTeleport", Kind.STRINGS, "unsafeBlocks", "suffocatingBlocks", "safeBlocks");
        aliases("general.safeTeleport", "base.safeTeleport", Kind.BOOLEAN, "setBlockWhenSafeNotFound", "getBlockFromInventory");
        aliases("general.safeTeleport", "base.safeTeleport", Kind.INTEGER, "safeChunkRange");
    }

    private LegacyConfigLayoutMigration() {
    }

    static Map<String, String> paths() {
        return Collections.unmodifiableMap(PATHS);
    }

    static boolean migrate(Map<String, Object> root) {
        boolean changed = false;
        for (Map.Entry<String, String> alias : PATHS.entrySet()) {
            String source = alias.getKey(), target = alias.getValue();
            Map<String, Object> from = parent(root, source, false);
            String sourceKey = leaf(source);
            if (from == null || !from.containsKey(sourceKey)) continue;
            Object value = from.get(sourceKey);
            TYPES.get(source).validate(value, source);
            Map<String, Object> to = parent(root, target, true);
            String targetKey = leaf(target);
            if (to.containsKey(targetKey)) {
                Object existing = to.get(targetKey);
                TYPES.get(source).validate(existing, target);
                if (!equal(value, existing))
                    throw new IllegalArgumentException("Conflicting old/new setting: " + source + " and " + target);
            } else {
                to.put(targetKey, value);
            }
            from.remove(sourceKey);
            prune(root, source.split("\\."), 0);
            changed = true;
        }
        return changed;
    }

    private static void aliases(String source, String target, Kind kind, String... keys) {
        for (String key : keys) {
            PATHS.put(source + "." + key, target + "." + key);
            TYPES.put(source + "." + key, kind);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parent(Map<String, Object> root, String path, boolean create) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int index = 0; index < parts.length - 1; index++) {
            String key = parts[index];
            if (!current.containsKey(key)) {
                if (!create) return null;
                current.put(key, new LinkedHashMap<String, Object>());
            }
            Object child = current.get(key);
            if (!(child instanceof Map)) throw new IllegalArgumentException("Expected table: " + path);
            current = (Map<String, Object>) child;
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private static void prune(Map<String, Object> current, String[] parts, int index) {
        if (index >= parts.length - 1) return;
        Object child = current.get(parts[index]);
        if (!(child instanceof Map)) return;
        Map<String, Object> table = (Map<String, Object>) child;
        prune(table, parts, index + 1);
        if (table.isEmpty()) current.remove(parts[index]);
    }

    private static String leaf(String path) {
        return path.substring(path.lastIndexOf('.') + 1);
    }

    private static boolean equal(Object first, Object second) {
        if (first instanceof Number && second instanceof Number)
            return new BigDecimal(first.toString()).compareTo(new BigDecimal(second.toString())) == 0;
        return Objects.equals(first, second);
    }

    private enum Kind {
        TEXT, BOOLEAN, NUMBER, INTEGER, STRINGS, COOLDOWN;

        void validate(Object value, String path) {
            boolean valid;
            switch (this) {
                case TEXT:
                    valid = value instanceof String;
                    break;
                case BOOLEAN:
                    valid = value instanceof Boolean;
                    break;
                case NUMBER:
                case INTEGER:
                    valid = value instanceof Number && Double.isFinite(((Number) value).doubleValue());
                    if (valid && this == INTEGER) {
                        double number = ((Number) value).doubleValue();
                        valid = number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE && number == Math.rint(number);
                    }
                    break;
                case STRINGS:
                    valid = value instanceof List && ((List<?>) value).stream().allMatch(item -> item instanceof String);
                    break;
                case COOLDOWN:
                    valid = value instanceof String;
                    if (valid) {
                        try {
                            EnumCoolDownType.valueOf((String) value);
                        } catch (IllegalArgumentException error) {
                            valid = false;
                        }
                    }
                    break;
                default:
                    throw new AssertionError(this);
            }
            if (!valid) throw new IllegalArgumentException("Invalid legacy setting (" + name() + "): " + path);
        }
    }
}
