package xin.vanilla.narcissus.integration.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Pattern;

public final class LegacyJourneyMapFiles {
    private static final Pattern OWNED_ID = Pattern.compile("(?:HOME|STAGE|WAYPOINT)_[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    private static final int MAX_FILE_BYTES = 1024 * 1024;
    private static final LinkOption[] NO_LINKS = {LinkOption.NOFOLLOW_LINKS};

    private LegacyJourneyMapFiles() {
    }

    public static String fileName(String owner, String id) {
        return "narcissus_farewell".equals(owner) && id != null && OWNED_ID.matcher(id).matches()
                ? "narcissus_farewell_" + id + ".json" : null;
    }

    public static synchronized void prepare(Path directory) throws IOException {
        prepare(directory, new FileAccess());
    }

    public static void requireNativeFileIdentity(Path source, String nativeFileName) throws IOException {
        if (!source.getFileName().toString().equals(nativeFileName))
            throw new IOException("Unproven waypoint file migration blocked: " + source);
    }

    static void prepare(Path directory, FileAccess access) throws IOException {
        Path root = directory.toAbsolutePath().normalize();
        checkAncestors(root);
        if (!Files.exists(root, NO_LINKS)) return;
        if (!Files.isDirectory(root, NO_LINKS)) throw new IOException("Waypoint directory is not a directory");
        List<Migration> pending = new ArrayList<>();
        Map<Path, byte[]> targets = new HashMap<>();
        try (java.util.stream.Stream<Path> files = Files.list(root)) {
            for (Path source : files.sorted(Comparator.comparing(Path::toString)).collect(java.util.stream.Collectors.toList())) {
                if (!source.getFileName().toString().endsWith(".json")) continue;
                if (Files.isSymbolicLink(source)) throw new IOException("Linked waypoint file: " + source);
                if (!Files.isRegularFile(source, NO_LINKS)) continue;
                byte[] bytes = read(source);
                String name = ownedName(bytes);
                if (name == null) continue;
                Path target = root.resolve(name);
                byte[] previous = targets.putIfAbsent(target, bytes);
                if (previous != null && !Arrays.equals(previous, bytes))
                    throw new IOException("Conflicting owned waypoint: " + target);
                if (source.equals(target)) continue;
                if (Files.exists(target, NO_LINKS) && !Arrays.equals(bytes, read(target)))
                    throw new IOException("Waypoint destination already exists: " + target);
                pending.add(new Migration(source, target, bytes));
            }
        }
        if (pending.isEmpty()) return;

        Path backups = root.resolve("narcissus-waypoint-backups");
        checkAncestors(backups);
        Files.createDirectories(backups);
        Path batch = Files.createDirectory(backups.resolve(UUID.randomUUID().toString()));
        // Preserve every input before the native reader can normalize IDs or remove old files.
        for (Migration item : pending) {
            requireUnchanged(item.source, item.bytes);
            Path backup = batch.resolve(item.source.getFileName());
            writeNew(backup, item.bytes, access);
            requireUnchanged(backup, item.bytes);
        }
        for (Migration item : pending) {
            requireUnchanged(item.source, item.bytes);
            checkAncestors(item.target);
            if (!Files.exists(item.target, NO_LINKS)) writeNew(item.target, item.bytes, access);
            requireUnchanged(item.target, item.bytes);
        }
        // All canonical files and backups are verified before any original is removed.
        for (Migration item : pending) {
            requireUnchanged(item.target, item.bytes);
            requireUnchanged(item.source, item.bytes);
            access.delete(item.source);
        }
    }

    private static String ownedName(byte[] bytes) {
        try {
            JsonElement parsed = new JsonParser().parse(new String(bytes, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) return null;
            JsonObject object = parsed.getAsJsonObject();
            return fileName(string(object, "origin"), string(object, "displayId"));
        } catch (RuntimeException malformed) {
            return null;
        }
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : null;
    }

    private static byte[] read(Path file) throws IOException {
        checkAncestors(file);
        if (!Files.isRegularFile(file, NO_LINKS) || Files.size(file) > MAX_FILE_BYTES)
            throw new IOException("Invalid waypoint file: " + file);
        byte[] bytes = Files.readAllBytes(file);
        if (bytes.length > MAX_FILE_BYTES) throw new IOException("Waypoint file too large: " + file);
        return bytes;
    }

    private static void requireUnchanged(Path file, byte[] bytes) throws IOException {
        if (!Arrays.equals(bytes, read(file))) throw new IOException("Waypoint file changed: " + file);
    }

    private static void checkAncestors(Path file) throws IOException {
        for (Path path = file; path != null; path = path.getParent()) {
            if (Files.isSymbolicLink(path)) throw new IOException("Linked waypoint path: " + path);
            if (Files.exists(path, NO_LINKS)) {
                // Resolving reparse points also detects Windows junctions, which are not symbolic links.
                if (!path.toRealPath().equals(path.toRealPath(NO_LINKS)))
                    throw new IOException("Redirected waypoint path: " + path);
                return;
            }
        }
    }

    private static void writeNew(Path file, byte[] bytes, FileAccess access) throws IOException {
        checkAncestors(file);
        Path temporary = file.resolveSibling(".narcissus-" + UUID.randomUUID() + ".tmp");
        try {
            access.write(temporary, bytes);
            requireUnchanged(temporary, bytes);
            checkAncestors(file);
            Files.move(temporary, file);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static class FileAccess {
        void write(Path file, byte[] bytes) throws IOException {
            try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE,
                    LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
        }

        void delete(Path file) throws IOException {
            Files.delete(file);
        }
    }

    private static final class Migration {
        final Path source, target;
        final byte[] bytes;

        Migration(Path source, Path target, byte[] bytes) {
            this.source = source;
            this.target = target;
            this.bytes = bytes;
        }
    }
}
