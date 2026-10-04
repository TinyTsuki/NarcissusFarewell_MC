package xin.vanilla.narcissus.search;

import net.minecraft.util.math.BlockPos;
import org.junit.Test;
import xin.vanilla.narcissus.enums.EnumSafeMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static xin.vanilla.narcissus.search.SafeCandidateCursor.Step.*;

public class SafeCandidateCursorTest {
    @Test
    public void fullHeightBoxKeepsEveryLegacyCandidateInOrderAcrossSlices() {
        SearchBox box = new SearchBox(0, 15, 0, 255, 0, 15);
        List<BlockPos> expected = LegacyCandidateOrder.enumerate(EnumSafeMode.NONE, 8, 64, 8, box);
        assertEquals(65536, expected.size());
        assertEquals(new BlockPos(8, 64, 8), expected.get(0));
        for (int slice : new int[]{1, 7, 64, 4096}) {
            assertEquals("slice=" + slice, expected, drain(EnumSafeMode.NONE, 8, 64, 8, box, slice));
        }
    }

    @Test
    public void negativeBoundaryAndOutsideCentersKeepAllModeOrders() {
        SearchBox box = new SearchBox(-3, 1, -2, 3, -4, 0);
        for (EnumSafeMode mode : EnumSafeMode.values()) {
            for (int[] center : new int[][]{{0, 0, -2}, {-3, -2, -4}, {1, 3, 0}, {2, 4, 1}, {-5, -8, -6}}) {
                List<BlockPos> expected = LegacyCandidateOrder.enumerate(mode, center[0], center[1], center[2], box);
                assertEquals(mode + " " + Arrays.toString(center), expected,
                        drain(mode, center[0], center[1], center[2], box, 7));
            }
        }
    }

    @Test
    public void offsetModeKeepsAsymmetricSixOffsetsAndFiltersHeightOnly() {
        SearchBox box = new SearchBox(0, 0, 0, 20, 0, 0);
        List<BlockPos> points = drain(EnumSafeMode.Y_C_OFFSET_3, 50, 10, -50, box, 1);
        List<Integer> ys = new ArrayList<>();
        for (BlockPos point : points) ys.add(point.getY());
        assertEquals(Arrays.asList(10, 9, 11, 8, 12, 7), ys);
        assertEquals(new BlockPos(50, 10, -50), points.get(0));
        assertEquals(Arrays.asList(new BlockPos(0, 0, 0), new BlockPos(0, 1, 0), new BlockPos(0, 2, 0)),
                drain(EnumSafeMode.Y_C_OFFSET_3, 0, 0, 0, box, 64));
    }

    @Test
    public void outsideShellsYieldBeforeReachingTheFirstCandidate() {
        SafeCandidateCursor cursor = new SafeCandidateCursor(EnumSafeMode.NONE, 100, 0, 0,
                new SearchBox(0, 15, 0, 1, 0, 1));
        for (int i = 0; i < 64; i++) assertEquals("step=" + i, SKIPPED, cursor.advance());
    }

    @Test
    public void centerCandidatePreservesPositiveThenNegativeZOrder() {
        List<BlockPos> points = drain(EnumSafeMode.NONE, 0, 0, 0,
                new SearchBox(-1, 1, -1, 1, -1, 1), 64);
        assertEquals(Arrays.asList(new BlockPos(0, 0, 0), new BlockPos(-1, 0, 0),
                new BlockPos(0, -1, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
                new BlockPos(0, 1, 0), new BlockPos(1, 0, 0)), points.subList(0, 7));
    }

    @Test
    public void integerHeightEndpointsTerminateWithoutWrapping() {
        SearchBox top = new SearchBox(0, 0, Integer.MAX_VALUE - 1, Integer.MAX_VALUE, 0, 0);
        assertEquals(Arrays.asList(new BlockPos(0, Integer.MAX_VALUE - 1, 0), new BlockPos(0, Integer.MAX_VALUE, 0)),
                drain(EnumSafeMode.Y_C_TO_T, 0, Integer.MAX_VALUE - 1, 0, top, 1));
        SearchBox bottom = new SearchBox(0, 0, Integer.MIN_VALUE, Integer.MIN_VALUE + 1, 0, 0);
        assertEquals(Arrays.asList(new BlockPos(0, Integer.MIN_VALUE + 1, 0), new BlockPos(0, Integer.MIN_VALUE, 0)),
                drain(EnumSafeMode.Y_C_TO_B, 0, Integer.MIN_VALUE + 1, 0, bottom, 1));
    }

    @Test
    public void largeDistanceArithmeticDoesNotExhaustAfterIntegerOverflow() {
        SafeCandidateCursor cursor = new SafeCandidateCursor(EnumSafeMode.NONE, Integer.MIN_VALUE, 0, 0,
                new SearchBox(Integer.MAX_VALUE - 1, Integer.MAX_VALUE, 0, 0, 0, 0));
        for (int i = 0; i < 64; i++) assertEquals(SKIPPED, cursor.advance());
    }

    @Test
    public void candidatesAtIntegerEdgesDoNotWrapOrDisappear() {
        int high = Integer.MAX_VALUE;
        int low = Integer.MIN_VALUE;
        SearchBox box = new SearchBox(high - 1, high, low, low + 1, low, low + 1);
        List<BlockPos> points = drain(EnumSafeMode.NONE, high, low, low, box, 7);
        assertEquals(Arrays.asList(new BlockPos(high, low, low), new BlockPos(high - 1, low, low),
                new BlockPos(high, low, low + 1), new BlockPos(high, low + 1, low),
                new BlockPos(high - 1, low, low + 1), new BlockPos(high - 1, low + 1, low),
                new BlockPos(high, low + 1, low + 1), new BlockPos(high - 1, low + 1, low + 1)), points);
    }

    @Test
    public void exhaustionIsStableAndCandidateAccessRequiresAnEmission() {
        SafeCandidateCursor cursor = new SafeCandidateCursor(EnumSafeMode.NONE, 0, 0, 0,
                new SearchBox(0, 0, 0, 0, 0, 0));
        assertThrows(IllegalStateException.class, cursor::x);
        assertEquals(CANDIDATE, cursor.advance());
        assertEquals(0, cursor.x());
        while (cursor.advance() != DONE) { }
        assertEquals(DONE, cursor.advance());
        assertThrows(IllegalStateException.class, cursor::y);
    }

    private static List<BlockPos> drain(EnumSafeMode mode, int cx, int cy, int cz, SearchBox box, int slice) {
        SafeCandidateCursor cursor = new SafeCandidateCursor(mode, cx, cy, cz, box);
        List<BlockPos> points = new ArrayList<>();
        int advances = 0;
        while (true) {
            for (int i = 0; i < slice; i++) {
                if (++advances > 2000000) fail("Cursor did not terminate within the fixture bound");
                SafeCandidateCursor.Step step = cursor.advance();
                if (step == DONE) {
                    if (points.size() == 65536) System.out.println("slice=" + slice + ", candidates=65536, advances=" + advances);
                    return points;
                }
                if (step == CANDIDATE) points.add(new BlockPos(cursor.x(), cursor.y(), cursor.z()));
            }
        }
    }
}
