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

public class PlayerDataReceivedNotice implements CustomPacketPayload {
    public final static CustomPacketPayload.Type<PlayerDataReceivedNotice> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(NarcissusFarewell.MODID, "player_data_received"));
    public final static StreamCodec<ByteBuf, PlayerDataReceivedNotice> STREAM_CODEC = new StreamCodec<>() {
        public @NotNull PlayerDataReceivedNotice decode(@NotNull ByteBuf byteBuf) {
            return new PlayerDataReceivedNotice((new FriendlyByteBuf(byteBuf)));
        }

        public void encode(@NotNull ByteBuf byteBuf, @NotNull PlayerDataReceivedNotice packet) {
            packet.toBytes(new FriendlyByteBuf(byteBuf));
        }
    };

    public PlayerDataReceivedNotice() {
    }

    public PlayerDataReceivedNotice(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(PlayerDataReceivedNotice packet, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            // 获取网络事件上下文并排队执行工作
            ctx.enqueueWork(() -> {
                // 获取发送数据包的玩家实体
                if (ctx.player() instanceof ServerPlayer player) {
                    NarcissusFarewell.getPlayerCapabilityStatus().put(player.getStringUUID(), true);
                }
            });
        }
    }
}
