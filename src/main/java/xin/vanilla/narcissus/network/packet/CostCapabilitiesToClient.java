package xin.vanilla.narcissus.network.packet;

import xin.vanilla.narcissus.internal.client.NarcissusClientSyncState;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import lombok.Value;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.narcissus.internal.network.CostQuoteCodec;
import xin.vanilla.narcissus.network.NetworkPacket;
import java.util.Objects;
import java.util.UUID;

@Value
@Accessors(fluent = true)
public class CostCapabilitiesToClient implements NetworkPacket {
    UUID sessionId;
    long generationId;
    public CostCapabilitiesToClient(UUID sessionId, long generationId) {
        this.sessionId = Objects.requireNonNull(sessionId);
        if (generationId < 1) throw new IllegalArgumentException("Invalid cost generation");
        this.generationId = generationId;
    }
    public CostCapabilitiesToClient(BaniraPacketBuffer buffer) {
        CostCapabilitiesToClient identity = CostQuoteCodec.readCapabilities(buffer);
        this.sessionId = identity.sessionId(); this.generationId = identity.generationId();
    }
    public void toBytes(BaniraPacketBuffer buffer) { CostQuoteCodec.writeCapabilities(buffer, sessionId, generationId); }
    public static void handle(CostCapabilitiesToClient packet, BaniraNetworkContext context) {
        context.enqueueWork(() -> {
            if (context.isClientSide()) {
                net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(net.minecraftforge.api.distmarker.Dist.CLIENT,
                        () -> () -> NarcissusClientSyncState.costQuotes().capabilities(packet.sessionId(), packet.generationId()));
            }
        });
        context.markHandled();
    }
}
