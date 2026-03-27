package xin.vanilla.narcissus.network;

import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;
import xin.vanilla.banira.common.network.NetworkHandler;
import xin.vanilla.narcissus.Identifier;
import xin.vanilla.narcissus.network.packet.*;

public final class NetworkInit {

    private static final NetworkHandler HANDLER = NetworkHandler.create("main_network", Identifier.id());

    public static final SimpleChannel INSTANCE = HANDLER.getChannel();

    public static void registerPackets() {
        HANDLER.registerSplit(PlayerDataSyncToClient.class, PlayerDataSyncToClient::toBytes, PlayerDataSyncToClient::new, PlayerDataSyncToClient::handle);
        HANDLER.register(WaypointSyncToClient.class, WaypointSyncToClient::toBytes, WaypointSyncToClient::new, WaypointSyncToClient::handle);
        HANDLER.register(StageDataSyncToClient.class, StageDataSyncToClient::toBytes, StageDataSyncToClient::new, StageDataSyncToClient::handle);
        HANDLER.register(CostConfigSyncToClient.class, CostConfigSyncToClient::toBytes, CostConfigSyncToClient::new, CostConfigSyncToClient::handle);

        HANDLER.register(WaypointTeleportToServer.class, WaypointTeleportToServer::toBytes, WaypointTeleportToServer::new, WaypointTeleportToServer::handle);
        HANDLER.register(WaypointDelToServer.class, WaypointDelToServer::toBytes, WaypointDelToServer::new, WaypointDelToServer::handle);
        HANDLER.register(WaypointAddHomeToServer.class, WaypointAddHomeToServer::toBytes, WaypointAddHomeToServer::new, WaypointAddHomeToServer::handle);
        HANDLER.register(WaypointAddStageToServer.class, WaypointAddStageToServer::toBytes, WaypointAddStageToServer::new, WaypointAddStageToServer::handle);

        HANDLER.register(TpBackToServer.class, TpBackToServer::toBytes, TpBackToServer::new, TpBackToServer::handle);
        HANDLER.register(TpGraveToServer.class, TpGraveToServer::toBytes, TpGraveToServer::new, TpGraveToServer::handle);
        HANDLER.register(TpHomeToServer.class, TpHomeToServer::toBytes, TpHomeToServer::new, TpHomeToServer::handle);
        HANDLER.register(TpNoToServer.class, TpNoToServer::toBytes, TpNoToServer::new, TpNoToServer::handle);
        HANDLER.register(TpYesToServer.class, TpYesToServer::toBytes, TpYesToServer::new, TpYesToServer::handle);

        HANDLER.register(AccessListEditToServer.class, AccessListEditToServer::toBytes, AccessListEditToServer::new, AccessListEditToServer::handle);
        HANDLER.register(PlayerConfigSyncToServer.class, PlayerConfigSyncToServer::toBytes, PlayerConfigSyncToServer::new, PlayerConfigSyncToServer::handle);
    }
}
