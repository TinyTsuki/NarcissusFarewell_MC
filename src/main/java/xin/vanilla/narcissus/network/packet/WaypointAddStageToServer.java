package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.StringUtils;
import xin.vanilla.narcissus.config.CommonConfig;
import xin.vanilla.narcissus.network.NetworkPacket;
import xin.vanilla.narcissus.util.NarcissusUtils;


@Getter
@Accessors(fluent = true)
public class WaypointAddStageToServer implements NetworkPacket {

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

    public WaypointAddStageToServer(BaniraPacketBuffer buf) {
        this.name = buf.readUtf(MAX_NAME_LEN);
        this.dimension = buf.readUtf(MAX_DIMENSION_LEN);
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void toBytes(BaniraPacketBuffer buf) {
        buf.writeUtf(name, MAX_NAME_LEN);
        buf.writeUtf(dimension, MAX_DIMENSION_LEN);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public static void handle(WaypointAddStageToServer packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (!ctx.isServerSide()) {
                return;
            }
            net.minecraft.server.level.ServerPlayer sender = ctx.senderAs(net.minecraft.server.level.ServerPlayer.class);
            if (sender == null) {
                return;
            }
            String prefix = NarcissusUtils.getCommandPrefix();
            String cmd = prefix + " " + CommonConfig.get().commandNames().commandSetStage()
                    + " " + StringUtils.formatString(packet.name())
                    + " " + packet.x() + " " + packet.y() + " " + packet.z()
                    + " " + StringUtils.formatString(packet.dimension());
            CommandUtils.executeCommand(sender, cmd);
        });
        ctx.markHandled();
    }
}
