package xin.vanilla.narcissus.search;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.narcissus.NarcissusComponent;

import java.util.*;
import java.util.function.BooleanSupplier;

public final class SearchChunkPool implements AutoCloseable {
    public enum Availability implements IEnumDescribable {
        READY, WAITING, BUSY;

        @Override
        public Component enumDescription() {
            return NarcissusComponent.get().literal(name());
        }
    }

    public interface Backend {
        boolean isReady(ResourceLocation dimension, int cx, int cz);

        void retain(ResourceLocation dimension, int cx, int cz);

        void release(ResourceLocation dimension, int cx, int cz);
    }

    private static final int LIMIT = 4;
    private final Backend backend;
    private final BooleanSupplier isOwnerThread;
    private final Map<Key, Entry> entries = new HashMap<>();
    private final Set<Entry> pending = new HashSet<>();
    private final Set<Lease> leases = new HashSet<>();
    private long tick;
    private int newLoads;
    private boolean closed;

    public SearchChunkPool(Backend backend, BooleanSupplier isOwnerThread) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.isOwnerThread = Objects.requireNonNull(isOwnerThread, "isOwnerThread");
    }

    public void beginTick() {
        checkOpen();
        tick++;
        newLoads = 0;
        // Only pending tickets need proactive polling; loaded holds are checked on use.
        for (Entry entry : new ArrayList<>(pending)) {
            if (entry.references > 0) refresh(entry);
        }
    }

    public Lease open(ResourceLocation dimension) {
        checkOpen();
        Lease lease = new Lease(Objects.requireNonNull(dimension, "dimension"));
        leases.add(lease);
        return lease;
    }

    public int pendingCount() {
        checkOwner();
        return pending.size();
    }

    public int leaseCount() {
        checkOwner();
        int count = 0;
        for (Entry entry : entries.values()) count += Math.max(1, entry.references);
        return count;
    }

    @Override
    public void close() {
        checkOwner();
        closed = true;
        Throwable failure = null;
        // Retry earlier cleanup failures, without retrying fresh failures in this call.
        for (Entry entry : new ArrayList<>(entries.values())) {
            if (entry.references != 0) continue;
            try {
                release(entry);
            } catch (RuntimeException | Error error) {
                failure = combine(failure, error);
            }
        }
        for (Lease lease : new ArrayList<>(leases)) {
            try {
                lease.close(false);
            } catch (RuntimeException | Error error) {
                failure = combine(failure, error);
            }
        }
        rethrow(failure);
    }

    private void checkOwner() {
        if (!isOwnerThread.getAsBoolean()) throw new IllegalStateException("Search tickets require owner thread");
    }

    private void checkOpen() {
        checkOwner();
        if (closed) throw new IllegalStateException("Search chunk pool is closed");
    }

    private void refresh(Entry entry) {
        if (entry.checkedTick == tick) return;
        Key key = entry.key;
        boolean ready = backend.isReady(key.dimension, key.x, key.z);
        if (entry.ready && !ready) throw new IllegalStateException("Retained search chunk lost readiness");
        entry.checkedTick = tick;
        if (ready && !entry.ready) pending.remove(entry);
        entry.ready = ready;
    }

    private void release(Entry entry) {
        if (entries.get(entry.key) != entry) return;
        Key key = entry.key;
        backend.release(key.dimension, key.x, key.z);
        entries.remove(key);
        pending.remove(entry);
    }

    private static Throwable combine(Throwable previous, Throwable next) {
        if (next == null) return previous;
        if (previous == null) return next;
        if (previous != next) previous.addSuppressed(next);
        return previous;
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof RuntimeException) throw (RuntimeException) failure;
        if (failure instanceof Error) throw (Error) failure;
    }

    public final class Lease implements AutoCloseable {
        private final ResourceLocation dimension;
        private final Map<Key, Entry> holds = new LinkedHashMap<>(LIMIT, .75f, true);
        private List<Entry> failedReleases;
        private boolean closed;

        private Lease(ResourceLocation dimension) {
            this.dimension = dimension;
        }

        public Availability require(int cx, int cz) {
            checkUsable();
            Key key = new Key(dimension, cx, cz);
            Entry entry = holds.get(key);
            if (entry != null) {
                refresh(entry);
                return availability(entry);
            }
            entry = entries.get(key);
            if (entry != null && entry.references == 0) {
                release(entry);
                entry = null;
            }
            if (entry != null) {
                refresh(entry);
                evict();
                entry.references++;
                holds.put(key, entry);
                return availability(entry);
            }
            boolean ready = backend.isReady(dimension, cx, cz);
            if (!ready && (newLoads >= LIMIT || pending.size() >= LIMIT)) return Availability.BUSY;
            evict();
            if (!ready) newLoads++;
            entry = new Entry(key, ready, tick);
            try {
                backend.retain(dimension, cx, cz);
            } catch (RuntimeException | Error error) {
                try {
                    backend.release(dimension, cx, cz);
                } catch (RuntimeException | Error cleanup) {
                    combine(error, cleanup);
                    entries.put(key, entry);
                    if (!ready) pending.add(entry);
                    rememberFailure(entry);
                }
                throw error;
            }
            entries.put(key, entry);
            if (!ready) pending.add(entry);
            entry.references = 1;
            holds.put(key, entry);
            return availability(entry);
        }

        public void retainFinal(int cx, int cz) {
            checkUsable();
            Entry keep = holds.get(new Key(dimension, cx, cz));
            if (keep == null) throw new IllegalStateException("Final search chunk is not held");
            refresh(keep);
            if (!keep.ready) throw new IllegalStateException("Final search chunk is not ready");
            rethrow(dropHolds(keep));
        }

        @Override
        public void close() {
            checkOwner();
            close(true);
        }

        private void close(boolean retryFailures) {
            Throwable failure = null;
            if (retryFailures && failedReleases != null) {
                Iterator<Entry> iterator = failedReleases.iterator();
                while (iterator.hasNext()) {
                    Entry entry = iterator.next();
                    try {
                        if (entry.references == 0) release(entry);
                        iterator.remove();
                    } catch (RuntimeException | Error error) {
                        failure = combine(failure, error);
                    }
                }
            }
            if (!closed) {
                closed = true;
                failure = combine(failure, dropHolds(null));
                leases.remove(this);
            }
            rethrow(failure);
        }

        private void checkUsable() {
            checkOpen();
            if (closed) throw new IllegalStateException("Search chunk lease is closed");
            if (failedReleases != null) {
                for (Entry entry : failedReleases) {
                    if (entries.get(entry.key) == entry && entry.references == 0) {
                        throw new IllegalStateException("Search chunk lease has unreleased tickets");
                    }
                }
            }
        }

        private Availability availability(Entry entry) {
            return entry.ready ? Availability.READY : Availability.WAITING;
        }

        private void evict() {
            if (holds.size() < LIMIT) return;
            Iterator<Entry> iterator = holds.values().iterator();
            Entry entry = iterator.next();
            iterator.remove();
            drop(entry);
        }

        private Throwable dropHolds(Entry keep) {
            Throwable failure = null;
            Iterator<Entry> iterator = holds.values().iterator();
            while (iterator.hasNext()) {
                Entry entry = iterator.next();
                if (entry == keep) continue;
                iterator.remove();
                try {
                    drop(entry);
                } catch (RuntimeException | Error error) {
                    failure = combine(failure, error);
                }
            }
            return failure;
        }

        private void drop(Entry entry) {
            if (--entry.references != 0) return;
            try {
                release(entry);
            } catch (RuntimeException | Error error) {
                rememberFailure(entry);
                throw error;
            }
        }

        private void rememberFailure(Entry entry) {
            if (failedReleases == null) failedReleases = new ArrayList<>();
            failedReleases.add(entry);
        }
    }

    private static final class Entry {
        private final Key key;
        private int references;
        private boolean ready;
        private long checkedTick;

        private Entry(Key key, boolean ready, long checkedTick) {
            this.key = key;
            this.ready = ready;
            this.checkedTick = checkedTick;
        }
    }

    private static final class Key {
        private final ResourceLocation dimension;
        private final int x;
        private final int z;
        private final long position;

        private Key(ResourceLocation dimension, int x, int z) {
            this.dimension = dimension;
            this.x = x;
            this.z = z;
            this.position = ChunkPos.asLong(x, z);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (!(other instanceof Key)) return false;
            Key key = (Key) other;
            return position == key.position && dimension.equals(key.dimension);
        }

        @Override
        public int hashCode() {
            return 31 * dimension.hashCode() + Long.hashCode(position);
        }
    }
}
