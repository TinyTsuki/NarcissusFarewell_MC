package xin.vanilla.narcissus.search;

import net.minecraft.world.level.block.Blocks;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class SafeBlockPolicyCompatibilityTest {
    @BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    public void legacyPathIsNativeSupportAndDeduplicatesModernName() {
        SafeBlockPolicy policy = SafeBlockPolicy.from(Arrays.asList("minecraft:grass_path", "minecraft:dirt_path"), Collections.emptyList(), Collections.emptyList());
        assertEquals(Collections.singletonList(Blocks.DIRT_PATH.defaultBlockState()), policy.supportStates());
    }

    @Test
    public void legacyUnsafePathDoesNotMarkAirUnsafe() {
        SafeBlockPolicy policy = SafeBlockPolicy.from(Collections.emptyList(), Collections.singletonList("minecraft:grass_path"), Collections.emptyList());
        assertTrue(policy.unsafe(Blocks.DIRT_PATH.defaultBlockState()));
        assertFalse(policy.unsafe(Blocks.AIR.defaultBlockState()));
    }

    @Test
    public void legacySuffocatingPathDoesNotMarkAirSuffocating() {
        SafeBlockPolicy policy = SafeBlockPolicy.from(Collections.emptyList(), Collections.emptyList(), Collections.singletonList("minecraft:grass_path"));
        assertTrue(policy.suffocating(Blocks.DIRT_PATH.defaultBlockState()));
        assertFalse(policy.suffocating(Blocks.AIR.defaultBlockState()));
    }
}
