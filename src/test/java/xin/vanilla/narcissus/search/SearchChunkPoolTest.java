package xin.vanilla.narcissus.search;

import net.minecraft.resources.ResourceLocation;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static xin.vanilla.narcissus.search.SearchChunkPool.Availability.*;

public class SearchChunkPoolTest {
    private static final ResourceLocation OVERWORLD = ResourceLocation.parse("minecraft:overworld");
    private static final ResourceLocation NETHER = ResourceLocation.parse("minecraft:the_nether");

    @Test
    public void sharedChunkIsRetainedOnceAndReleasedOnlyAfterItsLastLease() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease first = pool.open(OVERWORLD);
        SearchChunkPool.Lease second = pool.open(OVERWORLD);
        assertEquals(WAITING, first.require(4, -1));
        assertEquals(WAITING, second.require(4, -1));
        assertEquals(1, backend.retains(OVERWORLD, 4, -1));
        assertEquals(1, pool.pendingCount());
        assertEquals(2, pool.leaseCount());
        first.close();
        assertEquals(0, backend.releases(OVERWORLD, 4, -1));
        assertEquals(1, pool.leaseCount());
        second.close();
        second.close();
        assertEquals(1, backend.releases(OVERWORLD, 4, -1));
        assertEquals(0, pool.leaseCount());
        assertEquals(0, pool.pendingCount());
    }

    @Test
    public void equalChunkCoordinatesInDifferentDimensionsDoNotShareTickets() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        assertEquals(WAITING, pool.open(OVERWORLD).require(-4, 7));
        assertEquals(WAITING, pool.open(NETHER).require(-4, 7));
        assertEquals(2, backend.live.size());
        assertEquals(2, pool.pendingCount());
        pool.close();
        assertEquals(1, backend.releases(OVERWORLD, -4, 7));
        assertEquals(1, backend.releases(NETHER, -4, 7));
    }

    @Test
    public void fifthNewLoadIsBusyEvenIfEarlierPendingTicketsWereReleasedThisTick() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        for (int x = 0; x < 4; x++) {
            SearchChunkPool.Lease lease = pool.open(OVERWORLD);
            assertEquals(WAITING, lease.require(x, 0));
            lease.close();
        }
        SearchChunkPool.Lease fifth = pool.open(OVERWORLD);
        assertEquals(BUSY, fifth.require(4, 0));
        assertEquals(0, backend.retains(OVERWORLD, 4, 0));
        assertEquals(0, pool.pendingCount());
        pool.beginTick();
        assertEquals(WAITING, fifth.require(4, 0));
        pool.close();
    }

    @Test
    public void pendingLimitSurvivesTheTickBoundaryAndReadyChunkFreesOneSlot() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        for (int x = 0; x < 4; x++) assertEquals(WAITING, pool.open(OVERWORLD).require(x, 0));
        SearchChunkPool.Lease fifth = pool.open(OVERWORLD);
        pool.beginTick();
        assertEquals(BUSY, fifth.require(4, 0));
        assertEquals(4, pool.pendingCount());
        backend.ready.add(key(OVERWORLD, 0, 0));
        pool.beginTick();
        assertEquals(3, pool.pendingCount());
        assertEquals(WAITING, fifth.require(4, 0));
        assertEquals(4, pool.pendingCount());
        pool.close();
    }

    @Test
    public void loadedChunksDoNotUseTheNewLoadBudgetOrPendingSlots() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        for (int x = 0; x < 8; x++) {
            backend.ready.add(key(OVERWORLD, x, 0));
            assertEquals(READY, pool.open(OVERWORLD).require(x, 0));
        }
        for (int x = 8; x < 12; x++) assertEquals(WAITING, pool.open(OVERWORLD).require(x, 0));
        assertEquals(BUSY, pool.open(OVERWORLD).require(12, 0));
        assertEquals(4, pool.pendingCount());
        assertEquals(12, pool.leaseCount());
        pool.close();
    }

    @Test
    public void sharedWaitingChunkIsPolledOnlyOncePerTickAndNeverBusyWaits() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease first = pool.open(OVERWORLD);
        SearchChunkPool.Lease second = pool.open(OVERWORLD);
        assertEquals(WAITING, first.require(3, 5));
        for (int i = 0; i < 10; i++) assertEquals(WAITING, second.require(3, 5));
        assertEquals(1, backend.checks(OVERWORLD, 3, 5));
        pool.beginTick();
        assertEquals(2, backend.checks(OVERWORLD, 3, 5));
        backend.ready.add(key(OVERWORLD, 3, 5));
        assertEquals(WAITING, first.require(3, 5));
        assertEquals(2, backend.checks(OVERWORLD, 3, 5));
        pool.beginTick();
        assertEquals(READY, first.require(3, 5));
        assertEquals(READY, second.require(3, 5));
        assertEquals(3, backend.checks(OVERWORLD, 3, 5));
        assertEquals(0, pool.pendingCount());
        pool.close();
    }

    @Test
    public void fifthHoldEvictsLeastRecentlyUsedBeforeRetainingTheNextTicket() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease lease = pool.open(OVERWORLD);
        for (int x = 0; x < 5; x++) backend.ready.add(key(OVERWORLD, x, 0));
        for (int x = 0; x < 4; x++) assertEquals(READY, lease.require(x, 0));
        assertEquals(READY, lease.require(0, 0));
        assertEquals(READY, lease.require(4, 0));
        assertEquals(1, backend.releases(OVERWORLD, 1, 0));
        assertEquals(0, backend.releases(OVERWORLD, 0, 0));
        assertEquals(4, pool.leaseCount());
        assertEquals(4, backend.maxLive);
        pool.close();
    }

    @Test
    public void retainingFinalChunkDropsOtherHoldsButKeepsTheFinalSharedTicket() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease lease = pool.open(OVERWORLD);
        SearchChunkPool.Lease peer = pool.open(OVERWORLD);
        for (int x = 0; x < 3; x++) {
            backend.ready.add(key(OVERWORLD, x, 0));
            assertEquals(READY, lease.require(x, 0));
        }
        assertEquals(READY, peer.require(2, 0));
        lease.retainFinal(2, 0);
        lease.retainFinal(2, 0);
        assertEquals(2, pool.leaseCount());
        assertEquals(1, backend.live.size());
        assertEquals(1, backend.releases(OVERWORLD, 0, 0));
        lease.close();
        assertEquals(0, backend.releases(OVERWORLD, 2, 0));
        peer.close();
        assertEquals(1, backend.releases(OVERWORLD, 2, 0));
    }

    @Test
    public void finalRetentionCannotSilentlyLoadAnUnknownOrWaitingChunk() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease lease = pool.open(OVERWORLD);
        assertEquals(WAITING, lease.require(1, 0));
        assertThrows(IllegalStateException.class, () -> lease.retainFinal(1, 0));
        assertThrows(IllegalStateException.class, () -> lease.retainFinal(2, 0));
        assertEquals(1, pool.leaseCount());
        assertEquals(0, backend.retains(OVERWORLD, 2, 0));
        pool.close();
    }

    @Test
    public void partialRetainFailureRollsBackPendingSlotsButStillCountsItsLoadingAttempt() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease lease = pool.open(OVERWORLD);
        backend.failRetain = key(OVERWORLD, -1, 0);
        assertThrows(IllegalStateException.class, () -> lease.require(-1, 0));
        assertTrue(backend.live.isEmpty());
        assertEquals(0, pool.pendingCount());
        assertEquals(0, pool.leaseCount());
        for (int x = 0; x < 3; x++) assertEquals(WAITING, lease.require(x, 0));
        assertEquals(BUSY, lease.require(3, 0));
        pool.beginTick();
        assertEquals(WAITING, lease.require(3, 0));
        assertEquals(4, pool.pendingCount());
        pool.close();
    }

    @Test
    public void failedReadinessDoesNotEvictTheExistingCacheOrRegisterANewTicket() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease lease = pool.open(OVERWORLD);
        for (int x = 0; x < 4; x++) {
            backend.ready.add(key(OVERWORLD, x, 0));
            assertEquals(READY, lease.require(x, 0));
        }
        backend.failCheck = key(OVERWORLD, 4, 0);
        assertThrows(IllegalStateException.class, () -> lease.require(4, 0));
        assertEquals(4, pool.leaseCount());
        assertEquals(0, backend.retains(OVERWORLD, 4, 0));
        assertEquals(0, backend.releases(OVERWORLD, 0, 0));
        pool.close();
    }

    @Test
    public void operationsOutsideOwnerThreadCannotMutateNativeTickets() {
        Backend backend = new Backend();
        AtomicBoolean owner = new AtomicBoolean(true);
        SearchChunkPool pool = new SearchChunkPool(backend, owner::get);
        SearchChunkPool.Lease lease = pool.open(OVERWORLD);
        assertEquals(WAITING, lease.require(1, 0));
        owner.set(false);
        assertThrows(IllegalStateException.class, () -> pool.open(NETHER));
        assertThrows(IllegalStateException.class, () -> lease.require(2, 0));
        assertThrows(IllegalStateException.class, () -> lease.retainFinal(1, 0));
        assertThrows(IllegalStateException.class, lease::close);
        assertThrows(IllegalStateException.class, pool::beginTick);
        assertThrows(IllegalStateException.class, pool::close);
        assertEquals(1, backend.live.size());
        owner.set(true);
        assertEquals(1, pool.leaseCount());
        pool.close();
        assertTrue(backend.live.isEmpty());
    }

    @Test
    public void stoppingPoolClosesActiveLeasesOnceAndPreventsLateAcquisition() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease first = pool.open(OVERWORLD);
        SearchChunkPool.Lease second = pool.open(NETHER);
        assertEquals(WAITING, first.require(1, 0));
        assertEquals(WAITING, second.require(2, 0));
        pool.close();
        pool.close();
        first.close();
        second.close();
        assertEquals(0, pool.pendingCount());
        assertEquals(0, pool.leaseCount());
        assertEquals(1, backend.releases(OVERWORLD, 1, 0));
        assertEquals(1, backend.releases(NETHER, 2, 0));
        assertThrows(IllegalStateException.class, () -> pool.open(OVERWORLD));
        assertThrows(IllegalStateException.class, () -> first.require(3, 0));
    }

    @Test
    public void aFailedReleaseDoesNotSkipOtherCleanupOrPretendAllTicketsWereReleased() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        assertEquals(WAITING, pool.open(OVERWORLD).require(1, 0));
        assertEquals(WAITING, pool.open(NETHER).require(2, 0));
        backend.failRelease = key(OVERWORLD, 1, 0);
        assertThrows(IllegalStateException.class, pool::close);
        assertEquals(1, backend.live.size());
        assertEquals(1, pool.leaseCount());
        assertEquals(1, pool.pendingCount());
        assertEquals(1, backend.releases(NETHER, 2, 0));
        backend.failRelease = null;
        pool.close();
        assertTrue(backend.live.isEmpty());
        assertEquals(0, pool.leaseCount());
        assertEquals(0, pool.pendingCount());
    }

    @Test
    public void failedRollbackPreservesOriginalErrorAndAccountsForTheUnreleasedTicket() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease lease = pool.open(OVERWORLD);
        backend.failRetain = key(OVERWORLD, -1, 0);
        backend.failRelease = backend.failRetain;
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> lease.require(-1, 0));
        assertEquals("retain failed after side effect", failure.getMessage());
        assertEquals(1, failure.getSuppressed().length);
        assertEquals(1, pool.leaseCount());
        assertEquals(1, pool.pendingCount());
        backend.failRelease = null;
        pool.close();
        assertTrue(backend.live.isEmpty());
        assertEquals(0, pool.leaseCount());
    }

    @Test
    public void failedLeaseCloseCanRetryWithoutDoubleDecrementingReferences() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease lease = pool.open(OVERWORLD);
        assertEquals(WAITING, lease.require(0, 0));
        backend.failRelease = key(OVERWORLD, 0, 0);
        assertThrows(IllegalStateException.class, lease::close);
        assertEquals(1, pool.leaseCount());
        backend.failRelease = null;
        lease.close();
        lease.close();
        assertEquals(0, pool.leaseCount());
        assertEquals(0, pool.pendingCount());
        assertEquals(2, backend.releases(OVERWORLD, 0, 0));
        pool.close();
    }

    @Test
    public void lossOfAnAlreadyReadyRetainedChunkFailsInsteadOfPermittingUnsafeReads() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease lease = pool.open(OVERWORLD);
        backend.ready.add(key(OVERWORLD, 0, 0));
        assertEquals(READY, lease.require(0, 0));
        pool.beginTick();
        backend.ready.clear();
        assertThrows(IllegalStateException.class, () -> lease.require(0, 0));
        pool.close();
        assertTrue(backend.live.isEmpty());
    }

    @Test
    public void failedEvictionCannotAccumulateMoreNativeHoldsOnTheSameLease() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease lease = pool.open(OVERWORLD);
        for (int x = 0; x < 6; x++) backend.ready.add(key(OVERWORLD, x, 0));
        for (int x = 0; x < 4; x++) assertEquals(READY, lease.require(x, 0));
        backend.failRelease = key(OVERWORLD, 0, 0);
        assertThrows(IllegalStateException.class, () -> lease.require(4, 0));
        assertThrows(IllegalStateException.class, () -> lease.require(5, 0));
        assertEquals(0, backend.retains(OVERWORLD, 4, 0));
        assertEquals(0, backend.retains(OVERWORLD, 5, 0));
        assertEquals(4, pool.leaseCount());
        assertEquals(4, backend.maxLive);
        backend.failRelease = null;
        lease.close();
        assertTrue(backend.live.isEmpty());
        pool.close();
    }

    @Test
    public void finalRetentionKeepsOtherRequestsNonFinalChunksAlive() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease first = pool.open(OVERWORLD);
        SearchChunkPool.Lease peer = pool.open(OVERWORLD);
        for (int x = 0; x < 3; x++) backend.ready.add(key(OVERWORLD, x, 0));
        assertEquals(READY, first.require(0, 0));
        assertEquals(READY, first.require(1, 0));
        assertEquals(READY, peer.require(0, 0));
        assertEquals(READY, peer.require(2, 0));
        first.retainFinal(1, 0);
        assertEquals(0, backend.releases(OVERWORLD, 0, 0));
        assertEquals(READY, peer.require(0, 0));
        first.close();
        assertEquals(1, backend.releases(OVERWORLD, 1, 0));
        assertEquals(2, pool.leaseCount());
        peer.close();
        assertTrue(backend.live.isEmpty());
        pool.close();
    }

    @Test
    public void stoppedPoolCanStillReleaseLeaseThatFailedDuringShutdown() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease lease = pool.open(OVERWORLD);
        assertEquals(WAITING, lease.require(0, 0));
        backend.failRelease = key(OVERWORLD, 0, 0);
        assertThrows(IllegalStateException.class, pool::close);
        assertEquals(1, backend.releases(OVERWORLD, 0, 0));
        backend.failRelease = null;
        lease.close();
        pool.close();
        assertEquals(2, backend.releases(OVERWORLD, 0, 0));
        assertEquals(0, pool.pendingCount());
        assertEquals(0, pool.leaseCount());
    }

    @Test
    public void rollbackFailureIsRetriedOnlyOnceInEachPoolClose() {
        Backend backend = new Backend();
        SearchChunkPool pool = pool(backend);
        SearchChunkPool.Lease lease = pool.open(OVERWORLD);
        backend.failRetain = key(OVERWORLD, 0, 0);
        backend.failRelease = backend.failRetain;
        assertThrows(IllegalStateException.class, () -> lease.require(0, 0));
        assertThrows(IllegalStateException.class, pool::close);
        assertEquals(2, backend.releases(OVERWORLD, 0, 0));
        assertEquals(1, pool.pendingCount());
        backend.failRelease = null;
        pool.close();
        lease.close();
        assertEquals(3, backend.releases(OVERWORLD, 0, 0));
        assertEquals(0, pool.leaseCount());
    }

    private static SearchChunkPool pool(Backend backend) {
        return new SearchChunkPool(backend, () -> true);
    }

    private static String key(ResourceLocation dimension, int x, int z) {
        return dimension + ":" + x + ":" + z;
    }

    private static final class Backend implements SearchChunkPool.Backend {
        private final Set<String> ready = new HashSet<>();
        private final Set<String> live = new HashSet<>();
        private final Map<String, Integer> retainCalls = new HashMap<>();
        private final Map<String, Integer> releaseCalls = new HashMap<>();
        private final Map<String, Integer> readinessCalls = new HashMap<>();
        private String failRetain;
        private String failRelease;
        private String failCheck;
        private int maxLive;

        @Override
        public boolean isReady(ResourceLocation dimension, int cx, int cz) {
            String key = key(dimension, cx, cz);
            readinessCalls.merge(key, 1, Integer::sum);
            if (key.equals(failCheck)) throw new IllegalStateException("readiness failed");
            return ready.contains(key);
        }

        @Override
        public void retain(ResourceLocation dimension, int cx, int cz) {
            String key = key(dimension, cx, cz);
            retainCalls.merge(key, 1, Integer::sum);
            if (!live.add(key)) throw new AssertionError("Duplicate native retain: " + key);
            maxLive = Math.max(maxLive, live.size());
            if (key.equals(failRetain)) throw new IllegalStateException("retain failed after side effect");
        }

        @Override
        public void release(ResourceLocation dimension, int cx, int cz) {
            String key = key(dimension, cx, cz);
            releaseCalls.merge(key, 1, Integer::sum);
            if (key.equals(failRelease)) throw new IllegalStateException("release failed");
            live.remove(key);
        }

        private int retains(ResourceLocation dimension, int cx, int cz) { return retainCalls.getOrDefault(key(dimension, cx, cz), 0); }
        private int releases(ResourceLocation dimension, int cx, int cz) { return releaseCalls.getOrDefault(key(dimension, cx, cz), 0); }
        private int checks(ResourceLocation dimension, int cx, int cz) { return readinessCalls.getOrDefault(key(dimension, cx, cz), 0); }
    }
}
