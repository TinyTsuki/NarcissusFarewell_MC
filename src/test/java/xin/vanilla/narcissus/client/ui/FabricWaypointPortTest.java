package xin.vanilla.narcissus.client.ui;

import com.google.gson.JsonParser;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class FabricWaypointPortTest {
    private ClassNode read(String name) throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/narcissus/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void fabricKeepsItsClientBootstrapAndSharedNativePageRoutes() throws Exception {
        var entry = read("internal/fabric/client/FabricNarcissusClientEntry");
        assertTrue(entry.interfaces.contains("net/fabricmc/api/ClientModInitializer"));
        int bootstraps = 0;
        for (var method : entry.methods) for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call) {
                assertFalse(call.owner, call.owner.startsWith("net/minecraftforge/") || call.owner.startsWith("net/neoforged/"));
                if (call.owner.endsWith("/NarcissusClientBootstrap") && call.name.equals("init")) bootstraps++;
            }
        assertEquals(1, bootstraps);
        for (String screen : new String[]{"WaypointScreen", "AccessListScreen", "PlayerConfigScreen"}) {
            var node = read("screen/" + screen);
            assertTrue(screen, xin.vanilla.banira.internal.client.ui.UiNativePage.class.isAssignableFrom(
                    Class.forName("xin.vanilla.narcissus.screen." + screen, false, getClass().getClassLoader())));
            assertTrue(screen, node.methods.stream().anyMatch(method -> method.name.equals("uiPage")));
        }
        var metadata = JsonParser.parseString(Files.readString(Path.of("src/main/resources/fabric.mod.json"))).getAsJsonObject();
        assertEquals("xin.vanilla.narcissus.integration.map.JourneyMapWaypointPlugin",
                metadata.getAsJsonObject("entrypoints").getAsJsonArray("journeymap").get(0).getAsString());
        assertFalse(metadata.getAsJsonObject("depends").has("apricityui"));
    }

    @Test
    public void onlyTheSharedWaypointRouteRemainsWithoutDirectEngineDependencies() throws Exception {
        assertFalse(Files.exists(Path.of("src/main/java/xin/vanilla/narcissus/integration/ui/ApricityUI.java")));
        assertFalse(Files.exists(Path.of("src/main/resources/assets/apricityui/apricity/narcissus_farewell/index.html")));
        assertFalse(Files.readString(Path.of("build.gradle")).contains("curse.maven:apricityui"));
        assertFalse(Files.readString(Path.of("gradle.properties")).contains("apricityui_id"));
        var helper = read("integration/ScreenHelper");
        int routes = 0;
        for (var method : helper.methods) for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call && method.name.equals("openScreen")) {
                assertFalse(call.owner, call.owner.contains("ApricityUI") || call.owner.contains("ModList"));
                if (call.owner.endsWith("/WaypointScreen") && call.name.equals("<init>")) routes++;
            }
        assertEquals(1, routes);
    }

    @Test
    public void nativeBackgroundAndIconsKeepMinecraft21PublicContracts() throws Exception {
        int backgrounds = 0, factories = 0;
        for (String name : new String[]{"screen/WaypointScreen", "client/ui/WaypointUiSession", "client/ui/AccessListUiSession"})
            for (var method : read(name).methods) for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call) {
                    assertFalse(call.owner, call.owner.startsWith("com/sighs/"));
                    if (call.name.equals("renderBackground")) {
                        assertEquals("(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", call.desc);
                        backgrounds++;
                    }
                    if (call.owner.equals("net/minecraft/resources/ResourceLocation")) {
                        assertNotEquals("<init>", call.name);
                        if (call.name.equals("fromNamespaceAndPath")) factories++;
                    }
                }
        assertEquals(1, backgrounds);
        assertEquals(4, factories);
    }

    @Test
    public void waypointLabelsArePresentInBothLanguages() throws Exception {
        for (String language : new String[]{"en_us", "zh_cn"}) {
            var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/narcissus_farewell/lang/" + language + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
            for (String key : new String[]{"cost_bypass_cooldown", "waypoint_add", "waypoint_cost_label", "waypoint_history_order",
                    "waypoint_order_name", "waypoint_order_time", "waypoint_order_distance"}) {
                assertTrue(key, json.has("word.narcissus_farewell." + key));
                assertFalse(key, json.get("word.narcissus_farewell." + key).getAsString().isBlank());
            }
        }
    }

    @Test
    public void approvedResourcesStayWithinExistingLocalBudgets() throws Exception {
        assertTrue(UiBoundaryChecks.resources(UiBoundaryChecks.ownResources("narcissus_farewell"), true).isEmpty());
    }
}
