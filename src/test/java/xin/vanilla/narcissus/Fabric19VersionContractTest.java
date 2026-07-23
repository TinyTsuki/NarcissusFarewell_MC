package xin.vanilla.narcissus;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** 锁定 Fabric 1.19.2 分支必须适配的少量真实版本差异。 */
public class Fabric19VersionContractTest {

    @Test
    public void buildAndRuntimeUseFabric19Contracts() throws IOException {
        String build = read("build.gradle");
        String properties = read("gradle.properties");
        String mixins = read("src/main/resources/narcissus_farewell.mixins.json");
        String events = read("src/main/java/xin/vanilla/narcissus/internal/fabric/event/FabricNarcissusGameEventAdapter.java");
        String networkSmoke = read("src/main/java/xin/vanilla/narcissus/internal/client/dev/NarcissusNetworkSmokeClientRunner.java");
        String uiSmoke = read("src/main/java/xin/vanilla/narcissus/internal/client/dev/NarcissusUiSmokeRunner.java");
        String utilities = read("src/main/java/xin/vanilla/narcissus/util/NarcissusUtils.java");
        String metadata = read("src/main/resources/fabric.mod.json");

        assertTrue(build.contains("def javaVer = 17"));
        assertTrue(build.contains("changing = true"));
        assertTrue(build.contains("cacheChangingModulesFor 0, 'seconds'"));
        assertTrue(build.contains("Narcissus UI smoke reported failure"));
        assertTrue(build.contains("Narcissus UI smoke did not finish"));
        assertTrue(properties.contains("minecraft_version=1.19.2"));
        assertTrue(properties.contains("loader_version=0.14.10"));
        assertTrue(properties.contains("fabric_version=0.77.0+1.19.2"));
        assertTrue(properties.contains("modmenu_version=4.2.0-beta.2"));
        assertTrue(mixins.contains("\"compatibilityLevel\": \"JAVA_17\""));

        // Fabric 1.19.2 的命令回调改为 command API v2，并提供注册表上下文。
        assertTrue(events.contains("fabric.api.command.v2.CommandRegistrationCallback"));
        assertTrue(events.contains("(dispatcher, registryAccess, environment)"));
        assertFalse(events.contains("fabric.api.command.v1.CommandRegistrationCallback"));

        assertTrue(networkSmoke.contains("ServerAddress.parseString(server.ip)"));
        assertTrue(networkSmoke.contains("ConnectScreen.startConnecting"));
        assertFalse(networkSmoke.contains("new ConnectScreen("));
        assertTrue(uiSmoke.contains("createWorldOpenFlows().loadLevel(client.screen, options.worldName())"));
        assertFalse(uiSmoke.contains("client.loadLevel(options.worldName())"));
        assertTrue(utilities.contains("ItemParser.parseForItem("));
        assertTrue(utilities.contains("HolderLookup.forRegistry(Registry.ITEM)"));
        assertTrue(utilities.contains("new ItemInput(parse.item(), parse.nbt())"));
        assertTrue(metadata.contains("\"fabric-api\": \"*\""));
        assertFalse(metadata.contains("\"fabric\": \"*\""));
    }

    private static String read(String relativePath) throws IOException {
        Path path = Paths.get(relativePath);
        assertTrue("Missing Fabric 1.19 contract file: " + relativePath, Files.isRegularFile(path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
