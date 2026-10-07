package xin.vanilla.narcissus.internal.forge.cost;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.narcissus.config.migration.CostMigrationPlan;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ForgeFlatCostMigrationFileTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void flatFileMigratesTransactionallyAndRetainsAllUnrelatedValues() throws Exception {
        String fixture = System.getProperty("narcissus.legacyTestInput", "");
        byte[] original = fixture.isEmpty() ? (
                "[base]\nteleportCard=true\nteleportCardDaily=9\nteleportCardType='REFUND_COST'\nremoveOriginalTp=true\n"
                        + "[general]\nteleportCostDistanceLimit=8123\nteleportCostDistanceAcrossDimension=456\nteleportHomeLimit=7\n"
                        + "[integration]\nentities=['minecraft:arrow', 'tick, item -> tick >= 5']\n"
                        + "[cost.tpHome]\ncostTpHomeType='NONE'\ncostTpHomeNum=2\ncostTpHomeRate=0.003\n").getBytes(StandardCharsets.UTF_8)
                : Files.readAllBytes(Paths.get(fixture));
        Path parent = temporary.newFolder().toPath(), common = parent.resolve("common.toml"), root = parent.resolve("mod");
        Files.write(common, original);
        UnmodifiableConfig before = parse(original);
        ForgeCostMigrationFile migration = new ForgeCostMigrationFile();
        CostMigrationPlan plan = migration.prepare(common, root);
        assertTrue(plan.migrationRequired());
        assertArrayEquals(original, Files.readAllBytes(common));
        assertEquals(before.get("base.teleportCard"), plan.configuration().cards().enabled());
        assertEquals(((Number) before.get("base.teleportCardDaily")).intValue(), plan.configuration().cards().dailyGrant());
        assertEquals(((Number) before.get("general.teleportCostDistanceLimit")).intValue(), plan.configuration().maxDistance());
        assertEquals(((Number) before.get("general.teleportCostDistanceAcrossDimension")).intValue(), plan.configuration().crossDimensionDistance());
        migration.commit(plan);
        UnmodifiableConfig after = parse(Files.readAllBytes(common));
        for (UnmodifiableConfig.Entry entry : before.entrySet()) {
            if (!Arrays.asList("base", "general", "cost").contains(entry.getKey()))
                assertEquals(entry.getKey(), value(entry.getValue()), value(after.get(entry.getKey())));
        }
        Map<String, Object> expectedBase = table(before, "base"), expectedGeneral = table(before, "general");
        for (String key : Arrays.asList("teleportCard", "teleportCardDaily", "teleportCardType")) expectedBase.remove(key);
        expectedGeneral.remove("teleportCostDistanceLimit");
        expectedGeneral.remove("teleportCostDistanceAcrossDimension");
        assertEquals(expectedBase, table(after, "base"));
        assertEquals(expectedGeneral, table(after, "general"));
        Path backup;
        try (Stream<Path> paths = Files.walk(root.resolve("backups"))) {
            backup = paths.filter(path -> path.getFileName().toString().equals("original.toml")).findFirst().get();
        }
        assertArrayEquals(original, Files.readAllBytes(backup));
        byte[] migrated = Files.readAllBytes(common);
        ForgeCostMigrationFile restart = new ForgeCostMigrationFile();
        restart.migrateBeforeRegistration(common, root);
        assertArrayEquals(migrated, Files.readAllBytes(common));
    }

    @Test
    public void conflictingFlatAndNestedDistancesPreserveOriginalBytes() throws Exception {
        byte[] original = ("[base]\nteleportCard=false\n[base.teleportLimit]\nteleportCostDistanceLimit=7000\n"
                + "[general]\nteleportCostDistanceLimit=8000\n").getBytes(StandardCharsets.UTF_8);
        Path parent = temporary.newFolder().toPath(), common = parent.resolve("common.toml"), root = parent.resolve("mod");
        Files.write(common, original);
        try {
            new ForgeCostMigrationFile().migrateBeforeRegistration(common, root);
            fail("conflicting values must stop migration");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("teleportCostDistanceLimit"));
        }
        assertArrayEquals(original, Files.readAllBytes(common));
        assertFalse(Files.exists(root.resolve("cost/migration.json")));
    }

    @Test
    public void invalidFlatCardFlagPreservesOriginalBytes() throws Exception {
        byte[] original = "[base]\nteleportCard='false'\n".getBytes(StandardCharsets.UTF_8);
        Path parent = temporary.newFolder().toPath(), common = parent.resolve("common.toml"), root = parent.resolve("mod");
        Files.write(common, original);
        try {
            new ForgeCostMigrationFile().migrateBeforeRegistration(common, root);
            fail("invalid flag must stop migration");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("teleportCard"));
        }
        assertArrayEquals(original, Files.readAllBytes(common));
    }

    private static UnmodifiableConfig parse(byte[] bytes) {
        return new TomlParser().parse(new String(bytes, StandardCharsets.UTF_8));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> table(UnmodifiableConfig config, String key) {
        Object table = config.get(key);
        return table == null ? new LinkedHashMap<>() : (Map<String, Object>) value(table);
    }

    private static Object value(Object value) {
        if (value instanceof UnmodifiableConfig) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (UnmodifiableConfig.Entry entry : ((UnmodifiableConfig) value).entrySet()) result.put(entry.getKey(), value(entry.getValue()));
            return result;
        }
        if (value instanceof List) {
            List<Object> result = new ArrayList<>();
            for (Object item : (List<?>) value) result.add(value(item));
            return result;
        }
        return value;
    }
}
