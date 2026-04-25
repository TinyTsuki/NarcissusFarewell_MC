package xin.vanilla.narcissus.network.packet;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.network.NetworkInit;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;
import java.util.function.Supplier;

public class TpNoToServer implements NetworkPacket {

    @Override
    public Supplier<SimpleChannel> channel() {
        return () -> NetworkInit.INSTANCE;
    }

    public TpNoToServer() {
    }

    public TpNoToServer(PacketBuffer buf) {
    }

    public void toBytes(PacketBuffer buf) {
    }

    public static void handle(TpNoToServer packet, Supplier<NetworkEvent.Context> ctx) {
        // 获取网络事件上下文并排队执行工作
        ctx.get().enqueueWork(() -> {
            // 获取发送数据包的玩家实体
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                EnumTeleportType teleportType = NarcissusFarewell.getTeleportRequest().values().stream()
                        .filter(request -> !request.isIgnore())
                        .filter(request -> request.getTarget().getUUID().equals(player.getUUID()))
                        .max(Comparator.comparing(TeleportRequest::getRequestTime))
                        .orElse(new TeleportRequest())
                        .getTeleportType();
                if (EnumTeleportType.TP_ASK == teleportType || EnumTeleportType.TP_HERE == teleportType) {
                    EnumCommandType type = EnumTeleportType.TP_HERE == teleportType ? EnumCommandType.TP_HERE_NO : EnumCommandType.TP_ASK_NO;
                    CommandUtils.executeCommand(player, NarcissusUtils.getCommand(type));
                } else {
                    MessageUtils.sendNotification(player, NarcissusComponent.get().transAuto("tp_ask_not_found"), NarcissusNotificationTypes.TELEPORT_REQUEST);
                }
            }
        });
        // 设置数据包已处理状态，防止重复处理
        ctx.get().setPacketHandled(true);
    }
}
