package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;

import java.util.function.Supplier;


@Getter
public class PlayerConfigSyncToServer {

    private final CompoundNBT countdownTag;

    public PlayerConfigSyncToServer(CompoundNBT countdownTag) {
        this.countdownTag = countdownTag != null ? countdownTag : new CompoundNBT();
    }

    public PlayerConfigSyncToServer(PacketBuffer buf) {
        CompoundNBT n = buf.readNbt();
        this.countdownTag = n != null ? n : new CompoundNBT();
    }

    public void toBytes(PacketBuffer buf) {
        buf.writeNbt(countdownTag);
    }

    public static void handle(PlayerConfigSyncToServer packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!ctx.get().getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            PlayerTeleportData.getData(player).replaceAllTeleportCountdownsFromTag(packet.countdownTag);
            PlayerTeleportData.syncPlayerData(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
