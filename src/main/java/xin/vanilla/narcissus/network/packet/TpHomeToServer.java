package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.banira.common.util.CommandUtils;
import xin.vanilla.banira.internal.network.BaniraStreamCodecs;
import xin.vanilla.narcissus.Identifier;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;

public class TpHomeToServer implements CustomPacketPayload {

    public static final Type<TpHomeToServer> TYPE =
            new Type<>(Identifier.id().create("tp_home_server"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TpHomeToServer> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(TpHomeToServer::toBytes, TpHomeToServer::new);

    public TpHomeToServer() {
    }

    public TpHomeToServer(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TpHomeToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                CommandUtils.executeCommand(player, NarcissusUtils.getCommand(EnumCommandType.TP_HOME));
            }
        });
    }
}
