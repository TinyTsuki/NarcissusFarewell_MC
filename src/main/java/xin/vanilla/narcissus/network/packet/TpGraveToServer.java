package xin.vanilla.narcissus.network.packet;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.network.NetworkInit;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.function.Supplier;

public class TpGraveToServer implements NetworkPacket {

    @Override
    public Supplier<SimpleChannel> channel() {
        return () -> NetworkInit.INSTANCE;
    }

    public TpGraveToServer() {
    }

    public TpGraveToServer(PacketBuffer buf) {
    }

    public void toBytes(PacketBuffer buf) {
    }

    public static void handle(TpGraveToServer packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                CommandUtils.executeCommand(player, NarcissusUtils.getCommand(EnumCommandType.TP_GRAVE));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
