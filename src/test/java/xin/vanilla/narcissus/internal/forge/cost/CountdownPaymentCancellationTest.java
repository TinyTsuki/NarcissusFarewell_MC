package xin.vanilla.narcissus.internal.forge.cost;

import org.junit.*;
import xin.vanilla.narcissus.util.TeleportCountdownTracker;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class CountdownPaymentCancellationTest {
    private final ForgeCostPlayerFixture fixture = new ForgeCostPlayerFixture();
    @Before public void setup() throws Exception { fixture.setup(); }
    @After public void cleanup() throws Exception { TeleportCountdownTracker.clear(); fixture.cleanup(); }

    @Test public void replacingCountdownReleasesOldPaymentExactlyOnce() {
        AtomicInteger cancelled = new AtomicInteger();
        TeleportCountdownTracker.Session old = TeleportCountdownTracker.begin(fixture.player(), false, false, cancelled::incrementAndGet);
        TeleportCountdownTracker.Session replacement = TeleportCountdownTracker.begin(fixture.player(), false, false, () -> { });
        assertEquals(1, cancelled.get());
        assertTrue(old.isCancelled());
        assertFalse(old.tryMarkCompleteAndRemove());
        assertTrue(replacement.tryMarkCompleteAndRemove());
        assertFalse(replacement.tryMarkCompleteAndRemove());
        assertEquals(1, cancelled.get());
    }
    @Test public void shutdownAndLogoutReleasePendingPaymentsOnlyOnce() {
        AtomicInteger cancelled = new AtomicInteger();
        TeleportCountdownTracker.Session session = TeleportCountdownTracker.begin(fixture.player(), false, false, cancelled::incrementAndGet);
        TeleportCountdownTracker.clear();
        TeleportCountdownTracker.onPlayerLogout(fixture.player().getUUID());
        assertTrue(session.isCancelled());
        assertEquals(1, cancelled.get());
        assertFalse(session.tryMarkCompleteAndRemove());
    }
    @Test public void successfulCompletionDoesNotCancelPayment() {
        AtomicInteger cancelled = new AtomicInteger();
        TeleportCountdownTracker.Session session = TeleportCountdownTracker.begin(fixture.player(), false, false, cancelled::incrementAndGet);
        assertTrue(session.tryMarkCompleteAndRemove());
        TeleportCountdownTracker.clear();
        assertEquals(0, cancelled.get());
    }
    @Test public void cancellingSpecificCountdownDoesNotRemoveReplacement() {
        AtomicInteger cancelled = new AtomicInteger();
        TeleportCountdownTracker.Session old = TeleportCountdownTracker.begin(fixture.player(), false, false, cancelled::incrementAndGet);
        TeleportCountdownTracker.Session replacement = TeleportCountdownTracker.begin(fixture.player(), false, false, () -> { });
        old.cancel();
        assertEquals(1, cancelled.get());
        assertTrue(replacement.tryMarkCompleteAndRemove());
    }
    @Test public void releasingCompletedCountdownDoesNotInvokeCancellation() {
        AtomicInteger cancelled = new AtomicInteger();
        TeleportCountdownTracker.Session session = TeleportCountdownTracker.begin(fixture.player(), false, false, cancelled::incrementAndGet);
        assertTrue(session.tryMarkCompleteAndRemove());
        session.cancel();
        assertEquals(0, cancelled.get());
    }
}
