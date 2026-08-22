package xin.vanilla.narcissus.screen;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WaypointDragModelTest {
    @Test
    public void keepsInsertionAndAnimatedSlotsConsistent() {
        assertEquals(0, WaypointDragModel.insertionIndex(4, 1, 0, 28));
        assertEquals(1, WaypointDragModel.insertionIndex(4, 1, 42, 28));
        assertEquals(2, WaypointDragModel.insertionIndex(4, 1, 72, 28));
        assertEquals(0, WaypointDragModel.targetSlot(0, 1, 2));
        assertEquals(1, WaypointDragModel.targetSlot(2, 1, 2));
        assertEquals(3, WaypointDragModel.targetSlot(3, 1, 2));
    }

    @Test
    public void edgeScrollAcceleratesTowardViewportBoundary() {
        assertEquals(0.0D, WaypointDragModel.autoScrollSpeed(100, 20, 180, 40, 160), 0.001D);
        double nearTop = WaypointDragModel.autoScrollSpeed(50, 20, 180, 40, 160);
        double pastTop = WaypointDragModel.autoScrollSpeed(10, 20, 180, 40, 160);
        assertTrue(nearTop < 0.0D);
        assertTrue(pastTop < nearTop);
        assertEquals(-160.0D, pastTop, 0.001D);
    }
}
