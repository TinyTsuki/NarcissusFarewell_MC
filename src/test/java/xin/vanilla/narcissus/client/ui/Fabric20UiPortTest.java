package xin.vanilla.narcissus.client.ui;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.Assert.*;

public class Fabric20UiPortTest {
    private ClassNode node(String name) throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/narcissus/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    private long calls(ClassNode node, String owner, String name) {
        return node.methods.stream().flatMap(method -> Arrays.stream(method.instructions.toArray()))
                .filter(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.endsWith(owner) && call.name.equals(name)).count();
    }

    @Test
    public void threeOriginalEntryRoutesKeepOneNativeOwnerAndNoVendorDependency() throws Exception {
        var helper = node("integration/ScreenHelper");
        for (String page : new String[]{"WaypointScreen", "AccessListScreen", "PlayerConfigScreen"})
            assertEquals(page, 1, calls(helper, "screen/" + page, "<init>"));
        assertTrue(node("screen/WaypointScreen").interfaces.contains("xin/vanilla/banira/internal/client/ui/UiNativePage"));
        assertTrue(node("screen/AccessListScreen").interfaces.contains("xin/vanilla/banira/internal/client/ui/UiNativePage"));
        for (String type : new String[]{"screen/WaypointScreen", "screen/AccessListScreen", "screen/PlayerConfigScreen",
                "client/ui/NarcissusListUiSession", "client/ui/WaypointUiSession", "client/ui/AccessListUiSession",
                "client/ui/TeleportPreferencesUiSession"})
            for (var method : node(type).methods)
                for (var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode call)
                        assertFalse(type + " -> " + call.owner, call.owner.startsWith("net/minecraftforge/")
                                || call.owner.startsWith("net/neoforged/") || call.owner.startsWith("com/sighs/")
                                || call.owner.startsWith("org/lwjgl/") || call.owner.startsWith("java/awt/"));
    }

    @Test
    public void fabricLifecycleAndPlayerNbtWireKeepTheirOriginalRegistration() throws Exception {
        var bootstrap = node("client/NarcissusClientBootstrap");
        assertEquals(1, calls(bootstrap, "event/ClientModEventHandler", "register"));
        assertEquals(1, calls(bootstrap, "client/NarcissusClientBootstrap", "registerQuickAction"));
        var packet = node("network/packet/PlayerConfigSyncToServer");
        assertEquals(1, calls(packet, "internal/network/NarcissusNbtPacketCodec", "read"));
        assertEquals(1, calls(packet, "internal/network/NarcissusNbtPacketCodec", "write"));
        assertEquals(1, calls(packet, "data/player/PlayerTeleportData", "replaceAllTeleportCountdownsFromTag"));
        assertEquals(1, calls(packet, "data/player/PlayerTeleportData", "syncPlayerData"));
        assertTrue(calls(node("screen/PlayerConfigScreen"), "common/util/PacketUtils", "sendPacketToServer") > 0);
    }

    @Test
    public void sharedSessionsCannotPayOrSendPacketsAndScreensUseConnectionGuards() throws Exception {
        for (String type : new String[]{"NarcissusListUiSession", "WaypointUiSession", "AccessListUiSession",
                "TeleportPreferencesUiSession"})
            for (var method : node("client/ui/" + type).methods)
                for (var instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode call) {
                        assertFalse(call.owner.contains("PacketUtils"));
                        assertFalse(call.owner.contains("CostPaymentService"));
                    }
        for (String type : new String[]{"WaypointScreen", "AccessListScreen", "PlayerConfigScreen"}) {
            var screen = node("screen/" + type);
            assertTrue(type, calls(screen, "screen/" + type, "uiConnectionCurrent") > 0);
        }
        String source = Files.readString(Path.of("src/main/java/xin/vanilla/narcissus/screen/WaypointScreen.java"));
        String deletion = source.substring(source.indexOf("private void doActualDelete"), source.indexOf("public void removed()"));
        assertTrue(deletion.contains("new WaypointDelToServer"));
        assertFalse("Deletion is confirmed by the existing server sync", deletion.contains("removeIf"));
    }


}
