package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import xin.vanilla.narcissus.data.TeleportCost;
import xin.vanilla.narcissus.data.client.ClientCostConfig;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;


@Getter
@Accessors(fluent = true)
public class CostConfigSyncToClient {

    private static final int MAX_EXP_LEN = 256;

    private final Map<EnumTeleportType, TeleportCost> costMap;
    private final int distanceLimit;
    private final int distanceAcrossDimension;

    public CostConfigSyncToClient(Map<EnumTeleportType, TeleportCost> costMap, int distanceLimit, int distanceAcrossDimension) {
        this.costMap = costMap != null ? new HashMap<>(costMap) : new HashMap<>();
        this.distanceLimit = distanceLimit;
        this.distanceAcrossDimension = distanceAcrossDimension;
    }

    public CostConfigSyncToClient(FriendlyByteBuf buf) {
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

    private static TeleportCost readCost(FriendlyByteBuf buf) {
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

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(costMap.size());
        for (Map.Entry<EnumTeleportType, TeleportCost> entry : costMap.entrySet()) {
            buf.writeEnum(entry.getKey());
            writeCost(buf, entry.getValue());
        }
        buf.writeVarInt(distanceLimit);
        buf.writeVarInt(distanceAcrossDimension);
    }

    private static void writeCost(FriendlyByteBuf buf, TeleportCost cost) {
        buf.writeUtf(cost.getType() != null ? cost.getType().name() : "NONE", 32);
        buf.writeVarInt(cost.getNum());
        buf.writeDouble(cost.getRate());
        buf.writeVarInt(cost.getLower());
        buf.writeVarInt(cost.getUpper());
        buf.writeUtf(cost.getExp() != null ? cost.getExp() : "", MAX_EXP_LEN);
    }

    public static void handle(CostConfigSyncToClient packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientCostConfig.clear();
                    for (Map.Entry<EnumTeleportType, TeleportCost> entry : packet.costMap().entrySet()) {
                        ClientCostConfig.setCost(entry.getKey(), entry.getValue());
                    }
                    ClientCostConfig.setDistanceLimit(packet.distanceLimit());
                    ClientCostConfig.setDistanceAcrossDimension(packet.distanceAcrossDimension());
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
