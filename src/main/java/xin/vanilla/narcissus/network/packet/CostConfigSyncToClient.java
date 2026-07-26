package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.narcissus.data.TeleportCost;
import xin.vanilla.narcissus.data.client.ClientCostConfig;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.network.NetworkPacket;

import java.util.HashMap;
import java.util.Map;


@Getter
@Accessors(fluent = true)
public class CostConfigSyncToClient implements NetworkPacket {

    private static final int MAX_EXP_LEN = 256;

    private final Map<EnumTeleportType, TeleportCost> costMap;
    private final int distanceLimit;
    private final int distanceAcrossDimension;

    public CostConfigSyncToClient(Map<EnumTeleportType, TeleportCost> costMap, int distanceLimit, int distanceAcrossDimension) {
        this.costMap = costMap != null ? new HashMap<>(costMap) : new HashMap<>();
        this.distanceLimit = distanceLimit;
        this.distanceAcrossDimension = distanceAcrossDimension;
    }

    public CostConfigSyncToClient(BaniraPacketBuffer buf) {
        int count = buf.readVarInt();
        Map<EnumTeleportType, TeleportCost> map = new HashMap<>();
        for (int i = 0; i < count; i++) {
            EnumTeleportType type = buf.readEnum(EnumTeleportType.class);
            TeleportCost cost = readCost(buf);
            map.put(type, cost);
        }
        this.costMap = map;
        this.distanceLimit = buf.readVarInt();
        this.distanceAcrossDimension = buf.readVarInt();
    }

    private static TeleportCost readCost(BaniraPacketBuffer buf) {
        TeleportCost cost = new TeleportCost();
        String typeName = buf.readUtf(32);
        try {
            cost.setType(EnumCostType.valueOf(typeName));
        } catch (IllegalArgumentException ignored) {
            cost.setType(EnumCostType.NONE);
        }
        cost.setNum(buf.readVarInt());
        cost.setRate(buf.readDouble());
        cost.setLower(buf.readVarInt());
        cost.setUpper(buf.readVarInt());
        cost.setExp(buf.readUtf(MAX_EXP_LEN));
        return cost;
    }

    public void toBytes(BaniraPacketBuffer buf) {
        buf.writeVarInt(costMap.size());
        for (Map.Entry<EnumTeleportType, TeleportCost> entry : costMap.entrySet()) {
            buf.writeEnum(entry.getKey());
            writeCost(buf, entry.getValue());
        }
        buf.writeVarInt(distanceLimit);
        buf.writeVarInt(distanceAcrossDimension);
    }

    private static void writeCost(BaniraPacketBuffer buf, TeleportCost cost) {
        buf.writeUtf(cost.getType() != null ? cost.getType().name() : "NONE", 32);
        buf.writeVarInt(cost.getNum());
        buf.writeDouble(cost.getRate());
        buf.writeVarInt(cost.getLower());
        buf.writeVarInt(cost.getUpper());
        buf.writeUtf(cost.getExp() != null ? cost.getExp() : "", MAX_EXP_LEN);
    }

    public static void handle(CostConfigSyncToClient packet, BaniraNetworkContext ctx) {
        if (ctx.isClientSide()) {
            ctx.enqueueWork(() -> ClientSide.handle(packet));
        }
        ctx.markHandled();
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientSide {
        private static void handle(CostConfigSyncToClient packet) {
            ClientCostConfig.clear();
            for (Map.Entry<EnumTeleportType, TeleportCost> entry : packet.costMap().entrySet()) {
                ClientCostConfig.setCost(entry.getKey(), entry.getValue());
            }
            ClientCostConfig.setDistanceLimit(packet.distanceLimit());
            ClientCostConfig.setDistanceAcrossDimension(packet.distanceAcrossDimension());
        }
    }
}
