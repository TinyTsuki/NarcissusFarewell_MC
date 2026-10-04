package xin.vanilla.narcissus.search;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.junit.Test;

import java.util.function.Predicate;

import static org.junit.Assert.*;
import static xin.vanilla.narcissus.search.ViewSearchCursor.Step.*;

public class ViewSearchCursorTest {
    @Test
    public void originalFloatingDestinationsSurviveDirectionsCollisionsAndSlices() {
        double[][] rays = {{.2, 64.5, .2, .75, 0, 0}, {-.2, 64.5, -.2, -.75, 0, 0},
                {.2, 64.5, -.2, 0, 0, -.75}, {-.2, 64.5, .2, 0, .75, 0},
                {.2, 64.5, .2, 0, -.75, 0}, {-.2, 64.5, -.2, .45, .45, .3968626966596886}};
        Predicate<BlockPos> safety = p -> p.getY() % 2 == 0 && p.getX() % 3 == 0 && p.getZ() % 2 == 0;
        for (double[] ray : rays) {
            for (int collision : new int[]{-1, 0, 5}) {
                BlockPos obstacle = at(ray, collision < 0 ? 100 : collision, 0);
                Predicate<BlockPos> motion = p -> collision >= 0 && p.equals(obstacle);
                for (boolean safe : new boolean[]{false, true}) {
                    for (int slice : new int[]{1, 7, 64}) {
                        assertArrayEquals(legacy(ray, 17, safe, motion, safety),
                                run(ray, 17, safe, motion, safety, slice).destination, 0);
                    }
                }
            }
        }
    }

    @Test
    public void collisionUsesThePrecedingFloatingSampleNotTheBlockCenter() {
        double[] ray = {.2, 64.5, .2, .75, 0, 0};
        assertArrayEquals(new double[]{2.45, 64.5, .2},
                run(ray, 10, false, p -> p.getX() == 3, p -> false, 64).destination, 0);
        assertArrayEquals(new double[]{.2, 64.5, .2},
                run(ray, 1, false, p -> true, p -> false, 64).destination, 0);
    }

    @Test
    public void safeFallbackIsTheRawEndpointAndOffsetOrderIsUnchanged() {
        double[] ray = {.2, 10.5, .2, .75, 0, 0};
        assertArrayEquals(new double[]{7.7, 10.5, .2},
                run(ray, 10, true, p -> false, p -> false, 64).destination, 0);
        assertArrayEquals(new double[]{7.5, 9.15, .5},
                run(ray, 10, true, p -> false, p -> p.getY() == 9 || p.getY() == 11, 64).destination, 0);
    }

    @Test
    public void consecutiveMotionAndSafetyDuplicatesReduceReadsWithinOneSlice() {
        double[] ray = {.1, 64.5, .1, .75, 0, 0};
        Run result = run(ray, 16, true, p -> false, p -> false, 4096);
        assertEquals(13, result.motionReads);
        assertEquals(78, result.safetyReads);
        assertArrayEquals(legacy(ray, 16, true, p -> false, p -> false), result.destination, 0);
    }

    @Test
    public void aNewSliceRechecksARepeatedCellInsteadOfReusingOldWorldState() {
        ViewSearchCursor cursor = cursor(new double[]{.1, 64.5, .1, .75, 0, 0}, 16, false);
        assertEquals(MOTION, cursor.advance());
        cursor.accept(false);
        cursor.beginSlice();
        assertEquals(MOTION, cursor.advance());
        assertEquals(0, cursor.x());
        cursor.accept(true);
        assertEquals(DONE, cursor.advance());
        assertEquals(.1, cursor.resultX(), 0);
    }

    @Test
    public void pendingReadSurvivesSliceChangeAndCannotBeAdvancedTwice() {
        ViewSearchCursor cursor = cursor(new double[]{-.1, 2.5, -.1, -.75, 0, 0}, 1, false);
        assertEquals(MOTION, cursor.advance());
        cursor.beginSlice();
        assertEquals(-1, cursor.x());
        assertEquals(-1, cursor.z());
        assertThrows(IllegalStateException.class, cursor::advance);
        cursor.accept(false);
        assertThrows(IllegalStateException.class, () -> cursor.accept(false));
        assertThrows(IllegalStateException.class, cursor::resultX);
    }

    @Test
    public void reverseSearchKeepsFloatingCeilEvenWhenItExceedsTheEndpointIndex() {
        double[] ray = {0, 10, 0, .7500000000000001, 0, 0};
        assertArrayEquals(new double[]{2.5, 10.15, .5},
                run(ray, 2, true, p -> false, p -> true, 64).destination, 0);
    }

