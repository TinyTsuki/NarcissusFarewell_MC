package xin.vanilla.narcissus.internal.network;

import net.minecraft.nbt.CompoundNBT;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NarcissusNbtPacketCodecTest {

    @Test
    public void snbtRoundTripPreservesNestedValues() {
        CompoundNBT nested = new CompoundNBT();
        nested.putBoolean("enabled", true);
        CompoundNBT source = new CompoundNBT();
        source.putString("name", "narcissus");
        source.putInt("count", 7);
        source.put("nested", nested);

        CompoundNBT decoded = NarcissusNbtPacketCodec.deserialize(
                NarcissusNbtPacketCodec.serialize(source));

        assertEquals(source, decoded);
    }
}
