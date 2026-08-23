package xin.vanilla.narcissus.screen;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WaypointNameComparatorTest {
    @Test
    public void sortsNumericNamesByValue() {
        assertSorted(Arrays.asList("10", "2", "1", "9"), "1", "2", "9", "10");
    }

    @Test
    public void sortsMixedNamesAndChineseNamesNaturally() {
        assertSorted(Arrays.asList("home10", "home2", "home1"), "home1", "home2", "home10");
        assertSorted(Arrays.asList("家10号", "家2号", "家1号"), "家1号", "家2号", "家10号");
    }

    @Test
    public void keepsEqualNumericValuesDeterministic() {
        assertSorted(Arrays.asList("home002", "home02", "home2"), "home2", "home02", "home002");
    }

    private static void assertSorted(List<String> input, String... expected) {
        List<String> actual = new ArrayList<>(input);
        actual.sort(WaypointNameComparator.INSTANCE);
        assertEquals(Arrays.asList(expected), actual);
    }
}
