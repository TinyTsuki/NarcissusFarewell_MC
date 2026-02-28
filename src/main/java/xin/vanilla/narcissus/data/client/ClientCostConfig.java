package xin.vanilla.narcissus.data.client;

import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import xin.vanilla.narcissus.data.TeleportCost;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.EnumMap;
import java.util.Map;


@OnlyIn(Dist.CLIENT)
public final class ClientCostConfig {
    private static final Map<EnumTeleportType, TeleportCost> COST_MAP = new EnumMap<>(EnumTeleportType.class);
    @Getter
    @Setter
    private static int distanceLimit = 10000;
    @Getter
    @Setter
    private static int distanceAcrossDimension = 10000;

    private ClientCostConfig() {
    }

    public static void setCost(EnumTeleportType type, TeleportCost cost) {
        COST_MAP.put(type, cost);
    }

    public static TeleportCost getCost(EnumTeleportType type) {
        return COST_MAP.get(type);
    }

    public static void clear() {
        COST_MAP.clear();
    }
}
