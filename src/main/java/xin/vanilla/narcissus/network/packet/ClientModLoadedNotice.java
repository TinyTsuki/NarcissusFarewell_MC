package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.player.PlayerDataAttachment;

public class ClientModLoadedNotice implements CustomPacketPayload {
    public final static ResourceLocation ID = new ResourceLocation(NarcissusFarewell.MODID, "client_mod_loaded");

    public ClientModLoadedNotice() {
    }

    public ClientModLoadedNotice(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull ResourceLocation id() {
        return ID;
    }

    public void write(FriendlyByteBuf buf) {
    }

    public static void handle(ClientModLoadedNotice packet, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.workHandler().execute(() -> {
                ctx.player().ifPresent(player -> {
                    NarcissusFarewell.getPlayerCapabilityStatus().put(player.getStringUUID(), false);
                    // 同步玩家传送数据到客户端
                    PlayerDataAttachment.syncPlayerData((ServerPlayer) player);
                });
            });
        }
    }
}
