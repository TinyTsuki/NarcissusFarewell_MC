package xin.vanilla.narcissus.bootstrap;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 约束客户端初始化集中在独立 bootstrap，避免入口类和静态块隐式注册。 */
public class BootstrapStructureTest {

    @Test
    public void clientRegistrationUsesDedicatedBootstrap() throws Exception {
        String entry = read("src/main/java/xin/vanilla/narcissus/NarcissusFarewell.java");
        String bootstrap = read("src/main/java/xin/vanilla/narcissus/client/NarcissusClientBootstrap.java");
        String handler = read("src/main/java/xin/vanilla/narcissus/event/ClientModEventHandler.java");

        assertTrue(entry.contains("NarcissusClientBootstrap.init();"));
        assertFalse(entry.contains("class ClientProxy"));
        assertFalse(entry.contains("QuickActionRegistry"));
        assertTrue(bootstrap.contains("private static boolean initialized"));
        assertTrue(bootstrap.contains("ClientModEventHandler.register();"));
        assertTrue(bootstrap.contains("QuickActionRegistry.get().registerIcon"));
        assertTrue(handler.contains("void register()"));
        assertFalse(handler.contains("static {"));
    }

    private static String read(String relativePath) throws Exception {
        Path path = Paths.get(relativePath);
        assertTrue("Missing bootstrap file: " + relativePath, Files.isRegularFile(path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
