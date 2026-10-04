package xin.vanilla.narcissus.network.packet;

import lombok.Value;
import lombok.experimental.Accessors;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.narcissus.data.cost.CostQuote;
import xin.vanilla.narcissus.internal.network.CostQuoteCodec;
import xin.vanilla.narcissus.network.NetworkPacket;
import java.util.Objects;

@Value
@Accessors(fluent = true)
public class CostQuoteToClient implements NetworkPacket {
    CostQuote quote;
    public CostQuoteToClient(CostQuote quote) { this.quote = Objects.requireNonNull(quote); }
    public CostQuoteToClient(BaniraPacketBuffer buffer) { this(CostQuoteCodec.readQuote(buffer)); }
    public void toBytes(BaniraPacketBuffer buffer) { CostQuoteCodec.writeQuote(buffer, quote); }
}
