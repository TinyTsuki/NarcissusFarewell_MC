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
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;

public class TpYesNotice implements CustomPacketPayload {
    public final static CustomPacketPayload.Type<TpYesNotice> TYPE = new CustomPacketPayload.Type<>(NarcissusFarewell.createResource("tp_yes"));
    public final static StreamCodec<ByteBuf, TpYesNotice> STREAM_CODEC = new StreamCodec<>() {
        public @NotNull TpYesNotice decode(@NotNull ByteBuf byteBuf) {
            return new TpYesNotice((new FriendlyByteBuf(byteBuf)));
        }

        public void encode(@NotNull ByteBuf byteBuf, @NotNull TpYesNotice packet) {
            packet.toBytes(new FriendlyByteBuf(byteBuf));
        }
    };

    public TpYesNotice() {
    }

    public TpYesNotice(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(TpYesNotice packet, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            // 获取网络事件上下文并排队执行工作
            ctx.enqueueWork(() -> {
                // 获取发送数据包的玩家实体
                if (ctx.player() instanceof ServerPlayer player) {
                    ETeleportType teleportType = NarcissusFarewell.getTeleportRequest().values().stream()
                            .filter(request -> !request.isIgnore())
                            .filter(request -> request.getTarget().getUUID().equals(player.getUUID()))
                            .max(Comparator.comparing(TeleportRequest::getRequestTime))
                            .orElse(new TeleportRequest())
                            .getTeleportType();
                    if (ETeleportType.TP_ASK == teleportType || ETeleportType.TP_HERE == teleportType) {
                        EnumCommandType type = ETeleportType.TP_HERE == teleportType ? EnumCommandType.TP_HERE_YES : EnumCommandType.TP_ASK_YES;
                        NarcissusUtils.executeCommand(player, NarcissusUtils.getCommand(type));
                    } else {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_not_found"));
                    }
                }
            });
        }
    }
}
