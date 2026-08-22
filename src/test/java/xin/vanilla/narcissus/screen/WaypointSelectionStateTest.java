package xin.vanilla.narcissus.screen;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class WaypointSelectionStateTest {
    @Test
    public void refreshKeepsLoadedEntryWhenPreviouslyViewingAnEmptyTab() {
        WaypointSelectionState.Key added = key("home");
        assertEquals(added, WaypointSelectionState.afterRefresh(
                null, added, Collections.singletonList(added), value -> value));
    }

    @Test
    public void refreshMatchesExistingSelectionButDoesNotSelectAfterDeletion() {
        WaypointSelectionState.Key oldStage = key("old");
        WaypointSelectionState.Key sameStage = key("old");
        WaypointSelectionState.Key anotherStage = key("another");
        assertEquals(sameStage, WaypointSelectionState.afterRefresh(
                oldStage, anotherStage, Arrays.asList(sameStage, anotherStage), value -> value));
        assertNull(WaypointSelectionState.afterRefresh(
                oldStage, anotherStage, Collections.singletonList(anotherStage), value -> value));
    }

    private static WaypointSelectionState.Key key(String name) {
        return new WaypointSelectionState.Key("HOME", name, "minecraft:overworld", null);
    }
}
