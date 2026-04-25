package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.SimpleChannel;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.client.ClientStageData;
import xin.vanilla.narcissus.network.NetworkInit;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;


@Getter
@Accessors(fluent = true)
public class StageDataSyncToClient implements NetworkPacket {

    @Override
    public Supplier<SimpleChannel> channel() {
        return () -> NetworkInit.INSTANCE;
    }

    private static final int MAX_NAME_LEN = 64;
    private static final int MAX_DIMENSION_LEN = 256;

    private final Map<KeyValue<String, String>, SafeWorldCoordinate> stageCoordinate;

    public StageDataSyncToClient(Map<KeyValue<String, String>, SafeWorldCoordinate> stageCoordinate) {
        this.stageCoordinate = stageCoordinate != null ? new LinkedHashMap<>(stageCoordinate) : new LinkedHashMap<>();
    }

    public StageDataSyncToClient(FriendlyByteBuf buf) {
        int count = buf.readVarInt();
        Map<KeyValue<String, String>, SafeWorldCoordinate> map = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            String dimension = buf.readUtf(MAX_DIMENSION_LEN);
            String name = buf.readUtf(MAX_NAME_LEN);
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            SafeWorldCoordinate coord = new SafeWorldCoordinate(x, y, z, dimension);
            map.put(new KeyValue<>(dimension, name), coord);
        }
        this.stageCoordinate = map;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(stageCoordinate.size());
        for (Map.Entry<KeyValue<String, String>, SafeWorldCoordinate> entry : stageCoordinate.entrySet()) {
            buf.writeUtf(entry.getKey().key(), MAX_DIMENSION_LEN);
            buf.writeUtf(entry.getKey().value(), MAX_NAME_LEN);
            buf.writeDouble(entry.getValue().x());
            buf.writeDouble(entry.getValue().y());
            buf.writeDouble(entry.getValue().z());
        }
    }

    public static void handle(StageDataSyncToClient packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.isClientSide()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                    ClientStageData.setStageCoordinate(packet.stageCoordinate());
                });
            }
        });
        ctx.setPacketHandled(true);
    }
}
