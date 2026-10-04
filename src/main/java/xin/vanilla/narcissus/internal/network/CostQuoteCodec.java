package xin.vanilla.narcissus.internal.network;

import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.narcissus.data.cost.*;
import xin.vanilla.narcissus.enums.*;
import xin.vanilla.narcissus.network.packet.CostCapabilitiesToClient;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Framed primitive codec: every read debits the declared bounded payload first. */
public final class CostQuoteCodec {
    public static final int REQUEST_LIMIT = 4096, RESPONSE_LIMIT = 8192;
    private CostQuoteCodec() { }

    public static void writeRequest(BaniraPacketBuffer buffer, CostQuoteRequest request) {
        int size = requestSize(request); frame(buffer, size, REQUEST_LIMIT); writeIdentity(buffer, request);
    }
    public static CostQuoteRequest readRequest(BaniraPacketBuffer buffer) {
        Reader reader = new Reader(buffer, REQUEST_LIMIT); CostQuoteRequest result = reader.request(); reader.finish(); return result;
    }
    public static void writeQuote(BaniraPacketBuffer buffer, CostQuote quote) {
        frame(buffer, requestSize(quote.request()) + 28, RESPONSE_LIMIT); writeIdentity(buffer, quote.request());
        buffer.writeInt(quote.status().ordinal()); buffer.writeInt(quote.costType().ordinal());
        buffer.writeInt(quote.originalAmount()); buffer.writeInt(quote.cardAmount()); buffer.writeInt(quote.resourceAmount());
        buffer.writeInt((quote.bypassCooldown() ? 1 : 0) | (quote.commandPending() ? 2 : 0)); buffer.writeInt(quote.failure().ordinal());
    }
    public static CostQuote readQuote(BaniraPacketBuffer buffer) {
        Reader reader = new Reader(buffer, RESPONSE_LIMIT); CostQuoteRequest request = reader.request();
        CostQuote.Status status = reader.enumeration(CostQuote.Status.values()); EnumCostType type = reader.enumeration(EnumCostType.values());
        int amount = reader.integer(), cards = reader.integer(), resource = reader.integer(), flags = reader.integer();
        if ((flags & ~3) != 0) throw new IllegalArgumentException("Unknown quote flags");
        CostQuote result = new CostQuote(request, status, type, amount, cards, resource, (flags & 1) != 0, (flags & 2) != 0,
                reader.enumeration(EnumCostFailure.values()));
        reader.finish(); return result;
    }
    public static void writeCapabilities(BaniraPacketBuffer buffer, UUID session, long generation) {
        if (session == null || generation < 1) throw new IllegalArgumentException("Invalid cost capabilities");
        frame(buffer, 24, 28); uuid(buffer, session); buffer.writeLong(generation);
    }
    public static CostCapabilitiesToClient readCapabilities(BaniraPacketBuffer buffer) {
        Reader reader = new Reader(buffer, 28); UUID session = reader.uuid(); long generation = reader.longValue(); reader.finish();
        return new CostCapabilitiesToClient(session, generation);
    }
    private static int requestSize(CostQuoteRequest request) {
        CostQuoteTarget target = request.target();
        return 32 + 8 + 16 + 8 + CostQuoteTarget.checkedBytes(target.dimension()).length
                + CostQuoteTarget.checkedBytes(target.name()).length + 24 + 4 + CostQuoteTarget.checkedBytes(target.historyTime()).length + 4;
    }
    private static void frame(BaniraPacketBuffer buffer, int size, int limit) {
        if (size < 0 || size > limit - 4) throw new IllegalArgumentException("Quote packet exceeds limit");
        buffer.writeInt(size);
    }
    private static void uuid(BaniraPacketBuffer buffer, UUID id) { buffer.writeLong(id.getMostSignificantBits()); buffer.writeLong(id.getLeastSignificantBits()); }
    private static void text(BaniraPacketBuffer buffer, String text) {
        byte[] bytes = CostQuoteTarget.checkedBytes(text); buffer.writeInt(bytes.length); for (byte b : bytes) buffer.writeByte(b);
    }
    private static void writeIdentity(BaniraPacketBuffer buffer, CostQuoteRequest request) {
        uuid(buffer, request.sessionId()); buffer.writeLong(request.requestId()); buffer.writeLong(request.generationId());
        CostQuoteTarget target = request.target();
        buffer.writeInt(target.kind().ordinal()); buffer.writeInt(target.teleportType().ordinal()); uuid(buffer, target.owner());
        text(buffer, target.dimension()); text(buffer, target.name());
        buffer.writeDouble(target.x()); buffer.writeDouble(target.y()); buffer.writeDouble(target.z());
        text(buffer, target.historyTime()); buffer.writeInt(target.historyType().ordinal());
    }

    private static final class Reader {
        final BaniraPacketBuffer buffer;
        int remaining;
        Reader(BaniraPacketBuffer buffer, int limit) {
            this.buffer = buffer; remaining = buffer.readInt();
            if (remaining < 0 || remaining > limit - 4) throw new IllegalArgumentException("Invalid quote packet size");
        }
        void take(int count) {
            if (count < 0 || count > remaining) throw new IllegalArgumentException("Truncated quote payload");
            remaining -= count;
        }
        int integer() { take(4); return buffer.readInt(); }
        long longValue() { take(8); return buffer.readLong(); }
        double number() { take(8); return buffer.readDouble(); }
        UUID uuid() { return new UUID(longValue(), longValue()); }
        <T> T enumeration(T[] values) {
            int index = integer();
            if (index < 0 || index >= values.length) throw new IllegalArgumentException("Unknown quote enum value");
            return values[index];
        }
        String text() {
            int size = integer();
            if (size < 0 || size > 1024) throw new IllegalArgumentException("Quote string exceeds byte limit");
            take(size);
            byte[] bytes = new byte[size]; for (int i = 0; i < size; i++) bytes[i] = buffer.readByte();
            try {
                java.nio.charset.CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT);
                CharBuffer characters = CharBuffer.allocate(256);
                java.nio.charset.CoderResult result = decoder.decode(ByteBuffer.wrap(bytes), characters, true);
                if (result.isOverflow()) throw new IllegalArgumentException("Quote string exceeds character limit");
                if (result.isError()) result.throwException();
                result = decoder.flush(characters);
                if (result.isOverflow()) throw new IllegalArgumentException("Quote string exceeds character limit");
                if (result.isError()) result.throwException();
                characters.flip(); return characters.toString();
            } catch (CharacterCodingException e) { throw new IllegalArgumentException("Malformed quote UTF-8", e); }
        }
        CostQuoteRequest request() {
            UUID session = uuid(); long request = longValue(), generation = longValue();
            if (request < 1 || generation < 1) throw new IllegalArgumentException("Invalid quote identity");
            CostQuoteTarget target = new CostQuoteTarget(enumeration(CostQuoteTarget.Kind.values()), enumeration(EnumTeleportType.values()),
                    uuid(), text(), text(), number(), number(), number(), text(), enumeration(EnumTeleportType.values()));
            return new CostQuoteRequest(session, request, generation, target);
        }
        void finish() {
            if (remaining != 0) throw new IllegalArgumentException("Unexpected quote payload suffix");
            // Banira exposes no remaining-byte query; native packets have an independent buffer.
            try { buffer.readByte(); }
            catch (IndexOutOfBoundsException endOfPacket) { return; }
            throw new IllegalArgumentException("Undeclared quote packet suffix");
        }
    }
}
