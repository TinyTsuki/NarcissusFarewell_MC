package xin.vanilla.narcissus.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Objects;

public class TpHomeNotice {

    public TpHomeNotice() {
    }

    public TpHomeNotice(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(TpHomeNotice packet, CustomPayloadEvent.Context ctx) {
        // 获取网络事件上下文并排队执行工作
        ctx.enqueueWork(() -> {
            // 获取发送数据包的玩家实体
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                Objects.requireNonNull(player.getServer()).getCommands().performPrefixedCommand(player.createCommandSourceStack(), NarcissusUtils.getCommand(ECommandType.TP_HOME));
            }
        });
        // 设置数据包已处理状态，防止重复处理
        ctx.setPacketHandled(true);
    }
}
