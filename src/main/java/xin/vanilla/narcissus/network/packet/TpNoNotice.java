package xin.vanilla.narcissus.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.enums.EI18nType;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;
import java.util.Objects;

public class TpNoNotice implements CustomPacketPayload {
    public final static CustomPacketPayload.Type<TpNoNotice> TYPE = new CustomPacketPayload.Type<>(new ResourceLocation(NarcissusFarewell.MODID, "tp_no"));
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
            // 获取网络事件上下文并排队执行工作
            ctx.enqueueWork(() -> {
                // 获取发送数据包的玩家实体
                if (ctx.player() instanceof ServerPlayer player) {
                    ETeleportType teleportType = NarcissusFarewell.getTeleportRequest().values().stream()
                            .filter(request -> request.getTarget().getUUID().equals(player.getUUID()))
                            .max(Comparator.comparing(TeleportRequest::getRequestTime))
                            .orElse(new TeleportRequest())
                            .getTeleportType();
                    if (ETeleportType.TP_ASK == teleportType || ETeleportType.TP_HERE == teleportType) {
                        ECommandType type = ETeleportType.TP_HERE == teleportType ? ECommandType.TP_HERE_NO : ECommandType.TP_ASK_NO;
                        Objects.requireNonNull(player.getServer()).getCommands().performPrefixedCommand(player.createCommandSourceStack(), NarcissusUtils.getCommand(type));
                    } else {
                        NarcissusUtils.sendTranslatableMessage(player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_not_found"));
                    }
                }
            });
        }
    }
}
