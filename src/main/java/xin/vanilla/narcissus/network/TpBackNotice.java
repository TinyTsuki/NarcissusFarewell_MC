package xin.vanilla.narcissus.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Objects;

public class TpBackNotice implements IMessage {

    public TpBackNotice() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<TpBackNotice, IMessage> {
        @Override
        public IMessage onMessage(TpBackNotice packet, MessageContext ctx) {
            // 获取网络事件上下文并排队执行工作
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                EntityPlayerMP player = ctx.getServerHandler().player;
                if (player != null) {
                    Objects.requireNonNull(player.getServer()).getCommandManager().executeCommand(player, NarcissusUtils.getCommand(ECommandType.TP_BACK));
                }
            });
            return null;
        }
    }
}
