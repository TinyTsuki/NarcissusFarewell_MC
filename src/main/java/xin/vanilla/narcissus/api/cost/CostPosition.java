package xin.vanilla.narcissus.api.cost;

import lombok.Value;
import lombok.experimental.Accessors;
import xin.vanilla.narcissus.enums.EnumSafeMode;

import java.util.Objects;

@Value
@Accessors(fluent = true)
public class CostPosition {
    String dimensionId;
    double x;
    double y;
    double z;
    double yaw;
    double pitch;
    boolean safe;
    EnumSafeMode safeMode;

    public CostPosition(String dimensionId, double x, double y, double z, double yaw, double pitch,
                        boolean safe, EnumSafeMode safeMode) {
        if (Objects.requireNonNull(dimensionId, "dimensionId").isEmpty()
                || !Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || !Double.isFinite(yaw) || !Double.isFinite(pitch)) {
            throw new IllegalArgumentException("Invalid cost position");
        }
        this.dimensionId = dimensionId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.safe = safe;
        this.safeMode = Objects.requireNonNull(safeMode, "safeMode");
    }
}
