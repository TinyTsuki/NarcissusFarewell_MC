package xin.vanilla.narcissus.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SafeBlockTest {
    @Test
    public void normalizesLegacyGrassPathWithOptionalState() {
        assertEquals("minecraft:dirt_path", SafeBlock.normalizeBlockStateId("minecraft:grass_path"));
        assertEquals("minecraft:dirt_path[waterlogged=false]",
                SafeBlock.normalizeBlockStateId("minecraft:grass_path[waterlogged=false]"));
        assertEquals("example:grass_path", SafeBlock.normalizeBlockStateId("example:grass_path"));
    }
}
