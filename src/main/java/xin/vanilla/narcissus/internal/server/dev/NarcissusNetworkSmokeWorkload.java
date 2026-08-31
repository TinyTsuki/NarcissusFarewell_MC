package xin.vanilla.narcissus.internal.server.dev;

/** Controls the bounded sustained coordinate-search segment of the dev-only smoke. */
final class NarcissusNetworkSmokeWorkload {
    static final int DURATION_TICKS = 320;

    private final int startedAt;

    NarcissusNetworkSmokeWorkload(int startedAt) {
        this.startedAt = startedAt;
    }

    boolean completeAt(int currentTick) {
        return currentTick - startedAt >= DURATION_TICKS;
    }
}
