package xin.vanilla.narcissus.internal.client.dev;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarcissusNetworkSmokeClientPlanTest {
    @Test(expected = IllegalStateException.class)
    public void missingServerIntervalCannotProveOverlap() {
        plan("phase-one").requireSampleInterval(1000, 21000);
    }

    @Test
    public void clientIntervalMustBeEntirelyInsideTheServerSample() {
        NarcissusNetworkSmokeClientPlan plan = plan("phase-one");
        plan.accept(Collections.singletonList("PASS server-sampling-window start-ms=1000 end-ms=61000"));
        plan.requireSampleInterval(1000, 21000);
        plan.requireSampleInterval(41000, 61000);
        rejectInterval(plan, 999, 21000);
        rejectInterval(plan, 41001, 61001);
        rejectInterval(plan, 21000, 21000);
    }

    private static void rejectInterval(NarcissusNetworkSmokeClientPlan plan, long start, long end) {
        try {
            plan.requireSampleInterval(start, end);
            org.junit.Assert.fail("Invalid sample interval accepted");
        } catch (IllegalStateException expected) { }
    }

    @Test
    public void waitsForTheCustomChannelToSettleBeforeSendingClientPackets() {
        assertFalse(NarcissusNetworkSmokeClientPlan.isCustomChannelReady(0, true));
        assertFalse(NarcissusNetworkSmokeClientPlan.isCustomChannelReady(19, true));
        assertFalse(NarcissusNetworkSmokeClientPlan.isCustomChannelReady(20, false));
        assertTrue(NarcissusNetworkSmokeClientPlan.isCustomChannelReady(20, true));
    }

    @Test
    public void acceptsOnlyExactReadinessAndCurrentPhaseMarkers() {
        NarcissusNetworkSmokeClientPlan plan = plan("phase-one");
        plan.accept(Arrays.asList("PASS sustained-ready-extra", "PASS final-checkpoint-extra",
                "FINISHED phase-one-extra", "FINISHED phase-two"));
        assertFalse(plan.readyForUi());
        assertFalse(plan.canFinish(true, true));
        plan.accept(Collections.singletonList("PASS sustained-ready"));
        assertTrue(plan.readyForUi());
        plan.accept(Collections.<String>emptyList());
        assertTrue(plan.readyForUi());
        plan.accept(Arrays.asList("PASS final-checkpoint homes=48", "FINISHED phase-one"));
        assertTrue(plan.canFinish(true, true));
    }

    @Test
    public void phaseOneCannotExitUntilUiAndRawReportAndServerAreComplete() {
        NarcissusNetworkSmokeClientPlan plan = plan("phase-one");
        plan.accept(Collections.singletonList("PASS final-checkpoint"));
        assertFalse(plan.canFinish(true, true));
        plan.accept(Collections.singletonList("FINISHED phase-one"));
        assertFalse(plan.canFinish(false, true));
        assertFalse(plan.canFinish(true, false));
        assertTrue(plan.canFinish(true, true));
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsFinishedPhaseOneWithoutIndependentCheckpoint() {
        NarcissusNetworkSmokeClientPlan plan = plan("phase-one");
        plan.accept(Collections.singletonList("FINISHED phase-one"));
        plan.canFinish(true, true);
    }

    @Test(expected = IllegalStateException.class)
    public void rejectsServerFinishBeforeUiSampling() {
        NarcissusNetworkSmokeClientPlan plan = plan("phase-one");
        plan.accept(Arrays.asList("PASS sustained-ready", "FINISHED phase-one"));
        plan.readyForUi();
    }

    @Test(expected = IllegalStateException.class)
    public void propagatesServerFailureEvenAfterCompletion() {
        NarcissusNetworkSmokeClientPlan plan = plan("phase-one");
        plan.accept(Arrays.asList("FINISHED phase-one", "FAIL server workload expired"));
    }

    @Test
    public void phaseTwoWaitsForItsOwnServerButDoesNotRequireUiSampling() {
        NarcissusNetworkSmokeClientPlan plan = plan("phase-two");
        plan.accept(Arrays.asList("PASS sustained-ready", "FINISHED phase-one"));
        assertFalse(plan.readyForUi());
        assertFalse(plan.canFinish(false, false));
        plan.accept(Collections.singletonList("FINISHED phase-two"));
        assertTrue(plan.canFinish(false, false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownPhaseBeforeConnecting() {
        plan("phase-three");
    }

    @Test
    public void stateDeadlineUsesElapsedWallTimeAndResetsOnTransition() {
        NarcissusNetworkSmokeClientPlan plan = plan("phase-one");
        assertFalse(plan.timedOut(atSeconds(119)));
        assertTrue(plan.timedOut(atSeconds(120)));
        plan.transition(atSeconds(119));
        assertFalse(plan.timedOut(atSeconds(120)));
        assertFalse(plan.timedOut(atSeconds(238)));
        assertTrue(plan.timedOut(atSeconds(239)));
    }

    @Test
    public void transitionsCannotExtendTheGlobalWallDeadline() {
        NarcissusNetworkSmokeClientPlan plan = plan("phase-one");
        plan.transition(atSeconds(100));
        plan.transition(atSeconds(200));
        plan.transition(atSeconds(299));
        assertFalse(plan.timedOut(atSeconds(299)));
        assertTrue(plan.timedOut(atSeconds(300)));
    }

    @Test
    public void samplingCannotFinishWithoutRawReportAndFullDuration() {
        assertFalse(NarcissusNetworkSmokeClientPlan.uiComplete(false, false, seconds(20), true));
        assertFalse(NarcissusNetworkSmokeClientPlan.uiComplete(true, false, seconds(20), true));
        assertFalse(NarcissusNetworkSmokeClientPlan.uiComplete(true, true, seconds(20) - 1, true));
        assertTrue(NarcissusNetworkSmokeClientPlan.uiComplete(true, true, seconds(20), true));
    }

    @Test(expected = IllegalStateException.class)
    public void expiredSampleCannotWaitForMoreContentCycles() {
        NarcissusNetworkSmokeClientPlan.uiComplete(true, true, seconds(20), false);
    }

    private static NarcissusNetworkSmokeClientPlan plan(String phase) {
        return new NarcissusNetworkSmokeClientPlan(phase, 1000L);
    }

    private static long atSeconds(long seconds) {
        return 1000L + seconds(seconds);
    }

    private static long seconds(long seconds) {
        return TimeUnit.SECONDS.toNanos(seconds);
    }
}
