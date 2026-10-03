package xin.vanilla.narcissus.api.cost;

import java.util.UUID;

public interface CostPlayerView {
    UUID uuid();
    String name();
    CostPosition position();
    int experiencePoints();
    int experienceLevels();
    float health();
    float maxHealth();
    int foodLevel();
    int teleportCards();
    boolean alive();
    boolean removed();
    boolean creative();
    boolean spectator();
    /** Native references are trusted mutable objects, not revocable or sandboxed. */
    <T> T nativePlayer(Class<T> type);
    <T> T nativeTeleportData(Class<T> type);
}
