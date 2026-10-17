package xin.vanilla.narcissus.integration.map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class JourneyMapFileMigrationTest {
    private static final String ID = "7f21eb5e-04d8-4833-a28e-2e9ac7e859dc";
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private static String filename(String owner, String id) throws Exception {
        return (String) Class.forName("xin.vanilla.narcissus.integration.map.LegacyJourneyMapFiles")
                .getMethod("fileName", String.class, String.class).invoke(null, owner, id);
    }

    private static void prepare(Path directory) throws Exception {
        try {
            Class.forName("xin.vanilla.narcissus.integration.map.LegacyJourneyMapFiles")
                    .getMethod("prepare", Path.class).invoke(null, directory);
        } catch (InvocationTargetException error) {
            if (error.getCause() instanceof Exception) throw (Exception) error.getCause();
            throw error;
        }
    }

    private static byte[] json(String owner, String id, String name) {
        return ("{\"id\":\"old_file_-1-64-2\",\"origin\":\"" + owner
                + "\",\"displayId\":\"" + id + "\",\"name\":\"" + name
                + "\",\"x\":-1,\"y\":64,\"z\":-2,\"dimensions\":[\"minecraft:overworld\"]}\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static Path target(Path root, String kind) {
        return root.resolve("narcissus_farewell_" + kind + "_" + ID + ".json");
    }

    @Test public void stableFilesSeparateKindsAndRejectUnprovenOwnership() throws Exception {
        assertEquals("narcissus_farewell_HOME_" + ID + ".json", filename("narcissus_farewell", "HOME_" + ID));
        assertNotEquals(filename("narcissus_farewell", "HOME_" + ID), filename("narcissus_farewell", "STAGE_" + ID));
        assertNull(filename("journeymap", "HOME_" + ID));
        assertNull(filename("narcissus_farewell", "HOME_../" + ID));
        assertNull(filename("narcissus_farewell", "OTHER_" + ID));
        assertNull(filename("narcissus_farewell", "HOME_" + ID.toUpperCase(java.util.Locale.ROOT)));
        assertNull(filename("narcissus_farewell", null));
    }

    @Test public void conversionBacksUpExactBytesAndPreservesManualPoint() throws Exception {
        Path root = temporary.newFolder().toPath();
        byte[] home = json("narcissus_farewell", "HOME_" + ID, "home");
        byte[] stage = json("narcissus_farewell", "STAGE_" + ID, "home");
        byte[] manual = json("journeymap", "", "home");
        Files.write(root.resolve("old-home.json"), home);
        Files.write(root.resolve("old-stage.json"), stage);
        Files.write(root.resolve("home_-1-64-2.json"), manual);
        prepare(root);
        assertArrayEquals(home, Files.readAllBytes(target(root, "HOME")));
        assertArrayEquals(stage, Files.readAllBytes(target(root, "STAGE")));
        assertArrayEquals(manual, Files.readAllBytes(root.resolve("home_-1-64-2.json")));
        assertFalse(Files.exists(root.resolve("old-home.json")));
        assertFalse(Files.exists(root.resolve("old-stage.json")));
        try (java.util.stream.Stream<Path> paths = Files.walk(root.resolve("narcissus-waypoint-backups"))) {
            List<Path> backups = paths.filter(Files::isRegularFile).collect(java.util.stream.Collectors.toList());
            assertEquals(2, backups.size());
            assertTrue(backups.stream().anyMatch(path -> path.getFileName().toString().equals("old-home.json")));
            for (Path path : backups) assertArrayEquals(path.getFileName().toString().equals("old-home.json") ? home : stage, Files.readAllBytes(path));
        }
        prepare(root);
        Files.delete(target(root, "HOME"));
        prepare(root);
        assertFalse(Files.exists(target(root, "HOME")));
        assertArrayEquals(stage, Files.readAllBytes(target(root, "STAGE")));
        assertArrayEquals(manual, Files.readAllBytes(root.resolve("home_-1-64-2.json")));
    }

    @Test public void backupFailureKeepsEveryOriginalAndCreatesNoCanonicalFile() throws Exception {
        Path root = temporary.newFolder().toPath();
        byte[] bytes = json("narcissus_farewell", "HOME_" + ID, "home");
        Files.write(root.resolve("old.json"), bytes);
        Files.write(root.resolve("narcissus-waypoint-backups"), "blocked".getBytes(StandardCharsets.UTF_8));
        assertThrows(IOException.class, () -> prepare(root));
        assertArrayEquals(bytes, Files.readAllBytes(root.resolve("old.json")));
        assertFalse(Files.exists(target(root, "HOME")));
    }

    @Test public void existingTargetNeverOverwritesEitherRecord() throws Exception {
        Path root = temporary.newFolder().toPath();
        byte[] old = json("narcissus_farewell", "HOME_" + ID, "home");
        byte[] existing = json("journeymap", "", "manual");
        Files.write(root.resolve("old.json"), old);
        Files.write(target(root, "HOME"), existing);
        assertThrows(IOException.class, () -> prepare(root));
        assertArrayEquals(old, Files.readAllBytes(root.resolve("old.json")));
        assertArrayEquals(existing, Files.readAllBytes(target(root, "HOME")));
    }

    @Test public void blockedDestinationKeepsAllOriginalsBeforeAnyConversion() throws Exception {
        Path root = temporary.newFolder().toPath();
        byte[] home = json("narcissus_farewell", "HOME_" + ID, "home");
        byte[] stage = json("narcissus_farewell", "STAGE_" + ID, "home");
        Files.write(root.resolve("old-home.json"), home);
        Files.write(root.resolve("old-stage.json"), stage);
        Files.createDirectory(target(root, "STAGE"));
        assertThrows(IOException.class, () -> prepare(root));
        assertArrayEquals(home, Files.readAllBytes(root.resolve("old-home.json")));
        assertArrayEquals(stage, Files.readAllBytes(root.resolve("old-stage.json")));
        assertFalse(Files.exists(target(root, "HOME")));
    }

    @Test public void interruptedCopyWithIdenticalCanonicalCanResumeAfterBackup() throws Exception {
        Path root = temporary.newFolder().toPath();
        byte[] bytes = json("narcissus_farewell", "HOME_" + ID, "home");
        Files.write(root.resolve("old.json"), bytes);
        Files.write(target(root, "HOME"), bytes);
        prepare(root);
        assertFalse(Files.exists(root.resolve("old.json")));
        assertArrayEquals(bytes, Files.readAllBytes(target(root, "HOME")));
        try (java.util.stream.Stream<Path> files = Files.walk(root.resolve("narcissus-waypoint-backups"))) {
            Path backup = files.filter(Files::isRegularFile).findFirst().orElseThrow(IllegalStateException::new);
            assertArrayEquals(bytes, Files.readAllBytes(backup));
        }
    }

    @Test public void foreignMalformedAndUnrecognizedFilesStayByteIdentical() throws Exception {
        Path root = temporary.newFolder().toPath();
        List<byte[]> data = java.util.Arrays.asList(json("journeymap", "HOME_" + ID, "manual"),
                json("narcissus_farewell", "HOME_old", "old"), "{broken".getBytes(StandardCharsets.UTF_8),
                "{\"origin\":[],\"displayId\":42}".getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < data.size(); i++) Files.write(root.resolve(i + ".json"), data.get(i));
        prepare(root);
        for (int i = 0; i < data.size(); i++) assertArrayEquals(data.get(i), Files.readAllBytes(root.resolve(i + ".json")));
        assertFalse(Files.exists(root.resolve("narcissus-waypoint-backups")));
    }

    @Test public void missingDirectoryDoesNotCreateMapState() throws Exception {
        Path missing = temporary.getRoot().toPath().resolve("missing");
        prepare(missing);
        assertFalse(Files.exists(missing));
    }

    private void failedWriteCanResume(boolean backup, boolean partial) throws Exception {
        Path root = temporary.newFolder().toPath();
        byte[] bytes = json("narcissus_farewell", "HOME_" + ID, "home");
        Files.write(root.resolve("old.json"), bytes);
        LegacyJourneyMapFiles.FileAccess access = new LegacyJourneyMapFiles.FileAccess() {
            @Override void write(Path file, byte[] value) throws IOException {
                boolean isBackup = file.startsWith(root.resolve("narcissus-waypoint-backups"));
                if (isBackup == backup) {
                    super.write(file, partial ? java.util.Arrays.copyOf(value, value.length / 2) : value);
                    throw new IOException(partial ? "injected partial write" : "injected force/close failure");
                }
                super.write(file, value);
            }
        };
        assertThrows(IOException.class, () -> LegacyJourneyMapFiles.prepare(root, access));
        assertArrayEquals(bytes, Files.readAllBytes(root.resolve("old.json")));
        assertFalse("failed write must not publish a canonical JSON", Files.exists(target(root, "HOME")));
        if (backup) {
            try (java.util.stream.Stream<Path> paths = Files.walk(root.resolve("narcissus-waypoint-backups"))) {
                assertEquals("failed backup must not publish a partial file", 0, paths.filter(Files::isRegularFile).count());
            }
        }
        prepare(root);
        assertFalse(Files.exists(root.resolve("old.json")));
        assertArrayEquals(bytes, Files.readAllBytes(target(root, "HOME")));
    }

    @Test public void partialCanonicalWriteLeavesNoTargetAndRetrySucceeds() throws Exception { failedWriteCanResume(false, true); }
    @Test public void canonicalForceOrCloseFailureLeavesNoTargetAndRetrySucceeds() throws Exception { failedWriteCanResume(false, false); }
    @Test public void partialBackupWriteLeavesOriginalUntouchedAndRetrySucceeds() throws Exception { failedWriteCanResume(true, true); }
    @Test public void backupForceOrCloseFailureLeavesOriginalUntouchedAndRetrySucceeds() throws Exception { failedWriteCanResume(true, false); }

    private static void junction(Path link, Path destination) throws Exception {
        if (System.getProperty("os.name").startsWith("Windows")) {
            Process process = new ProcessBuilder("cmd.exe", "/d", "/c", "mklink", "/J", link.toString(), destination.toString())
                    .redirectErrorStream(true).start();
            byte[] output = com.google.common.io.ByteStreams.toByteArray(process.getInputStream());
            assertEquals(new String(output, java.nio.charset.Charset.defaultCharset()), 0, process.waitFor());
        } else Files.createSymbolicLink(link, destination);
    }

    @Test public void backupDirectoryJunctionCannotWriteOutsideMapDirectory() throws Exception {
        Path root = temporary.newFolder().toPath();
        Path outside = temporary.newFolder().toPath();
        Path link = root.resolve("narcissus-waypoint-backups");
        byte[] bytes = json("narcissus_farewell", "HOME_" + ID, "home");
        Files.write(root.resolve("old.json"), bytes);
        junction(link, outside);
        try {
            assertThrows(IOException.class, () -> prepare(root));
            assertArrayEquals(bytes, Files.readAllBytes(root.resolve("old.json")));
            assertFalse(Files.exists(target(root, "HOME")));
            try (java.util.stream.Stream<Path> entries = Files.list(outside)) { assertEquals(0, entries.count()); }
        } finally { Files.delete(link); }
    }

    @Test public void waypointDirectoryJunctionCannotModifyExternalFiles() throws Exception {
        Path parent = temporary.newFolder().toPath();
        Path outside = temporary.newFolder().toPath();
        Path link = parent.resolve("waypoints");
        byte[] bytes = json("narcissus_farewell", "HOME_" + ID, "home");
        Files.write(outside.resolve("old.json"), bytes);
        junction(link, outside);
        try {
            assertThrows(IOException.class, () -> prepare(link));
            assertArrayEquals(bytes, Files.readAllBytes(outside.resolve("old.json")));
            try (java.util.stream.Stream<Path> entries = Files.list(outside)) { assertEquals(1, entries.count()); }
        } finally { Files.delete(link); }
    }

    @Test public void laterCanonicalFailureRetainsAllOriginalsAndEarlierVerifiedCopies() throws Exception {
        Path root = temporary.newFolder().toPath();
        byte[] home = json("narcissus_farewell", "HOME_" + ID, "home");
        byte[] stage = json("narcissus_farewell", "STAGE_" + ID, "home");
        Files.write(root.resolve("1-home.json"), home);
        Files.write(root.resolve("2-stage.json"), stage);
        LegacyJourneyMapFiles.FileAccess access = new LegacyJourneyMapFiles.FileAccess() {
            int canonical;
            @Override void write(Path file, byte[] bytes) throws IOException {
                if (!file.startsWith(root.resolve("narcissus-waypoint-backups")) && ++canonical == 2) {
                    super.write(file, java.util.Arrays.copyOf(bytes, 7));
                    throw new IOException("second canonical failed");
                }
                super.write(file, bytes);
            }
        };
        assertThrows(IOException.class, () -> LegacyJourneyMapFiles.prepare(root, access));
        assertArrayEquals(home, Files.readAllBytes(root.resolve("1-home.json")));
        assertArrayEquals(stage, Files.readAllBytes(root.resolve("2-stage.json")));
        assertArrayEquals(home, Files.readAllBytes(target(root, "HOME")));
        assertFalse(Files.exists(target(root, "STAGE")));
        prepare(root);
        assertArrayEquals(stage, Files.readAllBytes(target(root, "STAGE")));
    }

    @Test public void laterDeleteFailureRetainsVerifiedBackupsAndCanResume() throws Exception {
        Path root = temporary.newFolder().toPath();
        byte[] home = json("narcissus_farewell", "HOME_" + ID, "home");
        byte[] stage = json("narcissus_farewell", "STAGE_" + ID, "home");
        Files.write(root.resolve("1-home.json"), home);
        Files.write(root.resolve("2-stage.json"), stage);
        LegacyJourneyMapFiles.FileAccess access = new LegacyJourneyMapFiles.FileAccess() {
            int deletions;
            @Override void delete(Path file) throws IOException {
                if (++deletions == 2) throw new IOException("second delete failed");
                super.delete(file);
            }
        };
        assertThrows(IOException.class, () -> LegacyJourneyMapFiles.prepare(root, access));
        assertArrayEquals(home, Files.readAllBytes(target(root, "HOME")));
        assertArrayEquals(stage, Files.readAllBytes(target(root, "STAGE")));
        assertArrayEquals(stage, Files.readAllBytes(root.resolve("2-stage.json")));
        try (java.util.stream.Stream<Path> files = Files.walk(root.resolve("narcissus-waypoint-backups"))) {
            assertEquals(2, files.filter(Files::isRegularFile).count());
        }
        prepare(root);
        assertFalse(Files.exists(root.resolve("2-stage.json")));
        assertArrayEquals(home, Files.readAllBytes(target(root, "HOME")));
    }
}
