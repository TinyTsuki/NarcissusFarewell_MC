package xin.vanilla.narcissus.internal.neoforge.cost;

import com.electronwill.nightconfig.toml.TomlParser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.narcissus.data.cost.CostConfiguration;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class CostRegistrationMigrationTest {
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void firstStartDoesNotCreateACommonFileOrMigrationBackup() throws Exception {
        Path parent = temporary.newFolder().toPath(), common = parent.resolve("common.toml"), root = parent.resolve("mod");
        CostConfiguration result = new NeoForgeCostMigrationFile().migrateBeforeRegistration(common, root);
        assertEquals(EnumCostType.NONE, result.parameters(EnumTeleportType.TP_HOME).type());
        assertFalse(Files.exists(common));
        assertFalse(Files.exists(root));
    }

    @Test
    public void realJavaPreflightCompletesBeforeLegacyConfigIsReplaced() throws Exception {
        Path common = writeCommon(legacy("max(num, sqrt(distance) * rate)"));
        byte[] original = Files.readAllBytes(common);
        Path root = common.getParent().resolve("mod");
        CostConfiguration result = new NeoForgeCostMigrationFile().migrateBeforeRegistration(common, root);
        assertEquals("LegacyHomeCost.java", result.parameters(EnumTeleportType.TP_HOME).customFile());
        assertEquals("HEALTH", new TomlParser().parse(read(common)).get("cost.home.type"));
        assertFalse(new TomlParser().parse(read(common)).contains("cost.tpHome"));
        assertTrue(Files.exists(root.resolve("cost/sources/LegacyHomeCost.java")));
        assertArrayEquals(original, Files.readAllBytes(backup(root, "original.toml")));
        assertEquals("untouched", new TomlParser().parse(read(common)).get("integration.marker"));
        byte[] converted = Files.readAllBytes(common);
        new NeoForgeCostMigrationFile().migrateBeforeRegistration(common, root);
        assertArrayEquals(converted, Files.readAllBytes(common));
        try (Stream<Path> files = Files.walk(root.resolve("backups"))) {
            assertEquals(1, files.filter(p -> p.getFileName().toString().equals("original.toml")).count());
        }
    }

    @Test
    public void uncompiledActiveCustomSourceCannotEraseTheOriginalConfiguration() throws Exception {
        Path common = writeCommon(legacy("max(num, sqrt(distance) * rate)"));
        Path root = common.getParent().resolve("mod"), source = root.resolve("cost/sources/helpers/Broken.java");
        write(source, "package xin.vanilla.banira.generated.cost.helpers; public class Broken { invalid Java }");
        byte[] original = Files.readAllBytes(common);
        rejected(() -> new NeoForgeCostMigrationFile().migrateBeforeRegistration(common, root));
        assertArrayEquals(original, Files.readAllBytes(common));
        assertFalse(Files.exists(root.resolve("cost/migration.json")));
    }

    @Test
    public void disabledInvalidFormulaIsPreservedWithoutBeingCompiled() throws Exception {
        Path common = writeCommon(legacy("unknown + '05'").replace("'HEALTH'", "'NONE'"));
        Path root = common.getParent().resolve("mod");
        CostConfiguration result = new NeoForgeCostMigrationFile().migrateBeforeRegistration(common, root);
        assertEquals(EnumCostType.NONE, result.parameters(EnumTeleportType.TP_HOME).type());
        assertTrue(read(root.resolve("cost/sources/LegacyHomeCost.java")).contains(
                xin.vanilla.banira.common.util.JsonUtils.GSON.toJson("unknown + '05'")));
    }

    @Test
    public void startupPreflightNeverExecutesUserStaticInitializersOrConstructors() throws Exception {
        Path common = writeCommon("[cost.home]\ntype='HEALTH'\n[cost.home.custom]\nfile='Cold.java'\n");
        Path root = common.getParent().resolve("mod");
        write(root.resolve("cost/sources/Cold.java"), "package xin.vanilla.banira.generated.cost; "
                + "public class Cold implements xin.vanilla.narcissus.api.cost.CostFormula { "
                + "static { if (true) throw new IllegalStateException(\"no init during migration\"); } "
                + "public Cold() { throw new IllegalStateException(\"no construction\"); } "
                + "public double calculate(xin.vanilla.narcissus.api.cost.CostContext context) { return 1; } }");
        byte[] original = Files.readAllBytes(common);
        new NeoForgeCostMigrationFile().migrateBeforeRegistration(common, root);
        assertArrayEquals(original, Files.readAllBytes(common));
    }

    @Test
    public void helperEditedAfterPreflightCannotPublishTheMigratedConfig() throws Exception {
        Path common = writeCommon(legacy("max(num, sqrt(distance) * rate)"));
        Path root = common.getParent().resolve("mod"), helper = root.resolve("cost/sources/helpers/Helper.java");
        write(helper, helperSource(1));
        byte[] original = Files.readAllBytes(common);
        NeoForgeCostMigrationFile migration = new NeoForgeCostMigrationFile(point -> {
            if (point == NeoForgeCostMigrationFile.Checkpoint.AFTER_SOURCES) {
                try {
                    write(helper, helperSource(2));
                } catch (IOException error) {
                    throw new java.io.UncheckedIOException(error);
                }
            }
        });
        rejected(() -> migration.migrateBeforeRegistration(common, root));
        assertArrayEquals(original, Files.readAllBytes(common));
        assertEquals(helperSource(2), read(helper));
    }

    @Test
    public void interruptedMigrationIsRecompiledBeforeRecoveryCanReplaceConfig() throws Exception {
        Path common = writeCommon(legacy("max(num, sqrt(distance) * rate)"));
        Path root = common.getParent().resolve("mod"), helper = root.resolve("cost/sources/helpers/Helper.java");
        write(helper, helperSource(1));
        byte[] original = Files.readAllBytes(common);
        NeoForgeCostMigrationFile interrupted = new NeoForgeCostMigrationFile(point -> {
            if (point == NeoForgeCostMigrationFile.Checkpoint.AFTER_SOURCES)
                throw new IllegalStateException("interrupted");
        });
        try {
            interrupted.migrateBeforeRegistration(common, root);
            fail("No interruption");
        } catch (IllegalStateException expected) {
            assertEquals("interrupted", expected.getMessage());
        }
        write(helper, "broken helper");
        rejected(() -> new NeoForgeCostMigrationFile().migrateBeforeRegistration(common, root));
        assertArrayEquals(original, Files.readAllBytes(common));
        write(helper, helperSource(1));
        new NeoForgeCostMigrationFile().migrateBeforeRegistration(common, root);
        assertEquals("HEALTH", new TomlParser().parse(read(common)).get("cost.home.type"));
    }

    @Test
    public void missingEnabledSourceDoesNotAuthorizeForgeDefaultCorrection() throws Exception {
        Path common = writeCommon("[cost.home]\ntype='HEALTH'\n[cost.home.custom]\nfile='Missing.java'\n");
        byte[] original = Files.readAllBytes(common);
        rejected(() -> new NeoForgeCostMigrationFile().migrateBeforeRegistration(common, common.getParent().resolve("mod")));
        assertArrayEquals(original, Files.readAllBytes(common));
    }

    @Test
    public void lowLevelCommitCannotBypassAnInvalidActiveCustomSource() throws Exception {
        Path common = writeCommon("[cost.stage]\ntype='EXP_POINT'\n[cost.stage.custom]\nfile='Broken.java'\n");
        Path root = common.getParent().resolve("mod");
        write(root.resolve("cost/sources/Broken.java"), "broken Java");
        byte[] original = Files.readAllBytes(common);
        NeoForgeCostMigrationFile migration = new NeoForgeCostMigrationFile();
        xin.vanilla.narcissus.config.migration.CostMigrationPlan plan = migration.prepare(common, root);
        rejected(() -> migration.commit(plan));
        assertArrayEquals(original, Files.readAllBytes(common));
    }

    @Test
    public void lowLevelRecoveryCannotBypassAChangedBrokenHelper() throws Exception {
        Path common = writeCommon(legacy("max(num, sqrt(distance) * rate)"));
        Path root = common.getParent().resolve("mod"), helper = root.resolve("cost/sources/helpers/Helper.java");
        write(helper, helperSource(1));
        byte[] original = Files.readAllBytes(common);
        NeoForgeCostMigrationFile interrupted = new NeoForgeCostMigrationFile(point -> {
            if (point == NeoForgeCostMigrationFile.Checkpoint.AFTER_SOURCES)
                throw new IllegalStateException("interrupted");
        });
        try {
            interrupted.migrateBeforeRegistration(common, root);
            fail("No interruption");
        } catch (IllegalStateException expected) {
        }
        write(helper, "broken helper");
        rejected(() -> new NeoForgeCostMigrationFile().recover(common, root));
        assertArrayEquals(original, Files.readAllBytes(common));
    }

    private Path writeCommon(String contents) throws IOException {
        Path common = temporary.newFolder().toPath().resolve("common.toml");
        write(common, contents);
        return common;
    }

    private static String legacy(String expression) {
        return "[cost.tpHome]\ncostTpHomeType='HEALTH'\ncostTpHomeNum=4\ncostTpHomeRate=0.01\n"
                + "costTpHomeNumUpper=20\ncostTpHomeNumLower=0\ncostTpHomeConf=''\ncostTpHomeExp=\"" + expression
                + "\"\n[integration]\nmarker='untouched'\n";
    }

    private static String helperSource(int number) {
        return "package xin.vanilla.banira.generated.cost.helpers; public class Helper { public static final int VALUE=" + number + "; }";
    }

    private static void write(Path path, String text) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, text.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static Path backup(Path root, String name) throws IOException {
        try (Stream<Path> files = Files.walk(root.resolve("backups"))) {
            return files.filter(p -> p.getFileName().toString().equals(name)).findFirst().orElseThrow(() -> new IOException("No backup"));
        }
    }

    private static void rejected(Checked operation) throws Exception {
        try {
            operation.run();
            fail("Expected migration rejection");
        } catch (IOException expected) {
        }
    }

    private interface Checked {
        void run() throws Exception;
    }
}
