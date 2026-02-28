package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.util.NarcissusUtils;
import xin.vanilla.narcissus.util.StringUtils;

import java.util.function.Supplier;


@Getter
@Accessors(fluent = true)
public class WaypointDelToServer {

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

    public WaypointDelToServer(PacketBuffer buf) {
        this.type = buf.readVarInt();
        this.name = buf.readUtf(MAX_NAME_LEN);
        this.dimension = buf.readUtf(MAX_DIMENSION_LEN);
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeVarInt(type);
        buf.writeUtf(name, MAX_NAME_LEN);
        buf.writeUtf(dimension, MAX_DIMENSION_LEN);
    }

    public static void handle(WaypointDelToServer packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                net.minecraft.entity.player.ServerPlayerEntity sender = ctx.get().getSender();
                if (sender == null) return;
                // home
                if (packet.type() == 0) {
                    String cmd = NarcissusUtils.getCommandPrefix() + " " + CommonConfig.COMMAND_DEL_HOME.get();
                    if (!packet.name().isEmpty()) cmd += " " + StringUtils.formatString(packet.name());
                    if (!packet.dimension().isEmpty()) cmd += " " + packet.dimension();
                    NarcissusUtils.executeCommand(sender, cmd);
                }
                // stage
                else if (packet.type() == 1) {
                    String cmd = NarcissusUtils.getCommandPrefix() + " " + CommonConfig.COMMAND_DEL_STAGE.get();
                    if (!packet.name().isEmpty()) cmd += " " + StringUtils.formatString(packet.name());
                    if (!packet.dimension().isEmpty()) cmd += " " + packet.dimension();
                    NarcissusUtils.executeCommand(sender, cmd);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
