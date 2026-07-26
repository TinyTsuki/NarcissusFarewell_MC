package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.internal.network.NarcissusNbtPacketCodec;
import xin.vanilla.narcissus.network.NetworkPacket;


@Getter
public class PlayerConfigSyncToServer implements NetworkPacket {

    private final CompoundTag countdownTag;

    public PlayerConfigSyncToServer(CompoundTag countdownTag) {
        this.countdownTag = countdownTag != null ? countdownTag : new CompoundTag();
    }

    public PlayerConfigSyncToServer(BaniraPacketBuffer buf) {
        CompoundTag n = NarcissusNbtPacketCodec.read(buf);
        this.countdownTag = n != null ? n : new CompoundTag();
    }

    public void toBytes(BaniraPacketBuffer buf) {
        NarcissusNbtPacketCodec.write(buf, countdownTag);
    }

    public static void handle(PlayerConfigSyncToServer packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (!ctx.isServerSide()) {
                return;
            }
            ServerPlayer player = ctx.senderAs(net.minecraft.server.level.ServerPlayer.class);
            if (player == null) {
                return;
            }
            PlayerTeleportData.getData(player).replaceAllTeleportCountdownsFromTag(packet.countdownTag);
            PlayerTeleportData.syncPlayerData(player);
        });
        ctx.markHandled();
    }
}
