package xin.vanilla.narcissus.internal.server.dev;

import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class NarcissusNetworkSmokeStatusTest {

    @Test
    public void phaseStatusPathsAndSparkPropertyKeepTheInheritedSmokeContract() {
        java.nio.file.Path client = Paths.get("evidence", "client-phase-one.status");
        assertTrue(xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus.serverStatusPath(client)
                .equals(Paths.get("evidence", "server-phase-one.status")));
        String key = xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus.SPARK_REPORT_PROPERTY;
        String previous = System.getProperty(key);
        try {
            System.setProperty(key, " report.sparkprofile ");
            assertTrue("report.sparkprofile".equals(
                    xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus.sparkReport()));
        } finally {
            if (previous == null) System.clearProperty(key);
            else System.setProperty(key, previous);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void statusPathRejectsNonClientEvidence() {
        xin.vanilla.narcissus.internal.dev.NarcissusNetworkSmokeStatus.serverStatusPath(
                Paths.get("evidence", "server-phase-one.status"));
    }

}
