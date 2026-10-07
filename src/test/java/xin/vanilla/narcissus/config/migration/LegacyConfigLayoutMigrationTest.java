package xin.vanilla.narcissus.config.migration;

import org.junit.Test;
import xin.vanilla.narcissus.config.CommonConfig;

import java.lang.reflect.Field;

import java.util.*;

import static org.junit.Assert.*;

public class LegacyConfigLayoutMigrationTest {
    @Test
    public void everySupportedAliasTargetsAnExistingSettingAndPreservesItsValue() throws Exception {
        assertEquals(97, LegacyConfigLayoutMigration.paths().size());
        Map<String, Object> input = new LinkedHashMap<>();
        Map<String, Object> expected = new LinkedHashMap<>();
        int index = 0;
        for (Map.Entry<String, String> alias : LegacyConfigLayoutMigration.paths().entrySet()) {
            Class<?> type = CommonConfig.class;
            Field field = null;
            for (String part : alias.getValue().split("\\.")) {
                field = type.getDeclaredField(part);
                type = field.getType();
            }
            Object value;
            if (type == String.class) value = "legacy_" + index;
            else if (type == boolean.class) value = index % 2 == 0;
            else if (type == int.class) value = index + 3;
            else if (type == double.class) value = index + .5;
            else if (List.class.isAssignableFrom(type)) value = Arrays.asList("legacy_" + index, "minecraft:stone");
            else if (type.isEnum()) value = ((Enum<?>) type.getEnumConstants()[0]).name();
            else throw new AssertionError("Unsupported target: " + alias.getValue() + " " + field);
            put(input, alias.getKey(), value);
            expected.put(alias.getValue(), value);
            index++;
        }
        CostMigrationPlan plan = CostConfigMigration.plan(input);
        assertTrue(plan.migrationRequired());
        for (Map.Entry<String, Object> entry : expected.entrySet())
            assertEquals(entry.getKey(), entry.getValue(), get(plan.configurationValues(), entry.getKey()));
        for (String path : LegacyConfigLayoutMigration.paths().keySet()) {
            assertNull(path, get(plan.configurationValues(), path));
            assertNotNull(path, get(input, path));
        }
        assertFalse(CostConfigMigration.plan(plan.configurationValues()).migrationRequired());
    }

    @Test
    public void oldSettingsMoveToTheirActivePathsAndRemovedOrUnknownDataStays() {
        Map<String, Object> input = map("commandNames", map("commandPrefix", "legacycheck", "unknown", "keep",
                "tpHome", map("commandSetHome", "oldsethome")),
                "conciseCommands", map("conciseLanguage", true, "tpAsk", map("conciseTpAsk", false)),
                "base", map("flySpeedMax", 7.5, "removeOriginalTp", true),
                "general", map("teleportHomeLimit", 17, "tpWithVehicle", false, "tpSound", "minecraft:block.note_block.bell",
                        "safeTeleport", map("safeChunkRange", 3, "unsafeBlocks", Arrays.asList("minecraft:lava"), "extra", "keep"),
                        "defaultLanguage", "zh_cn", "helpHeader", "old help", "helpInfoNumPerPage", 13));
        CostMigrationPlan plan = CostConfigMigration.plan(input);
        assertTrue(plan.migrationRequired());
        Map<String, Object> result = plan.configurationValues();
        assertEquals("legacycheck", get(result, "command.commandPrefix"));
        assertEquals("oldsethome", get(result, "command.tpHome.commandSetHome"));
        assertEquals(Boolean.TRUE, get(result, "concise.conciseLanguage"));
        assertEquals(Boolean.FALSE, get(result, "concise.tpAsk.conciseTpAsk"));
        assertEquals(7.5, get(result, "base.creativeFlight.flySpeedMax"));
        assertEquals(Boolean.TRUE, get(result, "base.other.removeOriginalTp"));
        assertEquals(17, get(result, "base.teleportLimit.teleportHomeLimit"));
        assertEquals(Boolean.FALSE, get(result, "base.teleportTogether.tpWithVehicle"));
        assertEquals("minecraft:block.note_block.bell", get(result, "base.other.tpSound"));
        assertEquals(3, get(result, "base.safeTeleport.safeChunkRange"));
        assertEquals(Arrays.asList("minecraft:lava"), get(result, "base.safeTeleport.unsafeBlocks"));
        assertEquals("keep", get(result, "commandNames.unknown"));
        assertEquals("keep", get(result, "general.safeTeleport.extra"));
        assertEquals("zh_cn", get(result, "general.defaultLanguage"));
        assertEquals("old help", get(result, "general.helpHeader"));
        assertEquals(13, get(result, "general.helpInfoNumPerPage"));
        assertEquals("legacycheck", get(input, "commandNames.commandPrefix"));
        assertFalse(CostConfigMigration.plan(result).migrationRequired());
    }

    @Test
    public void newAndOldEqualValuesAreAcceptedAndConflictsAreBlocked() {
        Map<String, Object> input = map("general", map("teleportHomeLimit", 17),
                "base", map("teleportLimit", map("teleportHomeLimit", 17L)));
        assertEquals(17L, get(CostConfigMigration.plan(input).configurationValues(), "base.teleportLimit.teleportHomeLimit"));
        ((Map<String, Object>) input.get("general")).put("teleportHomeLimit", 9);
        try {
            CostConfigMigration.plan(input);
            fail("conflicting settings must not be overwritten");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("teleportHomeLimit"));
        }
        assertEquals(9, get(input, "general.teleportHomeLimit"));
    }

    @Test
    public void aliasOnlyMigrationDoesNotInventLegacyCostsOrOverwriteCurrentCosts() {
        Map<String, Object> input = map("commandNames", map("commandPrefix", "legacycheck"),
                "cost", map("home", map("type", "NONE", "fixedAmount", 42.0)));
        CostMigrationPlan plan = CostConfigMigration.plan(input);
        assertTrue(plan.migrationRequired());
        assertEquals(42.0, get(plan.configurationValues(), "cost.home.fixedAmount"));
        assertFalse(((Map<?, ?>) plan.configurationValues().get("cost")).containsKey("tpHome"));
    }

    @Test
    public void invalidScalarAndMalformedParentAreRejectedInsteadOfReset() {
        for (Map<String, Object> input : Arrays.asList(
                map("conciseCommands", map("conciseLanguage", "true")),
                map("general", map("teleportHomeLimit", 2.5)),
                map("general", map("safeTeleport", map("unsafeBlocks", "minecraft:lava"))),
                map("commandNames", map("commandPrefix", "legacycheck"), "command", "invalid"))) {
            try {
                CostConfigMigration.plan(input);
                fail("invalid legacy setting accepted");
            } catch (IllegalArgumentException expected) {
                assertFalse(expected.getMessage().isEmpty());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Object get(Map<String, Object> input, String path) {
        Object value = input;
        for (String key : path.split("\\.")) {
            if (!(value instanceof Map)) return null;
            value = ((Map<String, Object>) value).get(key);
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static void put(Map<String, Object> input, String path, Object value) {
        String[] parts = path.split("\\.");
        for (int index = 0; index < parts.length - 1; index++)
            input = (Map<String, Object>) input.computeIfAbsent(parts[index], key -> new LinkedHashMap<String, Object>());
        input.put(parts[parts.length - 1], value);
    }

    private static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) result.put((String) pairs[index], pairs[index + 1]);
        return result;
    }
}
