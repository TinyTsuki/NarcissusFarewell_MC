package xin.vanilla.narcissus.network.packet;

import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.network.NetworkPacket;
import xin.vanilla.narcissus.util.NarcissusUtils;


public class TpGraveToServer implements NetworkPacket {

    public TpGraveToServer() {
    }

    public TpGraveToServer(BaniraPacketBuffer buf) {
    }

    public void toBytes(BaniraPacketBuffer buf) {
    }

    public static void handle(TpGraveToServer packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayerEntity player = ctx.senderAs(net.minecraft.entity.player.ServerPlayerEntity.class);
            if (player != null) {
                CommandUtils.executeCommand(player, NarcissusUtils.getCommand(EnumCommandType.TP_GRAVE));
            }
        });
        ctx.markHandled();
    }
}
