package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.integration.MapHelper;
import xin.vanilla.narcissus.network.NetworkPacket;


@Getter
@Accessors(fluent = true)
public class WaypointSyncToClient implements NetworkPacket {

    public enum Action {
        ADD,
        REMOVE,
    }

    public enum Type {
        HOME,
        STAGE,
    }

    private final Action action;
    private final Type type;
    private final String name;
    private final String dimension;
    private final double x;
    private final double y;
    private final double z;

    public WaypointSyncToClient(Action action, Type type, String name, SafeWorldCoordinate safeWorldCoordinate) {
        this.action = action;
        this.type = type;
        this.name = name;
        this.dimension = safeWorldCoordinate.dimensionId();
        this.x = safeWorldCoordinate.x();
        this.y = safeWorldCoordinate.y();
        this.z = safeWorldCoordinate.z();
    }

    public WaypointSyncToClient(BaniraPacketBuffer buf) {
        this.action = buf.readEnum(Action.class);
        this.type = buf.readEnum(Type.class);
        this.name = buf.readUtf(32);
        this.dimension = buf.readUtf(256);
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void toBytes(BaniraPacketBuffer buf) {
        buf.writeEnum(action);
        buf.writeEnum(type);
        buf.writeUtf(name, 32);
        buf.writeUtf(dimension, 256);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public static void handle(WaypointSyncToClient packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.isClientSide()) {
                MapHelper.handle(packet);
            }
        });
        ctx.markHandled();
    }
}
