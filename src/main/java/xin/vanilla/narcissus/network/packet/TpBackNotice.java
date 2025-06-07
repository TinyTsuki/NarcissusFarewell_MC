package xin.vanilla.narcissus.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;

public class TpBackNotice implements CustomPacketPayload {
    public final static CustomPacketPayload.Type<TpBackNotice> TYPE = new CustomPacketPayload.Type<>(NarcissusFarewell.createResource("tp_back"));
    public final static StreamCodec<ByteBuf, TpBackNotice> STREAM_CODEC = new StreamCodec<>() {
        public @NotNull TpBackNotice decode(@NotNull ByteBuf byteBuf) {
            return new TpBackNotice((new FriendlyByteBuf(byteBuf)));
        }

        public void encode(@NotNull ByteBuf byteBuf, @NotNull TpBackNotice packet) {
            packet.toBytes(new FriendlyByteBuf(byteBuf));
        }
    };

    public TpBackNotice() {
    }

    public TpBackNotice(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(TpBackNotice packet, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.enqueueWork(() -> {
                if (ctx.player() instanceof ServerPlayer player) {
                    NarcissusUtils.executeCommand(player, NarcissusUtils.getCommand(EnumCommandType.TP_BACK));
                }
            });
        }
    }
}
