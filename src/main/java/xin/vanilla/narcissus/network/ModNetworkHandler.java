package xin.vanilla.narcissus.network;

import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import xin.vanilla.narcissus.network.packet.*;
import xin.vanilla.narcissus.util.Identifier;

public class ModNetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    private static int ID = 0;
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            Identifier.create("main_network"),
            () -> PROTOCOL_VERSION,
            clientVersion -> true,      // 客户端版本始终有效
            serverVersion -> true       // 服务端版本始终有效
    );

    public static int nextID() {
        return ID++;
    }

    public static void registerPackets() {
        INSTANCE.registerMessage(nextID(), CustomConfigSyncToClient.class, CustomConfigSyncToClient::toBytes, CustomConfigSyncToClient::new, CustomConfigSyncToClient::handle);
        INSTANCE.registerMessage(nextID(), PlayerDataSyncToClient.class, PlayerDataSyncToClient::toBytes, PlayerDataSyncToClient::new, PlayerDataSyncToClient::handle);
        INSTANCE.registerMessage(nextID(), WaypointSyncToClient.class, WaypointSyncToClient::toBytes, WaypointSyncToClient::new, WaypointSyncToClient::handle);
        INSTANCE.registerMessage(nextID(), StageDataSyncToClient.class, StageDataSyncToClient::toBytes, StageDataSyncToClient::new, StageDataSyncToClient::handle);
        INSTANCE.registerMessage(nextID(), CostConfigSyncToClient.class, CostConfigSyncToClient::toBytes, CostConfigSyncToClient::new, CostConfigSyncToClient::handle);

        INSTANCE.registerMessage(nextID(), ModLoadedToBoth.class, ModLoadedToBoth::toBytes, ModLoadedToBoth::new, ModLoadedToBoth::handle);
        INSTANCE.registerMessage(nextID(), WaypointTeleportToServer.class, WaypointTeleportToServer::toBytes, WaypointTeleportToServer::new, WaypointTeleportToServer::handle);
        INSTANCE.registerMessage(nextID(), WaypointDelToServer.class, WaypointDelToServer::toBytes, WaypointDelToServer::new, WaypointDelToServer::handle);

        INSTANCE.registerMessage(nextID(), TpBackToServer.class, TpBackToServer::toBytes, TpBackToServer::new, TpBackToServer::handle);
        INSTANCE.registerMessage(nextID(), TpGraveToServer.class, TpGraveToServer::toBytes, TpGraveToServer::new, TpGraveToServer::handle);
        INSTANCE.registerMessage(nextID(), TpHomeToServer.class, TpHomeToServer::toBytes, TpHomeToServer::new, TpHomeToServer::handle);
        INSTANCE.registerMessage(nextID(), TpNoToServer.class, TpNoToServer::toBytes, TpNoToServer::new, TpNoToServer::handle);
        INSTANCE.registerMessage(nextID(), TpYesToServer.class, TpYesToServer::toBytes, TpYesToServer::new, TpYesToServer::handle);
    }
}
