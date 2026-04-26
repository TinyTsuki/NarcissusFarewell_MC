package xin.vanilla.narcissus.network.packet;

import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import xin.vanilla.narcissus.data.player.PlayerTeleportData;
import xin.vanilla.narcissus.network.NetworkPacket;


@Getter
public class PlayerConfigSyncToServer implements NetworkPacket {

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

    public static void handle(PlayerConfigSyncToServer packet, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            if (!ctx.isServerSide()) {
                return;
            }
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            PlayerTeleportData.getData(player).replaceAllTeleportCountdownsFromTag(packet.countdownTag);
            PlayerTeleportData.syncPlayerData(player);
        });
        ctx.setPacketHandled(true);
    }
}
