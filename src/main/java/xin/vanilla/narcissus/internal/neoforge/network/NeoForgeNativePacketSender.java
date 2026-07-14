package xin.vanilla.narcissus.internal.neoforge.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import xin.vanilla.banira.api.BaniraServer;

/** 隔离原版数据包广播，避免业务网络层依赖加载器通道。 */
public final class NeoForgeNativePacketSender {
    private NeoForgeNativePacketSender() {
    }

    public static void broadcast(Packet<?> packet) {
        MinecraftServer server = BaniraServer.currentAs(MinecraftServer.class);
        if (server != null) {
            server.getPlayerList().broadcastAll(packet);
        }
    }
}
