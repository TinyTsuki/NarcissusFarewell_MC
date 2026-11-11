package xin.vanilla.narcissus.integration.map;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class JourneyMapApiContractTest {
    @Test
    public void installedPublicApiSatisfiesProductionContract() throws Throwable {
        Method validate = JourneyMapWaypointPlugin.class.getDeclaredMethod("validateContract");
        validate.setAccessible(true);
        try {
            validate.invoke(null);
        } catch (InvocationTargetException error) {
            throw error.getCause();
        }
    }
}
