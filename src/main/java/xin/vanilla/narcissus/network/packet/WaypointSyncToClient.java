package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import xin.vanilla.narcissus.data.Coordinate;
import xin.vanilla.narcissus.integration.MapHelper;


@Getter
@Accessors(fluent = true)
public class WaypointSyncToClient {

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

    public WaypointSyncToClient(Action action, Type type, String name, Coordinate coordinate) {
        this.action = action;
        this.type = type;
        this.name = name;
        this.dimension = coordinate.getDimensionResourceId();
        this.x = coordinate.x();
        this.y = coordinate.y();
        this.z = coordinate.z();
    }

    public WaypointSyncToClient(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.type = buf.readEnum(Type.class);
        this.name = buf.readUtf(32);
        this.dimension = buf.readUtf(256);
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeEnum(type);
        buf.writeUtf(name, 32);
        buf.writeUtf(dimension, 256);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public static void handle(WaypointSyncToClient packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.isClientSide()) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MapHelper.handle(packet));
            }
        });
        ctx.setPacketHandled(true);
    }
}
