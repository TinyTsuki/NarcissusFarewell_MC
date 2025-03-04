package xin.vanilla.narcissus.network;


import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class ModNetworkHandler {
    private static final String CHANNEL_NAME = "main_network";
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
    private static int ID = 0;

    private static int nextID() {
        return ID++;
    }

    public static void registerPackets() {
        INSTANCE.registerMessage(PlayerDataSyncPacket.Handler.class, PlayerDataSyncPacket.class, nextID(), Side.CLIENT);
        INSTANCE.registerMessage(PlayerDataReceivedNotice.Handler.class, PlayerDataReceivedNotice.class, nextID(), Side.SERVER);
        INSTANCE.registerMessage(ClientModLoadedNotice.Handler.class, ClientModLoadedNotice.class, nextID(), Side.SERVER);
        INSTANCE.registerMessage(TpHomeNotice.Handler.class, TpHomeNotice.class, nextID(), Side.SERVER);
        INSTANCE.registerMessage(TpBackNotice.Handler.class, TpBackNotice.class, nextID(), Side.SERVER);
        INSTANCE.registerMessage(TpYesNotice.Handler.class, TpYesNotice.class, nextID(), Side.SERVER);
        INSTANCE.registerMessage(TpNoNotice.Handler.class, TpNoNotice.class, nextID(), Side.SERVER);
    }
}
