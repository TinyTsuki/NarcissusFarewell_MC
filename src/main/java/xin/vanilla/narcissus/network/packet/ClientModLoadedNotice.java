package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.NetworkEvent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.player.PlayerTeleportDataCapability;

public class ClientModLoadedNotice {

    public ClientModLoadedNotice() {
    }

    public ClientModLoadedNotice(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(ClientModLoadedNotice packet, NetworkEvent.ServerCustomPayloadEvent.Context ctx) {
        // 获取网络事件上下文并排队执行工作
        ctx.enqueueWork(() -> {
            // 获取发送数据包的玩家实体
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                NarcissusFarewell.getPlayerCapabilityStatus().put(player.getStringUUID(), false);
                // 同步玩家传送数据到客户端
                PlayerTeleportDataCapability.syncPlayerData(player);
            }
        });
        // 设置数据包已处理状态，防止重复处理
        ctx.setPacketHandled(true);
    }
}
