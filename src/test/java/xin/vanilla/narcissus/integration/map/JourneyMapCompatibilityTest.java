package xin.vanilla.narcissus.integration.map;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class JourneyMapCompatibilityTest {
    @Test
    public void absentOptionalMapDoesNotBreakWaypointDispatch() {
        JourneyMap.install(null);
        JourneyMap.startSession();
        try {
            JourneyMap.addWaypoint("home", "minecraft:overworld", -0.2, 64, -1.2);
            JourneyMap.removeWaypoint("home", "minecraft:overworld", -0.2, 64, -1.2);
        } finally {
            JourneyMap.endSession();
        }
    }

    @Test
    public void blockCoordinatesUseFloorOnBothSidesOfZero() throws Exception {
        Method block = JourneyMap.class.getDeclaredMethod("block", double.class);
        block.setAccessible(true);
        for (double value : new double[]{-10.00001, -1.2, -0.2, 0, 0.8, 10.00001}) {
            assertEquals((int) Math.floor(value), block.invoke(null, value));
        }
    }

    @Test
    public void deletionRequiresOwnershipDimensionAndEveryCoordinate() throws Exception {
        Method matches = JourneyMap.class.getDeclaredMethod("matches", String.class, String.class,
                java.util.Collection.class, int.class, int.class, int.class,
                String.class, String.class, int.class, int.class, int.class);
        matches.setAccessible(true);
        Object[] candidate = {"narcissus_farewell", "home", Arrays.asList("minecraft:overworld", "minecraft:the_nether"),
                -1, 64, -2, "home", "minecraft:overworld", -1, 64, -2};
        assertEquals(true, matches.invoke(null, candidate));
        for (int field : new int[]{0, 1, 2, 3, 4, 5}) {
            Object[] wrong = candidate.clone();
            wrong[field] = field == 2 ? Collections.emptyList() : field < 2 ? "manual" : 200;
            assertEquals("field " + field, false, matches.invoke(null, wrong));
        }
        candidate[2] = Collections.singletonList("minecraft:the_end");
        assertEquals(false, matches.invoke(null, candidate));
    }
}
