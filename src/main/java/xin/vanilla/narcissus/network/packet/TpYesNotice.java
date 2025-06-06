package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.data.TeleportRequest;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.enums.EnumI18nType;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;

public class TpYesNotice implements CustomPacketPayload {
    public final static ResourceLocation ID = new ResourceLocation(NarcissusFarewell.MODID, "tp_yes");


    public TpYesNotice() {
    }

    public TpYesNotice(FriendlyByteBuf buf) {
    }

    @Override
    public @NotNull ResourceLocation id() {
        return ID;
    }

    public void write(FriendlyByteBuf buf) {
    }

    public static void handle(TpYesNotice packet, IPayloadContext ctx) {
        if (ctx.flow().isServerbound()) {
            // 获取网络事件上下文并排队执行工作
            ctx.workHandler().execute(() -> {
                // 获取发送数据包的玩家实体
                ctx.player().ifPresent(player -> {
                    if (player instanceof ServerPlayer) {
                        ETeleportType teleportType = NarcissusFarewell.getTeleportRequest().values().stream()
                                .filter(request -> !request.isIgnore())
                                .filter(request -> request.getTarget().getUUID().equals(player.getUUID()))
                                .max(Comparator.comparing(TeleportRequest::getRequestTime))
                                .orElse(new TeleportRequest())
                                .getTeleportType();
                        if (ETeleportType.TP_ASK == teleportType || ETeleportType.TP_HERE == teleportType) {
                            EnumCommandType type = ETeleportType.TP_HERE == teleportType ? EnumCommandType.TP_HERE_YES : EnumCommandType.TP_ASK_YES;
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
