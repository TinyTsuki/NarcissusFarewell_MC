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
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Objects;

public class TpHomeNotice implements CustomPacketPayload {
    public final static CustomPacketPayload.Type<TpHomeNotice> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NarcissusFarewell.MODID, "tp_home"));
    public final static StreamCodec<ByteBuf, TpHomeNotice> STREAM_CODEC = new StreamCodec<>() {
        public @NotNull TpHomeNotice decode(@NotNull ByteBuf byteBuf) {
            return new TpHomeNotice((new FriendlyByteBuf(byteBuf)));
        }

        public void encode(@NotNull ByteBuf byteBuf, @NotNull TpHomeNotice packet) {
            packet.toBytes(new FriendlyByteBuf(byteBuf));
        }
    };

    public TpHomeNotice() {
    }

    public TpHomeNotice(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(TpHomeNotice packet, IPayloadContext ctx) {
        // 获取网络事件上下文并排队执行工作
        ctx.enqueueWork(() -> {
            // 获取发送数据包的玩家实体
            if (ctx.player() instanceof ServerPlayer player) {
                Objects.requireNonNull(player.getServer()).getCommands().performPrefixedCommand(player.createCommandSourceStack(), NarcissusUtils.getCommand(ECommandType.TP_HOME));
            }
        });
    }
}
