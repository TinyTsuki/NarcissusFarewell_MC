package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.banira.internal.network.BaniraStreamCodecs;
import xin.vanilla.narcissus.Identifier;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.network.NetworkPacket;

@Getter
public class PlayerConfigSyncToServer implements NetworkPacket {

    public static final Type<PlayerConfigSyncToServer> TYPE =
            new Type<>(Identifier.id().create("player_config_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerConfigSyncToServer> STREAM_CODEC =
            BaniraStreamCodecs.registryBuf(PlayerConfigSyncToServer::toBytes, PlayerConfigSyncToServer::new);

    private final CompoundTag countdownTag;

    public PlayerConfigSyncToServer(CompoundTag countdownTag) {
        this.countdownTag = countdownTag != null ? countdownTag : new CompoundTag();
    }

    public PlayerConfigSyncToServer(FriendlyByteBuf buf) {
        CompoundTag n = buf.readNbt();
        this.countdownTag = n != null ? n : new CompoundTag();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeNbt(countdownTag);
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PlayerConfigSyncToServer packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) {
                return;
            }
            PlayerTeleportData.getData(player).replaceAllTeleportCountdownsFromTag(packet.countdownTag);
            PlayerTeleportData.syncPlayerData(player);
        });
    }
}
