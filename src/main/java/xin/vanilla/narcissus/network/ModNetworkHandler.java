package xin.vanilla.narcissus.network;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import xin.vanilla.narcissus.NarcissusFarewell;

public class ModNetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static int ID = 0;
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(NarcissusFarewell.MODID, "main_network"),
            () -> PROTOCOL_VERSION,
            clientVersion -> true,      // 客户端版本始终有效
            serverVersion -> true       // 服务端版本始终有效
    );

    public static int nextID() {
        return ID++;
    }

    public static void registerPackets() {
        INSTANCE.registerMessage(nextID(), PlayerDataSyncPacket.class, PlayerDataSyncPacket::toBytes, PlayerDataSyncPacket::new, PlayerDataSyncPacket::handle);
        INSTANCE.registerMessage(nextID(), PlayerDataReceivedNotice.class, PlayerDataReceivedNotice::toBytes, PlayerDataReceivedNotice::new, PlayerDataReceivedNotice::handle);
        INSTANCE.registerMessage(nextID(), ClientModLoadedNotice.class, ClientModLoadedNotice::toBytes, ClientModLoadedNotice::new, ClientModLoadedNotice::handle);
    }
}
