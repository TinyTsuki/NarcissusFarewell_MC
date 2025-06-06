package xin.vanilla.narcissus.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import xin.vanilla.narcissus.enums.EnumCommandType;
import xin.vanilla.narcissus.util.NarcissusUtils;

import java.util.function.Supplier;

public class TpHomeNotice {

    public TpHomeNotice() {
    }

    public TpHomeNotice(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public static void handle(TpHomeNotice packet, Supplier<NetworkEvent.Context> ctx) {
        // 获取网络事件上下文并排队执行工作
        ctx.get().enqueueWork(() -> {
            // 获取发送数据包的玩家实体
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                NarcissusUtils.executeCommand(player, NarcissusUtils.getCommand(EnumCommandType.TP_HOME));
            }
        });
        // 设置数据包已处理状态，防止重复处理
        ctx.get().setPacketHandled(true);
    }
}
