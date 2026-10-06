package xin.vanilla.narcissus.internal.client.dev;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class NarcissusNetworkSmokeScreensTest {
    @Test
    public void localRenderFixtureIsRestoredBeforeReturningToLiveClientData() {
        AtomicReference<String> data = new AtomicReference<>("live-sentinel");
        NarcissusNetworkSmokeScreens.withRenderFixture(data::get, data::set, "local-render-only", () ->
                assertEquals("local-render-only", data.get()));
        assertEquals("live-sentinel", data.get());
    }

    @Test
    public void failedRenderCannotLeaveTheLocalFixtureInLiveClientData() {
        AtomicReference<String> data = new AtomicReference<>("live-sentinel");
        try {
            NarcissusNetworkSmokeScreens.withRenderFixture(data::get, data::set, "local-render-only", () -> {
                throw new IllegalStateException("render failed");
            });
            fail("Expected render failure");
        } catch (IllegalStateException expected) {
            assertEquals("render failed", expected.getMessage());
        }
        assertEquals("live-sentinel", data.get());
    }

    @Test
    public void rejectsFramesThatStartBeforeOrFinishAfterSampling() {
        AtomicBoolean sampling = new AtomicBoolean(true);
        NarcissusNetworkSmokeScreens.Metrics metrics = new NarcissusNetworkSmokeScreens.Metrics("test", sampling::get);
        metrics.beginCycle(1);
        String before = metrics.summary();
        metrics.render(100, 80, true, false);
        assertEquals(before, metrics.summary());
        sampling.set(false);
        metrics.render(100, 80, true, true);
        assertEquals(before, metrics.summary());
        assertFalse(metrics.readyForCycle());
    }

    @Test
    public void countsOnlyRenderedContentAndOnlyOncePerCycle() {
        NarcissusNetworkSmokeScreens.Metrics metrics = new NarcissusNetworkSmokeScreens.Metrics("test", () -> true);
        metrics.beginCycle(1);
        metrics.render(100, 80, false, true);
        assertFalse(metrics.readyForCycle());
        assertEquals(0, metrics.renderedCycles());
        metrics.render(200, 120, true, true);
        metrics.render(300, 160, true, true);
        assertTrue(metrics.readyForCycle());
        assertEquals(1, metrics.renderedCycles());
        assertTrue(metrics.summary().contains("test-render-wall-average-ns=200"));
        assertTrue(metrics.summary().contains("test-render-cpu-average-ns=120"));
        metrics.beginCycle(2);
        assertFalse(metrics.readyForCycle());
        metrics.render(400, -1, true, true);
        assertEquals(2, metrics.renderedCycles());
        assertTrue(metrics.summary().contains("test-render-cpu-samples=3"));
    }

    @Test
    public void expirationCannotCompleteTheTwentiethContentCycle() {
        AtomicBoolean sampling = new AtomicBoolean(true);
        NarcissusNetworkSmokeScreens.Metrics metrics = new NarcissusNetworkSmokeScreens.Metrics("test", sampling::get);
        for (int cycle = 1; cycle < 20; cycle++) {
            metrics.beginCycle(cycle);
            metrics.render(100, -1, true, true);
        }
        metrics.beginCycle(20);
        sampling.set(false);
        metrics.render(100, -1, true, true);
        assertEquals(19, metrics.renderedCycles());
        assertFalse(metrics.readyForCycle());
    }

    @Test
    public void coverageRequiresEveryViewAndTwentyCompletedCycles() {
        assertFalse(NarcissusNetworkSmokeScreens.completeCoverage(19, 0, 0, 1));
        assertFalse(NarcissusNetworkSmokeScreens.completeCoverage(5, 5, 5, 4));
        assertTrue(NarcissusNetworkSmokeScreens.completeCoverage(5, 5, 5, 5));
    }
}
