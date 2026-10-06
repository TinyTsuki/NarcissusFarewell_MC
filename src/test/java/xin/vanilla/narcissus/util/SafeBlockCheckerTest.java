package xin.vanilla.narcissus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.narcissus.search.SafeBlockPolicy;

import java.util.*;

import static org.junit.Assert.*;

public class SafeBlockCheckerTest {
    private static final BlockPos FOOT = new BlockPos(4, 64, -3);

    @BeforeClass
    public static void bootstrap() {
        try {
            xin.vanilla.narcissus.test.ForgeUnitTestBootstrap.bootstrap();
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
        for (net.minecraft.world.level.block.Block block : Arrays.asList(Blocks.STONE, Blocks.DIRT, Blocks.WATER, Blocks.LAVA, Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR))
            block.getStateDefinition().getPossibleStates().forEach(net.minecraft.world.level.block.state.BlockState::initCache);
    }

    @Test
    public void blockedFeetDoNotReadHeadOrSupport() {
        Access access = new Access();
        access.blocks.put(FOOT, Blocks.STONE.defaultBlockState());
        assertFalse(new SafeBlockChecker(access, policy()).isSafeBlock(FOOT, false));
        assertEquals(0, access.readsAt(FOOT.above()));
        assertEquals(0, access.readsAt(FOOT.below()));
    }

    @Test
    public void blockedOrSuffocatingHeadDoesNotReadSupport() {
        Access access = new Access();
        access.blocks.put(FOOT.above(), Blocks.STONE.defaultBlockState());
        SafeBlockChecker checker = new SafeBlockChecker(access, policy());
        assertFalse(checker.isSafeBlock(FOOT, false));
        assertEquals(0, access.readsAt(FOOT.below()));
        access.blocks.put(FOOT.above(), Blocks.AIR.defaultBlockState());
        access.suffocating.add(FOOT.above());
        checker.beginSlice();
        assertFalse(checker.isSafeBlock(FOOT, false));
        assertEquals(0, access.readsAt(FOOT.below()));
    }

    @Test
    public void liquidSupportAndOnlyAirOrCaveAirRetainLegacyRules() {
        Access access = new Access();
        SafeBlockChecker checker = new SafeBlockChecker(access, policy());
        access.blocks.put(FOOT.below(), Blocks.WATER.defaultBlockState());
        assertTrue(checker.isSafeBlock(FOOT, false));
        access.blocks.put(FOOT.below(), Blocks.LAVA.defaultBlockState());
        checker.beginSlice();
        assertFalse(checker.isSafeBlock(FOOT, false));
        for (BlockState below : Arrays.asList(Blocks.AIR.defaultBlockState(), Blocks.CAVE_AIR.defaultBlockState())) {
            access.blocks.put(FOOT.below(), below);
            checker.beginSlice();
            assertFalse(checker.isSafeBlock(FOOT, false));
            assertTrue(checker.isSafeBlock(FOOT, true));
        }
        access.blocks.put(FOOT.below(), Blocks.VOID_AIR.defaultBlockState());
        checker.beginSlice();
        assertFalse(checker.isSafeBlock(FOOT, true));
    }

    @Test
    public void waterCanOccupyFeetButConfiguredSuffocatingHeadIsRejected() {
        Access access = new Access();
        access.blocks.put(FOOT.below(), Blocks.STONE.defaultBlockState());
        access.blocks.put(FOOT, Blocks.WATER.defaultBlockState());
        SafeBlockChecker checker = new SafeBlockChecker(access, policy());
        assertTrue(checker.isSafeBlock(FOOT, false));
        access.blocks.put(FOOT.above(), Blocks.WATER.defaultBlockState());
        checker.beginSlice();
        assertFalse(checker.isSafeBlock(FOOT, false));
    }

    @Test
    public void stateEntriesKeepBroadBlockMatchingAndSupportOrderIsImmutable() {
        List<String> supports = new ArrayList<>(Arrays.asList("minecraft:dirt", "minecraft:cobblestone", "minecraft:dirt"));
        SafeBlockPolicy policy = SafeBlockPolicy.from(supports, Collections.singletonList("minecraft:campfire[lit=false]"),
                Collections.emptyList());
        supports.clear();
        assertEquals(Arrays.asList(Blocks.DIRT.defaultBlockState(), Blocks.COBBLESTONE.defaultBlockState()), policy.supportStates());
        assertThrows(UnsupportedOperationException.class, () -> policy.supportStates().clear());
        Access access = new Access();
        access.blocks.put(FOOT.below(), Blocks.CAMPFIRE.defaultBlockState());
        assertFalse(new SafeBlockChecker(access, policy).isSafeBlock(FOOT, false));
    }

    @Test
    public void cachedStatesAreReusedOnlyUntilTheNextSlice() {
        Access access = new Access();
        access.blocks.put(FOOT.below(), Blocks.STONE.defaultBlockState());
        SafeBlockChecker checker = new SafeBlockChecker(access, policy());
        assertTrue(checker.isSafeBlock(FOOT, false));
        assertTrue(checker.isSafeBlock(FOOT, false));
        assertEquals(1, access.readsAt(FOOT));
        access.blocks.put(FOOT, Blocks.STONE.defaultBlockState());
        checker.beginSlice();
        assertFalse(checker.isSafeBlock(FOOT, false));
        assertEquals(2, access.readsAt(FOOT));
    }

    @Test
    public void mutableInputDoesNotCorruptTheStateCacheKeys() {
        Access access = new Access();
        access.blocks.put(FOOT.below(), Blocks.STONE.defaultBlockState());
        SafeBlockChecker checker = new SafeBlockChecker(access, policy());
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos().set(FOOT);
        assertTrue(checker.isSafeBlock(mutable, false));
        mutable.set(FOOT.offset(10, 0, 0));
        assertFalse(checker.isSafeBlock(mutable, false));
        mutable.set(FOOT);
        assertTrue(checker.isSafeBlock(mutable, false));
        assertEquals(1, access.readsAt(FOOT));
        assertFalse(checker.isSafeBlock(FOOT.offset(10, 0, 0), false));
        assertEquals(1, access.readsAt(FOOT.offset(10, 0, 0)));
    }

    private static SafeBlockPolicy policy() {
        return SafeBlockPolicy.from(Collections.emptyList(), Collections.singletonList("minecraft:lava"),
                Collections.singletonList("minecraft:water"));
    }

    private static final class Access implements SafeBlockChecker.BlockAccess {
        private final Map<BlockPos, BlockState> blocks = new HashMap<>();
        private final Map<BlockPos, Integer> reads = new HashMap<>();
        private final Set<BlockPos> suffocating = new HashSet<>();

        @Override
        public BlockState state(BlockPos pos) {
            reads.merge(pos.immutable(), 1, Integer::sum);
            return blocks.getOrDefault(pos, Blocks.AIR.defaultBlockState());
        }

        @Override
        public boolean suffocates(BlockState state, BlockPos pos) {
            return suffocating.contains(pos);
        }

        private int readsAt(BlockPos pos) {
            return reads.getOrDefault(pos, 0);
        }
    }
}
