package xin.vanilla.narcissus.integration.map;

import journeymap.api.v2.client.IClientAPI;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JourneyMapApiReadinessTest {
    private JourneyMap.Adapter adapter(File mappingPath, String optionalWorldId) throws Exception {
        IClientAPI api = (IClientAPI) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IClientAPI.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getWorldId")) return optionalWorldId;
                    if (method.getName().equals("getDataPath")) return mappingPath;
                    throw new UnsupportedOperationException(method.getName());
                });
        Class<?> type = Class.forName(JourneyMapWaypointPlugin.class.getName() + "$ApiAdapter");
        Constructor<?> constructor = type.getDeclaredConstructor(IClientAPI.class);
        constructor.setAccessible(true);
        return (JourneyMap.Adapter) constructor.newInstance(api);
    }

    @Test
    public void mappingWithoutOptionalWorldIdIsReady() throws Exception {
        assertTrue(adapter(new File("owned-world/addon-data/narcissus_farewell"), null).ready());
    }

    @Test
    public void staleWorldIdDoesNotMakeAnUnmappedConnectionReady() throws Exception {
        assertFalse(adapter(null, "previous-world").ready());
    }
}
