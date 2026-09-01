package xin.vanilla.narcissus.internal.dev;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;

public class NarcissusNetworkSmokeStatusTest {

    @Test
    public void resolvesTheSiblingServerStatusForTheCurrentPhase() {
        Path clientStatus = Paths.get("run-network-smoke", "evidence", "sample", "client-phase-one.status");

        assertEquals(Paths.get("run-network-smoke", "evidence", "sample", "server-phase-one.status"),
                NarcissusNetworkSmokeStatus.serverStatusPath(clientStatus));
    }
}
