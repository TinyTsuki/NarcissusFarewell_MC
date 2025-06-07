package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.narcissus.NarcissusFarewell;

public class PlayerDataReceivedNotice implements CustomPacketPayload {
    public final static ResourceLocation ID = new ResourceLocation(NarcissusFarewell.MODID, "player_data_received");

    public PlayerDataReceivedNotice() {
    }

    public PlayerDataReceivedNotice(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull ResourceLocation id() {
        return ID;
    }

    public void write(FriendlyByteBuf buf) {
    }

    public static void handle(PlayerDataReceivedNotice packet, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.workHandler().execute(() -> {
                ctx.player().ifPresent(
                        player -> NarcissusFarewell.getPlayerCapabilityStatus().put(player.getStringUUID(), true)
                );
            });
        }
    }
}
