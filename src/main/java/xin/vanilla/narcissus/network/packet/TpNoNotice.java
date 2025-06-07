package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;

public class TpNoNotice implements CustomPacketPayload {
    public final static ResourceLocation ID = new ResourceLocation(NarcissusFarewell.MODID, "tp_no");

    public TpNoNotice() {
    }

    public TpNoNotice(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull ResourceLocation id() {
        return ID;
    }

    public void write(FriendlyByteBuf buf) {
    }

    public static void handle(TpNoNotice packet, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            ctx.workHandler().execute(() -> {
                ctx.player().ifPresent(player -> {
                    if (player instanceof ServerPlayer) {
                        EnumTeleportType teleportType = NarcissusFarewell.getTeleportRequest().values().stream()
                                .filter(request -> !request.isIgnore())
                                .filter(request -> request.getTarget().getUUID().equals(player.getUUID()))
                                .max(Comparator.comparing(TeleportRequest::getRequestTime))
                                .orElse(new TeleportRequest())
                                .getTeleportType();
                        if (EnumTeleportType.TP_ASK == teleportType || EnumTeleportType.TP_HERE == teleportType) {
                            EnumCommandType type = EnumTeleportType.TP_HERE == teleportType ? EnumCommandType.TP_HERE_NO : EnumCommandType.TP_ASK_NO;
                            NarcissusUtils.executeCommand((ServerPlayer) player, NarcissusUtils.getCommand(type));
                        } else {
                            NarcissusUtils.sendTranslatableMessage((ServerPlayer) player, I18nUtils.getKey(EnumI18nType.MESSAGE, "tp_ask_not_found"));
                        }
                    }
                });
            });
        }
    }
}
