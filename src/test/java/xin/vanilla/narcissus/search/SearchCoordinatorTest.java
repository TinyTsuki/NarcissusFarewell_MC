package xin.vanilla.narcissus.search;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;
import static xin.vanilla.narcissus.search.SearchTask.State.*;
import static xin.vanilla.narcissus.search.SearchTask.Failure.*;

public class SearchCoordinatorTest {
    @Test public void hardStepLimitAndSliceLimitApplyEvenWhenClockDoesNotAdvance() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            Task task = new Task(clock);
            tasks.add(task);
            assertTrue(coordinator.submit(task));
        }
        coordinator.tick();
        assertEquals(4096, tasks.stream().mapToInt(t -> t.steps).sum());
        for (Task task : tasks) {
            assertEquals(128, task.steps);
            assertEquals(64, task.largestSlice);
        }
        coordinator.close();
        assertEquals(0, coordinator.activeCount());
        for (Task task : tasks) assertEquals(1, task.closes);
    }

    @Test public void softBudgetAllowsAtMostOneSliceOverrun() {
        Clock clock = new Clock();
        Task task = new Task(clock);
        task.nanosPerStep = 1000;
        SearchCoordinator coordinator = coordinator(clock);
        assertTrue(coordinator.submit(task));
        coordinator.tick();
        assertEquals(2048, task.steps);
        assertEquals(64, task.largestSlice);
        assertEquals(2048000L, clock.now);
        coordinator.close();
    }

    @Test public void waitingAndZeroProgressTasksDoNotBlockAReadyPeerOrBusyPoll() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        Task waiting = new Task(clock);
        waiting.state = WAITING_CHUNK;
        waiting.progress = 0;
        Task stuck = new Task(clock);
        stuck.progress = 0;
        Task ready = new Task(clock);
        coordinator.submit(waiting);
        coordinator.submit(stuck);
        coordinator.submit(ready);
        coordinator.tick();
        assertEquals(1, waiting.calls);
        assertEquals(1, stuck.calls);
        assertEquals(4096, ready.steps);
        coordinator.tick();
        assertEquals(2, waiting.calls);
        assertEquals(2, stuck.calls);
        coordinator.close();
    }

    @Test public void exhaustedBudgetResumesAfterPreviousTaskInsteadOfStarvingTheTail() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        List<Task> tasks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            Task task = new Task(clock);
            task.nanosPerStep = 100000;
            tasks.add(task);
            coordinator.submit(task);
        }
        for (int i = 0; i < 8; i++) coordinator.tick();
        for (Task task : tasks) assertEquals(1, task.calls);
        coordinator.close();
    }

    @Test public void countdownUsesAdmissionSlotAndRejectedTaskIsClosedOnce() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        for (int i = 0; i < 32; i++) {
            Task task = new Task(clock);
            task.state = WAITING_COUNTDOWN;
            task.progress = 0;
            assertTrue(coordinator.submit(task));
        }
        Task rejected = new Task(clock);
        assertFalse(coordinator.submit(rejected));
        assertEquals(BUSY, rejected.reason);
        assertEquals(1, rejected.cancels);
        assertEquals(1, rejected.closes);
        assertEquals(32, coordinator.activeCount());
        coordinator.close();
        assertEquals(1, rejected.closes);
    }

    @Test public void staleSamePlayerIsRemovedBeforeCapacityCheck() {
        Clock clock = new Clock();
        AtomicReference<SearchExecutionSettings> settings = new AtomicReference<>(new SearchExecutionSettings(2, 1, 30));
        SearchCoordinator coordinator = new SearchCoordinator(() -> true, settings::get, () -> clock.now);
        Task old = new Task(clock);
        coordinator.submit(old);
        old.live = false;
        Task replacement = new Task(clock);
        replacement.id = old.id;
        assertTrue(coordinator.submit(replacement));
        assertEquals(PLAYER_CHANGED, old.reason);
        assertEquals(1, old.closes);
        assertEquals(1, coordinator.activeCount());
        coordinator.close();
    }

    @Test public void validSamePlayerRequestIsNotSilentlyReplaced() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        Task old = new Task(clock);
        Task duplicate = new Task(clock);
        duplicate.id = old.id;
        coordinator.submit(old);
        assertFalse(coordinator.submit(duplicate));
        assertEquals(BUSY, duplicate.reason);
        assertEquals(0, old.closes);
        coordinator.close();
    }

    @Test public void searchTimeoutUsesAdmissionValueButDoesNotExpireCountdown() {
        Clock clock = new Clock();
        AtomicReference<SearchExecutionSettings> settings = new AtomicReference<>(new SearchExecutionSettings(2, 32, 30));
        SearchCoordinator coordinator = new SearchCoordinator(() -> true, settings::get, () -> clock.now);
        Task first = new Task(clock);
        first.progress = 0;
        Task countdown = new Task(clock);
        countdown.state = WAITING_COUNTDOWN;
        countdown.progress = 0;
        coordinator.submit(first);
        coordinator.submit(countdown);
        settings.set(new SearchExecutionSettings(2, 32, 1));
        Task later = new Task(clock);
        later.progress = 0;
        coordinator.submit(later);
        clock.now = 1000000000L;
        coordinator.tick();
        assertEquals(TIMEOUT, later.reason);
        assertNull(first.reason);
        clock.now = 30000000000L;
        coordinator.tick();
        assertEquals(TIMEOUT, first.reason);
        assertNull(countdown.reason);
        assertEquals(1, coordinator.activeCount());
        coordinator.close();
    }

    @Test public void liveAndPolicyChangesCancelWithoutAdvancing() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        Task player = new Task(clock);
        Task policy = new Task(clock);
        coordinator.submit(player);
        coordinator.submit(policy);
        player.live = false;
        policy.matches = false;
        coordinator.tick();
        assertEquals(PLAYER_CHANGED, player.reason);
        assertEquals(POLICY_CHANGED, policy.reason);
        assertEquals(0, player.calls + policy.calls);
        assertEquals(0, coordinator.activeCount());
        assertEquals(1, player.closes);
        assertEquals(1, policy.closes);
    }

    @Test public void terminalTaskAndExplicitCancellationReleaseOnlyOnce() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        Task success = new Task(clock);
        success.nextState = COMMITTED;
        Task cancelled = new Task(clock);
        coordinator.submit(success);
        coordinator.submit(cancelled);
        coordinator.cancel(cancelled.id);
        coordinator.cancel(cancelled.id);
        coordinator.tick();
        coordinator.close();
        coordinator.close();
        assertEquals(1, success.calls);
        assertEquals(0, success.cancels);
        assertEquals(1, success.closes);
        assertEquals(SearchTask.Failure.CANCELLED, cancelled.reason);
        assertEquals(1, cancelled.cancels);
        assertEquals(1, cancelled.closes);
    }

    @Test public void taskFailureAndCleanupFailureCannotStrandOtherTasks() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        Task broken = new Task(clock);
        broken.throwStep = true;
        broken.throwCancel = true;
        broken.throwClose = true;
        Task peer = new Task(clock);
        peer.nextState = COMMITTED;
        coordinator.submit(broken);
        coordinator.submit(peer);
        coordinator.tick();
        assertEquals(ERROR, broken.reason);
        assertEquals(1, broken.closes);
        assertEquals(1, peer.calls);
        assertEquals(1, peer.closes);
        assertEquals(0, coordinator.activeCount());
        coordinator.close();
    }

    @Test public void badStepAccountingFailsRequestInsteadOfBypassingBound() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        Task broken = new Task(clock);
        broken.progress = 65;
        coordinator.submit(broken);
        coordinator.tick();
        assertEquals(ERROR, broken.reason);
        assertEquals(1, broken.closes);
        assertEquals(0, coordinator.activeCount());
    }

    @Test public void lowerLimitBlocksNewAdmissionWithoutCancellingExistingTasks() {
        Clock clock = new Clock();
        AtomicReference<SearchExecutionSettings> settings = new AtomicReference<>(new SearchExecutionSettings(2, 32, 30));
        SearchCoordinator coordinator = new SearchCoordinator(() -> true, settings::get, () -> clock.now);
        Task first = new Task(clock);
        Task second = new Task(clock);
        coordinator.submit(first);
        coordinator.submit(second);
        settings.set(new SearchExecutionSettings(.1, 1, 30));
        first.nanosPerStep = second.nanosPerStep = 1000;
        assertFalse(coordinator.submit(new Task(clock)));
        coordinator.tick();
        assertEquals(128, first.steps + second.steps);
        assertNull(first.reason);
        assertNull(second.reason);
        assertEquals(2, coordinator.activeCount());
        coordinator.close();
    }

    @Test public void badLiveSettingsReleaseAllTasksAndRefuseNewAdmission() {
        Clock clock = new Clock();
        AtomicBoolean bad = new AtomicBoolean();
        SearchCoordinator coordinator = new SearchCoordinator(() -> true, () -> {
            if (bad.get()) throw new IllegalArgumentException("bad live budget");
            return new SearchExecutionSettings(2, 32, 30);
        }, () -> clock.now);
        Task first = new Task(clock);
        coordinator.submit(first);
        bad.set(true);
        coordinator.tick();
        assertEquals(ERROR, first.reason);
        assertEquals(1, first.closes);
        Task rejected = new Task(clock);
        assertFalse(coordinator.submit(rejected));
        assertEquals(ERROR, rejected.reason);
        assertEquals(1, rejected.closes);
        bad.set(false);
        assertTrue(coordinator.submit(new Task(clock)));
        coordinator.close();
    }

    @Test public void ownerGuardAndServerStopPreventLateMutation() {
        Clock clock = new Clock();
        AtomicBoolean owner = new AtomicBoolean(true);
        SearchCoordinator coordinator = new SearchCoordinator(owner::get, () -> new SearchExecutionSettings(2, 32, 30), () -> clock.now);
        Task task = new Task(clock);
        coordinator.submit(task);
        owner.set(false);
        assertThrows(IllegalStateException.class, coordinator::tick);
        assertThrows(IllegalStateException.class, () -> coordinator.cancel(task.id));
        assertThrows(IllegalStateException.class, () -> coordinator.submit(new Task(clock)));
        assertThrows(IllegalStateException.class, coordinator::close);
        assertEquals(0, task.closes);
        owner.set(true);
        coordinator.close();
        assertEquals(SERVER_STOPPED, task.reason);
        Task late = new Task(clock);
        assertFalse(coordinator.submit(late));
        assertEquals(SERVER_STOPPED, late.reason);
        assertEquals(1, late.closes);
    }

    @Test public void settingsRejectInvalidRangesAndNonFiniteBudgets() {
        for (double value : new double[]{0, .09, 10.01, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> new SearchExecutionSettings(value, 32, 30));
        }
        assertThrows(IllegalArgumentException.class, () -> new SearchExecutionSettings(2, 0, 30));
        assertThrows(IllegalArgumentException.class, () -> new SearchExecutionSettings(2, 257, 30));
        assertThrows(IllegalArgumentException.class, () -> new SearchExecutionSettings(2, 32, 0));
        assertThrows(IllegalArgumentException.class, () -> new SearchExecutionSettings(2, 32, 301));
    }

    @Test public void expensiveMaintenanceCannotKeepTheTailFromRunning() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        Task slow = new Task(clock);
        slow.onPolicy = () -> clock.now += 3000000;
        Task peer = new Task(clock);
        peer.nextState = COMMITTED;
        coordinator.submit(slow);
        coordinator.submit(peer);
        coordinator.tick();
        assertEquals(0, slow.calls);
        coordinator.tick();
        assertEquals(1, peer.calls);
        assertEquals(1, peer.closes);
        coordinator.close();
    }

    @Test public void settingsReadIsIncludedInTheSharedBudget() {
        Clock clock = new Clock();
        AtomicBoolean chargeRead = new AtomicBoolean();
        SearchCoordinator coordinator = new SearchCoordinator(() -> true, () -> {
            if (chargeRead.get()) clock.now += 2000000;
            return new SearchExecutionSettings(2, 32, 30);
        }, () -> clock.now);
        Task task = new Task(clock);
        coordinator.submit(task);
        chargeRead.set(true);
        coordinator.tick();
        assertEquals(0, task.calls);
        assertEquals(1, coordinator.activeCount());
        coordinator.close();
    }

    @Test public void cancellationDuringPolicyCheckCannotAdvanceTheClosedRequest() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        Task task = new Task(clock);
        task.onPolicy = () -> coordinator.cancel(task.id);
        coordinator.submit(task);
        coordinator.tick();
        assertEquals(0, task.calls);
        assertEquals(1, task.closes);
        assertEquals(0, coordinator.activeCount());
    }

    @Test public void replacementDuringAdmissionValidationCannotBeOverwrittenOrLeaked() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        Task old = new Task(clock);
        coordinator.submit(old);
        old.live = false;
        Task inserted = new Task(clock);
        inserted.id = old.id;
        old.onLive = () -> {
            old.onLive = null;
            coordinator.cancel(old.id);
            assertTrue(coordinator.submit(inserted));
        };
        Task requested = new Task(clock);
        requested.id = old.id;
        assertFalse(coordinator.submit(requested));
        assertEquals(BUSY, requested.reason);
        assertEquals(1, requested.closes);
        coordinator.close();
        assertEquals(1, old.closes);
        assertEquals(1, inserted.closes);
    }

    @Test public void cancellationDuringStepAndDuplicateSubmissionAreIdempotent() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        Task task = new Task(clock);
        coordinator.submit(task);
        assertTrue(coordinator.submit(task));
        task.onStep = () -> coordinator.cancel(task.id);
        coordinator.tick();
        coordinator.close();
        assertEquals(1, task.calls);
        assertEquals(1, task.cancels);
        assertEquals(1, task.closes);
        assertEquals(0, coordinator.activeCount());
    }

    @Test public void recursiveTickCannotGrantAnotherGlobalStepBudget() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        Task first = new Task(clock);
        Task peer = new Task(clock);
        coordinator.submit(first);
        coordinator.submit(peer);
        first.onStep = () -> {
            first.onStep = null;
            coordinator.tick();
        };
        coordinator.tick();
        assertEquals(4096, first.steps + peer.steps);
        coordinator.close();
        assertEquals(1, first.closes);
        assertEquals(1, peer.closes);
    }

    @Test public void zeroStepStateFlipsCannotSpinForeverOrBypassVisitLimit() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        Task task = new Task(clock);
        task.progress = 0;
        task.onStep = () -> task.state = task.state == READY ? SEARCHING : READY;
        coordinator.submit(task);
        coordinator.tick();
        assertEquals(4096, task.calls);
        assertEquals(0, task.steps);
        coordinator.close();
    }

    private static SearchCoordinator coordinator(Clock clock) {
        return new SearchCoordinator(() -> true, () -> new SearchExecutionSettings(2, 32, 30), () -> clock.now);
    }

    @Test public void logoutPrunesRelatedInvalidTicketsWithoutAdvancingOtherSearches() {
        Clock clock = new Clock();
        SearchCoordinator coordinator = coordinator(clock);
        Task moving = new Task(clock), related = new Task(clock), other = new Task(clock);
        coordinator.submit(moving); coordinator.submit(related); coordinator.submit(other);
        moving.live = false; related.live = false;
        coordinator.pruneInvalid();
        assertEquals(1, coordinator.activeCount());
        assertEquals(1, moving.closes); assertEquals(1, related.closes);
        assertEquals(0, other.calls); assertEquals(0, other.closes);
        coordinator.close();
    }

    private static final class Clock { long now; }
    private static final class Task implements SearchTask {
        UUID id = UUID.randomUUID();
        final Clock clock;
        State state = SEARCHING;
        State nextState;
        Failure reason;
        int progress = 64, steps, calls, largestSlice, cancels, closes;
        long nanosPerStep;
        boolean live = true, matches = true, throwStep, throwCancel, throwClose;
        Runnable onLive, onPolicy, onStep;
        Task(Clock clock) { this.clock = clock; }
        @Override public UUID playerId() { return id; }
        @Override public boolean live() { if (onLive != null) onLive.run(); return live; }
        @Override public boolean policyMatches() { if (onPolicy != null) onPolicy.run(); return matches; }
        @Override public State state() { return state; }
        @Override public int step(int maxSteps) {
            calls++;
            if (onStep != null) onStep.run();
            largestSlice = Math.max(largestSlice, maxSteps);
            if (throwStep) throw new IllegalStateException("step failed");
            int count = progress == 65 ? 65 : Math.min(progress, maxSteps);
            steps += count;
            clock.now += nanosPerStep * count;
            if (nextState != null) state = nextState;
            return count;
        }
        @Override public void cancel(Failure reason) {
            this.reason = reason;
            cancels++;
            state = State.CANCELLED;
            if (throwCancel) throw new IllegalStateException("cancel failed");
        }
        @Override public void close() {
            closes++;
            if (throwClose) throw new IllegalStateException("close failed");
        }
    }
}
