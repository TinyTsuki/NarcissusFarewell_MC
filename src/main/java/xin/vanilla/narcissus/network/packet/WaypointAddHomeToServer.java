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
public class WaypointAddHomeToServer implements NetworkPacket {

    public static final Type<WaypointAddHomeToServer> TYPE =
            new Type<>(Identifier.id().create("waypoint_add_home"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WaypointAddHomeToServer> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(WaypointAddHomeToServer::toBytes, WaypointAddHomeToServer::new);

    private static final int MAX_NAME_LEN = 64;

    private final String name;

    public WaypointAddHomeToServer(String name) {
        this.name = name != null ? name : "";
    }

    public WaypointAddHomeToServer(FriendlyByteBuf buf) {
        this.name = buf.readUtf(MAX_NAME_LEN);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUtf(name, MAX_NAME_LEN);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WaypointAddHomeToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sender)) {
                return;
            }
            String prefix = NarcissusUtils.getCommandPrefix();
            String cmd = prefix + " " + CommonConfig.get().commandNames().commandSetHome();
            if (!packet.name().isEmpty()) {
                cmd += " " + StringUtils.formatString(packet.name());
            }
            CommandUtils.executeCommand(sender, cmd);
        });
    }
}
