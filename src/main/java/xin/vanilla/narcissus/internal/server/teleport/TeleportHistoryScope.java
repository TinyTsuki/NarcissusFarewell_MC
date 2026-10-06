package xin.vanilla.narcissus.internal.server.teleport;

import java.util.Objects;

/** Ownership of one initiating player's synchronous native move, not the whole riding graph. */
public final class TeleportHistoryScope implements AutoCloseable {
    private static final ThreadLocal<TeleportHistoryScope> ACTIVE = new ThreadLocal<>();
    private final Thread owner = Thread.currentThread();
    private final TeleportHistoryScope parent;
    private final Object player;
    private final Object dimension;
    private final double x, y, z;
    private final boolean ownsNativeMove;
    private boolean recorded;
    private Object recordedDimension;
    private double recordedX, recordedY, recordedZ;
    private boolean closed;

    private TeleportHistoryScope(Object player, Object dimension, double x, double y, double z, boolean ownsNativeMove) {
        this.player = Objects.requireNonNull(player, "player");
        this.dimension = ownsNativeMove ? Objects.requireNonNull(dimension, "dimension") : dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.ownsNativeMove = ownsNativeMove;
        parent = ACTIVE.get();
        ACTIVE.set(this);
    }

    public static TeleportHistoryScope open(Object player, Object dimension, double x, double y, double z) {
        return new TeleportHistoryScope(player, dimension, x, y, z, true);
    }

    public static TeleportHistoryScope track(Object player) {
        return new TeleportHistoryScope(player, null, 0, 0, 0, false);
    }

    public static boolean owns(Object player, Object dimension, double x, double y, double z) {
        TeleportHistoryScope scope = ACTIVE.get();
        // Forge's generic event has no destination dimension; Fabric supplies it explicitly.
        return scope != null && scope.ownsNativeMove && scope.player == player
                && (dimension == null || scope.dimension.equals(dimension))
                && scope.x == x && scope.y == y && scope.z == z;
    }

    public static void noteRecorded(Object player, Object dimension, double x, double y, double z) {
        for (TeleportHistoryScope scope = ACTIVE.get(); scope != null; scope = scope.parent) {
            if (scope.player != player) continue;
            scope.recorded = true;
            scope.recordedDimension = dimension;
            scope.recordedX = x;
            scope.recordedY = y;
            scope.recordedZ = z;
        }
    }

    public boolean hasRecordedActualEnd(Object player, Object dimension, double x, double y, double z) {
        return Thread.currentThread() == owner && this.player == player && recorded
                && Objects.equals(recordedDimension, dimension)
                && recordedX == x && recordedY == y && recordedZ == z;
    }

    @Override
    public void close() {
        if (closed) return;
        if (Thread.currentThread() != owner || ACTIVE.get() != this) {
            throw new IllegalStateException("Teleport history scope closed outside its owner invocation");
        }
        closed = true;
        if (parent == null) ACTIVE.remove();
        else ACTIVE.set(parent);
    }
}
