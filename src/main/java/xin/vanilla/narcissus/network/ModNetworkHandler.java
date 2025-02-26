package xin.vanilla.narcissus.network;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.network.packet.*;

public class ModNetworkHandler {
    public static void registerPackets(final RegisterPayloadHandlerEvent event) {
        final IPayloadRegistrar registrar = event.registrar(NarcissusFarewell.MODID).optional();

        registrar.play(PlayerDataSyncPacket.ID, PlayerDataSyncPacket::new,
                handler -> handler.client(PlayerDataSyncPacket::handle));
        registrar.play(PlayerDataReceivedNotice.ID, PlayerDataReceivedNotice::new,
                handler -> handler.server(PlayerDataReceivedNotice::handle));
        registrar.play(ClientModLoadedNotice.ID, ClientModLoadedNotice::new,
                handler -> handler.server(ClientModLoadedNotice::handle));
        registrar.play(TpHomeNotice.ID, TpHomeNotice::new,
                handler -> handler.server(TpHomeNotice::handle));
        registrar.play(TpBackNotice.ID, TpBackNotice::new,
                handler -> handler.server(TpBackNotice::handle));
        registrar.play(TpYesNotice.ID, TpYesNotice::new,
                handler -> handler.server(TpYesNotice::handle));
        registrar.play(TpNoNotice.ID, TpNoNotice::new,
                handler -> handler.server(TpNoNotice::handle));
    }
}
