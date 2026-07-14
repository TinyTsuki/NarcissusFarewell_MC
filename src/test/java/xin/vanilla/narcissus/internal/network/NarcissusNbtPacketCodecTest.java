package xin.vanilla.narcissus.internal.network;

import net.minecraft.nbt.CompoundTag;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NarcissusNbtPacketCodecTest {

    @Test
    public void snbtRoundTripPreservesNestedValues() {
        CompoundTag nested = new CompoundTag();
        nested.putBoolean("enabled", true);
        CompoundTag source = new CompoundTag();
        source.putString("name", "narcissus");
        source.putInt("count", 7);
        source.put("nested", nested);

        CompoundTag decoded = NarcissusNbtPacketCodec.deserialize(
                NarcissusNbtPacketCodec.serialize(source));

        assertEquals(source, decoded);
    }
}
