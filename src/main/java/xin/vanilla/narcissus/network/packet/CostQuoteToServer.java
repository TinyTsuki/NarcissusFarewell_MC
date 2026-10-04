package xin.vanilla.narcissus.network.packet;

import xin.vanilla.narcissus.internal.server.NarcissusCostService;
import net.minecraft.entity.player.ServerPlayerEntity;
import xin.vanilla.banira.common.network.BaniraNetworkContext;
import lombok.Value;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.narcissus.data.cost.CostQuoteRequest;
import xin.vanilla.narcissus.internal.network.CostQuoteCodec;
import xin.vanilla.narcissus.network.NetworkPacket;
import java.util.Objects;

@Value
@Accessors(fluent = true)
public class CostQuoteToServer implements NetworkPacket {
    CostQuoteRequest request;
    public CostQuoteToServer(CostQuoteRequest request) { this.request = Objects.requireNonNull(request); }
    public CostQuoteToServer(BaniraPacketBuffer buffer) { this(CostQuoteCodec.readRequest(buffer)); }
    public void toBytes(BaniraPacketBuffer buffer) { CostQuoteCodec.writeRequest(buffer, request); }
    public static void handle(CostQuoteToServer packet, BaniraNetworkContext context) {
        context.enqueueWork(() -> {
            if (!context.isClientSide() && context.sender() instanceof ServerPlayerEntity) {
                NarcissusCostService service = NarcissusCostService.get();
                if (service != null) service.quote((ServerPlayerEntity) context.sender(), packet.request());
            }
        });
        context.markHandled();
    }
}
