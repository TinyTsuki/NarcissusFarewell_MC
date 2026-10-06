package xin.vanilla.narcissus.internal.server.search;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.narcissus.config.CommonSearchConfiguration;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.enums.EnumSafeMode;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.search.*;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Internal owner-thread search and completion boundary.
 */
public final class NarcissusSearchRequest implements SearchTask {
    public interface Access {
        boolean live();

        boolean policyMatches();

        void beginSlice();

        default SafeCandidateCursor.YFilter heightFilter(SearchBox box, boolean belowAir) {
            return null;
        }

        boolean safe(BlockPos pos, boolean belowAir);

        boolean blocksMotion(BlockPos pos);

        int minY();

        int maxY();

        BlockState supportState();

        boolean hasSupportItem(ItemStack item);

        SafeWorldCoordinate random(SafeWorldCoordinate origin, int range);

        void cancelTicket();

        void failed(Failure reason);

        void viewNotFound(boolean safe);
    }

    public enum ResultKind implements IEnumDescribable {
        FOUND, EXHAUSTED_FALLBACK, SUPPORT_PLAN, VIEW_ENDPOINT;

        public Component enumDescription() {
            return xin.vanilla.narcissus.NarcissusComponent.get().literal(name());
        }
    }

    private final UUID id;
    private final SafeWorldCoordinate origin;
    private final CommonSearchConfiguration.Snapshot config;
    private final Access access;
    private final SearchChunkPool.Lease lease;
    private Consumer<NarcissusSearchRequest> callback;
    private State state = State.READY;
    private SafeWorldCoordinate working, result, supportOrigin;
    private SafeCandidateCursor candidate;
    private ViewSearchCursor view;
    private BlockPos pending;
    private ViewSearchCursor.Step viewStep;
    private EnumTeleportType type;
    private int range, retries;
    private boolean belowAir, viewSafe, viewFound, closed, ticketCancelled, pendingFallback;
    private BlockState support;
    private ResultKind kind;
    private Runnable cleanup;

    public NarcissusSearchRequest(UUID id, SafeWorldCoordinate origin, CommonSearchConfiguration.Snapshot config,
                                  Access access, SearchChunkPool.Lease lease, Consumer<NarcissusSearchRequest> callback) {
        this.id = Objects.requireNonNull(id);
        this.origin = origin.clone();
        this.config = Objects.requireNonNull(config);
        this.access = Objects.requireNonNull(access);
        this.lease = Objects.requireNonNull(lease);
        this.callback = Objects.requireNonNull(callback);
    }

    public void destination(SafeWorldCoordinate destination, EnumTeleportType type, int range) {
        if (closed) throw new IllegalStateException("Search is closed");
        this.type = type;
        this.range = range > 0 ? range : config.randomDistanceLimit();
        working = destination.clone();
        result = null;
        support = null;
        supportOrigin = null;
        view = null;
        pending = null;
        pendingFallback = false;
        retries = 0;
        belowAir = false;
        beginDestination();
    }

    private void beginDestination() {
        int offset = (config.safeChunkRange() - 1) * 16;
        int minX = (working.chunkX() << 4) - offset;
        int minZ = (working.chunkZ() << 4) - offset;
        SearchBox box = new SearchBox(minX, minX + 15 + offset, access.minY(), access.maxY(), minZ, minZ + 15 + offset);
        candidate = new SafeCandidateCursor(working.safeMode(), working.xInt(), working.yInt(), working.zInt(),
                box, working.safeMode() == EnumSafeMode.NONE ? access.heightFilter(box, belowAir) : null);
        state = State.READY;
    }

    public void view(double x, double y, double z, double dx, double dy, double dz, int range, boolean safe) {
        if (closed) throw new IllegalStateException("Search is closed");
        view = new ViewSearchCursor(x, y, z, dx, dy, dz, range, safe);
        viewSafe = safe;
        viewFound = false;
        pending = null;
        pendingFallback = false;
        result = null;
        state = State.READY;
    }

