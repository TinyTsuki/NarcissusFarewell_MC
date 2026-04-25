package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.network.BaniraStreamCodecs;
import xin.vanilla.narcissus.Identifier;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.util.NarcissusUtils;

@Getter
@Accessors(fluent = true)
public class WaypointAddStageToServer implements NetworkPacket {

    public static final Type<WaypointAddStageToServer> TYPE =
            new Type<>(Identifier.id().create("waypoint_add_stage"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointAddStageToServer> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(WaypointAddStageToServer::toBytes, WaypointAddStageToServer::new);

    private static final int MAX_NAME_LEN = 64;
    private static final int MAX_DIMENSION_LEN = 256;

    private final String name;
    private final String dimension;
    private final double x;
    private final double y;
    private final double z;

    public WaypointAddStageToServer(String name, String dimension, double x, double y, double z) {
        this.name = name != null ? name : "";
        this.dimension = dimension != null ? dimension : "";
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public WaypointAddStageToServer(FriendlyByteBuf buf) {
        this.name = buf.readUtf(MAX_NAME_LEN);
        this.dimension = buf.readUtf(MAX_DIMENSION_LEN);
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(name, MAX_NAME_LEN);
        buf.writeUtf(dimension, MAX_DIMENSION_LEN);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WaypointAddStageToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sender)) {
                return;
            }
            String prefix = NarcissusUtils.getCommandPrefix();
            String cmd = prefix + " " + CommonConfig.get().commandNames().commandSetStage()
                    + " " + StringUtils.formatString(packet.name())
                    + " " + packet.x() + " " + packet.y() + " " + packet.z()
                    + " " + StringUtils.formatString(packet.dimension());
            CommandUtils.executeCommand(sender, cmd);
        });
    }
}
