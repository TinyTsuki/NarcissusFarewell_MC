package xin.vanilla.narcissus.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.enums.EI18nType;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;
import java.util.Objects;

public class TpYesNotice implements IMessage {

    public TpYesNotice() {
    }

    @Override
    public void fromBytes(ByteBuf buf) {
    }

    @Override
    public void toBytes(ByteBuf buf) {
    }

    public static class Handler implements IMessageHandler<TpYesNotice, IMessage> {
        @Override
        public IMessage onMessage(TpYesNotice packet, MessageContext ctx) {
            // 获取网络事件上下文并排队执行工作
            ctx.getServerHandler().player.getServerWorld().addScheduledTask(() -> {
                EntityPlayerMP player = ctx.getServerHandler().player;
                if (player != null) {
                    ETeleportType teleportType = NarcissusFarewell.getTeleportRequest().values().stream()
                            .filter(request -> request.getTarget().getUniqueID().equals(player.getUniqueID()))
                            .max(Comparator.comparing(TeleportRequest::getRequestTime))
                            .orElse(new TeleportRequest())
                            .getTeleportType();
                    if (ETeleportType.TP_ASK == teleportType || ETeleportType.TP_HERE == teleportType) {
                        ECommandType type = ETeleportType.TP_HERE == teleportType ? ECommandType.TP_HERE_YES : ECommandType.TP_ASK_YES;
                        Objects.requireNonNull(player.getServer()).getCommandManager().executeCommand(player, NarcissusUtils.getCommand(type));
                    } else {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_not_found"));
                    }
                }
            });
            return null;
        }
    }
}
