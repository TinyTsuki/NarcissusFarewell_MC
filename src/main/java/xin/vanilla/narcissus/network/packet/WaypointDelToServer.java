package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.SimpleChannel;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.network.NetworkInit;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.function.Supplier;


@Getter
@Accessors(fluent = true)
public class WaypointDelToServer implements NetworkPacket {

    @Override
    public Supplier<SimpleChannel> channel() {
        return () -> NetworkInit.INSTANCE;
    }

    private static final int MAX_NAME_LEN = 64;
    private static final int MAX_DIMENSION_LEN = 256;

    private final int type;
    private final String name;
    private final String dimension;

    public WaypointDelToServer(int type, String name, String dimension) {
        this.type = type;
        this.name = name != null ? name : "";
        this.dimension = dimension != null ? dimension : "";
    }

    public WaypointDelToServer(FriendlyByteBuf buf) {
        this.type = buf.readVarInt();
        this.name = buf.readUtf(MAX_NAME_LEN);
        this.dimension = buf.readUtf(MAX_DIMENSION_LEN);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(type);
        buf.writeUtf(name, MAX_NAME_LEN);
        buf.writeUtf(dimension, MAX_DIMENSION_LEN);
    }

    public static void handle(WaypointDelToServer packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.isServerSide()) {
                ServerPlayer sender = ctx.getSender();
                if (sender == null) return;
                // home
                if (packet.type() == 0) {
                    String cmd = NarcissusUtils.getCommandPrefix() + " " + CommonConfig.get().commandNames().commandDelHome();
                    if (!packet.name().isEmpty()) cmd += " " + StringUtils.formatString(packet.name());
                    if (!packet.dimension().isEmpty()) cmd += " " + packet.dimension();
                    CommandUtils.executeCommand(sender, cmd);
                }
                // stage
                else if (packet.type() == 1) {
                    String cmd = NarcissusUtils.getCommandPrefix() + " " + CommonConfig.get().commandNames().commandDelStage();
                    if (!packet.name().isEmpty()) cmd += " " + StringUtils.formatString(packet.name());
                    if (!packet.dimension().isEmpty()) cmd += " " + packet.dimension();
                    CommandUtils.executeCommand(sender, cmd);
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