    @Test
    public void aNewSafetySliceCanFindAPreviouslyRejectedRepeatedPosition() {
        ViewSearchCursor cursor = cursor(new double[]{.1, 64.5, .1, .75, 0, 0}, 16, true);
        boolean changed = false;
        for (int i = 0; i < 1000; i++) {
            ViewSearchCursor.Step step = cursor.advance();
            if (step == DONE) {
                assertTrue(changed);
                assertArrayEquals(new double[]{9.5, 64.15, .5},
                        new double[]{cursor.resultX(), cursor.resultY(), cursor.resultZ()}, 0);
                return;
            }
            if (step == MOTION) cursor.accept(false);
            if (step == SAFETY) {
                boolean target = cursor.x() == 9 && cursor.y() == 64;
                cursor.accept(changed && target);
                if (target && !changed) {
                    cursor.beginSlice();
                    changed = true;
                }
            }
        }
        fail("View search did not finish");
    }

    @Test
    public void shortUnobstructedRayKeepsTheEndpointAndExhaustionIsStable() {
        double[] ray = {-.1, 4.5, -.1, -.75, 0, 0};
        assertArrayEquals(new double[]{-.85, 4.5, -.1},
                run(ray, 1, false, p -> false, p -> false, 1).destination, 0);
        ViewSearchCursor cursor = cursor(ray, 0, false);
        assertEquals(MOTION, cursor.advance());
        cursor.accept(false);
        assertEquals(DONE, cursor.advance());
        assertEquals(DONE, cursor.advance());
        assertThrows(IllegalStateException.class, cursor::x);
    }

    private static Run run(double[] ray, int range, boolean safe, Predicate<BlockPos> motion,
                           Predicate<BlockPos> safety, int slice) {
        ViewSearchCursor cursor = cursor(ray, range, safe);
        Run result = new Run();
        for (int advances = 0; advances < 10000; advances++) {
            if (advances % slice == 0) cursor.beginSlice();
            ViewSearchCursor.Step step = cursor.advance();
            if (step == DONE) {
                result.destination = new double[]{cursor.resultX(), cursor.resultY(), cursor.resultZ()};
                return result;
            }
            if (step == MOTION || step == SAFETY) {
                BlockPos point = new BlockPos(cursor.x(), cursor.y(), cursor.z());
                if (step == MOTION) {
                    result.motionReads++;
                    cursor.accept(motion.test(point));
                } else {
                    result.safetyReads++;
                    cursor.accept(safety.test(point));
                }
            }
        }
        throw new AssertionError("View cursor exceeded fixture step limit");
    }

    private static ViewSearchCursor cursor(double[] ray, int range, boolean safe) {
        return new ViewSearchCursor(ray[0], ray[1], ray[2], ray[3], ray[4], ray[5], range, safe);
    }

    private static BlockPos at(double[] ray, int index, int offset) {
        return new BlockPos(MathHelper.floor(ray[0] + ray[3] * index),
                MathHelper.floor(ray[1] + ray[4] * index) + offset, MathHelper.floor(ray[2] + ray[5] * index));
    }

    // Direct original sampling loops; independent of the resumable implementation.
    private static double[] legacy(double[] ray, int range, boolean safe, Predicate<BlockPos> motion,
                                   Predicate<BlockPos> safety) {
        int collision = -1;
        for (int step = 0; step <= range; step++) {
            if (motion.test(at(ray, step, 0))) {
                collision = step;
                break;
            }
        }
        int last = collision > 0 ? collision - 1 : collision == 0 ? 0 : range;
        double[] result = {ray[0] + ray[3] * last, ray[1] + ray[4] * last, ray[2] + ray[5] * last};
        if (safe) {
            double dist = Math.sqrt(Math.pow(result[0] - ray[0], 2) + Math.pow(result[1] - ray[1], 2)
                    + Math.pow(result[2] - ray[2], 2));
            for (int step = (int) Math.ceil(dist / .75); step >= 0; step--) {
                for (int offset : new int[]{0, -1, 1, -2, 2, -3}) {
                    BlockPos point = at(ray, step, offset);
                    if (safety.test(point)) return new double[]{point.getX() + .5, point.getY() + .15, point.getZ() + .5};
                }
            }
        }
        return result;
    }

    private static final class Run {
        private int motionReads;
        private int safetyReads;
        private double[] destination;
    }
}
