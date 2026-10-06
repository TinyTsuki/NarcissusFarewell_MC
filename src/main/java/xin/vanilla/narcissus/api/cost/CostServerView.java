package xin.vanilla.narcissus.api.cost;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CostServerView {
    long tick();

    int onlinePlayerCount();

    Optional<CostPlayerView> player(UUID id);

    List<CostPlayerView> players();

    /**
     * Looks up existing worlds only.
     */
    Optional<CostWorldView> world(String dimensionId);

    <T> T nativeServer(Class<T> type);
}
