package xin.vanilla.narcissus;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 锁定 Fabric 1.21.1 分支必须适配的少量真实版本差异。 */
public class Fabric21VersionContractTest {

    @Test
    public void buildAndRuntimeUseFabric21Contracts() throws IOException {
        String build = read("build.gradle");
        String properties = read("gradle.properties");
        String mixins = read("src/main/resources/narcissus_farewell.mixins.json");
        String events = read("src/main/java/xin/vanilla/narcissus/internal/fabric/event/FabricNarcissusGameEventAdapter.java");
        String networkSmoke = read("src/main/java/xin/vanilla/narcissus/internal/client/dev/NarcissusNetworkSmokeClientRunner.java");
        String uiSmoke = read("src/main/java/xin/vanilla/narcissus/internal/client/dev/NarcissusUiSmokeRunner.java");
        String utilities = read("src/main/java/xin/vanilla/narcissus/util/NarcissusUtils.java");
        String metadata = read("src/main/resources/fabric.mod.json");
        String resourcePack = read("src/main/resources/pack.mcmeta");

        assertTrue(build.contains("def javaVer = 21"));
        assertTrue(build.contains("changing = true"));
        assertTrue(build.contains("cacheChangingModulesFor 0, 'seconds'"));
        assertTrue(build.contains("Narcissus UI smoke reported failure"));
        assertTrue(build.contains("Narcissus UI smoke did not finish"));
        assertTrue(properties.contains("minecraft_version=1.21.1"));
        assertTrue(properties.contains("loader_version=0.16.14"));
        assertTrue(properties.contains("fabric_version=0.116.13+1.21.1"));
        assertTrue(properties.contains("modmenu_version=11.0.4"));
        assertTrue(mixins.contains("\"compatibilityLevel\": \"JAVA_21\""));

        // Fabric 1.21.1 继续使用 command API v2 与注册表上下文。
        assertTrue(events.contains("fabric.api.command.v2.CommandRegistrationCallback"));
        assertTrue(events.contains("(dispatcher, registryAccess, environment)"));
        assertFalse(events.contains("fabric.api.command.v1.CommandRegistrationCallback"));

        assertTrue(networkSmoke.contains("ServerAddress.parseString(server.ip)"));
        assertTrue(networkSmoke.contains("ConnectScreen.startConnecting(client.screen, client,"));
        assertTrue(networkSmoke.contains("server, false, null)"));
        assertFalse(networkSmoke.contains("new ConnectScreen("));
        assertTrue(uiSmoke.contains("createWorldOpenFlows().openWorld(options.worldName(),"));
        assertFalse(uiSmoke.contains("createWorldOpenFlows().loadLevel("));
        assertTrue(utilities.contains("ItemUtils.deserializeItemStack(teleportCost.getConf())"));
        assertFalse(utilities.contains("ItemParser.parseForItem("));
        assertTrue(metadata.contains("\"fabric-api\": \"*\""));
        assertFalse(metadata.contains("\"fabric\": \"*\""));
        assertTrue(resourcePack.contains("\"pack_format\": 34"));
    }

    private static String read(String relativePath) throws IOException {
        Path path = Paths.get(relativePath);
        assertTrue("Missing Fabric 1.21 contract file: " + relativePath, Files.isRegularFile(path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
