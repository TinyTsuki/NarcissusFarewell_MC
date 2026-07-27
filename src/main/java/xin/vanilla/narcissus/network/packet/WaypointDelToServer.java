package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.network.NetworkPacket;
import xin.vanilla.narcissus.util.NarcissusUtils;


@Getter
@Accessors(fluent = true)
public class WaypointDelToServer implements NetworkPacket {

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

    public WaypointDelToServer(BaniraPacketBuffer buf) {
        this.type = buf.readVarInt();
        this.name = buf.readUtf(MAX_NAME_LEN);
        this.dimension = buf.readUtf(MAX_DIMENSION_LEN);
    }

    public void toBytes(BaniraPacketBuffer buf) {
        buf.writeVarInt(type);
        buf.writeUtf(name, MAX_NAME_LEN);
        buf.writeUtf(dimension, MAX_DIMENSION_LEN);
    }

    public static void handle(WaypointDelToServer packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.isServerSide()) {
                net.minecraft.entity.player.ServerPlayerEntity sender = ctx.senderAs(net.minecraft.entity.player.ServerPlayerEntity.class);
                if (sender == null) return;
                // home
                if (packet.type() == 0) {
                    String cmd = NarcissusUtils.getCommandPrefix() + " " + CommonConfig.get().command().commandDelHome();
                    if (!packet.name().isEmpty()) cmd += " " + StringUtils.formatString(packet.name());
                    if (!packet.dimension().isEmpty()) cmd += " " + packet.dimension();
                    CommandUtils.executeCommand(sender, cmd);
                }
                // stage
                else if (packet.type() == 1) {
                    String cmd = NarcissusUtils.getCommandPrefix() + " " + CommonConfig.get().command().commandDelStage();
                    if (!packet.name().isEmpty()) cmd += " " + StringUtils.formatString(packet.name());
                    if (!packet.dimension().isEmpty()) cmd += " " + packet.dimension();
                    CommandUtils.executeCommand(sender, cmd);
                }
            }
        });
        ctx.markHandled();
    }
}
