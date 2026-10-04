package xin.vanilla.narcissus.search;

import xin.vanilla.banira.common.data.Component;
import xin.vanilla.banira.common.enums.IEnumDescribable;
import xin.vanilla.narcissus.NarcissusComponent;
import xin.vanilla.narcissus.enums.EnumSafeMode;

import java.util.Objects;

public final class SafeCandidateCursor {
    public enum Step implements IEnumDescribable {
        CANDIDATE, SKIPPED, DONE;

        @Override
        public Component enumDescription() {
            return NarcissusComponent.get().literal(name());
        }
    }

    private static final int[] Y_OFFSETS = {0, -1, 1, -2, 2, -3};

    private final EnumSafeMode mode;
    private final SearchBox box;
    private final int cx;
    private final int cy;
    private final int cz;
    private final long minDx;
    private final long maxDx;
    private final long minDy;
    private final long maxDy;
    private final long maxManhattan;
    private long shell;
    private long dx;
    private long dxEnd;
    private long dy;
    private long dyEnd;
    private int sign;
    private long columnY;
    private long columnEnd;
    private int columnDirection;
    private int offsetIndex;
    private int x;
    private int y;
    private int z;
    private Step last = Step.SKIPPED;

    public SafeCandidateCursor(EnumSafeMode mode, int cx, int cy, int cz, SearchBox box) {
        this.mode = Objects.requireNonNull(mode, "mode");
        this.box = Objects.requireNonNull(box, "box");
        this.cx = cx;
        this.cy = cy;
        this.cz = cz;
        minDx = (long) box.minX - cx;
        maxDx = (long) box.maxX - cx;
        minDy = (long) box.minY - cy;
        maxDy = (long) box.maxY - cy;
        maxManhattan = Math.max(Math.abs(minDx), Math.abs(maxDx))
                + Math.max(Math.abs(minDy), Math.abs(maxDy))
                + Math.max(Math.abs((long) box.minZ - cz), Math.abs((long) box.maxZ - cz));
        if (mode == EnumSafeMode.NONE) {
            startShell();
        } else if (mode == EnumSafeMode.Y_C_TO_T) {
            startColumn(cy, box.maxY, 1);
        } else if (mode == EnumSafeMode.Y_B_TO_C) {
            startColumn(box.minY, cy, 1);
        } else if (mode == EnumSafeMode.Y_C_TO_B) {
            startColumn(cy, box.minY, -1);
        } else if (mode == EnumSafeMode.Y_T_TO_C) {
            startColumn(box.maxY, cy, -1);
        }
    }

    public Step advance() {
        if (last == Step.DONE) return last;
        if (mode == EnumSafeMode.Y_C_OFFSET_3) {
            if (offsetIndex == Y_OFFSETS.length) return last = Step.DONE;
            long nextY = (long) cy + Y_OFFSETS[offsetIndex++];
            if (nextY < box.minY || nextY > box.maxY) return last = Step.SKIPPED;
            return emit(cx, (int) nextY, cz);
        }
        if (mode != EnumSafeMode.NONE) {
            if (columnDirection > 0 ? columnY > columnEnd : columnY < columnEnd) {
                return last = Step.DONE;
            }
            int nextY = (int) columnY;
            columnY += columnDirection;
            return emit(cx, nextY, cz);
        }
        if (shell > maxManhattan) return last = Step.DONE;
        if (dx > dxEnd) {
            shell++;
            startShell();
            return last = Step.SKIPPED;
        }
        if (dy > dyEnd) {
            dx++;
            startRow();
            return last = Step.SKIPPED;
        }
        long dzAbs = shell - Math.abs(dx) - Math.abs(dy);
        long nextZ = (long) cz + (sign == 0 ? dzAbs : -dzAbs);
        int nextX = (int) (cx + dx);
        int nextY = (int) (cy + dy);
        if (dzAbs == 0 || sign == 1) {
            dy++;
            sign = 0;
        } else {
            sign = 1;
        }
        if (nextZ < box.minZ || nextZ > box.maxZ) return last = Step.SKIPPED;
        return emit(nextX, nextY, (int) nextZ);
    }

    private void startShell() {
        dx = Math.max(-shell, minDx);
        dxEnd = Math.min(shell, maxDx);
        startRow();
    }

    private void startRow() {
        long rest = shell - Math.abs(dx);
        dy = Math.max(-rest, minDy);
        dyEnd = Math.min(rest, maxDy);
        sign = 0;
    }

    private void startColumn(long start, long end, int direction) {
        columnY = start;
        columnEnd = end;
        columnDirection = direction;
    }

    private Step emit(int nextX, int nextY, int nextZ) {
        x = nextX;
        y = nextY;
        z = nextZ;
        return last = Step.CANDIDATE;
    }

    public int x() {
        requireCandidate();
        return x;
    }

    public int y() {
        requireCandidate();
        return y;
    }

    public int z() {
        requireCandidate();
        return z;
    }

    private void requireCandidate() {
        if (last != Step.CANDIDATE) throw new IllegalStateException("No candidate");
    }
}
