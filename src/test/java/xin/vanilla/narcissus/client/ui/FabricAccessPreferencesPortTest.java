package xin.vanilla.narcissus.client.ui;

import com.google.gson.JsonParser;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class FabricAccessPreferencesPortTest {
    private ClassNode read(String name) throws Exception {
        var node = new ClassNode();
        try (var input = getClass().getResourceAsStream("/xin/vanilla/narcissus/" + name + ".class")) {
            assertNotNull(name, input);
            new ClassReader(input).accept(node, 0);
        }
        return node;
    }

    @Test
    public void accessUsesMinecraft21BackgroundAndPublicSkinContracts() throws Exception {
        int backgrounds = 0, defaults = 0, textures = 0;
        for (String name : new String[]{"screen/AccessListScreen", "client/ui/AccessProfileIcons"})
            for (var method : read(name).methods) for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call) {
                    assertFalse(call.owner, call.owner.startsWith("com/sighs/"));
                    if (call.name.equals("renderBackground")) {
                        assertEquals("(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", call.desc);
                        backgrounds++;
                    }
                    if (call.owner.equals("net/minecraft/resources/ResourceLocation"))
                        assertNotEquals("<init>", call.name);
                    if (call.owner.equals("net/minecraft/client/resources/DefaultPlayerSkin")) {
                        assertEquals("get", call.name);
                        assertEquals("(Ljava/util/UUID;)Lnet/minecraft/client/resources/PlayerSkin;", call.desc);
                        defaults++;
                    }
                    if (call.owner.equals("net/minecraft/client/resources/PlayerSkin") && call.name.equals("texture")) textures++;
                }
        assertEquals(1, backgrounds);
        assertEquals(1, defaults);
        assertEquals(1, textures);
    }

    @Test
    public void accessLabelsArePresentInBothLanguages() throws Exception {
        for (String language : new String[]{"en_us", "zh_cn"}) {
            var json = JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/narcissus_farewell/lang/" + language + ".json"))).getAsJsonObject();
            for (String key : new String[]{"access_auto_tpa", "access_auto_tph", "access_black_priority", "access_edit",
                    "access_no_match", "access_personal", "access_request_auto", "access_request_ignored",
                    "access_request_manual", "access_scope_label", "access_scope_open", "access_scope_white"}) {
                assertTrue(key, json.has("word.narcissus_farewell." + key));
                assertFalse(key, json.get("word.narcissus_farewell." + key).getAsString().isBlank());
            }
        }
    }
}
