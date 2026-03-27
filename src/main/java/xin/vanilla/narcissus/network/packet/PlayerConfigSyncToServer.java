package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;

import java.util.function.Supplier;


@Getter
public class PlayerConfigSyncToServer {

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

    public static void handle(PlayerConfigSyncToServer packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (!ctx.get().getDirection().getReceptionSide().isServer()) {
                return;
            }
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            PlayerTeleportData.getData(player).replaceAllTeleportCountdownsFromTag(packet.countdownTag);
            PlayerTeleportData.syncPlayerData(player);
        });
        ctx.get().setPacketHandled(true);
    }
}
