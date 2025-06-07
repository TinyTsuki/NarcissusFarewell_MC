package xin.vanilla.narcissus.network.packet;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;
import java.util.function.Supplier;

public class TpYesNotice {

    public TpYesNotice() {
    }

    public TpYesNotice(PacketBuffer buf) {
    }

    public void toBytes(PacketBuffer buf) {
    }

    public static void handle(TpYesNotice packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null) {
                EnumTeleportType teleportType = NarcissusFarewell.getTeleportRequest().values().stream()
                        .filter(request -> !request.isIgnore())
                        .filter(request -> request.getTarget().getUUID().equals(player.getUUID()))
                        .max(Comparator.comparing(TeleportRequest::getRequestTime))
                        .orElse(new TeleportRequest())
                        .getTeleportType();
                if (EnumTeleportType.TP_ASK == teleportType || EnumTeleportType.TP_HERE == teleportType) {
                    EnumCommandType type = EnumTeleportType.TP_HERE == teleportType ? EnumCommandType.TP_HERE_YES : EnumCommandType.TP_ASK_YES;
                    NarcissusUtils.executeCommand(player, NarcissusUtils.getCommand(type));
                } else {
                    NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_not_found"));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
