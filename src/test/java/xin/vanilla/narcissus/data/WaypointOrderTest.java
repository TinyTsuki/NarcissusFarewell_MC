package xin.vanilla.narcissus.data;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class WaypointOrderTest {

    @Test
    public void prependsNewEntryAndValidatesCompleteReorder() {
        LinkedHashMap<String, Integer> source = new LinkedHashMap<>();
        source.put("first", 1);
        source.put("second", 2);

        LinkedHashMap<String, Integer> prepended = WaypointOrder.prepend("new", 3, source);
        assertEquals(Arrays.asList("new", "first", "second"), new ArrayList<>(prepended.keySet()));

        LinkedHashMap<String, Integer> reordered =
                WaypointOrder.validated(source, Arrays.asList("second", "first"));
        assertEquals(Arrays.asList("second", "first"), new ArrayList<>(reordered.keySet()));
        assertNull(WaypointOrder.validated(source, Arrays.asList("first", "first")));
    }
}
