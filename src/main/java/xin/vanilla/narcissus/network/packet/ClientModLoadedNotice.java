package xin.vanilla.narcissus.network.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.player.PlayerDataAttachment;

public class ClientModLoadedNotice implements CustomPacketPayload {
    public final static CustomPacketPayload.Type<ClientModLoadedNotice> TYPE = new CustomPacketPayload.Type<>(NarcissusFarewell.createResource("client_mod_loaded"));
    public final static StreamCodec<ByteBuf, ClientModLoadedNotice> STREAM_CODEC = new StreamCodec<>() {
        public @NotNull ClientModLoadedNotice decode(@NotNull ByteBuf byteBuf) {
            return new ClientModLoadedNotice((new FriendlyByteBuf(byteBuf)));
        }

        public void encode(@NotNull ByteBuf byteBuf, @NotNull ClientModLoadedNotice packet) {
            packet.toBytes(new FriendlyByteBuf(byteBuf));
        }
    };

    public ClientModLoadedNotice() {
    }

    public ClientModLoadedNotice(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(ClientModLoadedNotice packet, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            // 获取网络事件上下文并排队执行工作
            ctx.enqueueWork(() -> {
                // 获取发送数据包的玩家实体
                if (ctx.player() instanceof ServerPlayer player) {
                    NarcissusFarewell.getPlayerCapabilityStatus().put(player.getStringUUID(), false);
                    // 同步玩家传送数据到客户端
                    PlayerDataAttachment.syncPlayerData(player);
                }
            });
        }
    }
}
