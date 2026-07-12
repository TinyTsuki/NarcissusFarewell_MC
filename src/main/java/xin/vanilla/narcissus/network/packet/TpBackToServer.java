package xin.vanilla.narcissus.network.packet;

import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.network.NetworkPacket;
import xin.vanilla.narcissus.util.NarcissusUtils;


public class TpBackToServer implements NetworkPacket {

    public TpBackToServer() {
    }

    public TpBackToServer(BaniraPacketBuffer buf) {
    }

    public void toBytes(BaniraPacketBuffer buf) {
    }

    public static void handle(TpBackToServer packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.senderAs(net.minecraft.server.level.ServerPlayer.class);
            if (player != null) {
                CommandUtils.executeCommand(player, NarcissusUtils.getCommand(EnumCommandType.TP_BACK));
            }
        });
        ctx.markHandled();
    }
}
