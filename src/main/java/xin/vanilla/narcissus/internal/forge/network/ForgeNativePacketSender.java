package xin.vanilla.narcissus.internal.forge.network;

import net.minecraft.network.IPacket;
import net.minecraft.server.MinecraftServer;
import xin.vanilla.banira.common.util.BaniraServerUtils;

/**
 * 隔离 1.16.5 原生数据包广播，避免业务代码误用 Banira 自定义包通道。
 */
public final class ForgeNativePacketSender {
    private ForgeNativePacketSender() {
    }

    public static void broadcast(IPacket<?> packet) {
        MinecraftServer server = BaniraServerUtils.currentServer();
        if (server != null) {
            server.getPlayerList().broadcastAll(packet);
        }
    }
}
