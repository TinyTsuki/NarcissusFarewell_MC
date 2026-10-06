package xin.vanilla.narcissus.internal.dev;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.banira.common.config.ConfigValueStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

public class NarcissusNetworkSmokeConfigsTest {
    private static final Gson GSON = new Gson();
    @Rule
    public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void snapshotsEveryPathAndRestartsWithCommaExpressionsEnumsAndNumbers() throws IOException {
        Fixture fixture = new Fixture(ConfigScope.COMMON);
        byte[] toml = Files.readAllBytes(fixture.toml);
        fixture.complete("phase-one");
        assertEquals(0, fixture.store.saves);
        assertArrayEquals(toml, Files.readAllBytes(fixture.toml));
        JsonObject snapshot = GSON.fromJson(read(fixture.checkpoint), JsonObject.class);
        JsonObject values = snapshot.getAsJsonObject("values");
        assertEquals(fixture.store.paths().size(), values.size());
        for (String path : fixture.store.paths()) assertTrue(path, values.has(path));
        assertEquals("min(num, max(distance, rate))", values.get("cost.expression").getAsString());
        assertEquals("min(1, 2)", values.getAsJsonArray("cost.list").get(0).getAsString());
        assertEquals("ACTIVE", values.get("base.mode").getAsString());
        assertEquals(0.1D, values.get("cost.rate").getAsDouble(), 0.0D);
        byte[] checkpoint = Files.readAllBytes(fixture.checkpoint);
        Store restarted = fixture.restartStore();
        assertTrue(restarted.get("base.mode") instanceof Mode);
        assertTrue(restarted.get("cost.rate") instanceof Double);
        assertEquals(values.size(), fixture.complete(restarted, "phase-two"));
        assertEquals(0, restarted.saves);
        assertArrayEquals(toml, Files.readAllBytes(fixture.toml));
        assertArrayEquals(checkpoint, Files.readAllBytes(fixture.checkpoint));
    }

    @Test
    public void scopesUseIndependentDirectoriesAndCheckpoints() throws IOException {
        Fixture server = new Fixture(ConfigScope.COMMON);
        Fixture client = new Fixture(ConfigScope.CLIENT);
        server.complete("phase-one");
        client.complete("phase-one");
        assertNotEquals(server.checkpoint, client.checkpoint);
        server.complete(server.restartStore(), "phase-two");
        client.complete(client.restartStore(), "phase-two");
        Files.write(client.checkpoint, Files.readAllBytes(server.checkpoint));
        assertThrows(IllegalStateException.class, () -> client.complete(client.restartStore(), "phase-two"));
    }

