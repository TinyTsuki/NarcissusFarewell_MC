package xin.vanilla.narcissus.internal.server.dev;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class NarcissusViewFixtureTest {
    @BeforeClass
    public static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    public void clearsRandomTerrainInsideTheCollisionRing() {
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        blocks.put(new BlockPos(0, 71, 0), Blocks.GRASS_BLOCK.defaultBlockState());
        blocks.put(new BlockPos(1, 71, 0), Blocks.DIRT.defaultBlockState());
        NarcissusNetworkSmokeServerRunner.prepareViewCollisionRing(blocks::put);
        assertEquals(17 * 17 * 11, blocks.size());
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                for (int y = 65; y <= 75; y++) {
                    BlockState actual = blocks.get(new BlockPos(x, y, z));
                    assertNotNull(actual);
                    assertSame(Math.abs(x) == 8 || Math.abs(z) == 8 ? Blocks.STONE : Blocks.AIR, actual.getBlock());
                }
            }
        }
        assertFalse(blocks.containsKey(new BlockPos(0, 64, 0)));
        assertFalse(blocks.containsKey(new BlockPos(9, 71, 0)));
    }
}
