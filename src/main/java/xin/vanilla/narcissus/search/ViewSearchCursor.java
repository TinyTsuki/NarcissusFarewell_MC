package xin.vanilla.narcissus.search;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.narcissus.NarcissusComponent;

import java.util.HashSet;
import java.util.Set;

public final class ViewSearchCursor {
    public enum Step implements IEnumDescribable {
        MOTION, SAFETY, SKIPPED, DONE;

        @Override
        public Component enumDescription() {
            return NarcissusComponent.get().literal(name());
        }
    }

    private static final int[] Y_OFFSETS = {0, -1, 1, -2, 2, -3};
    private final double eyeX;
    private final double eyeY;
    private final double eyeZ;
    private final double stepX;
    private final double stepY;
    private final double stepZ;
    private final int range;
    private final boolean safe;
    private final Set<BlockPos> unsafeCandidates = new HashSet<>();
    private long motionIndex;
    private int reverseIndex;
    private int offsetIndex;
    private boolean reversing;
    private boolean finished;
    private boolean previousMotion;
    private int previousX;
    private int previousY;
    private int previousZ;
    private int x;
    private int y;
    private int z;
    private double resultX;
    private double resultY;
    private double resultZ;
    private Step pending;
    private Step last = Step.SKIPPED;

    public ViewSearchCursor(double eyeX, double eyeY, double eyeZ, double stepX, double stepY, double stepZ,
                            int range, boolean safe) {
        if (range < 0 || !Double.isFinite(eyeX) || !Double.isFinite(eyeY) || !Double.isFinite(eyeZ)
                || !Double.isFinite(stepX) || !Double.isFinite(stepY) || !Double.isFinite(stepZ)) {
            throw new IllegalArgumentException("Invalid view search geometry");
        }
        this.eyeX = eyeX;
        this.eyeY = eyeY;
        this.eyeZ = eyeZ;
        this.stepX = stepX;
        this.stepY = stepY;
        this.stepZ = stepZ;
        this.range = range;
        this.safe = safe;
    }

    public Step advance() {
        if (pending != null) throw new IllegalStateException("Current read has not been accepted");
        if (finished) return last = Step.DONE;
        if (!reversing) {
            if (motionIndex > range) {
                endpoint(range);
                return last = finished ? Step.DONE : Step.SKIPPED;
            }
            position(motionIndex, 0);
            if (previousMotion && x == previousX && y == previousY && z == previousZ) {
                motionIndex++;
                return last = Step.SKIPPED;
            }
            return last = pending = Step.MOTION;
        }
        if (reverseIndex < 0) {
            finished = true;
            return last = Step.DONE;
        }
        position(reverseIndex, Y_OFFSETS[offsetIndex]);
        if (unsafeCandidates.contains(new BlockPos(x, y, z))) {
            nextSafety();
            return last = Step.SKIPPED;
        }
        return last = pending = Step.SAFETY;
    }

    public void accept(boolean matched) {
        if (pending == null) throw new IllegalStateException("No pending world read");
        if (pending == Step.MOTION) {
            if (matched) {
                endpoint(motionIndex == 0 ? 0 : motionIndex - 1);
            } else {
                previousMotion = true;
                previousX = x;
                previousY = y;
                previousZ = z;
                motionIndex++;
            }
        } else if (matched) {
            resultX = x + .5;
            resultY = y + .15;
            resultZ = z + .5;
            finished = true;
        } else {
            unsafeCandidates.add(new BlockPos(x, y, z));
            nextSafety();
        }
        pending = null;
    }

    private void endpoint(long index) {
        resultX = index == 0 ? eyeX : eyeX + stepX * index;
        resultY = index == 0 ? eyeY : eyeY + stepY * index;
        resultZ = index == 0 ? eyeZ : eyeZ + stepZ * index;
        if (safe) {
            double dist = Math.sqrt(Math.pow(resultX - eyeX, 2) + Math.pow(resultY - eyeY, 2)
                    + Math.pow(resultZ - eyeZ, 2));
            reverseIndex = (int) Math.ceil(dist / .75);
            reversing = true;
        } else {
            finished = true;
        }
    }

    private void position(long index, int yOffset) {
        x = MathHelper.floor(eyeX + stepX * index);
        y = MathHelper.floor(eyeY + stepY * index) + yOffset;
        z = MathHelper.floor(eyeZ + stepZ * index);
    }

    private void nextSafety() {
        if (++offsetIndex == Y_OFFSETS.length) {
            offsetIndex = 0;
            reverseIndex--;
        }
    }

    public void beginSlice() {
        previousMotion = false;
        unsafeCandidates.clear();
    }

    public int x() {
        requirePending();
        return x;
    }

    public int y() {
        requirePending();
        return y;
    }

    public int z() {
        requirePending();
        return z;
    }

    public double resultX() {
        requireDone();
        return resultX;
    }

    public double resultY() {
        requireDone();
        return resultY;
    }

    public double resultZ() {
        requireDone();
        return resultZ;
    }

    private void requirePending() {
        if (pending == null) throw new IllegalStateException("No pending world read");
    }

    private void requireDone() {
        if (last != Step.DONE) throw new IllegalStateException("Search has not finished");
    }
}
