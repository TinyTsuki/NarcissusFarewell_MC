package xin.vanilla.narcissus.network;

import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.SimpleChannel;
import xin.vanilla.narcissus.network.packet.*;
import xin.vanilla.narcissus.util.Identifier;

public class ModNetworkHandler {
    private static final int PROTOCOL_VERSION = 1;
    private static int ID = 0;
    public static final SimpleChannel INSTANCE = ChannelBuilder.named(Identifier.create("main_network"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .clientAcceptedVersions((status, version) -> true)    // 客户端版本始终有效
            .serverAcceptedVersions((status, version) -> true)    // 服务端版本始终有效
            .simpleChannel();

    public static int nextID() {
        return ID++;
    }

    public static void registerPackets() {
        INSTANCE.messageBuilder(CustomConfigSyncToClient.class, nextID()).encoder(CustomConfigSyncToClient::toBytes).decoder(CustomConfigSyncToClient::new).consumerMainThread(CustomConfigSyncToClient::handle).add();
        INSTANCE.messageBuilder(PlayerDataSyncToClient.class, nextID()).encoder(PlayerDataSyncToClient::toBytes).decoder(PlayerDataSyncToClient::new).consumerMainThread(PlayerDataSyncToClient::handle).add();
        INSTANCE.messageBuilder(WaypointSyncToClient.class, nextID()).encoder(WaypointSyncToClient::toBytes).decoder(WaypointSyncToClient::new).consumerMainThread(WaypointSyncToClient::handle).add();
        INSTANCE.messageBuilder(StageDataSyncToClient.class, nextID()).encoder(StageDataSyncToClient::toBytes).decoder(StageDataSyncToClient::new).consumerMainThread(StageDataSyncToClient::handle).add();
        INSTANCE.messageBuilder(CostConfigSyncToClient.class, nextID()).encoder(CostConfigSyncToClient::toBytes).decoder(CostConfigSyncToClient::new).consumerMainThread(CostConfigSyncToClient::handle).add();

        INSTANCE.messageBuilder(ModLoadedToBoth.class, nextID()).encoder(ModLoadedToBoth::toBytes).decoder(ModLoadedToBoth::new).consumerMainThread(ModLoadedToBoth::handle).add();
        INSTANCE.messageBuilder(WaypointTeleportToServer.class, nextID()).encoder(WaypointTeleportToServer::toBytes).decoder(WaypointTeleportToServer::new).consumerMainThread(WaypointTeleportToServer::handle).add();
        INSTANCE.messageBuilder(WaypointDelToServer.class, nextID()).encoder(WaypointDelToServer::toBytes).decoder(WaypointDelToServer::new).consumerMainThread(WaypointDelToServer::handle).add();

        INSTANCE.messageBuilder(TpBackToServer.class, nextID()).encoder(TpBackToServer::toBytes).decoder(TpBackToServer::new).consumerMainThread(TpBackToServer::handle).add();
        INSTANCE.messageBuilder(TpGraveToServer.class, nextID()).encoder(TpGraveToServer::toBytes).decoder(TpGraveToServer::new).consumerMainThread(TpGraveToServer::handle).add();
        INSTANCE.messageBuilder(TpHomeToServer.class, nextID()).encoder(TpHomeToServer::toBytes).decoder(TpHomeToServer::new).consumerMainThread(TpHomeToServer::handle).add();
        INSTANCE.messageBuilder(TpNoToServer.class, nextID()).encoder(TpNoToServer::toBytes).decoder(TpNoToServer::new).consumerMainThread(TpNoToServer::handle).add();
        INSTANCE.messageBuilder(TpYesToServer.class, nextID()).encoder(TpYesToServer::toBytes).decoder(TpYesToServer::new).consumerMainThread(TpYesToServer::handle).add();
    }
}
