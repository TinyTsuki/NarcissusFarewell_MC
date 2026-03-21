package xin.vanilla.narcissus.data.client;

import xin.vanilla.narcissus.data.TeleportCost;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.EnumMap;
import java.util.Map;

/**
 * 客户端缓存的传送代价（由 {@link xin.vanilla.narcissus.network.packet.CostConfigSyncToClient} 同步）。
 */
public final class ClientCostConfig {

    private static final Map<EnumTeleportType, TeleportCost> COSTS = new EnumMap<>(EnumTeleportType.class);
    private static int distanceLimit;
    private static int distanceAcrossDimension;

    private ClientCostConfig() {
    }

    public static void clear() {
        COSTS.clear();
        distanceLimit = 0;
        distanceAcrossDimension = 0;
    }

    public static void setCost(EnumTeleportType type, TeleportCost cost) {
        if (type != null && cost != null) {
            COSTS.put(type, cost);
        }
    }

    public static TeleportCost getCost(EnumTeleportType type) {
        return COSTS.get(type);
    }

    public static void setDistanceLimit(int limit) {
        distanceLimit = limit;
    }

    public static int getDistanceLimit() {
        return distanceLimit;
    }

    public static void setDistanceAcrossDimension(int value) {
        distanceAcrossDimension = value;
    }

    public static int getDistanceAcrossDimension() {
        return distanceAcrossDimension;
    }
}
