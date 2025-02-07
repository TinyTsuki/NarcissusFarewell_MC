package xin.vanilla.narcissus.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.capability.player.PlayerTeleportDataCapability;

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
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                EntityPlayerMP player = ctx.getServerHandler().player;
                if (player != null) {
                    // 更新玩家数据
                    NarcissusFarewell.getPlayerCapabilityStatus().put(player.getUniqueID().toString(), false);
                    // 同步玩家传送数据到客户端
                    PlayerTeleportDataCapability.syncPlayerData(player);
                }
            });
            return null;
        }
    }
}
