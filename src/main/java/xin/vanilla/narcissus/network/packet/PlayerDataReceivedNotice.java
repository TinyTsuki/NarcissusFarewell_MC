package xin.vanilla.narcissus.network.packet;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.narcissus.NarcissusFarewell;

import java.util.function.Supplier;

public class PlayerDataReceivedNotice {

    public PlayerDataReceivedNotice() {
    }

    public PlayerDataReceivedNotice(PacketBuffer buf) {
    }

    public void toBytes(PacketBuffer buf) {
    }

    public static void handle(PlayerDataReceivedNotice packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                NarcissusFarewell.getPlayerCapabilityStatus().put(player.getStringUUID(), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
