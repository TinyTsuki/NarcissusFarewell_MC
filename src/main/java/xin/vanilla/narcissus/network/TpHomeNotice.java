package xin.vanilla.narcissus.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Objects;

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
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                // 获取发送数据包的玩家实体
                EntityPlayerMP player = ctx.getServerHandler().player;
                if (player != null) {
                    Objects.requireNonNull(player.getServer()).getCommandManager().executeCommand(player, NarcissusUtils.getCommand(ECommandType.TP_HOME));
                }
            });
            return null;
        }
    }
}
