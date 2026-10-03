package xin.vanilla.narcissus.api.cost;

import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class CostRequestInfo {
    String id;
    long createdAt;
    long expiresAt;
    boolean safe;
    boolean ignored;
}
