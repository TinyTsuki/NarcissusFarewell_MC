package xin.vanilla.narcissus.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import xin.vanilla.banira.common.network.packet.SplitPacket;
import xin.vanilla.narcissus.NarcissusFarewell;
import xin.vanilla.narcissus.network.packet.*;

import java.util.List;
import java.util.function.BiConsumer;

@EventBusSubscriber(modid = NarcissusFarewell.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class NetworkInit {

    private NetworkInit() {
    }

    /**
     * 保留空方法，便于主类构造函数中仍调用（载荷实际在 {@link #registerPayloadHandlers} 注册）。
     */
    public static void registerPackets() {
    }

    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar reg = event.registrar("1").optional();

        reg.playToClient(PlayerDataSyncPacket.TYPE, PlayerDataSyncPacket.STREAM_CODEC, NetworkInit::onPlayerDataSplitFragment);
        reg.playToClient(WaypointSyncToClient.TYPE, WaypointSyncToClient.STREAM_CODEC, WaypointSyncToClient::handle);
        reg.playToClient(StageDataSyncToClient.TYPE, StageDataSyncToClient.STREAM_CODEC, StageDataSyncToClient::handle);
        reg.playToClient(CostConfigSyncToClient.TYPE, CostConfigSyncToClient.STREAM_CODEC, CostConfigSyncToClient::handle);

        reg.playToServer(TpBackToServer.TYPE, TpBackToServer.STREAM_CODEC, TpBackToServer::handle);
        reg.playToServer(TpGraveToServer.TYPE, TpGraveToServer.STREAM_CODEC, TpGraveToServer::handle);
        reg.playToServer(TpHomeToServer.TYPE, TpHomeToServer.STREAM_CODEC, TpHomeToServer::handle);
        reg.playToServer(TpNoToServer.TYPE, TpNoToServer.STREAM_CODEC, TpNoToServer::handle);
        reg.playToServer(TpYesToServer.TYPE, TpYesToServer.STREAM_CODEC, TpYesToServer::handle);

        reg.playToServer(WaypointTeleportToServer.TYPE, WaypointTeleportToServer.STREAM_CODEC, WaypointTeleportToServer::handle);
        reg.playToServer(WaypointDelToServer.TYPE, WaypointDelToServer.STREAM_CODEC, WaypointDelToServer::handle);
        reg.playToServer(WaypointAddHomeToServer.TYPE, WaypointAddHomeToServer.STREAM_CODEC, WaypointAddHomeToServer::handle);
        reg.playToServer(WaypointAddStageToServer.TYPE, WaypointAddStageToServer.STREAM_CODEC, WaypointAddStageToServer::handle);

        reg.playToServer(AccessListEditToServer.TYPE, AccessListEditToServer.STREAM_CODEC, AccessListEditToServer::handle);
        reg.playToServer(PlayerConfigSyncToServer.TYPE, PlayerConfigSyncToServer.STREAM_CODEC, PlayerConfigSyncToServer::handle);
    }

    private static void onPlayerDataSplitFragment(PlayerDataSyncPacket payload, IPayloadContext ctx) {
        dispatchSplitClientPayload(payload, ctx, PlayerDataSyncPacket::handle);
    }

    private static <T extends SplitPacket & CustomPacketPayload> void dispatchSplitClientPayload(
            T payload,
            IPayloadContext ctx,
            BiConsumer<T, IPayloadContext> onMerged) {
        List<T> complete = SplitPacket.handle(payload);
        if (complete != null && !complete.isEmpty()) {
            T merged = SplitPacket.merge(complete);
            if (merged != null) {
                ctx.enqueueWork(() -> onMerged.accept(merged, ctx));
            }
        }
    }
}
