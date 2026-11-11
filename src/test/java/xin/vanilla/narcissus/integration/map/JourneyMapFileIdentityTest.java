package xin.vanilla.narcissus.integration.map;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class JourneyMapFileIdentityTest {
    private static final String UUID = "7f21eb5e-04d8-4833-a28e-2e9ac7e859dc";

    private String file(String owner, String id) throws Exception {
        Method method = JourneyMap.class.getDeclaredMethod("waypointFileName", String.class, String.class);
        return (String) method.invoke(null, owner, id);
    }

    @Test
    public void differentKindsHaveStableIndependentFileNames() throws Exception {
        String home = file("narcissus_farewell", "HOME_" + UUID);
        assertEquals("narcissus_farewell_HOME_" + UUID + ".json", home);
        assertNotEquals(home, file("narcissus_farewell", "STAGE_" + UUID));
        assertNotEquals(home, file("narcissus_farewell", "WAYPOINT_" + UUID));
        assertEquals(home, file("narcissus_farewell", "HOME_" + UUID));
    }

    @Test
    public void foreignAndUnrecognizedIdsRetainNativeBehavior() throws Exception {
        assertNull(file("journeymap", "HOME_" + UUID));
        assertNull(file("narcissus_farewell", null));
        assertNull(file("narcissus_farewell", "home"));
        assertNull(file("narcissus_farewell", "OTHER_" + UUID));
        assertNull(file("narcissus_farewell", "HOME_../" + UUID));
    }

    @Test
    public void distinctIdsNeverReuseTheSameFile() throws Exception {
        assertNotEquals(file("narcissus_farewell", "HOME_" + UUID),
                file("narcissus_farewell", "HOME_7f21eb5e-04d8-4833-a28e-2e9ac7e859dd"));
    }


}
