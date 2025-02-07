package xin.vanilla.narcissus.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import xin.vanilla.narcissus.enums.ECommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.Objects;
import java.util.function.Supplier;

public class TpBackNotice {

    public TpBackNotice() {
    }

    public TpBackNotice(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(TpBackNotice packet, Supplier<NetworkEvent.Context> ctx) {
        // 获取网络事件上下文并排队执行工作
        ctx.get().enqueueWork(() -> {
            // 获取发送数据包的玩家实体
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                Objects.requireNonNull(player.getServer()).getCommands().performCommand(player.createCommandSourceStack(), NarcissusUtils.getCommand(ECommandType.TP_BACK));
            }
        });
        // 设置数据包已处理状态，防止重复处理
        ctx.get().setPacketHandled(true);
    }
}
