package xin.vanilla.narcissus.network.packet;

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
}
