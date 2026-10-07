package xin.vanilla.narcissus.config.migration;

import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.narcissus.api.cost.CostCardSettings;
import xin.vanilla.narcissus.api.cost.CostParameters;
import xin.vanilla.narcissus.data.cost.CostConfiguration;
import xin.vanilla.narcissus.enums.EnumCardType;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.*;

public final class CostConfigMigration {
    private static final Map<String, String> CARD_MODES = new LinkedHashMap<>();

    static {
        CARD_MODES.put("NONE", "REQUIRE_ONE_WITH_COST");
        CARD_MODES.put("LIKE_COST", "REQUIRE_MATCHING_WITH_COST");
        CARD_MODES.put("REFUND_COST", "OFFSET_COST");
        CARD_MODES.put("REFUND_ALL_COST", "WAIVE_COST");
        CARD_MODES.put("REFUND_COOLDOWN", "BYPASS_COOLDOWN");
        CARD_MODES.put("REFUND_COST_AND_COOLDOWN", "OFFSET_COST_AND_BYPASS_COOLDOWN");
        CARD_MODES.put("REFUND_ALL_COST_AND_COOLDOWN", "WAIVE_COST_AND_BYPASS_COOLDOWN");
    }

    private CostConfigMigration() {
    }

    public static CostMigrationPlan plan(Map<String, Object> legacy) {
        Map<String, Object> root = copy(Objects.requireNonNull(legacy, "legacy"));
        boolean layoutRequired = LegacyConfigLayoutMigration.migrate(root);
        Map<String, Object> cost = table(root, "cost");
        Map<String, Object> base = table(root, "base");
        Map<String, Object> general = table(root, "general");
        Map<String, Object> limits = table(base, "teleportLimit");
        Map<String, Object> oldCards = legacyCards(base);
        boolean required = EnumTeleportType.countdownConfigurableTypes().stream().anyMatch(type -> {
            String name = groupName(type);
            return cost.containsKey("tp" + Character.toUpperCase(name.charAt(0)) + name.substring(1));
        })
                || oldCards.containsKey("teleportCard") || oldCards.containsKey("teleportCardDaily")
                || oldCards.containsKey("teleportCardType") || limits.containsKey("teleportCostDistanceLimit")
                || limits.containsKey("teleportCostDistanceAcrossDimension")
                || general.containsKey("teleportCostDistanceLimit") || general.containsKey("teleportCostDistanceAcrossDimension");
        if (!required) return new CostMigrationPlan(layoutRequired, currentConfiguration(cost), root,
                Collections.emptyMap(), Collections.emptyMap());
        Map<String, String> sources = new LinkedHashMap<>(), disabled = new LinkedHashMap<>();
        EnumMap<EnumTeleportType, CostParameters> groups = new EnumMap<>(EnumTeleportType.class);
        for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) {
            String name = groupName(type), suffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            String oldName = "tp" + suffix, prefix = "costTp" + suffix;
            Map<String, Object> old = table(cost, oldName);
            if (cost.containsKey(name)) throw new IllegalArgumentException("Conflicting old/new cost group: " + name);
            try {
                EnumCostType kind = EnumCostType.valueOf(text(old, prefix + "Type", "NONE"));
                double num = number(old, prefix + "Num", 1), rate = number(old, prefix + "Rate", .002);
                int min = integer(old, prefix + "NumLower", 0), max = integer(old, prefix + "NumUpper", 20);
                String expression = text(old, prefix + "Exp", "num * distance * rate");
                String conf = text(old, prefix + "Conf", ""), file = "";
                double fixed = 0, perBlock = num * rate;
                try {
                    if (!LegacyCostExpression.standard(expression)) {
                        OptionalDouble constant = LegacyCostExpression.constant(expression, num, rate);
                        if (constant.isPresent()) {
                            fixed = constant.getAsDouble();
                            perBlock = 0;
                        } else {
                            file = "Legacy" + suffix + "Cost.java";
                            sources.put(file, source(file, expression, LegacyCostExpression.toJava(expression, num, rate), false));
                        }
                    }
                } catch (IllegalArgumentException error) {
                    if (kind != EnumCostType.NONE) throw error;
                    file = "Legacy" + suffix + "Cost.java";
                    sources.put(file, source(file, expression, "", true));
                    disabled.put(oldName, expression);
                }
                CostParameters params = new CostParameters(kind, fixed, perBlock, min, max,
                        kind == EnumCostType.ITEM ? conf : "", kind == EnumCostType.COMMAND ? conf.replace("[num]", "{amount}") : "", file);
                groups.put(type, params);
                Map<String, Object> target = copy(old);
                for (String ending : Arrays.asList("Type", "Num", "Rate", "NumUpper", "NumLower", "Exp", "Conf"))
                    target.remove(prefix + ending);
                target.put("type", kind.name());
                target.put("fixedAmount", fixed);
                target.put("perBlockAmount", perBlock);
                target.put("minAmount", min);
                target.put("maxAmount", max);
                target.put("item", params.item());
                target.put("command", params.command());
                target.put("custom", Collections.singletonMap("file", file));
                cost.remove(oldName);
                cost.put(name, target);
            } catch (RuntimeException error) {
                throw new IllegalArgumentException("cost." + oldName + ": " + error.getMessage(), error);
            }
        }
        int maxDistance = legacyDistance(limits, general, "teleportCostDistanceLimit", 10000);
        int crossDistance = legacyDistance(limits, general, "teleportCostDistanceAcrossDimension", 10000);
        if (cost.containsKey("distance") || cost.containsKey("cards"))
            throw new IllegalArgumentException("Conflicting old/new distance or card settings");
        Map<String, Object> distance = new LinkedHashMap<>();
        distance.put("maxDistance", maxDistance);
        distance.put("crossDimensionDistance", crossDistance);
        cost.put("distance", distance);
        Object enabled = oldCards.getOrDefault("teleportCard", false);
        if (!(enabled instanceof Boolean)) throw new IllegalArgumentException("Invalid teleportCard flag");
        int grant = integer(oldCards, "teleportCardDaily", 0);
        String oldMode = text(oldCards, "teleportCardType", "REFUND_ALL_COST");
        String mappedMode = CARD_MODES.get(oldMode);
        if (mappedMode == null) throw new IllegalArgumentException("Invalid legacy card mode: " + oldMode);
        EnumCardType mode = EnumCardType.valueOf(mappedMode);
        Map<String, Object> cards = new LinkedHashMap<>();
        cards.put("enabled", enabled);
        cards.put("dailyGrant", grant);
        cards.put("mode", CARD_MODES.get(oldMode));
        cost.put("cards", cards);
        limits.remove("teleportCostDistanceLimit");
        limits.remove("teleportCostDistanceAcrossDimension");
        general.remove("teleportCostDistanceLimit");
        general.remove("teleportCostDistanceAcrossDimension");
        if (root.containsKey("general")) root.put("general", general);
        oldCards.remove("teleportCard");
        oldCards.remove("teleportCardDaily");
        oldCards.remove("teleportCardType");
        base.remove("teleportCardDaily");
        base.remove("teleportCardType");
        if (!limits.isEmpty()) base.put("teleportLimit", limits);
        else base.remove("teleportLimit");
        if (oldCards.isEmpty()) base.remove("teleportCard");
        else base.put("teleportCard", oldCards);
        root.put("base", base);
        root.put("cost", cost);
        return new CostMigrationPlan(true, new CostConfiguration(groups, new CostCardSettings((boolean) enabled, grant, mode),
                maxDistance, crossDistance), root, sources, disabled);
    }

    public static String groupName(EnumTeleportType type) {
        if (!EnumTeleportType.countdownConfigurableTypes().contains(type))
            throw new IllegalArgumentException("Not a cost type: " + type);
        StringJoiner result = new StringJoiner("");
        String[] parts = type.name().substring(3).toLowerCase(Locale.ROOT).split("_");
        for (int i = 0; i < parts.length; i++)
            result.add(i == 0 ? parts[i] : Character.toUpperCase(parts[i].charAt(0)) + parts[i].substring(1));
        return result.toString();
    }

    private static CostConfiguration currentConfiguration(Map<String, Object> cost) {
        EnumMap<EnumTeleportType, CostParameters> groups = new EnumMap<>(EnumTeleportType.class);
        for (EnumTeleportType type : EnumTeleportType.countdownConfigurableTypes()) {
            Map<String, Object> group = table(cost, groupName(type));
            groups.put(type, new CostParameters(EnumCostType.valueOf(text(group, "type", "NONE")),
                    number(group, "fixedAmount", 0), number(group, "perBlockAmount", .002),
                    integer(group, "minAmount", 0), integer(group, "maxAmount", 20),
                    text(group, "item", ""), text(group, "command", ""), text(table(group, "custom"), "file", "")));
        }
        Map<String, Object> cards = table(cost, "cards"), distance = table(cost, "distance");
        Object enabled = cards.getOrDefault("enabled", false);
        if (!(enabled instanceof Boolean)) throw new IllegalArgumentException("Invalid cost.cards.enabled flag");
        String mode = text(cards, "mode", "WAIVE_COST");
        return new CostConfiguration(groups, new CostCardSettings((boolean) enabled, integer(cards, "dailyGrant", 0),
                EnumCardType.valueOf(mode)), integer(distance, "maxDistance", 10000), integer(distance, "crossDimensionDistance", 10000));
    }

    private static String source(String file, String original, String body, boolean blocked) {
        String name = file.substring(0, file.length() - 5);
        String message = "Unconverted legacy formula; edit this source before enabling its cost group";
        return "package xin.vanilla.banira.generated.cost;\npublic final class " + name
                + " implements xin.vanilla.narcissus.api.cost.CostFormula {\n"
                + "private static final String ORIGINAL_EXPRESSION = " + JsonUtils.GSON.toJson(original) + ";\n"
                + (blocked ? "public " + name + "() { throw new IllegalStateException(\"" + message + "\"); }\n" : "")
                + "public double calculate(xin.vanilla.narcissus.api.cost.CostContext context) {\n"
                + (blocked ? "throw new IllegalStateException(\"" + message + "\");\n" : "double distance = context.distance();\n" + body)
                + "}\n" + LegacyCostExpression.helpers() + "}\n";
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> copy(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((key, value) -> {
            if (value instanceof Map) result.put(key, copy((Map<String, Object>) value));
            else if (value instanceof List) result.put(key, new ArrayList<>((List<?>) value));
            else result.put(key, value);
        });
        return result;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> table(Map<String, Object> map, String name) {
        Object value = map.get(name);
        if (value == null) return new LinkedHashMap<>();
        if (!(value instanceof Map)) throw new IllegalArgumentException("Expected table: " + name);
        return (Map<String, Object>) value;
    }

    private static Map<String, Object> legacyCards(Map<String, Object> base) {
        Object value = base.get("teleportCard");
        if (value != null && !(value instanceof Map) && !(value instanceof Boolean))
            throw new IllegalArgumentException("Expected table or boolean: base.teleportCard");
        Map<String, Object> cards = value instanceof Map ? table(base, "teleportCard") : new LinkedHashMap<>();
        if (value instanceof Boolean) cards.put("teleportCard", value);
        for (String key : Arrays.asList("teleportCardDaily", "teleportCardType")) {
            if (!base.containsKey(key)) continue;
            if (cards.containsKey(key)) {
                boolean same = key.equals("teleportCardDaily")
                        ? integer(cards, key, 0) == integer(base, key, 0)
                        : text(cards, key, "").equals(text(base, key, ""));
                if (!same) throw new IllegalArgumentException("Conflicting legacy card settings: base." + key);
            }
            cards.put(key, base.get(key));
        }
        return cards;
    }

    private static int legacyDistance(Map<String, Object> limits, Map<String, Object> general,
                                      String key, int fallback) {
        if (limits.containsKey(key) && general.containsKey(key)
                && integer(limits, key, fallback) != integer(general, key, fallback))
            throw new IllegalArgumentException("Conflicting legacy distance settings: base.teleportLimit." + key + " and general." + key);
        return limits.containsKey(key) ? integer(limits, key, fallback) : integer(general, key, fallback);
    }

    private static String text(Map<String, Object> map, String key, String fallback) {
        Object value = map.getOrDefault(key, fallback);
        if (!(value instanceof String)) throw new IllegalArgumentException("Expected string: " + key);
        return (String) value;
    }

    private static double number(Map<String, Object> map, String key, double fallback) {
        Object value = map.getOrDefault(key, fallback);
        if (!(value instanceof Number) || !Double.isFinite(((Number) value).doubleValue()))
            throw new IllegalArgumentException("Expected finite number: " + key);
        return ((Number) value).doubleValue();
    }

    private static int integer(Map<String, Object> map, String key, int fallback) {
        double value = number(map, key, fallback);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE || value != Math.rint(value))
            throw new IllegalArgumentException("Expected integer: " + key);
        return (int) value;
    }
}
