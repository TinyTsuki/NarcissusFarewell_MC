package xin.vanilla.narcissus;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 锁定 Fabric 1.18.2 分支必须适配的少量真实版本差异。 */
public class Fabric18VersionContractTest {

    @Test
    public void buildAndRuntimeUseFabric18Contracts() throws IOException {
        String build = read("build.gradle");
        String properties = read("gradle.properties");
        String mixins = read("src/main/resources/narcissus_farewell.mixins.json");
        String events = read("src/main/java/xin/vanilla/narcissus/internal/fabric/event/FabricNarcissusGameEventAdapter.java");
        String networkSmoke = read("src/main/java/xin/vanilla/narcissus/internal/client/dev/NarcissusNetworkSmokeClientRunner.java");

        assertTrue(build.contains("def javaVer = 17"));
        assertTrue(build.contains("changing = true"));
        assertTrue(build.contains("cacheChangingModulesFor 0, 'seconds'"));
        assertTrue(properties.contains("minecraft_version=1.18.2"));
        assertTrue(properties.contains("fabric_version=0.77.0+1.18.2"));
        assertTrue(properties.contains("modmenu_version=3.2.5"));
        assertTrue(mixins.contains("\"compatibilityLevel\": \"JAVA_17\""));

        // Fabric 1.18.2 的命令回调仍属于 command API v1。
        assertTrue(events.contains("fabric.api.command.v1.CommandRegistrationCallback"));
        assertFalse(events.contains("fabric.api.command.v2.CommandRegistrationCallback"));

        assertTrue(networkSmoke.contains("ServerAddress.parseString(server.ip)"));
        assertTrue(networkSmoke.contains("ConnectScreen.startConnecting"));
        assertFalse(networkSmoke.contains("new ConnectScreen("));
    }

    private static String read(String relativePath) throws IOException {
        Path path = Paths.get(relativePath);
        assertTrue("Missing Fabric 1.18 contract file: " + relativePath, Files.isRegularFile(path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
