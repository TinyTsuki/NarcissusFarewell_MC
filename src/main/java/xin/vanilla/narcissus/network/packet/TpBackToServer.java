package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.banira.common.network.NetworkPacket;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.internal.network.BaniraStreamCodecs;
import xin.vanilla.narcissus.Identifier;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;

public class TpBackToServer implements NetworkPacket {

    public static final Type<TpBackToServer> TYPE =
            new Type<>(Identifier.id().create("tp_back_server"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TpBackToServer> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(TpBackToServer::toBytes, TpBackToServer::new);

    public TpBackToServer() {
    }

    public TpBackToServer(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TpBackToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                CommandUtils.executeCommand(player, NarcissusUtils.getCommand(EnumCommandType.TP_BACK));
            }
        });
    }
}
