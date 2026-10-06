package xin.vanilla.narcissus.search;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.narcissus.NarcissusComponent;

import java.util.UUID;

public interface SearchTask extends AutoCloseable {
    enum State implements IEnumDescribable {
        READY, WAITING_CHUNK, SEARCHING, RESOLVED, WAITING_COUNTDOWN, COMMITTED, CANCELLED, FAILED;

        @Override
        public Component enumDescription() {
            return NarcissusComponent.get().literal(name());
        }
    }

    enum Failure implements IEnumDescribable {
        BUSY, TIMEOUT, CANCELLED, POLICY_CHANGED, PLAYER_CHANGED, ERROR, SERVER_STOPPED;

        @Override
        public Component enumDescription() {
            return NarcissusComponent.get().literal(name());
        }
    }

    UUID playerId();

    boolean live();

    boolean policyMatches();

    State state();

    /**
     * Advance at most maxSteps bounded cursor operations; waiting may consume zero.
     */
    int step(int maxSteps);

    void cancel(Failure reason);

    @Override
    void close();
}
