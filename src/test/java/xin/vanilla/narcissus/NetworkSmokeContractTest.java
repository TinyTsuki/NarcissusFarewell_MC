package xin.vanilla.narcissus;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

/** 保证独立服务端/客户端 smoke 的两阶段同步与持久化契约保持完整。 */
public class NetworkSmokeContractTest {
    @Test
    public void dedicatedNetworkSmokeIsFullyWired() throws IOException {
        String build = read("build.gradle");
        String script = read("gradle/network-smoke.gradle");
        String server = read("src/main/java/xin/vanilla/narcissus/internal/server/dev/NarcissusNetworkSmokeServerRunner.java");
        String client = read("src/main/java/xin/vanilla/narcissus/internal/client/dev/NarcissusNetworkSmokeClientRunner.java");
        String main = read("src/main/java/xin/vanilla/narcissus/NarcissusFarewell.java");
        String clientEvents = read("src/main/java/xin/vanilla/narcissus/event/ClientModEventHandler.java");
        String ignore = read(".gitignore");

        assertContains(build, "apply from: 'gradle/network-smoke.gradle'");
        assertContains(script, "networkSmoke");
        assertContains(script, "phase-one");
        assertContains(script, "phase-two");
        assertContains(script, "destroyProcessTree");
        assertContains(script, "PASS server-shutdown");
        assertContains(server, "PASS persisted-player-config");
        assertContains(server, "PASS persisted-access-list");
        assertContains(server, "server.halt(false)");
        assertContains(client, "PlayerConfigSyncToServer");
        assertContains(client, "AccessListEditToServer");
        assertContains(client, "ConnectingScreen");
        assertContains(client, "SERVER_SETTLE_TICKS");
        assertContains(client, "State.SERVER_SETTLE");
        assertContains(client, "PASS persisted-access-list-client");
        assertContains(main, "NarcissusNetworkSmokeServerRunner.register()");
        assertContains(clientEvents, "NarcissusNetworkSmokeClientRunner.register()");
        assertContains(clientEvents, "NarcissusNetworkSmokeClientRunner.tick");
        assertContains(ignore, "/run-network-smoke/");
    }

    private static String read(String relativePath) throws IOException {
        Path path = Paths.get(relativePath);
        assertTrue("Missing network smoke file: " + relativePath, Files.isRegularFile(path));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static void assertContains(String source, String expected) {
        assertTrue("Missing network smoke contract fragment: " + expected, source.contains(expected));
    }
}
