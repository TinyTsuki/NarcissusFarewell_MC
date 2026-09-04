package xin.vanilla.narcissus.internal.server.dev;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarcissusNetworkSmokeClientCoordinationTest {

    @Test
    public void phaseOneClientWaitsForTheServerWorkloadMarker() throws Exception {
        String script = read("gradle/network-smoke.gradle");
        String client = read("src/main/java/xin/vanilla/narcissus/internal/client/dev/"
                + "NarcissusNetworkSmokeClientRunner.java");

        assertTrue(script.contains("narcissus.networkSmoke.serverStatus"));
        assertTrue(client.contains("PASS carpet-fake-player-cleanup"));
        assertTrue(client.contains("serverStatusContains"));
        assertFalse(client.contains("SERVER_SETTLE_TICKS"));
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }
}
