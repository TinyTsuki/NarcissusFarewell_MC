package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.data.KeyValue;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.util.PacketUtils;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.data.WaypointOrder;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.data.world.WorldStageData;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.network.NetworkPacket;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 客户端只提交键顺序，坐标值始终取服务端权威数据。 */
@Getter
@Accessors(fluent = true)
public final class WaypointReorderToServer implements NetworkPacket {
    private static final int MAX_ENTRIES = 4096;
    private static final int MAX_NAME_LEN = 64;
    private static final int MAX_DIMENSION_LEN = 256;

    public enum Type { HOME, STAGE }

    private final Type type;
    private final List<KeyValue<String, String>> orderedKeys;

    public WaypointReorderToServer(Type type, List<KeyValue<String, String>> orderedKeys) {
        this.type = type;
        this.orderedKeys = orderedKeys != null ? new ArrayList<>(orderedKeys) : new ArrayList<>();
    }

    public WaypointReorderToServer(BaniraPacketBuffer buf) {
        this.type = buf.readEnum(Type.class);
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_ENTRIES) {
            throw new IllegalArgumentException("Invalid waypoint order size: " + size);
        }
        this.orderedKeys = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            orderedKeys.add(new KeyValue<>(buf.readUtf(MAX_DIMENSION_LEN), buf.readUtf(MAX_NAME_LEN)));
        }
    }

    public void toBytes(BaniraPacketBuffer buf) {
        buf.writeEnum(type);
        buf.writeVarInt(orderedKeys.size());
        for (KeyValue<String, String> key : orderedKeys) {
            buf.writeUtf(key.key(), MAX_DIMENSION_LEN);
            buf.writeUtf(key.value(), MAX_NAME_LEN);
        }
    }

    public static void handle(WaypointReorderToServer packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (!ctx.isServerSide()) return;
            ServerPlayerEntity sender = ctx.senderAs(ServerPlayerEntity.class);
            if (sender == null) return;
            if (packet.type == Type.HOME) {
                PlayerTeleportData data = PlayerTeleportData.getData(sender);
                LinkedHashMap<KeyValue<String, String>, SafeWorldCoordinate> reordered =
                        WaypointOrder.validated(data.getHomeCoordinate(), packet.orderedKeys);
                if (reordered != null) {
                    data.setHomeCoordinate(reordered);
                    PlayerTeleportData.syncPlayerData(sender);
                }
                return;
            }
            if (!NarcissusUtils.hasCommandPermission(sender.createCommandSourceStack(), EnumCommandType.SET_STAGE)) {
                return;
            }
            WorldStageData data = WorldStageData.get(sender);
            LinkedHashMap<KeyValue<String, String>, SafeWorldCoordinate> reordered =
                    WaypointOrder.validated(data.getStageCoordinate(), packet.orderedKeys);
            if (reordered != null) {
                data.setCoordinate(reordered);
                PacketUtils.broadcastPacket(new StageDataSyncToClient(reordered));
            }
        });
        ctx.markHandled();
    }

}
