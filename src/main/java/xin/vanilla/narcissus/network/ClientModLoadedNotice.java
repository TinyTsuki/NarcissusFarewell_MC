package xin.vanilla.narcissus.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.util.ServerTaskExecutor;

public class ClientModLoadedNotice implements IMessage {

    public ClientModLoadedNotice() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<ClientModLoadedNotice, IMessage> {
        @Override
        public IMessage onMessage(ClientModLoadedNotice packet, MessageContext ctx) {
            // 排队工作到主线程
            ServerTaskExecutor.run(() -> {
                EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                if (player != null) {
                    // 更新玩家数据
                    NarcissusFarewell.getPlayerCapabilityStatus().put(player.getUniqueID().toString(), false);
                    // 同步玩家传送数据到客户端
                    PlayerTeleportData.syncPlayerData(player);
                }
            });
            return null;
        }
    }
}
