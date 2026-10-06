package xin.vanilla.narcissus.data.cost;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import xin.vanilla.narcissus.api.cost.*;
import xin.vanilla.narcissus.enums.EnumTeleportType;

/**
 * Internal calculation input; raw views must never be handed directly to scripts.
 */
@Value
@Builder
@Accessors(fluent = true)
public class CostContextInput {
    long operationId;
    long generationId;
    CostPhase phase;
    EnumTeleportType teleportType;
    CostParameters parameters;
    CostCardSettings cardSettings;
    CostPlayerView player;
    CostPlayerView payer;
    CostPlayerView requester;
    CostPlayerView targetPlayer;
    CostRequestInfo request;
    CostPosition source;
    CostPosition destination;
    double rawDistance;
    double distance;
    int cooldownSeconds;
    int countdownSeconds;
    CostWorldView sourceWorld;
    CostWorldView targetWorld;
    CostServerView server;
}
