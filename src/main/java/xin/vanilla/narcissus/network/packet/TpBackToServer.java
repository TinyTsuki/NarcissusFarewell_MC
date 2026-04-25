package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.SimpleChannel;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.network.NetworkInit;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.function.Supplier;

public class TpBackToServer implements NetworkPacket {

    @Override
    public Supplier<SimpleChannel> channel() {
        return () -> NetworkInit.INSTANCE;
    }

    public TpBackToServer() {
    }

    public TpBackToServer(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(TpBackToServer packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                CommandUtils.executeCommand(player, NarcissusUtils.getCommand(EnumCommandType.TP_BACK));
            }
        });
        ctx.setPacketHandled(true);
    }
}
