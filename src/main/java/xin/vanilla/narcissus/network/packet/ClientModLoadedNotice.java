package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.player.PlayerTeleportDataCapability;

public class ClientModLoadedNotice {

    public ClientModLoadedNotice() {
    }

    public ClientModLoadedNotice(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(ClientModLoadedNotice packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                NarcissusFarewell.getPlayerCapabilityStatus().put(player.getStringUUID(), false);
                // 同步玩家传送数据到客户端
                PlayerTeleportDataCapability.syncPlayerData(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
