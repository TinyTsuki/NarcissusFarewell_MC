package xin.vanilla.narcissus.internal.server.dev;

/**
 * Dedicated-server teleport workload order. The actual game operations stay in
 * {@link NarcissusNetworkSmokeServerRunner}; this class only keeps the async
 * completion contract deterministic and unit-testable.
 */
public final class NarcissusTeleportSmokeWorkload {
    private static final int SAFE_RANDOM_TELEPORTS = 3;

    private Step current = Step.SAFE_RANDOM;
    private int safeRandomTeleports;

    public Step current() {
        return current;
    }

    public void completeCurrent() {
        switch (current) {
            case SAFE_RANDOM:
                if (++safeRandomTeleports >= SAFE_RANDOM_TELEPORTS) {
                    current = Step.VIEW_END;
                }
                return;
            case VIEW_END:
                current = Step.CROSS_DIMENSION_FOLLOWER;
                return;
            case CROSS_DIMENSION_FOLLOWER:
                current = Step.COMPLETE;
                return;
            case COMPLETE:
                return;
            default:
                throw new IllegalStateException("Unknown teleport smoke step " + current);
        }
    }

    public enum Step {
        SAFE_RANDOM,
        VIEW_END,
        CROSS_DIMENSION_FOLLOWER,
        COMPLETE
    }
}
