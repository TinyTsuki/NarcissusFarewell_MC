package xin.vanilla.narcissus.internal.fabric.cost;

import com.electronwill.nightconfig.core.*;
import com.electronwill.nightconfig.toml.*;
import com.google.gson.*;
import xin.vanilla.banira.common.util.JsonUtils;
import xin.vanilla.narcissus.config.migration.*;
import xin.vanilla.narcissus.data.cost.CostConfiguration;
import xin.vanilla.narcissus.internal.server.NarcissusCostRuntime;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;
import java.util.*;
import java.util.function.Consumer;

/** Cold startup transaction, called before Forge registers/corrects the new schema. */
public final class FabricCostMigrationFile {
    private static final int MAX_CONFIG = 8 * 1024 * 1024, MAX_SOURCE = 256 * 1024, MAX_METADATA = 1024 * 1024;
    private static final String JOURNAL = "cost/migration.json";
    public enum Checkpoint { AFTER_BACKUP, AFTER_PREPARED, AFTER_SOURCES, AFTER_CONFIG }
    private final Consumer<Checkpoint> checkpoint;
    private Pending pending;

    public FabricCostMigrationFile() { this(ignored -> { }); }
    FabricCostMigrationFile(Consumer<Checkpoint> checkpoint) { this.checkpoint = Objects.requireNonNull(checkpoint); }

    /** Complete before registering COMMON; failures must prevent Forge from correcting the old file. */
    public synchronized CostConfiguration migrateBeforeRegistration(Path commonFile, Path modRoot) throws IOException {
        Path common = absolute(commonFile), root = absolute(modRoot);
        recover(common, root);
        if (!Files.exists(common, LinkOption.NOFOLLOW_LINKS)) return CostConfiguration.defaults();
        CostMigrationPlan plan = prepare(common, root);
        NarcissusCostRuntime.SourceValidation validation = NarcissusCostRuntime.preflight(
                plan.configuration(), local(root, "cost/sources"), plan.sources());
        commit(plan, validation::verify);
        return plan.configuration();
    }