    @Test
    public void rejectsEveryMissingSavedTomlFieldBeforeCreatingCheckpoint() throws IOException {
        for (String path : initialValues().keySet()) {
            Fixture fixture = new Fixture(ConfigScope.COMMON);
            CommentedConfig config = new TomlParser().parse(read(fixture.toml));
            config.remove(path);
            write(fixture.toml, new TomlWriter().writeToString(config));
            byte[] damaged = Files.readAllBytes(fixture.toml);
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> fixture.complete("phase-one"));
            assertTrue(failure.getMessage(), failure.getMessage().contains(path));
            assertFalse(Files.exists(fixture.checkpoint));
            assertEquals(0, fixture.store.saves);
            assertArrayEquals(damaged, Files.readAllBytes(fixture.toml));
        }
    }

    @Test
    public void rejectsWrongSavedTomlValueBeforeCreatingCheckpoint() throws IOException {
        Fixture fixture = new Fixture(ConfigScope.COMMON);
        CommentedConfig config = new TomlParser().parse(read(fixture.toml));
        config.set("cost.expression", "changed");
        write(fixture.toml, new TomlWriter().writeToString(config));
        byte[] damaged = Files.readAllBytes(fixture.toml);
        assertThrows(IllegalStateException.class, () -> fixture.complete("phase-one"));
        assertFalse(Files.exists(fixture.checkpoint));
        assertEquals(0, fixture.store.saves);
        assertArrayEquals(damaged, Files.readAllBytes(fixture.toml));
    }

    @Test
    public void rejectsEveryDeletedTomlFieldOnRestartEvenWhenHolderRetainsDefaults() throws IOException {
        for (String path : initialValues().keySet()) {
            Fixture fixture = new Fixture(ConfigScope.COMMON);
            fixture.complete("phase-one");
            CommentedConfig config = new TomlParser().parse(read(fixture.toml));
            config.remove(path);
            write(fixture.toml, new TomlWriter().writeToString(config));
            byte[] damaged = Files.readAllBytes(fixture.toml);
            Store restarted = fixture.restartStore();
            assertThrows(IllegalStateException.class, () -> fixture.complete(restarted, "phase-two"));
            assertEquals(0, restarted.saves);
            assertArrayEquals(damaged, Files.readAllBytes(fixture.toml));
        }
    }

    @Test
    public void rejectsChangedTomlEvenWhenRestartHolderMatchesCheckpoint() throws IOException {
        Fixture fixture = new Fixture(ConfigScope.COMMON);
        fixture.complete("phase-one");
        Store restarted = fixture.restartStore();
        CommentedConfig config = new TomlParser().parse(read(fixture.toml));
        config.set("cost.list", Arrays.asList("min(1", "2)", "minecraft:stone"));
        write(fixture.toml, new TomlWriter().writeToString(config));
        byte[] damaged = Files.readAllBytes(fixture.toml);
        assertThrows(IllegalStateException.class, () -> fixture.complete(restarted, "phase-two"));
        assertArrayEquals(damaged, Files.readAllBytes(fixture.toml));
        assertEquals(0, restarted.saves);
    }

    @Test
    public void rejectsChangedEnumAndNumericRestartValuesEvenWhenTomlAgrees() throws IOException {
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("base.mode", "DISABLED");
        changes.put("base.count", 8);
        changes.put("cost.rate", 0.2D);
        for (Map.Entry<String, Object> change : changes.entrySet()) {
            Fixture fixture = new Fixture(ConfigScope.COMMON);
            fixture.complete("phase-one");
            CommentedConfig config = new TomlParser().parse(read(fixture.toml));
            config.set(change.getKey(), change.getValue());
            write(fixture.toml, new TomlWriter().writeToString(config));
            Store restarted = fixture.restartStore();
            assertThrows(IllegalStateException.class, () -> fixture.complete(restarted, "phase-two"));
            assertEquals(0, restarted.saves);
        }
    }

    @Test
    public void rejectsDeletedOrChangedCheckpointFieldsWithoutRewritingEvidence() throws IOException {
        for (String path : initialValues().keySet()) {
            for (boolean delete : new boolean[]{true, false}) {
                Fixture fixture = new Fixture(ConfigScope.COMMON);
                fixture.complete("phase-one");
                JsonObject snapshot = GSON.fromJson(read(fixture.checkpoint), JsonObject.class);
                if (delete) snapshot.getAsJsonObject("values").remove(path);
                else snapshot.getAsJsonObject("values").addProperty(path, "changed");
                write(fixture.checkpoint, GSON.toJson(snapshot));
                byte[] damaged = Files.readAllBytes(fixture.checkpoint);
                assertThrows(IllegalStateException.class,
                        () -> fixture.complete(fixture.restartStore(), "phase-two"));
                assertArrayEquals(damaged, Files.readAllBytes(fixture.checkpoint));
            }
        }
    }

    @Test
    public void rejectsChangedHolderPathSet() throws IOException {
        for (boolean delete : new boolean[]{true, false}) {
            Fixture fixture = new Fixture(ConfigScope.COMMON);
            fixture.complete("phase-one");
            Store restarted = fixture.restartStore();
            if (delete) restarted.values.remove("base.count");
            else restarted.values.put("base.extra", true);
            assertThrows(IllegalStateException.class, () -> fixture.complete(restarted, "phase-two"));
        }
    }

    @Test
    public void rejectsMissingCheckpointWithoutSavingOrCreatingOne() throws IOException {
        Fixture fixture = new Fixture(ConfigScope.CLIENT);
        assertThrows(IllegalStateException.class, () -> fixture.complete("phase-two"));
        assertFalse(Files.exists(fixture.checkpoint));
        assertEquals(0, fixture.store.saves);
    }

    @Test
    public void rejectsMissingTomlWithoutRepairingIt() throws IOException {
        for (String phase : new String[]{"phase-one", "phase-two"}) {
            Fixture fixture = new Fixture(ConfigScope.CLIENT);
            if ("phase-two".equals(phase)) fixture.complete("phase-one");
            Store values = fixture.restartStore();
            Files.delete(fixture.toml);
            assertThrows(IllegalStateException.class, () -> fixture.complete(values, phase));
            assertFalse(Files.exists(fixture.toml));
            assertEquals(0, values.saves);
        }
    }

    @Test
    public void refusesToReplaceAnExistingPhaseOneCheckpointOrSave() throws IOException {
        Fixture fixture = new Fixture(ConfigScope.COMMON);
        fixture.complete("phase-one");
        byte[] original = Files.readAllBytes(fixture.checkpoint);
        fixture.store.values.put("base.count", 8L);
        assertThrows(IllegalStateException.class, () -> fixture.complete("phase-one"));
        assertEquals(0, fixture.store.saves);
        assertArrayEquals(original, Files.readAllBytes(fixture.checkpoint));
    }

    @Test
    public void rejectsUnknownPhaseOrWrongScopeWithoutSaving() throws IOException {
        Fixture fixture = new Fixture(ConfigScope.CLIENT);
        assertThrows(IllegalStateException.class, () -> fixture.complete("phase-three"));
        assertThrows(IllegalStateException.class, () -> NarcissusNetworkSmokeConfigs.complete(
                fixture.holder(fixture.store), ConfigScope.COMMON, "phase-one", fixture.directory));
        assertEquals(0, fixture.store.saves);
        assertFalse(Files.exists(fixture.checkpoint));
    }

    private final class Fixture {
        private final ConfigScope scope;
        private final String name;
        private final Path directory;
        private final Path toml;
        private final Path checkpoint;
        private final Store store;

        private Fixture(ConfigScope scope) throws IOException {
            this.scope = scope;
            name = "narcissus_farewell-" + (scope == ConfigScope.CLIENT ? "client" : "common");
            directory = temporary.newFolder().toPath().resolve("config");
            Files.createDirectories(directory);
            toml = directory.resolve(name + ".toml");
            checkpoint = directory.resolve(name + ".network-smoke.json");
            store = new Store();
            // Seed persisted evidence independently; verification may never flush the holder.
            CommentedConfig config = CommentedConfig.inMemory();
            store.values.forEach((path, value) -> config.set(path,
                    value instanceof Enum<?> ? ((Enum<?>) value).name() : value));
            write(toml, new TomlWriter().writeToString(config));
        }

        private ConfigHolder holder(Store values) {
            return ConfigHolder.create("narcissus_farewell", name, scope, values,
                    Collections.emptyList(), Collections.emptyMap(), Collections.emptyMap());
        }

        private int complete(String phase) {
            return complete(store, phase);
        }

        private int complete(Store values, String phase) {
            return NarcissusNetworkSmokeConfigs.complete(holder(values), scope, phase, directory);
        }

        private Store restartStore() throws IOException {
            Store restarted = new Store();
            CommentedConfig config = new TomlParser().parse(read(toml));
            for (String path : restarted.paths()) {
                if (config.contains(path)) restarted.values.put(path, config.get(path));
            }
            Object mode = restarted.values.get("base.mode");
            if (mode instanceof String) restarted.values.put("base.mode", Mode.valueOf((String) mode));
            return restarted;
        }
    }

    private static final class Store implements ConfigValueStore {
        private final Map<String, Object> values = initialValues();
        private int saves;

        @Override
        public Set<String> paths() {
            return values.keySet();
        }

        @Override
        public Object get(String path) {
            return values.get(path);
        }

        @Override
        public void set(String path, Object value) {
            values.put(path, value);
        }

        @Override
        public Class<?> valueClass(String path) {
            return values.get(path).getClass();
        }

        @Override
        public Object defaultValue(String path) {
            return initialValues().get(path);
        }

        @Override
        public boolean validate(String path, Object value) {
            return values.containsKey(path);
        }

        @Override
        public void save() {
            saves++;
            throw new AssertionError("Config verification must not save in either phase");
        }
    }

    private enum Mode {
        @SerializedName("not-the-toml-name") ACTIVE, DISABLED;

        @Override
        public String toString() {
            return "not-the-enum-name";
        }
    }

    private static Map<String, Object> initialValues() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("base.enabled", true);
        values.put("base.mode", Mode.ACTIVE);
        values.put("base.count", 7L);
        values.put("cost.rate", 0.1F);
        values.put("cost.expression", "min(num, max(distance, rate))");
        values.put("cost.list", Arrays.asList("min(1, 2)", "minecraft:stone"));
        values.put("cost.empty", Collections.emptyList());
        return values;
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void write(Path path, String text) throws IOException {
        Files.write(path, text.getBytes(StandardCharsets.UTF_8));
    }
}
