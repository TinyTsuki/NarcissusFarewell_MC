package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.common.util.MessageUtils;
import xin.vanilla.banira.internal.network.BaniraStreamCodecs;
import xin.vanilla.narcissus.Identifier;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.notification.NarcissusNotificationTypes;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;

public class TpNoToServer implements CustomPacketPayload {

    public static final Type<TpNoToServer> TYPE =
            new Type<>(Identifier.id().create("tp_no_server"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TpNoToServer> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(TpNoToServer::toBytes, TpNoToServer::new);

    public TpNoToServer() {
    }

    public TpNoToServer(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TpNoToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
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
        });
    }
}
