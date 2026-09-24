package xin.vanilla.narcissus.config;

import org.junit.Test;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Arrays;
import xin.vanilla.narcissus.enums.EnumCostType;
import static org.junit.Assert.*;

public class ConfigViewBaselineTest {
    @Test
    public void nestedAliasesAndCostConversionKeepTheirMeaning() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(CommonConfig.class);
        fixture.bind(CommonConfig.class);
        CommonConfigView view = CommonConfigView.get();
        fixture.values.put("command.tpAsk.commandTpAsk", "");
        assertEquals("tpa", view.command().tpAsk().commandTpAsk());
        assertEquals("tpa", view.command().tpAsk().commandTpAsk());
        fixture.values.put("command.tpAsk.commandTpAsk", "ask,one");
        assertEquals("ask,one", view.command().tpAsk().commandTpAsk());
        fixture.values.put("cost.tpAsk.costTpAskType", "invalid-cost");
        assertEquals(EnumCostType.NONE, view.cost().tpAsk().costTpAskType());
        view.cost().tpAsk().costTpAskRate(0.002D);
        assertEquals(0.002D, view.cost().tpAsk().costTpAskRate(), 0.0D);
        fixture.values.put("base.other.tpSound", "");
        assertEquals("minecraft:entity.enderman.teleport", view.base().other().tpSound());
        view.base().safeTeleport().unsafeBlocks(Arrays.asList("tag, clazz -> tag != null", "minecraft:lava"));
        assertEquals(2, view.base().safeTeleport().unsafeBlocks().size());
        assertEquals(0, fixture.saves);
        view.handle().save();
        assertEquals(1, fixture.saves);
    }

    @Test
    public void clientNullFallsBackAndWriteRequiresExplicitSave() throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(ClientConfig.class);
        fixture.bind(ClientConfig.class);
        ClientConfigView view = ClientConfigView.get();
        fixture.values.put("client.syncHomeMapWaypoint", null);
        assertTrue(view.client().syncHomeMapWaypoint());
        view.client().syncHomeMapWaypoint(false);
        assertFalse(view.client().syncHomeMapWaypoint());
        assertEquals(0, fixture.saves);
        view.handle().save();
        assertEquals(1, fixture.saves);
    }

    @Test public void commonPathsDefaultsAndReadsRemainEquivalent() throws Exception {
        ConfigBaselineFixture.bind(CommonConfig.class, null);
        compare(CommonConfig.class, CommonConfigView.get(), "common");
    }

    @Test public void clientPathsDefaultsAndReadsRemainEquivalent() throws Exception {
        ConfigBaselineFixture.bind(ClientConfig.class, null);
        compare(ClientConfig.class, ClientConfigView.get(), "client");
    }

    private static void compare(Class<?> config, Object view, String name) throws Exception {
        ConfigBaselineFixture fixture = new ConfigBaselineFixture(config);
        com.google.gson.JsonObject expected;
        try (java.io.Reader reader = new java.io.InputStreamReader(
                java.util.Objects.requireNonNull(ConfigViewBaselineTest.class.getResourceAsStream(
                        "/config-view-baseline/" + name + ".json")), java.nio.charset.StandardCharsets.UTF_8)) {
            expected = new com.google.gson.Gson().fromJson(reader, com.google.gson.JsonObject.class);
        }
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().serializeNulls().create();
        assertEquals(expected.get("schema"), gson.toJsonTree(fixture.schema()));
        for (String phase : Arrays.asList("unbound", "defaults", "changed")) {
            if (phase.equals("changed")) fixture.nonDefaultValues();
            ConfigBaselineFixture.bind(config, phase.equals("unbound") ? null : fixture.holder);
            Map<String, Object> current = ConfigBaselineFixture.readView(view, view.getClass());
            com.google.gson.JsonObject actual = gson.toJsonTree(current).getAsJsonObject();
            com.google.gson.JsonObject canonical = new com.google.gson.JsonObject();
            for (Map.Entry<String, com.google.gson.JsonElement> entry : expected.getAsJsonObject(phase).entrySet()) {
                String path = registeredPath(entry.getKey(), fixture.defaults.keySet());
                if (canonical.has(path)) assertEquals(canonical.get(path), entry.getValue());
                canonical.add(path, entry.getValue());
            }
            // The old proxy duplicated the teleportCard category and never read these three stored values.
            // Keep that historical evidence intact; verify the corrected reads separately against the backend.
            if (name.equals("common") && phase.equals("changed")) {
                for (String leaf : Arrays.asList("teleportCard", "teleportCardDaily", "teleportCardType")) {
                    String path = "base.teleportCard." + leaf;
                    assertEquals(gson.toJsonTree(fixture.defaults.get(path)), canonical.remove(path));
                    assertEquals(gson.toJsonTree(fixture.values.get(path)), actual.remove(path));
                }
            }
            assertEquals(name + ":" + phase, canonical, actual);
        }
    }

    private static String registeredPath(String oldPath, java.util.Set<String> registered) {
        if (registered.contains(oldPath)) return oldPath;
        String[] parts = oldPath.split("\\.");
        if (parts[0].equals("cost")) {
            String group = parts[1];
            return "cost." + group + ".cost" + Character.toUpperCase(group.charAt(0))
                    + group.substring(1) + Character.toUpperCase(parts[2].charAt(0)) + parts[2].substring(1);
        }
        String leaf = parts[parts.length - 1];
        java.util.List<String> matches = new java.util.ArrayList<>();
        for (String path : registered) {
            if (path.startsWith(parts[0] + ".") && path.endsWith("." + leaf)) matches.add(path);
        }
        assertEquals("Unambiguous legacy alias: " + oldPath, 1, matches.size());
        return matches.get(0);
    }
}
