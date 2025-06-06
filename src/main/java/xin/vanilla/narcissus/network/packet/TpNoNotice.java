package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;

public class TpNoNotice {

    public TpNoNotice() {
    }

    public TpNoNotice(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(TpNoNotice packet, CustomPayloadEvent.Context ctx) {
        // 获取网络事件上下文并排队执行工作
        ctx.enqueueWork(() -> {
            // 获取发送数据包的玩家实体
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                ETeleportType teleportType = NarcissusFarewell.getTeleportRequest().values().stream()
                        .filter(request -> !request.isIgnore())
                        .filter(request -> request.getTarget().getUUID().equals(player.getUUID()))
                        .max(Comparator.comparing(TeleportRequest::getRequestTime))
                        .orElse(new TeleportRequest())
                        .getTeleportType();
                if (ETeleportType.TP_ASK == teleportType || ETeleportType.TP_HERE == teleportType) {
                    EnumCommandType type = ETeleportType.TP_HERE == teleportType ? EnumCommandType.TP_HERE_NO : EnumCommandType.TP_ASK_NO;
                    NarcissusUtils.executeCommand(player, NarcissusUtils.getCommand(type));
                } else {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_not_found"));
                }
            }
        });
        // 设置数据包已处理状态，防止重复处理
        ctx.setPacketHandled(true);
    }
}
