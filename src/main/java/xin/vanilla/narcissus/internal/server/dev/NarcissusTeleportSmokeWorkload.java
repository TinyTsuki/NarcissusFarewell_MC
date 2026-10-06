package xin.vanilla.narcissus.internal.server.dev;

/**
 * Dedicated-server teleport workload order. The actual game operations stay in
 * {@link NarcissusNetworkSmokeServerRunner}; this class only keeps the async
 * completion contract deterministic and unit-testable.
 */
public final class NarcissusTeleportSmokeWorkload {
    static final int REQUIRED_CYCLES = 20;

    private Step current = Step.SAFE_RANDOM;
    private int cycles;

    public int cycles() {
        return cycles;
    }

    public Step current() {
        return current;
    }

    public void completeCurrent() {
        switch (current) {
            case SAFE_RANDOM:
                current = Step.VIEW_END;
                return;
            case VIEW_END:
                current = Step.CROSS_DIMENSION_FOLLOWER;
                return;
            case CROSS_DIMENSION_FOLLOWER:
                current = ++cycles == REQUIRED_CYCLES ? Step.COMPLETE : Step.SAFE_RANDOM;
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
