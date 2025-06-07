package xin.vanilla.narcissus.network.packet;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.function.Supplier;

public class TpBackNotice {

    public TpBackNotice() {
    }

    public TpBackNotice(PacketBuffer buf) {
    }

    public void toBytes(PacketBuffer buf) {
    }

    public static void handle(TpBackNotice packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                NarcissusUtils.executeCommand(player, NarcissusUtils.getCommand(EnumCommandType.TP_BACK));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
