package xin.vanilla.narcissus.internal.dev;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import xin.vanilla.banira.api.BaniraConfigs;
import xin.vanilla.banira.common.config.ConfigHolder;
import xin.vanilla.banira.common.config.ConfigScope;
import xin.vanilla.narcissus.config.ClientConfig;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.config.CommonConfigView;
import xin.vanilla.narcissus.config.ClientConfigView;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.TreeSet;

/** Dev-only full holder/TOML persistence proof, with separate local scope checkpoints. */
public final class NarcissusNetworkSmokeConfigs {
    private static final class RetainedViews {
        static final CommonConfigView COMMON = CommonConfigView.get();
        static final ClientConfigView CLIENT = ClientConfigView.get();
    }
    private static final Gson GSON = new GsonBuilder()
            .registerTypeHierarchyAdapter(Enum.class, (JsonSerializer<Enum<?>>)
                    (value, type, context) -> new JsonPrimitive(value.name()))
            .setPrettyPrinting().create();

    private NarcissusNetworkSmokeConfigs() { }

    public static void completeClient(String phase) {
        completeLocal(ClientConfig.class, ConfigScope.CLIENT, phase);
    }

    public static void completeServer(String phase) {
        completeLocal(CommonConfig.class, ConfigScope.COMMON, phase);
    }

    private static void completeLocal(Class<?> configClass, ConfigScope scope, String phase) {
        if (!NarcissusNetworkSmokeStatus.enabled()) throw new IllegalStateException("Smoke disabled");
        ConfigHolder holder = BaniraConfigs.holder(configClass);
        verifyGeneratedReads(holder, scope == ConfigScope.CLIENT ? RetainedViews.CLIENT : RetainedViews.COMMON, "");
        NarcissusNetworkSmokeStatus.append("PASS generated-config-reads scope=" + scope.name()
                + " phase=" + phase + " values=" + holder.getDescriptors().size());
        int count = complete(holder, scope, phase, Paths.get("config").toAbsolutePath());
        NarcissusNetworkSmokeStatus.append(("phase-one".equals(phase)
                ? "PASS complete-config-snapshot" : "PASS complete-config-restart")
                + " scope=" + scope.name() + " config=" + holder.getConfigName() + " values=" + count);
    }

    private static void verifyGeneratedReads(ConfigHolder holder, Object view, String prefix) {
        for (java.lang.reflect.Method method : view.getClass().getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())
                    || java.lang.reflect.Modifier.isStatic(method.getModifiers())
                    || method.getParameterCount() != 0 || method.getName().equals("handle")) continue;
            try {
                Object value = method.invoke(view);
                String path = prefix + method.getName();
                if (method.getReturnType().getEnclosingClass() == view.getClass()) {
                    verifyGeneratedReads(holder, value, path + ".");
                } else {
                    if (!holder.hasValue(path)) throw new IllegalStateException("Unknown generated path: " + path);
                    requireEqual(normalize(holder.get(path)), normalize(value), "generated view " + path);
                }
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Cannot inspect generated config", error);
            }
        }
    }

    static int complete(ConfigHolder holder, ConfigScope scope, String phase, Path directory) {
        if (!"phase-one".equals(phase) && !"phase-two".equals(phase)) {
            throw new IllegalStateException("Unknown config smoke phase: " + phase);
        }
        if (holder == null || holder.getConfigScope() != scope
                || (scope != ConfigScope.COMMON && scope != ConfigScope.CLIENT)) {
            throw new IllegalStateException("Missing or mismatched local config holder for " + scope);
        }
        Path toml = directory.resolve(holder.getConfigName() + ".toml");
        Path checkpoint = directory.resolve(holder.getConfigName() + ".network-smoke.json");
        JsonObject current = snapshot(holder);
        try {
            if ("phase-one".equals(phase)) {
                if (Files.exists(checkpoint)) throw new IllegalStateException("Config checkpoint already exists: " + checkpoint);
                // Inspect persisted evidence without flushing or repairing it first.
                compareToml(current.getAsJsonObject("values"), toml);
                Files.write(checkpoint, GSON.toJson(current).getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            } else {
                // Read both independent witnesses before any disconnect/unload can save defaults over them.
                JsonElement expected = GSON.fromJson(read(checkpoint), JsonElement.class);
                requireEqual(expected, current, "restart holder/checkpoint " + checkpoint);
                compareToml(expected.getAsJsonObject().getAsJsonObject("values"), toml);
            }
        } catch (IOException error) {
            throw new IllegalStateException("Cannot verify complete config " + holder.getConfigName() + " " + phase, error);
        }
        return current.getAsJsonObject("values").size();
    }

    private static JsonObject snapshot(ConfigHolder holder) {
        JsonObject values = new JsonObject();
        for (String path : new TreeSet<>(holder.valuePaths())) {
            Object value = holder.get(path);
            if (value == null) throw new IllegalStateException("Null holder value: " + path);
            values.add(path, normalize(value));
        }
        if (values.size() == 0) throw new IllegalStateException("Empty config holder: " + holder.getConfigName());
        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("schema", 1);
        snapshot.addProperty("configName", holder.getConfigName());
        snapshot.addProperty("scope", holder.getConfigScope().name());
        snapshot.add("values", values);
        return snapshot;
    }

    private static void compareToml(JsonObject values, Path path) throws IOException {
        CommentedConfig config = new TomlParser().parse(read(path));
        for (Map.Entry<String, JsonElement> entry : values.entrySet()) {
            String key = entry.getKey();
            if (!config.contains(key)) throw new IllegalStateException("Missing TOML value " + key + " in " + path);
            Object value = config.get(key);
            requireEqual(entry.getValue(), normalize(value), "TOML value " + key + " in " + path);
        }
    }

    private static JsonElement normalize(Object value) {
        if (value instanceof Iterable<?>) {
            JsonArray array = new JsonArray();
            for (Object item : (Iterable<?>) value) array.add(normalize(item));
            return array;
        }
        // Round-trip numeric text so Float 0.1, TOML Double 0.1 and JSON 0.1 compare identically.
        return GSON.fromJson(GSON.toJson(value), JsonElement.class);
    }

    private static void requireEqual(JsonElement expected, JsonElement actual, String context) {
        if (expected == null || !expected.equals(actual)) {
            throw new IllegalStateException("Complete config mismatch: " + context
                    + " expected=" + expected + " actual=" + actual);
        }
    }

    private static String read(Path path) throws IOException {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
