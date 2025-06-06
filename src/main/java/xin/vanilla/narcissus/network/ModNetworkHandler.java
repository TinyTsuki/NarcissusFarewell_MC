package xin.vanilla.narcissus.network;

import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.network.packet.*;

public class ModNetworkHandler {
    private static final int PROTOCOL_VERSION = 1;
    private static int ID = 0;
    public static final SimpleChannel INSTANCE = ChannelBuilder.named(NarcissusFarewell.createResource("main_network"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions((status, version) -> true)    // 客户端版本始终有效
            .serverAcceptedVersions((status, version) -> true)    // 服务端版本始终有效
            .simpleChannel();

    public static int nextID() {
        return ID++;
    }

    public static void registerPackets() {
        INSTANCE.messageBuilder(PlayerDataSyncPacket.class, nextID()).encoder(PlayerDataSyncPacket::toBytes).decoder(PlayerDataSyncPacket::new).consumerMainThread(PlayerDataSyncPacket::handle).add();
        INSTANCE.messageBuilder(PlayerDataReceivedNotice.class, nextID()).encoder(PlayerDataReceivedNotice::toBytes).decoder(PlayerDataReceivedNotice::new).consumerMainThread(PlayerDataReceivedNotice::handle).add();
        INSTANCE.messageBuilder(ClientModLoadedNotice.class, nextID()).encoder(ClientModLoadedNotice::toBytes).decoder(ClientModLoadedNotice::new).consumerMainThread(ClientModLoadedNotice::handle).add();
        INSTANCE.messageBuilder(TpHomeNotice.class, nextID()).encoder(TpHomeNotice::toBytes).decoder(TpHomeNotice::new).consumerMainThread(TpHomeNotice::handle).add();
        INSTANCE.messageBuilder(TpBackNotice.class, nextID()).encoder(TpBackNotice::toBytes).decoder(TpBackNotice::new).consumerMainThread(TpBackNotice::handle).add();
        INSTANCE.messageBuilder(TpYesNotice.class, nextID()).encoder(TpYesNotice::toBytes).decoder(TpYesNotice::new).consumerMainThread(TpYesNotice::handle).add();
        INSTANCE.messageBuilder(TpNoNotice.class, nextID()).encoder(TpNoNotice::toBytes).decoder(TpNoNotice::new).consumerMainThread(TpNoNotice::handle).add();
    }
}
