package xin.vanilla.narcissus.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

public final class SearchCoordinator implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int SLICE_STEPS = 64;
    private static final int TICK_STEPS = 4096;
    private final BooleanSupplier owner;
    private final Supplier<SearchExecutionSettings> settings;
    private final LongSupplier nanoClock;
    private final Map<UUID, Entry> active = new HashMap<>();
    private final Deque<Entry> rotation = new ArrayDeque<>();
    private long tick;
    private boolean closed;
    private boolean ticking;

    public SearchCoordinator(BooleanSupplier owner, Supplier<SearchExecutionSettings> settings, LongSupplier nanoClock) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.settings = Objects.requireNonNull(settings, "settings");
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
    }

    public boolean submit(SearchTask task) {
        checkOwner();
        Objects.requireNonNull(task, "task");
        UUID id = Objects.requireNonNull(task.playerId(), "playerId");
        if (closed) return reject(task, SearchTask.Failure.SERVER_STOPPED, null);
        Entry old = active.get(id);
        if (old != null) {
            if (old.task == task) return true;
            try {
                if (terminal(old.task.state())) terminate(old, null, null);
                else if (!old.task.live()) terminate(old, SearchTask.Failure.PLAYER_CHANGED, null);
                else if (!old.task.policyMatches()) terminate(old, SearchTask.Failure.POLICY_CHANGED, null);
                else return reject(task, SearchTask.Failure.BUSY, null);
            } catch (RuntimeException | Error error) {
                terminate(old, SearchTask.Failure.ERROR, error);
            }
        }
        try {
            SearchExecutionSettings current = Objects.requireNonNull(settings.get(), "settings result");
            if (closed) return reject(task, SearchTask.Failure.SERVER_STOPPED, null);
            if (active.containsKey(id)) return reject(task, SearchTask.Failure.BUSY, null);
            if (active.size() >= current.maxConcurrentSearches()) return reject(task, SearchTask.Failure.BUSY, null);
            Entry entry = new Entry(id, task, nanoClock.getAsLong(), current.timeoutSeconds());
            active.put(id, entry);
            rotation.addLast(entry);
            return true;
        } catch (RuntimeException | Error error) {
            return reject(task, SearchTask.Failure.ERROR, error);
        }
    }

    public void tick() {
        checkOwner();
        if (closed || ticking || active.isEmpty()) return;
        ticking = true;
        try {
            runTick();
        } finally {
            ticking = false;
        }
    }

    private void runTick() {
        long start = nanoClock.getAsLong();
        SearchExecutionSettings current;
        try {
            current = Objects.requireNonNull(settings.get(), "settings result");
        } catch (RuntimeException | Error error) {
            cancelAll(SearchTask.Failure.ERROR, error);
            return;
        }
        long budget = (long) (current.timeBudgetMs() * 1000000);
        tick++;
        int steps = 0;
        int idle = 0;
        for (int visits = 0; visits < TICK_STEPS && !rotation.isEmpty() && steps < TICK_STEPS; visits++) {
            if (nanoClock.getAsLong() - start >= budget) break;
            Entry entry = rotation.removeFirst();
            if (entry.suspendedTick == tick) {
                rotation.addLast(entry);
                if (++idle >= rotation.size()) break;
                continue;
            }
            idle = 0;
            try {
                SearchTask.State before = Objects.requireNonNull(entry.task.state(), "task state");
                if (terminal(before)) {
                    terminate(entry, null, null);
                } else if (!entry.task.live()) {
                    terminate(entry, SearchTask.Failure.PLAYER_CHANGED, null);
                } else if (!entry.task.policyMatches()) {
                    terminate(entry, SearchTask.Failure.POLICY_CHANGED, null);
                } else if (active.get(entry.id) != entry) {
                    continue;
                } else if (searching(before) && nanoClock.getAsLong() - entry.start >= entry.timeoutNanos) {
                    terminate(entry, SearchTask.Failure.TIMEOUT, null);
                } else if (nanoClock.getAsLong() - start >= budget) {
                    rotation.addLast(entry);
                    break;
                } else {
                    int limit = Math.min(SLICE_STEPS, TICK_STEPS - steps);
                    int consumed = entry.task.step(limit);
                    if (consumed < 0 || consumed > limit) throw new IllegalStateException("Invalid search step count");
                    steps += consumed;
                    // A callback may cancel, replace, or commit this entry during step.
                    if (active.get(entry.id) != entry) continue;
                    SearchTask.State after = Objects.requireNonNull(entry.task.state(), "task state");
                    if (terminal(after)) terminate(entry, null, null);
                    else {
                        if (after == SearchTask.State.WAITING_CHUNK || after == SearchTask.State.WAITING_COUNTDOWN
                                || consumed == 0 && before == after) entry.suspendedTick = tick;
                        rotation.addLast(entry);
                    }
                }
            } catch (RuntimeException | Error error) {
                terminate(entry, SearchTask.Failure.ERROR, error);
            }
        }
    }

    public void cancel(UUID playerId) {
        checkOwner();
        Entry entry = active.get(playerId);
        if (entry != null) terminate(entry, SearchTask.Failure.CANCELLED, null);
    }

    public int activeCount() {
        checkOwner();
        return active.size();
    }

    /**
     * Lifecycle-event cleanup, without spending another tick's search budget.
     */
    public void pruneInvalid() {
        checkOwner();
        for (Entry entry : new ArrayList<>(active.values())) {
            if (active.get(entry.id) != entry) continue;
            try {
                if (!entry.task.live()) terminate(entry, SearchTask.Failure.PLAYER_CHANGED, null);
            } catch (RuntimeException | Error error) {
                terminate(entry, SearchTask.Failure.ERROR, error);
            }
        }
    }

    @Override
    public void close() {
        checkOwner();
        if (closed) return;
        closed = true;
        cancelAll(SearchTask.Failure.SERVER_STOPPED, null);
    }

    private void checkOwner() {
        if (!owner.getAsBoolean()) throw new IllegalStateException("Search coordinator requires owner thread");
    }

    private static boolean terminal(SearchTask.State state) {
        return state == SearchTask.State.COMMITTED || state == SearchTask.State.CANCELLED || state == SearchTask.State.FAILED;
    }

    private static boolean searching(SearchTask.State state) {
        return state == SearchTask.State.READY || state == SearchTask.State.SEARCHING || state == SearchTask.State.WAITING_CHUNK;
    }

    private void terminate(Entry entry, SearchTask.Failure reason, Throwable error) {
        if (active.get(entry.id) != entry) return;
        active.remove(entry.id);
        rotation.remove(entry);
        finish(entry.task, reason, error);
    }

    private void cancelAll(SearchTask.Failure reason, Throwable error) {
        for (Entry entry : new ArrayList<>(active.values())) terminate(entry, reason, error);
    }

    private boolean reject(SearchTask task, SearchTask.Failure reason, Throwable error) {
        finish(task, reason, error);
        return false;
    }

    private static void finish(SearchTask task, SearchTask.Failure reason, Throwable error) {
        try {
            if (reason != null) task.cancel(reason);
        } catch (RuntimeException | Error failure) {
            error = combine(error, failure);
        } finally {
            try {
                task.close();
            } catch (RuntimeException | Error failure) {
                error = combine(error, failure);
            }
        }
        if (error != null) LOGGER.error("Teleport search failed", error);
    }

    private static Throwable combine(Throwable first, Throwable next) {
        if (first == null) return next;
        if (first != next) first.addSuppressed(next);
        return first;
    }

    private static final class Entry {
        final UUID id;
        final SearchTask task;
        final long start;
        final long timeoutNanos;
        long suspendedTick = Long.MIN_VALUE;

        Entry(UUID id, SearchTask task, long start, int timeoutSeconds) {
            this.id = id;
            this.task = task;
            this.start = start;
            this.timeoutNanos = timeoutSeconds * 1000000000L;
        }
    }
}
