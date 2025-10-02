package xin.vanilla.narcissus.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import xin.vanilla.narcissus.network.packet.*;

public class ModNetworkHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static void registerPackets(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION).optional();

        registrar.playToClient(PlayerDataSyncPacket.TYPE, PlayerDataSyncPacket.STREAM_CODEC, PlayerDataSyncPacket::handle);
        registrar.playToServer(ClientModLoadedNotice.TYPE, ClientModLoadedNotice.STREAM_CODEC, ClientModLoadedNotice::handle);
        registrar.playToServer(TpHomeNotice.TYPE, TpHomeNotice.STREAM_CODEC, TpHomeNotice::handle);
        registrar.playToServer(TpBackNotice.TYPE, TpBackNotice.STREAM_CODEC, TpBackNotice::handle);
        registrar.playToServer(TpYesNotice.TYPE, TpYesNotice.STREAM_CODEC, TpYesNotice::handle);
        registrar.playToServer(TpNoNotice.TYPE, TpNoNotice.STREAM_CODEC, TpNoNotice::handle);
    }
}
