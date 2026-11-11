package xin.vanilla.narcissus.integration.map;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

public class JourneyMapPortBoundaryTest {
    @Test
    public void optionalMapHasExplicitConnectionLifecycle() throws Exception {
        JourneyMap.class.getDeclaredMethod("startSession");
        JourneyMap.class.getDeclaredMethod("endSession");
        JourneyMap.class.getDeclaredMethod("tick");
    }

    @Test
    public void negativeBlockCoordinatesUseFloor() throws Exception {
        Method block = JourneyMap.class.getDeclaredMethod("block", double.class);
        block.setAccessible(true);
        assertEquals(-1, block.invoke(null, -0.2));
    }

    @Test
    public void deletionRejectsForeignOwnership() throws Exception {
        Method matches = JourneyMap.class.getDeclaredMethod("matches", String.class, String.class,
                Collection.class, int.class, int.class, int.class,
                String.class, String.class, int.class, int.class, int.class);
        matches.setAccessible(true);
        assertEquals(false, matches.invoke(null, "journeymap", "home", java.util.Arrays.asList("minecraft:overworld"),
                -1, 64, -2, "home", "minecraft:overworld", -1, 64, -2));
    }
}
