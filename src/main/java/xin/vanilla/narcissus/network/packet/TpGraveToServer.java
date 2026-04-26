package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.network.NetworkPacket;
import xin.vanilla.narcissus.util.NarcissusUtils;

public class TpGraveToServer implements NetworkPacket {

    public TpGraveToServer() {
    }

    public TpGraveToServer(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(TpGraveToServer packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                CommandUtils.executeCommand(player, NarcissusUtils.getCommand(EnumCommandType.TP_GRAVE));
            }
        });
        ctx.setPacketHandled(true);
    }
}