    public void waitForCountdown() {
        if (state != State.RESOLVED) throw new IllegalStateException("Search is not resolved");
        state = State.WAITING_COUNTDOWN;
    }

    public boolean complete(Runnable action) {
        if (closed || state != State.RESOLVED && state != State.WAITING_COUNTDOWN) return false;
        try {
            if (!live()) return abort(Failure.PLAYER_CHANGED);
            if (!policyMatches()) return abort(Failure.POLICY_CHANGED);
            access.beginSlice();
            if (!ready(result.toBlockPos())) return abort(Failure.ERROR);
            if (kind == ResultKind.FOUND || kind == ResultKind.VIEW_ENDPOINT && viewFound) {
                if (!access.safe(result.toBlockPos(), false)) return abort(Failure.POLICY_CHANGED);
            } else if (kind == ResultKind.SUPPORT_PLAN) {
                if (!access.safe(result.toBlockPos(), true) || !access.hasSupportItem(supportItem()))
                    return abort(Failure.POLICY_CHANGED);
            }
            if (!live()) return abort(Failure.PLAYER_CHANGED);
            if (!policyMatches()) return abort(Failure.POLICY_CHANGED);
            // Mark terminal before executing payment/teleport callbacks; they may throw or re-enter.
            state = State.COMMITTED;
            action.run();
            return true;
        } catch (RuntimeException | Error error) {
            if (state != State.COMMITTED) cancel(Failure.ERROR);
            throw error;
        } finally {
            close();
        }
    }

    private boolean abort(Failure reason) {
        cancel(reason);
        return false;
    }

    public SafeWorldCoordinate destination() {
        return result == null ? null : result.clone();
    }

    public ResultKind kind() {
        return kind;
    }

    public BlockState supportState() {
        return support;
    }

    public ItemStack supportItem() {
        return support != null && config.getBlockFromInventory() ? new ItemStack(support.getBlock()) : ItemStack.EMPTY;
    }

    public void callback(Consumer<NarcissusSearchRequest> callback) {
        this.callback = Objects.requireNonNull(callback);
    }

    public void onClose(Runnable action) {
        if (cleanup != null) throw new IllegalStateException("Countdown already attached");
        if (closed) action.run();
        else cleanup = Objects.requireNonNull(action);
    }

    public UUID playerId() {
        return id;
    }

    public boolean live() {
        return !closed && access.live();
    }

    public boolean policyMatches() {
        return config.matches() && access.policyMatches();
    }

    public State state() {
        return state;
    }

