package xin.vanilla.narcissus.integration.map;

import journeymap.client.api.IClientAPI;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Proxy;

import static org.junit.Assert.*;

public class JourneyMapApiReadinessTest {
    private JourneyMap.Adapter adapter(File path, boolean loaded) {
        IClientAPI api = (IClientAPI) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{IClientAPI.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getDataPath")) return path;
                    throw new UnsupportedOperationException(method.getName());
                });
        JourneyMap.Adapter store = new JourneyMap.Adapter() {
            public boolean ready() { return loaded; }
            public void add(JourneyMap.Point point) { }
            public void remove(JourneyMap.Point point) { }
        };
        return new JourneyMapWaypointPlugin.ApiAdapter(api, store);
    }

    @Test public void requiresBothMappedPathAndLoadedStore() throws Exception {
        assertFalse(adapter(null, true).ready());
        assertFalse(adapter(new File("world"), false).ready());
        assertTrue(adapter(new File("world"), true).ready());
    }

    @Test public void contractRejectsStaticAndWrongReturnMethods() throws Exception {
        assertThrows(NoSuchMethodException.class, () -> JourneyMapApiContract.method(Fixture.class, "value", String.class));
        assertThrows(NoSuchMethodException.class, () -> JourneyMapApiContract.method(Fixture.class, "wrong", String.class));
        assertEquals(File.class, JourneyMapApiContract.method(IClientAPI.class, "getDataPath", File.class, String.class).getReturnType());
    }

    public static class Fixture {
        public static String value() { return ""; }
        public int wrong() { return 0; }
    }
}
