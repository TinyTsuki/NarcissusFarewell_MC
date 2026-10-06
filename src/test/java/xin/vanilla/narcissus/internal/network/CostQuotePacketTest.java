package xin.vanilla.narcissus.internal.network;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import org.junit.Test;
import xin.vanilla.banira.common.network.BaniraPacketBuffer;
import xin.vanilla.narcissus.data.cost.CostPaymentPlan;
import xin.vanilla.narcissus.data.cost.CostQuote;
import xin.vanilla.narcissus.data.cost.CostQuoteRequest;
import xin.vanilla.narcissus.data.cost.CostQuoteTarget;
import xin.vanilla.narcissus.enums.EnumCostFailure;
import xin.vanilla.narcissus.enums.EnumCostType;
import xin.vanilla.narcissus.enums.EnumTeleportType;
import xin.vanilla.narcissus.network.packet.CostCapabilitiesToClient;
import xin.vanilla.narcissus.network.packet.CostQuoteToClient;
import xin.vanilla.narcissus.network.packet.CostQuoteToServer;

import java.lang.reflect.Proxy;
import java.util.UUID;

import static org.junit.Assert.*;

public class CostQuotePacketTest {
    private final UUID session = UUID.randomUUID();
    private final UUID player = UUID.randomUUID();
    private final PacketBuffer raw = new PacketBuffer(Unpooled.buffer());
    private final BaniraPacketBuffer buffer = (BaniraPacketBuffer) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[]{BaniraPacketBuffer.class}, (p, m, a) -> {
                switch (m.getName()) {
                    case "readByte":
                        return raw.readByte();
                    case "writeByte":
                        raw.writeByte((int) a[0]);
                        return null;
                    case "readInt":
                        return raw.readInt();
                    case "writeInt":
                        raw.writeInt((int) a[0]);
                        return null;
                    case "readLong":
                        return raw.readLong();
                    case "writeLong":
                        raw.writeLong((long) a[0]);
                        return null;
                    case "readDouble":
                        return raw.readDouble();
                    case "writeDouble":
                        raw.writeDouble((double) a[0]);
                        return null;
                    default:
                        throw new AssertionError("Unbounded or untested buffer method: " + m.getName());
                }
            });

    private CostQuoteRequest request(String name) {
        return new CostQuoteRequest(session, 17, 3, CostQuoteTarget.home(player, "minecraft:overworld", name));
    }

    @Test
    public void requestResponseAndCapabilityRoundTripUsingNativeBuffer() {
        CostQuoteRequest request = request("home,\u4e2d\u6587");
        new CostQuoteToServer(request).toBytes(buffer);
        assertTrue(raw.readableBytes() <= 4096);
        assertEquals(request, new CostQuoteToServer(buffer).request());
        assertEquals(0, raw.readableBytes());
        raw.clear();
        CostQuote quote = CostQuote.ready(request, EnumCostType.EXP_POINT, CostPaymentPlan.ready(7, 2, 5, true, false));
        new CostQuoteToClient(quote).toBytes(buffer);
        assertTrue(raw.readableBytes() <= 8192);
        assertEquals(quote, new CostQuoteToClient(buffer).quote());
        assertEquals(0, raw.readableBytes());
        raw.clear();
        new CostCapabilitiesToClient(session, 3).toBytes(buffer);
        CostCapabilitiesToClient capability = new CostCapabilitiesToClient(buffer);
        assertEquals(session, capability.sessionId());
        assertEquals(3, capability.generationId());
    }

    @Test
    public void oversizedNegativeAndTruncatedEnvelopesAreRejectedBeforePayloadReads() {
        for (int size : new int[]{-1, 4093, Integer.MAX_VALUE}) {
            raw.clear();
            raw.writeInt(size);
            reject(() -> new CostQuoteToServer(buffer));
            assertEquals(4, raw.readerIndex());
        }
        raw.clear();
        raw.writeInt(8189);
        reject(() -> new CostQuoteToClient(buffer));
        assertEquals(4, raw.readerIndex());
        raw.clear();
        new CostQuoteToServer(request("a")).toBytes(buffer);
        raw.writerIndex(raw.writerIndex() - 1);
        reject(() -> new CostQuoteToServer(buffer));
    }

    @Test
    public void stringLengthIsCheckedBeforeAllocatingOrReadingBytes() {
        new CostQuoteToServer(request("a")).toBytes(buffer);
        // Envelope + request identity + target kind/type/owner + dimension string.
        int nameOffset = 4 + 16 + 8 + 8 + 4 + 4 + 16 + 4 + "minecraft:overworld".length();
        raw.setInt(nameOffset, Integer.MAX_VALUE);
        reject(() -> new CostQuoteToServer(buffer));
        assertEquals(nameOffset + 4, raw.readerIndex());
    }

    @Test
    public void characterByteAndMalformedUnicodeLimitsAreStrict() {
        reject(() -> request(repeat("x", 257)));
        reject(() -> request("\ud800"));
        String astral = repeat("\ud83c\udf38", 128);
        new CostQuoteToServer(request(astral)).toBytes(buffer);
        assertEquals(astral, new CostQuoteToServer(buffer).request().target().name());
        raw.clear();
        new CostQuoteToServer(request("a")).toBytes(buffer);
        int nameOffset = 4 + 16 + 8 + 8 + 4 + 4 + 16 + 4 + "minecraft:overworld".length();
        raw.setByte(nameOffset + 4, 0xff);
        reject(() -> new CostQuoteToServer(buffer));
    }

    @Test
    public void unknownEnumsNonFiniteCoordinatesAndWrongShapeAreRejected() {
        new CostQuoteToServer(request("a")).toBytes(buffer);
        raw.setInt(4 + 16 + 8 + 8, Integer.MAX_VALUE);
        reject(() -> new CostQuoteToServer(buffer));
        reject(() -> CostQuoteTarget.coordinate("minecraft:overworld", Double.POSITIVE_INFINITY, 1, 2));
        reject(() -> new CostQuoteRequest(session, 0, 1, request("a").target()));
        raw.clear();
        new CostQuoteToServer(request("a")).toBytes(buffer);
        raw.setInt(0, raw.getInt(0) + 1);
        raw.writeByte(0);
        reject(() -> new CostQuoteToServer(buffer));
    }

    @Test
    public void byteLengthAndRemainingBudgetRejectBeforeReadingStringPayload() {
        for (int size : new int[]{-1, 1025, 1024}) {
            raw.clear();
            new CostQuoteToServer(request("a")).toBytes(buffer);
            int offset = nameOffset();
            raw.setInt(offset, size);
            reject(() -> new CostQuoteToServer(buffer));
            assertEquals(offset + 4, raw.readerIndex());
        }
    }

    @Test
    public void decodedCharacterLimitAndEveryTargetShapeRoundTrip() {
        new CostQuoteToServer(request(repeat("x", 256))).toBytes(buffer);
        assertEquals(256, new CostQuoteToServer(buffer).request().target().name().length());
        raw.clear();
        new CostQuoteToServer(request(repeat("x", 256))).toBytes(buffer);
        replaceName(repeat("x", 257).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        reject(() -> new CostQuoteToServer(buffer));
        for (CostQuoteTarget target : new CostQuoteTarget[]{CostQuoteTarget.stage("minecraft:overworld", "stage"),
                CostQuoteTarget.history(player, EnumTeleportType.TP_BACK, 1700000000123L, EnumTeleportType.TP_HOME, "minecraft:overworld", 1, 2, 3),
                CostQuoteTarget.player(player, EnumTeleportType.TP_HERE), CostQuoteTarget.coordinate("minecraft:the_nether", 3, 4, 5),
                CostQuoteTarget.unknown(EnumTeleportType.TP_VIEW)}) {
            raw.clear();
            CostQuoteRequest request = new CostQuoteRequest(session, 1, 1, target);
            new CostQuoteToServer(request).toBytes(buffer);
            assertEquals(request, new CostQuoteToServer(buffer).request());
        }
    }

    @Test
    public void unavailableResultsNeverBecomeZeroPriceAndUnknownResponseFlagsAreRejected() {
        CostQuote quote = CostQuote.unavailable(request("a"), CostQuote.Status.UNKNOWN, EnumCostFailure.UNKNOWN_TARGET);
        new CostQuoteToClient(quote).toBytes(buffer);
        assertFalse(new CostQuoteToClient(buffer).quote().amount().isPresent());
        raw.clear();
        new CostQuoteToClient(quote).toBytes(buffer);
        raw.setInt(raw.writerIndex() - 8, 8);
        reject(() -> new CostQuoteToClient(buffer));
    }

    @Test
    public void actualPacketSuffixCannotBypassDeclaredFrameSize() {
        for (int suffix : new int[]{1, 4096}) {
            raw.clear();
            new CostQuoteToServer(request("a")).toBytes(buffer);
            raw.writeZero(suffix);
            reject(() -> new CostQuoteToServer(buffer));
        }
        raw.clear();
        new CostQuoteToClient(CostQuote.unavailable(request("a"), CostQuote.Status.UNKNOWN,
                EnumCostFailure.UNKNOWN_TARGET)).toBytes(buffer);
        raw.writeZero(8192);
        reject(() -> new CostQuoteToClient(buffer));
        raw.clear();
        new CostCapabilitiesToClient(session, 1).toBytes(buffer);
        raw.writeByte(0);
        reject(() -> new CostCapabilitiesToClient(buffer));
    }

    private int nameOffset() {
        return 4 + 16 + 8 + 8 + 4 + 4 + 16 + 4 + "minecraft:overworld".length();
    }

    private void replaceName(byte[] bytes) {
        int offset = nameOffset(), oldLength = raw.getInt(offset);
        byte[] suffix = new byte[raw.writerIndex() - offset - 4 - oldLength];
        raw.getBytes(offset + 4 + oldLength, suffix);
        raw.ensureWritable(bytes.length + suffix.length);
        raw.setInt(offset, bytes.length);
        raw.setBytes(offset + 4, bytes);
        raw.setBytes(offset + 4 + bytes.length, suffix);
        raw.writerIndex(offset + 4 + bytes.length + suffix.length);
        raw.setInt(0, raw.writerIndex() - 4);
        raw.readerIndex(0);
    }

    private static String repeat(String s, int n) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < n; i++) b.append(s);
        return b.toString();
    }

    private static void reject(Runnable run) {
        try {
            run.run();
            fail("Invalid payload accepted");
        } catch (IllegalArgumentException | IndexOutOfBoundsException expected) {
        }
    }
}
