package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.player.PlayerTeleportDataCapability;

import java.util.function.Supplier;

public class ClientModLoadedNotice {

    public ClientModLoadedNotice() {
    }

    public ClientModLoadedNotice(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(ClientModLoadedNotice packet, Supplier<NetworkEvent.Context> ctx) {
        // 获取网络事件上下文并排队执行工作
        ctx.get().enqueueWork(() -> {
            // 获取发送数据包的玩家实体
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                NarcissusFarewell.getPlayerCapabilityStatus().put(player.getStringUUID(), false);
                // 同步玩家传送数据到客户端
                PlayerTeleportDataCapability.syncPlayerData(player);
            }
        });
        // 设置数据包已处理状态，防止重复处理
        ctx.get().setPacketHandled(true);
    }
}
