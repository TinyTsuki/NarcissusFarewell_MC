package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.data.KeyValue;
import xin.vanilla.narcissus.data.client.ClientStageData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;


@Getter
@Accessors(fluent = true)
public class StageDataSyncToClient {

    private static final int MAX_NAME_LEN = 64;
    private static final int MAX_DIMENSION_LEN = 256;

    private final Map<KeyValue<String, String>, Coordinate> stageCoordinate;

    public StageDataSyncToClient(Map<KeyValue<String, String>, Coordinate> stageCoordinate) {
        this.stageCoordinate = stageCoordinate != null ? new LinkedHashMap<>(stageCoordinate) : new LinkedHashMap<>();
    }

    public StageDataSyncToClient(PacketBuffer buf) {
        int count = buf.readVarInt();
        Map<KeyValue<String, String>, Coordinate> map = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String dimension = buf.readUtf(MAX_DIMENSION_LEN);
            String name = buf.readUtf(MAX_NAME_LEN);
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            Coordinate coord = new Coordinate(x, y, z, dimension);
            map.put(new KeyValue<>(dimension, name), coord);
        }
        this.stageCoordinate = map;
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeVarInt(stageCoordinate.size());
        for (Map.Entry<KeyValue<String, String>, Coordinate> entry : stageCoordinate.entrySet()) {
            buf.writeUtf(entry.getKey().key(), MAX_DIMENSION_LEN);
            buf.writeUtf(entry.getKey().value(), MAX_NAME_LEN);
            buf.writeDouble(entry.getValue().x());
            buf.writeDouble(entry.getValue().y());
            buf.writeDouble(entry.getValue().z());
        }
    }

    public static void handle(StageDataSyncToClient packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientStageData.setStageCoordinate(packet.stageCoordinate());
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
