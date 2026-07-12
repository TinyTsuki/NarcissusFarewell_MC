package xin.vanilla.narcissus.internal.forge.network;

import net.minecraft.network.IPacket;
import xin.vanilla.banira.BaniraCodex;

/**
 * 隔离 1.16.5 原生数据包广播，避免业务代码误用 Banira 自定义包通道。
 */
public final class ForgeNativePacketSender {
    private ForgeNativePacketSender() {
    }

    public static void broadcast(IPacket<?> packet) {
        if (BaniraCodex.serverInstance().key() != null) {
            BaniraCodex.serverInstance().key().getPlayerList().broadcastAll(packet);
        }
    }
}