    public synchronized CostMigrationPlan prepare(Path commonFile, Path modRoot) throws IOException {
        pending = null;
        Path common = absolute(commonFile), root = absolute(modRoot);
        recover(common, root);
        byte[] original = read(common, MAX_CONFIG);
        CostMigrationPlan plan;
        byte[] candidate;
        try {
            plan = CostConfigMigration.plan(plain(new TomlParser().parse(utf8(original))));
            candidate = new TomlWriter().writeToString(config(plan.configurationValues())).getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException error) { throw new IOException("Invalid cost configuration: " + error.getMessage(), error); }
        if (candidate.length > MAX_CONFIG) throw new IOException("Migrated configuration exceeds size limit");
        if (!plan.migrationRequired()) {
            pending = new Pending(plan, common, root, original, null);
            return plan;
        }
        for (Map.Entry<String, String> source : plan.sources().entrySet()) {
            byte[] bytes = source.getValue().getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_SOURCE) throw new IOException("Migrated source exceeds size limit: " + source.getKey());
            verifyCompatible(local(root, "cost/sources/" + source.getKey()), bytes, MAX_SOURCE);
        }
        String backup = "backups/cost-migration/" + UUID.randomUUID();
        writeNew(local(root, backup + "/original.toml"), original);
        writeNew(local(root, backup + "/candidate.toml"), candidate);
        JsonObject journal = new JsonObject(), files = new JsonObject();
        journal.addProperty("version", 1); journal.addProperty("state", "PREPARED");
        journal.addProperty("commonFile", common.toString()); journal.addProperty("backup", backup);
        journal.addProperty("originalHash", hash(original)); journal.addProperty("candidateHash", hash(candidate));
        for (Map.Entry<String, String> source : plan.sources().entrySet()) {
            byte[] bytes = source.getValue().getBytes(StandardCharsets.UTF_8);
            writeNew(local(root, backup + "/sources/" + source.getKey()), bytes);
            files.addProperty(source.getKey(), hash(bytes));
        }
        journal.add("sources", files);
        journal.add("disabledExpressions", JsonUtils.GSON.toJsonTree(plan.disabledExpressions()));
        writeNew(local(root, backup + "/manifest.json"), json(journal));
        checkpoint.accept(Checkpoint.AFTER_BACKUP);
        pending = new Pending(plan, common, root, original, journal);
        return plan;
    }

    public synchronized void commit(CostMigrationPlan plan) throws IOException {
        Pending candidate = pending;
        if (candidate == null || candidate.plan != plan) throw new IllegalStateException("Foreign or consumed cost migration plan");
        NarcissusCostRuntime.SourceValidation validation = NarcissusCostRuntime.preflight(
                plan.configuration(), local(candidate.root, "cost/sources"), plan.sources());
        commit(plan, validation::verify);
    }

    private void commit(CostMigrationPlan plan, SourceCheck validation) throws IOException {
        Pending candidate = pending;
        if (candidate == null || candidate.plan != plan) throw new IllegalStateException("Foreign or consumed cost migration plan");
        pending = null;
        if (!Arrays.equals(candidate.original, read(candidate.common, MAX_CONFIG))) throw new IOException("Common configuration changed during migration");
        if (!plan.migrationRequired()) { validation.verify(); return; }
        verifyEvidence(candidate.root, candidate.journal);
        verifySources(candidate.root, candidate.journal, false);
        Path journal = local(candidate.root, JOURNAL);
        byte[] previous = optional(journal, MAX_METADATA);
        if (previous != null && !metadata(previous, candidate.common).get("state").getAsString().equals("COMMITTED")) {
            throw new IOException("Unresolved cost migration journal");
        }
        replace(journal, previous, json(candidate.journal), MAX_METADATA);
        checkpoint.accept(Checkpoint.AFTER_PREPARED);
        complete(candidate.common, candidate.root, candidate.journal, true, validation);
    }

    public synchronized void recover(Path commonFile, Path modRoot) throws IOException {
        Path common = absolute(commonFile), root = absolute(modRoot);
        byte[] journal = optional(local(root, JOURNAL), MAX_METADATA);
        if (journal == null) return;
        JsonObject metadata = metadata(journal, common);
        verifyEvidence(root, metadata);
        if (metadata.get("state").getAsString().equals("COMMITTED")) {
            // A completed migration may now have manual configuration/source edits. Restore missing files only.
            verifySources(root, metadata, true);
            installSources(root, metadata);
            return;
        }
        byte[] current = read(common, MAX_CONFIG);
        String revision = hash(current);
        if (!revision.equals(metadata.get("originalHash").getAsString()) && !revision.equals(metadata.get("candidateHash").getAsString())) {
            throw new IOException("Common configuration changed during interrupted migration; recovery blocked");
        }
        verifySources(root, metadata, false);
        String backup = metadata.get("backup").getAsString();
        CostMigrationPlan candidate;
        try {
            candidate = CostConfigMigration.plan(plain(new TomlParser().parse(
                    utf8(read(local(root, backup + "/candidate.toml"), MAX_CONFIG)))));
        }
        catch (RuntimeException error) { throw new IOException("Invalid migration recovery candidate", error); }
        Map<String, String> overlays = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> source : metadata.getAsJsonObject("sources").entrySet()) {
            String file = source.getKey();
            overlays.put(file, utf8(read(local(root, backup + "/sources/" + file), MAX_SOURCE)));
        }
        NarcissusCostRuntime.SourceValidation compiled = NarcissusCostRuntime.preflight(
                candidate.configuration(), local(root, "cost/sources"), overlays);
        complete(common, root, metadata, false, compiled::verify);
    }

    private void complete(Path common, Path root, JsonObject metadata, boolean checkpoints, SourceCheck validation) throws IOException {
        verifyEvidence(root, metadata);
        verifySources(root, metadata, false);
        installSources(root, metadata);
        if (checkpoints) checkpoint.accept(Checkpoint.AFTER_SOURCES);
        verifyEvidence(root, metadata);
        verifySources(root, metadata, false);
        validation.verify();
        byte[] current = read(common, MAX_CONFIG);
        String revision = hash(current);
        String backup = metadata.get("backup").getAsString();
        if (revision.equals(metadata.get("originalHash").getAsString())) {
            replace(common, current, read(local(root, backup + "/candidate.toml"), MAX_CONFIG), MAX_CONFIG);
        } else if (!revision.equals(metadata.get("candidateHash").getAsString())) {
            throw new IOException("Common configuration changed before replacement");
        }
        if (checkpoints) checkpoint.accept(Checkpoint.AFTER_CONFIG);
        if (!hash(read(common, MAX_CONFIG)).equals(metadata.get("candidateHash").getAsString())) {
            throw new IOException("Common configuration changed after replacement");
        }
        Path journal = local(root, JOURNAL);
        byte[] previous = read(journal, MAX_METADATA);
        JsonObject expected = metadata(previous, common);
        if (!expected.get("backup").equals(metadata.get("backup"))) throw new IOException("Cost migration journal changed");
        metadata.addProperty("state", "COMMITTED");
        replace(journal, previous, json(metadata), MAX_METADATA);
    }

    private interface SourceCheck { void verify() throws IOException; }

    private static void verifyEvidence(Path root, JsonObject metadata) throws IOException {
        String backup = metadata.get("backup").getAsString();
        if (!hash(read(local(root, backup + "/original.toml"), MAX_CONFIG)).equals(metadata.get("originalHash").getAsString())
                || !hash(read(local(root, backup + "/candidate.toml"), MAX_CONFIG)).equals(metadata.get("candidateHash").getAsString())) {
            throw new IOException("Cost migration backup checksum mismatch");
        }
        for (Map.Entry<String, JsonElement> source : metadata.getAsJsonObject("sources").entrySet()) {
            if (!hash(read(local(root, backup + "/sources/" + source.getKey()), MAX_SOURCE)).equals(source.getValue().getAsString())) {
                throw new IOException("Cost source recovery checksum mismatch: " + source.getKey());
            }
        }
    }

    private static void verifySources(Path root, JsonObject metadata, boolean allowExistingEdits) throws IOException {
        for (Map.Entry<String, JsonElement> source : metadata.getAsJsonObject("sources").entrySet()) {
            byte[] live = optional(local(root, "cost/sources/" + source.getKey()), MAX_SOURCE);
            if (!allowExistingEdits && live != null && !hash(live).equals(source.getValue().getAsString())) {
                throw new IOException("Existing cost source differs: " + source.getKey());
            }
        }
    }

    private static void installSources(Path root, JsonObject metadata) throws IOException {
        for (Map.Entry<String, JsonElement> source : metadata.getAsJsonObject("sources").entrySet()) {
            Path target = local(root, "cost/sources/" + source.getKey());
            if (optional(target, MAX_SOURCE) != null) continue;
            writeNew(target, read(local(root, metadata.get("backup").getAsString() + "/sources/" + source.getKey()), MAX_SOURCE));
        }
    }

    private static JsonObject metadata(byte[] bytes, Path common) throws IOException {
        try {
            JsonObject object = JsonUtils.GSON.fromJson(utf8(bytes), JsonObject.class);
            if (object == null || object.get("version").getAsInt() != 1 || !object.get("commonFile").getAsString().equals(common.toString())
                    || !Arrays.asList("PREPARED", "COMMITTED").contains(object.get("state").getAsString())) throw new IllegalArgumentException();
            String backup = object.get("backup").getAsString(), prefix = "backups/cost-migration/";
            if (!backup.startsWith(prefix) || !UUID.fromString(backup.substring(prefix.length())).toString().equals(backup.substring(prefix.length()))) {
                throw new IllegalArgumentException();
            }
            checksum(object.get("originalHash").getAsString()); checksum(object.get("candidateHash").getAsString());
            if (object.getAsJsonObject("sources").size() > 16) throw new IllegalArgumentException();
            for (Map.Entry<String, JsonElement> source : object.getAsJsonObject("sources").entrySet()) {
                if (!source.getKey().endsWith(".java") || source.getKey().contains("/") || source.getKey().contains("\\")
                        || source.getKey().contains(":") || source.getKey().equals(".java")) throw new IllegalArgumentException();
                checksum(source.getValue().getAsString());
            }
            return object;
        } catch (RuntimeException error) { throw new IOException("Invalid cost migration journal", error); }
    }

    private static void checksum(String value) {
        if (value.length() != 64) throw new IllegalArgumentException();
        for (char c : value.toCharArray()) if (!(c >= '0' && c <= '9' || c >= 'a' && c <= 'f')) throw new IllegalArgumentException();
    }

    private static Path absolute(Path path) throws IOException {
        Path result = path.toAbsolutePath().normalize(); checkAncestors(result); return result;
    }

    private static Path local(Path root, String relative) throws IOException {
        if (relative.isEmpty() || relative.startsWith("/") || relative.contains("\\") || relative.contains(":")) throw new IOException("Invalid cost file path");
        for (String part : relative.split("/", -1)) if (part.isEmpty() || part.equals(".") || part.equals("..")) throw new IOException("Invalid cost file path");
        Path result = root.resolve(relative).normalize();
        if (!result.startsWith(root)) throw new IOException("Cost path leaves mod directory");
        checkAncestors(result); return result;
    }

    private static void checkAncestors(Path path) throws IOException {
        Path current = path.getRoot();
        for (Path part : path) {
            current = current.resolve(part);
            if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) continue;
            BasicFileAttributes attributes = Files.readAttributes(current, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (attributes.isSymbolicLink() || attributes.isOther() || !current.toRealPath().equals(current.toAbsolutePath().normalize())) {
                throw new IOException("Linked cost paths are not allowed: " + current);
            }
        }
    }

    private static byte[] read(Path file, int limit) throws IOException {
        checkAncestors(file);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Missing cost file: " + file);
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            if (channel.size() > limit) throw new IOException("Cost file exceeds size limit: " + file);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); ByteBuffer buffer = ByteBuffer.allocate(8192);
            while (channel.read(buffer) != -1) {
                buffer.flip(); if (bytes.size() + buffer.remaining() > limit) throw new IOException("Cost file grew beyond limit");
                bytes.write(buffer.array(), 0, buffer.remaining()); buffer.clear();
            }
            return bytes.toByteArray();
        }
    }
    private static byte[] optional(Path file, int limit) throws IOException {
        checkAncestors(file);
        return Files.exists(file, LinkOption.NOFOLLOW_LINKS) ? read(file, limit) : null;
    }
    private static void verifyCompatible(Path target, byte[] bytes, int limit) throws IOException {
        byte[] current = optional(target, limit);
        if (current != null && !Arrays.equals(current, bytes)) throw new IOException("Existing cost file differs: " + target);
    }
    private static void writeNew(Path file, byte[] bytes) throws IOException {
        byte[] current = optional(file, MAX_CONFIG);
        if (current != null) { verifyCompatible(file, bytes, MAX_CONFIG); return; }
        write(file, null, bytes, false, MAX_CONFIG);
    }
    private static void replace(Path file, byte[] expected, byte[] bytes, int limit) throws IOException { write(file, expected, bytes, true, limit); }
    private static void write(Path file, byte[] expected, byte[] bytes, boolean replace, int limit) throws IOException {
        if (bytes.length > limit) throw new IOException("Cost write exceeds size limit");
        checkAncestors(file); Files.createDirectories(file.getParent()); checkAncestors(file);
        Path temporary = Files.createTempFile(file.getParent(), ".cost-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes); while (buffer.hasRemaining()) channel.write(buffer); channel.force(true);
            }
            if (!Arrays.equals(expected, optional(file, limit))) throw new IOException("Cost file changed before write: " + file);
            checkAncestors(file);
            if (replace) Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            else Files.move(temporary, file);
            if (!Arrays.equals(bytes, read(file, limit))) throw new IOException("Cost write verification failed");
        } finally { Files.deleteIfExists(temporary); }
    }

    private static String hash(byte[] bytes) {
        try {
            StringBuilder result = new StringBuilder(64);
            for (byte value : MessageDigest.getInstance("SHA-256").digest(bytes)) result.append(String.format(Locale.ROOT, "%02x", value & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static byte[] json(JsonObject object) { return JsonUtils.PRETTY_GSON.toJson(object).getBytes(StandardCharsets.UTF_8); }
    private static String utf8(byte[] bytes) throws IOException {
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }
    private static Map<String, Object> plain(UnmodifiableConfig config) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (UnmodifiableConfig.Entry entry : config.entrySet()) result.put(entry.getKey(), plainValue(entry.getValue()));
        return result;
    }
    private static Object plainValue(Object value) {
        if (value instanceof UnmodifiableConfig) return plain((UnmodifiableConfig) value);
        if (value instanceof List) { List<Object> result = new ArrayList<>(); for (Object item : (List<?>) value) result.add(plainValue(item)); return result; }
        return value;
    }
    private static CommentedConfig config(Map<String, Object> values) {
        CommentedConfig result = CommentedConfig.inMemory();
        values.forEach((key, value) -> result.set(Collections.singletonList(key), configValue(value)));
        return result;
    }
    @SuppressWarnings("unchecked") private static Object configValue(Object value) {
        if (value instanceof Map) return config((Map<String, Object>) value);
        if (value instanceof List) { List<Object> result = new ArrayList<>(); for (Object item : (List<?>) value) result.add(configValue(item)); return result; }
        return value;
    }
    private static final class Pending {
        final CostMigrationPlan plan; final Path common, root; final byte[] original; final JsonObject journal;
        Pending(CostMigrationPlan plan, Path common, Path root, byte[] original, JsonObject journal) {
            this.plan = plan; this.common = common; this.root = root; this.original = original; this.journal = journal;
        }
    }
}
