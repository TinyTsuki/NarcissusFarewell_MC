package xin.vanilla.narcissus.internal.fabric.cost;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.narcissus.config.migration.CostMigrationPlan;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class FabricCostMigrationFileTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void preparesFromLegacyTomlBeforeRegistrationWithoutChangingTheFile() throws Exception {
        Fixture f = fixture();
        FabricCostMigrationFile migration = new FabricCostMigrationFile();
        try (CommentedFileConfig loaded = CommentedFileConfig.builder(f.common).sync().build()) {
            loaded.load();
            assertEquals(Integer.valueOf(2), loaded.get("cost.tpHome.costTpHomeNum"));
            CostMigrationPlan plan = migration.prepare(f.common, f.root);
            assertEquals(.006, plan.configuration().parameters(EnumTeleportType.TP_HOME).perBlockAmount(), 0);
            assertArrayEquals(f.original, Files.readAllBytes(f.common));
            assertTrue(loaded.contains("cost.tpHome.costTpHomeNum"));
            assertArrayEquals(f.original, Files.readAllBytes(f.common));
        }
    }

    @Test public void commitPreservesUnknownValuesAndOriginalBytesAndIsIdempotent() throws Exception {
        Fixture f = fixture();
        FabricCostMigrationFile migration = new FabricCostMigrationFile();
        CostMigrationPlan plan = migration.prepare(f.common, f.root);
        migration.commit(plan);
        assertArrayEquals(f.original, Files.readAllBytes(findBackup(f.root, "original.toml")));
        assertEquals("保留配置", new TomlParser().parse(read(f.common)).get("title"));
        assertEquals(Integer.valueOf(7), new TomlParser().parse(read(f.common)).get("base.teleportLimit.teleportHomeLimit"));
        assertEquals("data merge entity @s {Tags:[\"a,b\",\"{amount}\"]}", new TomlParser().parse(read(f.common)).get("cost.stage.command"));
        assertEquals(java.util.Arrays.asList("minecraft:arrow", "tick, item -> tick >= 5"),
                new TomlParser().parse(read(f.common)).get("integration.entities"));
        assertTrue(Files.exists(f.root.resolve("cost/sources/LegacyBackCost.java")));
        byte[] migrated = Files.readAllBytes(f.common);
        FabricCostMigrationFile restart = new FabricCostMigrationFile();
        restart.recover(f.common, f.root);
        CostMigrationPlan repeated = restart.prepare(f.common, f.root);
        assertFalse(repeated.migrationRequired());
        restart.commit(repeated);
        assertArrayEquals(migrated, Files.readAllBytes(f.common));
    }

    @Test public void concurrentCommonEditAndExistingSourceConflictCannotBeOverwritten() throws Exception {
        Fixture f = fixture();
        FabricCostMigrationFile migration = new FabricCostMigrationFile();
        CostMigrationPlan plan = migration.prepare(f.common, f.root);
        byte[] edited = (read(f.common) + "\n[manual]\nvalue = 1\n").getBytes(StandardCharsets.UTF_8);
        Files.write(f.common, edited);
        ioFailure(() -> migration.commit(plan));
        assertArrayEquals(edited, Files.readAllBytes(f.common));
        Files.write(f.common, f.original);
        Path source = f.root.resolve("cost/sources/LegacyBackCost.java");
        Files.createDirectories(source.getParent()); Files.write(source, "user source".getBytes(StandardCharsets.UTF_8));
        ioFailure(() -> new FabricCostMigrationFile().prepare(f.common, f.root));
        assertEquals("user source", read(source));
        assertArrayEquals(f.original, Files.readAllBytes(f.common));
    }

    @Test public void interruptionAfterSourceInstallOrConfigReplaceRecoversToOneCompleteSchema() throws Exception {
        for (FabricCostMigrationFile.Checkpoint point : new FabricCostMigrationFile.Checkpoint[]{
                FabricCostMigrationFile.Checkpoint.AFTER_SOURCES, FabricCostMigrationFile.Checkpoint.AFTER_CONFIG}) {
            Fixture f = fixture();
            FabricCostMigrationFile interrupted = new FabricCostMigrationFile(current -> {
                if (current == point) throw new IllegalStateException("Simulated interruption");
            });
            CostMigrationPlan plan = interrupted.prepare(f.common, f.root);
            try { interrupted.commit(plan); fail("No interruption"); }
            catch (IllegalStateException expected) { }
            new FabricCostMigrationFile().recover(f.common, f.root);
            assertEquals("EXP_POINT", new TomlParser().parse(read(f.common)).get("cost.home.type"));
            assertFalse(new TomlParser().parse(read(f.common)).contains("cost.tpHome"));
            assertTrue(Files.exists(f.root.resolve("cost/sources/LegacyBackCost.java")));
        }
    }

    @Test public void committedMissingSourceIsRestoredButAnExistingEditSurvives() throws Exception {
        Fixture f = fixture();
        FabricCostMigrationFile migration = new FabricCostMigrationFile();
        migration.commit(migration.prepare(f.common, f.root));
        Path source = f.root.resolve("cost/sources/LegacyBackCost.java");
        byte[] expected = Files.readAllBytes(source);
        Files.delete(source);
        new FabricCostMigrationFile().recover(f.common, f.root);
        assertArrayEquals(expected, Files.readAllBytes(source));
        Files.write(source, "edited source".getBytes(StandardCharsets.UTF_8));
        new FabricCostMigrationFile().recover(f.common, f.root);
        assertEquals("edited source", read(source));
    }

    @Test public void corruptRecoveryEvidenceAndManualConfigChangesBlockIncompleteRecovery() throws Exception {
        Fixture f = fixture();
        FabricCostMigrationFile interrupted = new FabricCostMigrationFile(point -> {
            if (point == FabricCostMigrationFile.Checkpoint.AFTER_SOURCES) throw new IllegalStateException("stop");
        });
        try { interrupted.commit(interrupted.prepare(f.common, f.root)); fail("No interruption"); }
        catch (IllegalStateException expected) { }
        Files.write(findBackup(f.root, "original.toml"), "bad backup".getBytes(StandardCharsets.UTF_8));
        ioFailure(() -> new FabricCostMigrationFile().recover(f.common, f.root));
        assertArrayEquals(f.original, Files.readAllBytes(f.common));
    }

    @Test public void sourceEditedAfterPrepareCannotPublishConfiguration() throws Exception {
        Fixture f = fixture();
        FabricCostMigrationFile migration = new FabricCostMigrationFile();
        CostMigrationPlan plan = migration.prepare(f.common, f.root);
        Path source = f.root.resolve("cost/sources/LegacyBackCost.java");
        Files.createDirectories(source.getParent()); Files.write(source, "manual source".getBytes(StandardCharsets.UTF_8));
        ioFailure(() -> migration.commit(plan));
        assertArrayEquals(f.original, Files.readAllBytes(f.common));
        assertEquals("manual source", read(source));
    }

    @Test public void linkedModRootIsRejectedBeforeWritingAnyBackup() throws Exception {
        Fixture f = fixture(); Path outside = temporary.newFolder().toPath();
        try { Files.createSymbolicLink(f.root, outside); }
        catch (IOException | UnsupportedOperationException unavailable) {
            if (!System.getProperty("os.name").startsWith("Windows")) { Assume.assumeNoException(unavailable); return; }
            // Windows junctions do not require the symbolic-link privilege and exercise real path escape checks.
            Process helper = new ProcessBuilder("cmd.exe", "/d", "/c", "mklink", "/J", f.root.toString(), outside.toString()).start();
            try { assertTrue(helper.waitFor(15, TimeUnit.SECONDS)); assertEquals(0, helper.exitValue()); }
            finally { if (helper.isAlive()) helper.destroyForcibly().waitFor(5, TimeUnit.SECONDS); }
        }
        ioFailure(() -> new FabricCostMigrationFile().prepare(f.common, f.root));
        assertEquals(0, outside.toFile().list().length);
    }

    @Test public void sourceChangedAfterInstallBlocksConfigReplacement() throws Exception {
        Fixture f = fixture();
        FabricCostMigrationFile migration = new FabricCostMigrationFile(point -> {
            if (point == FabricCostMigrationFile.Checkpoint.AFTER_SOURCES) {
                try { Files.write(f.root.resolve("cost/sources/LegacyBackCost.java"), "edited during commit".getBytes(StandardCharsets.UTF_8)); }
                catch (IOException error) { throw new UncheckedIOException(error); }
            }
        });
        CostMigrationPlan plan = migration.prepare(f.common, f.root);
        ioFailure(() -> migration.commit(plan));
        assertArrayEquals(f.original, Files.readAllBytes(f.common));
    }

    @Test public void candidateCorruptedAfterPreparationCannotReplaceCommonConfig() throws Exception {
        Fixture f = fixture();
        FabricCostMigrationFile migration = new FabricCostMigrationFile(point -> {
            if (point == FabricCostMigrationFile.Checkpoint.AFTER_PREPARED) {
                try { Files.write(findBackup(f.root, "candidate.toml"), "corrupted candidate".getBytes(StandardCharsets.UTF_8)); }
                catch (IOException error) { throw new UncheckedIOException(error); }
            }
        });
        CostMigrationPlan plan = migration.prepare(f.common, f.root);
        ioFailure(() -> migration.commit(plan));
        assertArrayEquals(f.original, Files.readAllBytes(f.common));
    }

    @Test public void manualCommonEditAfterAnInterruptionIsPreservedAndBlocksRecovery() throws Exception {
        Fixture f = fixture();
        FabricCostMigrationFile interrupted = new FabricCostMigrationFile(point -> {
            if (point == FabricCostMigrationFile.Checkpoint.AFTER_SOURCES) throw new IllegalStateException("stop");
        });
        try { interrupted.commit(interrupted.prepare(f.common, f.root)); fail("No interruption"); }
        catch (IllegalStateException expected) { }
        byte[] edit = "title = 'manually edited'\n".getBytes(StandardCharsets.UTF_8); Files.write(f.common, edit);
        ioFailure(() -> new FabricCostMigrationFile().recover(f.common, f.root));
        assertArrayEquals(edit, Files.readAllBytes(f.common));
    }

    @Test public void malformedEncodingAndSourceTraversalFailWithoutChangingInput() throws Exception {
        Fixture f = fixture();
        byte[] invalidUtf8 = {(byte) 0xff, (byte) 0xfe}; Files.write(f.common, invalidUtf8);
        ioFailure(() -> new FabricCostMigrationFile().prepare(f.common, f.root));
        assertArrayEquals(invalidUtf8, Files.readAllBytes(f.common));
        String traversal = "[cost.home]\ntype='EXP_POINT'\n[cost.home.custom]\nfile='../outside.java'\n";
        Files.write(f.common, traversal.getBytes(StandardCharsets.UTF_8));
        ioFailure(() -> new FabricCostMigrationFile().prepare(f.common, f.root));
        assertEquals(traversal, read(f.common));
        assertFalse(Files.exists(f.root.resolve("backups")));
    }

    @Test public void recoveryRejectsJournalPathEscapeAndMissingOrCorruptSourceSnapshot() throws Exception {
        Fixture f = fixture();
        FabricCostMigrationFile migration = new FabricCostMigrationFile();
        migration.commit(migration.prepare(f.common, f.root));
        Path live = f.root.resolve("cost/sources/LegacyBackCost.java"); Files.delete(live);
        Path snapshot;
        try (Stream<Path> paths = Files.walk(f.root.resolve("backups"))) {
            snapshot = paths.filter(path -> path.getFileName().toString().equals("LegacyBackCost.java")).findFirst().get();
        }
        Files.write(snapshot, "corrupt source snapshot".getBytes(StandardCharsets.UTF_8));
        ioFailure(() -> new FabricCostMigrationFile().recover(f.common, f.root));
        assertFalse(Files.exists(live));
        Path journal = f.root.resolve("cost/migration.json");
        com.google.gson.JsonObject object = xin.vanilla.banira.common.util.JsonUtils.GSON.fromJson(read(journal), com.google.gson.JsonObject.class);
        object.addProperty("backup", "backups/cost-migration/../../outside");
        Files.write(journal, object.toString().getBytes(StandardCharsets.UTF_8));
        ioFailure(() -> new FabricCostMigrationFile().recover(f.common, f.root));
        assertFalse(Files.exists(live));
    }

    private Fixture fixture() throws Exception {
        Path parent = temporary.newFolder().toPath(), common = parent.resolve("common.toml"), root = parent.resolve("narcissus_farewell");
        byte[] original;
        try (InputStream input = getClass().getResourceAsStream("/cost-migration/legacy.toml")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[4096]; int count;
            while ((count = input.read(buffer)) != -1) out.write(buffer, 0, count);
            original = out.toByteArray();
        }
        Files.write(common, original);
        return new Fixture(common, root, original);
    }

    private static Path findBackup(Path root, String name) throws IOException {
        try (Stream<Path> paths = Files.walk(root.resolve("backups"))) {
            return paths.filter(path -> path.getFileName().toString().equals(name)).findFirst().orElseThrow(() -> new IOException("Missing backup"));
        }
    }
    private static String read(Path file) throws IOException { return new String(Files.readAllBytes(file), StandardCharsets.UTF_8); }
    private static void ioFailure(Checked call) throws Exception {
        try { call.run(); fail("Expected safe file rejection"); } catch (IOException expected) { }
    }
    private interface Checked { void run() throws Exception; }
    private static final class Fixture {
        final Path common, root; final byte[] original;
        Fixture(Path common, Path root, byte[] original) { this.common = common; this.root = root; this.original = original; }
    }
}
