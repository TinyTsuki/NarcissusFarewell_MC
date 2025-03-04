package xin.vanilla.narcissus.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;
import xin.vanilla.narcissus.util.ServerTaskExecutor;

public class TpHomeNotice implements IMessage {

    public TpHomeNotice() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<TpHomeNotice, IMessage> {
        @Override
        public IMessage onMessage(TpHomeNotice packet, MessageContext ctx) {
            // 获取网络事件上下文并排队执行工作
            ServerTaskExecutor.run(() -> {
                // 获取发送数据包的玩家实体
                EntityPlayerMP player = ctx.getServerHandler().playerEntity;
                if (player != null) {
                    NarcissusFarewell.getServerInstance().getCommandManager().executeCommand(player, NarcissusUtils.getCommand(ECommandType.TP_HOME));
                }
            });
            return null;
        }
    }
}
