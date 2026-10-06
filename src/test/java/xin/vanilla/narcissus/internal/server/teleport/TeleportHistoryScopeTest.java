package xin.vanilla.narcissus.internal.server.teleport;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class TeleportHistoryScopeTest {
    @Test
    public void trackingNeverSuppressesNativeEventsOutsideOwnedCall() {
        Object player = new Object();
        try (TeleportHistoryScope tracker = TeleportHistoryScope.track(player)) {
            assertFalse(TeleportHistoryScope.owns(player, "nether", 4, 70.1, 8));
            try (TeleportHistoryScope nativeMove = TeleportHistoryScope.open(player, "nether", 4, 70.1, 8)) {
                assertTrue(TeleportHistoryScope.owns(player, "nether", 4, 70.1, 8));
            }
            assertFalse(TeleportHistoryScope.owns(player, "nether", 4, 70.1, 8));
        }
    }

    @Test
    public void nestedCommitAfterCloseMarksMatchingActiveParents() {
        Object player = new Object();
        try (TeleportHistoryScope tracker = TeleportHistoryScope.track(player)) {
            TeleportHistoryScope outer = TeleportHistoryScope.open(player, "overworld", 4, 70.1, 8);
            try (TeleportHistoryScope ignored = outer) {
                try (TeleportHistoryScope nested = TeleportHistoryScope.open(player, "nether", 9, 64, 9)) {
                    assertFalse(outer.hasRecordedActualEnd(player, "nether", 9, 64, 9));
                }
                TeleportHistoryScope.noteRecorded(player, "nether", 9, 64, 9);
                assertTrue(outer.hasRecordedActualEnd(player, "nether", 9, 64, 9));
                assertTrue(tracker.hasRecordedActualEnd(player, "nether", 9, 64, 9));
            }
            assertTrue(outer.hasRecordedActualEnd(player, "nether", 9, 64, 9));
        }
    }

    @Test
    public void anotherActorCannotBorrowRecordedEndpoint() {
        Object player = new Object(), passenger = new Object();
        try (TeleportHistoryScope tracker = TeleportHistoryScope.track(player)) {
            TeleportHistoryScope.noteRecorded(passenger, "nether", 4, 70.1, 8);
            assertFalse(tracker.hasRecordedActualEnd(player, "nether", 4, 70.1, 8));
            assertFalse(tracker.hasRecordedActualEnd(passenger, "nether", 4, 70.1, 8));
        }
    }

    @Test
    public void lastCheckpointMustMatchExactCurrentEnd() {
        Object player = new Object();
        try (TeleportHistoryScope tracker = TeleportHistoryScope.track(player)) {
            TeleportHistoryScope.noteRecorded(player, "nether", 4, 70.1, 8);
            assertTrue(tracker.hasRecordedActualEnd(player, "nether", 4, 70.1, 8));
            assertFalse(tracker.hasRecordedActualEnd(player, "overworld", 4, 70.1, 8));
            assertFalse(tracker.hasRecordedActualEnd(player, "nether", 5, 70.1, 8));
            assertFalse(tracker.hasRecordedActualEnd(player, "nether", 4, 70, 8));
            assertFalse(tracker.hasRecordedActualEnd(player, "nether", 4, 70.1, 9));
            TeleportHistoryScope.noteRecorded(player, "overworld", 9, 64, 9);
            assertFalse(tracker.hasRecordedActualEnd(player, "nether", 4, 70.1, 8));
            assertTrue(tracker.hasRecordedActualEnd(player, "overworld", 9, 64, 9));
        }
    }

    @Test
    public void recordingInOtherActorScopeMarksOnlyMatchingParent() {
        Object player = new Object(), passenger = new Object();
        try (TeleportHistoryScope tracker = TeleportHistoryScope.track(player)) {
            try (TeleportHistoryScope other = TeleportHistoryScope.open(passenger, "nether", 4, 70.1, 8)) {
                TeleportHistoryScope.noteRecorded(player, "nether", 4, 70.1, 8);
                assertTrue(tracker.hasRecordedActualEnd(player, "nether", 4, 70.1, 8));
                assertFalse(other.hasRecordedActualEnd(passenger, "nether", 4, 70.1, 8));
            }
        }
    }

    @Test
    public void retainedClosedCheckpointCannotLeakIntoNextTransfer() {
        Object player = new Object();
        TeleportHistoryScope completed = TeleportHistoryScope.track(player);
        TeleportHistoryScope.noteRecorded(player, "nether", 4, 70.1, 8);
        completed.close();
        assertTrue(completed.hasRecordedActualEnd(player, "nether", 4, 70.1, 8));
        try (TeleportHistoryScope next = TeleportHistoryScope.track(player)) {
            assertFalse(next.hasRecordedActualEnd(player, "nether", 4, 70.1, 8));
        }
    }

    @Test
    public void ownershipRequiresSameActorAndExactTarget() {
        Object player = new Object();
        try (TeleportHistoryScope ignored = TeleportHistoryScope.open(player, "overworld", 4, 70.1, 8)) {
            assertTrue(TeleportHistoryScope.owns(player, "overworld", 4, 70.1, 8));
            assertFalse(TeleportHistoryScope.owns(new Object(), "overworld", 4, 70.1, 8));
            assertFalse(TeleportHistoryScope.owns(player, "overworld", 5, 70.1, 8));
            assertFalse(TeleportHistoryScope.owns(player, "overworld", 4, 70, 8));
            assertFalse(TeleportHistoryScope.owns(player, "overworld", 4, 70.1, 9));
        }
        assertFalse(TeleportHistoryScope.owns(player, null, 4, 70.1, 8));
    }

    @Test
    public void fabricDimensionRejectsEqualCoordinatesInAnotherWorld() {
        Object player = new Object();
        try (TeleportHistoryScope ignored = TeleportHistoryScope.open(player, "nether", 4, 70.1, 8)) {
            assertFalse(TeleportHistoryScope.owns(player, "overworld", 4, 70.1, 8));
            assertTrue(TeleportHistoryScope.owns(player, new String("nether"), 4, 70.1, 8));
        }
    }

    @Test
    public void genericEventWithoutDimensionStillMatchesOwnedMove() {
        Object player = new Object();
        try (TeleportHistoryScope ignored = TeleportHistoryScope.open(player, "nether", 4, 70.1, 8)) {
            assertTrue(TeleportHistoryScope.owns(player, null, 4, 70.1, 8));
        }
    }

    @Test
    public void nestedSameActorRestoresParentTarget() {
        Object player = new Object();
        try (TeleportHistoryScope outer = TeleportHistoryScope.open(player, "overworld", 4, 70.1, 8)) {
            try (TeleportHistoryScope inner = TeleportHistoryScope.open(player, "nether", 9, 64, 9)) {
                assertTrue(TeleportHistoryScope.owns(player, "nether", 9, 64, 9));
                assertFalse(TeleportHistoryScope.owns(player, null, 4, 70.1, 8));
            }
            assertTrue(TeleportHistoryScope.owns(player, "overworld", 4, 70.1, 8));
        }
    }

    @Test
    public void nestedDifferentActorDoesNotBorrowOuterOwnership() {
        Object first = new Object(), second = new Object();
        try (TeleportHistoryScope outer = TeleportHistoryScope.open(first, "overworld", 4, 70.1, 8)) {
            try (TeleportHistoryScope inner = TeleportHistoryScope.open(second, "overworld", 4, 70.1, 8)) {
                assertFalse(TeleportHistoryScope.owns(first, null, 4, 70.1, 8));
                assertTrue(TeleportHistoryScope.owns(second, null, 4, 70.1, 8));
            }
            assertTrue(TeleportHistoryScope.owns(first, null, 4, 70.1, 8));
        }
    }

    @Test
    public void exceptionalExitRestoresParentAndClearsFinalScope() {
        Object player = new Object();
        try (TeleportHistoryScope outer = TeleportHistoryScope.open(player, "overworld", 4, 70.1, 8)) {
            try (TeleportHistoryScope inner = TeleportHistoryScope.open(player, "nether", 9, 64, 9)) {
                throw new IllegalStateException("native failure");
            } catch (IllegalStateException expected) {
                assertTrue(TeleportHistoryScope.owns(player, null, 4, 70.1, 8));
            }
        }
        assertFalse(TeleportHistoryScope.owns(player, null, 4, 70.1, 8));
    }

    @Test
    public void anotherThreadCannotObserveOrCloseOwnership() throws Exception {
        Object player = new Object();
        AtomicBoolean visible = new AtomicBoolean(true), rejected = new AtomicBoolean();
        try (TeleportHistoryScope scope = TeleportHistoryScope.open(player, "overworld", 4, 70.1, 8)) {
            Thread thread = new Thread(() -> {
                visible.set(TeleportHistoryScope.owns(player, null, 4, 70.1, 8));
                try {
                    scope.close();
                } catch (IllegalStateException expected) {
                    rejected.set(true);
                }
            });
            thread.start();
            thread.join();
            assertFalse(visible.get());
            assertTrue(rejected.get());
            assertTrue(TeleportHistoryScope.owns(player, null, 4, 70.1, 8));
        }
    }

    @Test
    public void repeatedInnerCloseCannotEraseRestoredParent() {
        Object player = new Object();
        try (TeleportHistoryScope outer = TeleportHistoryScope.open(player, "overworld", 4, 70.1, 8)) {
            TeleportHistoryScope inner = TeleportHistoryScope.open(player, "nether", 9, 64, 9);
            inner.close();
            inner.close();
            assertTrue(TeleportHistoryScope.owns(player, null, 4, 70.1, 8));
        }
    }
}
