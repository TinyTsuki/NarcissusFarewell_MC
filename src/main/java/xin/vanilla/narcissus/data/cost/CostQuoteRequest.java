package xin.vanilla.narcissus.data.cost;

import lombok.Value;
import lombok.experimental.Accessors;
import java.util.Objects;
import java.util.UUID;

@Value
@Accessors(fluent = true)
public class CostQuoteRequest {
    UUID sessionId;
    long requestId, generationId;
    CostQuoteTarget target;
    public CostQuoteRequest(UUID sessionId, long requestId, long generationId, CostQuoteTarget target) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.target = Objects.requireNonNull(target, "target");
        if (requestId < 1 || generationId < 1) throw new IllegalArgumentException("Invalid quote request identity");
        this.requestId = requestId; this.generationId = generationId;
    }
}