    public int step(int maxSteps) {
        if (closed || state == State.WAITING_COUNTDOWN || state == State.RESOLVED) return 0;
        access.beginSlice();
        if (view != null) view.beginSlice();
        state = State.SEARCHING;
        int consumed = 0;
        while (consumed < maxSteps && state == State.SEARCHING) {
            if (result != null) {
                BlockPos finalBlock = result.toBlockPos();
                if (!ready(finalBlock)) break;
                lease.retainFinal(finalBlock.getX() >> 4, finalBlock.getZ() >> 4);
                state = State.RESOLVED;
                callback.accept(this);
                break;
            }
            if (pendingFallback) {
                exhausted();
                continue;
            }
            if (pending == null) {
                consumed++;
                if (view != null) {
                    viewStep = view.advance();
                    if (viewStep == ViewSearchCursor.Step.DONE) {
                        SafeWorldCoordinate end = origin.clone();
                        end.x(view.resultX()).y(view.resultY()).z(view.resultZ());
                        end.safe(false);
                        end.safeMode(EnumSafeMode.Y_C_OFFSET_3);
                        if (origin.equalsInRange(end, 1)) {
                            cancel(Failure.CANCELLED);
                            access.viewNotFound(viewSafe);
                        } else {
                            result = end;
                            kind = ResultKind.VIEW_ENDPOINT;
                        }
                        continue;
                    }
                    if (viewStep == ViewSearchCursor.Step.SKIPPED) continue;
                    pending = new BlockPos(view.x(), view.y(), view.z());
                } else {
                    SafeCandidateCursor.Step next = candidate.advance();
                    if (next == SafeCandidateCursor.Step.DONE) {
                        exhausted();
                        continue;
                    }
                    if (next == SafeCandidateCursor.Step.SKIPPED) continue;
                    pending = new BlockPos(candidate.x(), candidate.y(), candidate.z());
                }
            }
            if (!ready(pending)) break;
            if (view != null) {
                boolean matched = viewStep == ViewSearchCursor.Step.MOTION ? access.blocksMotion(pending) : access.safe(pending, false);
                if (viewStep == ViewSearchCursor.Step.SAFETY && matched) viewFound = true;
                view.accept(matched);
            } else if (access.safe(pending, belowAir)) {
                SafeWorldCoordinate found = new SafeWorldCoordinate(pending.getX() + .5, pending.getY() + .15,
                        pending.getZ() + .5, working.dimension());
                if (belowAir) {
                    if (!found.xyzString().equals(supportOrigin.xyzString())) {
                        result = found;
                        kind = ResultKind.SUPPORT_PLAN;
                    } else {
                        result = supportOrigin;
                        support = null;
                        kind = ResultKind.EXHAUSTED_FALLBACK;
                    }
                } else {
                    result = found;
                    kind = ResultKind.FOUND;
                }
            }
            pending = null;
        }
        return consumed;
    }

    private boolean ready(BlockPos pos) {
        if (lease.require(pos.getX() >> 4, pos.getZ() >> 4) != SearchChunkPool.Availability.READY) {
            state = State.WAITING_CHUNK;
            return false;
        }
        return true;
    }

    private void exhausted() {
        if (belowAir) {
            result = supportOrigin;
            support = null;
            kind = ResultKind.EXHAUSTED_FALLBACK;
            return;
        }
        // Checking the original fallback itself also requires a ready lease.
        pending = working.toBlockPos();
        pendingFallback = true;
        if (!ready(pending)) return;
        pendingFallback = false;
        if (access.safe(pending, false)) {
            result = working.clone();
            kind = ResultKind.FOUND;
            pending = null;
            return;
        }
        pending = null;
        if (type == EnumTeleportType.TP_RANDOM && retries < config.randomRetries()) {
            retries++;
            working = access.random(origin.clone(), range).safe(true);
            beginDestination();
            state = State.SEARCHING;
        } else if (config.setBlockWhenSafeNotFound() && (support = access.supportState()) != null) {
            supportOrigin = working.clone();
            belowAir = true;
            beginDestination();
            state = State.SEARCHING;
        } else {
            result = working.clone();
            kind = ResultKind.EXHAUSTED_FALLBACK;
        }
    }

    public void cancel(Failure reason) {
        if (closed || state == State.COMMITTED || state == State.CANCELLED || state == State.FAILED) return;
        state = reason == Failure.CANCELLED || reason == Failure.SERVER_STOPPED || reason == Failure.PLAYER_CHANGED ? State.CANCELLED : State.FAILED;
        try {
            cancelTicket();
        } finally {
            if (reason != Failure.CANCELLED && reason != Failure.SERVER_STOPPED && reason != Failure.PLAYER_CHANGED)
                access.failed(reason);
        }
    }

    public void close() {
        if (closed) {
            lease.close();
            return;
        }
        closed = true;
        try {
            cancelTicket();
        } finally {
            try {
                Runnable release = cleanup;
                cleanup = null;
                if (release != null) release.run();
            } finally {
                lease.close();
            }
        }
    }

    private void cancelTicket() {
        if (!ticketCancelled) {
            ticketCancelled = true;
            access.cancelTicket();
        }
    }
}
