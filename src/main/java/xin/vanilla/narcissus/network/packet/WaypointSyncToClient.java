package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.banira.internal.network.BaniraStreamCodecs;
import xin.vanilla.narcissus.Identifier;
import xin.vanilla.narcissus.data.SafeWorldCoordinate;
import xin.vanilla.narcissus.integration.MapHelper;

@Getter
@Accessors(fluent = true)
public class WaypointSyncToClient implements CustomPacketPayload {

    public static final Type<WaypointSyncToClient> TYPE =
            new Type<>(Identifier.id().create("waypoint_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointSyncToClient> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(WaypointSyncToClient::toBytes, WaypointSyncToClient::new);

    public enum Action {
        ADD,
        REMOVE,
    }

    public enum Kind {
        HOME,
        STAGE,
    }

    private final Action action;
    private final Kind kind;
    private final String name;
    private final String dimension;
    private final double x;
    private final double y;
    private final double z;

    public WaypointSyncToClient(Action action, Kind kind, String name, SafeWorldCoordinate safeWorldCoordinate) {
        this.action = action;
        this.kind = kind;
        this.name = name;
        this.dimension = safeWorldCoordinate.dimensionId();
        this.x = safeWorldCoordinate.x();
        this.y = safeWorldCoordinate.y();
        this.z = safeWorldCoordinate.z();
    }

    public WaypointSyncToClient(FriendlyByteBuf buf) {
        this.action = buf.readEnum(Action.class);
        this.kind = buf.readEnum(Kind.class);
        this.name = buf.readUtf(32);
        this.dimension = buf.readUtf(256);
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeEnum(action);
        buf.writeEnum(kind);
        buf.writeUtf(name, 32);
        buf.writeUtf(dimension, 256);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WaypointSyncToClient packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.flow() == PacketFlow.CLIENTBOUND) {
                MapHelper.handle(packet);
            }
        });
    }
}
