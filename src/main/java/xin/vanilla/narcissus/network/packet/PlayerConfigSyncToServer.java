package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.internal.network.NarcissusNbtPacketCodec;
import xin.vanilla.narcissus.network.NetworkPacket;



@Getter
public class PlayerConfigSyncToServer implements NetworkPacket {

    private final CompoundNBT countdownTag;

    public PlayerConfigSyncToServer(CompoundNBT countdownTag) {
        this.countdownTag = countdownTag != null ? countdownTag : new CompoundNBT();
    }

    public PlayerConfigSyncToServer(BaniraPacketBuffer buf) {
        CompoundNBT n = NarcissusNbtPacketCodec.read(buf);
        this.countdownTag = n != null ? n : new CompoundNBT();
    }

    public void toBytes(BaniraPacketBuffer buf) {
        NarcissusNbtPacketCodec.write(buf, countdownTag);
    }

    public static void handle(PlayerConfigSyncToServer packet, BaniraNetworkContext ctx) {
        ctx.enqueueWork(() -> {
            if (!ctx.isServerSide()) {
                return;
            }
            ServerPlayerEntity player = ctx.senderAs(net.minecraft.entity.player.ServerPlayerEntity.class);
            if (player == null) {
                return;
            }
            PlayerTeleportData.getData(player).replaceAllTeleportCountdownsFromTag(packet.countdownTag);
            PlayerTeleportData.syncPlayerData(player);
        });
        ctx.markHandled();
    }
}
