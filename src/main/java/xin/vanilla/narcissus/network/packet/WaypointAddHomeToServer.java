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
public class WaypointAddHomeToServer implements NetworkPacket {

    private static final int MAX_NAME_LEN = 64;

    private final String name;

    public WaypointAddHomeToServer(String name) {
        this.name = name != null ? name : "";
    }

    public WaypointAddHomeToServer(BaniraPacketBuffer buf) {
        this.name = buf.readUtf(MAX_NAME_LEN);
    }

    public void toBytes(BaniraPacketBuffer buf) {
        buf.writeUtf(name, MAX_NAME_LEN);
    }

    public static void handle(WaypointAddHomeToServer packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (!ctx.isServerSide()) {
                return;
            }
            net.minecraft.entity.player.ServerPlayerEntity sender = ctx.senderAs(net.minecraft.entity.player.ServerPlayerEntity.class);
            if (sender == null) {
                return;
            }
            String prefix = NarcissusUtils.getCommandPrefix();
            String cmd = prefix + " " + CommonConfig.get().commandNames().commandSetHome();
            if (!packet.name().isEmpty()) {
                cmd += " " + StringUtils.formatString(packet.name());
            }
            CommandUtils.executeCommand(sender, cmd);
        });
        ctx.markHandled();
    }
}
