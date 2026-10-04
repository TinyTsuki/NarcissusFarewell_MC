package xin.vanilla.narcissus.internal.fabric.search;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.junit.BeforeClass;
import org.junit.Test;
import xin.vanilla.narcissus.search.SafeBlockPolicy;
import xin.vanilla.narcissus.search.SafeCandidateCursor;
import xin.vanilla.narcissus.util.SafeBlockChecker;

import java.util.*;
import static org.junit.Assert.*;

public class SectionSearchPrunerTest {
    @BeforeClass public static void bootstrap() { net.minecraft.SharedConstants.tryDetectVersion(); net.minecraft.server.Bootstrap.bootStrap(); }

    @Test public void emptyAirCannotSupportNormalSearchButAllowsPlacementSearch() {
        Terrain terrain = new Terrain();
        SectionSearchPruner pruner = new SectionSearchPruner(terrain, policy());
        assertEquals(Long.MAX_VALUE, pruner.filter(0, 15, false).next(0, 0, 255));
        assertEquals(90, pruner.filter(0, 15, true).next(0, 90, 255));
    }

    @Test public void floorAtSectionBoundaryAndLiquidSupportAreNotExcluded() {
        Terrain terrain = new Terrain();
        terrain.put(4, 63, 4, Blocks.STONE.defaultBlockState());
        terrain.put(4, 31, 4, Blocks.WATER.defaultBlockState());
        SectionSearchPruner pruner = new SectionSearchPruner(terrain, policy());
        SafeCandidateCursor.YFilter filter = pruner.filter(0, 15, false);
        assertEquals(64, filter.next(0, 64, 64));
        assertEquals(32, filter.next(0, 32, 32));
        assertEquals(Long.MAX_VALUE, filter.next(0, 65, 79));
    }

    @Test public void missingZChunkKeepsAllHeightsPossibleWithoutLoading() {
        Terrain terrain = new Terrain();
        terrain.missing.add("0,-1");
        SectionSearchPruner pruner = new SectionSearchPruner(terrain, policy());
        assertEquals(90, pruner.filter(-16, 15, false).next(0, 90, 90));
        assertEquals(0, terrain.sectionReads);
    }

    @Test public void sliceRefreshSeesAddedAndRemovedSupport() {
        Terrain terrain = new Terrain();
        SectionSearchPruner pruner = new SectionSearchPruner(terrain, policy());
        SafeCandidateCursor.YFilter filter = pruner.filter(0, 15, false);
        assertEquals(Long.MAX_VALUE, filter.next(0, 80, 80));
        terrain.put(2, 79, 2, Blocks.STONE.defaultBlockState());
        pruner.beginSlice();
        assertEquals(80, filter.next(0, 80, 80));
        terrain.put(2, 79, 2, Blocks.AIR.defaultBlockState());
        pruner.beginSlice();
        assertEquals(Long.MAX_VALUE, filter.next(0, 80, 80));
    }

    @Test public void noActuallySafeCandidateIsPrunedAcrossMixedSections() {
        Terrain terrain = new Terrain();
        terrain.put(-3, -1, -3, Blocks.STONE.defaultBlockState());
        terrain.put(-2, 15, -3, Blocks.WATER.defaultBlockState());
        terrain.put(-1, 32, -3, Blocks.DIRT.defaultBlockState());
        terrain.put(-3, 47, -3, Blocks.LAVA.defaultBlockState());
        SafeBlockPolicy policy = SafeBlockPolicy.from(Collections.emptyList(),
                Collections.singletonList("minecraft:lava"), Collections.emptyList());
        SectionSearchPruner pruner = new SectionSearchPruner(terrain, policy);
        SafeBlockChecker checker = new SafeBlockChecker(new SafeBlockChecker.BlockAccess() {
            public BlockState state(BlockPos p) { return terrain.state(p); }
            public boolean suffocates(BlockState state, BlockPos p) { return false; }
        }, policy);
        for (boolean air : new boolean[]{false, true}) {
            SafeCandidateCursor.YFilter filter = pruner.filter(-16, -1, air);
            for (int x = -4; x < 0; x++) for (int z = -4; z < 0; z++) for (int y = -3; y < 65; y++) {
                if (checker.isSafeBlock(new BlockPos(x, y, z), air))
                    assertEquals("safe candidate " + x + "," + y + "," + z, y, filter.next(x >> 4, y, y));
            }
        }
    }

    @Test public void globalOrUnknownPaletteDoesNotClaimAbsence() {
        Terrain terrain = new Terrain();
        terrain.sections.put("0,0,5", new LevelChunkSection(5, net.minecraft.data.BuiltinRegistries.BIOME) {
            @Override public boolean maybeHas(java.util.function.Predicate<BlockState> predicate) { return true; }
        });
        terrain.sections.get("0,0,5").setBlockState(0, 0, 0, Blocks.STONE.defaultBlockState());
        SectionSearchPruner pruner = new SectionSearchPruner(terrain, policy());
        assertEquals(90, pruner.filter(0, 15, false).next(0, 90, 90));
    }

    @Test public void supportInAnotherZChunkPreventsWholeRowExclusion() {
        Terrain terrain = new Terrain();
        terrain.put(-1, 79, -1, Blocks.STONE.defaultBlockState());
        SectionSearchPruner pruner = new SectionSearchPruner(terrain, policy());
        assertEquals(80, pruner.filter(-16, 15, false).next(-1, 80, 80));
        assertEquals(Long.MAX_VALUE, pruner.filter(0, 15, false).next(-1, 80, 80));
    }

    private static SafeBlockPolicy policy() {
        return SafeBlockPolicy.from(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }
    private static final class Terrain implements SectionSearchPruner.ChunkAccess {
        final Map<String, LevelChunkSection> sections = new HashMap<>();
        final Set<String> missing = new HashSet<>();
        int sectionReads;
        public boolean ready(int x, int z) { return !missing.contains(x + "," + z); }
        public LevelChunkSection section(int x, int z, int y) { sectionReads++; return sections.get(x + "," + z + "," + y); }
        void put(int x, int y, int z, BlockState state) {
            sections.computeIfAbsent((x >> 4) + "," + (z >> 4) + "," + (y >> 4), k -> new LevelChunkSection(y >> 4, net.minecraft.data.BuiltinRegistries.BIOME))
                    .setBlockState(x & 15, y & 15, z & 15, state);
        }
        BlockState state(BlockPos p) {
            LevelChunkSection section = section(p.getX() >> 4, p.getZ() >> 4, p.getY() >> 4);
            return section == null ? Blocks.AIR.defaultBlockState() : section.getBlockState(p.getX() & 15, p.getY() & 15, p.getZ() & 15);
        }
    }
}
