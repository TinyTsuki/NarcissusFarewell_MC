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
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.banira.internal.network.BaniraStreamCodecs;
import xin.vanilla.narcissus.Identifier;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.network.NetworkPacket;
import xin.vanilla.narcissus.util.NarcissusUtils;

@Getter
@Accessors(fluent = true)
public class WaypointDelToServer implements NetworkPacket {

    public static final Type<WaypointDelToServer> TYPE =
            new Type<>(Identifier.id().create("waypoint_del"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointDelToServer> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(WaypointDelToServer::toBytes, WaypointDelToServer::new);

    private static final int MAX_NAME_LEN = 64;
    private static final int MAX_DIMENSION_LEN = 256;

    private final int delKind;
    private final String name;
    private final String dimension;

    public WaypointDelToServer(int delKind, String name, String dimension) {
        this.delKind = delKind;
        this.name = name != null ? name : "";
        this.dimension = dimension != null ? dimension : "";
    }

    public WaypointDelToServer(FriendlyByteBuf buf) {
        this.delKind = buf.readVarInt();
        this.name = buf.readUtf(MAX_NAME_LEN);
        this.dimension = buf.readUtf(MAX_DIMENSION_LEN);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(delKind);
        buf.writeUtf(name, MAX_NAME_LEN);
        buf.writeUtf(dimension, MAX_DIMENSION_LEN);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WaypointDelToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sender)) {
                return;
            }
            if (packet.delKind() == 0) {
                String cmd = NarcissusUtils.getCommandPrefix() + " " + CommonConfig.get().commandNames().commandDelHome();
                if (!packet.name().isEmpty()) cmd += " " + StringUtils.formatString(packet.name());
                if (!packet.dimension().isEmpty()) cmd += " " + packet.dimension();
                CommandUtils.executeCommand(sender, cmd);
            } else if (packet.delKind() == 1) {
                String cmd = NarcissusUtils.getCommandPrefix() + " " + CommonConfig.get().commandNames().commandDelStage();
                if (!packet.name().isEmpty()) cmd += " " + StringUtils.formatString(packet.name());
                if (!packet.dimension().isEmpty()) cmd += " " + packet.dimension();
                CommandUtils.executeCommand(sender, cmd);
            }
        });
    }
}
