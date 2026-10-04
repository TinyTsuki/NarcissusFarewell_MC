package xin.vanilla.narcissus.api.cost;

import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.Optional;

/** Borrowed views are valid only during this calculation on its owning server thread. */
public interface CostContext {
    long operationId();
    long generationId();
    CostPhase phase();
    EnumTeleportType teleportType();
    EnumCostType costType();
    CostPlayerView player();
    CostPlayerView payer();
    Optional<CostPlayerView> requester();
    Optional<CostPlayerView> targetPlayer();
    Optional<CostRequestInfo> request();
    CostPosition source();
    Optional<CostPosition> destination();
    double rawDistance();
    double distance();
    boolean crossDimension();
    CostParameters parameters();
    CostCardSettings cardSettings();
    int cooldownSeconds();
    int countdownSeconds();
    CostWorldView sourceWorld();
    Optional<CostWorldView> targetWorld();
    CostServerView server();

    default <T> T nativePlayer(Class<T> type) { return player().nativePlayer(type); }
    default <T> T nativePayer(Class<T> type) { return payer().nativePlayer(type); }
    default <T> T nativeSourceWorld(Class<T> type) { return sourceWorld().nativeWorld(type); }
    default <T> Optional<T> nativeTargetWorld(Class<T> type) { return targetWorld().map(world -> world.nativeWorld(type)); }
    default <T> T nativeServer(Class<T> type) { return server().nativeServer(type); }
}
