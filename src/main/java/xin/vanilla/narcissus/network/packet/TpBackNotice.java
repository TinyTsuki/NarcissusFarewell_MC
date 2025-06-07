package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.function.Supplier;

public class TpBackNotice {

    public TpBackNotice() {
    }

    public TpBackNotice(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(TpBackNotice packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                NarcissusUtils.executeCommand(player, NarcissusUtils.getCommand(EnumCommandType.TP_BACK));
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
