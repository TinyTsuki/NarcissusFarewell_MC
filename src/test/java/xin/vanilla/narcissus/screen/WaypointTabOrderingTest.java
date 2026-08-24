package xin.vanilla.narcissus.screen;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WaypointTabOrderingTest {

    @Test
    public void onlyEditableWaypointTabsSupportOrdering() {
        assertTrue(WaypointScreen.WaypointListTab.PRIVATE.supportsOrdering());
        assertTrue(WaypointScreen.WaypointListTab.PUBLIC.supportsOrdering());
        assertFalse(WaypointScreen.WaypointListTab.FOOTPRINTS.supportsOrdering());
    }
}
