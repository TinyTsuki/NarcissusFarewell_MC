package xin.vanilla.narcissus.internal.server.dev;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarcissusNetworkSmokeWorkloadTest {

    @Test
    public void completesAfterTheFixedSearchWindow() {
        NarcissusNetworkSmokeWorkload workload = new NarcissusNetworkSmokeWorkload(80);

        assertFalse(workload.completeAt(399));
        assertTrue(workload.completeAt(400));
    }

}
