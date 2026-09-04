package xin.vanilla.narcissus.internal.server.dev;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarcissusNetworkSmokeRuntimePreparationTest {
    @Test
    public void preparesBothChildRuntimesBeforeStartingTimedGameProcesses() throws IOException {
        String script = new String(Files.readAllBytes(Paths.get("gradle", "network-smoke.gradle")), StandardCharsets.UTF_8);

        assertTrue(script.contains("def prepareNetworkSmokeRuntime"));
        assertTrue(script.contains("prepareNetworkSmokeRuntime('runServer'"));
        assertTrue(script.contains("prepareNetworkSmokeRuntime('runClient'"));
        assertTrue(script.contains("startGradleRun('runServer', phase, serverStatus, sparkReport, serverLog, false)"));
        assertTrue(script.contains("startGradleRun('runClient', phase, clientStatus, sparkReport, clientLog, false)"));
        assertFalse(script.contains("startGradleRun('runServer', phase, serverStatus, sparkReport, serverLog, phase == 'phase-one')"));
    }
}
