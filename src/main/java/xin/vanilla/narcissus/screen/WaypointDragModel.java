package xin.vanilla.narcissus.screen;

/** 地标拖动的插入槽位与边缘滚动计算，保持视觉位置和最终顺序一致。 */
final class WaypointDragModel {
    private WaypointDragModel() {
    }

    static int insertionIndex(int itemCount, int sourceIndex, double contentY, int itemHeight) {
        if (itemCount <= 1 || sourceIndex < 0 || sourceIndex >= itemCount || itemHeight <= 0) {
            return 0;
        }
        int beforeRemoval = (int) Math.floor((Math.max(0.0D, contentY) + itemHeight / 2.0D) / itemHeight);
        beforeRemoval = Math.max(0, Math.min(itemCount, beforeRemoval));
        int afterRemoval = beforeRemoval > sourceIndex ? beforeRemoval - 1 : beforeRemoval;
        return Math.max(0, Math.min(itemCount - 1, afterRemoval));
    }

    static int targetSlot(int itemIndex, int sourceIndex, int insertionIndex) {
        if (itemIndex == sourceIndex) {
            return -1;
        }
        int remainingIndex = itemIndex < sourceIndex ? itemIndex : itemIndex - 1;
        return remainingIndex >= insertionIndex ? remainingIndex + 1 : remainingIndex;
    }

    static double autoScrollSpeed(double pointerY, double top, double bottom,
                                  double edgeSize, double maxSpeed) {
        if (edgeSize <= 0.0D || maxSpeed <= 0.0D || bottom <= top) {
            return 0.0D;
        }
        double usableEdge = Math.min(edgeSize, (bottom - top) / 2.0D);
        if (pointerY < top + usableEdge) {
            double ratio = Math.min(1.0D, Math.max(0.0D, (top + usableEdge - pointerY) / usableEdge));
            return -maxSpeed * ratio * ratio;
        }
        if (pointerY > bottom - usableEdge) {
            double ratio = Math.min(1.0D, Math.max(0.0D, (pointerY - (bottom - usableEdge)) / usableEdge));
            return maxSpeed * ratio * ratio;
        }
        return 0.0D;
    }
}
