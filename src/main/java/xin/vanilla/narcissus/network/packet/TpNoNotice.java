package xin.vanilla.narcissus.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;

public class TpNoNotice implements CustomPacketPayload {
    public final static CustomPacketPayload.Type<TpNoNotice> TYPE = new CustomPacketPayload.Type<>(NarcissusFarewell.createResource("tp_no"));
    public final static StreamCodec<ByteBuf, TpNoNotice> STREAM_CODEC = new StreamCodec<>() {
        public @NotNull TpNoNotice decode(@NotNull ByteBuf byteBuf) {
            return new TpNoNotice((new FriendlyByteBuf(byteBuf)));
        }

        public void encode(@NotNull ByteBuf byteBuf, @NotNull TpNoNotice packet) {
            packet.toBytes(new FriendlyByteBuf(byteBuf));
        }
    };

    public TpNoNotice() {
    }

    public TpNoNotice(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(TpNoNotice packet, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player) {
                    EnumTeleportType teleportType = NarcissusFarewell.getTeleportRequest().values().stream()
                            .filter(request -> !request.isIgnore())
                            .filter(request -> request.getTarget().getUUID().equals(player.getUUID()))
                            .max(Comparator.comparing(TeleportRequest::getRequestTime))
                            .orElse(new TeleportRequest())
                            .getTeleportType();
                    if (EnumTeleportType.TP_ASK == teleportType || EnumTeleportType.TP_HERE == teleportType) {
                        EnumCommandType type = EnumTeleportType.TP_HERE == teleportType ? EnumCommandType.TP_HERE_NO : EnumCommandType.TP_ASK_NO;
                        NarcissusUtils.executeCommand(player, NarcissusUtils.getCommand(type));
                    } else {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_not_found"));
                    }

                }
            });
        }
    }
}
