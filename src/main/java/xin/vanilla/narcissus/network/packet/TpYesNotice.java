package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.config.TeleportRequest;
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.enums.EI18nType;
import xin.vanilla.narcissus.enums.ETeleportType;
import xin.vanilla.narcissus.util.I18nUtils;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Comparator;
import java.util.Objects;

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
                    ETeleportType teleportType = NarcissusFarewell.getTeleportRequest().values().stream()
                            .filter(request -> request.getTarget().getUUID().equals(player.getUUID()))
                            .max(Comparator.comparing(TeleportRequest::getRequestTime))
                            .orElse(new TeleportRequest())
                            .getTeleportType();
                    if (ETeleportType.TP_ASK == teleportType || ETeleportType.TP_HERE == teleportType) {
                        ECommandType type = ETeleportType.TP_HERE == teleportType ? ECommandType.TP_HERE_YES : ECommandType.TP_ASK_YES;
                        Objects.requireNonNull(player.getServer()).getCommands().performPrefixedCommand(player.createCommandSourceStack(), NarcissusUtils.getCommand(type));
                    } else {
                        NarcissusUtils.sendTranslatableMessage((ServerPlayer) player, I18nUtils.getKey(EI18nType.MESSAGE, "tp_ask_not_found"));
                    }
                });
            });
        }
    }
}
