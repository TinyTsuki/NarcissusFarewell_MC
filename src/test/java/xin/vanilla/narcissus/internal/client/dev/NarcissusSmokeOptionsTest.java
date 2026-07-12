package xin.vanilla.narcissus.internal.client.dev;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NarcissusSmokeOptionsTest {
    @Test
    public void usesSafeDefaults() {
        NarcissusSmokeOptions options = NarcissusSmokeOptions.from(key -> null);

        assertFalse(options.enabled());
        assertFalse(options.exitOnFinish());
        assertFalse(options.teleportEnabled());
        assertEquals("", options.worldName());
    }

    @Test
    public void readsAndTrimsSystemStyleProperties() {
        Map<String, String> values = new HashMap<>();
        values.put("narcissus.uiSmoke", "true");
        values.put("narcissus.uiSmoke.exitOnFinish", "TRUE");
        values.put("narcissus.uiSmoke.teleport", "true");
        values.put("narcissus.uiSmoke.world", "  flat_1_16_5  ");

        NarcissusSmokeOptions options = NarcissusSmokeOptions.from(values::get);

        assertTrue(options.enabled());
        assertTrue(options.exitOnFinish());
        assertTrue(options.teleportEnabled());
        assertEquals("flat_1_16_5", options.worldName());
    }
}
