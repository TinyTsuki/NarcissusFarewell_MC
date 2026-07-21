package xin.vanilla.narcissus.internal.fabric.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import xin.vanilla.banira.api.BaniraServer;

/** Fabric 原生数据包广播只封装在加载器内部。 */
public final class FabricNativePacketSender {
    private FabricNativePacketSender() {
    }

    public static void broadcast(Packet<?> packet) {
        MinecraftServer server = BaniraServer.currentAs(MinecraftServer.class);
        if (server != null) {
            server.getPlayerList().broadcastAll(packet);
        }
    }
}
