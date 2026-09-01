package xin.vanilla.narcissus.internal.server.dev;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NarcissusTeleportSmokeWorkloadTest {

    @Test
    public void requiresThreeSafeRandomTeleportsBeforeAdvancingToViewAndFollowers() {
        NarcissusTeleportSmokeWorkload workload = new NarcissusTeleportSmokeWorkload();

        assertEquals(NarcissusTeleportSmokeWorkload.Step.SAFE_RANDOM, workload.current());
        workload.completeCurrent();
        assertEquals(NarcissusTeleportSmokeWorkload.Step.SAFE_RANDOM, workload.current());
        workload.completeCurrent();
        assertEquals(NarcissusTeleportSmokeWorkload.Step.SAFE_RANDOM, workload.current());
        workload.completeCurrent();
        assertEquals(NarcissusTeleportSmokeWorkload.Step.VIEW_END, workload.current());
        workload.completeCurrent();
        assertEquals(NarcissusTeleportSmokeWorkload.Step.CROSS_DIMENSION_FOLLOWER, workload.current());
        workload.completeCurrent();
        assertEquals(NarcissusTeleportSmokeWorkload.Step.COMPLETE, workload.current());
    }
}
