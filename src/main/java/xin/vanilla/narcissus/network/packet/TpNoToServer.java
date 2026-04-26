package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.network.NetworkPacket;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;

public class TpNoToServer implements NetworkPacket {

    public TpNoToServer() {
    }

    public TpNoToServer(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(TpNoToServer packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
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
        ctx.setPacketHandled(true);
    }
}
