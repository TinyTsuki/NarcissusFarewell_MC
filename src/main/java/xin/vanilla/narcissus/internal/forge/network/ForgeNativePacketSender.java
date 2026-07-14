package xin.vanilla.narcissus.internal.forge.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import xin.vanilla.banira.api.BaniraServer;

/**
 * 隔离 Forge 原生数据包广播，避免业务代码误用 Banira 自定义包通道。
 */
public final class ForgeNativePacketSender {
    private ForgeNativePacketSender() {
    }

    public static void broadcast(Packet<?> packet) {
        MinecraftServer server = BaniraServer.currentAs(MinecraftServer.class);
        if (server != null) {
            server.getPlayerList().broadcastAll(packet);
        }
    }
}
