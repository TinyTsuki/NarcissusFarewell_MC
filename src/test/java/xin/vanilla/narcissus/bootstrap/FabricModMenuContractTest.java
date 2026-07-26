package xin.vanilla.narcissus.bootstrap;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/**
 * Fabric 的 Mod Menu 设置按钮必须打开水仙辞自己的客户端配置，并保留返回页面。
 */
public class FabricModMenuContractTest {

    @Test
    public void metadataRegistersClientConfigFactory() throws Exception {
        String metadata = read("src/main/resources/fabric.mod.json");
        String integration = read("src/main/java/xin/vanilla/narcissus/internal/fabric/modmenu/NarcissusModMenuIntegration.java");
        String smoke = read("src/main/java/xin/vanilla/narcissus/internal/client/dev/NarcissusUiSmokeRunner.java");

        assertTrue(metadata.contains("\"modmenu\""));
        assertTrue(metadata.contains("NarcissusModMenuIntegration"));
        assertTrue(integration.contains("implements ModMenuApi"));
        assertTrue(integration.contains("ClientConfig.get().holder()"));
        assertTrue(integration.contains(".parentScreen(parent)"));
        assertTrue(smoke.contains("\"modmenu-client-config\""));
        assertTrue(smoke.contains("NarcissusModMenuIntegration"));
        assertTrue(smoke.contains("new ScreenStep(\"modmenu-client-config\", true"));
    }

    @Test
    public void translatorUsesExplicitModIdentity() throws Exception {
        String source = read("src/main/java/xin/vanilla/narcissus/NarcissusLang.java");
        assertTrue(source.contains("super(NarcissusFarewell.MODID, NarcissusLang.class);"));
    }

    private static String read(String relativePath) throws Exception {
        Path path = Paths.get(relativePath);
        assertTrue("Missing Fabric contract file: " + relativePath, Files.isRegularFile(path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
